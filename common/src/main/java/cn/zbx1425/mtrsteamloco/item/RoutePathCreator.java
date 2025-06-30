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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.TooltipFlag;

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

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
        CompoundTag tag = stack.getOrCreateTag();
        List<BlockPos> nodes = readNodes(tag);
        if (nodes.isEmpty()) {
            list.add(Text.translatable("tooltip.mtrsteamloco.route_path_creator.empty"));
        } else {
            Message message = Message.load(tag);
            list.add(Text.translatable(message.content, message.index));
            boolean inError = false;
            for (int i = 0; i < nodes.size(); i++) {
                boolean currentError = i == message.index;
                inError = inError || currentError;
                list.add(Text.literal("-> (" + nodes.get(i).toShortString() + ')').withStyle(Style.EMPTY.withColor(inError ? (currentError ? 0xff0000 : 0xffff00): 0xffffff)));
            }
        }
    }

/*

CompoundTag {
    "nodes": (long[]) [...............],
    "message": {
        "index": (int) ...,
        (可能没有) "content": (string) ... 
    }
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
        Message res = verifyPath(nodes, ctx.getLevel());
        if (res.index >= 0 && ctx.getPlayer() != null) {
            ctx.getPlayer().displayClientMessage(Text.translatable("gui.mtrsteamloco.rail_path_creator.message.now", nodes.size()).append(Text.translatable(res.content, res.index)), true);
        }
        writeNodes(nodes, compoundTag);
        res.save(compoundTag);
        return InteractionResult.SUCCESS;
    }

    private static Message verifyPath(List<BlockPos> nodes, Level world) {
        if (nodes.size() < 2) return Message.CORRECT;
        RailwayData data = RailwayData.getInstance(world);
        if (data == null) {
            return Message.DataNotFound;
        }
        Map<BlockPos, Map<BlockPos, Rail>> railMap = ((RailwayDataAccessor) (Object) data).getRails();

        BlockPos lastPos = nodes.get(0);
        RailAngle lastAngle = null;
        Rail lastRail = null;
        boolean lastTurnBack = false;
        Map<BlockPos, Rail> subMap = railMap.get(lastPos);
        if (subMap == null) {
            return new Message(0, "gui.mtrsteamloco.rail_path_creator.error.no_rail");
        }

        for (int i = 1; i < nodes.size(); i++) {
            BlockPos currentPos = nodes.get(i);
            Rail currentRail = subMap.get(currentPos);
            if (currentRail == null) return new Message(i, "gui.mtrsteamloco.rail_path_creator.error.no_rail");
            if (currentRail.railType == RailType.NONE) return new Message(i, "gui.mtrsteamloco.rail_path_creator.error.one_way");
            if (lastAngle == null) {
                lastAngle = currentRail.facingEnd;
            } else {
                RailAngle currentAngle = currentRail.facingStart;
                if (( 
                    ((RailExtraSupplier) (Object)lastRail).getPosStart().equals(((RailExtraSupplier) (Object) currentRail).getPosEnd()) && 
                    ((RailExtraSupplier) (Object)lastRail).getPosEnd().equals(((RailExtraSupplier) (Object)currentRail).getPosStart()))) {
                    if (lastRail.railType == RailType.TURN_BACK && currentRail.railType == RailType.TURN_BACK) {
                        if (lastTurnBack) {
                            return new Message(i, "gui.mtrsteamloco.rail_path_creator.error.repeatedly_turn_back");
                        } else {
                            lastTurnBack = true;
                        }
                    } else {
                        return new Message(i, "gui.mtrsteamloco.rail_path_creator.error.illegal_turn_back");
                    }
                } else {
                    lastTurnBack = false;
                    if (Math.abs(lastAngle.angleDegrees - currentAngle.angleDegrees) > 10) {
                        return new Message(i, "gui.mtrsteamloco.rail_path_creator.error.angle");
                    }
                }
                lastAngle = currentRail.facingEnd;
            }
            subMap = railMap.get(currentPos);
            if (subMap == null) return new Message(i, "gui.mtrsteamloco.rail_path_creator.error.no_rail");
            lastRail = currentRail;
            lastPos = currentPos;
        }

        return Message.CORRECT;
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

    public static class Message {
        public static final Message CORRECT = new Message(-1, "gui.mtrsteamloco.rail_path_creator.message.success");
        public static final Message DataNotFound = new Message(-2, "gui.mtrsteamloco.rail_path_creator.error.data_not_found");

        public final int index;
        public final String content;

        public Message(int index, String content) {
            this.index = index;
            this.content = content;
        }

        public void save(CompoundTag tag) {
            CompoundTag sub = new CompoundTag();
            sub.putInt("index", index);
            if (content != null) sub.putString("content", content);
            tag.put("message", sub);
        }

        public static Message load(CompoundTag tag) {
            CompoundTag sub = tag.getCompound("message");
            int index = sub.getInt("index");
            if (index == -1) return CORRECT;
            if (index == -2) return DataNotFound;
            String content = "";
            if (sub.contains("content")) content = sub.getString("content");
            return new Message(index, content);
        }
    }
}