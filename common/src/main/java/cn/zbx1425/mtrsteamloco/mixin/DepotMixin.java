package cn.zbx1425.mtrsteamloco.mixin;

import mtr.data.*;
import mtr.path.*;
import mtr.packet.*;
import cn.zbx1425.mtrsteamloco.data.IRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import cn.zbx1425.mtrsteamloco.path.DepotPathGen;

import java.util.*;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Depot.class)
public class DepotMixin {

    @Inject(method = "generateMainRoute", remap = false, at = @At("HEAD"), cancellable = true)
    private void generateMainRoute(MinecraftServer minecraftServer, Level world, DataCache dataCache, Map<BlockPos, Map<BlockPos, Rail>> rails, Set<Siding> sidings, Consumer<Thread> callback, CallbackInfo ci) {
        System.out.println("DepotMixin generateMainRoute");
		ci.cancel();

		DepotPathGen.generateMainRoute(minecraftServer, world, dataCache, rails, sidings, callback, (Depot) (Object) this);
	}
}