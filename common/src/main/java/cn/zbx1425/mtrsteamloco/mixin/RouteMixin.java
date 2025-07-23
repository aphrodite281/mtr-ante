package cn.zbx1425.mtrsteamloco.mixin;

import mtr.data.Route;
import mtr.path.PathData;
import mtr.data.RailType;
import cn.zbx1425.mtrsteamloco.data.IRoute;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import org.msgpack.core.MessagePacker;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.Value;
import org.msgpack.core.buffer.ArrayBufferOutput;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Route.class)
public abstract class RouteMixin implements IRoute{
    private List<PathData> pathData = new ArrayList<>();

    @Shadow(remap = false) List<Route.RoutePlatform> platformIds;

    @Override
    public List<PathData> getPathData() {
        return pathData;
    }

    @Override
    public void setPathData(List<PathData> pathData) {
        PathData last = null;
        for (PathData data : pathData) {
            if (data.rail.railType == RailType.PLATFORM && data.savedRailBaseId != 0) {
                if (last == null || !last.isOppositeRail(data)) platformIds.add(new Route.RoutePlatform(data.savedRailBaseId));
            }
            last = data;
        }

        this.pathData = pathData;
    }

    // @Override
    // public void setAllData(FriendlyByteBuf buffer) {
        
    // }
    
    @Inject(method = "<init>(Ljava/util/Map;)V", at = @At("TAIL"), remap = false)
    private void fromMessagePack(Map<String, Value> map, CallbackInfo ci) {
        try {
            if (map.containsKey("ante_path")) {
                ArrayValue arrayValue = map.get("ante_path").asArrayValue();
                for (Value value : arrayValue) {
                    Map<Value, Value> oldMap = value.asMapValue().map();
                    final HashMap<String, Value> resultMap = new HashMap<>(oldMap.size());
                    oldMap.forEach((key, newValue) -> resultMap.put(key.asStringValue().asString(), newValue));
                    pathData.add(new PathData(resultMap));
                }
            }
        } catch (Exception e) {
            System.out.println("Error while loading path data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"))
    private void fromPacket(FriendlyByteBuf packet, CallbackInfo ci) {
        int length = packet.readInt();
        for (int i = 0; i < length; i++) {
            pathData.add(new PathData(packet));
        }
    }

    @Inject(method = "toMessagePack", at = @At("TAIL"), remap = false)
    private void toMessagePack(MessagePacker messagePacker, CallbackInfo ci) throws IOException {
        if (pathData.isEmpty()) return;
        messagePacker.packString("ante_path");
        messagePacker.packArrayHeader(pathData.size());
        for (PathData data : pathData) {
            messagePacker.packMapHeader(data.messagePackLength());
            data.toMessagePack(messagePacker);
        }
    }

    @Inject(method = "writePacket", at = @At("TAIL"))
    private void toPacket(FriendlyByteBuf packet, CallbackInfo ci) {
        packet.writeInt(pathData.size());
        for (PathData data : pathData) {
            data.writePacket(packet);
        }
    }

    @Inject(method = "messagePackLength", at = @At("TAIL"), cancellable = true, remap = false)
    private void messagePackLength(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(cir.getReturnValue() + pathData.size() <= 0 ? 0 : 1);
    }
}