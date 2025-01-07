package cn.zbx1425.mtrsteamloco.gui;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy;
import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy.BlockEntityEyeCandy;
import cn.zbx1425.mtrsteamloco.data.EyeCandyProperties;
import cn.zbx1425.mtrsteamloco.data.EyeCandyRegistry;
import cn.zbx1425.mtrsteamloco.network.PacketUpdateBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import mtr.client.IDrawing;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.screen.WidgetBetterCheckbox;
import mtr.screen.WidgetBetterTextField;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
#if MC_VERSION >= "12000"
import net.minecraft.client.gui.GuiGraphics;
#endif
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import cn.zbx1425.mtrsteamloco.render.scripting.eyecandy.EyeCandyScriptContext;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.gui.entries.FloatListEntry;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.client.gui.screens.Screen;
import cn.zbx1425.mtrsteamloco.Main;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.Function;

public class EyeCandyScreen {

    public static Screen createScreen(BlockPos blockPos) {
        Optional<BlockEntityEyeCandy> opt = getBlockEntity(blockPos);
        BlockEntityEyeCandy blockEntity = opt.orElse(null);
        if (blockEntity == null) {
            return null;
        }

        List<Consumer<BlockEntityEyeCandy>> update = new ArrayList<>();// updateBlockEntityCallbacks;

        EyeCandyProperties properties = EyeCandyRegistry.elements.get(blockEntity.prefabId);
        String pid = "";
        if (properties != null) {
            pid = properties.name.getString() + " (" + blockEntity.prefabId + ")";
        }

        Set<Map.Entry<String, EyeCandyProperties>> entries = EyeCandyRegistry.elements.entrySet();
        Map<String, String> elementMap = new HashMap<>();
        for (Map.Entry<String, EyeCandyProperties> entry : entries) {
            EyeCandyProperties prop = entry.getValue();
            String prid = entry.getKey();
            String name = prop.name.getString();
            elementMap.put(name + " (" + prid + ")", prid);
        }
        List<String> elementList = new ArrayList<>(elementMap.keySet());
        Collections.sort(elementList);

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(null)
                .setTitle(tr("title"))
                .setDoesConfirmSave(false)
                .transparentBackground();
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory common = builder.getOrCreateCategory(
                Text.translatable("gui.mtrsteamloco.config.client.category.common")
        );

        common.addEntry(entryBuilder.startTextDescription(
                Text.translatable("gui.mtrsteamloco.eye_candy.present", (properties != null ? properties.name.getString() : blockEntity.prefabId + " (???)"))
        ).build());

        common.addEntry(entryBuilder.startDropdownMenu(
            tr("select"),
            DropdownMenuBuilder.TopCellElementBuilder.of(pid, str -> str))
            .setDefaultValue(pid).setSelections(elementList).setSaveConsumer(btnKey -> {
                update.add(be -> {
                    be.setPrefabId(elementMap.get(btnKey));
                });
            }).build()
        );

        common.addEntry(entryBuilder
                .startBooleanToggle(
                        tr("full_light"),
                        blockEntity.fullLight
                ).setSaveConsumer(checked -> {
                    if (checked != blockEntity.fullLight) {
                        update.add(be -> be.fullLight = checked);
                    }
                }).setDefaultValue(blockEntity.fullLight).build()
        );

        common.addEntry(entryBuilder
                .startBooleanToggle(
                        tr("as_platform"), 
                        blockEntity.asPlatform
                ).setSaveConsumer(checked -> {
                    if (checked != blockEntity.asPlatform) {
                        update.add(be -> be.asPlatform = checked);
                    }
                }).setDefaultValue(blockEntity.asPlatform).build()
        );

        // common.addEntry(entryBuilder.startTextDescription(
        //             Text.translatable("gui.mtrsteamloco.eye_candy.shape", blockEntity.shape.toString())
        //     ).build());

        // common.addEntry(entryBuilder.startTextDescription(
        //             Text.translatable("gui.mtrsteamloco.eye_candy.collision", blockEntity.collision.toString())
        //     ).build());

        if (blockEntity.fixedMatrix) {
            common.addEntry(entryBuilder.startTextDescription(
                    tr("fixed")
            ).build());

            common.addEntry(entryBuilder.startTextDescription(
                    Text.literal("TX: " + blockEntity.translateX * 100 + "cm, TY: " + blockEntity.translateY * 100 + "cm, TZ: " + blockEntity.translateZ * 100 + "cm")
            ).build());

            common.addEntry(entryBuilder.startTextDescription(
                    Text.literal("RX: " + Math.toDegrees(blockEntity.rotateX) + "°, RY: " + Math.toDegrees(blockEntity.rotateY) + "°, RZ: " + Math.toDegrees(blockEntity.rotateZ) + "°")
            ).build());
        } else {
            common.addEntry(entryBuilder.startTextField(
                            Text.literal("TX"),
                            blockEntity.translateX * 100 + "cm"
                ).setSaveConsumer(str -> {
                    float value = parseMovement(str).orElse(blockEntity.translateX);
                    if (value != blockEntity.translateX) {
                        final float v = value;
                        update.add(be -> be.translateX = v);
                    } 
                }).setDefaultValue(blockEntity.translateX * 100 + "cm")
                .setErrorSupplier(verifyMovement)
                .build());

            common.addEntry(entryBuilder.startTextField(
                            Text.literal("TY"),
                            blockEntity.translateY * 100 + "cm"
                ).setSaveConsumer(str -> {
                    float value = parseMovement(str).orElse(blockEntity.translateY);
                    if (value != blockEntity.translateY) {
                        final float v = value;
                        update.add(be -> be.translateY = v);
                    } 
                }).setDefaultValue(blockEntity.translateY * 100 + "cm")
                .setErrorSupplier(verifyMovement)
                .build());

            common.addEntry(entryBuilder.startTextField(
                            Text.literal("TZ"),
                            blockEntity.translateZ * 100 + "cm"
                ).setSaveConsumer(str -> {
                    float value = parseMovement(str).orElse(blockEntity.translateZ);
                    if (value != blockEntity.translateZ) {
                        final float v = value;
                        update.add(be -> be.translateZ = v);
                    } 
                }).setDefaultValue(blockEntity.translateZ * 100 + "cm")
                .setErrorSupplier(verifyMovement)
                .build());

            common.addEntry(entryBuilder.startTextField(
                            Text.literal("RX"),
                            Math.toDegrees(blockEntity.rotateX) + "°"
                ).setSaveConsumer(str -> {  
                    Float value = parseRotation(str).orElse(blockEntity.rotateX);
                    if (value != blockEntity.rotateX) {
                        final float v = value;
                        update.add(be -> be.rotateX = v);
                    }
                }).setDefaultValue(Math.toDegrees(blockEntity.rotateX) + "°")
                .setErrorSupplier(verifyRotation)
                .build());

            common.addEntry(entryBuilder.startTextField(
                            Text.literal("RY"),
                            Math.toDegrees(blockEntity.rotateY) + "°"
                ).setSaveConsumer(str -> {
                    Float value = parseRotation(str).orElse(blockEntity.rotateY);
                    if (value != blockEntity.rotateY) {
                        final float v = value;
                        update.add(be -> be.rotateY = v);
                    }
                }).setDefaultValue(Math.toDegrees(blockEntity.rotateY) + "°")
                .setErrorSupplier(verifyRotation)
                .build());

            common.addEntry(entryBuilder.startTextField(
                            Text.literal("RZ"),
                            Math.toDegrees(blockEntity.rotateZ) + "°"
                ).setSaveConsumer(str -> {
                    Float value = parseRotation(str).orElse(blockEntity.rotateZ);
                    if (value != blockEntity.rotateZ) {
                        final float v = value;
                        update.add(be -> be.rotateZ = v);
                    }
                }).setDefaultValue(Math.toDegrees(blockEntity.rotateZ) + "°")
                .setErrorSupplier(verifyRotation)
                .build());
        }

        Set<String> keys = new HashSet<>(blockEntity.data.keySet());
        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys);
        if (!sortedKeys.isEmpty()) {
            common.addEntry(entryBuilder.startTextDescription(
                    tr("custom_data")
            ).build());
            for (String key : sortedKeys) {
                String value = blockEntity.data.get(key);
                common.addEntry(entryBuilder.startTextField(
                        Text.literal(key),
                        value
                ).setSaveConsumer(str -> {
                    if (!str.equals(value)) {
                        update.add(be -> be.data.put(key, str));
                    }
                }).setDefaultValue(value).build());
            }
        }
        

