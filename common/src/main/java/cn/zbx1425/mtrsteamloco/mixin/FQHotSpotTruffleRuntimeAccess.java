package cn.zbx1425.mtrsteamloco.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(com.oracle.truffle.runtime.hotspot.HotSpotTruffleRuntimeAccess.class)
public abstract class FQHotSpotTruffleRuntimeAccess {
    @Inject(method = "createRuntime", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onCreateRuntime(CallbackInfoReturnable<com.oracle.truffle.api.TruffleRuntime> cir) {
        cir.cancel();
        try {
            // HotSpotThreadLocalHandshake.initializePendingOffset();
            // HotSpotFastThreadLocal.ensureLoaded();

            Class<?> clazz = Class.forName("jdk.graal.compiler.truffle.hotspot.HotSpotTruffleCompilationSupport");
            com.oracle.truffle.compiler.TruffleCompilationSupport compilationSupport = (com.oracle.truffle.compiler.TruffleCompilationSupport) (Object) clazz.getConstructor().newInstance();
            com.oracle.truffle.runtime.hotspot.HotSpotTruffleRuntime rt = new com.oracle.truffle.runtime.hotspot.HotSpotTruffleRuntime(compilationSupport);
            // registerVirtualThreadMountHooks();
            compilationSupport.registerRuntime(rt);
            cir.setReturnValue(rt);
        } catch (Exception e) {
            e.printStackTrace();
            cir.setReturnValue(new com.oracle.truffle.api.impl.DefaultTruffleRuntime("ANTE Shield"));
        }
    }

    // @Shadow(remap = false)
    // private static void registerVirtualThreadMountHooks() {
    // }
}