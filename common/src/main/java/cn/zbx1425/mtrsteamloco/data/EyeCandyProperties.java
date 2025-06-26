package cn.zbx1425.mtrsteamloco.data;

import cn.zbx1425.mtrsteamloco.scripting.ScriptHolderBase;
import cn.zbx1425.sowcerext.model.ModelCluster;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import mtr.mappings.Text;

import java.io.Closeable;
import java.io.IOException;

public class EyeCandyProperties implements Closeable {

    public static final EyeCandyProperties DEFAULT = new EyeCandyProperties("default_key", Text.literal(""), null, null, "0, 0, 0, 16, 16, 16", "0, 0, 0, 0, 0, 0", true, 0, false, false, false, "ANTE");

    public String key;
    public MutableComponent name;

    public ModelCluster model;
    public ScriptHolderBase script;
    public String shape;
    public String collisionShape;
    public boolean fixedMatrix;
    public int lightLevel;
    public boolean isTicketBarrier;
    public boolean isEntrance;
    public boolean asPlatform;
    public String group;
    public String path;

    public EyeCandyProperties(String key, MutableComponent name, ModelCluster model, ScriptHolderBase script, String shape, String collisionShape, boolean fixedMatrix, int lightLevel, boolean isTicketBarrier, boolean isEntrance, boolean asPlatform, String group) {
        this.key = key;
        this.name = name;
        this.model = model;
        this.script = script;
        this.shape = shape;
        this.collisionShape = collisionShape;
        this.fixedMatrix = fixedMatrix;
        this.lightLevel = lightLevel;
        this.isTicketBarrier = isTicketBarrier;
        this.isEntrance = isEntrance;
        this.asPlatform = asPlatform;
        this.group = group;
        this.path = group + "/" + key;
    }

    @Override
    public void close() throws IOException {
        if (model != null) model.close();
    }
}
