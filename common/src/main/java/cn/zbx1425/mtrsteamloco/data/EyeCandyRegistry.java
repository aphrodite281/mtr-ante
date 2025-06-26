package cn.zbx1425.mtrsteamloco.data;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.render.integration.MtrModelRegistryUtil;
import cn.zbx1425.mtrsteamloco.scripting.ScriptHolderBase;
import cn.zbx1425.mtrsteamloco.scripting.ScriptHolderClient;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.sowcerext.model.RawModel;
import cn.zbx1425.sowcerext.util.ResourceUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import mtr.mappings.Text;
import mtr.mappings.Utilities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import cn.zbx1425.mtrsteamloco.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EyeCandyRegistry {

    public static final Map<String, EyeCandyProperties> ELEMENTS = new HashMap<>();
    public static final Map<String, EyeCandyProperties> PATH_MAP = new HashMap<>();
    public static Tree.Root<EyeCandyProperties> TREE = new Tree.Root<>("block.mtrsteamloco.eye_candy");

    public static void register(String key, EyeCandyProperties properties) {
        ELEMENTS.put(key, properties);
        PATH_MAP.put(properties.path, properties);
    }

    public static void reload(ResourceManager resourceManager) {
        ELEMENTS.clear();
        PATH_MAP.clear();
        List<Pair<ResourceLocation, Resource>> resources =
                MtrModelRegistryUtil.listResources(resourceManager, "mtrsteamloco", "eyecandies", ".json");
        for (Pair<ResourceLocation, Resource> pair : resources) {
            try {
                try (InputStream is = Utilities.getInputStream(pair.getSecond())) {
                    JsonObject rootObj = (new JsonParser()).parse(IOUtils.toString(is, StandardCharsets.UTF_8)).getAsJsonObject();
                #if MC_VERSION >= "11902"
                    String baseGroup = rootObj.has("group") ? rootObj.get("group").getAsString() : pair.getSecond().sourcePackId() + '/' + pair.getFirst().getPath().replaceAll("eyecandies/", "").replaceAll(".json", "");
                #else
                    String baseGroup = rootObj.has("group") ? rootObj.get("group").getAsString() : pair.getSecond().getSourceName() + '/' + pair.getSecond().getLocation().getPath().replaceAll("eyecandies/", "").replaceAll(".json", "");
                #endif
                    if (rootObj.has("model")) {
                        String key = FilenameUtils.getBaseName(pair.getFirst().getPath());
                        register(key, loadFromJson(resourceManager, key, rootObj, baseGroup));
                    } else {
                        for (Map.Entry<String, JsonElement> entry : rootObj.entrySet()) {
                            if (!entry.getValue().isJsonObject()) continue;
                            JsonObject obj = entry.getValue().getAsJsonObject();
                            String key = entry.getKey().toLowerCase(Locale.ROOT);
                            register(key, loadFromJson(resourceManager, key, obj, baseGroup));
                        }
                    }
                }
            } catch (Exception ex) {
                Main.LOGGER.error("Failed loading eye-candy: " + pair.getFirst().toString(), ex);
                MtrModelRegistryUtil.recordLoadingError("Failed loading Eye-candy " + pair.getFirst().toString(), ex);
            }
        }

        TREE = Tree.loadTree("block.mtrsteamloco.eye_candy", PATH_MAP, t -> t.name.getString());
    }

    public static EyeCandyProperties getProperty(String key) {
        return ELEMENTS.getOrDefault(key, null);
    }

    private static EyeCandyProperties loadFromJson(ResourceManager resourceManager, String key, JsonObject obj, String group) throws Exception {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Invalid eye-candy key: " + key + " (empty)");
        }

        if (key.contains("/")) {
            throw new IllegalArgumentException("Invalid eye-candy key: " + key + " (contains /)");
        }

        if (obj.has("atlasIndex")) {
            MainClient.atlasManager.load(
                    MtrModelRegistryUtil.resourceManager,  new ResourceLocation(obj.get("atlasIndex").getAsString())
            );
        }

        int lightLevel = 0;
        if (obj.has("lightLevel")) {
            lightLevel = obj.get("lightLevel").getAsInt();
        }

        ModelCluster cluster = null;
        if (obj.has("model")) {
            RawModel rawModel = MainClient.modelManager.loadRawModel(resourceManager,
                    new ResourceLocation(obj.get("model").getAsString()), MainClient.atlasManager).copy();

            if (obj.has("textureId")) {
                rawModel.replaceTexture("default.png", new ResourceLocation(obj.get("textureId").getAsString()));
            }
            if (obj.has("flipV") && obj.get("flipV").getAsBoolean()) {
                rawModel.applyUVMirror(false, true);
            }

            if (obj.has("translation")) {
                JsonArray vec = obj.get("translation").getAsJsonArray();
                rawModel.applyTranslation(vec.get(0).getAsFloat(), vec.get(1).getAsFloat(), vec.get(2).getAsFloat());
            }
            if (obj.has("rotation")) {
                JsonArray vec = obj.get("rotation").getAsJsonArray();
                rawModel.applyRotation(new Vector3f(1, 0, 0), vec.get(0).getAsFloat());
                rawModel.applyRotation(new Vector3f(0, 1, 0), vec.get(1).getAsFloat());
                rawModel.applyRotation(new Vector3f(0, 0, 1), vec.get(2).getAsFloat());
            }
            if (obj.has("scale")) {
                JsonArray vec = obj.get("scale").getAsJsonArray();
                rawModel.applyScale(vec.get(0).getAsFloat(), vec.get(1).getAsFloat(), vec.get(2).getAsFloat());
            }
            if (obj.has("mirror")) {
                JsonArray vec = obj.get("mirror").getAsJsonArray();
                rawModel.applyMirror(
                        vec.get(0).getAsBoolean(), vec.get(1).getAsBoolean(), vec.get(2).getAsBoolean(),
                        vec.get(0).getAsBoolean(), vec.get(1).getAsBoolean(), vec.get(2).getAsBoolean()
                );
            }

            rawModel.sourceLocation = new ResourceLocation(rawModel.sourceLocation.toString() + "/" + key);

            cluster = MainClient.modelManager.uploadVertArrays(rawModel);
        }
        ScriptHolderBase script = null;
        if (obj.has("scriptFiles")) {
            script = new ScriptHolderClient();
            Map<ResourceLocation, String> scripts = new Object2ObjectArrayMap<>();
            if (obj.has("scriptTexts")) {
                JsonArray scriptTexts = obj.get("scriptTexts").getAsJsonArray();
                for (int i = 0; i < scriptTexts.size(); i++) {
                    scripts.put(new ResourceLocation("mtrsteamloco", "script_texts/" + key + "/" + i),
                            scriptTexts.get(i).getAsString());
                }
            }
            JsonArray scriptFiles = obj.get("scriptFiles").getAsJsonArray();
            for (int i = 0; i < scriptFiles.size(); i++) {
                ResourceLocation scriptLocation = new ResourceLocation(scriptFiles.get(i).getAsString());
                scripts.put(scriptLocation, ResourceUtil.readResource(resourceManager, scriptLocation));
            }
            script.load("EyeCandy " + key, "Block", resourceManager, scripts, obj, key, "create", "render", "dispose", "use");
        }
        String shape = obj.has("shape")? obj.get("shape").getAsString() : "0, 0, 0, 16, 16, 16";
        String collisionShape = obj.has("collisionShape") ? obj.get("collisionShape").getAsString() : "0, 0, 0, 0, 0, 0";
        boolean fixedMatrix = obj.has("fixedMatrix") ? obj.get("fixedMatrix").getAsBoolean() : false;
        boolean isTicketBarrier = obj.has("isTicketBarrier") ? obj.get("isTicketBarrier").getAsBoolean() : false;
        boolean isEntrance = obj.has("isEntrance") ? obj.get("isEntrance").getAsBoolean() : false;
        boolean asPlatform = obj.has("asPlatform") ? obj.get("asPlatform").getAsBoolean() : false;
        group = obj.has("group") ? obj.get("group").getAsString() : group;
        if (cluster == null && script == null) {
            throw new IllegalArgumentException("Invalid eye-candy json: " + key);
        } else {
            return new EyeCandyProperties(key, Text.translatable(obj.get("name").getAsString()), cluster, script, shape, collisionShape, fixedMatrix, lightLevel, isTicketBarrier, isEntrance, asPlatform, group);
        }
    }
}
