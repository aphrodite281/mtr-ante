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
import mtr.data.Platform;
import mtr.data.Station;
import mtr.path.PathData;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import cn.zbx1425.mtrsteamloco.network.PacketScreen;
import net.minecraft.server.level.ServerPlayer;

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
        if (player instanceof ServerPlayer sp) PacketScreen.sendScreenS2C(sp, "route_path_creator");
        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
        CompoundTag tag = stack.getOrCreateTagElement("ANTE-Data");
        List<PathData> path = readPath(tag);
        if (path.isEmpty()) {
            list.add(Text.translatable("tooltip.mtrsteamloco.route_path_creator.empty"));
        } else {
            if (tag.contains("last_pos")) {
                BlockPos lastPos = BlockPos.of(tag.getLong("last_pos"));
                list.add(Text.literal("Last position: " + lastPos.toShortString()));
            }
            BlockPos lastPos = null;
            for (PathData pd : path) {
                BlockPos currentPos = pd.startingPos;
                if (lastPos == null) {
                    list.add(Text.literal("§8" + currentPos.toShortString()));//↕↑↓⇓⇑⇕
                } else if (currentPos.equals(lastPos)){
                    list.add(Text.literal("§8" + currentPos.toShortString()));
                } else {
                    list.add(Text.literal("§2" + lastPos.toShortString() + "§r -> §4" + currentPos.toShortString()));
                }
                list.add(Text.literal("Rail: " + pd.rail.railType.name() + " " + pd.rail.railType.speedLimit + "km/h " + String.format("%.1f", pd.rail.getLength()) + "m " + (pd.dwellTime * 0.5f) + "s"));
                lastPos = getPos(pd.rail, false);
            }
            if (lastPos != null) {
                list.add(Text.literal("§8" + lastPos.toShortString()));
            }
        }
    }

