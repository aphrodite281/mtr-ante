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
import java.util.List;
import java.util.HashMap;
import cn.zbx1425.mtrsteamloco.render.scripting.rail.RailScriptContext;

public class BakedRail {

    private static final double ACCEPT_THRESHOLD = 1E-2;
    private static final double HALF_ACCEPT_THRESHOLD = ACCEPT_THRESHOLD / 2;

    public HashMap<Long, ArrayList<Posture>> coveredChunks = new HashMap<>();

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
            final double length = rail.getLength() - ACCEPT_THRESHOLD;
            final double ins = length / Math.max(1, Math.round(length / interval));
            Slice pre = new Slice(getMatrix(rail, HALF_ACCEPT_THRESHOLD, reverse));
            for (double i = ins + HALF_ACCEPT_THRESHOLD; i <= length + ACCEPT_THRESHOLD; i += ins) {
                Slice thi = new Slice(getMatrix(rail, i, reverse));
                pre.position.add(thi.position);
                pre.position.mul(0.5f);
                coveredChunks
                    .computeIfAbsent(chunkIdFromWorldPos((int) pre.position.x(), (int) pre.position.z()), ignored -> new ArrayList<>())
                    // .add(thi.matrix);
                    .add(getMatrix(pre, thi, reverse, interval));
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

    private static Matrix4f getMatrix(Rail rail, double loca, boolean reverse) {
        Vec3 last = rail.getPosition(loca - HALF_ACCEPT_THRESHOLD);
        Vec3 next = rail.getPosition(loca + HALF_ACCEPT_THRESHOLD);
        Vec3 mid = last.add(next).scale(0.5);

        if (reverse) {
            Vec3 temp = last;
            last = next;
            next = temp;
        }

        Vec3 direction = next.subtract(last).normalize();

        final float yaw = (float) Mth.atan2(direction.x, direction.z);
        final float pitch = (float) Math.asin(direction.y);

        Matrix4f result = new Matrix4f();

        result.translate(mid.x, mid.y, mid.z);
        result.rotateY((float) Math.PI + yaw);
        result.rotateX(pitch);

        float roll = RailExtraSupplier.getRollAngle(rail, loca);
        result.rotateZ(reverse ? roll : -roll);

        return result;
    }

    private static Matrix4f getMatrix(Slice from, Slice to, boolean reverse, float interval) {
        if (reverse) {
            Slice temp = from;
            from = to;
            to = temp;
        }

        List<double[]> points = new ArrayList<>();
        points.addAll(from.points);
        points.addAll(to.points);
        String str = "";
        for (double[] point : points) {
            str += point[0] + "," + point[1] + "," + point[2] + "\n";
        }
        System.out.println(str);
        double[][] matrix = MatrixCalculator.computeMatrix(interval, points);
        Matrix4f result = new Matrix4f();
        if (true) {
            result = new Matrix4f(
                matrix[0][0], matrix[1][0], matrix[2][0], matrix[3][0],
                matrix[0][1], matrix[1][1], matrix[2][1], matrix[3][1],
                matrix[0][2], matrix[1][2], matrix[2][2], matrix[3][2],
                matrix[0][3], matrix[1][3], matrix[2][3], matrix[3][3]
            );
        } else {
            result = new Matrix4f(
                matrix[0][0], matrix[0][1], matrix[0][2], matrix[0][3],
                matrix[1][0], matrix[1][1], matrix[1][2], matrix[1][3],
                matrix[2][0], matrix[2][1], matrix[2][2], matrix[2][3],
                matrix[3][0], matrix[3][1], matrix[3][2], matrix[3][3]
            );
        }
        System.out.println(matrix);
        System.out.println(result);
        // TransformMatrixCalculator.Verifier.verifyTransformation(matrix, interval, TransformMatrixCalculator.generateOriginalPoints(interval), points, 1E-2);
        return result;
    }

    private static class Slice {
        public final List<double[]> points = new ArrayList();
        public final Matrix4f matrix;
        public final Vector3f position;

        public Slice(Matrix4f matrix) {
            this.matrix = matrix;
            points.add(transfrom(-1, 1));
            points.add(transfrom(1, 1));
            points.add(transfrom(1, -1));
            points.add(transfrom(-1, -1));
            position = matrix.transform(new Vector3f(0, 0, 0));
        }

        private double[] transfrom(float x, float y) {
            Vector3f dest = matrix.transform(new Vector3f(-x, y, 0));
            return new double[] { dest.x(), dest.y(), dest.z() };
        }

        public static List<Vector3f> toVector3f(Slice... slice) {
            List<Vector3f> result = new ArrayList<>();
            for (Slice s : slice) {
                for (double[] point : s.points) {
                    result.add(new Vector3f(point[0], point[1], point[2]));
                }
            }
          
           return result;
        }
    }
}