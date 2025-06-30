package cn.zbx1425.mtrsteamloco.mixin;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.msgpack.core.MessagePacker;
import org.msgpack.value.Value;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.data.BezierCurve;
import cn.zbx1425.mtrsteamloco.data.ConfigResponder;
import cn.zbx1425.mtrsteamloco.data.RailAngleExtra;
import cn.zbx1425.mtrsteamloco.data.RailCalculator;
import cn.zbx1425.mtrsteamloco.data.RailCalculator.Vec2;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import cn.zbx1425.mtrsteamloco.network.util.DoubleFloatMapSerializer;
import cn.zbx1425.mtrsteamloco.network.util.StringMapSerializer;
import cn.zbx1425.mtrsteamloco.render.rail.BakedRail;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.mtrsteamloco.network.PacketUpdateRail;
import io.netty.buffer.Unpooled;
import mtr.data.MessagePackHelper;
import mtr.data.Rail;
import mtr.data.Rail.RenderRail;
import mtr.data.RailAngle;
import mtr.data.RailType;
import mtr.data.TransportMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Rail.class, priority = 1425)
public abstract class RailMixin implements RailExtraSupplier {

    private static final double ACCEPT_THRESHOLD = 1E-4;
    @Shadow(remap = false) @Final @Mutable private RailType railType;
	@Shadow(remap = false) @Final @Mutable private TransportMode transportMode;
	@Shadow(remap = false) @Final @Mutable private RailAngle facingStart;
	@Shadow(remap = false) @Final @Mutable private RailAngle facingEnd;
	@Shadow(remap = false) @Final @Mutable private double h1, k1, r1, tStart1, tEnd1;
	@Shadow(remap = false) @Final @Mutable private double h2, k2, r2, tStart2, tEnd2;
	@Shadow(remap = false) @Final @Mutable private int yStart, yEnd;
	@Shadow(remap = false) @Final @Mutable private boolean reverseT1, isStraight1, reverseT2, isStraight2;

    private String modelKey = "";
    private boolean isSecondaryDir = false;
    private float verticalCurveRadius = 0f;
    private Map<String, String> customConfigs = new HashMap<>();
    private Map<String, ConfigResponder> customResponders = new HashMap<>();
    private Map<Double, Float> rollAngleMap = new HashMap<>();
    private int openingDirection = 0;// 0 不开 1 左 2 右 3 双向
    private float rollingOffset = 1.435F / 2F;
    private int pathMode = 0;
    // 0: 曲线 + 直线(原版模式) 1: 贝塞尔曲线
    private BezierCurve bezier = null;
    private BlockPos posStart, posEnd;
    private RailAngle railAngleStart, railAngleEnd;

    @Override
    public String getModelKey() {
        return modelKey;
    }

    @Override
    public void setModelKey(String key) {
        this.modelKey = key;
    }

    @Override
    public boolean getRenderReversed() {
        return isSecondaryDir;
    }

    @Override
    public void setRenderReversed(boolean value) {
        this.isSecondaryDir = value;
    }

    @Override
    public float getVerticalCurveRadius() {
        return verticalCurveRadius;
    }

    @Override
    public void setVerticalCurveRadius(float value) {
        this.verticalCurveRadius = value;
    }

    @Override
    public int getHeight() {
        return yEnd - yStart;
    }

    @Override
    public Map<Double, Float> getRollAngleMap() {
        return rollAngleMap;
    }

