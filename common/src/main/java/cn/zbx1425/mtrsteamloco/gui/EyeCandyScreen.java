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
import cn.zbx1425.mtrsteamloco.scripting.eyecandy.EyeCandyScriptContext;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.gui.screens.Screen;
import cn.zbx1425.mtrsteamloco.Main;
import net.minecraft.network.chat.Component;
import cn.zbx1425.mtrsteamloco.ClientConfig;
import me.shedaniel.clothconfig2.gui.entries.*;
import net.minecraft.client.gui.components.Button;
import cn.zbx1425.mtrsteamloco.gui.entries.*;
import com.mojang.blaze3d.platform.Window;
import cn.zbx1425.mtrsteamloco.data.ConfigResponder;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.Function;

public class EyeCandyScreen {

    public static Screen createScreen(BlockPos blockPos, Screen parent) {
        Optional<BlockEntityEyeCandy> opt = getBlockEntity(blockPos);
        BlockEntityEyeCandy blockEntity = opt.orElse(null);
        if (blockEntity == null) {
            return null;
        }

        List<Consumer<BlockEntityEyeCandy>> update = new ArrayList<>();// updateBlockEntityCallbacks;

        EyeCandyProperties properties = blockEntity.getProperties();
        String pid = "";
        if (properties != null) {
            pid = properties.name.getString() + " (" + blockEntity.prefabId + ")";
        }

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(tr("title"))
                .setDoesConfirmSave(false)
                .setSavingRunnable(() -> {
                    for (Consumer<BlockEntityEyeCandy> callback : update) {
                        callback.accept(blockEntity);
                    }
                    blockEntity.sendUpdateC2S();
                })
                .transparentBackground();
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory common = builder.getOrCreateCategory(
                Text.translatable("gui.mtrsteamloco.config.client.category.common")
        );

        common.addEntry(ButtonListEntry.createCenteredInstance(
            Text.translatable("gui.mtrsteamloco.eye_candy.present", properties.name.getString()),
            btn -> Minecraft.getInstance().setScreen(createSelectScreen(blockEntity, () -> createScreen(blockPos, parent)))));

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

        common.addEntry(entryBuilder.startTextDescription(
                    Text.translatable("gui.mtrsteamloco.eye_candy.shape", blockEntity.getShape())
            ).build());

        common.addEntry(entryBuilder.startTextDescription(
                    Text.translatable("gui.mtrsteamloco.eye_candy.collision", blockEntity.getCollisionShape())
            ).build());

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

            common.addEntry(entryBuilder.startTextDescription(
                    Text.literal("SX: " + blockEntity.scaleX + ", SY: " + blockEntity.scaleY + ", SZ: " + blockEntity.scaleZ)
            ).build());
        } else {
            List<SliderOrTextFieldListEntry> entries = new ArrayList<>();

            common.addEntry(new SimpleButtonListEntry(
                tr("adjust_settings_and_reset_pose"), 
                Text.translatable("gui.mtrsteamloco.adjust_settings"),
                btn -> Minecraft.getInstance().setScreen(createAdjustScreen(blockPos, () -> createScreen(blockPos, parent))),
                entryBuilder.getResetButtonKey(), 
                btn -> {
                    blockEntity.translateX = 0;
                    blockEntity.translateY = 0;
                    blockEntity.translateZ = 0;
                    blockEntity.rotateX = 0;
                    blockEntity.rotateY = 0;
                    blockEntity.rotateZ = 0;
                    blockEntity.scaleX = 1;
                    blockEntity.scaleY = 1;
                    blockEntity.scaleZ = 1;
                    blockEntity.sendUpdateC2S();
                    for (int i = 0; i < 9; i++) {
                        entries.get(i).setValue(getValue(i, blockEntity));
                    }
                },
                entryBuilder.getResetButtonKey()
            ));
        
            addTranslation(entries, common, entryBuilder, 0, blockEntity);
            addTranslation(entries, common, entryBuilder, 1, blockEntity);
            addTranslation(entries, common, entryBuilder, 2, blockEntity);
            addRotation(entries, common, entryBuilder, 3, blockEntity);
            addRotation(entries, common, entryBuilder, 4, blockEntity);
            addRotation(entries, common, entryBuilder, 5, blockEntity);
            addScale(entries, common, entryBuilder, 6, blockEntity);
            addScale(entries, common, entryBuilder, 7, blockEntity);
            addScale(entries, common, entryBuilder, 8, blockEntity);
        }

        List<AbstractConfigListEntry> customEntrys = ConfigResponder.getEntrysFromMaps(blockEntity.getCustomConfigs(), blockEntity.getCustomResponders(), entryBuilder, () -> createScreen(blockPos, parent));
        for (AbstractConfigListEntry entry : customEntrys) {
            common.addEntry(entry);
        }

