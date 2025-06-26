package cn.zbx1425.mtrsteamloco.render.block;

import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy;
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
import com.mojang.blaze3d.vertex.PoseStack;
import mtr.RegistryObject;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.data.TrainClient;
import mtr.mappings.BlockEntityRendererMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.LevelRenderer;
#if MC_VERSION >= "11904"
import net.minecraft.world.item.ItemDisplayContext;
#else
import net.minecraft.client.renderer.block.model.ItemTransforms;
#endif
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import cn.zbx1425.mtrsteamloco.Main;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class BlockEntityEyeCandyRenderer extends BlockEntityRendererMapper<BlockEyeCandy.BlockEntityEyeCandy> {

    public BlockEntityEyeCandyRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    private static HashSet<BlockEyeCandy.BlockEntityEyeCandy> entitysToRender = new HashSet<>();
    private static HashSet<BlockEyeCandy.BlockEntityEyeCandy> entitysToRenderWritting = new HashSet<>();

    private static final RegistryObject<ItemStack> BRUSH_ITEM_STACK = new RegistryObject<>(() -> new ItemStack(mtr.Items.BRUSH.get(), 1));

    private static final RegistryObject<ItemStack> BARRIER_ITEM_STACK = new RegistryObject<>(() -> new ItemStack(net.minecraft.world.item.Items.BARRIER, 1));

    @Override
    public void render(BlockEyeCandy.BlockEntityEyeCandy blockEntity, float f, @NotNull PoseStack matrices, @NotNull MultiBufferSource vertexConsumers, int light, int overlay) {
        entitysToRenderWritting.add(blockEntity);
    }

    public static void exchange() {
        entitysToRender = entitysToRenderWritting;
        entitysToRenderWritting = new HashSet<>();
    }

    private static void print(String s) {
        Main.LOGGER.info(s);
    }

    public static void commit(@NotNull PoseStack matrices, @NotNull MultiBufferSource vertexConsumers) {
        Matrix4f worldPose = new Matrix4f(matrices.last().pose()).copy();
        HashSet<BlockEyeCandy.BlockEntityEyeCandy> temp = new HashSet<>(entitysToRender);
        for (BlockEyeCandy.BlockEntityEyeCandy blockEntity : temp) {
            if (blockEntity == null) continue;
            
            final Level world = blockEntity.getLevel();
            if (world == null) continue;
            BlockPos pos = blockEntity.getBlockPos();

            int light = LevelRenderer.getLightColor(world, pos);;

            int lightToUse = blockEntity.fullLight ? LightTexture.pack(15, 15) : light;

            EyeCandyProperties prop = EyeCandyRegistry.getProperty(blockEntity.prefabId);
            if (prop == null || RailRenderDispatcher.isHoldingBrush) {
                matrices.pushPose();
                matrices.translate(pos.getX(), pos.getY(), pos.getZ());
                matrices.translate(0.5f, 0.5f, 0.5f);
                PoseStackUtil.rotY(matrices, (float) ((System.currentTimeMillis() % 1000) * (Math.PI * 2 / 1000)));
#if MC_VERSION >= "11904"
                if (blockEntity.prefabId != null && prop == null) {
                    Minecraft.getInstance().getItemRenderer().renderStatic(BARRIER_ITEM_STACK.get(), ItemDisplayContext.GROUND, lightToUse, 0, matrices, vertexConsumers, world, 0);
                } else {
                    Minecraft.getInstance().getItemRenderer().renderStatic(BRUSH_ITEM_STACK.get(), ItemDisplayContext.GROUND, lightToUse, 0, matrices, vertexConsumers, world, 0);
                }
#else
                if (blockEntity.prefabId != null && prop == null) {
                    Minecraft.getInstance().getItemRenderer().renderStatic(BARRIER_ITEM_STACK.get(), ItemTransforms.TransformType.GROUND, lightToUse, 0, matrices, vertexConsumers, 0);
                } else {
                    Minecraft.getInstance().getItemRenderer().renderStatic(BRUSH_ITEM_STACK.get(), ItemTransforms.TransformType.GROUND, lightToUse, 0, matrices, vertexConsumers, 0);
                }
#endif
                matrices.popPose();

            }
            if (prop == null) continue;
            
            Matrix4f candyPose = worldPose.copy();
            candyPose.mul(blockEntity.getBaseMatrix());
            if (prop.model != null) {
                MainClient.drawScheduler.enqueue(prop.model, candyPose, lightToUse);
            }
            if (prop.script != null && blockEntity.scriptContext != null) {
                synchronized (blockEntity.scriptContext) {
                    blockEntity.scriptContext.commit(MainClient.drawScheduler, candyPose, worldPose, lightToUse);
                }
                prop.script.tryCallRenderFunctionAsync(blockEntity.scriptContext);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(BlockEyeCandy.@NotNull BlockEntityEyeCandy blockEntity) {
        return true;
    }

    @Override
    public boolean shouldRender(BlockEyeCandy.@NotNull BlockEntityEyeCandy blockEntity, @NotNull Vec3 vec3) {
        return true;
    }
}