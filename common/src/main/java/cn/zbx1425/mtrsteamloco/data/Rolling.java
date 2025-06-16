package cn.zbx1425.mtrsteamloco.data;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.sowcer.math.*;

public class Rolling {
    private static Rotation rotation = Rotation.IDENTITY;
    private static Rotation tempRotation = null;

    public static void setRotation(Rotation rotation) {
        tempRotation = rotation;
    }

    public static void update() {
        if (tempRotation != null) {
            rotation = tempRotation;
            tempRotation = null;
        } else {
            rotation = Rotation.IDENTITY;
        }
    }

    public static void applyRolling(PoseStack poseStack) {
        if (!ClientConfig.enableRolling) return;
        if (rotation.isIdentity()) return;
        
        poseStack.mulPose(getRollQuaternion(true).asMoj());
    }

    public static Vector3f applyRolling(Vector3f pos, float eyeHeight) {
        if (rotation.isIdentity()) return pos;

        pos = pos.copy();
        Rotation rot = rotation;


        Matrix4f mat = new Matrix4f();
        mat.rotateY(rot.yaw);
        mat.rotateX(-rot.pitch);
        mat.rotateZ(rot.reversed? rot.roll : -rot.roll);

        pos.add(0, -eyeHeight, 0);
        Vector3f ep = mat.transform(new Vector3f(0, eyeHeight, 0));
        pos.add(ep);
        return pos;
    }

    public static Quaternionf getRollQuaternion() {
        return getRollQuaternion(false);
    }

    public static Quaternionf getRollQuaternion(boolean reversed) {
        if (rotation.isIdentity()) return new Quaternionf(new Vector3f(0, 0, 0), 0);

        Rotation rot = rotation;

        float roll = reversed != rot.reversed ? rot.roll : -rot.roll;
        float pitch = reversed ? rot.pitch : -rot.pitch;

        if (ClientConfig.enableRolling) {
            return new Quaternionf()
            .rotateY(rot.yaw)
               .rotateX(pitch)
               .rotateZ(roll)
            .rotateY(-rot.yaw);
        } else {
            return new Quaternionf().rotateY(rot.yaw)
                .rotateX(pitch)
            .rotateY(-rot.yaw);
        }
    }

    public static Quaternionf _getRollQuaternion(boolean reversed) {
        if (rotation.isIdentity() || !ClientConfig.enableRolling) return new Quaternionf(new Vector3f(0, 0, 0), 0);

        Rotation rot = rotation;

        Matrix4f mat = new Matrix4f();
        mat.rotateY(rot.yaw);
        mat.rotateX(rot.pitch);
        Vector3f fp = mat.transform(new Vector3f(0, 0, 1));
        Quaternionf q = new Quaternionf(fp, reversed != rot.reversed ? rot.roll : -rot.roll);

        return q;
    }

    public static class Rotation {
        public final Vector3f pos;
        public final float yaw;
        public final float pitch;
        public final float roll;
        public final boolean reversed;

        public static final Rotation IDENTITY = new Rotation(0, 0, 0, 0, 0, 0, false);

        public Rotation(double x, double y, double z, float yaw, float pitch, float roll, boolean reversed) {
            this.pos = new Vector3f(x, y, z);
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
            this.reversed = reversed;
        }

        public boolean isIdentity() {
            return yaw == 0 && pitch == 0 && roll == 0;
        }

        public Rotation copy() {
            return new Rotation(pos.x(), pos.y(), pos.z(), yaw, pitch, roll, reversed);
        }
    }
}