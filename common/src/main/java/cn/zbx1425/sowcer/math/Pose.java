package cn.zbx1425.sowcer.math;

import net.minecraft.util.Mth;
import cn.zbx1425.mtrsteamloco.mixin.PoseAccessor;

public class Pose implements Posture {
    private final Matrix4f pose;
    private final Matrix3f normal;

    public Pose() {
        this.pose = new Matrix4f();
        this.normal = new Matrix3f();
    }

    public Pose(Pose pose) {
        this.pose = new Matrix4f(pose.pose());
        this.normal = new Matrix3f(pose.normal());
    }

    public Pose(com.mojang.blaze3d.vertex.PoseStack.Pose pose) {
        this.pose = new Matrix4f(pose.pose());
        this.normal = new Matrix3f(pose.normal());
    }

    public Pose(Matrix4f pose) {
        this.pose = pose;
        this.normal = new Matrix3f(pose);
    }

    public Pose(Matrix4f pose, Matrix3f normal) {
        this.pose = pose;
        this.normal = normal;
    }

    public Matrix4f pose() {
        return pose;
    }

    public Matrix3f normal() {
        return normal;
    }

    public void translate(float x, float y, float z) {
        pose.translate(x, y, z);
    }

    public void scale(float x, float y, float z) {
        pose.scale(x, y, z);
        if (x == y && y == z) {
            if (x > 0.0F) {
                return;
            }

            normal.scale(-1.0F);
        }

        float f = 1.0F / x;
        float f1 = 1.0F / y;
        float f2 = 1.0F / z;
        float f3 = Mth.fastInvCubeRoot(f * f1 * f2);
        normal.scale(f3 * f, f3 * f1, f3 * f2);
    }

    public void scale(float factor) {
        scale(factor, factor, factor);
    }

    public void multiply(Quaternionf q) {
        pose.multiply(q);
        normal.multiply(q);
    }

    public void rotateX(float angle) {
        multiply(new Quaternionf(Vector3f.XP, angle));
    }

    public void rotateY(float angle) {
        multiply(new Quaternionf(Vector3f.YP, angle));
    }

    public void rotateZ(float angle) {
        multiply(new Quaternionf(Vector3f.ZP, angle));
    }

    public com.mojang.blaze3d.vertex.PoseStack.Pose asMoj() {
        return PoseAccessor.create(pose.asMoj(), normal.asMoj());
    }

    @Override
    public Matrix4f getAsMatrix4f() {
        return pose;
    }

    @Override
    public Matrix3f getAsMatrix3f() {
        return normal;
    }

    @Override
    public Pose getAsPose() {
        return this;
    }

    @Override
    public PoseStack getAsPoseStack() {
        return new PoseStack(this);
    }

    @Override
    public Matrices getAsMatrices() {
        return new Matrices(getAsMatrix4f());
    }
}