        return builder.build();
    }

    private static Screen createAdjustScreen(BlockPos blockPos, Supplier<Screen> parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(new FakeScreen(parent))
                .setTitle(Text.translatable("gui.mtrsteamloco.adjust_settings"))
                .setDoesConfirmSave(false)
                .setSavingRunnable(() -> {
                    ClientConfig.save();
                })
                .transparentBackground();
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory common = builder.getOrCreateCategory(
                Text.translatable("gui.mtrsteamloco.config.client.category.common")
        );

        List<AbstractConfigListEntry> entries = new ArrayList<>();
        ClientConfig.eyecandyScreenGroup.getListEntries(entries, entryBuilder, () -> createAdjustScreen(blockPos, parent));

        for (AbstractConfigListEntry entry : entries) {
            common.addEntry(entry);
        }

        return builder.build();
    }

    private static Screen createSelectScreen(BlockEntityEyeCandy blockEntity, Supplier<Screen> parent) {
        return new SelectScreen(parent,  EyeCandyRegistry.TREE, () -> blockEntity.prefabId, (mc, screen, key) -> {
            blockEntity.setPrefabId(key);
            blockEntity.sendUpdateC2S();
        }, "https://aphrodite281.github.io/mtr-ante/#/eyecandy");
    }

    private static void save(int type, float value, BlockEntityEyeCandy blockEntity) {
        float old = getValue(type, blockEntity);
        if (old == value) return;
        switch (type) {
            case 0: blockEntity.translateX = value; break;
            case 1: blockEntity.translateY = value; break;
            case 2: blockEntity.translateZ = value; break;
            case 3: blockEntity.rotateX = value; break;
            case 4: blockEntity.rotateY = value; break;
            case 5: blockEntity.rotateZ = value; break;
            case 6: blockEntity.scaleX = value; break;
            case 7: blockEntity.scaleY = value; break;
            case 8: blockEntity.scaleZ = value; break;
        }
        blockEntity.sendUpdateC2S();
    }

    private static float getValue(int type, BlockEntityEyeCandy blockEntity) {
        switch (type) {
            case 0: return blockEntity.translateX;
            case 1: return blockEntity.translateY;
            case 2: return blockEntity.translateZ;
            case 3: return blockEntity.rotateX;
            case 4: return blockEntity.rotateY;
            case 5: return blockEntity.rotateZ;
            case 6: return blockEntity.scaleX;
            case 7: return blockEntity.scaleY;
            case 8: return blockEntity.scaleZ;
        }
        return 0;
    }

    private static String getStr(int type, BlockEntityEyeCandy blockEntity) {
        switch (type) {
            case 0: return "TX";
            case 1: return "TY";
            case 2: return "TZ";
            case 3: return "RX";
            case 4: return "RY";
            case 5: return "RZ";
            case 6: return "SX";
            case 7: return "SY";
            case 8: return "SZ";
        }
        return "";
    }

    private static void addTranslation(List<SliderOrTextFieldListEntry> entries, ConfigCategory common, ConfigEntryBuilder entryBuilder, int type, BlockEntityEyeCandy blockEntity) {
        ClientConfig.Entry e = ClientConfig.eyecandyScreenGroup.entries[0];
        int type0 = type;
        SliderOrTextFieldListEntry entry = new SliderOrTextFieldListEntry(
            Text.literal(getStr(type, blockEntity)),
            entryBuilder.getResetButtonKey(),
            getValue(type, blockEntity),
            e.min, e.max, e.step, f -> String.format("%.0fCM", f * 100),
            f -> save(type, f, blockEntity),
            str -> parseMovement(str),
            e.modes[type0], i -> {
                e.modes[type0] = i;
                ClientConfig.save();
            }
        );

        common.addEntry(entry);
        entries.add(entry);
    }

    private static void addRotation(List<SliderOrTextFieldListEntry> entries, ConfigCategory common, ConfigEntryBuilder entryBuilder, int type, BlockEntityEyeCandy blockEntity) {
        ClientConfig.Entry e = ClientConfig.eyecandyScreenGroup.entries[1];
        int type0 = type - 3;
        SliderOrTextFieldListEntry entry = new SliderOrTextFieldListEntry(
            Text.literal(getStr(type, blockEntity)),
            entryBuilder.getResetButtonKey(),
            getValue(type, blockEntity) * 180 / (float) Math.PI,
            e.min, e.max, e.step, f -> String.format("%.0f°", f),
            f -> save(type, f / 180 * (float) Math.PI, blockEntity),
            str -> parseRotation(str),
            e.modes[type0], i -> {
                e.modes[type0] = i;
                ClientConfig.save();
            }
        );

        common.addEntry(entry);
        entries.add(entry);
    }

    private static void addScale(List<SliderOrTextFieldListEntry> entries, ConfigCategory common, ConfigEntryBuilder entryBuilder, int type, BlockEntityEyeCandy blockEntity) {
        ClientConfig.Entry e = ClientConfig.eyecandyScreenGroup.entries[2];
        int type0 = type - 6;
        SliderOrTextFieldListEntry entry = new SliderOrTextFieldListEntry(
            Text.literal(getStr(type, blockEntity)),
            entryBuilder.getResetButtonKey(),
            getValue(type, blockEntity),
            e.min, e.max, e.step, f -> String.format("%.2f", f),
            f -> save(type, f, blockEntity),
            str -> parseScale(str),
            e.modes[type0], i -> {
                e.modes[type0] = i;
                ClientConfig.save();
            }
        );

        common.addEntry(entry);
        entries.add(entry);
    }

    private static Optional<Float> parseScale(String str) {
        try {
            Float value = Float.parseFloat(str);
            return Optional.of(value);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
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
            return Optional.of(value);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<BlockEyeCandy.BlockEntityEyeCandy> getBlockEntity(BlockPos blockPos) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return Optional.empty();
        return level.getBlockEntity(blockPos, Main.BLOCK_ENTITY_TYPE_EYE_CANDY.get());
    }

    private static Component tr(String key) {
        return Text.translatable("gui.mtrsteamloco.eye_candy." + key);
    }
}
