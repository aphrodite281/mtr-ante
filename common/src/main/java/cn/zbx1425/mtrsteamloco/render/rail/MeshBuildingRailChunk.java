package cn.zbx1425.mtrsteamloco.render.rail;

import cn.zbx1425.mtrsteamloco.data.RailModelRegistry;
import cn.zbx1425.sowcer.batch.BatchManager;
import cn.zbx1425.sowcer.batch.EnqueueProp;
import cn.zbx1425.sowcer.batch.ShaderProp;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.sowcer.model.Model;
import cn.zbx1425.sowcer.model.VertArrays;
import cn.zbx1425.sowcer.vertex.VertAttrMapping;
import cn.zbx1425.sowcer.vertex.VertAttrSrc;
import cn.zbx1425.sowcer.vertex.VertAttrState;
import cn.zbx1425.sowcer.vertex.VertAttrType;
import cn.zbx1425.sowcerext.model.*;
import net.minecraft.client.renderer.LightTexture;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import cn.zbx1425.mtrsteamloco.data.RailModelProperties;
import com.mojang.blaze3d.systems.RenderSystem;
import mtr.data.Rail;
import net.minecraft.world.level.LightLayer;
import cn.zbx1425.sowcerext.reuse.DrawScheduler;
import net.minecraft.world.phys.Vec3;
import cn.zbx1425.sowcer.batch.MaterialProp;
import cn.zbx1425.mtrsteamloco.render.RenderUtil;
import cn.zbx1425.mtrsteamloco.ClientConfig;

import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.Map;

public class MeshBuildingRailChunk extends RailChunkBase {

    private final RawModel railModel;

    private boolean deform = false;
    private Model uploadedCombinedModel;
    private VertArrays vertArrays;

	private static final double ACCEPT_THRESHOLD = 1e-4;
    private static final double HALF_ACCEPT_THRESHOLD = ACCEPT_THRESHOLD / 2;


    private static final VertAttrMapping RAIL_MAPPING = new VertAttrMapping.Builder()
            .set(VertAttrType.POSITION, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.COLOR, VertAttrSrc.VERTEX_BUF_OR_GLOBAL)
            .set(VertAttrType.UV_TEXTURE, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.UV_OVERLAY, VertAttrSrc.GLOBAL)
            .set(VertAttrType.UV_LIGHTMAP, VertAttrSrc.VERTEX_BUF_OR_GLOBAL)
            .set(VertAttrType.NORMAL, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.MATRIX_MODEL, VertAttrSrc.GLOBAL)
            .build();

    protected MeshBuildingRailChunk(Long chunkId, String modelKey) {
        super(chunkId, modelKey);
        this.railModel = RailModelRegistry.getProperty(modelKey).rawModel;
    }

    @Override
    public void rebuildBuffer(Level world) {
        super.rebuildBuffer(world);
        if (railModel == null) return;

        EXECUTOR.execute(() -> {
            RawModel combinedModel = ClientConfig.enableRailDeform ? transformModelDeform(world) : transformModel(world);
            checkBoundingBox();
            
            Supplier<Model> supplier = combinedModel.uploadAsync(RAIL_MAPPING);
            UPLOAD_QUEUE.offer(() -> {
                if (uploadedCombinedModel != null) uploadedCombinedModel.close();
                if (vertArrays != null) vertArrays.close();
                uploadedCombinedModel = supplier.get();
                vertArrays = VertArrays.createAll(uploadedCombinedModel, RAIL_MAPPING, null);
                bufferBuilding = false;
            });
        });
    }

    private RawModel transformModel(Level world) {
        deform = false;
        RawModel combinedModel = new RawModel();

        for (Map.Entry<BakedRail, ArrayList<Matrix4f>> entry : containingRails.entrySet()) {
            ArrayList<Matrix4f> railSpan = entry.getValue();
            for (Matrix4f pieceMat : railSpan) {
                final Vector3f lightPos = pieceMat.getTranslationPart();
                final BlockPos lightBlockPos = new BlockPos(Mth.floor(lightPos.x()), Mth.floor(lightPos.y() + 0.1), Mth.floor(lightPos.z()));
                final int light = LightTexture.pack(world.getBrightness(LightLayer.BLOCK, lightBlockPos), world.getBrightness(LightLayer.SKY, lightBlockPos));
                combinedModel.appendTransformed(railModel, pieceMat, entry.getKey().color, light);
            }
        }

        return  combinedModel;
    }

