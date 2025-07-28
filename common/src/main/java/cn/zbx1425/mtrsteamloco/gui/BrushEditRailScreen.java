package cn.zbx1425.mtrsteamloco.gui;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.data.*;
import cn.zbx1425.mtrsteamloco.gui.entries.ButtonListEntry;
import cn.zbx1425.mtrsteamloco.network.PacketUpdateHoldingItem;
import cn.zbx1425.mtrsteamloco.network.PacketUpdateRail;
import cn.zbx1425.mtrsteamloco.render.RailPicker;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.level.block.state.BlockState;
import io.netty.buffer.Unpooled;
import mtr.client.ClientData;
import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.buffer.ByteBufUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import cn.zbx1425.mtrsteamloco.block.BlockDirectNode.BlockEntityDirectNode;
import net.minecraft.util.FormattedCharSequence;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.FormattedText;
import mtr.client.IDrawing;
import mtr.data.Rail;
import net.minecraft.client.gui.components.AbstractSliderButton;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import net.minecraft.client.gui.screens.Screen;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import mtr.screen.WidgetBetterCheckbox;
import mtr.block.BlockNode;
import mtr.screen.WidgetBetterTextField;
import net.minecraft.util.Mth;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.Util;
import mtr.data.RailType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
#if MC_VERSION >= "12000"
import net.minecraft.client.gui.GuiGraphics;
#endif
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
#if MC_VERSION >= "12000"
import net.minecraft.client.gui.GuiGraphics;
#else
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
#endif
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import me.shedaniel.clothconfig2.gui.entries.*;
import mtr.mappings.Text;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import mtr.data.TransportMode;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.mtrsteamloco.ClientConfig;
import net.minecraft.client.gui.components.EditBox;
import me.shedaniel.clothconfig2.api.Tooltip;
import me.shedaniel.math.Point;
import cn.zbx1425.mtrsteamloco.network.util.DoubleFloatMapSerializer;
import cn.zbx1425.mtrsteamloco.gui.entries.*;
import net.minecraft.world.InteractionHand;
import cn.zbx1425.mtrsteamloco.network.PacketReplaceRailNode;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Optional;

public class BrushEditRailScreen {

    private final Rail pickedRail;
    private final RailExtraSupplier supplier;
    private final BlockPos pickedPosStart;
    private final BlockPos pickedPosEnd;

    private Screen screen;
    private Screen parent;

    private BrushEditRailScreen(Rail pickedRail, BlockPos pickedPosStart, BlockPos pickedPosEnd, Screen parent) {
        if (pickedRail.railType == RailType.NONE) {
            pickedRail = null;
            Map<BlockPos, Rail> mp0 = ClientData.RAILS.get(pickedPosEnd);
            if (mp0 != null) pickedRail = mp0.get(pickedPosStart);
            BlockPos temp = pickedPosStart;
            pickedPosStart = pickedPosEnd;
            pickedPosEnd = temp;
        }
        this.pickedRail = pickedRail;
        this.supplier = (RailExtraSupplier) pickedRail;
        this.pickedPosStart = pickedPosStart;
        this.pickedPosEnd = pickedPosEnd;
        this.parent = parent;
        init();
    }

    private BrushEditRailScreen(Screen parent) {
        this(RailPicker.pickedRail, RailPicker.pickedPosStart, RailPicker.pickedPosEnd, parent);
    }
    
