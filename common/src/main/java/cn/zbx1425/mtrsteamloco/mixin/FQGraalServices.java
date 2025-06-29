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

import java.util.Iterator;
import java.util.ServiceLoader;

@Mixin(org.graalvm.compiler.serviceprovider.GraalServices.class)
public class FQGraalServices {
    @Inject(method = "load", at = @At("HEAD"), cancellable = true, remap = false)
    private static <S> void load0(Class<S> service, CallbackInfoReturnable<Iterable<S>> cir) {
        Module module = org.graalvm.compiler.serviceprovider.GraalServices.class.getModule();
        // Graal cannot know all the services used by another module
        // (e.g. enterprise) so dynamically register the service use now.
        if (!module.canUse(service)) {
            module.addUses(service);
        }

        cir.setReturnValue(() -> {
            ModuleLayer layer = module.getLayer();
            if (layer == null) {
                layer = ModuleLayer.boot();
            }
            Iterator<S> iterator = ServiceLoader.load(layer, service).iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public S next() {
                    S provider = iterator.next();
                    // Allow Graal extensions to access JVMCI
                    openJVMCITo(provider.getClass());
                    return provider;
                }

                @Override
                public void remove() {
                    iterator.remove();
                }
            };
        });
    }

    @Shadow(remap = false)
    static void openJVMCITo(Class<?> other) {
        throw new IllegalAccessError("openJVMCITo Mixin Shadow Failure");
    }
}