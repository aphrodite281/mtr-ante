package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import cn.zbx1425.mtrsteamloco.data.RailModelRegistry;
import cn.zbx1425.mtrsteamloco.render.rail.RailRenderDispatcher;
import de.javagl.obj.Obj;
import io.netty.buffer.Unpooled;
import mtr.data.MessagePackHelper;
import mtr.data.Rail;
import mtr.data.RailType;
import mtr.data.TransportMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.Value;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Mixin(value = Rail.class, priority = 1425)
public abstract class RailMixin implements RailExtraSupplier {

    private String modelKey = "";
    private boolean isSecondaryDir = false;
    private float verticalCurveRadius = 0f;

    @Override
    public String getModelKey() {
        return modelKey;
    }

    @Override
    public void setModelKey(String key) {
        this.modelKey = key;
    }

    @Override
    public boolean getIsSecondaryDir() {
        return isSecondaryDir;
    }

    @Override
    public void setIsSecondaryDir(boolean value) {
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

    @Inject(method = "<init>(Ljava/util/Map;)V", at = @At("TAIL"), remap = false)
    private void fromMessagePack(Map<String, Value> map, CallbackInfo ci) {
        if (!Main.enableRegistry) return;
        MessagePackHelper messagePackHelper = new MessagePackHelper(map);
        modelKey = messagePackHelper.getString("model_key", "");
        isSecondaryDir = messagePackHelper.getBoolean("is_secondary_dir", false);
    }

    @Inject(method = "toMessagePack", at = @At("TAIL"), remap = false)
    private void toMessagePack(MessagePacker messagePacker, CallbackInfo ci) throws IOException {
        if (!Main.enableRegistry) return;
        messagePacker.packString("model_key").packString(modelKey);
        messagePacker.packString("is_secondary_dir").packBoolean(isSecondaryDir);
    }

    @Inject(method = "messagePackLength", at = @At("TAIL"), cancellable = true, remap = false)
    private void messagePackLength(CallbackInfoReturnable<Integer> cir) {
        if (!Main.enableRegistry) return;
        cir.setReturnValue(cir.getReturnValue() + 2);
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
    }

    @Inject(method = "writePacket", at = @At("TAIL"))
    private void toPacket(FriendlyByteBuf packet, CallbackInfo ci) {
        if (!Main.enableRegistry) return;
        packet.writeInt(NTE_PACKET_EXTRA_MAGIC);
        packet.writeUtf(modelKey);
        packet.writeBoolean(isSecondaryDir);
    }

    @Redirect(method = "renderSegment", remap = false, at = @At(value = "INVOKE", target = "Ljava/lang/Math;round(D)J"))
    private long redirectRenderSegmentRound(double r) {
        if (ClientConfig.getRailRenderLevel() < 2) return Math.round(r);

        Rail instance = (Rail)(Object)this;
        if (instance.railType == RailType.NONE) {
            return Math.round(r);
        } else {
            return Math.round(r / RailModelRegistry.getRepeatInterval(RailRenderDispatcher.getModelKeyForRender(instance)));
        }
    }

    @Shadow @Final private int yStart, yEnd;

    private float vTheta;

    @Inject(method = "getPositionY", at = @At("HEAD"), cancellable = true)
    private void getPositionY(double rawValue, CallbackInfoReturnable<Double> cir) {
        if (((Rail)(Object)this).railType.railSlopeStyle == RailType.RailSlopeStyle.CABLE) return;
        double H = yEnd - yStart;
        double L = ((Rail)(Object)this).getLength();
        if (verticalCurveRadius < 0) {
            // Magic value for a flat rail
            cir.setReturnValue((rawValue / L) * H + yStart);
        } else if ( verticalCurveRadius == 0 ||
                verticalCurveRadius * verticalCurveRadius > Mth.lengthSquared(H / 2, L / 2)) {
            // Magic default value / impossible radius, fallback to MTR all curvy track
        } else {
            if (vTheta == 0) vTheta = RailExtraSupplier.getVTheta((Rail)(Object)this, verticalCurveRadius);
            if (!Double.isFinite(vTheta)) return;
            float curveL = Mth.sin(vTheta) * verticalCurveRadius;
            float curveH = (1 - Mth.cos(vTheta)) * verticalCurveRadius;
            if (rawValue < curveL) {
                float r = (float)rawValue;
                cir.setReturnValue(curveH - Math.sqrt(verticalCurveRadius * verticalCurveRadius - r * r));
            } else if (rawValue > L - curveL) {
                float r = (float)(L - rawValue);
                cir.setReturnValue(H - curveH + Math.sqrt(verticalCurveRadius * verticalCurveRadius - r * r));
            } else {
                cir.setReturnValue(((rawValue - curveL) / (L - 2 * curveL)) * (H - 2 * curveH) + curveH);
            }
        }
    }

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
