package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.sowcer.math.Matrix4f;
import net.minecraft.world.phys.Vec3;
import cn.zbx1425.mtrsteamloco.data.VehicleRidingClientExtraSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import mtr.data.*;
import mtr.data.*;
import mtr.client.*;
import mtr.*;
import mtr.packet.PacketTrainDataGuiClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.client.Camera;
import mtr.mappings.Utilities;
import net.minecraft.world.entity.player.Player;
import mtr.entity.EntitySeat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import mtr.render.RenderTrains;
import cn.zbx1425.sowcer.math.*;
import net.minecraft.client.renderer.entity.EntityRenderer;
import cn.zbx1425.mtrsteamloco.data.Rolling;
import cn.zbx1425.mtrsteamloco.data.Rolling.Rotation;

import java.util.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VehicleRidingClient.class)
public abstract class VehicleRidingClientMixin implements VehicleRidingClientExtraSupplier {

    private float[] roll = new float[0];
	private Vec3[] positions;
	private boolean reversed;
	private boolean hasPitchAscending = false;
	private boolean hasPitchDescending = false;

    @Override
    public float getRoll(int index) {
        if (index >= roll.length || index < 0) return 0; 
        return roll[index];
    }

    @Override
    public void setRoll(float[] roll) {
		this.roll = roll;
    }

	@Override
	public void setPositions(Vec3[] positions) {
		this.positions = positions;
	}

	@Override
	public void setReversed(boolean reversed) {
		this.reversed = reversed;
	}

	@Shadow(remap = false) private float clientPrevYaw;
	@Shadow(remap = false) private float oldPercentageX;
	@Shadow(remap = false) private float oldPercentageZ;
	@Shadow(remap = false) private double lastSentX;
	@Shadow(remap = false) private double lastSentY;
	@Shadow(remap = false) private double lastSentZ;
	@Shadow(remap = false) private float lastSentTicks;
	@Shadow(remap = false) private int interval;
	@Shadow(remap = false) private int previousInterval;

	@Shadow(remap = false) private List<Double> offset;
	@Shadow(remap = false) private Map<UUID, Float> percentagesX;
	@Shadow(remap = false) private Map<UUID, Float> percentagesZ;
	@Shadow(remap = false) private Map<UUID, Float> newPercentagesX;
	@Shadow(remap = false) private Map<UUID, Float> newPercentagesZ;
	@Shadow(remap = false) private Map<UUID, Vec3> riderPositions;
	@Shadow(remap = false) private Set<UUID> ridingEntities;
	@Shadow(remap = false) private ResourceLocation packetId;

	@Shadow(remap = false)
	private Vec3 getViewOffset() {
		throw new AssertionError();
	}

	@Unique
	private Rotation prevRotation = Rotation.IDENTITY;

	@Inject(method = "renderPlayerAndGetOffset", remap = false, at = @At("HEAD"), cancellable = true)
	private void onRenderPlayerAndGetOffset(CallbackInfoReturnable<Vec3> cir) {
		if (positions == null) return;

		final boolean noOffset = offset.isEmpty();
		final LocalPlayer clientPlayer = Minecraft.getInstance().player;

		riderPositions.forEach((uuid, position) -> {
			if (clientPlayer != null) {
				if (uuid.equals(clientPlayer.getUUID())) {
					Rolling.setRotation(prevRotation);
				}
			}
			
			if (noOffset) {
				renderRidingPlayer(getViewOffset(), uuid, position);
			} else {
				renderRidingPlayer(getViewOffset(), uuid, position.subtract(offset.get(0), offset.get(1), offset.get(2)));
			}
		});

		if (noOffset) {
			cir.setReturnValue(Vec3.ZERO);
		} else {
			cir.setReturnValue(new Vec3(offset.get(0), offset.get(1), offset.get(2)));
		}
		cir.cancel();
	}

