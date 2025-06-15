package cn.zbx1425.mtrsteamloco.mixin;

import mtr.data.RailwayDataDriveTrainModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import mtr.data.*;
import cn.zbx1425.mtrsteamloco.data.TrainExtraSupplier;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Siding.class)
public abstract class SidingMixin {

    // @Inject(method = "simulateTrain", at = @At("TAIL"), remap = false)
    private void __onSimulateTrain(DataCache dataCache, RailwayDataDriveTrainModule railwayDataDriveTrainModule, List<Map<UUID, Long>> trainPositions, SignalBlocks signalBlocks, Map<Player, Set<TrainServer>> trainsInPlayerRange, Set<TrainServer> trainsToSync, Map<Long, List<ScheduleEntry>> schedulesForPlatform, Map<Long, Map<BlockPos, TrainDelay>> trainDelays, CallbackInfo ci) {
        for (TrainServer train : trainsToSync) {
            if (!((TrainExtraSupplier) train).isConfigsChanged()) continue;
            trainsToSync.add(train);
            ((TrainExtraSupplier) train).isConfigsChanged(false);
        }
    }
}