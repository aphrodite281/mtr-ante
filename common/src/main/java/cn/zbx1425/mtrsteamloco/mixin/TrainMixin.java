package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.Main;
import mtr.block.BlockPSDAPGBase;
import mtr.block.BlockPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy;
import net.minecraft.world.phys.Vec3;
import mtr.render.TrainRendererBase;
import net.minecraft.util.Mth;
import mtr.path.PathData;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.mtrsteamloco.data.TrainExtraSupplier;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import org.msgpack.core.MessagePacker;
import cn.zbx1425.mtrsteamloco.network.util.StringMapSerializer;
import org.msgpack.value.Value;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.data.ConfigResponder;
import mtr.data.*;
import static mtr.data.Train.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Train.class)
public abstract class TrainMixin implements TrainExtraSupplier{

	private int doorDelay = 20;

	@Shadow(remap = false) protected float speed;
	@Shadow(remap = false) protected double railProgress;
	@Shadow(remap = false) protected boolean doorTarget;
	@Shadow(remap = false) protected float doorValue;
	@Shadow(remap = false) protected float elapsedDwellTicks;
	@Shadow(remap = false) protected int nextStoppingIndex;
	@Shadow(remap = false) protected int nextPlatformIndex;
	@Shadow(remap = false) protected boolean reversed;
	@Shadow(remap = false) protected boolean isOnRoute;
	@Shadow(remap = false) protected boolean isCurrentlyManual;
	@Shadow(remap = false) protected int manualNotch;

	@Shadow(remap = false) private long sidingId;
	@Shadow(remap = false) private String trainId;
	@Shadow(remap = false) private String baseTrainType;
	@Shadow(remap = false) private TransportMode transportMode;
	@Shadow(remap = false) private int spacing;
	@Shadow(remap = false) private int width;
	@Shadow(remap = false) private int trainCars;
	@Shadow(remap = false) private float accelerationConstant;
	@Shadow(remap = false) private boolean isManualAllowed;
	@Shadow(remap = false) private int maxManualSpeed;
	@Shadow(remap = false) private int manualToAutomaticTime;
	@Shadow(remap = false) private List<PathData> path;

	@Shadow(remap = false) protected List<Double> distances;
	@Shadow(remap = false) protected int repeatIndex1;
	@Shadow(remap = false) protected int repeatIndex2;
	@Shadow(remap = false) protected Set<UUID> ridingEntities = new HashSet<>();
	@Shadow(remap = false) protected SimpleContainer inventory;

	@Shadow(remap = false) private float railLength;

    private Map<String, String> customConfigs = new HashMap<>();
	private Map<String, ConfigResponder> configResponders = new HashMap<>();
	private boolean isConfigsChanged = false;
	
