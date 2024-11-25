package cn.zbx1425.mtrsteamloco.render.scripting.util;

import cn.zbx1425.sowcer.math.Vector3f;
import com.mojang.text2speech.Narrator;
import mtr.mappings.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.commands.synchronization.brigadier.StringArgumentSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import mtr.data.Rail;
import mtr.path.PathData;
import net.minecraft.core.BlockPos;
import mtr.client.ClientData;
import mtr.data.RailwayData;
import mtr.data.Station;
import mtr.data.Platform;
import mtr.data.ScheduleEntry;
import mtr.data.RailwayData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import mtr.block.BlockNode;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;

public class MinecraftClientUtil {

    public static boolean worldIsRaining() {
        return Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.isRaining();
    }

    public static boolean worldIsRainingAt(Vector3f pos) {
        return Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.isRainingAt(pos.toBlockPos());
    }

    public static int worldDayTime() {
        return Minecraft.getInstance().level != null
                ? (int) Minecraft.getInstance().level.getDayTime() : 0;
    }

    public static void narrate(String message) {
        Minecraft.getInstance().execute(() -> {
            Narrator.getNarrator().say(message, true);
        });
    }

    public static void displayMessage(String message, boolean actionBar) {
        final Player player = Minecraft.getInstance().player;
        if (player != null) {
            Minecraft.getInstance().execute(() -> {
                player.displayClientMessage(Text.literal(message), actionBar);
            });
        }
    }
    
    public static void execute(Runnable runnable) {
        Minecraft.getInstance().execute(runnable);
    }

    public static void levelEvent(int p_109534_, Vector3f p_109535_, int p_109536_) {
        final Player player = Minecraft.getInstance().player;
        if (player != null) {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().level.levelEvent(player, p_109534_, p_109535_.toBlockPos(), p_109536_);
            });
        }
    }

    public static int getOccupiedAspect(Vector3f vPos, float facing, int aspects) {
        BlockPos pos = vPos.toBlockPos();
        Map<BlockPos, Float> nodesToScan = new HashMap<>();
        nodesToScan.put(pos, facing);
        int occupiedAspect = -1;

        for (int j = 1; j < aspects; j++) {
            final Map<BlockPos, Float> newNodesToScan = new HashMap<>();

            for (final Map.Entry<BlockPos, Float> checkNode : nodesToScan.entrySet()) {
                final Map<BlockPos, Rail> railMap = ClientData.RAILS.get(checkNode.getKey());

                if (railMap != null) {
                    for (final BlockPos endPos : railMap.keySet()) {
                        final Rail rail = railMap.get(endPos);

                        if (rail.facingStart.similarFacing(checkNode.getValue())) {
                            if (ClientData.SIGNAL_BLOCKS.isOccupied(PathData.getRailProduct(checkNode.getKey(), endPos))) {
                                return j;
                            } else {
                                final Boolean isOccupied = ClientData.OCCUPIED_RAILS.get(PathData.getRailProduct(checkNode.getKey(), endPos));
                                if (isOccupied != null && isOccupied) {
                                    return j;
                                }
                            }

                            newNodesToScan.put(endPos, rail.facingEnd.getOpposite().angleDegrees);
                            occupiedAspect = 0;
                        }
                    }
                }
            }

            nodesToScan = newNodesToScan;
        }

        return occupiedAspect;
    }

    public static Station getStationAt(Vector3f pos) {
        return RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, pos.toBlockPos());
    }

    public static Platform getPlatformAt(Vector3f pos, int radius, int lower, int upper) {
        Station station = RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, pos.toBlockPos());
        Map<Long, Platform> platformPositions = ClientData.DATA_CACHE.requestStationIdToPlatforms(station.id);
        Long id = RailwayData.getClosePlatformId(ClientData.PLATFORMS, ClientData.DATA_CACHE, pos.toBlockPos(), radius, lower, upper);
        Platform platform = platformPositions.get(id);
        return platform;   
    }

    public static Vector3f getNodeAt(Vector3f vPos, Float fFacing) {
        BlockPos pos = vPos.toBlockPos();
        Direction facing = Direction.fromYRot(fFacing);
        BlockGetter world = Minecraft.getInstance().level;
	    final int[] checkDistance = {0, 1, -1, 2, -2, 3, -3, 4, -4};
	    for (final int z : checkDistance) {
	    	for (final int x : checkDistance) {
	    		for (int y = -5; y <= 0; y++) {
	    			final BlockPos checkPos = pos.above(y).relative(facing.getClockWise(), x).relative(facing, z);
	    			final BlockState checkState = world.getBlockState(checkPos);
	    			if (checkState.getBlock() instanceof BlockNode) {
	    				return new Vector3f(checkPos);
	    			}
	    		}
	    	}
	    }
	    return null;
    }

    public static float getCameraDistance(Vector3f from) {
        Vector3f cameraPos = new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        return cameraPos.distance(from);
    }

    public static Vector3f getCameraPos() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
    }

	public Level getWorld() {
		return Minecraft.getInstance().level;
	}
}
