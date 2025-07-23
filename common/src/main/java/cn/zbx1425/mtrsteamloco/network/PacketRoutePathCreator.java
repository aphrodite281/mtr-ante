package cn.zbx1425.mtrsteamloco.network;

import net.minecraft.resources.ResourceLocation;
import cn.zbx1425.mtrsteamloco.Main;
import mtr.data.RailwayData;
import mtr.data.Route;
import io.netty.buffer.Unpooled;
import mtr.RegistryClient;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy;
import net.minecraft.world.level.block.state.BlockState;
import mtr.client.ClientData;
import mtr.Registry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class PacketRoutePathCreator {

    public static ResourceLocation ROUTE_C2S = new ResourceLocation(Main.MOD_ID, "route_path_creator/route_c2s");
    public static ResourceLocation ROUTE_S2C = new ResourceLocation(Main.MOD_ID, "route_path_creator/route_s2c");

    public static void sendRouteC2S(Route route) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        route.writePacket(packet);
        RegistryClient.sendToServer(ROUTE_C2S, packet);
    }

    public static void receiveRouteC2S(MinecraftServer server, ServerPlayer player, FriendlyByteBuf packet) {
#if MC_VERSION >= "12000"
        Level level = player.level();
#else
        Level level = player.level;
#endif
        RailwayData rd = RailwayData.getInstance(level);
        if (rd != null) {
            FriendlyByteBuf np = new FriendlyByteBuf(packet.copy());
            Route route = new Route(packet);
            server.execute(() -> {
                rd.routes.add(route);
                rd.dataCache.sync();
                for (Player p : level.players()) {
                    if (p instanceof ServerPlayer sp) Registry.sendToPlayer(sp, ROUTE_S2C, np);
                }
            });
        }
    }

    public static void receiveRouteS2C(FriendlyByteBuf packet) {
        Client.addRoute(packet);
    }

    private static class Client {
        public static void addRoute(FriendlyByteBuf packet) {
            Route route = new Route(packet);
            Minecraft.getInstance().execute(() -> {
                ClientData.ROUTES.add(route);
                ClientData.DATA_CACHE.sync();
            });
        }
    }

}