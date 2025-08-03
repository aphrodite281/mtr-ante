package cn.zbx1425.mtrsteamloco.data;

import cn.zbx1425.mtrsteamloco.scripting.ScriptHolderBase;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.sowcerext.model.RawModel;
import cn.zbx1425.sowcerext.model.RawMesh;
import cn.zbx1425.sowcerext.model.Vertex;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import mtr.mappings.Text;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.Vector3f;
import static java.lang.Math.*;
import net.minecraft.resources.ResourceLocation;
import cn.zbx1425.mtrsteamloco.scripting.ScriptResourceUtil;
import cn.zbx1425.mtrsteamloco.data.RelativePosition.*;

import java.io.Closeable;
import java.io.IOException;

public class EyeCandyProperties implements Closeable {

    public static final EyeCandyProperties DEFAULT = new EyeCandyProperties("default_key", Text.literal(""), null, null, null, null, null, "0, 0, 0, 16, 16, 16", "0, 0, 0, 0, 0, 0", true, 0, false, false, false, "ANTE", Combination.decode(""));

    public String key;
    public MutableComponent name;

    public ModelCluster model;

    public BakedModel itemBakedModel;
    public ModelCluster itemModel;
    public Matrix4f itemTransform;

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
    public Combination positions;

    public EyeCandyProperties(String key, MutableComponent name, ModelCluster model, ModelCluster itemModel, Matrix4f itemTransform, BakedModel itemBakedModel, ScriptHolderBase script, String shape, String collisionShape, boolean fixedMatrix, int lightLevel, boolean isTicketBarrier, boolean isEntrance, boolean asPlatform, String group, Combination positions) {
        this.key = key;
        this.name = name;
        this.model = model;

        this.itemBakedModel = itemBakedModel;
        this.itemModel = itemModel;
        this.itemTransform = itemTransform;

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
        this.positions = positions;
    }

    @Override
    public void close() throws IOException {
        if (model != null) model.close();
    }
}
