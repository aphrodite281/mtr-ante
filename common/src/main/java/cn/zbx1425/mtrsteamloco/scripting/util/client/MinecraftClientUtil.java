package cn.zbx1425.mtrsteamloco.scripting.util.client;

import cn.zbx1425.mtrsteamloco.scripting.util.WrappedEntity;
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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.pipeline.RenderCall;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy;
import net.minecraft.world.level.block.Block;
import mtr.block.BlockPlatform;
import mtr.block.BlockPSDAPGBase;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.LevelRenderer;


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

    public static void recordRenderCall(RenderCall renderCall) {
        RenderSystem.recordRenderCall(renderCall);
    }

    public static boolean isOnRenderThreadOrInit() {
        return RenderSystem.isOnRenderThreadOrInit();
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

    public static Vector3f getCameraPos() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
    }

    public static float getCameraDistance(Vector3f from) {
        Vector3f cameraPos = getCameraPos();
        return cameraPos.distance(from);
    }

	public static Level getLevel() {
		return Minecraft.getInstance().level;
	} 

    public static boolean[] canOpenDoorsAt(Vector3f p1, Vector3f p2) {
        final Level world = getLevel();
		final Vec3 pos1 = p1.toVec3();
		final Vec3 pos2 = p2.toVec3();
		final int dwellTicks = 114514;

		final double x = getAverage(pos1.x, pos2.x);
		final double y = getAverage(pos1.y, pos2.y) + 1;
		final double z = getAverage(pos1.z, pos2.z);

		final double realSpacing = pos2.distanceTo(pos1);
		final float yaw = (float) Mth.atan2(pos2.x - pos1.x, pos2.z - pos1.z);
		final float pitch = realSpacing == 0 ? 0 : (float) asin((pos2.y - pos1.y) / realSpacing);
		final boolean doorLeftOpen = scanDoors(world, x, y, z, (float) Math.PI + yaw, pitch, realSpacing / 2);
		final boolean doorRightOpen = scanDoors(world, x, y, z, yaw, pitch, realSpacing / 2);
		return new boolean[]{doorLeftOpen, doorRightOpen};
	}

    static double getAverage(double a, double b) {
		return (a + b) / 2;
	}

    static boolean scanDoors(Level world, double trainX, double trainY, double trainZ, float checkYaw, float pitch, double halfSpacing) {
		final Vec3 offsetVec = new Vec3(1, 0, 0).yRot(checkYaw).xRot(pitch);
		final Vec3 traverseVec = new Vec3(0, 0, 1).yRot(checkYaw).xRot(pitch);
        for (int checkX = 1; checkX <= 3; checkX++) {
			for (int checkY = -2; checkY <= 3; checkY++) {
				for (double checkZ = -halfSpacing; checkZ <= halfSpacing; checkZ++) {
                    final BlockPos checkPos = RailwayData.newBlockPos(trainX + offsetVec.x * checkX + traverseVec.x * checkZ, trainY + checkY, trainZ + offsetVec.z * checkX + traverseVec.z * checkZ);
					final Block block = world.getBlockState(checkPos).getBlock();
                    if (block instanceof BlockPlatform || block instanceof BlockPSDAPGBase) {
						return true;
					}
                    if (block instanceof BlockEyeCandy) {
                        final BlockEntity entity = world.getBlockEntity(checkPos);
                        if (entity instanceof BlockEyeCandy.BlockEntityEyeCandy) {
                            BlockEyeCandy.BlockEntityEyeCandy e = (BlockEyeCandy.BlockEntityEyeCandy) entity;
                            if (e.isPlatform()) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    static double asin(double value) {
		return Math.asin(value);
	}

    public static WrappedEntity getCameraEntity() {
        return new WrappedEntity(Minecraft.getInstance().cameraEntity);
    }

    public static WrappedEntity getPlayer() {
        return new WrappedEntity(Minecraft.getInstance().player);
    }

    public static int packLightTexture(int p_109886_, int  p_109887_) {
       return p_109886_ << 4 | p_109887_ << 20;
    }

    public static int getLightColor(Vector3f pos) {
        return LevelRenderer.getLightColor(getLevel(), pos.toBlockPos());
    }

    public static void setScreen(Screen screen) {
        execute(() -> Minecraft.getInstance().setScreen(screen));
    }

    public static Screen getPresentScreen() {
        return Minecraft.getInstance().screen;
    }

    public static void reloadResourcePacks() {
        execute(Minecraft.getInstance()::reloadResourcePacks);
    }

    public static void markRendererAllChanged() {
        Minecraft.getInstance().levelRenderer.allChanged();
    }
}
