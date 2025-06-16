package cn.zbx1425.sowcer.math;

#if MC_VERSION < "11903"
import com.mojang.math.Quaternion;
#else 
import org.joml.AxisAngle4f;
#endif

public class Quaternionf{
    public Quaternionf() {
        this(Vector3f.ZERO, 0.0f);
    }

#if MC_VERSION >= "11903"
    private final org.joml.Quaternionf impl;

    public Quaternionf(Vector3f axis, float angle) {
        this.impl = new org.joml.Quaternionf(new AxisAngle4f(angle, axis.asMoj()));
    }

    public Quaternionf(Quaternionf other) {
        this.impl = new org.joml.Quaternionf(other.impl);
    }

    public Quaternionf mul(Quaternionf other) {
        this.impl.mul(other.impl);
        return this;
    }

    public float i() {
        return impl.x;
    }

    public float j() {
        return impl.y;
    }

    public float k() {
        return impl.z;
    }

    public float r() {
        return impl.w;
    }

    public void set(float i, float j, float k, float r) {
        impl.x = i;
        impl.y = j;
        impl.z = k;
        impl.w = r;
    }

    public void i(float i) {
        impl.x = i;
    }

    public void j(float j) {
        impl.y = j;
    }

    public void k(float k) {
        impl.z = k;
    }

    public void r(float r) {
        impl.w = r;
    }

    public org.joml.Quaternionf asMoj() {
        return impl;
    }

#else
    private final Quaternion impl;

    public Quaternionf(Vector3f axis, float angle) {
        this.impl = new Quaternion(axis.asMoj(), angle, false);
    }

    public Quaternionf(Quaternion impl) {
        this.impl = impl;
    }

    public Quaternionf(Quaternionf other) {
        this.impl = new Quaternion(other.impl);
    }

    public Quaternionf mul(Quaternionf other) {
        this.impl.mul(other.impl);
        return this;
    }

    public float i() {
        return impl.i();
    }

    public float j() {
        return impl.j();
    }

    public float k() {
        return impl.k();
    }

    public float r() {
        return impl.r();
    }

    public void set(float i, float j, float k, float r) {
        impl.set(i, j, k, r);
    }

    public void i(float i) {
        set(i, j(), k(), r());
    }

    public void j(float j) {
        set(i(), j, k(), r());
    }

    public void k(float k) {
        set(i(), j(), k, r());
    }

    public void r(float r) {
        set(i(), j(), k(), r);
    }

    public Quaternion asMoj() {
        return impl;
    }
#endif

    public Quaternionf rotateX(double angle) {
        return rotateX((float) angle);
    }

    public Quaternionf rotateY(double angle) {
        return rotateY((float) angle);
    }

    public Quaternionf rotateZ(double angle) {
        return rotateZ((float) angle);
    }

    public Quaternionf rotateX(float angle) {
        rotate(Vector3f.XP, angle);
        return this;
    }

    public Quaternionf rotateY(float angle) {
        rotate(Vector3f.YP, angle);
        return this;
    }

    public Quaternionf rotateZ(float angle) {
        rotate(Vector3f.ZP, angle);
        return this;
    }

    public Quaternionf rotate(Vector3f axis, float angle) {
        mul(new Quaternionf(axis, angle));
        return this;
    }
}