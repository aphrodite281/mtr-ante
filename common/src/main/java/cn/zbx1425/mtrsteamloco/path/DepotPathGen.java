package cn.zbx1425.mtrsteamloco.path;

import mtr.data.*;
import mtr.path.*;
import mtr.packet.*;
import cn.zbx1425.mtrsteamloco.data.IRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import cn.zbx1425.mtrsteamloco.mixin.PathDataAccessor;

import java.util.*;
import java.util.function.Consumer;

public class DepotPathGen {
    public static void generateMainRoute(MinecraftServer minecraftServer, Level world, DataCache dataCache, Map<BlockPos, Map<BlockPos, Rail>> rails, Set<Siding> sidings, Consumer<Thread> callback, Depot depot) {
        final List<SavedRailBase> platformsMerged = new ArrayList<>();
		final List<List<SavedRailBase>> platformsInRoute = new ArrayList<>();
        final List<List<PathData>> pathsInRoute = new ArrayList<>();

		depot.routeIds.forEach(routeId -> {
			final Route route = dataCache.routeIdMap.get(routeId);
			if (route != null) {
                List<PathData> routePath = new ArrayList<>(((IRoute) (Object) route).getPathData());
				List<SavedRailBase> routePlatforms = new ArrayList<>();
				if (routePath.isEmpty()) {
					route.platformIds.forEach(platformId -> {
						final Platform platform = dataCache.platformIdMap.get(platformId.platformId);
						routePlatforms.add(platform);
						if (platform != null && (platformsMerged.isEmpty() || platform.id != platformsMerged.get(platformsMerged.size() - 1).id)) {
							platformsMerged.add(platform);
						}
					});
				} else {
					for (int i = 0; i < route.platformIds.size(); i++) {
						final Platform platform = dataCache.platformIdMap.get(route.platformIds.get(i).platformId);
						if (platform != null) {
							routePlatforms.add(platform);
							if (i != 0 || platformsMerged.isEmpty() || platform.id != platformsMerged.get(platformsMerged.size() - 1).id) {
								platformsMerged.add(platform);
							}
						}
					}
				}

				platformsInRoute.add(routePlatforms);
				pathsInRoute.add(routePath);
			}
		});
        for(List<PathData> lp : pathsInRoute) {
            for (int i = 0; i < lp.size(); i++) {
                lp.set(i, copy(lp.get(i)));
            }
        }

		final int cruisingAltitude = depot.cruisingAltitude;
		final boolean useFastSpeed = cruisingAltitude >= world.getMaxBuildHeight() + 64;
		final long id = depot.id;
		final String name = depot.name;

		final Thread thread = new Thread(() -> {
			try {
				final List<PathData> tempPath = new ArrayList<>();
				SavedRailBase lastPlatform = null;
				for (int i = 0; i < pathsInRoute.size(); i++) {
					List<SavedRailBase> platforms = platformsInRoute.get(i);
					if (lastPlatform != null) {
						if (!platforms.isEmpty() && !(platforms.get(0) == lastPlatform)) {
							List<PathData> temp = new ArrayList<>();
							PathFinder.findPath(temp, rails, Arrays.asList(lastPlatform, platforms.get(0)), 1, cruisingAltitude, useFastSpeed);
							tempPath.addAll(temp);
						}
					}
					if (!platforms.isEmpty()) {
						lastPlatform = platforms.get(platforms.size() - 1);
					}
					List<PathData> routePath = pathsInRoute.get(i);
					if (routePath.isEmpty()) {
						PathFinder.findPath(routePath, rails, platforms, 1, cruisingAltitude, useFastSpeed);
					}
					if (routePath.size() > 2) {
						PathData first = routePath.get(0);
						PathData last = tempPath.isEmpty() ? null : tempPath.get(tempPath.size() - 1);
						if (last != null) {
							if (first.isOppositeRail(last)) {
								((PathDataAccessor) (Object) first).setDwellTime(0);
								((PathDataAccessor) (Object) first).setSavedRailBaseId(0);
							} else if (first.isSameRail(last)) {
								routePath.remove(0);
							}
						}
						tempPath.addAll(routePath);
					}
				}
				int stopIndex = 1;
				for (int i = 1; i < tempPath.size(); i++) {
					PathData curr = tempPath.get(i);
					if (curr.dwellTime != 0 && curr.rail.railType == RailType.PLATFORM && curr.savedRailBaseId != 0 && !(tempPath.get(i - 1).rail.railType == RailType.PLATFORM)) stopIndex++;
					((PathDataAccessor) (Object) curr).setStopIndex(stopIndex);
				}
				final int[] successfulSegments = new int[]{Integer.MAX_VALUE};
				final int successfulSegmentsMain = platformsMerged.size();
				sidings.forEach(siding -> {
					final BlockPos sidingMidPos = siding.getMidPos();
					if (siding.isTransportMode(depot.transportMode) && depot.inArea(sidingMidPos.getX(), sidingMidPos.getZ())) {
						final SavedRailBase firstPlatform = platformsMerged.isEmpty() ? null : platformsMerged.get(0);
						final SavedRailBase TlastPlatform = platformsMerged.isEmpty() ? null : platformsMerged.get(platformsMerged.size() - 1);
						final int result = siding.generateRoute(minecraftServer, tempPath, successfulSegmentsMain, rails, firstPlatform, TlastPlatform, depot.repeatInfinitely, cruisingAltitude, useFastSpeed);
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

    private static PathData copy(PathData pathData) {
        return new PathData(pathData.rail, pathData.savedRailBaseId, pathData.dwellTime, pathData.startingPos, ((PathDataAccessor) (Object) pathData).getEndingPos(), pathData.stopIndex);
    }
}