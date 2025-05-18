package cn.zbx1425.sowcer.math;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
public class Matrix4f implements Posture {

#if MC_VERSION >= "11903"

    protected final org.joml.Matrix4f impl;
    public Matrix4f() {
        this.impl = new org.joml.Matrix4f();
        this.impl.identity();
    }

    public Matrix4f(org.joml.Matrix4f moj) {
        this.impl = moj;
    }

    public Matrix4f(Matrix3f mat3) {
        this.impl = new org.joml.Matrix4f(mat3.asMoj());
    } 

    public Matrix4f(Matrix4f other) {
        this.impl = new org.joml.Matrix4f(other.impl);
    }

    public Matrix4f(
        float m00, float m01, float m02, float m03, 
        float m10, float m11, float m12, float m13, 
        float m20, float m21, float m22, float m23,
        float m30, float m31, float m32, float m33) {
        this.impl = new org.joml.Matrix4f(
            m00, m01, m02, m03, 
            m10, m11, m12, m13, 
            m20, m21, m22, m23,
            m30, m31, m32, m33);
    }

    public Matrix4f copy() {
        return new Matrix4f(this);
    }

    public org.joml.Matrix4f asMoj() {
        return impl;
    }

    public static Matrix4f translation(float x, float y, float z) {
        Matrix4f result = new Matrix4f();
        result.impl.translation(x, y, z);
        return result;
    }

    public void multiply(Matrix4f other) {
        impl.mul(other.impl);
    }

    public void multiply(Quaternionf q) {
        impl.rotate(q.asMoj());
    }

    public void store(FloatBuffer buffer) {
        buffer
                .put(0,  impl.m00())
                .put(1,  impl.m01())
                .put(2,  impl.m02())
                .put(3,  impl.m03())
                .put(4,  impl.m10())
                .put(5,  impl.m11())
                .put(6,  impl.m12())
                .put(7,  impl.m13())
                .put(8,  impl.m20())
                .put(9,  impl.m21())
                .put(10, impl.m22())
                .put(11, impl.m23())
                .put(12, impl.m30())
                .put(13, impl.m31())
                .put(14, impl.m32())
                .put(15, impl.m33());
    }

    public void load(FloatBuffer buffer) {
        float[] bufferValues = new float[16];
        buffer.get(bufferValues);
        impl.set(bufferValues);
    }

    public void rotateX(float rad) {
        impl.rotateX(rad);
    }

    public void rotateY(float rad) {
        impl.rotateY(rad);
    }

    public void rotateZ(float rad) {
        impl.rotateZ(rad);
    }

    public void rotate(Vector3f axis, float rad) {
        impl.rotate(rad, axis.impl);
    }

    public void translate(float x, float y, float z) {
        impl.translate(x, y, z);
    }

    public Vector3f transform(Vector3f src) {
        org.joml.Vector4f dest = new org.joml.Vector4f(src.x(), src.y(), src.z(), 1.0f);
        impl.transform(dest);
        return new Vector3f(dest.x() / dest.w(), dest.y() / dest.w(), dest.z() / dest.w());
    }

    public Vector3f transform3(Vector3f src) {
        org.joml.Vector3f srcCpy = new org.joml.Vector3f(src.impl);
        return new Vector3f(impl.transformDirection(srcCpy));
    }

    public org.joml.Matrix3f getRotationPart() {
        org.joml.Matrix3f result = new org.joml.Matrix3f();
        return impl.get3x3(result);
    }

    public Vector3f getTranslationPart() {
       org.joml.Vector3f result = new org.joml.Vector3f();
       return new Vector3f(impl.getTranslation(result));
    }

    public void scale(float x, float y, float z) {
        impl.scale(x, y, z);
    }

#else

    protected final com.mojang.math.Matrix4f impl;

    public Matrix4f() {
        this.impl = new com.mojang.math.Matrix4f();
        this.impl.setIdentity();
    }

    public Matrix4f(com.mojang.math.Matrix4f moj) {
        this.impl = moj;
    }

    public Matrix4f(Matrix3f mat3) {
        this();
        float[] srcValues = new float[9];
        FloatBuffer srcFloatBuffer = FloatBuffer.wrap(srcValues);
        mat3.store(srcFloatBuffer);
        float[] dstValues = new float[16];
        dstValues[0] = srcValues[0];
        dstValues[1] = srcValues[1];
        dstValues[2] = srcValues[2];
        dstValues[3] = 0.0F;
        dstValues[4] = srcValues[3];
        dstValues[5] = srcValues[4];
        dstValues[6] = srcValues[5];
        dstValues[7] = 0.0F;
        dstValues[8] = srcValues[6];
        dstValues[9] = srcValues[7];
        dstValues[10] = srcValues[8];
        dstValues[11] = 0.0F;
        dstValues[12] = 0.0F;
        dstValues[13] = 0.0F;
        dstValues[14] = 0.0F;
        dstValues[15] = 1.0F;
        FloatBuffer dstFloatBuffer = FloatBuffer.wrap(dstValues);
        load(dstFloatBuffer);
    }
    
