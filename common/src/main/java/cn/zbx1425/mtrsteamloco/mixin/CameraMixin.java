
/* eslint-disable*/
package cn.zbx1425.mtrsteamloco.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.util.Mth;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.mtrsteamloco.data.Rolling;
#if MC_VERSION >= "11903"
import org.joml.Quaternionf;
#else
import com.mojang.math.Quaternion;
#endif

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow public abstract void setPosition(Vec3 p_90582_);
    @Shadow protected abstract Vec3 getPosition();
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;
#if MC_VERSION >= "11903"
    @Shadow @Final private Quaternionf rotation;

    @Unique private Quaternionf roll = new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
#else
    @Shadow private Quaternion rotation;

    @Unique private Quaternion roll = new Quaternion(0.0F, 0.0F, 0.0F, 1.0F);
#endif


    @Inject(
        method = "setup",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setPosition(DDD)V",
            ordinal = 0,
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectSetup(BlockGetter p_90576_, Entity p_90577_, boolean p_90578_, boolean p_90579_, float p_90580_, CallbackInfo ci) {
        float eyeHeight = (float) Mth.lerp(p_90580_, this.eyeHeightOld, this.eyeHeight);
        
        Vector3f pos = new Vector3f(getPosition());
        pos = Rolling.applyRolling(pos, eyeHeight);
        setPosition(pos.toVec3());
        roll = Rolling.getRollQuaternion().asMoj();
    }

#if MC_VERSION >= "11903"
    @Inject(
        method = "setRotation",
        at = @At(
            value = "INVOKE",
            target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;",
            ordinal = 0,
            shift = At.Shift.AFTER,
            remap = false
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectSetRotation(float p_90573_, float p_90574_, CallbackInfo ci) {
        rotation.mul(roll);
    }
#else
    @Inject(
        method = "setRotation",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/math/Quaternion;set(FFFF)V",
            ordinal = 0,
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectSetRotation(float p_90573_, float p_90574_, CallbackInfo ci) {
        rotation.mul(roll);
    }
#endif

}