/*
CompoundTag 结构: 

CompoundTag {
    "ANTE-Data": {
        "path": (PathData[]) [.................]
        "last_pos": (long -> BlockPos) ......
        "route_name": (string) ....
        "route_color": (int) ....
    }
}
*/

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Block block = ctx.getLevel().getBlockState(pos).getBlock();
        if (block == null || !(block instanceof BlockNode)) return InteractionResult.FAIL;
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
        Player player = ctx.getPlayer();
        
        ItemStack itemStack = ctx.getItemInHand();
        CompoundTag compoundTag = itemStack.getOrCreateTagElement("ANTE-Data");
        
        RailwayData data = RailwayData.getInstance(ctx.getLevel());
        if (data == null) {
            if (player != null) player.displayClientMessage(Text.translatable("gui.mtrsteamloco.rail_path_creator.error.data_not_found"), true);
            return InteractionResult.SUCCESS;
        }
        Map<BlockPos, Map<BlockPos, Rail>> railMap = ((RailwayDataAccessor) (Object) data).getRails();

        if (compoundTag.contains("last_pos")) {
            BlockPos lastPos = BlockPos.of(compoundTag.getLong("last_pos"));
            List<PathData> path = readPath(compoundTag);
            Map<BlockPos, Rail> subMap = railMap.get(lastPos);
            if (subMap == null) {
                if (player != null) player.displayClientMessage(Text.translatable("gui.mtrsteamloco.rail_path_creator.error.no_rail"), true);
                return InteractionResult.SUCCESS;
            }
            Rail rail = subMap.get(pos);
            if (rail == null) {
                if (player != null) player.displayClientMessage(Text.translatable("gui.mtrsteamloco.rail_path_creator.error.no_rail"), true);
                return InteractionResult.SUCCESS;
            }
            if (rail.railType == RailType.NONE) {
                if (player != null) player.displayClientMessage(Text.translatable("gui.mtrsteamloco.rail_path_creator.error.one_way"), true);
                return InteractionResult.SUCCESS;
            }
            long pid = 0;
            int dwellTime = 0;
            for (Platform platform : data.platforms) {
                if (platform.containsPos(pos) && platform.containsPos(lastPos)) {
                    pid = platform.id;
                    dwellTime = platform.getDwellTime();
                }
            }
            if (path.size() == 0 && pid == 0) {
                if (player != null) player.displayClientMessage(Text.translatable("gui.mtrsteamloco.rail_path_creator.error.must_start_with_platform"), true);
                compoundTag.remove("last_pos");
                return InteractionResult.SUCCESS;
            }
            PathData pathData = new PathData(rail, pid, dwellTime, getPos(rail, true), getPos(rail, false), 0);
            path.add(pathData);
            
            boolean lastTurnBack = false;
            RailAngle lastAngle = null;
            for (int i = 0; i < path.size(); i++) {
                PathData pd = path.get(i);
                Rail rai = pd.rail;
                if (i >= 1) {
                    PathData prev = path.get(i - 1);
                    if (getPos(rai, true).equals(getPos(prev.rail, false)) && getPos(rai, false).equals(getPos(prev.rail, true))) {
                        if (rai.railType == RailType.TURN_BACK || rai.railType == RailType.PLATFORM) {
                            if (lastTurnBack) {
                                if (player != null) {
                                    player.displayClientMessage(Text.translatable("gui.mtrsteamloco.rail_path_creator.error.repeatedly_turn_back"), true);
                                }
                                return InteractionResult.SUCCESS;
                            } else {
                                if (i == 1) {
                                    if (player != null) {
                                        player.displayClientMessage(Text.translatable("gui.mtrsteamloco.rail_path_creator.error.cannot_turn_back_at_start"), true);
                                    }
                                    return InteractionResult.SUCCESS;
                                }
                                lastTurnBack = true;
                            }
                        } else {
                            if (player != null) {
                                player.displayClientMessage(Text.translatable("gui.mtrsteamloco.rail_path_creator.error.illegal_turn_back"), true);
                            }
                            return InteractionResult.SUCCESS;
                        }
                        
                    } else {
                        lastTurnBack = false;
                        if (Math.abs(lastAngle.angleDegrees - rai.facingStart.angleDegrees) < 135) {
                            if (player != null) {
                                player.displayClientMessage(Text.translatable("gui.mtrsteamloco.rail_path_creator.error.angle").append(Text.literal(i + " " + lastAngle.angleDegrees + "->" + rai.facingStart.angleDegrees)), true);
                            }
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
                lastAngle = rai.facingEnd;
            }
            if (player != null) player.displayClientMessage(Text.translatable("gui.mtrsteamloco.rail_path_creator.success.path", path.size()), true);
            writePath(path, compoundTag);
        } else {
            if (player != null) player.displayClientMessage(Text.translatable("gui.mtrsteamloco.rail_path_creator.success.pos"), true);
        }

        compoundTag.putLong("last_pos", pos.asLong());
        
        return InteractionResult.SUCCESS;
    }
    
    private static BlockPos getPos(Rail rail, boolean isStart) {
        RailExtraSupplier supplier = (RailExtraSupplier) (Object) rail;
        return isStart? supplier.getPosStart() : supplier.getPosEnd();
    }

    public static List<PathData> readPath(CompoundTag compoundTag) {
        ByteBuf buf0 = Unpooled.wrappedBuffer(compoundTag.getByteArray("path"));
        FriendlyByteBuf buf = new FriendlyByteBuf(buf0);
        List<PathData> path = new ArrayList<>();
        while (buf.isReadable()) {
            PathData data = new PathData(buf);
            path.add(data);
        }
        return path;
    }

    public static void writePath(List<PathData> path, CompoundTag compoundTag) {
        ByteBuf buf0 = Unpooled.buffer();
        FriendlyByteBuf buf = new FriendlyByteBuf(buf0);
        for (PathData data : path) {
            data.writePacket(buf);
        }
        byte[] bytes = new byte[buf0.readableBytes()];
        buf0.readBytes(bytes);
        compoundTag.putByteArray("path", bytes);
    }
}