    private void checkBoundingBox() {
        float yMin = 256, yMax = -64;
        for (Map.Entry<BakedRail, ArrayList<Matrix4f>> entry : containingRails.entrySet()) {
            for (Matrix4f pieceMat : entry.getValue()) {
                final Vector3f lightPos = pieceMat.getTranslationPart();
                yMin = Math.min(yMin, lightPos.y());
                yMax = Math.max(yMax, lightPos.y());
            }
        }

        if (yMin > yMax) yMin = yMax;
        setBoundingBox(yMin, yMax);
    }

    private RawModel transformModelDeform(Level world) {
        deform = true;
        RawModel combinedModel = new RawModel();
        for (BakedRail bakedRail : containingRails.keySet()) {
            Rail rail = bakedRail.rail;
            RailModelProperties prop = bakedRail.getProperties();
            RailExtraSupplier supplier = (RailExtraSupplier) rail;
            boolean reverse = supplier.getRenderReversed();
            float interval = prop.repeatInterval;
            float yOffset = prop.yOffset;
            RawModel railModel = prop.rawModel;
            final double length = rail.getLength() - ACCEPT_THRESHOLD;
            final double ins = length / Math.max(1, Math.round(length / interval));
            final double halfIns = ins / 2;

            for (double i = halfIns; i <= length ; i += ins) {
                Vec3 mid = rail.getPosition(i);
                if (BakedRail.chunkIdFromWorldPos((int) mid.x, (int) mid.z) != chunkId) continue;

                Vector3f lightPos = new Vector3f(mid);
                BlockPos lightBlockPos = new BlockPos(Mth.floor(lightPos.x()), Mth.floor(lightPos.y() + 0.1), Mth.floor(lightPos.z()));
                int light = LightTexture.pack(world.getBrightness(LightLayer.BLOCK, lightBlockPos), world.getBrightness(LightLayer.SKY, lightBlockPos));

                RawModel rm = railModel.copy();
                for (Map.Entry<MaterialProp, RawMesh> entry : rm.meshList.entrySet()) {
                    for (Vertex v : entry.getValue().vertices) {
                        Matrix4f mat = getLookAtMat(rail, transform(i, ins, interval, v.position.z(), reverse), yOffset, reverse);
                        v.position = mat.transform(new Vector3f(v.position.x(), v.position.y(), 0));
                        v.normal = mat.transform3(v.normal);
                        v.light = light;
                        v.color = bakedRail.color;
                    }
                }
                combinedModel.append(rm);
            }
        }
        return combinedModel;
    }

    private double transform(double i, double di, double interval, double value, boolean reverse) {
        return i + di * (value / interval) * (reverse ? -1 : 1);
    }

    private Matrix4f getLookAtMat(Rail rail, double value, float yOffset, boolean reverse) {
        if (value < HALF_ACCEPT_THRESHOLD) value = HALF_ACCEPT_THRESHOLD;
        else if (value + HALF_ACCEPT_THRESHOLD > rail.getLength()) value = rail.getLength() - HALF_ACCEPT_THRESHOLD;
        Vec3 from = rail.getPosition(value - HALF_ACCEPT_THRESHOLD);
        Vec3 to = rail.getPosition(value + HALF_ACCEPT_THRESHOLD);
        Vec3 mid = from.add(to).scale(0.5F);
        float roll = RailExtraSupplier.getRollAngle(rail, value);
        return BakedRail.getLookAtMat(mid, from, to, roll, yOffset, reverse);
    }

    @Override
    public void enqueue(BatchManager batchManager, ShaderProp shaderProp) {
        if (railModel == null) return;

        if (vertArrays == null) return;

        if (ClientConfig.enableRailDeform != deform) isDirty = true;

        VertAttrState attrState = new VertAttrState().setModelMatrix(shaderProp.viewMatrix).setOverlayUVNoOverlay();
        if (!RailRenderDispatcher.isHoldingRailItem) attrState.setColor(-1);
        batchManager.enqueue(vertArrays, new EnqueueProp(attrState), ShaderProp.DEFAULT);
    }

    @Override
    public void close() {
        if (vertArrays != null) vertArrays.close();
        if (uploadedCombinedModel != null) uploadedCombinedModel.close();
        vertArrays = null;
        uploadedCombinedModel = null;
    }
}
