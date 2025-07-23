package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.gui.BrushEditRailScreen;
import cn.zbx1425.mtrsteamloco.network.PacketScreen;
import cn.zbx1425.mtrsteamloco.render.RailPicker;
import mtr.item.ItemWithCreativeTabBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import mtr.block.BlockNode;
import cn.zbx1425.mtrsteamloco.block.BlockDirectNode.BlockEntityDirectNode;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemWithCreativeTabBase.class)
public abstract class ItemWithCreativeTabBaseMixin extends Item {

    public ItemWithCreativeTabBaseMixin(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (this == mtr.Items.BRUSH.get()) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (block instanceof BlockNode) {
                if (blockEntity instanceof BlockEntityDirectNode e) {
                    if (state.getValue(BlockNode.IS_CONNECTED) == false) {
                        if (!level.isClientSide) {
                            PacketScreen.sendScreenBlockS2C((ServerPlayer) context.getPlayer(), "direct_node", pos);
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
                if (context.isSecondaryUseActive()) {
                    if (level.isClientSide) {
                        // BrushEditRailScreen.acquirePickInfoWhenUse();
                        return super.useOn(context);
                    } else {
                        PacketScreen.sendScreenS2C((ServerPlayer) context.getPlayer(), "brush_edit_rail");
                    }
                } else {
                    if (level.isClientSide) {
                        // BrushEditRailScreen.acquirePickInfoWhenUse();
                        CompoundTag railBrushProp = context.getPlayer().getMainHandItem().getTagElement("NTERailBrush");
                        BrushEditRailScreen.applyBrushToPickedRail(railBrushProp, true);
                    } else {
                        return super.useOn(context);
                    }
                }
                return InteractionResult.SUCCESS;
            } else {
                return super.useOn(context);
            }
        } else {
            return super.useOn(context);
        }
    }
}