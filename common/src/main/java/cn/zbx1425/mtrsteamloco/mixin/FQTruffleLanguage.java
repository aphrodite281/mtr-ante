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

@Mixin(com.oracle.truffle.api.TruffleLanguage.class)
public abstract class FQTruffleLanguage {
    @Inject(method = "initializeLibgraal", at = @At("HEAD"), cancellable = true, remap = false)
    protected void isThreadAccessAllowed(CallbackInfoReturnable<Long> cir) {
        cir.cancel();
        cir.setReturnValue(0L);
    }
}