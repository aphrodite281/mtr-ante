package cn.zbx1425.mtrsteamloco.render.train;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.mixin.TrainClientAccessor;
import cn.zbx1425.mtrsteamloco.render.RenderUtil;
import cn.zbx1425.sowcerext.multipart.MultipartContainer;
import cn.zbx1425.sowcerext.multipart.MultipartUpdateProp;
import cn.zbx1425.sowcerext.multipart.animated.AnimatedLoader;
import com.mojang.math.Vector3f;
import mtr.MTRClient;
import mtr.data.TrainClient;
import mtr.data.VehicleRidingClient;
import mtr.render.TrainRendererBase;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.List;

public class RenderTrainD51 extends TrainRendererBase {

    protected static MultipartContainer modelD51;

    private final TrainClient train;
    private final MultipartUpdateProp updateProp = new MultipartUpdateProp();

    private final TrainRendererBase trailingCarRenderer;

    private static int renderingCarNum = 0;

    public static void initGLModel(ResourceManager resourceManager) {
        try {
            MainClient.atlasManager.load(resourceManager, new ResourceLocation("mtrsteamloco:models/atlas/d51.json"));
            modelD51 = AnimatedLoader.loadModel(resourceManager, MainClient.modelManager, MainClient.atlasManager,
                    new ResourceLocation("mtrsteamloco:models/d51/d51.animated"));
        } catch (IOException e) {
            modelD51 = null;
            Main.LOGGER.error(e);
        }
    }

    public RenderTrainD51(TrainRendererBase trailingCarRenderer) {
        this.train = null;
        this.trailingCarRenderer = trailingCarRenderer;
    }

    private RenderTrainD51(TrainClient trainClient, TrainRendererBase trailingCarRenderer) {
        this.train = trainClient;
        if (trailingCarRenderer == null) {
            this.trailingCarRenderer = null;
        } else {
            this.trailingCarRenderer = trailingCarRenderer.createTrainInstance(this.train);
        }
    }

    @Override
    public TrainRendererBase createTrainInstance(TrainClient trainClient) {
        return new RenderTrainD51(trainClient, this.trailingCarRenderer);
    }

    @Override
    public void renderCar(int carIndex, double x, double y, double z, float yaw, float pitch, boolean doorLeftOpen, boolean doorRightOpen) {
        if (RenderUtil.shouldSkipRenderTrain(train)) return;

        int carNum = !train.isReversed() ? carIndex : (train.trainCars - carIndex - 1);
        renderingCarNum = carNum;

        if (trailingCarRenderer != null && carNum != 0) {
            int carIndexToRender;
            if (trailingCarRenderer instanceof RenderTrainDK3) {
                if (carNum == 1) {
                    carIndexToRender = (train.trainCars < 3) ? 1 : 0;
                } else if (carNum == 2) {
                    carIndexToRender = 2;
                    matrices.translate(0, 0, 1);
                } else {
                    return;
                }
            } else {
                carIndexToRender = (carNum == train.trainCars - 1) ? carNum: carNum - 1; // Make sure we always get a proper tail
            }

            trailingCarRenderer.renderCar(carIndexToRender, x, y, z, yaw, pitch, doorLeftOpen, doorRightOpen);
            return;
        }

        if (isTranslucentBatch) {
            return;
        }

        final BlockPos posAverage = applyAverageTransform(train.getViewOffset(), x, y, z);
        if (posAverage == null) {
            return;
        }

        matrices.pushPose();
        matrices.translate(x, y - 1, z);
        matrices.mulPose(Vector3f.YP.rotation((float) Math.PI + yaw));
        matrices.mulPose(Vector3f.XP.rotation(train.transportMode.hasPitch ? pitch : 0));

        if (train.isReversed()) {
            matrices.mulPose(Vector3f.YP.rotation((float) Math.PI));
        }

        final int light = LightTexture.pack(world.getBrightness(LightLayer.BLOCK, posAverage), world.getBrightness(LightLayer.SKY, posAverage));

        updateProp.update(train, carIndex, !train.isReversed());

        RenderUtil.updateAndEnqueueAll(modelD51, updateProp, matrices.last().pose(), light, vertexConsumers);

        if (ClientConfig.enableSmoke && train.getIsOnRoute() && (int)MTRClient.getGameTick() % 4 == 0) {
            Vector3f smokeOrigin = new Vector3f(0, 2.7f, 8.4f);
            Vector3f carPos = new Vector3f((float)x, (float)y, (float)z);
            Vec3 offset = ((TrainClientAccessor) train).getVehicleRidingClient().getVehicleOffset();
            if (offset != null) {
                carPos.add((float)offset.x, (float)offset.y, (float)offset.z);
            }

            smokeOrigin.transform(Vector3f.XP.rotation(pitch));
            smokeOrigin.transform(Vector3f.YP.rotation((!train.isReversed() ? (float) Math.PI : 0) + yaw));
            smokeOrigin.add(carPos);
            world.addParticle(Main.PARTICLE_STEAM_SMOKE, smokeOrigin.x(), smokeOrigin.y(), smokeOrigin.z(), 0.0, 0.7f, 0.0);
        }

        matrices.popPose();
        matrices.popPose();
    }

    @Override
    public void renderConnection(Vec3 prevPos1, Vec3 prevPos2, Vec3 prevPos3, Vec3 prevPos4, Vec3 thisPos1, Vec3 thisPos2, Vec3 thisPos3, Vec3 thisPos4, double x, double y, double z, float yaw, float pitch) {
        if (trailingCarRenderer != null && renderingCarNum > 1) {
            trailingCarRenderer.renderConnection(prevPos1, prevPos2, prevPos3, prevPos4, thisPos1, thisPos2, thisPos3, thisPos4, x, y, z, yaw, pitch);
        }
    }

    @Override
    public void renderBarrier(Vec3 prevPos1, Vec3 prevPos2, Vec3 prevPos3, Vec3 prevPos4, Vec3 thisPos1, Vec3 thisPos2, Vec3 thisPos3, Vec3 thisPos4, double x, double y, double z, float yaw, float pitch) {
        if (trailingCarRenderer != null && renderingCarNum > 1) {
            trailingCarRenderer.renderBarrier(prevPos1, prevPos2, prevPos3, prevPos4, thisPos1, thisPos2, thisPos3, thisPos4, x, y, z, yaw, pitch);
        }
    }

}
