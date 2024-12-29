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
import net.minecraft.client.gui.screens.Screen;
import cn.zbx1425.mtrsteamloco.Main;

import java.util.*;
import java.util.function.Consumer;

public class EyeCandyScreen {
    public class SelectEyeCandyScreen extends SelectListScreen {
        Screen parent;
        BlockEntityEyeCandy blockEntity;

        private static final String INSTRUCTION_LINK = "https://www.zbx1425.cn/nautilus/mtr-nte/#/eyecandy";
        private final WidgetLabel lblInstruction = new WidgetLabel(0, 0, 0, Text.translatable("gui.mtrsteamloco.eye_candy.tip_resource_pack"), () -> {
            this.minecraft.setScreen(new ConfirmLinkScreen(bl -> {
                if (bl) {
                    Util.getPlatform().openUri(INSTRUCTION_LINK);
                }
                this.minecraft.setScreen(this);
            }, INSTRUCTION_LINK, true));
        });

        public SelectEyeCandyScreen(Screen parent, BlockEntityEyeCandy blockEntity) {
            super(Text.literal("Select EyeCandy"));
            this.parent = parent;
            this.blockEntity = blockEntity;
        }

            @Override
#if MC_VERSION >= "12000"
        public void render(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
#else
        public void render(@NotNull PoseStack guiGraphics, int i, int j, float f) {
#endif
            this.renderBackground(guiGraphics);
            super.render(guiGraphics, i, j, f);
            super.renderSelectPage(guiGraphics);
        }

        @Override
        protected void init() {
            super.init();

            loadPage();
        }

        @Override
        protected void loadPage() {
            clearWidgets();

            scrollList.visible = true;
            loadSelectPage(key -> !key.equals(blockEntity.prefabId));
            lblInstruction.alignR = true;
            IDrawing.setPositionAndWidth(lblInstruction, width / 2 + SQUARE_SIZE, height - SQUARE_SIZE - TEXT_HEIGHT, 0);
            lblInstruction.setWidth(width / 2 - SQUARE_SIZE * 2);
            addRenderableWidget(lblInstruction);
        }

        @Override
        protected void onBtnClick(String btnKey) {
            if (blockEntity.prefabId != btnKey) {
                EyeCandyProperties oldProp = EyeCandyRegistry.elements.get(blockEntity.prefabId);
                if (oldProp != null && oldProp.script != null) {
                    oldProp.script.tryCallDisposeFunctionAsync(blockEntity.scriptContext);
                }
                blockEntity.prefabId = btnKey;
                EyeCandyProperties newProp = EyeCandyRegistry.elements.get(btnKey);
                if (newProp != null && newProp.script != null) {
                    blockEntity.scriptContext = new EyeCandyScriptContext(blockEntity);
                }
                blockEntity.shape = newProp.shape;
                blockEntity.noCollision = newProp.noCollision;
                blockEntity.fixedShape = newProp.fixedShape;
                blockEntity.fixedMatrix = newProp.fixedMatrix;
                blockEntity.lightLevel = newProp.lightLevel;
                blockEntity.data.clear();
                PacketUpdateBlockEntity.sendUpdateC2S(blockEntity);
            }
        }

        @Override
        protected List<Pair<String, String>> getRegistryEntries() {
            return EyeCandyRegistry.elements.entrySet().stream()
                    .map(e -> new Pair<>(e.getKey(), e.getValue().name.getString()))
                    .toList();
        }

        @Override
        public void onClose() {
            this.minecraft.setScreen(parent);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private final BlockPos blockPos;
    private final List<Consumer<BlockEntityEyeCandy>> updateBlockEntityCallbacks = new ArrayList<>();
    public Screen screen;
    private final BlockEntityEyeCandy blockEntity;

    public EyeCandyScreen(BlockPos blockPos) {
        this.blockPos = blockPos;
        Optional<BlockEntityEyeCandy> optionalBlockEntity = getBlockEntity();
        if (!optionalBlockEntity.isEmpty()) {
            blockEntity = optionalBlockEntity.get();
        } else {
            Main.LOGGER.error("Cannot find block entity at " + blockPos);
            blockEntity = null;
        }
    }

    public void setScreen() {
        Minecraft.getInstance().setScreen(createScreen());
    }

    public Screen createScreen() {
        if (blockEntity == null) {
            return null;
        }

        EyeCandyProperties properties = EyeCandyRegistry.elements.get(blockEntity.prefabId);

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(null)
                .setTitle(Text.translatable("gui.mtrsteamloco.config.client.title"))
                .setDoesConfirmSave(false)
                .transparentBackground();
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory common = builder.getOrCreateCategory(
                Text.translatable("gui.mtrsteamloco.config.client.category.common")
        );

        common.addEntry(entryBuilder.startTextDescription(
                Text.literal("当前模型: " + (properties != null ? properties.name.getString() : blockEntity.prefabId + " (???)"))
        ).build());

        common.addEntry(new ButtonListEntry(
                Text.literal("选择模型"),
                sender -> Minecraft.getInstance().setScreen(new SelectEyeCandyScreen(screen, blockEntity))
        ));

        common.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("gui.mtrsteamloco.eye_candy.full_light"),
                        blockEntity.fullLight
                ).setSaveConsumer(checked -> {
                    if (checked != blockEntity.fullLight) {
                        updateBlockEntity(be -> be.fullLight = checked);
                    }
                }).setDefaultValue(false).build()
        );

        common.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.literal("当作站台"),
                        blockEntity.platform
                ).setSaveConsumer(checked -> {
                    if (checked != blockEntity.platform) {
                        updateBlockEntity(be -> be.platform = checked);
                    }
                }).setDefaultValue(false).build()
        );

