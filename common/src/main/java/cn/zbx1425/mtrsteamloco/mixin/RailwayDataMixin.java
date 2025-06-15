package cn.zbx1425.mtrsteamloco.mixin;

import net.minecraft.world.phys.Vec3;
import mtr.data.*;
import cn.zbx1425.mtrsteamloco.data.*;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.netty.buffer.Unpooled;
import mtr.MTR;
import mtr.Registry;
import mtr.block.BlockNode;
import mtr.mappings.PersistentStateMapper;
import mtr.mappings.Utilities;
import mtr.packet.*;
import mtr.path.PathData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import java.util.stream.Collectors;

import java.util.*;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;

@Mixin(RailwayData.class)
public class RailwayDataMixin implements IPacket {

    @Shadow(remap = false) private Set<Station> stations = new HashSet<>();
	@Shadow(remap = false) private Set<Platform> platforms = new HashSet<>();
	@Shadow(remap = false) private Set<Siding> sidings = new HashSet<>();
	@Shadow(remap = false) private Set<Route> routes = new HashSet<>();
	@Shadow(remap = false) private Set<Depot> depots = new HashSet<>();
	@Shadow(remap = false) private Set<LiftServer> lifts = new HashSet<>();
	@Shadow(remap = false) private DataCache dataCache = new DataCache(stations, platforms, sidings, routes, depots, lifts);

	@Shadow(remap = false) private RailwayDataLoggingModule railwayDataLoggingModule;
	@Shadow(remap = false) private RailwayDataCoolDownModule railwayDataCoolDownModule;
	@Shadow(remap = false) private RailwayDataPathGenerationModule railwayDataPathGenerationModule;
	@Shadow(remap = false) private RailwayDataRailActionsModule railwayDataRailActionsModule;
	@Shadow(remap = false) private RailwayDataDriveTrainModule railwayDataDriveTrainModule;
	@Shadow(remap = false) private RailwayDataRouteFinderModule railwayDataRouteFinderModule;

	@Shadow(remap = false) private int prevPlatformCount;
	@Shadow(remap = false) private int prevSidingCount;
	@Shadow(remap = false) private boolean useTimeAndWindSync;

	@Shadow(remap = false) private Level world;
	@Shadow(remap = false) private Map<BlockPos, Map<BlockPos, Rail>> rails = new HashMap<>();
	@Shadow(remap = false) private SignalBlocks signalBlocks = new SignalBlocks();

	@Shadow(remap = false) private RailwayDataFileSaveModule railwayDataFileSaveModule;

	@Shadow(remap = false) private List<Map<UUID, Long>> trainPositions = new ArrayList<>(2);
	@Shadow(remap = false) private Map<Player, BlockPos> playerLastUpdatedPositions = new HashMap<>();
	@Shadow(remap = false) private List<Player> playersToSyncSchedules = new ArrayList<>();
	@Shadow(remap = false) private UpdateNearbyMovingObjects<TrainServer> updateNearbyTrains;
	@Shadow(remap = false) private UpdateNearbyMovingObjects<LiftServer> updateNearbyLifts;
	@Shadow(remap = false) private Map<Long, List<ScheduleEntry>> schedulesForPlatform = new HashMap<>();
	@Shadow(remap = false) private Map<Long, Map<BlockPos, TrainDelay>> trainDelays = new HashMap<>();

    @Shadow(remap = false) @Final @Mutable private static int RAIL_UPDATE_DISTANCE = 128;
	@Shadow(remap = false) private static int PLAYER_MOVE_UPDATE_THRESHOLD = 16;
	@Shadow(remap = false) private static int SCHEDULE_UPDATE_TICKS = 60;

	@Shadow(remap = false) private static int DATA_VERSION = 1;

	@Shadow(remap = false) private static String NAME = "mtr_train_data";
	@Shadow(remap = false) private static String KEY_RAW_MESSAGE_PACK = "raw_message_pack";
	@Shadow(remap = false) private static String KEY_DATA_VERSION = "mtr_data_version";
	@Shadow(remap = false) private static String KEY_STATIONS = "stations";
	@Shadow(remap = false) private static String KEY_PLATFORMS = "platforms";
	@Shadow(remap = false) private static String KEY_SIDINGS = "sidings";
	@Shadow(remap = false) private static String KEY_ROUTES = "routes";
	@Shadow(remap = false) private static String KEY_DEPOTS = "depots";
	@Shadow(remap = false) private static String KEY_LIFTS = "lifts";
	@Shadow(remap = false) private static String KEY_RAILS = "rails";
	@Shadow(remap = false) private static String KEY_SIGNAL_BLOCKS = "signal_blocks";
	@Shadow(remap = false) private static String KEY_USE_TIME_AND_WIND_SYNC = "use_time_and_wind_sync";

