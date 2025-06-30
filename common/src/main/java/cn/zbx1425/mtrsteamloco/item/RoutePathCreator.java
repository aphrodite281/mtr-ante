package cn.zbx1425.mtrsteamloco.item;

import net.minecraft.core.BlockPos;
import mtr.CreativeModeTabs;
import mtr.item.ItemWithCreativeTabBase;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import mtr.block.BlockNode;
import net.minecraft.world.item.ItemStack;
import mtr.data.RailAngle;
import mtr.data.RailwayData;
import net.minecraft.nbt.CompoundTag;
import mtr.mappings.Text;
import cn.zbx1425.mtrsteamloco.mixin.RailwayDataAccessor;
import cn.zbx1425.mtrsteamloco.Main;
import mtr.data.Rail;
import mtr.data.RailType;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoutePathCreator extends ItemWithCreativeTabBase {
    public RoutePathCreator() {
        super(
            CreativeModeTabs.CORE, p -> p.stacksTo(1)
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        return InteractionResultHolder.success(itemStack);
    }

/*

CompoundTag {
    "nodes": (long[]) [...............],
    "fail_index": (int) ...,
    "route_name": (string) "..."
}

*/

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Block block = ctx.getLevel().getBlockState(pos).getBlock();
        if (block == null || !(block instanceof BlockNode)) return InteractionResult.FAIL;
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
        
        ItemStack itemStack = ctx.getItemInHand();
        CompoundTag compoundTag = itemStack.getOrCreateTag();
        List<BlockPos> nodes = readNodes(compoundTag);
        nodes.add(pos);
        int flag = verifyPath(nodes, ctx.getLevel());
        if (flag > 0 && ctx.getPlayer() != null) {
            ctx.getPlayer().displayClientMessage(Text.translatable(flag == nodes.size() ? 
                "gui.mtrsteamloco.rail_path_creator.error_current" : 
                "gui.mtrsteamloco.rail_path_creator.error_before", flag), true);
        }
        writeNodes(nodes, compoundTag);
        return InteractionResult.SUCCESS;
    }

    private static int verifyPath(List<BlockPos> nodes, Level world) {
        if (nodes.size() < 2) return -1;
        RailwayData data = RailwayData.getInstance(world);
        if (data == null) {
            Main.LOGGER.error("Railway data not found");
            return -2;
        }
        Map<BlockPos, Map<BlockPos, Rail>> railMap = ((RailwayDataAccessor) (Object) data).getRails();

        BlockPos lastPos = nodes.get(0);
        RailAngle lastAngle = null;
        Rail lastRail = null;
        boolean lastTurnBack = false;
        Map<BlockPos, Rail> subMap = railMap.get(lastPos);
        if (subMap == null) {
            return 0;
        }

        for (int i = 1; i < nodes.size(); i++) {
            BlockPos currentPos = nodes.get(i);
            Rail currentRail = subMap.get(currentPos);
            if (currentRail == null) return i;
            if (currentRail.railType == RailType.NONE) return i;
            if (lastAngle == null) {
                lastAngle = currentRail.facingEnd;
                
            } else {
                RailAngle currentAngle = currentRail.facingStart;
                if ((
                    lastRail.railType == RailType.TURN_BACK && currentRail.railType == RailType.TURN_BACK && 
                    ((RailExtraSupplier) (Object)lastRail).getPosStart().equals(((RailExtraSupplier) (Object) currentRail).getPosEnd()) && 
                    ((RailExtraSupplier) (Object)lastRail).getPosEnd().equals(((RailExtraSupplier) (Object)currentRail).getPosStart()))) {
                    if (lastTurnBack) {
                        return i;
                    } else {
                        lastTurnBack = true;
                    }
                } else {
                    lastTurnBack = false;
                    if (Math.abs(lastAngle.angleDegrees - currentAngle.angleDegrees) > 10) {
                        return i;
                    }
                }
                lastAngle = currentRail.facingEnd;
            }
            subMap = railMap.get(currentPos);
            if (subMap == null) return i;
            lastRail = currentRail;
            lastPos = currentPos;
        }

        return -1;
    }

    private static List<BlockPos> readNodes(CompoundTag compoundTag) {
        long[] array = compoundTag.getLongArray("nodes");
        List<BlockPos> nodes = new ArrayList<>();
        for (long l : array) {
            nodes.add(BlockPos.of(l));
        }
        return nodes;
    }

    private static void writeNodes(List<BlockPos> nodes, CompoundTag compoundTag) {
        long[] array = new long[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            array[i] = nodes.get(i).asLong();
        }
        compoundTag.putLongArray("nodes", array);
    }
}