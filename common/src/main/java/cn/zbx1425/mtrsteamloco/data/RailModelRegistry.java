package cn.zbx1425.mtrsteamloco.data;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.render.integration.MtrModelRegistryUtil;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.sowcer.model.Model;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.sowcerext.model.RawModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import mtr.mappings.Text;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import mtr.mappings.Utilities;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import cn.zbx1425.mtrsteamloco.scripting.ScriptHolderBase;
import cn.zbx1425.mtrsteamloco.scripting.ScriptHolderClient;
import net.minecraft.server.packs.resources.ResourceManager;
import cn.zbx1425.sowcerext.util.ResourceUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RailModelRegistry {

    public static Map<String, RailModelProperties> ELEMENTS = new HashMap<>();
    public static final Map<String, RailModelProperties> PATH_MAP = new HashMap<>();
    public static Tree.Root<RailModelProperties> TREE = new Tree.Root<>("rail.mtrsteamloco.type");

    public static ModelCluster railNodeModel;
    
    public static void register(String key, RailModelProperties properties) {
        if (properties == null) return;
        ELEMENTS.put(key, properties);
        PATH_MAP.put(properties.path, properties);
    }

    public static void reload(ResourceManager resourceManager) {
        ELEMENTS.clear();
        PATH_MAP.clear();
        TREE = new Tree.Root<>("rail.mtrsteamloco.type");

        //
        register("", new RailModelProperties("", Text.translatable("rail.mtrsteamloco.default"), null, 1f, 0f, null, "group.mtrsteamloco.builtin"));
        // This is pulled from registry and shouldn't be shown
        register("null", new RailModelProperties("null", Text.translatable("rail.mtrsteamloco.hidden"), null, Float.MAX_VALUE, 0f, null, "group.mtrsteamloco.builtin"));

        try {
            RawModel railNodeRawModel = MainClient.modelManager.loadRawModel(resourceManager,
                    new ResourceLocation("mtrsteamloco:models/rail_node.csv"), MainClient.atlasManager);
            railNodeModel = MainClient.modelManager.uploadVertArrays(railNodeRawModel);
        } catch (Exception ex) {
            Main.LOGGER.error("Failed loading rail node model", ex);
            MtrModelRegistryUtil.recordLoadingError("Failed loading Rail Node", ex);
        }

        List<Pair<ResourceLocation, Resource>> resources =
                MtrModelRegistryUtil.listResources(resourceManager, "mtrsteamloco", "rails", ".json");
        for (Pair<ResourceLocation, Resource> pair : resources) {
            try {
                try (InputStream is = Utilities.getInputStream(pair.getSecond())) {
                    JsonObject rootObj = (new JsonParser()).parse(IOUtils.toString(is, StandardCharsets.UTF_8)).getAsJsonObject();
                #if MC_VERSION >= "11902"
                    String baseGroup = rootObj.has("group") ? rootObj.get("group").getAsString() : pair.getSecond().sourcePackId() + '/' + pair.getFirst().getPath().replaceAll("rails/", "").replaceAll(".json", "");
                #else 
                    String baseGroup = rootObj.has("group") ? rootObj.get("group").getAsString() : pair.getSecond().getSourceName() + '/' + pair.getSecond().getLocation().getPath().replaceAll("rails/", "").replaceAll(".json", "");
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
                Main.LOGGER.error("Failed loading rail: " + pair.getFirst().toString(), ex);
                MtrModelRegistryUtil.recordLoadingError("Failed loading Rail " + pair.getFirst().toString(), ex);
            }
        }

        MainClient.railRenderDispatcher.clearRail();
        TREE = Tree.loadTree("rail.mtrsteamloco.type", PATH_MAP, t -> t.name.getString());
    }

    private static final RailModelProperties EMPTY_PROPERTY = new RailModelProperties(
            "null", Text.literal(""), null, 1f, 0, null, "group.mtrsteamloco.builtin"
    );

    public static RailModelProperties getProperty(String key) {
        return ELEMENTS.getOrDefault(key, EMPTY_PROPERTY);
    }

    private static RailModelProperties loadFromJson(ResourceManager resourceManager, String key, JsonObject obj, String baseGroup) throws Exception {
        if (obj.has("atlasIndex")) {
            MainClient.atlasManager.load(
                    MtrModelRegistryUtil.resourceManager,  new ResourceLocation(obj.get("atlasIndex").getAsString())
            );
        }
        
        RawModel rawModel = null;

        if (obj.has("model")) {
            rawModel = MainClient.modelManager.loadRawModel(resourceManager,
                    new ResourceLocation(obj.get("model").getAsString()), MainClient.atlasManager).copy();

            if (obj.has("textureId")) {
                rawModel.replaceTexture("default.png", new ResourceLocation(obj.get("textureId").getAsString()));
            }
            if (obj.has("flipV") && obj.get("flipV").getAsBoolean()) {
                rawModel.applyUVMirror(false, true);
            }

            rawModel.sourceLocation = new ResourceLocation(rawModel.sourceLocation.toString() + "/" + key);
        }
        
        float repeatInterval = obj.has("repeatInterval") ? obj.get("repeatInterval").getAsFloat() : 0.5f;
        float yOffset = obj.has("yOffset") ? obj.get("yOffset").getAsFloat() : 0f;


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
            script.load("Rail " + key, "Rail", resourceManager, scripts, obj, key, "create", "render", "dispose");
        }
        String group = obj.has("group") ? obj.get("group").getAsString() : baseGroup;

        return new RailModelProperties(key, Text.translatable(obj.get("name").getAsString()), rawModel, repeatInterval, yOffset, script, group);
    }
}
