package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.render.RailPicker;
import cn.zbx1425.mtrsteamloco.render.RenderUtil;
import cn.zbx1425.mtrsteamloco.render.ShadersModHandler;
import cn.zbx1425.mtrsteamloco.render.rail.RailRenderDispatcher;
import cn.zbx1425.mtrsteamloco.scripting.ScriptContextManager;
import cn.zbx1425.sowcer.util.GlStateTracker;
import cn.zbx1425.sowcerext.model.integration.BufferSourceProxy;
import com.mojang.blaze3d.vertex.PoseStack;
import cn.zbx1425.sowcer.math.Matrix4f;
import net.minecraft.client.player.LocalPlayer;
import mtr.entity.EntitySeat;
import net.minecraft.resources.ResourceLocation;
import cn.zbx1425.mtrsteamloco.render.block.BlockEntityEyeCandyRenderer;
import cn.zbx1425.mtrsteamloco.render.block.BlockEntityDirectNodeRenderer;
import mtr.render.RenderTrains;
import net.minecraft.client.Minecraft;
import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.Level;
import cn.zbx1425.mtrsteamloco.gui.DirectNodeScreen;
import cn.zbx1425.mtrsteamloco.render.RailDistanceRenderer;
import mtr.mappings.EntityRendererMapper;
import mtr.entity.EntitySeat;
import cn.zbx1425.mtrsteamloco.data.Rolling;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mtr.MTRClient;
import mtr.block.BlockNode;
import mtr.block.BlockPlatform;
import mtr.block.BlockSignalLightBase;
import mtr.block.BlockSignalSemaphoreBase;
import mtr.client.*;
import mtr.data.*;
import mtr.entity.EntitySeat;
import mtr.item.ItemNodeModifierBase;
import mtr.mappings.EntityRendererMapper;
import mtr.mappings.Text;
import mtr.mappings.Utilities;
import mtr.mappings.UtilitiesClient;
import mtr.model.ModelLift1;
import mtr.path.PathData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import cn.zbx1425.mtrsteamloco.item.RoutePathCreator;

import java.util.*;

import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTrains.class)
public class RenderTrainsMixin extends EntityRendererMapper<EntitySeat> implements IGui{

    RenderTrainsMixin() {
        super(null);
    }

    @Override
	public ResourceLocation getTextureLocation(EntitySeat entity) {
		return null;
	}

    @Shadow(remap = false) private static void renderRailStandard(Level world, Rail rail, float yOffset, boolean renderColors, float railWidth) {
        throw new IllegalStateException("Mixin failed to apply");
    }

    @Shadow(remap = false) private static void renderRailStandard(Level world, Rail rail, float yOffset, boolean renderColors, float railWidth, String texture, float u1, float v1, float u2, float v2) {
        throw new IllegalStateException("Mixin failed to apply");
    }

    @Shadow(remap = false) private static void renderSignalsStandard(Level world, PoseStack matrices, MultiBufferSource vertexConsumers, Rail rail, BlockPos startPos, BlockPos endPos) {
        throw new IllegalStateException("Mixin failed to apply");
    }

    private static void lambda$render$8(net.minecraft.client.player.LocalPlayer player, net.minecraft.core.BlockPos startPos, int maxRailDistance, java.util.Map<UUID, RailType> renderedRailMap, net.minecraft.world.level.Level world, boolean renderColors, com.mojang.blaze3d.vertex.PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource vertexConsumers, net.minecraft.core.BlockPos endPos, mtr.data.Rail rail) {

        if (!((RailExtraSupplier) (Object) rail).isBetween(player.getX(), player.getY(), player.getZ(), maxRailDistance)) {
			return;
		}

		final UUID railProduct = PathData.getRailProduct(startPos, endPos);
		if (renderedRailMap.containsKey(railProduct)) {
			if (renderedRailMap.get(railProduct) == rail.railType) {
				return;
			}
		} else {
			renderedRailMap.put(railProduct, rail.railType);
		}

		switch (rail.transportMode) {
			case TRAIN:
				renderRailStandard(world, rail, 0.0625F + SMALL_OFFSET, renderColors, 1);
				if (renderColors) {
					renderSignalsStandard(world, matrices, vertexConsumers, rail, startPos, endPos);
				}
				break;
			case BOAT:
				if (renderColors) {
					renderRailStandard(world, rail, 0.0625F + SMALL_OFFSET, true, 0.5F);
					renderSignalsStandard(world, matrices, vertexConsumers, rail, startPos, endPos);
				}
				break;
			case CABLE_CAR:
				if (rail.railType.hasSavedRail || rail.railType == RailType.CABLE_CAR_STATION) {
					renderRailStandard(world, rail, 0.25F + SMALL_OFFSET, renderColors, 0.25F, "mtr:textures/block/metal.png", 0.25F, 0, 0.75F, 1);
				}
				if (renderColors && !rail.railType.hasSavedRail) {
					renderRailStandard(world, rail, 0.5F + SMALL_OFFSET, true, 1, "mtr:textures/block/one_way_rail_arrow.png", 0, 0.75F, 1, 0.25F);
				}

				if (rail.railType != RailType.NONE) {
					rail.render((x1, z1, x2, z2, x3, z3, x4, z4, y1, y2) -> {
						final int r = renderColors ? (rail.railType.color >> 16) & 0xFF : 0;
						final int g = renderColors ? (rail.railType.color >> 8) & 0xFF : 0;
						final int b = renderColors ? rail.railType.color & 0xFF : 0;
						IDrawing.drawLine(matrices, vertexConsumers, (float) x1, (float) y1 + 0.5F, (float) z1, (float) x3, (float) y2 + 0.5F, (float) z3, r, g, b);
					}, 0, 0);
				}

				break;
			case AIRPLANE:
				if (renderColors) {
					renderRailStandard(world, rail, 0.0625F + SMALL_OFFSET, true, 1);
					renderSignalsStandard(world, matrices, vertexConsumers, rail, startPos, endPos);
				} else {
					renderRailStandard(world, rail, 0.0625F + SMALL_OFFSET, false, 0.25F, "textures/block/iron_block.png", 0.25F, 0, 0.75F, 1);
				}
				break;
		}
    }

