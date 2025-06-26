package cn.zbx1425.mtrsteamloco.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.UseOnContext;
import mtr.item.ItemNodeModifierBase;
import mtr.data.Rail;
import mtr.data.RailType;
import net.minecraft.world.level.block.Block;
import mtr.data.TransportMode;
import mtr.block.BlockNode;
import cn.zbx1425.sowcer.math.Matrix4f;
import mtr.mappings.Text;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import mtr.packet.PacketTrainDataGuiServer;
import cn.zbx1425.sowcer.math.Vector3f;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import mtr.data.RailwayData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import cn.zbx1425.mtrsteamloco.data.RailActionsModuleExtraSupplier;
import net.minecraft.world.level.block.state.BlockState;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import cn.zbx1425.mtrsteamloco.network.util.IntegerArraySerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import cn.zbx1425.mtrsteamloco.network.PacketScreen;
import mtr.data.RailAngle;
import cn.zbx1425.mtrsteamloco.Main;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import cn.zbx1425.mtrsteamloco.render.rail.BakedRail;
import cn.zbx1425.mtrsteamloco.scripting.ScriptHolderBase;
import cn.zbx1425.mtrsteamloco.data.RailModelProperties;
import cn.zbx1425.mtrsteamloco.data.RailModelRegistry;

import java.util.*;
import java.io.*;

public class CompoundCreator extends ItemNodeModifierBase {
    private static final String TAG_TASKS = "tasks";

