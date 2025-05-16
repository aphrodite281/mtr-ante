package cn.zbx1425.sowcer.math;

public class PoseStack implements Posture {
    private final com.mojang.blaze3d.vertex.PoseStack impl;

    public PoseStack() {
        this.impl = new com.mojang.blaze3d.vertex.PoseStack();
    }

    public PoseStack(com.mojang.blaze3d.vertex.PoseStack impl) {
        this.impl = impl;
    }

    public PoseStack(Pose pose) {
        this();
#if MC_VERSION >= "11903"
        this.impl.last().pose().mul(pose.pose().asMoj());
#else
        this.impl.last().pose().multiply(pose.pose().asMoj());
#endif
        this.impl.last().normal().mul(pose.normal().asMoj());
    }

    public void pushPose() {
        impl.pushPose();
    }

    public void popPose() {
        impl.popPose();
    }

    public void mul(Quaternionf rotation) {
        impl.mulPose(rotation.asMoj());
    }

    public Pose last() {
        return new Pose(impl.last());
    }

    public boolean clear() {
        return impl.clear();
    }

    public void setIdentity() {
        impl.setIdentity();
    }

    @Override
    public PoseStack getAsPoseStack() {
        return this;
    }

    @Override
    public Pose getAsPose() {
        return last();
    }

    @Override
    public Matrix4f getAsMatrix4f() {
        return last().pose();
    }

    @Override
    public Matrix3f getAsMatrix3f() {
        return last().normal();
    }

    @Override
    public Matrices getAsMatrices() {
        return new Matrices(getAsMatrix4f());
    }

}