    @Inject(at = @At("HEAD"),
            method = "render(Lmtr/entity/EntitySeat;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V")
    private static void renderHead(EntitySeat entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci) {
        Minecraft.getInstance().level.getProfiler().popPush("MTRRailwayData");
        RenderUtil.commonVertexConsumers = vertexConsumers;
        RenderUtil.commonPoseStack = matrices;
        RenderUtil.updateElapsedTicks();
    }

    @Inject(at = @At("TAIL"),
            method = "render(Lmtr/entity/EntitySeat;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V")
    private static void renderTail(EntitySeat entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci) {
        // Already once per frame, since TAIL
        Rolling.update();
        Minecraft.getInstance().level.getProfiler().popPush("NTERailwayData");
        Matrix4f viewMatrix = new Matrix4f(matrices.last().pose());
        MainClient.railRenderDispatcher.prepareDraw();
        if (ClientConfig.getRailRenderLevel() >= 2) {
            GlStateTracker.capture();
            MainClient.railRenderDispatcher.drawRails(Minecraft.getInstance().level, MainClient.drawScheduler, viewMatrix);
            MainClient.drawScheduler.commitRaw(MainClient.drawContext);

            GlStateTracker.restore();
            if (Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes() && !Minecraft.getInstance().showOnlyReducedInfo()) {
                MainClient.railRenderDispatcher.drawBoundingBoxes(matrices, vertexConsumers.getBuffer(RenderType.lines()));
            }

            MainClient.railRenderDispatcher.drawRailNodes(Minecraft.getInstance().level, MainClient.drawScheduler, viewMatrix);
        }

        // if (ShadersModHandler.isRenderingShadowPass()) {
        //     Main.LOGGER.info("0 shadow pass" + System.currentTimeMillis() + " " + entity);
        // } else {
        //     Main.LOGGER.info("0 normal pass" + System.currentTimeMillis() + " " + entity);
        // }

        BlockEntityEyeCandyRenderer.commit(matrices, vertexConsumers);
        BlockEntityDirectNodeRenderer.commit(matrices, vertexConsumers);

        MainClient.drawContext.drawWithBlaze = !ClientConfig.useRenderOptimization();
        MainClient.drawContext.sortTranslucentFaces = ClientConfig.translucentSort;
        BufferSourceProxy vertexConsumersProxy = new BufferSourceProxy(vertexConsumers);
        MainClient.drawScheduler.commit(vertexConsumersProxy, MainClient.drawContext);
        vertexConsumersProxy.commit();

        if (Minecraft.getInstance().player != null && RailRenderDispatcher.isHoldingBrush) {
            RailPicker.pick();
            RailPicker.render(matrices, vertexConsumers);
            RailDistanceRenderer.render(matrices, vertexConsumers);
        } else {
            RailPicker.pickedRail = null;
        }

        ScriptContextManager.disposeDeadContexts();
    }

    @Inject(at = @At("HEAD"), cancellable = true,
            method = "renderRailStandard(Lnet/minecraft/world/level/Level;Lmtr/data/Rail;FZFLjava/lang/String;FFFF)V")
    private static void onRenderRailStandard(Level world, Rail rail, float yOffset, boolean renderColors, float railWidth, String texture, float u1, float v1, float u2, float v2, CallbackInfo ci) {
        if (ClientConfig.getRailRenderLevel() == 0) {
            ci.cancel();
            return;
        }
        if (ClientConfig.getRailRenderLevel() >= 2) {
            boolean railAccepted = MainClient.railRenderDispatcher.registerRail(rail);
            if (railAccepted) ci.cancel();
        }
    }

    @Inject(method = "isHoldingRailRelated", at = @At("TAIL"), remap = false, cancellable = true) 
    private static void onIsHoldingRailRelated(Player player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(
            cir.getReturnValue() ||
            Utilities.isHolding(player, item -> item instanceof RoutePathCreator)
        );
    }

}
