package cn.zbx1425.mtrsteamloco.data;

import cn.zbx1425.mtrsteamloco.render.scripting.ScriptHolder;
import cn.zbx1425.sowcerext.model.ModelCluster;
import net.minecraft.network.chat.Component;

import java.io.Closeable;
import java.io.IOException;

public class EyeCandyProperties implements Closeable {

    public Component name;

    public ModelCluster model;
    public ScriptHolder script;
    public String shape;
    public boolean noCollision;
    public boolean fixedShape;
    public boolean fixedMatrix;
    public int lightLevel;

    public EyeCandyProperties(Component name, ModelCluster model, ScriptHolder script, String shape, boolean noCollision, boolean fixedShape, boolean fixedMatrix, int lightLevel) {
        this.name = name;
        this.model = model;
        this.script = script;
        this.shape = shape;
        this.noCollision = noCollision;
        this.fixedShape = fixedShape;
        this.fixedMatrix = fixedMatrix;
        this.lightLevel = lightLevel;
    }

    @Override
    public void close() throws IOException {
        if (model != null) model.close();
    }
}
