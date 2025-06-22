package cn.zbx1425.mtrsteamloco.render.scripting;

import cn.zbx1425.mtrsteamloco.render.scripting.util.OrderedMap;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import java.util.concurrent.Future;
import java.util.*;

public abstract class AbstractScriptContext {

    public ProxyObject state = ProxyObject.fromMap(new HashMap<>()); 
    public boolean created = false;
    public Future<?> scriptStatus;
    public double lastExecuteTime = 0;
    public boolean disposed = false;
    public long lastExecuteDuration = 0;
    public OrderedMap<String, Object> debugInfo = new OrderedMap<>();

    public abstract void renderFunctionFinished();

    public abstract Object getWrapperObject();

    public abstract boolean isBearerAlive();

    public OrderedMap<String, Object> getDebugInfo() {
        synchronized (debugInfo) {
            return new OrderedMap<>(debugInfo);
        }
    }

    public void removeDebugInfo(String key) {
        synchronized (debugInfo) {
            debugInfo.remove(key);
        }
    }

    public void setDebugInfo(String key, Object... values) {
        synchronized (debugInfo) {
            OrderedMap.PlacementOrder order = OrderedMap.PlacementOrder.CENTRAL;
            List<Object> list = new ArrayList<>();
            
            if (values == null || values.length == 0) return;
            Collections.addAll(list, values);
            
            if (list.size() > 1 && list.get(0) instanceof OrderedMap.PlacementOrder) {
                order = (OrderedMap.PlacementOrder) list.remove(0);
            }
            
            debugInfo.put(key, list.size() == 1 ? list.get(0) : list, order);
        }
    }

    public void clearDebugInfo() {
        synchronized (debugInfo) {
            debugInfo.clear();
        }
    }
}