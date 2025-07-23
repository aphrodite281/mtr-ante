package cn.zbx1425.mtrsteamloco.mixin;

import mtr.data.*;
import mtr.path.*;
import mtr.packet.*;
import cn.zbx1425.mtrsteamloco.data.IRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

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

    @Inject(method = "generateMainRoute", remap = false, at = @At("HEAD"))
    private void generateMainRoute(MinecraftServer minecraftServer, Level world, DataCache dataCache, Map<BlockPos, Map<BlockPos, Rail>> rails, Set<Siding> sidings, Consumer<Thread> callback, CallbackInfo ci) {
        System.out.println("DepotMixin generateMainRoute");

		final List<SavedRailBase> platformsMerged = new ArrayList<>();
		final List<List<SavedRailBase>> platformsInRoute = new ArrayList<>();
        final List<List<PathData>> pathsInRoute = new ArrayList<>();
		final Depot depot = (Depot) (Object) this;

		depot.routeIds.forEach(routeId -> {
			final Route route = dataCache.routeIdMap.get(routeId);
			if (route != null) {
                List<PathData> routePath = ((IRoute) (Object) route).getPathData();
				List<SavedRailBase> routePlatforms = new ArrayList<>();
                route.platformIds.forEach(platformId -> {
					final Platform platform = dataCache.platformIdMap.get(platformId.platformId);
					if (platform != null && (routePlatforms.isEmpty() || platform.id != routePlatforms.get(routePlatforms.size() - 1).id)) {
						routePlatforms.add(platform);
					}
					if (platform != null && (platformsMerged.isEmpty() || platform.id != platformsMerged.get(platformsMerged.size() - 1).id)) {
						platformsMerged.add(platform);
					}
				});
				platformsInRoute.add(routePlatforms);
				pathsInRoute.add(routePath);
			}
		});

		final int cruisingAltitude = depot.cruisingAltitude;
		final boolean useFastSpeed = cruisingAltitude >= world.getMaxBuildHeight() + 64;
		final long id = depot.id;
		final String name = depot.name;

		final Thread thread = new Thread(() -> {
			try {
				final List<PathData> tempPath = new ArrayList<>();
				int stopIndex = 1;
				for (int i = 0; i < pathsInRoute.size(); i++) {
					List<PathData> routePath = pathsInRoute.get(i);
					List<SavedRailBase> platforms = platformsInRoute.get(i);
					if (routePath.isEmpty()) {
						PathFinder.findPath(routePath, rails, platforms, stopIndex, cruisingAltitude, useFastSpeed);
					} else {
						routePath = applyOffset(routePath, stopIndex);
					}
					stopIndex += platforms.size();
					tempPath.addAll(routePath);
				}
				final int[] successfulSegments = new int[]{Integer.MAX_VALUE};
				final int successfulSegmentsMain = stopIndex;
				sidings.forEach(siding -> {
					final BlockPos sidingMidPos = siding.getMidPos();
					if (siding.isTransportMode(depot.transportMode) && depot.inArea(sidingMidPos.getX(), sidingMidPos.getZ())) {
						final SavedRailBase firstPlatform = platformsMerged.isEmpty() ? null : platformsMerged.get(0);
						final SavedRailBase lastPlatform = platformsMerged.isEmpty() ? null : platformsMerged.get(platformsMerged.size() - 1);
						final int result = siding.generateRoute(minecraftServer, tempPath, successfulSegmentsMain, rails, firstPlatform, lastPlatform, depot.repeatInfinitely, cruisingAltitude, useFastSpeed);
						if (result < successfulSegments[0]) {
							successfulSegments[0] = result;
						}
					}
				});

				PacketTrainDataGuiServer.generatePathS2C(world, id, successfulSegments[0]);
				System.out.println("Finished path generation" + (name.isEmpty() ? "" : " for " + name));
			} catch (Exception e) {
				e.printStackTrace();
				PacketTrainDataGuiServer.generatePathS2C(world, id, 0);
				System.out.println("Failed to generate path" + (name.isEmpty() ? "" : " for " + name));
			}
		});
		callback.accept(thread);
		thread.start();
	}

	private List<PathData> applyOffset(List<PathData> pathData, int stopIndexOffset) {
		final List<PathData> newPathData = new ArrayList<>();
		for (PathData path : pathData) {
			newPathData.add(new PathData(path.rail, path.savedRailBaseId, path.dwellTime, path.startingPos, ((PathDataAccessor) (Object) path).getEndingPos(), path.stopIndex + stopIndexOffset));
		}
		return newPathData;
	}
}