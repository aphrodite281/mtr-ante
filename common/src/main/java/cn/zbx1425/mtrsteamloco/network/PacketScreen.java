package cn.zbx1425.mtrsteamloco.network;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.gui.BrushEditRailScreen;
import cn.zbx1425.mtrsteamloco.gui.EyeCandyScreen;
import cn.zbx1425.mtrsteamloco.gui.RailPathEditorScreen;
import cn.zbx1425.mtrsteamloco.item.RailPathEditor;
import cn.zbx1425.mtrsteamloco.gui.CompoundCreatorScreen;
import cn.zbx1425.mtrsteamloco.gui.DirectNodeScreen;
import cn.zbx1425.mtrsteamloco.gui.RoutePathCreatorScreen;
import io.netty.buffer.Unpooled;
import mtr.Registry;
import mtr.data.Rail;
import mtr.mappings.UtilitiesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class PacketScreen {

    public static ResourceLocation PACKET_SHOW_SCREEN = new ResourceLocation(Main.MOD_ID, "show_screen");

    public static void sendScreenS2C(ServerPlayer player, String screenName) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeInt(0);
        packet.writeUtf(screenName);
        Registry.sendToPlayer(player, PACKET_SHOW_SCREEN, packet);
    }

    public static void sendScreenBlockS2C(ServerPlayer player, String screenName, BlockPos pos) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeInt(1);
        packet.writeUtf(screenName);
        packet.writeBlockPos(pos);
        Registry.sendToPlayer(player, PACKET_SHOW_SCREEN, packet);
    }

    public static void sendScreenRailPathEditorS2C(ServerPlayer player, Rail rail, BlockPos posStart, BlockPos posEnd) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeInt(2);
        packet.writeUtf("rail_path_editor");
        packet.writeBlockPos(posStart);
        packet.writeBlockPos(posEnd);
        rail.writePacket(packet);
        System.out.println("sending rail path editor screen");
        Registry.sendToPlayer(player, PACKET_SHOW_SCREEN, packet);
    }

    public static void receiveScreenS2C(FriendlyByteBuf packet) {
        MakeClassLoaderHappy.receiveScreenS2C(packet);
    }

    private static class MakeClassLoaderHappy {
        public static void receiveScreenS2C(FriendlyByteBuf packet) {
            Minecraft minecraftClient = Minecraft.getInstance();
            int type = packet.readInt();
            String screenName = packet.readUtf();
            final BlockPos pos0;
            if (type > 0) {
                pos0 = packet.readBlockPos();
            } else {
                pos0 = null;
            }
            final BlockPos pos1;
            final Rail rail;
            if (screenName.equals("rail_path_editor")) {
                pos1 = packet.readBlockPos();
                rail = new Rail(packet);
            } else {
                pos1 = null;
                rail = null;
            }
            minecraftClient.execute(() -> {
                switch (screenName) {
                    case "eye_candy":
                        minecraftClient.setScreen(EyeCandyScreen.createScreen(pos0, null));
                        break;
                    case "brush_edit_rail":
                        minecraftClient.setScreen(BrushEditRailScreen.createScreen(null));
                        break;
                    case "compound_creator":
                        minecraftClient.setScreen(CompoundCreatorScreen.createScreen(null));
                        break;
                    case "direct_node":
                        minecraftClient.setScreen(DirectNodeScreen.createScreen(minecraftClient.level, pos0, null));
                    case "rail_path_editor":
                        minecraftClient.setScreen(RailPathEditorScreen.createScreen(pos0, pos1, rail, null));
                        break;
                    case "route_path_creator":
                        minecraftClient.setScreen(RoutePathCreatorScreen.createScreen(null));
                        break;
                }
            });
        }
    }
}
