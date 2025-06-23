package cn.zbx1425.mtrsteamloco.render.scripting;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;
import com.google.gson.JsonObject;
import cn.zbx1425.mtrsteamloco.Main;
import net.minecraft.world.entity.player.Player;
import com.google.gson.Gson;
import cn.zbx1425.mtrsteamloco.render.scripting.util.*;
import cn.zbx1425.sowcer.math.*;
import cn.zbx1425.mtrsteamloco.data.ShapeSerializer;
import net.minecraft.network.chat.Component;
import cn.zbx1425.mtrsteamloco.Main;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.SandboxPolicy;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.lang.reflect.Method;

public abstract class ScriptHolderBase {

    private static ExecutorService SCRIPT_THREAD = Executors.newSingleThreadExecutor();

    public final String side;
    private Context context; 
    private Value globalBindings; 
    public final Map<String, List<Value>> functions = new HashMap<>(); 

    public long failTime = 0;
    public Exception failException = null;

    public String name;
    public String contextTypeName;
    private Map<ResourceLocation, String> scripts;

    private JsonObject config;
    private String key;
    private String[] functionNames;

    protected static final String PRETREATMENT = "load(\"nashorn:mozilla_compat.js\");";

    public ScriptHolderBase(String side) {
        this.side = side;
    }

    public void load(
        String name, String contextTypeName, ResourceManager resourceManager, 
        Map<ResourceLocation, String> scripts, JsonObject config, String key, 
        String... functionNames) throws Exception {
        this.name = name;
        this.contextTypeName = contextTypeName;
        this.scripts = scripts;
        this.config = config;
        this.key = key;
        this.functionNames = functionNames;

        context = Context.newBuilder("js")  
            .allowCreateThread(true)  
            .allowCreateProcess(true)
            .allowHostClassLoading(true)  
            .allowHostClassLookup(className -> true)  
            .allowNativeAccess(true)  
            .allowIO(IOAccess.ALL)  
            .allowEnvironmentAccess(EnvironmentAccess.INHERIT)  
            .allowExperimentalOptions(true)  
            .allowInnerContextOptions(true)  
            .sandbox(SandboxPolicy.TRUSTED)
            .allowHostAccess(
                HostAccess.newBuilder()
                .allowPublicAccess(true)
                .allowAllImplementations(true)
                .allowAllClassImplementations(true)
                .allowArrayAccess(true)
                .allowListAccess(true)
                .allowBufferAccess(true)
                .allowIterableAccess(true)
                .allowIteratorAccess(true)
                .allowMapAccess(true)
                .allowAccessInheritance(true)
                .allowBigIntegerNumberAccess(true)
                .targetTypeMapping(
                    Double.class,
                    Integer.class,
                    value -> true,
                    value -> (int) Math.round(value)
                )
                .targetTypeMapping(
                    Double.class,
                    Float.class,
                    value -> true,
                    value -> value.floatValue()
                )
                .targetTypeMapping(
                    Double.class,
                    Long.class,
                    value -> true,
                    value -> Math.round(value)
                )
                .targetTypeMapping(
                    Double.class,
                    Short.class,
                    value -> true,
                    value -> (short) Math.round(value)
                )
                .targetTypeMapping(
                    Double.class,
                    Byte.class,
                    value -> true,
                    value -> (byte) Math.round(value)
                )
                .build()
            )
            .option("js.nashorn-compat", "true")
            .option("js.ecmascript-version", "latest")
            .option("js.foreign-object-prototype", "true")
            .option("log.file", "")
            .build();

        globalBindings = context.getBindings("js");    
        
        appendImporter();
        
        for (Map.Entry<ResourceLocation, String> entry : scripts.entrySet()) {
            String scriptContent = entry.getValue() != null ? 
                entry.getValue() : 
                ScriptResourceUtil.readString(entry.getKey());

            ScriptResourceUtil.executeScript(context, scriptContent, entry.getKey());
        }

        
        for (String fn : functionNames) {
            registerFunction(fn);
            registerFunction(fn + contextTypeName);
        }

        // SCRIPT_THREAD.submit(() -> {
        //     context.enter();
        // });
    }

