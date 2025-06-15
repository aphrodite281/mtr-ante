package cn.zbx1425.mtrsteamloco.mixin;

import mtr.data.Depot;
import net.minecraft.world.level.Level;
import mtr.data.Train;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Train.class)
public interface TrainAccessor {

    @Accessor(value = "distances", remap = false)
    List<Double> getDistances();

//     @Invoker(remap = false) @Final @Mutable
//     void invokeSimulateTrain(Level world, float ticksElapsed, Depot depot);
}
