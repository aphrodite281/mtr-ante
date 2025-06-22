package cn.zbx1425.sowcerext.util;

import cn.zbx1425.sowcer.batch.MaterialProp;
import mtr.mappings.Utilities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.*;
import java.util.Locale;

public class ResourceUtil {

    public static String readResource(ResourceManager manager, ResourceLocation location) throws IOException {
#if MC_VERSION >= "11902"
        final List<Resource> resources = manager.getResourceStack(location);
#else
        final List<Resource> resources = manager.getResources(location);
#endif
        if (resources.isEmpty()) return "";
        return IOUtils.toString(new BOMInputStream(Utilities.getInputStream(resources.get(0))), StandardCharsets.UTF_8);
    }

    public static ResourceLocation resolveRelativePath(ResourceLocation baseFile, String relative, String expectExtension) {
        relative = relative.toLowerCase(Locale.ROOT).replace('\\', '/');

        if (relative.contains(":")) {
            relative = relative.replaceAll("[^a-z0-9/.:_-]", "_");
            return new ResourceLocation(relative);
        }

        relative = relative.replaceAll("[^a-z0-9/._-]", "_");

        if (relative.endsWith(".jpg") || relative.endsWith(".bmp") || relative.endsWith(".tga")) {
            relative = relative.substring(0, relative.length() - 4) + ".png";
        }

        if (expectExtension != null && !relative.endsWith(expectExtension)) {
            relative += expectExtension;
        }

        String base = baseFile.getPath();
        String[] baseParts = base.split("/");
        String[] relativeParts = relative.split("/");
        Deque<String> stack = new LinkedList<>();
        for (String part : baseParts) {
            if (part.isEmpty()) throw new IllegalArgumentException("Invalid base file path: " + base);
            stack.push(part);
        }
        stack.pop();
        for (String part : relativeParts) {
            if (part.equals(".")) continue;
            if (part.equals("..")) {
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("Out of range: " + relative + " relative to "   + base);
                } else {
                    stack.pop();
                }
                continue;
            }
            stack.push(part);
        }
        StringBuilder sb = new StringBuilder();
        while (!stack.isEmpty()) {
            sb.append(stack.removeLast()).append("/");
        }
        String path = sb.toString();
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

        return new ResourceLocation(baseFile.getNamespace(), path);
    }
}