    private void init() {
        if (pickedRail == null) screen = parent;
        else {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.translatable("gui.mtrsteamloco.brush_edit_rail.title"))
                    .setDoesConfirmSave(false)
                    .transparentBackground();
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ConfigCategory common = builder.getOrCreateCategory(
                    Text.translatable("gui.mtrsteamloco.config.client.category.common")
            );

            CompoundTag brushTag = getBrushTag();
            String modelKey = supplier.getModelKey();

            Minecraft minecraft = Minecraft.getInstance();
            BlockEntityDirectNode entity = null;
            if (minecraft != null) {
                if (minecraft.level != null) {
                    BlockEntity e = minecraft.level.getBlockEntity(pickedPosStart);
                    if (e != null) {
                        if (e instanceof BlockEntityDirectNode en) {
                            entity = en;
                        }
                    }
                }
            }

            if (entity != null) {
                final BlockEntityDirectNode entity1 = entity;
                common.addEntry(
                    ButtonListEntry.createCenteredInstance(
                        Text.translatable("gui.mtrsteamloco.brush_edit_rail.adjust_angle"),
                        btn -> minecraft.setScreen(DirectNodeScreen.createScreen(entity1, () -> createScreen(pickedRail, pickedPosStart, pickedPosEnd, parent)))
                    )
                );
            } else if (pickedRail.transportMode == TransportMode.TRAIN) {
                BlockState state = minecraft.level.getBlockState(pickedPosStart);
                if (state != null && state.getBlock() instanceof BlockNode) {
                    common.addEntry(
                        ButtonListEntry.createCenteredInstance(
                            Text.translatable("gui.mtrsteamloco.brush_edit_rail.switch_to_direct_node"),
                            btn -> PacketReplaceRailNode.sendUpdateC2S(minecraft.level, pickedPosStart, state, "brush_edit_rail")
                        )
                    );
                }
            }


            common.addEntry(
                entryBuilder.startTextDescription(
                    Text.translatable("gui.mtrsteamloco.brush_edit_rail.brush_hint")
                ).build());

            boolean enableRailType = brushTag != null && brushTag.contains("RailType");
            common.addEntry(
                entryBuilder.startBooleanToggle(
                    Text.translatable("gui.mtrsteamloco.brush_edit_rail.enable_rail_type"),
                    enableRailType
                ).setTooltipSupplier(checked -> {
                    if (checked != enableRailType) {
                        updateBrushTag(
                            compoundTag -> {
                                if (checked) {
                                    compoundTag.putString("RailType", pickedRail.railType.toString());
                                } else {
                                    compoundTag.remove("RailType");
                                }
                            }
                        );
                        Minecraft.getInstance().setScreen(BrushEditRailScreen.createScreen(pickedRail, pickedPosStart, pickedPosEnd, parent));
                    }
                    return Optional.empty();
                }).build()
            );
            if (enableRailType) {
                common.addEntry(
                    entryBuilder.startTextField(
                        Text.translatable("gui.mtrsteamloco.brush_edit_rail.rail_type"),
                        brushTag.getString("RailType")
                    ).setErrorSupplier(str -> {
                        try {
                            RailType type = RailType.valueOf(str);
                            if (type != null && type != RailType.NONE) return Optional.empty();
                        } catch (Exception e) {}
                        return Optional.of(Text.translatable("gui.mtrsteamloco.brush_edit_rail.rail_type_error"));
                    }
                    ).setSaveConsumer(str -> {
                        updateBrushTag(compoundTag -> {
                            compoundTag.putString("RailType", str);
                        });
                    }).build()
                );
            }

            boolean enableModelKey = brushTag != null && brushTag.contains("ModelKey");
            common.addEntry(
                entryBuilder.startBooleanToggle(
                    Text.translatable("gui.mtrsteamloco.brush_edit_rail.enable_model_key"),
                    enableModelKey
                ).setTooltipSupplier(checked -> {
                    if (checked != enableModelKey) {
                        updateBrushTag(
                            compoundTag -> {
                                if (checked) {
                                    compoundTag.putString("ModelKey", modelKey);
                                } else {
                                    compoundTag.remove("ModelKey");
                                }
                            });
                        Minecraft.getInstance().setScreen(BrushEditRailScreen.createScreen(pickedRail, pickedPosStart, pickedPosEnd, parent));
                    }
                    return Optional.empty();
                })
                .setDefaultValue(enableModelKey).build()
            );

            if (enableModelKey) {
                RailModelProperties properties = RailModelRegistry.ELEMENTS.get(modelKey);
                common.addEntry(ButtonListEntry.createCenteredInstance(
                    Text.translatable("gui.mtrsteamloco.brush_edit_rail.present", (properties != null ? (properties.name.getString()) : (modelKey + " (???)"))),
                    btn -> Minecraft.getInstance().setScreen(new SelectScreen(() -> createScreen(pickedRail, pickedPosStart, pickedPosEnd, parent), RailModelRegistry.TREE, supplier::getModelKey, (mc, screen, btnKey) -> {
                        BrushEditRailScreen.updateBrushTag(compoundTag -> compoundTag.putString("ModelKey", btnKey));
                        ((RailExtraSupplier) pickedRail).setModelKey(btnKey);
                        PacketUpdateRail.sendUpdateC2S(pickedRail, pickedPosStart, pickedPosEnd);
                    } ,"https://aphrodite281.github.io/mtr-ante/#/railmodel")))
                );
            }

            boolean enableVertCurveRadius = brushTag != null && brushTag.contains("VerticalCurveRadius");
            float vertCurveRadius = ((RailExtraSupplier)pickedRail).getVerticalCurveRadius();
            common.addEntry(
                entryBuilder.startBooleanToggle(
                    Text.translatable("gui.mtrsteamloco.brush_edit_rail.enable_vertical_curve_radius"),
                    enableVertCurveRadius
                ).setTooltipSupplier(checked -> {
                    if (checked != enableVertCurveRadius) {
                        updateBrushTag(compoundTag -> {
                            if (checked) {
                                compoundTag.putFloat("VerticalCurveRadius", vertCurveRadius);
                            } else {
                                compoundTag.remove("VerticalCurveRadius");
                            }
                        });
                        Minecraft.getInstance().setScreen(BrushEditRailScreen.createScreen(pickedRail, pickedPosStart, pickedPosEnd, parent));
                    }
                    return Optional.empty();
                })
                .setDefaultValue(enableVertCurveRadius).build()
            );
            if (enableVertCurveRadius) {
                common.addEntry(new VertCurveRadiusListEntry(vertCurveRadius, this));
            }

            boolean enableRollAngle = brushTag != null && brushTag.contains("RollAngleMap");

            common.addEntry(
                entryBuilder.startBooleanToggle(
                    Text.translatable("gui.mtrsteamloco.brush_edit_rail.enable_roll_angle"),
                    enableRollAngle
                ).setTooltipSupplier(checked -> {
                    if (checked != enableRollAngle) {
                        if (!checked) {
                            updateBrushTag(compoundTag -> {
                                compoundTag.remove("RollAngleMap");
                            });
                        } else {
                            updateBrushTag(compoundTag -> {
                                compoundTag.putString("RollAngleMap", DoubleFloatMapSerializer.serializeToString(new HashMap<>()));
                            });
                        }
                        Minecraft.getInstance().setScreen(BrushEditRailScreen.createScreen(pickedRail, pickedPosStart, pickedPosEnd, parent));
                    }
                    return Optional.empty();
                }).build()
            );
            Vector3f p1 = new Vector3f(pickedRail.getPosition(0));
            Vector3f p2 = new Vector3f(pickedRail.getPosition(pickedRail.getLength()));
            Vector3f start = new Vector3f(pickedPosStart);
            float d1 = p1.distance(start);
            float d2 = p2.distance(start);
            boolean flag = d1 < d2;
            if (enableRollAngle) common.addEntry(new RollAnglesListEntry(pickedRail, flag, this::updateRailAngle, f -> PacketUpdateRail.sendUpdateC2S(pickedRail, pickedPosStart, pickedPosEnd), () -> BrushEditRailScreen.createScreen(pickedRail, pickedPosStart, pickedPosEnd, parent)));

            Function<Integer, Optional<Component[]>> f = v -> Optional.of(new Component[]{Text.translatable("gui.mtrsteamloco.brush_edit_rail.opening_direction_tooltip")});

            common.addEntry(
                entryBuilder.startIntSlider(
                    Text.translatable("开门方向"),
                    supplier.getOpeningDirection(),
                    0, 3
                )
                .setTooltipSupplier(f)
                .setDefaultValue(0)
                .setSaveConsumer(value -> {
                    supplier.setOpeningDirection(value);
                    PacketUpdateRail.sendUpdateC2S(pickedRail, pickedPosStart, pickedPosEnd);
                }).build()
            );

            Map<String, String> customConfigs = supplier.getCustomConfigs();
            Map<String, ConfigResponder> responders = supplier.getCustomResponders();
            List<AbstractConfigListEntry> entries = ConfigResponder.getEntrysFromMaps(customConfigs, responders, entryBuilder, () -> BrushEditRailScreen.createScreen(pickedRail, pickedPosStart, pickedPosEnd, parent));
            for (AbstractConfigListEntry entry : entries) {
                common.addEntry(entry);
            }
            screen = builder.build();
        }
    }

    public void updateRailAngle(Map<Double, Float> res) {
        PacketUpdateRail.sendUpdateC2S(pickedRail, pickedPosStart, pickedPosEnd);
        updateBrushTag(compoundTag -> {
            Map<Double, Float> res1 = new HashMap<>();
            Set<Map.Entry<Double, Float>> entries = new HashSet<>(res.entrySet());
            double length = pickedRail.getLength();
            for (Map.Entry<Double, Float> entry : entries) {
                res1.put(entry.getKey() / length, entry.getValue());
            }
            compoundTag.putString("RollAngleMap", DoubleFloatMapSerializer.serializeToString(res1));
        });
    }

    public void updateRadius(float newRadius, boolean send) {
        if (send) {
            updateBrushTag(compoundTag -> {
                compoundTag.putFloat("VerticalCurveRadius", newRadius);
            });
        }
    }

    public Component[] getVerticalValueText(float verticalRadius) {
        Rail rail = pickedRail;
        if (rail == null) return new Component[] {Text.literal("(???)")};
        int H = Math.abs(((RailExtraSupplier)rail).getHeight());
        double L = rail.getLength();
        double maxRadius = (H == 0) ? 0 : (H * H + L * L) / (H * 4);
        double gradient;
        if (verticalRadius < 0) {
            gradient = H / L * 1000;
        } else if (verticalRadius == 0 || verticalRadius > maxRadius) {
            gradient = Math.tan(RailExtraSupplier.getVTheta(rail, maxRadius)) * 1000;
        } else {
            gradient = Math.tan(RailExtraSupplier.getVTheta(rail, verticalRadius)) * 1000;
        }
        return new Component[] {
            Text.translatable("gui.mtrsteamloco.brush_edit_rail.vertical_curve_radius_values.1"),
            Text.translatable("gui.mtrsteamloco.brush_edit_rail.vertical_curve_radius_values.2", String.format("%.1f", maxRadius)),
            Text.translatable("gui.mtrsteamloco.brush_edit_rail.vertical_curve_radius_values.3", String.format("%.1f", gradient))};
    }

    public Screen getScreen() {
        return screen;
    }

    public static Screen createScreen(Screen parent) {
        return new BrushEditRailScreen(parent).getScreen();
    }

    public static Screen createScreen(Rail rail, BlockPos posStart, BlockPos posEnd, Screen parent) {
        return new BrushEditRailScreen(rail, posStart, posEnd, parent).getScreen();
    }

    public static void applyBrushToPickedRail(BlockPos posStart, BlockPos posEnd, Rail pickedRail, CompoundTag railBrushProp, boolean isBatchApply) {
        if (railBrushProp == null ||  posStart == null || posEnd == null || pickedRail == null) return;
        Rail anti = null;
        if (pickedRail.railType == RailType.NONE) {
            Map<BlockPos, Rail> r1 = ClientData.RAILS.get(posEnd);
            if (r1 == null) return;
            anti = pickedRail;
            pickedRail = r1.get(posStart);
            if (pickedRail == null) return;
            if (pickedRail.railType == RailType.NONE) return;
            BlockPos temp = posStart;
            posStart = posEnd;
            posEnd = temp;
            Main.LOGGER.info("Switched start and end positions");
        }
        RailExtraSupplier pickedExtra = (RailExtraSupplier) pickedRail;
        boolean propertyUpdated = false;
        if (railBrushProp.contains("ModelKey") &&
                !railBrushProp.getString("ModelKey").equals(pickedExtra.getModelKey())) {
            String modelKey = railBrushProp.getString("ModelKey");
            pickedExtra.setModelKey(modelKey);
            if (anti != null) ((RailExtraSupplier) anti).setModelKey(modelKey);
            propertyUpdated = true;
        }
        if (railBrushProp.contains("VerticalCurveRadius") &&
                railBrushProp.getFloat("VerticalCurveRadius") != pickedExtra.getVerticalCurveRadius()) {
            pickedExtra.setVerticalCurveRadius(railBrushProp.getFloat("VerticalCurveRadius"));
            propertyUpdated = true;
        }
        if (railBrushProp.contains("RollAngleMap")) {
            Map<Double, Float> raw = DoubleFloatMapSerializer.deserialize(railBrushProp.getString("RollAngleMap"));
            Map<Double, Float> rollAngleMap = new HashMap<>();
            Set<Map.Entry<Double, Float>> entries = new HashSet<>(raw.entrySet());
            double length = pickedRail.getLength();
            for(Map.Entry<Double, Float> entry : entries) {
                rollAngleMap.put(entry.getKey() * length, entry.getValue());
            }
            pickedExtra.setRollAngleMap(rollAngleMap);
            propertyUpdated = true;
        }
        if (railBrushProp.contains("RailType")) {
            pickedExtra.setRailType(RailType.valueOf(railBrushProp.getString("RailType")));
            Main.LOGGER.info("RailType updated to " + railBrushProp.getString("RailType"));
            propertyUpdated = true;
        }
        if (isBatchApply && !propertyUpdated) {
            // Right-click again to reverse the direction
            pickedExtra.setRenderReversed(!pickedExtra.getRenderReversed());
        }
        PacketUpdateRail.sendUpdateC2S(pickedRail, posStart, posEnd);
    }

    public static void applyBrushToPickedRail(CompoundTag railBrushProp, boolean isBatchApply) {
        Rail pickedRail = RailPicker.pickedRail;
        BlockPos pickedPosStart = RailPicker.pickedPosStart;
        BlockPos pickedPosEnd = RailPicker.pickedPosEnd;
        if (railBrushProp == null) return;
        if (pickedRail == null) return;
        applyBrushToPickedRail(pickedPosStart, pickedPosEnd, pickedRail, railBrushProp, isBatchApply);
    }

    public static CompoundTag getBrushTag() {
        if (Minecraft.getInstance().player == null) return null;
        ItemStack mainHandItem = Minecraft.getInstance().player.getMainHandItem();
        ItemStack offHandItem = Minecraft.getInstance().player.getOffhandItem();
        CompoundTag nteTag = null;
        if (mainHandItem.is(mtr.Items.BRUSH.get())) {
            nteTag = mainHandItem.getTagElement("NTERailBrush");
        } else if (offHandItem.is(mtr.Items.BRUSH.get())) {
            nteTag = offHandItem.getTagElement("NTERailBrush");
        }
        return nteTag;
    }

    public static void updateBrushTag(Consumer<CompoundTag> modifier) {
        if (Minecraft.getInstance().player == null) return;
        ItemStack mainHandItem = Minecraft.getInstance().player.getMainHandItem();
        ItemStack offHandItem = Minecraft.getInstance().player.getOffhandItem();
        CompoundTag nteTag = null;
        InteractionHand hand = null;
        if (mainHandItem.is(mtr.Items.BRUSH.get())) {
            nteTag = mainHandItem.getOrCreateTagElement("NTERailBrush");
            hand = InteractionHand.MAIN_HAND;
        } else if (offHandItem.is(mtr.Items.BRUSH.get())) {
            nteTag = offHandItem.getOrCreateTagElement("NTERailBrush");
            hand = InteractionHand.OFF_HAND;
        }
        if (nteTag == null) return;
        modifier.accept(nteTag);
        applyBrushToPickedRail(nteTag, false);
        PacketUpdateHoldingItem.sendUpdateC2S(hand);
    }
    
    @Environment(EnvType.CLIENT)
    private class VertCurveRadiusListEntry extends TooltipListEntry<String> implements ContainerEventHandler {
        
        private final Button btnSetDefaultRadius = UtilitiesClient.newButton(
        Text.translatable("gui.mtrsteamloco.brush_edit_rail.vertical_curve_radius_set_max"),
        sender -> updateRadius(0, true));
        private final Button btnSetNoRadius = UtilitiesClient.newButton(
            Text.translatable("gui.mtrsteamloco.brush_edit_rail.vertical_curve_radius_set_none"),
            sender -> updateRadius(-1, true)
        );
        private final WidgetBetterTextField radiusInput = new WidgetBetterTextField("", 8); 
        private final BrushEditRailScreen screen;
        private final List<AbstractWidget> widgets;
        private float vertCurveRadius = 0;

        public VertCurveRadiusListEntry(float v, BrushEditRailScreen screen) {
            super(Text.literal(""), null, false);
            vertCurveRadius = v;
            setTooltipSupplier(() -> Optional.of(screen.getVerticalValueText(vertCurveRadius)));
            this.screen = screen;

            updateRadius(vertCurveRadius, false);
            radiusInput.setResponder(
                text -> {
                    if (!text.isEmpty()) {
                        try {
                            float newRadius = Float.parseFloat(text);
                            Rail rail = pickedRail;
                            if (rail != null) {
                                int H = Math.abs(((RailExtraSupplier) rail).getHeight());
                                double L = rail.getLength();
                                double maxRadius = (H == 0) ? 0 : (H * H + L * L) / (H * 4);
                                if (newRadius < maxRadius) {
                                    radiusInput.setTextColor(0xE0E0E0);
                                } else {
                                    radiusInput.setTextColor(0xEEEE00);
                                }
                            } else {
                                radiusInput.setTextColor(0xEEEE00);
                            }
                            updateRadius(newRadius, true);
                        } catch (Exception ignored) {
                            radiusInput.setTextColor(0xFF0000);
                        }
                    }
                });
            widgets = Lists.newArrayList(btnSetDefaultRadius, btnSetNoRadius, radiusInput);
        }

        public void updateRadius(float newRadius, boolean send) {
            btnSetDefaultRadius.active = newRadius != 0;
            btnSetNoRadius.active = newRadius >= 0;
            String expectedText;
            if (newRadius <= 0) {
                expectedText = "";
            } else {
                expectedText = Integer.toString((int) newRadius);
            }
            if (!expectedText.equals(radiusInput.getValue())) {
                radiusInput.setValue(expectedText);
                radiusInput.moveCursorToStart();
            }
            vertCurveRadius = newRadius;
            screen.updateRadius(newRadius, send);
        }

        @Override
        public boolean isEdited() {
            return false;
        }

        @Override
        public String getValue() {
            return "";
        }
        
        @Override
        public Optional<String> getDefaultValue() {
            return Optional.empty();
        }

        @Override
#if MC_VERSION >= "12000"
        public void render(GuiGraphics matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
#else
        public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
#endif
            super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
            IDrawing.setPositionAndWidth(radiusInput, 80, y, 200);
            IDrawing.setPositionAndWidth(btnSetDefaultRadius, 290, y, 60);
            IDrawing.setPositionAndWidth(btnSetNoRadius, 355, y, 60);
            radiusInput.render(matrices, mouseX, mouseY, delta);
            btnSetDefaultRadius.render(matrices, mouseX, mouseY, delta);
            btnSetNoRadius.render(matrices, mouseX, mouseY, delta);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }
        
        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }
        
        @Override
        public void save() {
            
        }
    }
}
