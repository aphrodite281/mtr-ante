package cn.zbx1425.mtrsteamloco.data;

import cn.zbx1425.mtrsteamloco.scripting.ScriptHolderBase;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.sowcerext.model.RawModel;
import cn.zbx1425.sowcerext.model.RawMesh;
import cn.zbx1425.sowcerext.model.Vertex;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import mtr.mappings.Text;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.Vector3f;
import static java.lang.Math.*;

import java.io.Closeable;
import java.io.IOException;

public class EyeCandyProperties implements Closeable {

    public static final EyeCandyProperties DEFAULT = new EyeCandyProperties("default_key", Text.literal(""), null, null, null, "0, 0, 0, 16, 16, 16", "0, 0, 0, 0, 0, 0", true, 0, false, false, false, "ANTE");

    public String key;
    public MutableComponent name;

    public ModelCluster model;
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

    public EyeCandyProperties(String key, MutableComponent name, ModelCluster model, ModelCluster itemModel, ScriptHolderBase script, String shape, String collisionShape, boolean fixedMatrix, int lightLevel, boolean isTicketBarrier, boolean isEntrance, boolean asPlatform, String group) {
        this.key = key;
        this.name = name;
        this.model = model;
        this.itemModel = itemModel;

        if (itemModel != null) {
            Matrix4f mat = new Matrix4f();
            mat.rotateY((float) toRadians(-30));
            float minx = 0, miny = 0, minz = 0, maxx = 0, maxy = 0, maxz = 0;
            RawModel[] rms = new RawModel[]{itemModel.opaqueParts, itemModel.translucentParts};
            for (RawModel rm : rms) {
                for (RawMesh mesh : rm.meshList.values()) {
                    for (Vertex vert : mesh.vertices) {
                        Vector3f pos = mat.transform(vert.position);
                        minx = min(minx, pos.x());
                        maxx = max(maxx, pos.x());
                        miny = min(miny, pos.y());
                        maxy = max(maxy, pos.y());

                        minz = min(minz, pos.z());
                        maxz = max(maxz, pos.z());
                    }
                }
            }
            miny = min(miny, 0);
            float xm = max(abs(minx), abs(maxx)), ym = maxy - miny, zm = max(abs(minz), abs(maxz));
            float f1 = 0.5f / max(0.5f, max(xm, zm)), f2 = 1f / max(1, ym);
            itemTransform = new Matrix4f();
            itemTransform.mul(mat);
            itemTransform.scale(min(f1, f2));
            itemTransform.translate(0, -miny, 0);
        }

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
