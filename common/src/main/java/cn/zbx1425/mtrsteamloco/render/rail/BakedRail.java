package cn.zbx1425.mtrsteamloco.render.rail;

import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import cn.zbx1425.mtrsteamloco.data.RailModelRegistry;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.sowcer.util.AttrUtil;
import net.minecraft.world.phys.Vec3;
import mtr.data.Rail;
import cn.zbx1425.mtrsteamloco.data.RailModelProperties;
import net.minecraft.util.Mth;
import cn.zbx1425.mtrsteamloco.Main;

import java.util.ArrayList;
import java.util.HashMap;
import cn.zbx1425.mtrsteamloco.render.scripting.rail.RailScriptContext;

public class BakedRail {

    public HashMap<Long, ArrayList<Matrix4f>> coveredChunks = new HashMap<>();

    public static final int POS_SHIFT = 1;

    public String modelKey;
    public int color;
    public RailScriptContext scriptContext;

    public BakedRail(Rail rail) {
        modelKey = RailRenderDispatcher.getModelKeyForRender(rail);
        RailModelProperties prop = getProperties();
        color = AttrUtil.argbToBgr(rail.railType.color | 0xFF000000);

        if (prop.script != null) {
            scriptContext = new RailScriptContext(rail);
        }

        if (!modelKey.equals("null")) {
            RailExtraSupplier supplier = (RailExtraSupplier) rail;
            final boolean reverse = supplier.getRenderReversed();
            final float interval = prop.repeatInterval;
            final float yOffset = prop.yOffset;
            final double length = rail.getLength();
            final double ins = length / Math.round(length / interval);
            Vec3 pre = rail.getPosition(0);
            for (double i = ins; i <= length + ins * 0.2; i += ins) {
                Vec3 thi = rail.getPosition(i);
                Vec3 mid = thi.add(pre).scale(0.5);
                float roll = RailExtraSupplier.getRollAngle(rail, i - ins / 2);
                coveredChunks
                    .computeIfAbsent(chunkIdFromWorldPos((int) mid.x, (int) mid.z), ignored -> new ArrayList<>())
                    .add(getLookAtMat(mid, pre, thi, roll, yOffset, reverse));
                pre = thi;
            }
        }
    }

    public void dispose() {
        if (scriptContext != null) {
            scriptContext.dispose();
        }
    }

    public RailModelProperties getProperties() {
        return RailModelRegistry.getProperty(modelKey);
    }

    public static long chunkIdFromWorldPos(float bpX, float bpZ) {
        return ((long)((int)bpX >> (4 + POS_SHIFT)) << 32) | ((long)((int)bpZ >> (4 + POS_SHIFT)) & 0xFFFFFFFFL);
    }

    public static long chunkIdFromSectPos(int spX, int spZ) {
        return ((long)(spX >> POS_SHIFT) << 32) | ((long)(spZ >> POS_SHIFT) & 0xFFFFFFFFL);
    }

    public static Matrix4f getLookAtMat(Vec3 pos, Vec3 last, Vec3 next, float roll, float yOffset, boolean reverse) {

        Matrix4f matrix4f = new Matrix4f();
        matrix4f.translate((float) pos.x, (float) pos.y, (float) pos.z);

        if (reverse) {
            Vec3 temp = last;
            last = next;
            next = temp;
        }

        final float yaw = (float) Mth.atan2(next.x - last.x, next.z - last.z);
        final float pitch = (float) Mth.atan2(next.y - last.y, (float) Math.sqrt((next.x - last.x) * (next.x - last.x) + (next.z - last.z) * (next.z - last.z)));

        matrix4f.rotateY(yaw);
        matrix4f.rotateX(-pitch);
        matrix4f.translate(0, yOffset, 0);
        matrix4f.rotateZ(reverse? -roll : roll);

        return matrix4f;
    }
}