    @Override
    public void setRollAngleMap(Map<Double, Float> rollAngleMap) {
        this.rollAngleMap = rollAngleMap;
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
    public Map<String, ConfigResponder> getCustomResponders() {
        return customResponders;
    }

    @Override
    public void setCustomResponders(Map<String, ConfigResponder> customResponders) {
        this.customResponders = customResponders;
    }

    @Override
    public void setOpeningDirectionRaw(int direction) {
        this.openingDirection = direction;
    }

    @Override
    public void setOpeningDirection(int direction) {
        if (getRenderReversed()) {
            if (direction == 1) direction = 2;
            else if (direction == 2) direction = 1;
        }
        this.openingDirection = direction;
    }

    @Override
    public int getOpeningDirection() {
        int direction = openingDirection;
        if (getRenderReversed()) {
            if (direction == 1) direction = 2;
            else if (direction == 2) direction = 1;
        }
        return direction;
    }

    @Override
    public int getOpeningDirectionRaw() {
        return openingDirection;
    }

    @Override
    public void setRailType(RailType railType) {
        this.railType = railType;
    }

    @Override
    public void partialCopyFrom(Rail other) {
        RailExtraSupplier oth = (RailExtraSupplier) other;
        setModelKey(oth.getModelKey());
        // setRenderReversed(oth.getRenderReversed());
        setVerticalCurveRadius(oth.getVerticalCurveRadius());
        setCustomConfigs(oth.getCustomConfigs());
        double length0 = other.getLength();
        double length1 = ((Rail) (Object) this).getLength();
        double k = length1 / length0;
        Map<Double, Float> src = new HashMap<>(oth.getRollAngleMap());
        Map<Double, Float> dst = new HashMap<>();
        for (Map.Entry<Double, Float> entry : src.entrySet()) {
            dst.put(entry.getKey() * k, entry.getValue());
        }
        setRollAngleMap(dst);
        setOpeningDirection(oth.getOpeningDirection());
        setRollingOffset(oth.getRollingOffset());
    }

    @Override
    public void setRollingOffset(float rollingOffset) {
        this.rollingOffset = rollingOffset;
    }

    @Override
    public float getRollingOffset() {
        return rollingOffset;
    }

    @Override
    public boolean isStraightOnly() {
        return (isStraight1 || (h1 == 0 && k1 == 0 && r1 == 0 && tStart1 == 0 && tEnd1 == 0)) && (isStraight2 || (h2 == 0 && k2 == 0 && r2 == 0 && tStart2 == 0 && tEnd2 == 0));
    }

    @Override
    public int getPathMode() {
        return pathMode;
    }

    @Override
    public void changePathMode(int mode) {
        if (pathMode == mode) return;
        switch (mode) {
            case 0: genForSegmentsAndArcs(); break;
            case 1: genForBezier(); break;
        }
    }

    @Override
    public void setBezier(BezierCurve bezier) {
        this.bezier = bezier;
        pathMode = 1;
    }

    @Override
    public boolean isBetween(double x, double y, double z, double radius) {
        if (pathMode == 1) {
            return bezier.isBetween(x, y, z, radius);
        } else {
            Vec3 start = getPosition(0);
            Vec3 end = getPosition(getLength());
            Vec3 max = max(start, end).add(radius, radius, radius);
            Vec3 min = min(start, end).add(-radius, -radius, -radius);
            return x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z;
        }
    }

    @Override
    public Rail getTransposition(RailType railType) {
        if (railType != RailType.NONE) railType = this.railType;
        Rail result = new Rail(posEnd, facingEnd, posStart, facingStart, railType, transportMode);
        RailExtraSupplier extra = (RailExtraSupplier) (Object) result;
        extra.setModelKey(modelKey);
        extra.setRenderReversed(!isSecondaryDir);
        extra.setVerticalCurveRadius(verticalCurveRadius);
        extra.setCustomConfigs(customConfigs);
        extra.setRollAngleMap(rollAngleMap);
        extra.setOpeningDirection(openingDirection);
        extra.setRollingOffset(rollingOffset);
        if (pathMode == 1) {
            if (bezier == null) genForBezier();
            extra.setBezier(bezier.getTransposition());
        }
        return result;
    }

    @Override
    public boolean couldSwitchModeTo(int mode) {
        switch(mode) {
            case 0: {
                final int xStart = posStart.getX();
                final int zStart = posStart.getZ();
                final int xEnd = posEnd.getX();
                final int zEnd = posEnd.getZ();

                RailCalculator.Group group = RailCalculator.calculate(xStart, zStart, xEnd, zEnd, facingStart.angleRadians, facingEnd.angleRadians);
                if (group == null) return false;
                return true;
            }
            case 1: return true;
            default: return false;
        }
    }

    @Override
    public void sendUpdateC2S() {
        PacketUpdateRail.sendUpdateC2S((Rail) (Object) this, posStart, posEnd);
    }

    @Override
    public BlockPos getPosStart() {
        return posStart;
    }

    @Override
    public BlockPos getPosEnd() {
        return posEnd;
    }

    private Vec3 max(Vec3 a, Vec3 b) {
        return new Vec3(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    private Vec3 min(Vec3 a, Vec3 b) {
        return new Vec3(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
    }

    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lmtr/data/RailAngle;Lnet/minecraft/core/BlockPos;Lmtr/data/RailAngle;Lmtr/data/RailType;Lmtr/data/TransportMode;)V", at = @At("TAIL"))
    private void onCreate(BlockPos posStart, RailAngle facingStart, BlockPos posEnd, RailAngle facingEnd, RailType railType, TransportMode transportMode, CallbackInfo ci) {
        this.facingStart = facingStart;
        this.facingEnd = facingEnd;
        this.posStart = posStart;
        this.posEnd = posEnd;

        if (transportMode == TransportMode.TRAIN) {
            genForSegmentsAndArcs();
        }
    }

    private void genForSegmentsAndArcs() {
        pathMode = 0;

        final int xStart = posStart.getX();
		final int zStart = posStart.getZ();
		final int xEnd = posEnd.getX();
		final int zEnd = posEnd.getZ();

        RailCalculator.Group group = RailCalculator.calculate(xStart, zStart, xEnd, zEnd, facingStart.angleRadians, facingEnd.angleRadians);

        if (group != null) {
            h1 = group.first.h;
            k1 = group.first.k;
            r1 = group.first.r;
            tStart1 = group.first.tStart;
            tEnd1 = group.first.tEnd;
            reverseT1 = group.first.reverseT;
            isStraight1 = group.first.isStraight;

            h2 = group.second.h;
            k2 = group.second.k;
            r2 = group.second.r;
            tStart2 = group.second.tStart;
            tEnd2 = group.second.tEnd;
            reverseT2 = group.second.reverseT;
            isStraight2 = group.second.isStraight;
        }
    }

    private BlockPos round(Vec3 pos) {
        return new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
    }

    private void genForBezier() {
        Vec3 posStart = Vec3.atLowerCornerOf(this.posStart);
        Vec3 posEnd = Vec3.atLowerCornerOf(this.posEnd);

        double length = Math.pow(posStart.x - posEnd.x, 2) + Math.pow(posStart.z - posEnd.z, 2);
        length = Math.sqrt(length);

        Vec3 p1 = getCtrlPoint(facingStart.angleRadians, posStart, length / 2, 0);
        Vec3 p2 = getCtrlPoint(facingEnd.angleRadians, posEnd, length / 2, 0);

        bezier = new BezierCurve(
            0.1, posStart, p1, p2, posEnd
        );
        pathMode = 1;

        refreshRailAngle();
    }

    private Vec3 getCtrlPoint(double rad, Vec3 base, double t, float elevationAngle) {
        Vec3 ad = new Vec3(0, 0, t).yRot((float) (Math.PI / 2 - rad)).xRot(elevationAngle).add(base);
        return ad;
    }

    private void refreshRailAngle() {
        this.facingStart = getRailAngle(false);
        this.facingEnd = getRailAngle(true);
    }

    @Shadow(remap = false) public abstract double getLength();
    @Shadow(remap = false) public abstract Vec3 getPosition(double distance);
    @Shadow(remap = false) public abstract RailAngle getRailAngle(boolean getEnd);

    @Inject(method = "getLength", at = @At("HEAD"), cancellable = true, remap = false)
    private void getLength(CallbackInfoReturnable<Double> cir) {
        if (pathMode == 1 && bezier!= null) {
            cir.setReturnValue(bezier.getLength());
            cir.cancel();
            return;
        }
    }

    @Inject(method = "getRailAngle", at = @At("HEAD"), cancellable = true, remap = false)
    private void getRailAngle(boolean getEnd, CallbackInfoReturnable<RailAngle> cir) {
        final double start;
		final double end;
		if (getEnd) {
			start = getLength();
			end = start - ACCEPT_THRESHOLD;
		} else {
			start = 0;
			end = ACCEPT_THRESHOLD;
		}
		final Vec3 pos1 = getPosition(start);
		final Vec3 pos2 = getPosition(end);
		RailAngle result =  RailAngleExtra.fromRadians(Math.atan2(pos2.z - pos1.z, pos2.x - pos1.x));
        cir.setReturnValue(result);
        cir.cancel();
        return;
    }

    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true, remap = false)
    private void isValid(CallbackInfoReturnable<Boolean> cir) {
        boolean b1 = (h1 != 0 || k1 != 0 || h2 != 0 || k2 != 0 || r1 != 0 || r2 != 0 || tStart1 != 0 || tStart2 != 0 || tEnd1 != 0 || tEnd2 != 0 || (bezier != null && pathMode == 1));
        RailAngle f1 = getRailAngle(false);
        RailAngle f2 = getRailAngle(true);
        boolean b2 = false;
        boolean b3 = false;
        float f3 = f1.angleDegrees - facingStart.angleDegrees;
        float f4 = f2.angleDegrees - facingEnd.angleDegrees;
        f3 = Math.abs(f3) % 180;
        f4 = Math.abs(f4) % 180;
        if (f3 < 0.3 || f3 > 179.7) b2 = true;
        if (f4 < 0.3 || f4 > 179.7) b3 = true;
        b2 = b2 && b3;
        cir.setReturnValue(b1 && b2);
        cir.cancel();
        return;
    }

    public boolean goodRadius() {
        return true;
    }

    @Inject(method = "<init>(Ljava/util/Map;)V", at = @At("TAIL"), remap = false)
    private void fromMessagePack(Map<String, Value> map, CallbackInfo ci) {
        MessagePackHelper messagePackHelper = new MessagePackHelper(map);
        modelKey = messagePackHelper.getString("model_key", "");
        isSecondaryDir = messagePackHelper.getBoolean("is_secondary_dir", false);
        verticalCurveRadius = messagePackHelper.getFloat("vertical_curve_radius", 0);
        try {
			customConfigs = StringMapSerializer.deserialize(messagePackHelper.getString("custom_configs"));
		} catch (IOException e) {
			customConfigs = new HashMap<>();
		}
        try {
            rollAngleMap = DoubleFloatMapSerializer.deserialize(messagePackHelper.getString("roll_angle_map"));
        } catch (Exception e) {
            rollAngleMap = new HashMap<>();
        }
        openingDirection = messagePackHelper.getInt("opening_direction", 0);
        rollingOffset = messagePackHelper.getFloat("rolling_offset", 1.435F / 2F);
        pathMode = messagePackHelper.getInt("path_mode", 0);
        if (pathMode == 1) {
            bezier = new BezierCurve(map);
            refreshRailAngle();
        }
        if (!map.containsKey("pos_start") ||!map.containsKey("facing_start") ||!map.containsKey("pos_end") ||!map.containsKey("facing_end")) {
            posStart = round(getPosition(0));
            posEnd = round(getPosition(getLength()));
            return;
        }
        posStart = BlockPos.of(messagePackHelper.getLong("pos_start"));
        facingStart = RailAngleExtra.fromRadians(messagePackHelper.getDouble("facing_start"));
        posEnd = BlockPos.of(messagePackHelper.getLong("pos_end"));
        facingEnd = RailAngleExtra.fromRadians(messagePackHelper.getDouble("facing_end"));
    }

    @Inject(method = "toMessagePack", at = @At("TAIL"), remap = false)
    private void toMessagePack(MessagePacker messagePacker, CallbackInfo ci) throws IOException {
        messagePacker.packString("model_key").packString(modelKey);
        messagePacker.packString("is_secondary_dir").packBoolean(isSecondaryDir);
        messagePacker.packString("vertical_curve_radius").packFloat(verticalCurveRadius);
        String res;
		try {
			res = StringMapSerializer.serializeToString(customConfigs);
		} catch (IOException e) {
			res = "";
		}
		messagePacker.packString("custom_configs").packString(res);
        String res2;
        try {
			res2 = DoubleFloatMapSerializer.serializeToString(rollAngleMap);
		} catch (Exception e) {
			res2 = "";
		}
        messagePacker.packString("roll_angle_map").packString(res2);
        messagePacker.packString("opening_direction").packInt(openingDirection);
        messagePacker.packString("rolling_offset").packFloat(rollingOffset);
        messagePacker.packString("path_mode").packInt(pathMode);
        if (pathMode == 1 && bezier != null) {
            bezier.save(messagePacker);
        }
        messagePacker.packString("pos_start").packLong(posStart.asLong());
        messagePacker.packString("facing_start").packDouble(facingStart.angleRadians);
        messagePacker.packString("pos_end").packLong(posEnd.asLong());
        messagePacker.packString("facing_end").packDouble(facingEnd.angleRadians);
    }

    @Inject(method = "messagePackLength", at = @At("TAIL"), cancellable = true, remap = false)
    private void messagePackLength(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(cir.getReturnValue() + 12 + (pathMode == 0 ? 0 : 1));
    }

    private final int NTE_PACKET_EXTRA_MAGIC = 0x25141425;

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"))
    private void fromPacket(FriendlyByteBuf packet, CallbackInfo ci) {
        if (!Main.enableRegistry) return;
        if (packet.readableBytes() <= 4) return;
        if (packet.readInt() != NTE_PACKET_EXTRA_MAGIC) {
            packet.readerIndex(packet.readerIndex() - 4);
            return;
        }
        modelKey = packet.readUtf();
        isSecondaryDir = packet.readBoolean();
        verticalCurveRadius = packet.readFloat();
        try {
			customConfigs = StringMapSerializer.deserialize(packet.readUtf());
		} catch (IOException e) {
			customConfigs = new HashMap<>();
		}
        try {
            rollAngleMap = DoubleFloatMapSerializer.deserialize(packet.readUtf());
        } catch (Exception e) {
            rollAngleMap = new HashMap<>();
        }
        openingDirection = packet.readInt();
        rollingOffset = packet.readFloat();
        pathMode = packet.readInt();
        if (pathMode == 1) {
            bezier = new BezierCurve(packet);
            refreshRailAngle();
        }
        posStart = packet.readBlockPos();
        facingStart = RailAngleExtra.fromRadians(packet.readDouble());
        posEnd = packet.readBlockPos();
        facingEnd = RailAngleExtra.fromRadians(packet.readDouble());
    }

    @Inject(method = "writePacket", at = @At("TAIL"))
    private void toPacket(FriendlyByteBuf packet, CallbackInfo ci) {
        if (!Main.enableRegistry) return;
        packet.writeInt(NTE_PACKET_EXTRA_MAGIC);
        packet.writeUtf(modelKey);
        packet.writeBoolean(isSecondaryDir);
        packet.writeFloat(verticalCurveRadius);
        String res;
		try {
			res = StringMapSerializer.serializeToString(customConfigs);
		} catch (IOException e) {
			res = "";
		}
		packet.writeUtf(res);
        String res2;
        try {
			res2 = DoubleFloatMapSerializer.serializeToString(rollAngleMap);
        } catch (Exception e) {
            res2 = "";
        }
        packet.writeUtf(res2);
        packet.writeInt(openingDirection);
        packet.writeFloat(rollingOffset);
        packet.writeInt(pathMode);
        if (pathMode == 1 && bezier != null) {
            bezier.save(packet);
        }
        packet.writeBlockPos(posStart);
        packet.writeDouble(facingStart.angleRadians);
        packet.writeBlockPos(posEnd);
        packet.writeDouble(facingEnd.angleRadians);
    }

    // @Redirect(method = "renderSegment", remap = false, at = @At(value = "INVOKE", target = "Ljava/lang/Math;round(D)J"))
    // private long redirectRenderSegmentRound(double r) {
    //     if (ClientConfig.getRailRenderLevel() < 2) return Math.round(r);

    //     Rail instance = (Rail)(Object)this;
    //     if (instance.railType == RailType.NONE) {
    //         return Math.round(r);
    //     } else {
    //         return Math.round(r / RailModelRegistry.getProperty(RailRenderDispatcher.getModelKeyForRender(instance)).repeatInterval);
    //     }
    // }

    private float vTheta;

    private static final int CABLE_CURVATURE_SCALE = 1000;
	private static final int MAX_CABLE_DIP = 8;

    private double _getPositionY(double value) {
		final double length = getLength();

		if (railType.railSlopeStyle == RailType.RailSlopeStyle.CABLE) {
			if (value < 0.5) {
				return yStart;
			} else if (value > length - 0.5) {
				return yEnd;
			}

			final double offsetValue = value - 0.5;
			final double offsetLength = length - 1;
			final double posY = yStart + (yEnd - yStart) * offsetValue / offsetLength;
			final double dip = offsetLength * offsetLength / 4 / CABLE_CURVATURE_SCALE;
			return posY + (dip > MAX_CABLE_DIP ? MAX_CABLE_DIP / dip : 1) * (offsetValue - offsetLength) * offsetValue / CABLE_CURVATURE_SCALE;
		} else {
			final double intercept = length / 2;
			final double yChange;
			final double yInitial;
			final double offsetValue;

			if (value < intercept) {
				yChange = (yEnd - yStart) / 2D;
				yInitial = yStart;
				offsetValue = value;
			} else {
				yChange = (yStart - yEnd) / 2D;
				yInitial = yEnd;
				offsetValue = length - value;
			}

			return yChange * offsetValue * offsetValue / (intercept * intercept) + yInitial;
		}
	}

    @Inject(method = "getPosition", at = @At("HEAD"), cancellable = true, remap = false)
    private void getPosition(double rawValue, CallbackInfoReturnable<Vec3> cir) {
        if (pathMode != 1 || bezier == null) return;// 回到原版模式
        BezierCurve.Vec3 vec = bezier.getPosition(rawValue);
        cir.setReturnValue(new Vec3(vec.x, vec.y, vec.z).add(0.5, 0, 0.5).add(0, rollingYOffset(rawValue), 0));
        cir.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void render(RenderRail callback, float offsetRadius1, float offsetRadius2, CallbackInfo ci) {
        Rail rail = (Rail) (Object) this;
		final double count = rail.getLength();
		final double increment = count / Math.round(count);
        final Vector3f a = new Vector3f(offsetRadius1, 0, 0);
        final Vector3f b = new Vector3f(offsetRadius2, 0, 0);

		for (double i = 0; i < count - 0.1; i += increment) {
            Matrix4f mat0 = getLookAtMat(rail, i);
            Matrix4f mat1 = getLookAtMat(rail, i + increment);
            final Vector3f corner1 = mat0.transform(a);
            final Vector3f corner2 = mat0.transform(b);
            final Vector3f corner3 = mat1.transform(b);
            final Vector3f corner4 = mat1.transform(a);

			final float y1 = mat0.transform(Vector3f.ZERO).y();
			final float y2 = mat1.transform(Vector3f.ZERO).y();

			callback.renderRail(corner1.x(), corner1.z(), corner2.x(), corner2.z(), corner3.x(), corner3.z(), corner4.x(), corner4.z(), y1, y2);
		}
        ci.cancel();
	}

    private static final double HALF_ACCEPT_THRESHOLD = ACCEPT_THRESHOLD / 2;

    private Matrix4f getLookAtMat(Rail rail, double value) {
        if (value < HALF_ACCEPT_THRESHOLD) value = HALF_ACCEPT_THRESHOLD;
        else if (value + HALF_ACCEPT_THRESHOLD > rail.getLength()) value = rail.getLength() - HALF_ACCEPT_THRESHOLD;
        Vec3 from = rail.getPosition(value - HALF_ACCEPT_THRESHOLD);
        Vec3 to = rail.getPosition(value + HALF_ACCEPT_THRESHOLD);
        Vec3 mid = from.add(to).scale(0.5F);
        float roll = RailExtraSupplier.getRollAngle(rail, value);
        return BakedRail.getLookAtMat(mid, from, to, roll, 0, isSecondaryDir);
    }

    @Inject(method = "getPositionY", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetPositionY(double rawValue, CallbackInfoReturnable<Double> cir) {
        if (((Rail)(Object)this).railType.railSlopeStyle == RailType.RailSlopeStyle.CABLE) return;
        double H = Math.abs(yEnd - yStart);
        double L = ((Rail)(Object)this).getLength();
        int sign = yStart < yEnd ? 1 : -1;
        double maxRadius = (H == 0) ? 0 : Math.abs((H * H + L * L) / (H * 4));
        double result = 0d;
        if (pathMode == 1) {
            if (bezier != null)
                result = bezier.getPosition(rawValue).y;
        } else if (verticalCurveRadius < 0) {
            // Magic value for a flat rail
            result = sign * (rawValue / L) * H + yStart;
        } else if (verticalCurveRadius == 0 || verticalCurveRadius > maxRadius) {
            // Magic default value / impossible radius, fallback to MTR all curvy track
            result  = _getPositionY(rawValue);
        } else {
            if (vTheta == 0) vTheta = RailExtraSupplier.getVTheta((Rail)(Object)this, verticalCurveRadius);
            if (!Double.isFinite(vTheta)) {
                result = _getPositionY(rawValue);
            } else {
                float curveL = Mth.sin(vTheta) * verticalCurveRadius;
                float curveH = (1 - Mth.cos(vTheta)) * verticalCurveRadius;
                if (rawValue < curveL) {
                    float r = (float)rawValue;
                    result = sign * (verticalCurveRadius - Math.sqrt(verticalCurveRadius * verticalCurveRadius - r * r)) + yStart;
                } else if (rawValue > L - curveL) {
                    float r = (float)(L - rawValue);
                    result = -sign * (verticalCurveRadius - Math.sqrt(verticalCurveRadius * verticalCurveRadius - r * r)) + yEnd;
                } else {
                    result = sign * (((rawValue - curveL) / (L - 2 * curveL)) * (H - 2 * curveH) + curveH) + yStart;
                }
            }
        }
        result += rollingYOffset(rawValue);
        cir.setReturnValue(result);
        cir.cancel();
        return;
    }

    private double rollingYOffset(double rawValue) {
        float r = RailExtraSupplier.getRollAngle((Rail) (Object) this, rawValue);
        float offset = getRollingOffset();
        if (r == 0) return 0;
        double dy = new Vec2(offset, 0).rotateRad(r).z;
        return Math.abs(dy);
    }

    // private double 

    private static final FriendlyByteBuf hashBuilder = new FriendlyByteBuf(Unpooled.buffer());
    private byte[] dataBytes;
    private int hashCode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (dataBytes == null) createDataBytes();
        if (((RailMixin)o).dataBytes == null) ((RailMixin)o).createDataBytes();
        return Arrays.equals(dataBytes, ((RailMixin)o).dataBytes);
    }

    @Override
    public int hashCode() {
        if (dataBytes == null) createDataBytes();
        return hashCode;
    }

    private void createDataBytes() {
        hashBuilder.clear();
        ((Rail)(Object)this).writePacket(hashBuilder);
        dataBytes = new byte[hashBuilder.writerIndex()];
        hashBuilder.getBytes(0, dataBytes);
        hashCode = Arrays.hashCode(dataBytes);
    }
}
