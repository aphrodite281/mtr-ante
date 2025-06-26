package cn.zbx1425.mtrsteamloco.render.block;

import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.block.BlockDirectNode.BlockEntityDirectNode;
import cn.zbx1425.mtrsteamloco.data.EyeCandyProperties;
import cn.zbx1425.mtrsteamloco.data.EyeCandyRegistry;
import cn.zbx1425.mtrsteamloco.render.ShadersModHandler;
import cn.zbx1425.mtrsteamloco.render.rail.RailRenderDispatcher;
import cn.zbx1425.mtrsteamloco.scripting.ScriptContextManager;
import cn.zbx1425.mtrsteamloco.scripting.eyecandy.EyeCandyScriptContext;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.PoseStackUtil;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.sowcerext.model.integration.BufferSourceProxy;
import cn.zbx1425.mtrsteamloco.ClientConfig;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import mtr.RegistryObject;
import mtr.block.BlockNode;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.data.TrainClient;
import mtr.data.RailAngle;
import mtr.mappings.BlockEntityRendererMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import cn.zbx1425.sowcer.object.VertArray;
import cn.zbx1425.sowcer.model.VertArrays;
import cn.zbx1425.mtrsteamloco.render.integration.MtrModelRegistryUtil;
import net.minecraft.client.renderer.LevelRenderer;
#if MC_VERSION >= "11904"
import net.minecraft.world.item.ItemDisplayContext;
#else
import net.minecraft.client.renderer.block.model.ItemTransforms;
#endif
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.sowcerext.reuse.ModelManager;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class BlockEntityDirectNodeRenderer extends BlockEntityRendererMapper<BlockEntityDirectNode> {

    private static final ResourceLocation VERTICAL_MODEL_LOCATION = new ResourceLocation(Main.MOD_ID, "models/block/rail_node_vertical.obj");
    private static final ResourceLocation CONNECTION_MODEL_LOCATION = new ResourceLocation(Main.MOD_ID, "models/block/rail_node_connection.obj");

    private static ModelCluster VERTICAL_MODEL = null;
    private static ModelCluster CONNECTION_MODEL = null;

    public static void initGLModel(ResourceManager resourceManager) {
        try {
            VERTICAL_MODEL = MainClient.modelManager.uploadVertArrays(MainClient.modelManager.loadRawModel(resourceManager, VERTICAL_MODEL_LOCATION, null));
            applyColor(VERTICAL_MODEL, 0xAAAAFFFF);
        } catch (Exception e) {
            Main.LOGGER.error("Failed to load vertical model: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            CONNECTION_MODEL = MainClient.modelManager.uploadVertArrays(MainClient.modelManager.loadRawModel(resourceManager, CONNECTION_MODEL_LOCATION, null));
            applyColor(CONNECTION_MODEL, 0xAAAAFFFF);
        } catch (Exception e) {
            Main.LOGGER.error("Failed to load connection model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void applyColor(ModelCluster model, int color) {
        VertArrays[] vertArrays = new VertArrays[] {model.uploadedOpaqueParts, model.uploadedTranslucentParts};
        for (VertArrays vertArray : vertArrays) {
            for (VertArray vert : vertArray.meshList) {
                vert.materialProp.attrState.setColor(color);
            }
        }
    }

    public BlockEntityDirectNodeRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    private static HashSet<BlockEntityDirectNode> entitysToRender = new HashSet<>();
    private static HashSet<BlockEntityDirectNode> entitysToRenderWritting = new HashSet<>();

    @Override
    public void render(BlockEntityDirectNode blockEntity, float f, @NotNull PoseStack matrices, @NotNull MultiBufferSource vertexConsumers, int light, int overlay) {
        entitysToRenderWritting.add(blockEntity);
    }

    public static void exchange() {
        entitysToRender = entitysToRenderWritting;
        entitysToRenderWritting = new HashSet<>();
    }

    public static void commit(@NotNull PoseStack matrices, @NotNull MultiBufferSource vertexConsumers) {
        if (VERTICAL_MODEL == null || CONNECTION_MODEL == null) return;
        Matrix4f worldPose = new Matrix4f(matrices.last().pose()).copy();
        HashSet<BlockEntityDirectNode> temp = new HashSet<>(entitysToRender);
        for (BlockEntityDirectNode blockEntity : temp) {
            if (blockEntity == null) continue;
            
            final Level world = blockEntity.getLevel();
            if (world == null) continue;

            int light = LevelRenderer.getLightColor(world, blockEntity.getBlockPos());;

            Matrix4f basePose = worldPose.copy();
            final BlockPos pos = blockEntity.getBlockPos();
            basePose.translate(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);

            RailAngle railAngle = blockEntity.getRailAngle();
            if (railAngle == null) {
                double k = 2 * Math.PI;
                double v = System.currentTimeMillis() / 1000D * k % k;
                basePose.rotateY((float) v);
                MainClient.drawScheduler.enqueue(VERTICAL_MODEL, basePose, light);
            } else {
                boolean b = blockEntity.getBlockState().getValue(BlockNode.IS_CONNECTED);
                if (b && ClientConfig.enableRail3D) continue;
                basePose.rotateY((float) Math.PI / 2F - (float) railAngle.angleRadians);
                MainClient.drawScheduler.enqueue(b ? CONNECTION_MODEL : VERTICAL_MODEL, basePose, light);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull BlockEntityDirectNode blockEntity) {
        return true;
    }

    @Override
    public boolean shouldRender(@NotNull BlockEntityDirectNode blockEntity, @NotNull Vec3 vec3) {
        return true;
    }
}