package cn.zbx1425.mtrsteamloco.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "com.oracle.truffle.polyglot.PolyglotContextImpl", remap = false)
public abstract class FQGraal {
    @Inject(method = "checkAllThreadAccesses", at = @At("HEAD"), cancellable = true, remap = false)
    private void checkAllThreadAccesses(Thread enteringThread, boolean singleThread, CallbackInfo ci) {// throws PolyglotThreadAccessException
        System.out.println("checkAllThreadAccesses");
        ci.cancel();
    }
}