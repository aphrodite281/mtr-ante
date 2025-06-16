package cn.zbx1425.sowcer.math;

import java.nio.FloatBuffer;

public class Matrix3f implements Posture{
#if MC_VERSION >= "11903"

    private final org.joml.Matrix3f impl;

    public Matrix3f(org.joml.Matrix3f impl) {
        this.impl = impl;
    }

    public Matrix3f() {
        this(new org.joml.Matrix3f());
    }

    public Matrix3f(Matrix3f other) {
        this.impl = new org.joml.Matrix3f(other.asMoj());
    }

    public Matrix3f(Quaternionf q) {
        this();
        this.impl.set(q.asMoj());
    }

    public Matrix3f(Matrix4f m) {
        this.impl = new org.joml.Matrix3f(m.asMoj());
    }

    public void store(FloatBuffer buffer) {
        buffer
        .put(0,  impl.m00())
        .put(1,  impl.m01())
        .put(2,  impl.m02())
        .put(3, impl.m10())
        .put(4, impl.m11())
        .put(5, impl.m12())
        .put(6, impl.m20())
        .put(7, impl.m21())
        .put(8, impl.m22());
    }

    public void load(FloatBuffer buffer) {
        float[] bufferValues = new float[9];
        buffer.get(bufferValues);
        impl.set(bufferValues);
    }

    public void setIdentity() {
        this.impl.identity();
    }

    public void multiply(Matrix3f other) {
        this.impl.mul(other.impl);
    }

    public void multiply(Quaternionf q) {
        this.impl.rotate(q.asMoj());
    }

    public void add(Matrix3f other) {
        this.impl.add(other.impl);
    }

    public void sub(Matrix3f other) {
        this.impl.sub(other.impl);
    }

    public void scale(float sx, float sy, float sz) {
        this.impl.scale(sx, sy, sz, this.impl);
    }

    public org.joml.Matrix3f asMoj() {
        return this.impl;
    }

    public Vector3f transform(Vector3f src) {
        org.joml.Vector3f srcCpy = new org.joml.Vector3f(src.impl);
        return new Vector3f(this.impl.transform(srcCpy));
    }
#else
    private final com.mojang.math.Matrix3f impl;

    public Matrix3f(com.mojang.math.Matrix3f impl) {
        this.impl = impl;
    }

    public Matrix3f() {
        this(new com.mojang.math.Matrix3f());
    }

    public Matrix3f(Matrix3f other) {
        this(new com.mojang.math.Matrix3f(other.asMoj()));
    }

    public Matrix3f(Quaternionf q) {
        this.impl = new com.mojang.math.Matrix3f(q.asMoj());
    }

    public Matrix3f(Matrix4f m) {
        this.impl = new com.mojang.math.Matrix3f(m.asMoj());
    }

    public void store(FloatBuffer buffer) {
        this.impl.store(buffer);
    }

    public void load(FloatBuffer buffer) {
        this.impl.load(buffer);
    }

    public void setIdentity() {
        this.impl.setIdentity();
    }

    public void multiply(Matrix3f other) {
        this.impl.mul(other.impl);
    }

    public void multiply(Quaternionf q) {
        this.impl.mul(q.asMoj());
    }

    public void add(Matrix3f other) {
        this.impl.add(other.impl);
    }

    public void sub(Matrix3f other) {
        this.impl.sub(other.impl);
    }

    public void scale(float sx, float sy, float sz) {
        this.impl.mul(com.mojang.math.Matrix3f.createScaleMatrix(sx, sy, sz));
    }

    public com.mojang.math.Matrix3f asMoj() {
        return this.impl;
    }

    public Vector3f transform(Vector3f src) {
        Vector3f pos3 = src.copy();
        pos3.impl.transform(impl);
        return pos3;
    }
#endif

    public void scale(float s) {
        scale(s, s, s);
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

    @Override
    public Matrix3f getAsMatrix3f() {
        return this;
    }

    @Override
    public Matrix4f getAsMatrix4f() {
        return new Matrix4f(this);
    }

    @Override
    public Matrices getAsMatrices() {
        return new Matrices(getAsMatrix4f());
    }

    @Override
    public Pose getAsPose() {
        return new Pose(getAsMatrix4f());
    }

    @Override
    public PoseStack getAsPoseStack() {
        return new PoseStack(getAsPose());
    }


}