	@Shadow(remap = false) protected abstract boolean handlePositions(Level world, Vec3[] positions, float ticksElapsed);
	@Shadow(remap = false) protected abstract boolean canDeploy(Depot depot);
	@Shadow(remap = false) protected abstract boolean isRailBlocked(int checkIndex);
	@Shadow(remap = false) protected abstract boolean skipScanBlocks(Level world, double trainX, double trainY, double trainZ);
	@Shadow(remap = false) protected abstract boolean openDoors(Level world, Block block, BlockPos checkPos, int dwellTicks);
	@Shadow(remap = false) protected abstract boolean openDoors();
	@Shadow(remap = false) protected abstract double asin(double value);
	@Shadow(remap = false) protected abstract int getIndex(int car, int trainSpacing, boolean roundDown);
	@Shadow(remap = false) protected abstract int getIndex(double tempRailProgress, boolean roundDown);
	@Shadow(remap = false) protected abstract int getTotalDwellTicks();
	@Shadow(remap = false) protected abstract boolean isOppositeRail();
	@Shadow(remap = false) protected abstract boolean isRepeat();
	@Shadow(remap = false) protected abstract void startUp(Level world, int trainCars, int trainSpacing, boolean isOppositeRail);
	@Shadow(remap = false) protected abstract float getRailSpeed(int railIndex);
	@Shadow(remap = false) protected abstract Vec3 getRoutePosition(int car, int trainSpacing);
	@Shadow(remap = false) protected abstract boolean scanDoors(Level world, double trainX, double trainY, double trainZ, float checkYaw, float pitch, double halfSpacing, int dwellTicks);
	protected void _calculateCar(Level world, Vec3[] positions, int index, int dwellTicks, CCB calculateCarCallback) {
		final Vec3 pos1 = positions[index];
		final Vec3 pos2 = positions[index + 1];

		if (pos1 != null && pos2 != null) {
			final double x = getAverage0(pos1.x, pos2.x);
			final double y = getAverage0(pos1.y, pos2.y) + 1;
			final double z = getAverage0(pos1.z, pos2.z);

			final double realSpacing = pos2.distanceTo(pos1);
			final float yaw = (float) Mth.atan2(pos2.x - pos1.x, pos2.z - pos1.z);
			final float pitch = realSpacing == 0 ? 0 : (float) asin((pos2.y - pos1.y) / realSpacing);
			final boolean doorLeftOpen = scanDoors(world, x, y, z, (float) Math.PI + yaw, pitch, realSpacing / 2, dwellTicks) && doorValue > 0;
			final boolean doorRightOpen = scanDoors(world, x, y, z, yaw, pitch, realSpacing / 2, dwellTicks) && doorValue > 0;

			calculateCarCallback.calculateCarCallback(x, y, z, yaw, pitch, realSpacing, doorLeftOpen, doorRightOpen);
		}
	}
	@Shadow(remap = false)
	protected abstract void simulateCar(
			Level world, int ridingCar, float ticksElapsed,
			double carX, double carY, double carZ, float carYaw, float carPitch,
			double prevCarX, double prevCarY, double prevCarZ, float prevCarYaw, float prevCarPitch,
			boolean doorLeftOpen, boolean doorRightOpen, double realSpacing
	);

	private static double getAverage0(double a, double b) {
		return (a + b) / 2;
	}

	@FunctionalInterface
	protected interface CCB {
		void calculateCarCallback(double x, double y, double z, float yaw, float pitch, double realSpacing, boolean doorLeftOpen, boolean doorRightOpen);
	}

	@Override
	public Map<String, String> getCustomConfigs() {
		return customConfigs;
	}

	@Override
	public void setCustomConfigs(Map<String, String> customConfigs) {
		this.customConfigs = customConfigs;
	}

	@Override
	public boolean isConfigsChanged() {
		return isConfigsChanged;
	}

	@Override
	public void isConfigsChanged(boolean isConfigsChanged) {
		this.isConfigsChanged = isConfigsChanged;
	}

	@Override
	public void setConfigResponders(Map<String, ConfigResponder> configResponders) {
		this.configResponders = configResponders;
	}

	@Override
	public Map<String, ConfigResponder> getConfigResponders() {
		return configResponders;
	}

	@Override
	public float getRollAngleAt(double value) {
		int i = getIndex(value, true);
		if (i != 0) value -= distances.get(i - 1);
        Rail r = path.get(i).rail;
        float rot = RailExtraSupplier.getRollAngle(r, value);
		return rot;
	}

	@Inject(method = "<init>(JFLjava/util/List;Ljava/util/List;IIFZIILjava/util/Map;)V", at = @At("TAIL"), remap = false)
	private void fromMassagePack(
			long sidingId, float railLength,
			List<PathData> path, List<Double> distances, int repeatIndex1, int repeatIndex2,
			float accelerationConstant, boolean isManualAllowed, int maxManualSpeed, int manualToAutomaticTime,
			Map<String, Value> map, CallbackInfo ci
	) {
		MessagePackHelper messagePackHelper = new MessagePackHelper(map);
		try {
			customConfigs = StringMapSerializer.deserialize(messagePackHelper.getString("custom_configs"));
		} catch (IOException e) {
			customConfigs = new HashMap<>();
		}
	}