    public void reload(ResourceManager resourceManager) throws Exception {
        close();
        load(name, contextTypeName, resourceManager, scripts, config, key, functionNames);
    }

    protected void inject(Class clazz, String method, String alias) {
        if (alias == null) alias = method;
        context.eval("js", "var " + alias + " = Java.type('" + clazz.getName() + "')." + method + ";");
    }

    protected void inject(Class clazz, String alias) {
        if (alias == null) alias = clazz.getSimpleName();
        context.eval("js", "var " + alias + " = Java.type('" + clazz.getName() + "');");
    }

    protected void inject(String key, Object value) {
        context.eval("js", "var " + key + " = '" + value + "';");
    }

    protected void eval(String script) {
        context.eval("js", script);
    }

    protected void appendImporter() {
        eval(PRETREATMENT);
        inject("SIDE", side);
        inject("CONFIG_INFO", new Gson().toJson(config));
        eval("CONFIG_INFO = JSON.parse(CONFIG_INFO);");
        inject("MOD_ENV", Main.class.getPackageName().split("\\.")[0]);

        inject(ScriptResourceUtil.class, "includeScript", "include");
        inject(ScriptResourceUtil.class, "print", "print");
        inject(JsFriendlyJavaUtils.class, "asJavaArray", "asJavaArray");

        inject(TimingUtil.class, "Timing");
        inject(StateTracker.class, "StateTracker");
        inject(CycleTracker.class, "CycleTracker");
        inject(RateLimit.class, "RateLimit");
        inject(TextUtil.class, "TextUtil");
        inject(GlobalRegister.class, "GlobalRegister");
        inject(WrappedEntity.class, "WrappedEntity");
        inject(ComponentUtil.class, "ComponentUtil");
        inject(OrderedMap.class, "OrderedMap");
        inject(OrderedMap.PlacementOrder.class, "PlacementOrder");
        inject(ShapeSerializer.class, "ShapeSerializer");

        inject(Matrices.class, "Matrices");
        inject(Matrix4f.class, "Matrix4f");
        inject(Vector3f.class, "Vector3f");   

        inject(Component.class, "Component");
        
        inject(Optional.class, "Optional");
    }

    private void registerFunction(String name) {
        Value func = globalBindings.getMember(name);
        if (func != null && func.canExecute()) {
            functions.computeIfAbsent(name, k -> new ArrayList<>(1))
                     .add(func);
        }
    }

    public Future<?> callFunctionAsync(List<Value> functions, AbstractScriptContext scriptCtx, 
                                      Runnable finishCallback, Object... args) {
        if (duringFailTimeout()) return null;
        if (context == null) {
            Main.LOGGER.error("Script context is null, cannot execute function");
            return null;
        }

        // return SCRIPT_THREAD.submit(() -> {
            long start = System.currentTimeMillis();
            try {                
                Object[] allArgs = new Object[3 + args.length];
                allArgs[0] = scriptCtx;
                allArgs[1] = scriptCtx.state != null ? scriptCtx.state : ProxyObject.fromMap(new HashMap<>());
                allArgs[2] = scriptCtx.getWrapperObject();
                System.arraycopy(args, 0, allArgs, 3, args.length);
                
                for (Value function : functions) {
                    function.executeVoid(allArgs);
                }
                
                if (finishCallback != null) finishCallback.run();

            } catch (Exception ex) {
                Main.LOGGER.error("Error in ANTE Resource Pack JavaScript", ex);
                failTime = System.currentTimeMillis();
                failException = ex;
            } finally {
            }
        // });
        scriptCtx.lastExecuteTime = System.currentTimeMillis() - start;
        return null;
    }

    public void tryCallFunctionAsync(String function, AbstractScriptContext scriptCtx, Runnable callback, Object... args) {
        // if (!(scriptCtx.scriptStatus == null || scriptCtx.scriptStatus.isDone())) return;
        if (scriptCtx.disposed) return;
        List<Value> functions = this.functions.get(function);
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

    public void close() {
        if (context != null) {
            // context.leave();
            context.close();
            context = null;
            // SCRIPT_THREAD.submit(() -> {
                
            // });
        }
    }
}