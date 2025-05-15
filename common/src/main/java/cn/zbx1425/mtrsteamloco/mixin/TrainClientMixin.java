package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.render.train.NoopTrainRenderer;
import cn.zbx1425.mtrsteamloco.sound.NoopTrainSound;
import net.minecraft.network.FriendlyByteBuf;
import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import cn.zbx1425.mtrsteamloco.render.scripting.train.ScriptedTrainRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.player.LocalPlayer;
import mtr.path.PathData;
import mtr.sound.TrainSoundBase;
import cn.zbx1425.mtrsteamloco.data.TrainExtraSupplier;
import cn.zbx1425.sowcer.math.Matrices;
import net.minecraft.world.phys.Vec3;
import mtr.render.TrainRendererBase;
import mtr.data.TrainClient.*;
import mtr.data.*;
import mtr.client.*;
import mtr.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import cn.zbx1425.mtrsteamloco.data.VehicleRidingClientExtraSupplier;
import mtr.render.RenderDrivingOverlay;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrainClient.class)
public abstract class TrainClientMixin extends Train implements IGui{

    public TrainClientMixin(
			long sidingId, float railLength,
			List<PathData> path, List<Double> distances, int repeatIndex1, int repeatIndex2,
			float accelerationConstant, boolean isManualAllowed, int maxManualSpeed, int manualToAutomaticTime,
			CompoundTag compoundTag
	) {
        super(sidingId, railLength, path, distances, repeatIndex1, repeatIndex2, accelerationConstant, isManualAllowed, maxManualSpeed, manualToAutomaticTime, compoundTag);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void initTail(FriendlyByteBuf packet, CallbackInfo ci) {
        if (!ClientConfig.enableTrainRender) ((TrainClientAccessor) this).setTrainRenderer(NoopTrainRenderer.INSTANCE);
        if (!ClientConfig.enableTrainSound) ((TrainClientAccessor) this).setTrainSound(NoopTrainSound.INSTANCE);
    }

    @Inject(method = "copyFromTrain", at = @At("TAIL"), remap = false)
    private void copyFromTrainTail(Train other, CallbackInfo ci) {
        ((TrainExtraSupplier) (Object) this).setCustomConfigs(((TrainExtraSupplier) (Object) other).getCustomConfigs());
    }

	@Shadow(remap = false) boolean isRemoved = false;
	@Shadow(remap = false) boolean justMounted;
	@Shadow(remap = false) float oldSpeed;
	@Shadow(remap = false) double oldRailProgress;
	@Shadow(remap = false) float oldDoorValue;
	@Shadow(remap = false) boolean doorOpening;
	@Shadow(remap = false) boolean isSitting;
	@Shadow(remap = false) boolean previousShifting;

    @Shadow(remap = false) static float CONNECTION_HEIGHT;
    @Shadow(remap = false) static float CONNECTION_Z_OFFSET;
    @Shadow(remap = false) static float CONNECTION_X_OFFSET;
    @Shadow(remap = false) TrainSoundBase trainSound;
    @Shadow(remap = false) VehicleRidingClient vehicleRidingClient;
    @Shadow(remap = false) TrainRendererBase trainRenderer;
    @Shadow(remap = false) Set<Runnable> trainTranslucentRenders;
	@Shadow(remap = false) SpeedCallback speedCallback;
	@Shadow(remap = false) AnnouncementCallback announcementCallback;
	@Shadow(remap = false) AnnouncementCallback lightRailAnnouncementCallback;
	@Shadow(remap = false) Depot depot;
	@Shadow(remap = false) List<Long> routeIds;
	@Shadow(remap = false) int currentStationIndex;
	@Shadow(remap = false) Route thisRoute;
	@Shadow(remap = false) Route nextRoute;
	@Shadow(remap = false) Station thisStation;
	@Shadow(remap = false) Station nextStation;
	@Shadow(remap = false) Station lastStation;

	@Shadow(remap = false) abstract int getPreviousStoppingIndex(int headIndex);
	@Shadow(remap = false) abstract boolean justOpening();

	protected boolean handlePositions(Level world, Vec3[] positions, float ticksElapsed) {
		final Minecraft client = Minecraft.getInstance();
		final LocalPlayer clientPlayer = client.player;
		if (clientPlayer == null) {
			return false;
		}

		vehicleRidingClient.begin();

		float[] rolls = new float[trainCars];
		for (int i = 0; i < trainCars; i++) {
			float roll = TrainExtraSupplier.getRollAngleAt((Train) (Object) this, i);
			rolls[i] = roll;
		}
		((VehicleRidingClientExtraSupplier) (Object) vehicleRidingClient).setReversed(reversed);
		((VehicleRidingClientExtraSupplier) (Object) vehicleRidingClient).setRoll(rolls);
		((VehicleRidingClientExtraSupplier) (Object) vehicleRidingClient).setPositions(positions);

		if (ticksElapsed > 0) {
			if (isPlayerRiding(clientPlayer)) {
				final int headIndex = getIndex(0, spacing, false);
				final int stopIndex = path.get(headIndex).stopIndex - 1;

				if (speedCallback != null) {
					speedCallback.speedCallback(speed * 20, stopIndex, routeIds);
				}

				if (announcementCallback != null) {
					final double targetProgress = distances.get(getPreviousStoppingIndex(headIndex)) + (trainCars + 1) * spacing;
					if (oldRailProgress < targetProgress && railProgress >= targetProgress) {
						announcementCallback.announcementCallback(stopIndex, routeIds);
					}
				}

				if (lightRailAnnouncementCallback != null && (justOpening() || justMounted)) {
					lightRailAnnouncementCallback.announcementCallback(stopIndex, routeIds);
				}
			}

			final TrainProperties trainProperties = TrainClientRegistry.getTrainProperties(trainId);
			vehicleRidingClient.movePlayer(uuid -> {
				final int currentRidingCar = Mth.clamp((int) Math.floor(vehicleRidingClient.getPercentageZ(uuid)), 0, positions.length - 2);
				final CalculateCarCallback calculateCarCallback = (x, y, z, yaw, pitch, realSpacingRender, doorLeftOpenRender, doorRightOpenRender) -> vehicleRidingClient.setOffsets(uuid, x, y, z, yaw, pitch, transportMode.maxLength == 1 ? spacing : realSpacingRender, width, doorLeftOpenRender, doorRightOpenRender, transportMode.hasPitchAscending, transportMode.hasPitchDescending, trainProperties.riderOffset, trainProperties.riderOffsetDismounting, speed > 0, doorValue == 0, () -> {
					final boolean isShifting = clientPlayer.isShiftKeyDown();
					if (Config.shiftToToggleSitting() && !MTRClient.isVivecraft()) {
						if (isShifting && !previousShifting) {
							isSitting = !isSitting;
						}
						clientPlayer.setPose(isSitting && !client.gameRenderer.getMainCamera().isDetached() ? Pose.CROUCHING : Pose.STANDING);
					}
					previousShifting = isShifting;
				});
				calculateCar(world, positions, currentRidingCar, 0, (x, y, z, yaw, pitch, realSpacingRender, doorLeftOpenRender, doorRightOpenRender) -> {
					vehicleRidingClient.moveSelf(id, uuid, realSpacingRender, width, yaw, currentRidingCar, trainCars, doorLeftOpenRender, doorRightOpenRender, !trainProperties.hasGangwayConnection, ticksElapsed);

					final int newRidingCar = Mth.clamp((int) Math.floor(vehicleRidingClient.getPercentageZ(uuid)), 0, positions.length - 2);
					if (currentRidingCar == newRidingCar) {
						calculateCarCallback.calculateCarCallback(x, y, z, yaw, pitch, realSpacingRender, doorLeftOpenRender, doorRightOpenRender);
					} else {
						calculateCar(world, positions, newRidingCar, 0, calculateCarCallback);
					}
				});
			});
		}

		vehicleRidingClient.end();
		justMounted = false;

		final Entity camera = client.cameraEntity;
		final Vec3 cameraPos = camera == null ? Vec3.ZERO : camera.position();
		double nearestDistance = Double.POSITIVE_INFINITY;
		int nearestCar = 0;
		for (int i = 0; i < trainCars; i++) {
			final double checkDistance = cameraPos.distanceToSqr(positions[i]);
			if (checkDistance < nearestDistance) {
				nearestCar = i;
				nearestDistance = checkDistance;
			}
		}
		final BlockPos soundPos = RailwayData.newBlockPos(positions[nearestCar].x, positions[nearestCar].y, positions[nearestCar].z);
		trainSound.playNearestCar(world, soundPos, nearestCar);

		return true;
	}

    // @Override
	protected void simulateCar(
			Level world, int ridingCar, float ticksElapsed,
			double carX, double carY, double carZ, float carYaw, float carPitch,
			double prevCarX, double prevCarY, double prevCarZ, float prevCarYaw, float prevCarPitch,
			boolean doorLeftOpen, boolean doorRightOpen, double realSpacing
	) {
        final LocalPlayer clientPlayer = Minecraft.getInstance().player;
		if (clientPlayer == null) {
			return;
		}

		final BlockPos soundPos = RailwayData.newBlockPos(carX, carY, carZ);
		trainSound.playAllCars(world, soundPos, ridingCar);
		if (doorLeftOpen || doorRightOpen) {
			trainSound.playAllCarsDoorOpening(world, soundPos, ridingCar);
		}

		final Vec3 offset = vehicleRidingClient.renderPlayerAndGetOffset();
		final double newX = carX - offset.x;
		final double newY = carY - offset.y;
		final double newZ = carZ - offset.z;

		doorOpening = doorValue > oldDoorValue;
		trainRenderer.renderCar(ridingCar, newX, newY, newZ, carYaw, carPitch, doorLeftOpen, doorRightOpen);
		trainTranslucentRenders.add(() -> trainRenderer.renderCar(ridingCar, newX, newY, newZ, carYaw, carPitch, doorLeftOpen, doorRightOpen));

		if (ridingCar > 0) {
			float roll = TrainExtraSupplier.getRollAngleAt((Train) (Object) this, ridingCar);
            float preRoll = TrainExtraSupplier.getRollAngleAt((Train) (Object) this, ridingCar - 1);

            Matrices pre = new Matrices();
            pre.translate(prevCarX - offset.x, prevCarY - offset.y, prevCarZ - offset.z);
            pre.rotateY(prevCarYaw);
            pre.rotateX(- prevCarPitch);
            pre.translate(0, -1, 0);
            pre.rotateZ(- preRoll);
            pre.translate(0, 1, 0);

            Matrices thi = new Matrices();
            thi.translate(newX, newY, newZ);
            thi.rotateY(carYaw);
            thi.rotateX(- carPitch);
            thi.translate(0, -1, 0);
            thi.rotateZ(- roll);
            thi.translate(0, 1, 0);

            pre.pushPose();
            pre.translate(0, 0, spacing / 2D - 1);
            Vec3 prevPos0 = now(pre);
            pre.popPose();

            thi.pushPose();
            thi.translate(0, 0, -(spacing / 2D - 1));
            Vec3 thisPos0 = now(thi);
            thi.popPose();

            final Vec3 connectPos = prevPos0.add(thisPos0).scale(0.5);
			final float connectYaw = (float) Mth.atan2(thisPos0.x - prevPos0.x, thisPos0.z - prevPos0.z);
			final float connectPitch = realSpacing == 0 ? 0 : (float) Math.asin((thisPos0.y - prevPos0.y) / realSpacing);

			for (int i = 0; i < 2; i++) {
				final double xStart = width / 2D + (i == 0 ? -1 : 0.5) * CONNECTION_X_OFFSET;
				final double zStart = spacing / 2D - (i == 0 ? 1 : 2) * CONNECTION_Z_OFFSET;

                pre.pushPose();
                pre.translate(xStart, SMALL_OFFSET, zStart);
                Vec3 prevPos1 = now(pre);
                pre.translate(0, CONNECTION_HEIGHT, 0);
                Vec3 prevPos2 = now(pre);
                pre.popPushPose();
                pre.translate(-xStart, CONNECTION_HEIGHT + SMALL_OFFSET, zStart);
                Vec3 prevPos3 = now(pre);
                pre.translate(0, - CONNECTION_HEIGHT, 0);
                Vec3 prevPos4 = now(pre);
                pre.popPose();

                thi.pushPose();
                thi.translate(-xStart, SMALL_OFFSET, -zStart);
                Vec3 thisPos1 = now(thi);
                thi.translate(0, CONNECTION_HEIGHT, 0);
                Vec3 thisPos2 = now(thi);
                thi.popPushPose();
                thi.translate(xStart, CONNECTION_HEIGHT + SMALL_OFFSET, -zStart);
                Vec3 thisPos3 = now(thi);
                thi.translate(0, - CONNECTION_HEIGHT, 0);
                Vec3 thisPos4 = now(thi);
                thi.popPose();
                
				if (i == 0) {
					trainRenderer.renderConnection(prevPos1, prevPos2, prevPos3, prevPos4, thisPos1, thisPos2, thisPos3, thisPos4, connectPos.x, connectPos.y, connectPos.z, connectYaw, connectPitch);
				} else {
					trainRenderer.renderBarrier(prevPos1, prevPos2, prevPos3, prevPos4, thisPos1, thisPos2, thisPos3, thisPos4, connectPos.x, connectPos.y, connectPos.z, connectYaw, connectPitch);
				}
			}
		}
    }

	@Inject(method = "simulateTrain", at = @At("HEAD"), remap = false, cancellable = true)
	private void simulateTrain(Level world, float ticksElapsed, SpeedCallback speedCallback, AnnouncementCallback announcementCallback, AnnouncementCallback lightRailAnnouncementCallback, CallbackInfo ci) {
		trainTranslucentRenders.clear();
		this.speedCallback = speedCallback;
		this.announcementCallback = announcementCallback;
		this.lightRailAnnouncementCallback = lightRailAnnouncementCallback;
		oldSpeed = speed;
		oldRailProgress = railProgress;
		oldDoorValue = doorValue;
		
		if (ticksElapsed != 0) {
			final int stopIndex = path.get(getIndex(0, spacing, false)).stopIndex - 1;
			if (!RailwayData.useRoutesAndStationsFromIndex(stopIndex, routeIds, ClientData.DATA_CACHE, (currentStationIndex, thisRoute1, nextRoute1, thisStation1, nextStation1, lastStation1) -> {
				this.currentStationIndex = currentStationIndex;
				thisRoute = thisRoute1;
				nextRoute = nextRoute1;
				thisStation = thisStation1;
				nextStation = nextStation1;
				lastStation = lastStation1;
			})) {
				currentStationIndex = 0;
				thisRoute = null;
				nextRoute = null;
				thisStation = null;
				nextStation = null;
				lastStation = null;
			}
		}

		simulateTrain(world, 0, null);

		if (depot == null || routeIds.isEmpty()) {
			final Siding siding = ClientData.DATA_CACHE.sidingIdMap.get(sidingId);
			depot = siding == null ? null : ClientData.DATA_CACHE.sidingIdToDepot.get(siding.id);
			routeIds = depot == null ? new ArrayList<>() : depot.routeIds;
			if (depot != null) {
				depot.lastDeployedMillis = System.currentTimeMillis();
			}
		}

		final LocalPlayer player = Minecraft.getInstance().player;
		if (isManualAllowed && Train.isHoldingKey(player) && isPlayerRiding(player)) {
			RenderDrivingOverlay.setData(manualNotch, (TrainClient) (Object) this);
		}
		
		ci.cancel();
	}

    private static Vec3 now(Matrices matrices) {
        return matrices.last().getTranslationPart().toVec3();
    }
}
