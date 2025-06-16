package cn.zbx1425.mtrsteamloco.mixin;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
#if MC_VERSION >= "11903"
import org.joml.Matrix3f;
import org.joml.Matrix4f;
#else 
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
#endif

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Pose.class)
public interface PoseAccessor {
    @Invoker(value = "<init>")
    static Pose create(Matrix4f mat4f, Matrix3f mat3f) {
        throw new AssertionError();
    }
}