    public void simulateTrains() {

        RAIL_UPDATE_DISTANCE = world.getServer().getPlayerList().getViewDistance() * 16;
		List<? extends Player> players = world.players();
		players.forEach(player -> {
			BlockPos playerBlockPos = player.blockPosition();
			Vec3 playerPos = player.position();

			if (!playerLastUpdatedPositions.containsKey(player) || playerLastUpdatedPositions.get(player).distManhattan(playerBlockPos) > PLAYER_MOVE_UPDATE_THRESHOLD) {
				Map<BlockPos, Map<BlockPos, Rail>> railsToAdd = new HashMap<>();
				rails.forEach((startPos, blockPosRailMap) -> blockPosRailMap.forEach((endPos, rail) -> {
					if (((RailExtraSupplier) (Object) rail).isBetween(playerPos.x, playerPos.y, playerPos.z, RAIL_UPDATE_DISTANCE)) {
						if (!railsToAdd.containsKey(startPos)) {
							railsToAdd.put(startPos, new HashMap<>());
						}
						railsToAdd.get(startPos).put(endPos, rail);
					}
				}));

				FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
				packet.writeInt(railsToAdd.size());
				railsToAdd.forEach((posStart, railMap) -> {
					packet.writeBlockPos(posStart);
					packet.writeInt(railMap.size());
					railMap.forEach((posEnd, rail) -> {
						packet.writeBlockPos(posEnd);
						rail.writePacket(packet);
					});
				});

				if (packet.readableBytes() <= MAX_PACKET_BYTES) {
					Registry.sendToPlayer((ServerPlayer) player, PACKET_WRITE_RAILS, packet);
				}
				playerLastUpdatedPositions.put(player, playerBlockPos);
			}
		});

		updateNearbyTrains.startTick();
		trainPositions.remove(0);
		trainPositions.add(new HashMap<>());
		schedulesForPlatform.clear();
		signalBlocks.resetOccupied();
		sidings.forEach(siding -> {
			siding.setSidingData(world, dataCache.sidingIdToDepot.get(siding.id), rails);
			siding.simulateTrain(dataCache, railwayDataDriveTrainModule, trainPositions, signalBlocks, updateNearbyTrains.newDataSetInPlayerRange, updateNearbyTrains.dataSetToSync, schedulesForPlatform, trainDelays);
		});
		depots.forEach(depot -> depot.deployTrain((RailwayData)(Object) this, world));

		updateNearbyLifts.startTick();
		lifts.forEach(lift -> lift.tickServer(world, updateNearbyLifts.newDataSetInPlayerRange, updateNearbyLifts.dataSetToSync));

		railwayDataCoolDownModule.tick();
		railwayDataDriveTrainModule.tick();
		railwayDataRailActionsModule.tick();
		railwayDataRouteFinderModule.tick();
		updateNearbyTrains.tick();
		if (sidings.size() > 0) {
			
		}
		updateNearbyLifts.tick();

		if (MTR.isGameTickInterval(SCHEDULE_UPDATE_TICKS)) {
			players.forEach(player -> {
				if (!playersToSyncSchedules.contains(player)) {
					playersToSyncSchedules.add(player);
				}
			});
		}
		if (!playersToSyncSchedules.isEmpty()) {
			Player player = playersToSyncSchedules.remove(0);
			BlockPos playerBlockPos = player.blockPosition();
			Vec3 playerPos = player.position();

			Set<Long> platformIds = platforms.stream().filter(platform -> {
				if (platform.isCloseToSavedRail(playerBlockPos, PLAYER_MOVE_UPDATE_THRESHOLD, PLAYER_MOVE_UPDATE_THRESHOLD, PLAYER_MOVE_UPDATE_THRESHOLD)) {
					return true;
				}
				Station station = dataCache.platformIdToStation.get(platform.id);
				return station != null && station.inArea(playerBlockPos.getX(), playerBlockPos.getZ());
			}).map(platform -> platform.id).collect(Collectors.toSet());

			Set<UUID> railsToAdd = new HashSet<>();
			rails.forEach((startPos, blockPosRailMap) -> blockPosRailMap.forEach((endPos, rail) -> {
				if (((RailExtraSupplier) (Object) rail).isBetween(playerPos.x, playerPos.y, playerPos.z, RAIL_UPDATE_DISTANCE)) {
					railsToAdd.add(PathData.getRailProduct(startPos, endPos));
				}
			}));
			Map<Long, Boolean> signalBlockStatus = new HashMap<>();
			Map<UUID, Boolean> occupiedRails = new HashMap<>();
			railsToAdd.forEach(rail -> {
				signalBlocks.getSignalBlockStatus(signalBlockStatus, rail);
				occupiedRails.put(rail, trainPositions.get(1).containsKey(rail));
			});

			if (!platformIds.isEmpty() || !signalBlockStatus.isEmpty() || !occupiedRails.isEmpty()) {
				FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
				packet.writeInt(platformIds.size());
				platformIds.forEach(platformId -> {
					packet.writeLong(platformId);
					List<ScheduleEntry> scheduleEntries = schedulesForPlatform.get(platformId);
					if (scheduleEntries == null) {
						packet.writeInt(0);
					} else {
						packet.writeInt(scheduleEntries.size());
						scheduleEntries.forEach(scheduleEntry -> scheduleEntry.writePacket(packet));
					}
				});

				packet.writeInt(signalBlockStatus.size());
				signalBlockStatus.forEach((id, occupied) -> {
					packet.writeLong(id);
					packet.writeBoolean(occupied);
				});

				packet.writeInt(occupiedRails.size());
				occupiedRails.forEach((rail, occupied) -> {
					packet.writeUUID(rail);
					packet.writeBoolean(occupied);
				});

				if (packet.readableBytes() <= MAX_PACKET_BYTES) {
					Registry.sendToPlayer((ServerPlayer) player, PACKET_UPDATE_SCHEDULE, packet);
				}
			}
		}

		if (prevPlatformCount != platforms.size() || prevSidingCount != sidings.size()) {
			dataCache.sync();
		}
		prevPlatformCount = platforms.size();
		prevSidingCount = sidings.size();

		railwayDataFileSaveModule.autoSaveTick();
	}
}