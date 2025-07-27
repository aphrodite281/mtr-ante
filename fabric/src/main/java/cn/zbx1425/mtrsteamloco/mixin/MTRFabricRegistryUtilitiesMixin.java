package cn.zbx1425.mtrsteamloco.mixin;

import mtr.mappings.FabricRegistryUtilities;
#if MC_VERSION >= "12000"
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import mtr.mappings.Text;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
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

@Mixin(FabricRegistryUtilities.class)
public interface MTRFabricRegistryUtilitiesMixin {
#if MC_VERSION >= "12000"
    @Inject(method = "registerCreativeModeTab", at = @At("HEAD"), cancellable = true)
    private static void onRegisterCreativeModeTab(CreativeModeTab creativeModeTab, Item item, CallbackInfo ci) {
        ci.cancel();
		ItemGroupEvents
				.modifyEntriesEvent(BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(creativeModeTab).orElseThrow())
				.register(entries -> entries.accept(item));
	}

    @Inject(method = "createCreativeModeTab", at = @At("HEAD"), cancellable = true)
    private static void onCreateCreativeModeTab(ResourceLocation id, Supplier<ItemStack> supplier, CallbackInfoReturnable<CreativeModeTab> cir) {
		CreativeModeTab tab = FabricItemGroup.builder()
				.icon(supplier)
				.title(Text.translatable(String.format("itemGroup.%s.%s", id.getNamespace(), id.getPath())))
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab);
		cir.setReturnValue(tab);
        cir.cancel();
	}
#endif
}