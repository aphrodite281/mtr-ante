package cn.zbx1425.mtrsteamloco.network;

import cn.zbx1425.mtrsteamloco.Main;
import io.netty.buffer.Unpooled;
import mtr.RegistryClient;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import mtr.data.ScheduleEntry;
import mtr.data.RailwayData;
import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy;
import cn.zbx1425.mtrsteamloco.network.util.Serializer;
import cn.zbx1425.mtrsteamloco.data.Schedule;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class PacketUpdateBlockEntity {

    public static ResourceLocation PACKET_UPDATE_BLOCK_ENTITY = new ResourceLocation(Main.MOD_ID, "update_block_entity");

    public static void sendUpdateC2S(BlockEntityMapper blockEntity) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeResourceLocation(level.dimension().location());
        packet.writeBlockPos(blockEntity.getBlockPos());
#if MC_VERSION >= "11903"
        packet.writeId(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE, blockEntity.getType());
#else
        packet.writeVarInt(net.minecraft.core.Registry.BLOCK_ENTITY_TYPE.getId(blockEntity.getType()));
#endif
        CompoundTag tag = new CompoundTag();
        blockEntity.writeCompoundTag(tag);
        packet.writeNbt(tag);

        RegistryClient.sendToServer(PACKET_UPDATE_BLOCK_ENTITY, packet);
    }

    public static void receiveUpdateC2S(MinecraftServer server, ServerPlayer player, FriendlyByteBuf packet) {
#if MC_VERSION >= "11903"
        ResourceKey<Level> levelKey = packet.readResourceKey(net.minecraft.core.registries.Registries.DIMENSION);
#else
        ResourceKey<Level> levelKey = ResourceKey.create(net.minecraft.core.Registry.DIMENSION_REGISTRY, packet.readResourceLocation());
#endif
        BlockPos blockPos = packet.readBlockPos();
#if MC_VERSION >= "11903"
        BlockEntityType<?> blockEntityType = packet.readById(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE);
#else
        BlockEntityType<?> blockEntityType = net.minecraft.core.Registry.BLOCK_ENTITY_TYPE.byId(packet.readVarInt());
#endif

        CompoundTag compoundTag = packet.readNbt();

        server.execute(() -> {
            ServerLevel level = server.getLevel(levelKey);
            if (level == null || blockEntityType == null) return;
            level.getBlockEntity(blockPos, blockEntityType).ifPresent(blockEntity -> {
                if (compoundTag != null) {
					RailwayData railwayData = RailwayData.getInstance(level);
                    if (railwayData!= null) {
                        Long platformId = railwayData.getClosePlatformId(railwayData.platforms, railwayData.dataCache, blockPos, 5, 4, 4);
                        if (platformId != null) {
                            compoundTag.putLong("platformId", platformId);
                            List<ScheduleEntry> schedules = railwayData.getSchedulesAtPlatform(platformId);
                            if (schedules != null) {
                                List<Schedule> schedulesList = new ArrayList<>();
                                for (ScheduleEntry scheduleEntry : schedules) {
                                    schedulesList.add(new Schedule(scheduleEntry.arrivalMillis, scheduleEntry.trainCars, scheduleEntry.routeId, scheduleEntry.currentStationIndex, scheduleEntry.arrivalMillis - System.currentTimeMillis()));
                                }
                                try {
                                    compoundTag.putByteArray("schedules", Serializer.serialize(schedulesList));
                                }catch (IOException e) {}
                            }
                        }
                        int ticks = ((BlockEyeCandy.BlockEntityEyeCandy) blockEntity).ticks + 1;
                        compoundTag.putInt("ticks", ticks);
                    }
					
                    blockEntity.load(compoundTag);
                    blockEntity.setChanged();
                    level.getChunkSource().blockChanged(blockPos);
                }
            });
        });
    }
}