        builder.setSavingRunnable(() -> {
            for (Consumer<BlockEntityEyeCandy> callback : update) {
                callback.accept(blockEntity);
            }
            blockEntity.sendUpdateC2S();
        });

        return builder.build();
    }

    private static Optional<Float> parseMovement(String str) {
        try {
            Float value = 0f;
            str = str.toLowerCase().trim();
            if (str.endsWith("cm")) {
                value = Float.parseFloat(str.substring(0, str.length() - 2)) / 100;
            } else if (str.endsWith("m")) {
                value = Float.parseFloat(str.substring(0, str.length() - 1));
            } else {
                value = Float.parseFloat(str);
            }
            return Optional.of(value);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Float> parseRotation(String str) {
        try {
            Float value = 0f;
            str = str.toLowerCase().trim();
            if (str.endsWith("°")) {
                value = Float.parseFloat(str.substring(0, str.length() - 1));
            } else {
                value = Float.parseFloat(str);
            }
            value = (float) Math.toRadians(value);
            return Optional.of(value);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Function<String, Optional<Component>> verifyMovement = (str) -> {
        if (parseMovement(str).isEmpty()) {
            return Optional.of(Text.translatable("gui.mtrsteamloco.error.invalid_value"));
        } else {
            return Optional.empty();
        }
    };

    private static Function<String, Optional<Component>> verifyRotation = (str) -> {
        if (parseRotation(str).isEmpty()) {
            return Optional.of(Text.translatable("gui.mtrsteamloco.error.invalid_value"));
        } else {
            return Optional.empty();
        }
    };

    private static Optional<BlockEyeCandy.BlockEntityEyeCandy> getBlockEntity(BlockPos blockPos) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return Optional.empty();
        return level.getBlockEntity(blockPos, Main.BLOCK_ENTITY_TYPE_EYE_CANDY.get());
    }

    private static Component tr(String key) {
        return Text.translatable("gui.mtrsteamloco.eye_candy." + key);
    }
}