	@Unique
	private Map<UUID, Float> prevYaw = new HashMap<>();
	private Map<UUID, Float> prevPitch = new HashMap<>();

	private static double getValueFromPercentage(double percentage, double total) {
		return (percentage - 0.5) * total;
	}

	@Inject(method = "setOffsets", at = @At("HEAD"), remap = false, cancellable = true)
	public void setOffsets(UUID uuid, double x, double y, double z, float yaw, float pitch, double length, int width, boolean doorLeftOpen, boolean doorRightOpen, boolean hasPitchAscending, boolean hasPitchDescending, float riderOffset, float riderOffsetDismounting, boolean shouldSetOffset, boolean shouldSetYaw, Runnable clientPlayerCallback, CallbackInfo ci) {
		if (positions == null) return;

		prevYaw.put(uuid, yaw);
		prevPitch.put(uuid, pitch);
		this.hasPitchAscending = hasPitchAscending;
		this.hasPitchDescending = hasPitchDescending;

		final LocalPlayer clientPlayer = Minecraft.getInstance().player;
		if (clientPlayer == null) {
			return;
		}

		final boolean isClientPlayer = uuid.equals(clientPlayer.getUUID());
		final double percentageX = getValueFromPercentage(percentagesX.get(uuid), width);
		final float riderOffsetNew = doorLeftOpen && percentageX < 0 || doorRightOpen && percentageX > 1 ? riderOffsetDismounting : riderOffset;
		int currentRidingCar = Mth.clamp((int) Math.floor(percentagesZ.get(uuid)), 0, positions.length - 2);
		Matrix4f mat = new Matrix4f();
		mat.rotateY(yaw);//
		mat.rotateX((pitch < 0 ? hasPitchAscending : hasPitchDescending) ? -pitch : 0);
		mat.translate(0, -1, 0);
		mat.rotateZ((reversed ? 1 : -1) * getRoll(currentRidingCar));
		mat.translate(0, 1, 0);
		Vector3f playerOff = new Vector3f((float) percentageX, riderOffsetNew, (float) getValueFromPercentage(Mth.frac(percentagesZ.get(uuid)), length));
		Vector3f vect = mat.transform(playerOff);
		final Vec3 playerOffset = vect.toVec3();
		ClientData.updatePlayerRidingOffset(uuid);
		riderPositions.put(uuid, playerOffset.add(x, y, z));

		if (isClientPlayer) {
			prevRotation = new Rotation(x, y, z, yaw, (pitch < 0 ? hasPitchAscending : hasPitchDescending) ? pitch : 0, getRoll(currentRidingCar), reversed);

			final double moveX = x + playerOffset.x;
			final double moveY = y + playerOffset.y;
			final double moveZ = z + playerOffset.z;
			final boolean movePlayer;

			if (MTRClient.isVivecraft()) {
				final Entity vehicle = clientPlayer.getVehicle();
				if (vehicle instanceof EntitySeat) {
					((EntitySeat) vehicle).setPosByTrain(moveX, moveY, moveZ);
					movePlayer = false;
				} else {
					movePlayer = true;
				}

				final float tempPercentageX = percentagesX.get(uuid);
				final boolean doorOpen = doorLeftOpen && tempPercentageX < 0 || doorRightOpen && tempPercentageX > 1;
				final boolean movedFar = Math.abs(lastSentX - moveX) > 2 || Math.abs(lastSentY - moveY) > 2 || Math.abs(lastSentZ - moveZ) > 2;

				if (doorOpen || MTRClient.getGameTick() - lastSentTicks > 60 && movedFar) {
					PacketTrainDataGuiClient.sendUpdateEntitySeatPassengerPosition(moveX, moveY, moveZ);
					lastSentX = moveX;
					lastSentY = moveY;
					lastSentZ = moveZ;
					lastSentTicks = MTRClient.getGameTick();
				}
			} else {
				movePlayer = true;
			}

			if (movePlayer) {
				clientPlayer.fallDistance = 0;
				clientPlayer.setDeltaMovement(0, 0, 0);
				clientPlayer.setSpeed(0);
				if (MTRClient.getGameTick() > 40) {
					clientPlayer.absMoveTo(moveX, moveY, moveZ);
				}
			}

			clientPlayerCallback.run();

			if (shouldSetOffset) {
				if (shouldSetYaw) {
					float angleDifference = (float) Math.toDegrees(clientPrevYaw - yaw);
					if (angleDifference > 180) {
						angleDifference -= 360;
					} else if (angleDifference < -180) {
						angleDifference += 360;
					}
					Utilities.incrementYaw(clientPlayer, angleDifference);
				}
				offset.add(x);
				offset.add(y);
				offset.add(z);
				if (MTRClient.isVivecraft()) {
					offset.add(playerOffset.x);
					offset.add(playerOffset.y);
					offset.add(playerOffset.z);
				} else {
					float eyeHeight = clientPlayer.getEyeHeight();
					final Vec3 off;
					if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
						playerOff.add(0, eyeHeight, 0);
						off = mat.transform(playerOff).toVec3();
					} else {
						off = playerOffset.add(0, eyeHeight, 0);
					}
					offset.add(off.x);
					offset.add(off.y);
					offset.add(off.z);
				}
			}

			clientPrevYaw = yaw;
		}
		ci.cancel();
	}

	private void renderRidingPlayer(Vec3 viewOffset, UUID playerId, Vec3 playerPositionOffset) {
		if (positions == null) return;
		final BlockPos posAverage = TrainRendererBaseAccessor.invokeApplyAverageTransform(viewOffset, playerPositionOffset.x, playerPositionOffset.y, playerPositionOffset.z);
		if (posAverage == null) {
			return;
		}
		PoseStack matrices = TrainRendererBaseAccessor.getMatrices();
		Camera camera = TrainRendererBaseAccessor.getCamera();
		Level world = TrainRendererBaseAccessor.getWorld();
		matrices.translate(0, RenderTrains.PLAYER_RENDER_OFFSET, 0);
		
		final Player renderPlayer = world.getPlayerByUUID(playerId);
		if (renderPlayer != null && (!playerId.equals(TrainRendererBaseAccessor.getPlayer().getUUID()) || camera.isDetached())) {
			EntityRenderer<? super Entity> entityrenderer = TrainRendererBaseAccessor.getEntityRenderDispatcher().getRenderer(renderPlayer);
			Vec3 vec3 = entityrenderer.getRenderOffset(renderPlayer, 1);
			vec3 = vec3.add(playerPositionOffset);
			matrices.pushPose();
			matrices.translate(vec3.x, vec3.y, vec3.z);
			Float yaw = prevYaw.get(playerId);
			Float pitch = prevPitch.get(playerId);
			if (yaw != null && pitch != null) {
				int currentRidingCar = Mth.clamp((int) Math.floor(percentagesZ.get(playerId)), 0, positions.length - 2);
				float p = (pitch < 0 ? hasPitchAscending : hasPitchDescending) ? -pitch : 0;
				PoseStackUtil.rotY(matrices, yaw);
				PoseStackUtil.rotX(matrices, p);
				PoseStackUtil.rotZ(matrices, (reversed? 1 : -1) * getRoll(currentRidingCar));
				PoseStackUtil.rotY(matrices, -yaw);
			}
			matrices.translate(-vec3.x, -vec3.y, -vec3.z);
			matrices.translate(playerPositionOffset.x, playerPositionOffset.y, playerPositionOffset.z);
			TrainRendererBaseAccessor.getEntityRenderDispatcher().render(renderPlayer, 0, 0, 0, 0, 1, matrices, TrainRendererBaseAccessor.getVertexConsumers(), 0xF000F0);
			matrices.popPose();
		}
		matrices.popPose();
	}
}