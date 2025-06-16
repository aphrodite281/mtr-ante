package cn.zbx1425.sowcer.math;

import java.util.Stack;

public class Matrices implements Posture {


    private final Stack<Matrix4f> stack = new Stack<>();

    public Matrices() {
        stack.push(new Matrix4f());
    }

    public Matrices(Matrix4f matrix) {
        stack.push(matrix.copy());
    }

    public Matrices(Matrices matrices) {
        for (Matrix4f matrix : matrices.stack) {
            stack.push(matrix.copy());
        }
    }

    public Matrices copy() {
        return new Matrices(this);
    }

    public void translate(double x, double y, double z) {
        this.translate((float)x, (float)y, (float)z);
    }

    public void translate(float x, float y, float z) {
        stack.peek().translate(x, y, z);
    }

    public void rotate(float x, float y, float z, float radian) {
        stack.peek().rotate(new Vector3f(x, y, z), radian);
    }

    public void rotateX(float radian) {
        stack.peek().rotateX(radian);
    }

    public void rotateY(float radian) {
        stack.peek().rotateY(radian);
    }

    public void rotateZ(float radian) {
        stack.peek().rotateZ(radian);
    }

    public void pushPose() {
        stack.push(stack.peek().copy());
    }

    public void popPose() {
        stack.pop();
    }

    public void popPushPose() {
        stack.pop();
        stack.push(stack.peek().copy());
    }

    public Matrix4f last() {
        return this.stack.peek();
    }

    public boolean clear() {
        return this.stack.size() == 1;
    }

    public void setIdentity() {
        stack.pop();
        stack.push(new Matrix4f());
    }

    @Override
    public PoseStack getAsPoseStack() {
        return new PoseStack(getAsPose());
    }

    @Override
    public Pose getAsPose() {
        return new Pose(last());
    }

    @Override
    public Matrix4f getAsMatrix4f() {
        return last();
    }

    @Override
    public Matrix3f getAsMatrix3f() {
        return new Matrix3f(last());
    }

    @Override
    public Matrices getAsMatrices() {
        return this;
    }
}