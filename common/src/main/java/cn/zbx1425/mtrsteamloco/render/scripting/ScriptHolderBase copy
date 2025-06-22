package cn.zbx1425.mtrsteamloco.render.scripting;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.render.scripting.util.*;
import cn.zbx1425.sowcer.math.*;
import cn.zbx1425.sowcerext.util.ResourceUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import vendor.cn.zbx1425.mtrsteamloco.org.mozilla.javascript.*;
import mtr.block.IBlock;
import net.minecraft.world.entity.player.Player;
import cn.zbx1425.mtrsteamloco.data.ShapeSerializer;
import net.minecraft.network.chat.Component;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class ScriptHolderBase {

    private static ExecutorService SCRIPT_THREAD = Executors.newSingleThreadExecutor();

    public final String side;
    private Scriptable scope;
    public final Map<String, List<Function>> functions = new HashMap<>();

    public long failTime = 0;
    public Exception failException = null;

    public String name;
    public String contextTypeName;
    private Map<ResourceLocation, String> scripts;

    private JsonObject config;
    private String key;

    public ScriptHolderBase(String side) {
        this.side = side;
    }

    public void load(String name, String contextTypeName, ResourceManager resourceManager, Map<ResourceLocation, String> scripts, JsonObject config, String key, String... functionNames) throws Exception {
        this.name = name;
        this.contextTypeName = contextTypeName;
        this.scripts = scripts;
        functions.clear();
        this.failTime = 0;
        this.failException = null;
        this.config = config;
        this.key = key;

        Context rhinoCtx = Context.enter();
        rhinoCtx.setLanguageVersion(Context.VERSION_ES6);
        try {
            scope = createImporter(rhinoCtx, config, key);
            appendImporter(scope, rhinoCtx);

            // Run scripts
            ScriptResourceUtil.activeContext = rhinoCtx;
            ScriptResourceUtil.activeScope = scope;
            for (Map.Entry<ResourceLocation, String> entry : scripts.entrySet()) {
                String scriptStr = entry.getValue() == null
                        ? ResourceUtil.readResource(resourceManager, entry.getKey()) : entry.getValue();
                ScriptResourceUtil.executeScript(rhinoCtx, scope, entry.getKey(), scriptStr);
                for (String functionName : functionNames) {
                    acquireFunction(functionName, functionName);
                    acquireFunction(functionName + contextTypeName, functionName);
                }
            }
            ScriptResourceUtil.activeContext = null;
            ScriptResourceUtil.activeScope = null;
        } finally {
            Context.exit();
        }
    }

    protected abstract void appendImporter(Scriptable scope, Context rhinoCtx);

    private ImporterTopLevel createImporter(Context rhinoCtx, JsonObject config, String key) throws Exception {
        ImporterTopLevel scope = new ImporterTopLevel(rhinoCtx);

        scope.put("SIDE", scope, side);

        // Populate Scope with global functions
        scope.put("include", scope, new NativeJavaMethod(
                ScriptResourceUtil.class.getMethod("includeScript", Object.class), "includeScript"));
        scope.put("print", scope, new NativeJavaMethod(
                ScriptResourceUtil.class.getMethod("print", Object[].class), "print"));
        scope.put("asJavaArray", scope, new NativeJavaMethod(
                JsFriendlyJavaUtils.class.getMethod("asJavaArray", List.class, Class.class), "asJavaArray"));

        // scope.put("Resources", scope, new NativeJavaClass(scope, ScriptResourceUtil.class));
        scope.put("Timing", scope, new NativeJavaClass(scope, TimingUtil.class));
        scope.put("StateTracker", scope, new NativeJavaClass(scope, StateTracker.class));
        scope.put("CycleTracker", scope, new NativeJavaClass(scope, CycleTracker.class));
        scope.put("RateLimit", scope, new NativeJavaClass(scope, RateLimit.class));
        scope.put("TextUtil", scope, new NativeJavaClass(scope, TextUtil.class));
        scope.put("GlobalRegister", scope, new NativeJavaClass(scope, GlobalRegister.class));
        scope.put("WrappedEntity", scope, new NativeJavaClass(scope, WrappedEntity.class));
        scope.put("ComponentUtil", scope, new NativeJavaClass(scope, ComponentUtil.class));
        scope.put("OrderedMap", scope, new NativeJavaClass(scope, OrderedMap.class));   
        scope.put("PlacementOrder", scope, new NativeJavaClass(scope, OrderedMap.PlacementOrder.class));
        scope.put("ShapeSerializer", scope, new NativeJavaClass(scope, ShapeSerializer.class));

        scope.put("Matrices", scope, new NativeJavaClass(scope, Matrices.class));
        scope.put("Matrix4f", scope, new NativeJavaClass(scope, Matrix4f.class));
        scope.put("Vector3f", scope, new NativeJavaClass(scope, Vector3f.class));   
        
        scope.put("Component", scope, new NativeJavaClass(scope, Component.class));
        
        scope.put("Optional", scope, new NativeJavaClass(scope, Optional.class));

        JsonObject copy = config;
        if (!copy.has(key)) copy.addProperty("key", key);
        String jsonStr = new GsonBuilder().setPrettyPrinting().create().toJson(copy);
        scope.put("CONFIG_INFO", scope, jsonStr);
        String code = "CONFIG_INFO = JSON.parse(CONFIG_INFO);";
        rhinoCtx.evaluateString(scope, code, "parse CONFIG_INFO", 1, null);
        scope.put("CompoundTag", scope, new NativeJavaClass(scope, CompoundTag.class));
        
        rhinoCtx.evaluateString(scope, "\"use strict\"", "", 1, null);

        return scope;
    }

    public void reload(ResourceManager resourceManager) throws Exception {
        load(name, contextTypeName, resourceManager, scripts, config, key);
    }

    private void acquireFunction(String functionName, String dest) {
        Object jsFunction = scope.get(functionName, scope);
        if (jsFunction != Scriptable.NOT_FOUND) {
            if (jsFunction instanceof Function) {
                List<Function> functions = this.functions.get(functionName);
                if (functions == null) {
                    functions = new ArrayList<>();
                    this.functions.put(functionName, functions);
                }
                functions.add((Function)jsFunction);
            }
            scope.delete(functionName);
        }
    }

    public Future<?> callFunctionAsync(List<Function> functions, AbstractScriptContext scriptCtx, Runnable finishCallback, Object... args) {
        if (functions == null) return null;
        if (duringFailTimeout()) return null;
        failTime = 0;
        return SCRIPT_THREAD.submit(() -> {
            if (Thread.currentThread().isInterrupted()) return;
            Context rhinoCtx = Context.enter();
            if (scriptCtx.state == null) scriptCtx.state = rhinoCtx.newObject(scope);
            try {
                long startTime = System.nanoTime();

                TimingUtil.prepareForScript(scriptCtx);
                Object[] functionParam = new Object[3 + args.length];
                functionParam[0] = scriptCtx;
                functionParam[1] = scriptCtx.state;
                functionParam[2] = scriptCtx.getWrapperObject();
                System.arraycopy(args, 0, functionParam, 3, args.length);

                for (Function function : functions) {
                    function.call(rhinoCtx, scope, scope, functionParam);
                }
                if (finishCallback != null) finishCallback.run();
                scriptCtx.lastExecuteDuration = System.nanoTime() - startTime;
            } catch (Exception ex) {
                Main.LOGGER.error("Error in ANTE Resource Pack JavaScript", ex);
                failTime = System.currentTimeMillis();
                failException = ex;
            } finally {
                Context.exit();
            }
        });
    }

    public void tryCallFunctionAsync(String function, AbstractScriptContext scriptCtx, Runnable callback, Object... args) {
        if (!(scriptCtx.scriptStatus == null || scriptCtx.scriptStatus.isDone())) return;
        if (scriptCtx.disposed) return;
        List<Function> functions = this.functions.get(function);
        if (functions == null) functions = new ArrayList<>();
        scriptCtx.scriptStatus = callFunctionAsync(functions, scriptCtx, callback, args);
    }

    public void tryCallRenderFunctionAsync(AbstractScriptContext scriptCtx) {
        ScriptContextManager.trackContext(scriptCtx, this);
        if (!scriptCtx.created) {
            tryCallFunctionAsync("create", scriptCtx, () -> scriptCtx.created = true);
        } else {
            tryCallFunctionAsync("render", scriptCtx, () -> scriptCtx.renderFunctionFinished(), true);
        }
    }

    public void tryCallDisposeFunctionAsync(AbstractScriptContext scriptCtx) {
        scriptCtx.disposed = true;
        tryCallFunctionAsync("dispose", scriptCtx, () -> scriptCtx.created = false, false);
    }

    public void tryCallUseFunctionAsync(AbstractScriptContext scriptCtx, Player player) {
        tryCallFunctionAsync("use", scriptCtx, null, true, new WrappedEntity(player));
    }

    private boolean duringFailTimeout() {
        return failTime > 0 && (System.currentTimeMillis() - failTime) < 4000;
    }

    public static void resetRunner() {
        SCRIPT_THREAD.shutdownNow();
        SCRIPT_THREAD = Executors.newSingleThreadExecutor();
    }
}