	@Inject(method = "<init>(JFLjava/util/List;Ljava/util/List;IIFZIILnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
	private void fromCompoundTag(
			long sidingId, float railLength,
			List<PathData> path, List<Double> distances, int repeatIndex1, int repeatIndex2,
			float accelerationConstant, boolean isManualAllowed, int maxManualSpeed, int manualToAutomaticTime,
			CompoundTag compoundTag, CallbackInfo ci
	) {
		try {
			customConfigs = StringMapSerializer.deserialize(compoundTag.getString("custom_configs"));
		} catch (IOException e) {
			customConfigs = new HashMap<>();
		}
	}

	@Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"))
	private void fromFriendlyByteBuf(FriendlyByteBuf buffer, CallbackInfo ci) {
		try {
			customConfigs = StringMapSerializer.deserialize(buffer.readUtf());
		} catch (IOException e) {
			customConfigs = new HashMap<>();
		}
	}

	@Inject(method = "toMessagePack", at = @At("TAIL"), remap = false)
    private void toMessagePack(MessagePacker messagePacker, CallbackInfo ci) throws IOException {
		String res;
		try {
			res = StringMapSerializer.serializeToString(customConfigs);
		} catch (IOException e) {
			res = "";
		}
		messagePacker.packString("custom_configs").packString(res);
	}

    @Inject(method = "messagePackLength", at = @At("TAIL"), cancellable = true, remap = false)
    private void messagePackLength(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(cir.getReturnValue() + 1);
    }

	@Inject(method = "writePacket", at = @At("TAIL"))
    private void toPacket(FriendlyByteBuf packet, CallbackInfo ci) {
		String res;
		try {
			res = StringMapSerializer.serializeToString(customConfigs);
		} catch (IOException e) {
			res = "";
		}
		packet.writeUtf(res);
	}

	private static Class<?> IBlockPlatformClass = Void.class;

	static {
		try {
			IBlockPlatformClass = Class.forName("team.dovecotmc.metropolis.block.interfaces.IBlockPlatform");
			Main.LOGGER.info("Loaded metropolis IBlockPlatformClass");
		} catch (ClassNotFoundException e) {
			Main.LOGGER.error("Failed to load metropolis IBlockPlatformClass");
		}
	}

    @Inject(method = "scanDoors", at = @At("HEAD"), cancellable = true)
    private void onScanDoors(Level world, double trainX, double trainY, double trainZ, float checkYaw, float pitch, double halfSpacing, int dwellTicks, CallbackInfoReturnable<Boolean> ci) {
        if (skipScanBlocks(world, trainX, trainY, trainZ)) {
            ci.setReturnValue(false);
			return;
		}

		boolean hasPlatform = false;
		boolean isClientSide = world.isClientSide();
		final Vec3 offsetVec = new Vec3(1, 0, 0).yRot(checkYaw).xRot(pitch);
		final Vec3 traverseVec = new Vec3(0, 0, 1).yRot(checkYaw).xRot(pitch);
		Set<BlockPos> OKPos = new HashSet<>();
		for (int checkX = 1; checkX <= 3; checkX++) {
			for (int checkY = -2; checkY <= 3; checkY++) {
				for (double checkZ = -halfSpacing; checkZ <= halfSpacing; checkZ++) {
					final BlockPos checkPos = RailwayData.newBlockPos(trainX + offsetVec.x * checkX + traverseVec.x * checkZ, trainY + checkY, trainZ + offsetVec.z * checkX + traverseVec.z * checkZ);
					final Block block = world.getBlockState(checkPos).getBlock();

					if (block instanceof BlockPlatform || block instanceof BlockPSDAPGBase || IBlockPlatformClass.isInstance(block)) {
						openDoors(world, block, checkPos, dwellTicks);
						hasPlatform = true;
					}else if (block instanceof BlockEyeCandy) {
						if (OKPos.contains(checkPos)) continue;
						int[] dir = new int[]{1, -1};
						int[] f = new int[]{1, 0, 0, 1, 0, 0};
						if (checkEyeCandy(world, checkPos, isClientSide)) hasPlatform = true;
						for (int i = 0; i < 3; i++) {
							for (int j = 0; j < 2; j++) {
								for (int k = 1; k <= 40; k++) {
									int v = dir[j] * k;
									BlockPos pos = checkPos.offset(f[i] * v, f[i + 1] * v, f[i + 2] * v);
									if (OKPos.contains(pos)) break;
									OKPos.add(pos);
									if (checkEyeCandy(world, pos, isClientSide)) hasPlatform = true;
									else break;
								}
							}
						}
						
					}
				}
			}
		}
        ci.setReturnValue(hasPlatform);
		return;
    }

	private boolean checkEyeCandy(Level world, BlockPos pos, boolean isClientSide) {
		final BlockEntity entity = world.getBlockEntity(pos);
		if (entity instanceof BlockEyeCandy.BlockEntityEyeCandy) {
			BlockEyeCandy.BlockEntityEyeCandy e = (BlockEyeCandy.BlockEntityEyeCandy) entity;
			if (e.isPlatform()) {
				if (isClientSide) {
					e.setDoorTarget(doorTarget);
					e.setDoorValue(doorValue);
				}
				return true;
			} else return false;
		} else {
			return false;
		}
	}

	private static interface Void {
	}

	@Inject(method = "convertMaxManualSpeed", at = @At("HEAD"), cancellable = true, remap = false)
	private static void convertMaxManualSpeed(int maxManualSpeed, CallbackInfoReturnable<RailType> ci) {
		maxManualSpeed = Math.min(maxManualSpeed, RailType.values().length - 1);
		ci.setReturnValue(RailType.values()[maxManualSpeed]);
		ci.cancel();
		return;
	}

	private void pl(Object o) {
		System.out.println(o);
	}


	// @Overwrite(remap = false)
	// @Final
	// @Mutable

	private boolean mustStop(int stopIndex) {
		boolean result =
			!isCurrentlyManual ||
			isRailBlocked(stopIndex) ||
			(isRepeat() && stopIndex >= repeatIndex2 && distances.size() > repeatIndex1 ? 
				path.get(repeatIndex2).isOppositeRail(path.get(repeatIndex1)) : 
				(stopIndex >= distances.size() - 1 || path.get(stopIndex).isOppositeRail(path.get(stopIndex + 1))));
		return result;
	}

	@Final
	@Mutable
	@Inject(method = "simulateTrain(Lnet/minecraft/world/level/Level;FLmtr/data/Depot;)V", at = @At("HEAD"), cancellable = true, remap = true)
	protected void onSimulateTrain(Level world, float ticksElapsed, Depot depot, CallbackInfo ci) {
		ci.cancel();
		if (world == null) {
			return;
		}

		try {
			if (nextStoppingIndex >= path.size()) {
				return;
			}

			final boolean tempDoorOpen;
			final float tempDoorValue;
			final int totalDwellTicks = getTotalDwellTicks();

			if (!isOnRoute) {
				railProgress = (railLength + trainCars * spacing) / 2;
				reversed = false;
				tempDoorOpen = false;
				tempDoorValue = 0;
				speed = 0;
				nextStoppingIndex = 0;

				if (!isCurrentlyManual && canDeploy(depot) || isCurrentlyManual && manualNotch > 0) {
					startUp(world, trainCars, spacing, isOppositeRail());
				}
			} else {
				final float newAcceleration = accelerationConstant * ticksElapsed;

				if (railProgress >= distances.get(distances.size() - 1) - (railLength - trainCars * spacing) / 2) {
					isOnRoute = false;
					manualNotch = -2;
					ridingEntities.clear();
					tempDoorOpen = false;
					tempDoorValue = 0;
				} else {
					if (speed <= 0) {
						speed = 0;

						final boolean isOppositeRail = isOppositeRail();
						final boolean railBlocked = isRailBlocked(getIndex(0, spacing, true) + (isOppositeRail ? 2 : 1));

						if (totalDwellTicks == 0) {
							tempDoorOpen = false;
						} else {
							if (elapsedDwellTicks == 0 && isRepeat() && getIndex(railProgress, false) >= repeatIndex2 && distances.size() > repeatIndex1) {
								if (path.get(repeatIndex2).isOppositeRail(path.get(repeatIndex1))) {
									railProgress = distances.get(repeatIndex1 - 1) + trainCars * spacing;
									reversed = !reversed;
								} else {
									railProgress = distances.get(repeatIndex1);
								}
							}

							if (elapsedDwellTicks < totalDwellTicks - DOOR_MOVE_TIME - doorDelay - ticksElapsed || !railBlocked) {
								elapsedDwellTicks += ticksElapsed;
							}

							tempDoorOpen = openDoors();
						}

						if (!world.isClientSide() && (isCurrentlyManual || elapsedDwellTicks >= totalDwellTicks) && !railBlocked && (!isCurrentlyManual || manualNotch > 0)) {
							startUp(world, trainCars, spacing, isOppositeRail);
						}
					} else {
						if (!world.isClientSide()) {
							for (int checkIndex = getIndex(0, spacing, true) + 1; 
							nextPlatformIndex > 0 && nextPlatformIndex < path.size() && checkIndex <= nextPlatformIndex; checkIndex++) {
								if (isRailBlocked(checkIndex)) {
									nextStoppingIndex = checkIndex - 1;
									break;
								} else {
									nextStoppingIndex = nextPlatformIndex;
									if (manualNotch < -2) {
										manualNotch = 0;
									}
								}
							}
						}

						final double stoppingDistance = distances.get(nextStoppingIndex) - railProgress;
						if (mustStop(nextStoppingIndex) && !transportMode.continuousMovement && stoppingDistance < 0.5 * speed * speed / accelerationConstant) {
							speed = stoppingDistance <= 0 ? Train.ACCELERATION_DEFAULT : (float) Math.max(speed - (0.5 * speed * speed / stoppingDistance) * ticksElapsed, Train.ACCELERATION_DEFAULT);
							manualNotch = -3;
						} else {
							if (isCurrentlyManual) {
								if (manualNotch >= -2) {
									final RailType railType = Train.convertMaxManualSpeed(maxManualSpeed);
									speed = Mth.clamp(speed + manualNotch * newAcceleration / 2, 0, railType == null ? RailType.IRON.maxBlocksPerTick : railType.maxBlocksPerTick);
								}
							} else {
								final float railSpeed = getRailSpeed(getIndex(0, spacing, false));
								if (speed < railSpeed) {
									speed = Math.min(speed + newAcceleration, railSpeed);
									manualNotch = 2;
								} else if (speed > railSpeed) {
									speed = Math.max(speed - newAcceleration, railSpeed);
									manualNotch = -2;
								} else {
									manualNotch = 0;
								}
							}
						}

						tempDoorOpen = transportMode.continuousMovement && openDoors();
					}

					boolean in = false;
					for (int i = 0; isRepeat() && railProgress + speed * ticksElapsed >= distances.get(repeatIndex2) && distances.size() > repeatIndex1 && !path.get(repeatIndex2).isOppositeRail(path.get(repeatIndex1)) ; i++) {
						railProgress = (railProgress - distances.get(repeatIndex2 - 1)) +  distances.get(repeatIndex1 - 1);
						in = true;
						if (i > 100) {
							System.out.println("Infinite loop detected in TrainMixin.simulateTrain");
							break;
						}
					}
					if (in) {
						float speed0 = speed;
						startUp(world, trainCars, spacing, false);
						speed = speed0;
					}

					railProgress += speed * ticksElapsed;
					if (!transportMode.continuousMovement && railProgress > distances.get(nextStoppingIndex)) {
						if (mustStop(nextStoppingIndex)) {
							railProgress = distances.get(nextStoppingIndex);
							speed = 0;
							manualNotch = -2;
						} else {
							float speed0 = speed;
							startUp(world, trainCars, spacing, false);
							speed = speed0;
						}
					}

					tempDoorValue = Mth.clamp(doorValue + ticksElapsed * (doorTarget ? 1 : -1) / DOOR_MOVE_TIME, 0, 1);
				}
			}

			doorTarget = tempDoorOpen;
			doorValue = tempDoorValue;
			if (doorTarget || doorValue != 0) {
				manualNotch = -2;
			}

			if (!path.isEmpty()) {
				final Vec3[] positions = new Vec3[trainCars + 1];
				for (int i = 0; i <= trainCars; i++) {
					positions[i] = getRoutePosition(reversed ? trainCars - i : i, spacing);
				}

				if (handlePositions(world, positions, ticksElapsed)) {
					final double[] prevX = {0};
					final double[] prevY = {0};
					final double[] prevZ = {0};
					final float[] prevYaw = {0};
					final float[] prevPitch = {0};

					for (int i = 0; i < trainCars; i++) {
						final int ridingCar = i;
						_calculateCar(world, positions, i, totalDwellTicks, (x, y, z, yaw, pitch, realSpacing, doorLeftOpen, doorRightOpen) -> {
							simulateCar(
									world, ridingCar, ticksElapsed,
									x, y, z,
									yaw, pitch,
									prevX[0], prevY[0], prevZ[0],
									prevYaw[0], prevPitch[0],
									doorLeftOpen, doorRightOpen, realSpacing
							);
							prevX[0] = x;
							prevY[0] = y;
							prevZ[0] = z;
							prevYaw[0] = yaw;
							prevPitch[0] = pitch;
						});
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