        if (blockEntity.fixedMatrix) {
            common.addEntry(entryBuilder.startTextDescription(
                    Text.literal("模型位置已固定，无法编辑")
            ).build());

            common.addEntry(entryBuilder.startTextDescription(
                    Text.literal("TX: " + blockEntity.translateX + "cm, TY: " + blockEntity.translateY + "cm, TZ: " + blockEntity.translateZ + "cm")
            ).build());

            common.addEntry(entryBuilder.startTextDescription(
                    Text.literal("RX: " + Math.toDegrees(blockEntity.rotateX) + "°, RY: " + Math.toDegrees(blockEntity.rotateY) + "°, RZ: " + Math.toDegrees(blockEntity.rotateZ) + "°")
            ).build());
        } else {
            StringListEntry tx = entryBuilder.startTextField(
                            Text.literal("TX"),
                            blockEntity.translateX + "cm"
                ).setSaveConsumer(str -> {
                    try {
                        Float value = 0f;
                        if (str.endsWith("cm")) {
                            value = Float.parseFloat(str.substring(0, str.length() - 2));
                        } else if (str.endsWith("m")) {
                            value = Float.parseFloat(str.substring(0, str.length() - 1)) * 100;
                        } else {
                            value = Float.parseFloat(str);
                        }
                        if (value != blockEntity.translateX) {
                            final float v = value;
                            updateBlockEntity(be -> be.translateX = v);
                        }
                    } catch (NumberFormatException e) {
                        // tx.setValue(blockEntity.translateX + "cm");
                    }  
                }).setDefaultValue(blockEntity.translateX + "cm")
                .build();

            common.addEntry(tx);

            StringListEntry ty = entryBuilder.startTextField(
                            Text.literal("TY"),
                            blockEntity.translateY + "cm"
                ).setSaveConsumer(str -> {
                    try {
                        Float value = 0f;
                        if (str.endsWith("cm")) {
                            value = Float.parseFloat(str.substring(0, str.length() - 2));
                        } else if (str.endsWith("m")) {
                            value = Float.parseFloat(str.substring(0, str.length() - 1)) * 100;
                        } else {
                            value = Float.parseFloat(str);
                        }
                        if (value != blockEntity.translateY) {
                            final float v = value;
                            updateBlockEntity(be -> be.translateY = v);
                        }
                    } catch (NumberFormatException e) {
                        // tx.setValue(blockEntity.translateY + "cm");
                    }  
                }).setDefaultValue(blockEntity.translateY + "cm")
                .build();

            common.addEntry(ty);

            StringListEntry tz = entryBuilder.startTextField(
                            Text.literal("TZ"),
                            blockEntity.translateZ + "cm"
                ).setSaveConsumer(str -> {  
                    try {
                        Float value = 0f;
                        if (str.endsWith("cm")) {
                            value = Float.parseFloat(str.substring(0, str.length() - 2));
                        } else if (str.endsWith("m")) {
                            value = Float.parseFloat(str.substring(0, str.length() - 1)) * 100;
                        } else {
                            value = Float.parseFloat(str);
                        }
                        if (value != blockEntity.translateZ) {
                            final float v = value;
                            updateBlockEntity(be -> be.translateZ = v);
                        }
                    } catch (NumberFormatException e) {
                        // tz.setValue(blockEntity.translateZ + "cm");
                    }  
                }).setDefaultValue(blockEntity.translateZ + "cm")
                .build();

            common.addEntry(tz);

            StringListEntry rx = entryBuilder.startTextField(
                            Text.literal("RX"),
                            blockEntity.rotateX + "°"
                ).setSaveConsumer(str -> {  
                    try {
                        Float value = 0f;
                        if (str.endsWith("°")) {
                            value = Float.parseFloat(str.substring(0, str.length() - 1));
                        } else {
                            value = Float.parseFloat(str);
                        }
                        if (value != blockEntity.rotateX) {
                            final float v = value;
                            updateBlockEntity(be -> be.rotateX = v);
                        }
                    } catch (NumberFormatException e) {
                        // tz.setValue(blockEntity.translateZ + "cm");
                    }  
                }).setDefaultValue(blockEntity.rotateX + "°")
                .build();

            common.addEntry(rx);

            StringListEntry ry = entryBuilder.startTextField(
                            Text.literal("RY"),
                            blockEntity.rotateY + "°"
                ).setSaveConsumer(str -> {  
                    try {
                        Float value = 0f;
                        if (str.endsWith("°")) {
                            value = Float.parseFloat(str.substring(0, str.length() - 1));
                        } else {
                            value = Float.parseFloat(str);
                        }
                        if (value != blockEntity.rotateY) {
                            final float v = value;
                            updateBlockEntity(be -> be.rotateY = v);
                        }
                    } catch (NumberFormatException e) {
                        // tz.setValue(blockEntity.translateZ + "cm");
                    }  
                }).setDefaultValue(blockEntity.rotateY + "°")
                .build();

            common.addEntry(ry);

            StringListEntry rz = entryBuilder.startTextField(
                            Text.literal("RZ"),
                            blockEntity.rotateZ + "°"
                ).setSaveConsumer(str -> {  
                    try {
                        Float value = 0f;
                        if (str.endsWith("°")) {
                            value = Float.parseFloat(str.substring(0, str.length() - 1));
                        } else {
                            value = Float.parseFloat(str);
                        }
                        if (value != blockEntity.rotateZ) {
                            final float v = value;
                            updateBlockEntity(be -> be.rotateZ = v);
                        }
                    } catch (NumberFormatException e) {
                        // tz.setValue(blockEntity.translateZ + "cm");
                    }  
                }).setDefaultValue(blockEntity.rotateZ + "°")
                .build();

            common.addEntry(rz);
        }

