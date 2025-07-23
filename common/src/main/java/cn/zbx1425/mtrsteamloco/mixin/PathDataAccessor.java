package cn.zbx1425.mtrsteamloco.mixin;

import mtr.path.PathData;
import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PathData.class)
public interface PathDataAccessor {
    @Accessor(remap = false) @Final
    BlockPos getEndingPos();

    @Accessor(remap = false) @Final @Mutable
    void setDwellTime(int dwellTime);

    @Accessor(remap = false) @Final @Mutable
    void setStopIndex(int dwellTime);

    @Accessor(remap = false) @Final @Mutable
    void setSavedRailBaseId(long savedRailBaseId);
}