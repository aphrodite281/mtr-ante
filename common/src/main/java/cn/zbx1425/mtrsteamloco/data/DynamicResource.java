package cn.zbx1425.mtrsteamloco.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import java.io.ByteArrayInputStream;
import net.minecraft.server.packs.AbstractPackResources;
#if MC_VERSION >= "11903"
import net.minecraft.server.packs.resources.IoSupplier;
#endif

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;

public class DynamicResource {
    private static MultiPackResourceManager MPRM = null;
    private static DynamicPack DYNAMIC_PACK = null;
    private static Set<String> ADDED_NAMESPACES = new HashSet<>();

    public static void addResourcesClient(ResourceLocation loc, IoSupplier<InputStream> funGetStream) {
        MultiPackResourceManager mprm = (MultiPackResourceManager) (Object) ((ReloadableResourceManager) (Object) Minecraft.getInstance().getResourceManager()).resources;
        if (MPRM == null || MPRM != mprm) {
            MPRM = mprm;
            if (DYNAMIC_PACK != null && MPRM.packs.contains(DYNAMIC_PACK)) {
                MPRM.packs.remove(DYNAMIC_PACK);
            }
            DYNAMIC_PACK = new DynamicPack("ANTE Virtual Dynamic Pack", "{\"pack\":{\"pack_format\":8,\"description\":\"ANTE Virtual Dynamic Pack\"}}");

            MPRM.packs = new ArrayList<>(MPRM.packs);
            MPRM.packs.add(DYNAMIC_PACK);
            ADDED_NAMESPACES = new HashSet<>();
        }
        DYNAMIC_PACK.addResoure(PackType.CLIENT_RESOURCES, loc, funGetStream);
        Set<String> current = new HashSet<>(DYNAMIC_PACK.getNamespaces(PackType.CLIENT_RESOURCES));
        current.removeAll(ADDED_NAMESPACES);
        for (String ns : current) {
            if (!ns.isEmpty()) {
                MPRM.namespacedManagers.computeIfAbsent(ns, k -> new FallbackResourceManager(PackType.CLIENT_RESOURCES, k))
            #if MC_VERSION >= "11900"
                    .push(DYNAMIC_PACK);
            #else
                    .add(DYNAMIC_PACK);
            #endif
            }
        }
        ADDED_NAMESPACES = new HashSet<>(DYNAMIC_PACK.getNamespaces(PackType.CLIENT_RESOURCES));
    }

    private static class DynamicPack implements PackResources {
        private final String name, pack_mcmeta;
        private final Map<PackType, Map<ResourceLocation, IoSupplier<InputStream>>> resources = new HashMap<>();
        private final Map<PackType, Set<String>> namespaces = new HashMap<>();

        public DynamicPack(String name, String pack_mcmeta) {
            this.name = name;
            this.pack_mcmeta = pack_mcmeta;
        }

        public void addResoure(PackType packType, ResourceLocation loc, IoSupplier<InputStream> funGetStream) {
            resources.computeIfAbsent(packType, k -> new HashMap<>()).put(loc, funGetStream);
            namespaces.computeIfAbsent(packType, k -> new HashSet<>()).add(loc.getNamespace());
        }
    #if MC_VERSION < "11903"
        @Override
        public InputStream getRootResource(String fileName) {
            return null;
        }

        @Override
        public InputStream getResource(PackType packType, ResourceLocation loc) throws IOException {
            Map<ResourceLocation, IoSupplier<InputStream>> map = resources.get(packType);
            if (map == null) {
                throw new FileNotFoundException("Resource not found: " + loc);
            }
            return map.get(loc).get();
        }

        @Override
        public Collection<ResourceLocation> getResources(PackType type, String namespace, String path, int maxDepth, Predicate<String> filter) {
            String[] strings;
            String lpath;
            Map<ResourceLocation, IoSupplier<InputStream>> map = resources.get(type);
            if (map == null) return new ArrayList<>();
            List<ResourceLocation> list = new ArrayList<>();
            for (ResourceLocation loc : map.keySet()) {
                if (!loc.getNamespace().equals(namespace)) continue;

                if (!(lpath = loc.getPath()).startsWith(path) || (strings = lpath.split("/")).length < maxDepth + 1 || !filter.test(strings[strings.length - 1])) continue;
                list.add(new ResourceLocation(namespace, lpath));
            }
            return list;
        }

        @Override
        public boolean hasResource(PackType packType, ResourceLocation loc) {
            Map<ResourceLocation, IoSupplier<InputStream>> map = resources.get(packType);
            if (map == null) return false;
            return map.containsKey(loc);
        }

        @Override
        public String getName() {
            return name;
        }
    
    #else

      #if MC_VERSION < "12000"
            
        @Override
        public boolean isBuiltin() {
            return true;
        }

      #endif
        @Override
        public IoSupplier<InputStream> getRootResource(String... fileName) {
            return null;
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation loc) {
            Map<ResourceLocation, IoSupplier<InputStream>> map = resources.get(packType);
            if (map != null) return null;
            return map.get(loc);
        }

        @Override
        public String packId() {
            return name;
        }

        @Override
        public void listResources(PackType type, String namespace, String path, PackResources.ResourceOutput output) {
            Map<ResourceLocation, IoSupplier<InputStream>> map = resources.get(type);
            if (map == null) return;
            for (Map.Entry<ResourceLocation, IoSupplier<InputStream>> entry : map.entrySet()) {
                ResourceLocation loc = entry.getKey();
                if (!loc.getNamespace().equals(namespace)) continue;
                if (!loc.getPath().startsWith(path)) continue;
                output.accept(loc, entry.getValue());
            }
        }
    #endif

        @Override
        public Set<String> getNamespaces(PackType packType) {
            return namespaces.getOrDefault(packType, new HashSet<>());
        }

        @Override
        public void close() {

        }

        @Override
        public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
            if (pack_mcmeta == null) return null;
            return AbstractPackResources.getMetadataFromStream(deserializer, new ByteArrayInputStream(pack_mcmeta.getBytes()));
        }
    }

#if MC_VERSION < "11903"
    @FunctionalInterface
    public static interface IoSupplier<T> {
        T get() throws IOException;
    }
#endif
}