        Set<String> keys = new HashSet<>(blockEntity.data.keySet());
        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys);
        if (!sortedKeys.isEmpty()) {
            common.addEntry(entryBuilder.startTextDescription(
                    Text.literal("自定义数据")
            ).build());
            for (String key : sortedKeys) {
                String value = blockEntity.data.get(key);
                common.addEntry(entryBuilder.startTextField(
                        Text.literal(key),
                        value
                ).setSaveConsumer(str -> {
                    if (!str.equals(value)) {
                        updateBlockEntity(be -> be.data.put(key, str));
                    }
                }).setDefaultValue(value).build());
            }
        }
        

        builder.setSavingRunnable(() -> save());

        return builder.build();
    }

    private void updateBlockEntity(Consumer<BlockEyeCandy.BlockEntityEyeCandy> callback) {
        updateBlockEntityCallbacks.add(callback);
    }

    private void save() {
        for (Consumer<BlockEntityEyeCandy> callback : updateBlockEntityCallbacks) {
            callback.accept(blockEntity);
        }
        updateBlockEntityCallbacks.clear();
        PacketUpdateBlockEntity.sendUpdateC2S(blockEntity);
    }

    private Optional<BlockEyeCandy.BlockEntityEyeCandy> getBlockEntity() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return Optional.empty();
        return level.getBlockEntity(blockPos, Main.BLOCK_ENTITY_TYPE_EYE_CANDY.get());
    }
}
