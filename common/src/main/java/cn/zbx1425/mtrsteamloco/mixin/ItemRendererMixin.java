package cn.zbx1425.mtrsteamloco.mixin;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.StringUtils;
import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy;
import cn.zbx1425.mtrsteamloco.data.EyeCandyRegistry;
import cn.zbx1425.mtrsteamloco.data.EyeCandyProperties;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.sowcerext.reuse.DrawScheduler;
import cn.zbx1425.sowcerext.model.integration.BufferSourceProxy;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.sowcer.math.Matrix4f;
import net.minecraft.client.renderer.entity.ItemRenderer;
import cn.zbx1425.mtrsteamloco.render.ShadersModHandler;
import net.minecraft.client.renderer.texture.OverlayTexture;
#if MC_VERSION <= "11904"
import net.minecraft.client.renderer.block.model.ItemTransforms;
#else
import net.minecraft.world.item.ItemDisplayContext;
#endif

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Inject(method = "render", cancellable = true, at = @At(value = "HEAD"))
#if MC_VERSION <= "11904"
    public void onRender(ItemStack itemStack, ItemTransforms.TransformType transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model, CallbackInfo ci) {
#else
    public void onRender(ItemStack itemStack, ItemDisplayContext transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model, CallbackInfo ci) {
#endif
        if (itemStack.isEmpty()) {
            return;
        }
        Item item = itemStack.getItem();
        if (item instanceof BlockItem bi) {
            Block block = bi.getBlock();
            if (block instanceof BlockEyeCandy) {
                // ci.cancel();
                CompoundTag et = itemStack.getTagElement("BlockEntityTag");
                if (et == null || !et.contains("prefabId")) return;
                String prefabId = et.getString("prefabId");
                if (EyeCandyRegistry.ELEMENTS.containsKey(prefabId)) {
                    EyeCandyProperties properties = EyeCandyRegistry.ELEMENTS.get(prefabId);
                    if (properties.itemModel != null) {
                        ModelCluster cluster = properties.itemModel;
                        poseStack.pushPose();
                        model.getTransforms().getTransform(transformType).apply(leftHand, poseStack);
                        Matrix4f matrix = new Matrix4f(new Matrix4f(poseStack.last().pose()));
                        matrix.translate(0, -0.5f, 0);
                        matrix.mul(properties.itemTransform);
                    #if MC_VERSION <= "11903"
                        if (transformType == ItemTransforms.TransformType.GUI) {
                    #else 
                        if (transformType == ItemDisplayContext.GUI) {
                    #endif
                            if (ShadersModHandler.isShaderPackInUse()) MainClient.drawContext.drawWithBlaze = true;
                            MainClient.drawScheduler.drawAlone(cluster, matrix, combinedLight, combinedOverlay, new BufferSourceProxy(buffer), MainClient.drawContext);
                        } else {
                            MainClient.drawScheduler.enqueue(cluster, matrix, combinedLight, combinedOverlay);
                        }
                        poseStack.popPose();
                        ci.cancel();
                    }
                }
            }
        }
    }
}