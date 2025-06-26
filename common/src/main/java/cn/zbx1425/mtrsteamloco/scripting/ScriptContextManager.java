package cn.zbx1425.mtrsteamloco.scripting;

import java.util.*;

public class ScriptContextManager {

    public static final Map<AbstractScriptContext, ScriptHolderBase> livingContexts = new HashMap<>();

    public static void trackContext(AbstractScriptContext context, ScriptHolderBase scriptHolder) {
        synchronized (livingContexts) {
            livingContexts.put(context, scriptHolder);
        }
    }

    public static void disposeDeadContexts() {
        synchronized (livingContexts) {
            for (Iterator<Map.Entry<AbstractScriptContext, ScriptHolderBase>> it = livingContexts.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<AbstractScriptContext, ScriptHolderBase> entry = it.next();
                if (!entry.getKey().isBearerAlive() || entry.getKey().disposed) {
                    if (entry.getKey().created) {
                        entry.getValue().tryCallDisposeFunctionAsync(entry.getKey());
                    }
                    it.remove();
                }
            }
        }
    }

    public static void disposeAllContexts() {
        synchronized (livingContexts) {
            for (Map.Entry<AbstractScriptContext, ScriptHolderBase> entry : livingContexts.entrySet()) {
                if (entry.getKey().created) {
                    entry.getValue().tryCallDisposeFunctionAsync(entry.getKey());
                }
            }
            livingContexts.clear();
        }
    }

    public static void clearDebugInfo() {
        for (AbstractScriptContext context : livingContexts.keySet()) {
            context.clearDebugInfo();
        }
    }
}