    public Matrix4f(
        float m00, float m01, float m02, float m03, 
        float m10, float m11, float m12, float m13, 
        float m20, float m21, float m22, float m23,
        float m30, float m31, float m32, float m33) {
        this();
        float[] srcValues = new float[] {
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33
        };
        FloatBuffer srcFloatBuffer = FloatBuffer.wrap(srcValues);
        load(srcFloatBuffer);
    }

    public Matrix4f(Matrix4f other) {
        this.impl = other.impl.copy();
    }

    public Matrix4f copy() {
        return new Matrix4f(this);
    }

    public com.mojang.math.Matrix4f asMoj() {
        return impl;
    }

    public static Matrix4f translation(float x, float y, float z) {
        Matrix4f result = new Matrix4f();
        result.impl.translate(new com.mojang.math.Vector3f(x, y, z));
        return result;
    }

    public void multiply(Matrix4f other) {
        impl.multiply(other.impl);
    }

    public void multiply(Quaternionf q) {
        impl.multiply(q.asMoj());
    }

    public void store(FloatBuffer buffer) {
        impl.store(buffer);
    }

    public void load(FloatBuffer buffer) {
        impl.load(buffer);
    }

    public void rotateX(float rad) {
        impl.multiply(com.mojang.math.Vector3f.XP.rotation(rad));
    }

    public void rotateY(float rad) {
        impl.multiply(com.mojang.math.Vector3f.YP.rotation(rad));
    }

    public void rotateZ(float rad) {
        impl.multiply(com.mojang.math.Vector3f.ZP.rotation(rad));
    }

    public void rotate(Vector3f axis, float rad) {
        impl.multiply(axis.impl.rotation(rad));
    }

    public void translate(float x, float y, float z) {
        impl.multiplyWithTranslation(x, y, z);
    }

    public Vector3f transform(Vector3f src) {
        com.mojang.math.Vector4f pos4 = new com.mojang.math.Vector4f(src.x(), src.y(), src.z(), 1.0F);
        pos4.transform(impl);
        return new Vector3f(pos4.x() / pos4.w(), pos4.y() / pos4.w(), pos4.z() / pos4.w());
    }

    public Vector3f transform3(Vector3f src) {
        Vector3f pos3 = src.copy();
        pos3.impl.transform(new com.mojang.math.Matrix3f(impl));
        return pos3;
    }

    public com.mojang.math.Matrix3f getRotationPart() {
        float[] srcValues = new float[16];
        FloatBuffer srcFloatBuffer = FloatBuffer.wrap(srcValues);
        impl.store(srcFloatBuffer);
        ByteBuffer dstBuffer = ByteBuffer.allocate(9 * 4);
        FloatBuffer dstFloatBuffer = dstBuffer.asFloatBuffer();
        dstFloatBuffer.put(srcValues, 0, 3);
        dstFloatBuffer.put(srcValues, 4, 3);
        dstFloatBuffer.put(srcValues, 8, 3);
        com.mojang.math.Matrix3f result = new com.mojang.math.Matrix3f();
        result.load(dstFloatBuffer);
        return result;
    }

    public Vector3f getTranslationPart() {
        float[] srcValues = new float[16];
        FloatBuffer srcFloatBuffer = FloatBuffer.wrap(srcValues);
        impl.store(srcFloatBuffer);
        return new Vector3f(srcValues[12], srcValues[13], srcValues[14]);
    }

    public void scale(float x, float y, float z) {
        float[] values = new float[16];
        FloatBuffer floatBuffer = FloatBuffer.wrap(values);
        impl.store(floatBuffer);
        values[0] *= x;
        values[1] *= x;
        values[2] *= x;
        values[3] *= x;
        values[4] *= y;
        values[5] *= y;
        values[6] *= y;
        values[7] *= y;
        values[8] *= z;
        values[9] *= z;
        values[10] *= z;
        values[11] *= z;
        impl.load(FloatBuffer.wrap(values));
    }

#endif
    public Matrix4f(
        double m00, double m01, double m02, double m03, 
        double m10, double m11, double m12, double m13, 
        double m20, double m21, double m22, double m23,
        double m30, double m31, double m32, double m33
    ) {
        this(
            (float) m00, (float) m01, (float) m02, (float) m03,
            (float) m10, (float) m11, (float) m12, (float) m13,
            (float) m20, (float) m21, (float) m22, (float) m23,
            (float) m30, (float) m31, (float) m32, (float) m33
        );
    }


