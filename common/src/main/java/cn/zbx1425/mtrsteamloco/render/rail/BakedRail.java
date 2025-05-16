package cn.zbx1425.mtrsteamloco.render.rail;

import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import cn.zbx1425.mtrsteamloco.data.RailModelRegistry;
import cn.zbx1425.sowcer.math.*;
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

    public HashMap<Long, ArrayList<Pose>> coveredChunks = new HashMap<>();

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
                    .add(getLookAtPose(mid, pre, thi, roll, yOffset, reverse, interval));
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

    public static Pose getLookAtPose(Vec3 pos, Vec3 last, Vec3 next, float roll, float yOffset, boolean reverse, float interval) {

        Pose pose = new Pose();
        pose.translate((float) pos.x, (float) pos.y, (float) pos.z);

        if (reverse) {
            Vec3 temp = last;
            last = next;
            next = temp;
        }

        final float yaw = (float) Mth.atan2(next.x - last.x, next.z - last.z);
        final float pitch = (float) Mth.atan2(next.y - last.y, (float) Math.sqrt((next.x - last.x) * (next.x - last.x) + (next.z - last.z) * (next.z - last.z)));

        pose.rotateY(yaw);
        pose.rotateX(-pitch);
        pose.translate(0, yOffset, 0);
        pose.rotateZ(reverse? -roll : roll);

        float length = (float) next.distanceTo(last);
        pose.scale(interval / length);

        return pose;
    }
}
