package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.render.rail.RailRenderDispatcher;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import mtr.MTRClient;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.Level;
import cn.zbx1425.mtrsteamloco.data.Rolling;
import mtr.client.ClientData;
import cn.zbx1425.mtrsteamloco.mixin.TrainAccessor;
import cn.zbx1425.mtrsteamloco.mixin.RenderTrainsAccessor;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow @Final Minecraft minecraft;
    @Unique private Boolean hideGuiOptionCache = null;

#if MC_VERSION >= "11903"
    @Inject(method = "getProjectionMatrix", at = @At("TAIL"), cancellable = true)
    void getProjectionMatrixTail(double fov, CallbackInfoReturnable<org.joml.Matrix4f> cir) {
        if (RailRenderDispatcher.isPreviewingModel) {
            org.joml.Matrix4f result = new org.joml.Matrix4f();
            result.translation(0.5f, 0f, 0f);
            result.scale(0.8f, 0.8f, 1f);
            result.mul(cir.getReturnValue());
#else
    @Inject(method = "getProjectionMatrix", at = @At("TAIL"), cancellable = true)
    void getProjectionMatrixTail(double fov, CallbackInfoReturnable<com.mojang.math.Matrix4f> cir) {
        if (RailRenderDispatcher.isPreviewingModel) {
            com.mojang.math.Matrix4f result = com.mojang.math.Matrix4f.createTranslateMatrix(0.5f, 0f, 0f);
            result.multiply(com.mojang.math.Matrix4f.createScaleMatrix(0.8f, 0.8f, 1f));
            result.multiply(cir.getReturnValue());
#endif

            cir.setReturnValue(result);
        }
        if (RailRenderDispatcher.isPreviewingModel && hideGuiOptionCache == null) {
            hideGuiOptionCache = minecraft.options.hideGui;
            minecraft.options.hideGui = true;
        } else if (!RailRenderDispatcher.isPreviewingModel && hideGuiOptionCache != null) {
            minecraft.options.hideGui = hideGuiOptionCache;
            hideGuiOptionCache = null;
        }
    }

    @Inject(
        method = "renderLevel",
        at = @At("HEAD")
    )
    private void injectSimulateTrains(
        float partialTick,
        long finishNanoTime,
        PoseStack poseStack,
        CallbackInfo ci
    ) {
        // simulateTrains(Minecraft.getInstance());
        // Rolling.update();
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
#if MC_VERSION >= "11903"
            target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;normal()Lorg/joml/Matrix3f;",
#else 
            target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;normal()Lcom/mojang/math/Matrix3f;",
#endif
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void injectCustomRotation(
        float partialTick,
        long finishNanoTime,
        PoseStack poseStack,
        CallbackInfo ci
    ) {
        Rolling.applyRolling(poseStack);
    }

    private void simulateTrains(Minecraft client) {
        if (client == null) return;
        Level world = client.level;
        if (world == null) return;
		final float lastFrameDuration = MTRClient.getLastFrameDuration();
		final float newLastFrameDuration = client.isPaused() || RenderTrainsAccessor.getLastRenderedTick() == MTRClient.getGameTick() ? 0 : lastFrameDuration;
        ClientData.TRAINS.forEach(train -> {
            // ((TrainAccessor) train).invokeSimulateTrain(world, newLastFrameDuration, null);
        });
    }
}
