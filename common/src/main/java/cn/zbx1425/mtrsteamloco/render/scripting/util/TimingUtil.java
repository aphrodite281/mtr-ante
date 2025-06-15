package cn.zbx1425.mtrsteamloco.render.scripting.util;

import cn.zbx1425.mtrsteamloco.render.RenderUtil;
import cn.zbx1425.mtrsteamloco.render.scripting.AbstractScriptContext;
import net.minecraft.client.Minecraft;

@SuppressWarnings("unused")
public class TimingUtil {

    public static double runningSeconds = 0;

    private static double timeElapsedForScript = 0;
    private static double frameDeltaForScript = 0;

    public static void prepareForScript(AbstractScriptContext scriptContext) {
        timeElapsedForScript = runningSeconds;
        frameDeltaForScript = timeElapsedForScript - scriptContext.lastExecuteTime;
        scriptContext.lastExecuteTime = timeElapsedForScript;
    }

    public static double elapsed() {
        return timeElapsedForScript;
    }

    public static double delta() {
        return frameDeltaForScript;
    }

    public static String gameTime() {
        return Long.toString(Minecraft.getInstance().level.getGameTime());
    }
}
