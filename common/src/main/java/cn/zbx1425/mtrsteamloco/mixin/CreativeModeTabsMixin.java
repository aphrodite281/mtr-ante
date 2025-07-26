package cn.zbx1425.mtrsteamloco.mixin;

import net.minecraft.world.item.CreativeModeTabs;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeTabs.class)
public class CreativeModeTabsMixin {
    @Inject(method = "tabs", at = @At("HEAD"), cancellable = true)
    private static List<CreativeModeTab> tabs() {
        return CreativeModeTabs.allTabs();
    }
}