    public CompoundCreator() {
        super(true, false, false, true);
    }

    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            PacketScreen.sendScreenBlockS2C((ServerPlayer) player, "compound_creator", BlockPos.ZERO);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (clickCondition(ctx)) {
            super.useOn(ctx);
        } else {
            Player player = ctx.getPlayer();
            if (player instanceof ServerPlayer serverPlayer) {
                PacketScreen.sendScreenBlockS2C(serverPlayer, "compound_creator", BlockPos.ZERO);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(Level world, BlockPos start, BlockPos end, Player player, RailwayData data) {
        
    }

	@Override
	protected void onConnect(Level world, ItemStack stack, TransportMode transportMode, BlockState stateStart, BlockState stateEnd, BlockPos posStart, BlockPos posEnd, RailAngle facingStart, RailAngle facingEnd, Player player, RailwayData railwayData) {
        BlockPos tempPos = posStart;
        posStart = posEnd;
        posEnd = tempPos;
        RailAngle tempFacing = facingStart;
        facingStart = facingEnd;
        facingEnd = tempFacing;
        BlockState tempState = stateStart;
        stateStart = stateEnd;
        stateEnd = tempState;

        if (player == null) return;
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(TAG_TASKS)) {
            RailActionsModuleExtraSupplier acc = (RailActionsModuleExtraSupplier) (Object) railwayData.railwayDataRailActionsModule;
            List<Task> tasks = new ArrayList<>();
            CompoundTag tasksTag = tag.getCompound(TAG_TASKS);
            Set<String> keys = tasksTag.getAllKeys();
            for (String key : keys) {
                CompoundTag taskTag = tasksTag.getCompound(key);
                String type = taskTag.getString(Task.TAG_TYPE);
                if (type.equals(SliceTask.TYPE)) {
                    tasks.add(new SliceTask(taskTag));
                } else if (type.equals(RailModifierTask.TYPE)) {
                    tasks.add(new RailModifierTask(taskTag));
                } else {
                    player.displayClientMessage(Text.translatable("gui.mtrsteamloco.unknown_task_type", type), true);
                }
            }
            tasks.sort(Comparator.comparingInt(sliceTask -> sliceTask.order));
            int size = tasks.size();
            boolean changed = false;
            for (int i = 0; i < size; i++) {
                Task task = tasks.get(i);
                if (task instanceof SliceTask) {
                    if (railwayData.containsRail(posStart, posEnd)) {
                        acc.getRailActions().add(new SliceAction(acc.getWorld(), player, acc.getRails().get(posStart).get(posEnd), (SliceTask) tasks.get(i)));
                    } else {
                        player.displayClientMessage(Text.translatable("gui.mtr.rail_not_found_action"), true);
                    }
                } else if (task instanceof RailModifierTask) {
                    changed = true;
                    RailModifierTask task1 = (RailModifierTask) task;
                    boolean result = railModifier(world, stack, transportMode, stateStart, stateEnd, posStart, posEnd, facingStart, facingEnd, player, railwayData, task1.rail, task1.isOneWay, task1.isReversed);
                    Main.LOGGER.info("RailModifierTask result: " + result);
                } else {
                    Main.LOGGER.error("Unknown task type: " + task.name);
                }
            }
            acc.sendUpdateS2C();
        } else {
            player.displayClientMessage(Text.translatable("gui.mtrsteamloco.no_tasks_found"), true);
        }
	}

    private static boolean railModifier(Level world, ItemStack stack, TransportMode transportMode, BlockState stateStart, BlockState stateEnd, BlockPos posStart, BlockPos posEnd, RailAngle facingStart, RailAngle facingEnd, Player player, RailwayData railwayData, Rail baseRail, boolean isOneWay, boolean isReversed) {

        RailType railType = baseRail.railType;
        if (railType.hasSavedRail && (railwayData.hasSavedRail(posStart) || railwayData.hasSavedRail(posEnd))) {
			if (player != null) {
				player.displayClientMessage(Text.translatable("gui.mtr.platform_or_siding_exists"), true);
			}
		} else {
			boolean isValidContinuousMovement;
			final RailType newRailType;
			if (transportMode.continuousMovement) {
				final Block blockStart = stateStart.getBlock();
				final Block blockEnd = stateEnd.getBlock();

				if (blockStart instanceof BlockNode.BlockContinuousMovementNode && blockEnd instanceof BlockNode.BlockContinuousMovementNode) {
					if (((BlockNode.BlockContinuousMovementNode) blockStart).isStation && ((BlockNode.BlockContinuousMovementNode) blockEnd).isStation) {
						isValidContinuousMovement = true;
						newRailType = railType.hasSavedRail ? railType : RailType.CABLE_CAR_STATION;
					} else {
						final int differenceX = posEnd.getX() - posStart.getX();
						final int differenceZ = posEnd.getZ() - posStart.getZ();
						isValidContinuousMovement = !railType.hasSavedRail && facingStart.isParallel(facingEnd)
								&& ((facingStart == RailAngle.N || facingStart == RailAngle.S) && differenceX == 0
								|| (facingStart == RailAngle.E || facingStart == RailAngle.W) && differenceZ == 0
								|| (facingStart == RailAngle.NE || facingStart == RailAngle.SW) && differenceX == -differenceZ
								|| (facingStart == RailAngle.SE || facingStart == RailAngle.NW) && differenceX == differenceZ);
						newRailType = RailType.CABLE_CAR;
					}
				} else {
					isValidContinuousMovement = false;
					newRailType = railType;
				}
			} else {
				isValidContinuousMovement = true;
				newRailType = railType;
			}

            RailType p1, p2;
            if (isReversed) {
                p2 = newRailType;
                p1 = isOneWay ? RailType.NONE : newRailType;
            } else {
                p1 = newRailType;
                p2 = isOneWay ? RailType.NONE : newRailType;
            }
			final Rail rail1 = new Rail(posStart, facingStart, posEnd, facingEnd, p1, transportMode);
			final Rail rail2 = new Rail(posEnd, facingEnd, posStart, facingStart, p2, transportMode);

			final boolean goodRadius = rail1.goodRadius() && rail2.goodRadius();
			final boolean isValid = rail1.isValid() && rail2.isValid();

            // Main.LOGGER.info("*****" + isValid + " " + isValidContinuousMovement + " " + goodRadius);

			if (goodRadius && isValid && isValidContinuousMovement) {
                RailExtraSupplier rail1Extra = (RailExtraSupplier) (Object) rail1;
                RailExtraSupplier rail2Extra = (RailExtraSupplier) (Object) rail2;
                rail1Extra.partialCopyFrom(baseRail);
                rail2Extra.partialCopyFrom(baseRail);
                rail1Extra.setRenderReversed(isReversed ? false : true);
                rail2Extra.setRenderReversed(isReversed ? true : false);

				railwayData.addRail(player, transportMode, posStart, posEnd, rail1, false);
				final long newId = railwayData.addRail(player, transportMode, posEnd, posStart, rail2, true);
				world.setBlockAndUpdate(posStart, stateStart.setValue(BlockNode.IS_CONNECTED, true));
				world.setBlockAndUpdate(posEnd, stateEnd.setValue(BlockNode.IS_CONNECTED, true));
				PacketTrainDataGuiServer.createRailS2C(world, transportMode, posStart, posEnd, rail1, rail2, newId);
                return true;
			} else if (player != null) {
				player.displayClientMessage(Text.translatable(isValidContinuousMovement ? goodRadius ? "gui.mtr.invalid_orientation" : "gui.mtr.radius_too_small" : "gui.mtr.cable_car_invalid_orientation"), true);
			}
            return false;
		}
        return false;
    }

    public static class Task {
        public static final String TAG_TYPE = "type";
        public static final String TAG_ORDER = "order";
        public static final String TAG_NAME = "name";

        public int order;
        public String name;

        public Task(int order, String name) {
            this.order = order;
            this.name = name;
        }

        public Task(CompoundTag compoundTag) {
            order = compoundTag.getInt(TAG_ORDER);
            name = compoundTag.getString(TAG_NAME);
        }

        public Task(Task other) {
            order = other.order;
            name = other.name;
        }

        public void copyFrom(Task other) {
            order = other.order;
            name = other.name;
        }

        public CompoundTag toCompoundTag() {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putInt(TAG_ORDER, order);
            compoundTag.putString(TAG_NAME, name);
            return compoundTag;
        }
    }

    public static class SliceTask extends Task {
        public static final String TYPE = "Slice";

        public int width;
        public int height;
        public double start;
        public Double length;
        public Double interval;
        public double increment;
        public List<Lump> lumps;
        public boolean useYaw;
        public boolean usePitch;
        public boolean useRoll;

        public static final String TAG_HEIGHT = "height";
        public static final String TAG_LENGTH = "length";
        public static final String TAG_START = "start";
        public static final String TAG_WIDTH = "width";
        public static final String TAG_INTERVAL = "interval";
        public static final String TAG_INCREMENT = "increment";
        public static final String TAG_LUMPS = "lumps";
        public static final String TAG_USE_YAW = "use_yaw";
        public static final String TAG_USE_PITCH = "use_pitch";
        public static final String TAG_USE_ROLL = "use_roll";

        public SliceTask() {
            super(0, TYPE);
            this.width = 11;
            this.height = 11;
            this.start = 0;
            this.length = null;
            this.interval = null;
            this.increment = 0.1;
            this.lumps = new ArrayList<>();
            for (int i = 0; i < this.width * this.height; i++) {
                lumps.add(new Lump(null, true));
            }
            this.useYaw = true;
            this.usePitch = true;
            this.useRoll = true;
        }

        public SliceTask(SliceTask other) {
            super(other.order, other.name);
            this.width = other.width;
            this.height = other.height;
            this.start = other.start;
            this.length = other.length;
            this.interval = other.interval;
            this.increment = other.increment;
            this.lumps = Lump.copyFrom(other.lumps);
            this.useYaw = other.useYaw;
            this.usePitch = other.usePitch;
            this.useRoll = other.useRoll;
        }

        public SliceTask(int order, String name, int width, int height, double start, Double length, Double interval, double increment, List<Lump> lumps, boolean useYaw, boolean usePitch, boolean useRoll) {
            super(order, name);
            this.width = width;
            this.height = height;
            this.start = start;
            this.length = length;
            this.interval = interval;
            this.increment = increment;
            this.lumps = lumps;
            this.useYaw = useYaw;
            this.usePitch = usePitch;
            this.useRoll = useRoll;
        }

        public SliceTask(CompoundTag compoundTag) {
            super(compoundTag);
            this.width = compoundTag.getInt(TAG_WIDTH);
            this.height = compoundTag.getInt(TAG_HEIGHT);
            this.start = compoundTag.getDouble(TAG_START);
            if (compoundTag.contains(TAG_LENGTH)) {
                this.length = compoundTag.getDouble(TAG_LENGTH);
            } else {
                this.length = null;
            }
            this.increment = compoundTag.getDouble(TAG_INCREMENT);
            if (compoundTag.contains(TAG_INTERVAL)) {
                this.interval = compoundTag.getDouble(TAG_INTERVAL);
            } else {
                this.interval = null;
            }
            this.lumps = Lump.fromByteArray(compoundTag.getByteArray(TAG_LUMPS));
            this.useYaw = compoundTag.getBoolean(TAG_USE_YAW);
            this.usePitch = compoundTag.getBoolean(TAG_USE_PITCH);
            this.useRoll = compoundTag.getBoolean(TAG_USE_ROLL);
        }

        public boolean setWidthAndHeight(int width, int height) {
            if (width < 1 || height < 1) return false;
            if (width % 2 != 1 || height % 2 != 1) return false;
            if (width == this.width && height == this.height) return false;
            
            List<Lump> lumps = new ArrayList<>();
            for (int i = 0; i < width * height; i++) lumps.add(new Lump(null, true));
            int thiMidX = width / 2;
            int thiMidY = height / 2;
            int oldMidX = this.width / 2;
            int oldMidY = this.height / 2;
            
            for (int i = -thiMidY; i <= thiMidY; i++) {
                int thiy = thiMidY + i;
                int oldy = oldMidY + i;
                if (oldy < 0 || oldy >= this.height) continue;
                for (int j = -thiMidX; j <= thiMidX; j++) {
                    int thix = thiMidX + j;
                    int oldx = oldMidX + j;
                    if (oldx < 0 || oldx >= this.width) continue;
                    // blockIds[thiy * width + thix] = this.blockIds[oldy * this.width + oldx];
                    lumps.set(thiy * width + thix, this.lumps.get(oldy * this.width + oldx));
                }
            }
            this.width = width;
            this.height = height;
            this.lumps = lumps;
            return true;
        }

        public void copyFrom(SliceTask other) {
            super.copyFrom(other);
            width = other.width;
            height = other.height;
            start = other.start;
            length = other.length;
            interval = other.interval;
            increment = other.increment;
            lumps = Lump.copyFrom(other.lumps);
            useYaw = other.useYaw;
            usePitch = other.usePitch;
            useRoll = other.useRoll;
        }

        public CompoundTag toCompoundTag() {
            CompoundTag compoundTag = super.toCompoundTag();
            compoundTag.putString(TAG_TYPE, TYPE);
            compoundTag.putInt(TAG_WIDTH, width);
            compoundTag.putInt(TAG_HEIGHT, height);
            compoundTag.putDouble(TAG_START, start);
            if (length != null) {
                compoundTag.putDouble(TAG_LENGTH, length);
            }
            compoundTag.putDouble(TAG_INCREMENT, increment);
            if (interval!= null) {
                compoundTag.putDouble(TAG_INTERVAL, interval);
            }
            compoundTag.putByteArray(TAG_LUMPS, Lump.toByteArray(lumps));
            compoundTag.putBoolean(TAG_USE_YAW, useYaw);
            compoundTag.putBoolean(TAG_USE_PITCH, usePitch);
            compoundTag.putBoolean(TAG_USE_ROLL, useRoll);
            return compoundTag;
        }
    }

    public static class Lump {
        public BlockState blockState;
        public boolean replacement;
        public Lump(Lump other) {
            this(other.blockState, other.replacement);
        }
        public Lump(BlockState blockState, boolean replacement) {
            this.blockState = blockState;
            this.replacement = replacement;
        }
        public static byte[] toByteArray(List<Lump> lumps) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            try {
                dos.writeInt(lumps.size());
                for (Lump lump : lumps) {
                    boolean b = lump.blockState != null;
                    dos.writeBoolean(b);
                    if (b) dos.writeInt(Block.getId(lump.blockState));
                    dos.writeBoolean(lump.replacement);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bos.toByteArray();
        }
        public static List<Lump> fromByteArray(byte[] bytes) {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(bis);
            List<Lump> lumps = new ArrayList<>();
            try {
                int size = dis.readInt();
                for (int i = 0; i < size; i++) {
                    boolean hasBlockState = dis.readBoolean();
                    BlockState blockState = null;
                    if (hasBlockState) blockState = Block.stateById(dis.readInt());
                    boolean replacement = dis.readBoolean();
                    lumps.add(new Lump(blockState, replacement));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            return lumps;
        }

            public static List<Lump> copyFrom(List<Lump> lumps) {
                List<Lump> result = new ArrayList<>();
                for (Lump lump : lumps) {
                    result.add(new Lump(lump));
                }
                return result;
            }
        }

    public static class RailModifierTask extends Task {
        public static final String TYPE = "Modifier";

        public Rail rail;
        public boolean isOneWay;
        public boolean isReversed;

        public static final String TAG_NAME = "name";
        public static final String TAG_RAIL = "rail";
        public static final String TAG_IS_ONE_WAY = "is_one_way";
        public static final String TAG_IS_REVERSED = "is_reversed";

        public RailModifierTask() {
            super(0, TYPE);
            this.rail = new Rail(new BlockPos(0, -1145, 0), RailAngle.N, new BlockPos(0, -1145, 10), RailAngle.S, RailType.IRON, TransportMode.TRAIN);
            this.isOneWay = false;
            this.isReversed = false;
            tryCallRailScript();
        }

        public RailModifierTask(int order, String name, Rail rail, boolean isOneWay, boolean isReversed) {
            super(order, name);
            this.rail = rail;
            this.isOneWay = isOneWay;
            this.isReversed = isReversed;
            tryCallRailScript();
        }

        public RailModifierTask(CompoundTag compoundTag) {
            super(compoundTag);
            ByteBuf buf = Unpooled.wrappedBuffer(compoundTag.getByteArray(TAG_RAIL));
            this.rail = new Rail(new FriendlyByteBuf(buf));
            buf.release();
            this.isOneWay = compoundTag.getBoolean(TAG_IS_ONE_WAY);
            this.isReversed = compoundTag.getBoolean(TAG_IS_REVERSED);
            tryCallRailScript();
        }

        public RailModifierTask(RailModifierTask other) {
            super(other.order, other.name);
            
            ByteBuf buf = Unpooled.buffer();
            FriendlyByteBuf buf2 = new FriendlyByteBuf(buf);
            other.rail.writePacket(buf2);
            this.rail = new Rail(buf2);
            buf.release();
            this.isOneWay = other.isOneWay;
            this.isReversed = other.isReversed;
        }

        public void tryCallRailScript() {
            RailModelProperties prop = RailModelRegistry.getProperty(((RailExtraSupplier) rail).getModelKey());
            if (prop == null) return;
            if (prop.script == null) return;
            // 有空再做
        }

        public void copyFrom(RailModifierTask other) {
            super.copyFrom(other);
            ByteBuf buf = Unpooled.buffer();
            FriendlyByteBuf buf2 = new FriendlyByteBuf(buf);
            other.rail.writePacket(buf2);
            this.rail = new Rail(buf2);
            buf.release();
            isOneWay = other.isOneWay;
            isReversed = other.isReversed;
        }

        public CompoundTag toCompoundTag() {
            CompoundTag compoundTag = super.toCompoundTag();
            compoundTag.putString(TAG_TYPE, TYPE);
            ByteBuf buf = Unpooled.buffer();
            rail.writePacket(new FriendlyByteBuf(buf));
            compoundTag.putByteArray(TAG_RAIL, ByteBufUtil.getBytes(buf));
            buf.release();
            compoundTag.putBoolean(TAG_IS_ONE_WAY, isOneWay);
            compoundTag.putBoolean(TAG_IS_REVERSED, isReversed);
            return compoundTag;
        }
    }

    public static class SliceAction extends Rail.RailActions {
        public final SliceTask task;
        public double[] starts;
        public int index;
        private int width;
        private int height;
        private double distance;
        private final Level world;
		private final UUID uuid;
		private final String playerName;
		private final Rail rail;
		private final double length;
		private final Set<BlockPos> blacklistedPos = new HashSet<>();
        private double distanceSqr;
        private Vec3 last;

        private final double INCREMENT;

        public SliceAction(Level world, Player player, Rail rail, SliceTask task) {
            super(world, player, null, rail, 0, 0, null);
			this.world = world;
			uuid = player.getUUID();
			playerName = player.getName().getString();
			this.rail = rail;
            this.task = task;
            index = 0;
            length = rail.getLength();
            width = task.width;
            height = task.height;
            INCREMENT = task.increment < 0.1D ? 0.1D : task.increment;
            distanceSqr = INCREMENT * INCREMENT;

            if (task.interval == null || task.length == null) {
                starts = new double[]{task.start};
            } else {
                double temp = task.start;
                double count = task.length + task.interval;
                List<Double> list = new ArrayList<>();
                while (temp + count < length) {
                    list.add(temp);
                    temp += count;
                }
                starts = list.stream().mapToDouble(Double::doubleValue).toArray();
            }
            distance = starts[0];
            last = rail.getPosition(starts[0]);
        }

        @Override
        public boolean build() {
            final long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 2) {
                if (index >= starts.length) {
                    showProgressMessage(100);
                    return true;
                }
                
                if (distance >= starts[index] + (task.length == null ? length : task.length)) {
                    index++;
                    if (index >= starts.length) {
                        showProgressMessage(100);
                        return true;
                    }
                    distance = starts[index];
                    last = rail.getPosition(starts[index]);
                    continue;
                }
                distance += INCREMENT;
                if (distance >= (length - 0.1)) {
                    showProgressMessage(100);
                    return true;
                }
                Vec3 next = rail.getPosition(distance);
                // Vec3 center = new Vec3((last.x + next.x) / 2, (last.y + next.y) / 2, (last.z + next.z) / 2);

                Matrix4f mat = new Matrix4f();
                mat.translate((float) last.x, (float) last.y, (float) last.z);
                if (task.useYaw) {
                    final float yaw = (float) Mth.atan2(next.x - last.x, next.z - last.z);
                    mat.rotateY(yaw);
                }
                if (task.usePitch) {
                    final float pitch = (float) Mth.atan2(next.y - last.y, (float) Math.sqrt((next.x - last.x) * (next.x - last.x) + (next.z - last.z) * (next.z - last.z)));
                    mat.rotateX(pitch);
                }
                if (task.useRoll) {
                    final float roll = RailExtraSupplier.getRollAngle(rail, distance - INCREMENT / 2);
                    mat.rotateZ(roll);
                }

                last = next;

                mat.translate(width / 2.0F - 0.5F, height / 2.0F - 0.5F, 0);

                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        int index = i * width + j;
                        Lump lump = task.lumps.get(index);
                        BlockState state = lump.blockState;
                        Vector3f pos = mat.getTranslationPart();
                        mat.translate(-1.0F, 0, 0);
                        BlockPos blockPos = new BlockPos((int) Math.floor(pos.x()), (int) Math.floor(pos.y()), (int) Math.floor(pos.z()));
                        if (!world.getBlockState(blockPos).isAir() && !lump.replacement) continue;
                        if (state == null) continue;
                        if (blacklistedPos.contains(blockPos)) continue;
                        if (!canPlace(world,blockPos)) continue;
                            
                        if (state.hasProperty(BlockStateProperties.FACING)) {
                            Direction dir = rotateDirection(state.getValue(BlockStateProperties.FACING), mat);
                            state = state.setValue(BlockStateProperties.FACING, dir);
                        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                            Direction dir = rotateDirection(state.getValue(BlockStateProperties.HORIZONTAL_FACING), mat);
                            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, dir);
                        } else if (state.hasProperty(BlockStateProperties.FACING_HOPPER)) {
                            Direction dir = rotateDirection(state.getValue(BlockStateProperties.FACING_HOPPER), mat);
                            state = state.setValue(BlockStateProperties.FACING_HOPPER, dir);
                        }
                        world.setBlockAndUpdate(blockPos, state);
                        blacklistedPos.add(blockPos);
                    }
                    mat.translate(width, -1.0F, 0);
                }
            }

            showProgressMessage(RailwayData.round(100 * distance / length, 1));
            return false;
        }

        private Direction rotateDirection(Direction dir, Matrix4f mat) {
            if (dir == Direction.UP || Direction.DOWN == dir) return dir;
            double d = dir.toYRot() + mat.getEulerAnglesYXZ().y() / Math.PI * 180 + 180;
            return Direction.fromYRot(d);
        }

        private void showProgressMessage(float percentage) {
            final Player player = world.getPlayerByUUID(uuid);
			if (player != null) {
				player.displayClientMessage(Text.translatable("gui.mtr.percentage_complete_slice", percentage), true);
			}
        }

        private static boolean canPlace(Level world, BlockPos pos) {
			return world.getBlockEntity(pos) == null && !(world.getBlockState(pos).getBlock() instanceof BlockNode);
		}

        @Override
        public void writePacket(FriendlyByteBuf packet) {
			packet.writeLong(id);
			packet.writeUtf(playerName);
			packet.writeFloat(RailwayData.round(length, 1));
			packet.writeUtf("percentage_complete_slice");
			packet.writeUtf("rail_action_slice");
			packet.writeInt(0x47fec4);
		}
    }
}