    public void mul(Matrix4f other) {
        multiply(other);
    }

    public void translate(Vector3f vec) {
        translate(vec.x(), vec.y(), vec.z());
    }

    public void rotateXYZ(float x, float y, float z) {
        rotateX(x);
        rotateY(y);
        rotateZ(z);
    }

    public void rotateXYZ(Vector3f vec) {
        rotateXYZ(vec.x(), vec.y(), vec.z());
    }

    public void rotateYXZ(float y, float x, float z) {
        rotateY(y);
        rotateX(x);
        rotateZ(z);
    }

    public void rotateYXZ(Vector3f vec) {
        rotateYXZ(vec.y(), vec.x(), vec.z());
    }

    public void rotateZYX(float z, float y, float x) {
        rotateZ(z);
        rotateY(y);
        rotateX(x);
    }

    public void rotateZYX(Vector3f vec) {
        rotateZYX(vec.z(), vec.y(), vec.x());
    }

    public Vector3f getEulerAnglesZYX() {
        float[] src = new float[16];
        FloatBuffer srcFloatBuffer = FloatBuffer.wrap(src);
        store(srcFloatBuffer);

        float x = (float) Math.atan2(src[index(1, 2)], src[index(2, 2)]);
        float y = (float) Math.atan2(-src[index(0, 2)], Math.sqrt(1.0F - src[index(0, 2)] * src[index(0, 2)]));
        float z = (float) Math.atan2(src[index(0, 1)], src[index(0, 0)]);
        return new Vector3f(x, y, z);
    }

    public Vector3f getEulerAnglesXYZ() {
        float[] src = new float[16];
        FloatBuffer srcFloatBuffer = FloatBuffer.wrap(src);
        store(srcFloatBuffer);

        float x = (float) Math.atan2(-src[index(2, 1)], src[index(2, 2)]);
        float y = (float) Math.atan2(src[index(2, 0)], Math.sqrt(1.0F - src[index(2, 0)] * src[index(2, 0)]));
        float z = (float) Math.atan2(-src[index(1, 0)], src[index(0, 0)]);
        return new Vector3f(x, y, z);
    }

    public Vector3f getEulerAnglesYXZ() {
        float[] src = new float[16];
        FloatBuffer srcFloatBuffer = FloatBuffer.wrap(src);
        store(srcFloatBuffer);

        float x = (float) Math.atan2(-src[index(2, 1)], Math.sqrt(1.0F - src[index(2, 1)] * src[index(2, 1)]));
        float y = (float) Math.atan2(src[index(2, 0)], src[index(2, 2)]);
        float z = (float) Math.atan2(src[index(1, 0)], src[index(1, 1)]);
        return new Vector3f(x, y, z);
    }

    public void shear(
        float xy, float xz,
        float yx, float yz,
        float zx, float zy
    ) {
        multiply(new Matrix4f(
            1, xy, xz, 0, 
            yx, 1, yz, 0,
            zx, zy, 1, 0,
            0, 0, 0, 1
        ));
    }

    public void translate(double x, double y, double z) {
        translate((float) x, (float) y, (float) z);
    }

    int index(int p_27642_, int p_27643_) {
      return p_27643_ * 4 + p_27642_;
    }

    @Override
    public int hashCode() {
        return impl.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Matrix4f matrix4f = (Matrix4f) o;

        return impl.equals(matrix4f.impl);
    }

    public String toString() {
        float[] src = new float[16];
        FloatBuffer srcFloatBuffer = FloatBuffer.wrap(src);
        store(srcFloatBuffer);
        StringBuilder sb = new StringBuilder();
        sb.append("Matrix4f[\n");
        for (int i = 0; i < 4; i++) {
            sb.append("  ");
            for (int j = 0; j < 4; j++) {
                sb.append(src[j * 4 + i]);
                sb.append(", ");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Matrix4f getAsMatrix4f() {
        return this;
    }

    @Override
    public Matrix3f getAsMatrix3f() {
        return new Matrix3f(this);
    }

    @Override
    public Pose getAsPose() {
        return new Pose(this);
    }

    @Override
    public PoseStack getAsPoseStack() {
        return new PoseStack(getAsPose());
    }

    @Override
    public Matrices getAsMatrices() {
        return new Matrices(this);
    }

    public static final Matrix4f IDENTITY = new Matrix4f();
}
