package cn.zbx1425.mtrsteamloco.gui.entries;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.data.*;
import cn.zbx1425.mtrsteamloco.gui.entries.ButtonListEntry;
import cn.zbx1425.mtrsteamloco.network.PacketUpdateHoldingItem;
import cn.zbx1425.mtrsteamloco.network.PacketUpdateRail;
import cn.zbx1425.mtrsteamloco.render.RailPicker;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import com.mojang.blaze3d.vertex.PoseStack;
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
import mtr.screen.WidgetBetterTextField;
import net.minecraft.util.Mth;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.Util;
import mtr.data.RailType;
import cn.zbx1425.mtrsteamloco.gui.WidgetSlider;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
#if MC_VERSION >= "12000"
import net.minecraft.client.gui.GuiGraphics;
#else
import net.minecraft.client.gui.GuiComponent;
#endif
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import cn.zbx1425.mtrsteamloco.gui.FakeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import me.shedaniel.clothconfig2.gui.entries.*;
import mtr.mappings.Text;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.mtrsteamloco.ClientConfig;
import net.minecraft.client.gui.components.EditBox;
import me.shedaniel.clothconfig2.api.Tooltip;
import me.shedaniel.math.Point;
import cn.zbx1425.mtrsteamloco.network.util.DoubleFloatMapSerializer;

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

@Environment(EnvType.CLIENT)
public class RollAnglesListEntry extends TooltipListEntry<String> implements ContainerEventHandler {
    private final List<Node> nodes = new ArrayList<>();
    private Node now = null;
    
    Button btnAddNode = UtilitiesClient.newButton(
        Text.translatable("+"),
        sender -> addNode(new Node(0, 0)));
    Button btnRemoveNode = UtilitiesClient.newButton(
        Text.translatable("-"),
        sender -> removeNode(now));
    Button btnClear = UtilitiesClient.newButton(
        Text.translatable("gui.mtrsteamloco.brush_edit_rail.clear"),
        sender -> removeAllNodes()
    );
    Button btnCopy = UtilitiesClient.newButton(
        Text.translatable("gui.mtrsteamloco.brush_edit_rail.copy"),
        btn -> {
            if (now == null) return;
            addNode(new Node(now));
        }
    );
    Button btnAdjust;
    // Button btnChangeMode = UtilitiesClient.newButton(
    //     Text.translatable("⇄"),
    //     sender -> {
    //         ClientConfig.useEditBoxSetRailRolling = !ClientConfig.useEditBoxSetRailRolling;
    //         ClientConfig.save();
    //         focusNode(now);
    //     }
    // );
    private final List<AbstractWidget> widgets;
    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/block/white_concrete_powder.png");
    private static final ResourceLocation POS = new ResourceLocation(Main.MOD_ID, "textures/gui/rail/ante.png");
    private final boolean flag;
    private int sliderWidth, sliderStartX;
    private Consumer<Map<Double, Float>> updateFunction;
    private Rail pickedRail;
    private RailExtraSupplier supplier;
    private SliderTextFieldGroup offsetInput, positionInput, angleInput;
    private Supplier<Screen> parent;

    public RollAnglesListEntry(Rail pickedRail, boolean reversed, Consumer<Map<Double, Float>> updateFunction, Consumer<Float> updateRollingOffset,  Supplier<Screen> parent) {
        super(Text.literal(""), null, false);
        this.supplier = (RailExtraSupplier) pickedRail;
        this.pickedRail = pickedRail;
        this.updateFunction = updateFunction;
        flag = reversed;

        ClientConfig.EntryGroup e0 = ClientConfig.rollAnglesListEntryGroup;
        ClientConfig.Entry e = e0.entries[0], e1 = e0.entries[1];

        offsetInput = new SliderTextFieldGroup(
            e1.min, e1.max, e1.step, e1.modes[0],
            i -> {
                e1.modes[0] = i;
                ClientConfig.save();
            }, supplier.getRollingOffset(), 1.435F / 2F, d -> {
                supplier.setRollingOffset(d);
                updateRollingOffset.accept(d);
            }, d -> Text.literal(String.format("%.4fM", d)), false,
            Text.translatable("gui.mtrsteamloco.brush_edit_rail.offset_input")
        );

        positionInput = new SliderTextFieldGroup(
            0F, (float) pickedRail.getLength(), 10000, e.modes[1], 
            i -> {
                e.modes[1] = i;
                ClientConfig.save();
            }, 0F, 0F, d -> {
                if (now == null) return;
                now.directSetValue(d / pickedRail.getLength());
            }, d -> Text.literal(String.format("%.4fM", d)), true,
            Text.translatable("gui.mtrsteamloco.brush_edit_rail.position_input")
        );

        
        angleInput = new SliderTextFieldGroup(
            e.min, e.max, e.step, e.modes[0],
            i -> {
                e.modes[0] = i;
                ClientConfig.save();
            }, 0F, 0F, d -> {
                if (now == null) return;
                now.setAngle((float) Math.toRadians(d));
            }, d -> Text.literal(String.format("%.2f°", d)), false,
            Text.translatable("gui.mtrsteamloco.brush_edit_rail.angle_input")
        );

        Map<Double, Float> rollAngles = supplier.getRollAngleMap();
        Set<Map.Entry<Double, Float>> entries = new HashSet<>(rollAngles.entrySet());
        for (Map.Entry<Double, Float> entry : entries) {
            addNode(new Node(entry.getKey(), entry.getValue()), false);
        }
        if (nodes.isEmpty()) {
            focusNode(null);
        }

        positionInput.setEditable(!entries.isEmpty());
        angleInput.setEditable(!entries.isEmpty());

        this.parent = parent;

        btnAdjust = UtilitiesClient.newButton(
            Text.translatable("gui.mtrsteamloco.adjust_settings"),
            sender -> {
                Minecraft.getInstance().setScreen(createAdjustScreen(parent));
            }
        );

        widgets = Lists.newArrayList(btnAddNode, btnRemoveNode, btnClear, btnCopy, btnAdjust);
    }

    private static Screen createAdjustScreen(Supplier<Screen> parent) {
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
        ClientConfig.rollAnglesListEntryGroup.getListEntries(entries, entryBuilder, () -> createAdjustScreen(parent));

        for (AbstractConfigListEntry entry : entries) {
            common.addEntry(entry);
        }

        return builder.build();
    }

    public void update() {
        Map<Double, Float> res = new HashMap<>();
        List<Node> copy = new ArrayList<>(nodes);
        for (Node node : copy) {
            res.put(node.getValue(), node.getAngle());
        }
        supplier.setRollAngleMap(res);
        updateFunction.accept(res);
    }

    public void focusNode(Node node) {
        now = node;
        if (now != null) {
            angleInput.setValue((float) Math.toDegrees(now.getAngle()));
            positionInput.setValue((float) (now.getValue()));
            for (int i = 0; i < nodes.size(); i++) {
                if (nodes.get(i) == node) {
                    nodes.remove(i);
                    nodes.add(0, node);
                    break;
                }
            }
            angleInput.setEditable(true);
            positionInput.setEditable(true);
            btnClear.active = true;
            btnCopy.active = true;
            btnRemoveNode.active = true;
        } else {
            angleInput.setValue(0F);
            positionInput.setValue(0F);
            angleInput.setEditable(false);
            positionInput.setEditable(false);
            btnClear.active = false;
            btnCopy.active = false;
            btnRemoveNode.active = false;
        }
    }
    
    public void addNode(Node node) {
        addNode(node, true);
    }

    public void addNode(Node node, boolean update) {
        nodes.add(node);
        focusNode(node);
        if (update) update();
    }
    public void removeNode(Node node) {
        if (node == null) return;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i) == node) {
                nodes.remove(i);
                if (now == node) {
                    focusNode(nodes.isEmpty() ? null : nodes.get(0));
                }
                break;
            }
        }
        update();
    }
    public void removeAllNodes() {
        nodes.clear();
        focusNode(null);
        update();
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
    public void save() {
        
    }
    @Override
    public int getItemHeight() {
        return 24 * (now == null ? 4 : 6);
    }
    @Override
    public List<? extends GuiEventListener> children() {
        List<GuiEventListener> res = new ArrayList<>(widgets);
        res.addAll(nodes);
        res.addAll(offsetInput.children());
        if (now == null) return res;
        res.addAll(positionInput.children());
        res.addAll(angleInput.children());
        return res;
    }
    
    @Override
    public List<? extends NarratableEntry> narratables() {
        List<NarratableEntry> res = new ArrayList<>(widgets);
        res.addAll(offsetInput.narratables());
        if (now == null) return res;
        res.addAll(positionInput.narratables());
        res.addAll(angleInput.narratables());
        return widgets;
    }

    @Override
#if MC_VERSION >= "12000"
    public void render(GuiGraphics matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
#else
    public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
#endif
        super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        FormattedCharSequence displayedFieldName = Text.translatable("gui.mtrsteamloco.brush_edit_rail.roll_setting").getVisualOrderText();
        Window window = Minecraft.getInstance().getWindow();
        if (Minecraft.getInstance().font.isBidirectional()) {
            drawString(matrices, Minecraft.getInstance().font, displayedFieldName, window.getGuiScaledWidth() - x - Minecraft.getInstance().font.width(displayedFieldName), y + 6, getPreferredTextColor());
        } else {
            drawString(matrices, Minecraft.getInstance().font, displayedFieldName, x, y + 6, getPreferredTextColor());
        }
        y += 24;

        offsetInput.render(matrices, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
    
        y += 24;

        y += 2;
        boolean reversed = ((RailExtraSupplier) pickedRail).getRenderReversed();
        if (!reversed == flag) {
            blitPos(matrices, x, y, mouseX, mouseY);
        } else {
            blitPos(matrices, x + entryWidth - 20, y, mouseX, mouseY);
        }
        sliderWidth = entryWidth - 40;
        sliderStartX = x + 20;
        blit(matrices, WHITE, sliderStartX, y + 6, sliderWidth, 8);
        for (int i = nodes.size() - 1; i >= 0; i--) {
            Node node = nodes.get(i);
            node.render(matrices, x, y + 10, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        }
        y += 20;
        y += 2 * 2;

        int w = 40, in = 10;
        int aw = 5 * w + 5 * in;
        w += in;
        int s = x + (int) Math.round((entryWidth - aw) / 2f);

        IDrawing.setPositionAndWidth(btnAddNode, s, y, w);
        s += w;
        IDrawing.setPositionAndWidth(btnRemoveNode, s, y, w);
        s += w;
        IDrawing.setPositionAndWidth(btnClear, s, y, w);
        s += w;
        IDrawing.setPositionAndWidth(btnCopy, s, y, w);
        s += w + in;
        IDrawing.setPositionAndWidth(btnAdjust, s, y, w);
        btnAddNode.render(matrices, mouseX, mouseY, delta);
        btnRemoveNode.render(matrices, mouseX, mouseY, delta);
        btnClear.render(matrices, mouseX, mouseY, delta);
        btnCopy.render(matrices, mouseX, mouseY, delta);
        btnAdjust.render(matrices, mouseX, mouseY, delta);
        y += 22;
        
        if (now == null) return;
        positionInput.render(matrices, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);

        y += 24;
        angleInput.render(matrices, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
    }
    protected FormattedCharSequence[] wrapLinesToScreen(Component[] lines) {
        return wrapLines(lines, Minecraft.getInstance().getWindow().getGuiScaledWidth());
    }

    protected FormattedCharSequence[] wrapLines(Component[] lines, int width) {
        final Font font = Minecraft.getInstance().font;
        
        return Arrays.stream(lines)
                .map(line -> font.split(line, width))
                .flatMap(List::stream)
                .toArray(FormattedCharSequence[]::new);
    }

#if MC_VERSION >= "12000"
    public static void drawString(GuiGraphics matrices, Font font, FormattedCharSequence text, int x, int y, int color) {
        matrices.drawString(font, text, x, y, color);
    }
#else
    public static void drawString(PoseStack matrices, Font font, FormattedCharSequence text, int x, int y, int color) {
        GuiComponent.drawString(matrices, font, text, x, y, color);
    }
#endif
#if MC_VERSION >= "12000"
    private static int drawText(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color) {
        FormattedText formattedText = FormattedText.of(text);
        List<FormattedCharSequence> lines = font.split(formattedText, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 40);
        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(font, line, x, y, color);
            y += Mth.ceil(font.lineHeight * 1.1f);
        }
        return y;
    }
    private static void blit(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height) {
        guiGraphics.blit(texture, x, y, width, height, 0, 0, 1, 1, 1, 1);
    }
    // private static void blit(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int argb) {
    // }
#else
    private static int drawText(PoseStack matrices, Font font, String text, int x, int y, int color) {
        FormattedText formattedText = FormattedText.of(text);
        List<FormattedCharSequence> lines = font.split(formattedText, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 40);
        for (FormattedCharSequence line : lines) {
            font.drawShadow(matrices, line, x, y, color);
            y += Mth.ceil(font.lineHeight * 1.1f);
        }
        return y;
    }
    private static void blit(PoseStack matrices, ResourceLocation texture, int x, int y, int width, int height) {
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(matrices, x, y, width, height, 0, 0, 1, 1, 1, 1);
    }
    // private static void blit(PoseStack matrices, int x1, int y1, int x2, int y2, int argb) {
    //     GuiComponent.fill(matrices, x1, y1, x2, y2, argb);
    // }
#endif

#if MC_VERSION >= "12000"
    private void blitPos(GuiGraphics matrices, int x, int y, int mx, int my) {
#else
    private void blitPos(PoseStack matrices, int x, int y, int mx, int my) {
#endif
        blit(matrices, POS, x, y, 20, 20);

        if (x <= mx && mx <= x + 20 && y <= my && my <= y + 20) {
            addTooltip(Tooltip.of(new Point(mx, my), wrapLinesToScreen(new Component[] {Text.translatable("gui.mtrsteamloco.brush_edit_rail.roll_setting.pos")})));
        }
    }

    private class Slider extends AbstractSliderButton {
        Function<Double, Component> textGetter;
        Consumer<Double> valueSetter;
        protected Slider(int x, int y, int w, int h, double min, double value, double max, Function<Double, Component> textGetter,Consumer<Double> valueSetter) {
            super(x, y, w, h, Text.translatable(""), value);
            this.textGetter = textGetter;
            this.valueSetter = valueSetter;
        }
        
        @Override
        public void updateMessage() {
            setMessage(textGetter.apply(value));
        }
        
        @Override
        protected void applyValue() {
            valueSetter.accept(value);
        }
        
        @Override
        public boolean keyPressed(int int_1, int int_2, int int_3) {
            if (!isEditable())
                return false;
            return super.keyPressed(int_1, int_2, int_3);
        }
        
        @Override
        public boolean mouseDragged(double double_1, double double_2, int int_1, double double_3, double double_4) {
            if (!isEditable())
                return false;
            return super.mouseDragged(double_1, double_2, int_1, double_3, double_4);
        }
        public void setValue(double value, boolean air) {
            double d0 = this.value;
            this.value = Mth.clamp(value, 0.0D, 1.0D);
            if (d0 != this.value) {
                this.applyValue();
            }
            this.updateMessage();
        }
    }
    private class Node implements GuiEventListener{
        private double value;
        private float angle;
        private int y;
        private boolean dragged = false;
        // private static final ResourceLocation P = new ResourceLocation(Main.MOD_ID, "textures/gui/rail/p.png");
        private static final ResourceLocation P = new ResourceLocation(Main.MOD_ID, "textures/gui/rail/pi.png");
        private static final ResourceLocation Y = new ResourceLocation(Main.MOD_ID, "textures/gui/rail/y.png");
        private static final int NW = 6;
        private static final int NH = 10;
        
        public Node(double value, float angle) {
            this.value = value / pickedRail.getLength();
            this.angle = angle;
        }

        public Node(Node node) {
            this.value = node.value;
            this.angle = node.angle;
        }

#if MC_VERSION >= "12000"
        public void render(GuiGraphics matrices, int x, int y, int width, int height, int mouseX, int mouseY, boolean isHovered, float delta) {
#else 
        public void render(PoseStack matrices, int x, int y, int width, int height, int mouseX, int mouseY, boolean isHovered, float delta) {
#endif  
            this.y = y;
            if (isActive()) {
                blit(matrices, Y, getX() - NW / 2, this.y - NH / 2,  NW, NH);
            } else {
                blit(matrices, P, getX() - NW / 2, this.y - NH / 2, NW,  NH);
            }
        }

        public int getX() {
            return (int) (sliderStartX + value * sliderWidth);
        }

        public void directSetValue(double value) {
            this.value = value;
            update();
        }

        public void setValue(int x) {
            double old = value;
            if (x <= sliderStartX) x = sliderStartX;
            else if (x >= sliderStartX + sliderWidth) x = sliderStartX + sliderWidth;
            else value = (x - sliderStartX) / (double) sliderWidth;
            if (old == value) return;
            update();
        }

        public void setValue(double value) {
            double old = this.value;
            if (value < 0) value = 0;
            else if (value > 1) value = 1;
            if (old == value) return;
            this.value = value;
            update();
        }
        public void setAngle(float angle) {
            if (this.angle == angle) return;
            this.angle = angle;
            update();
        }
        public double getValue() {
            return value * pickedRail.getLength();
        }
        public float getAngle() {
            return angle;
        }
        public boolean isActive() {
            return now == this;
        }

#if MC_VERSION >= "11904"
        boolean isFocused = false;
        
        @Override
        public boolean isFocused() {
            return isFocused;
        }
        @Override
        public void setFocused(boolean b) {
            isFocused = b;
        }
#endif

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            int midX = getX();
            float minX = midX - NW / 2;
            float maxX = midX + NW / 2;
            float minY = y - NH / 2;
            float maxY = y + NH / 2;
            return mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY;
        }
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int in) {
            if (isMouseOver(mouseX, mouseY)) {
                focusNode(this);
                return true;
            }
            return false;
        }
        @Override
        public boolean mouseDragged(double sx, double sy, int t, double dx, double dy) {
            if ((isActive() && dragged) || isMouseOver(sx, sy)) {
                focusNode(this);
                setValue((int) (sx + dx));
                dragged = true;
                return true;
            }
            return false;
        }
        @Override
        public boolean mouseReleased(double x, double y, int t) {
            if (isActive() && dragged) {
                dragged = false;
                return true;
            }
            return false;
        }
        @Override
        public boolean keyPressed(int p_93596_, int p_93597_, int p_93598_) {
            boolean flag = p_93596_ == 263;
            if (flag || p_93596_ == 262) {
                double f = flag ? -0.025 : 0.025;
                setValue(value + f);
            }
            return false;
        }
    }

    private class SliderTextFieldGroup {
        private final EditBox textField;
        private final WidgetSlider slider;
        private final Button btnChangeMode, btnReset;
        private final float min, max, defaultValue;
        private float now;
        private final int step;
        private int mode;
        private final Consumer<Integer> saveModeConsumer;
        private final Consumer<Float> valueSetter;

        public SliderTextFieldGroup(float min, float max, int step, int mode, Consumer<Integer> saveModeConsumer, float now, float defaultValue, Consumer<Float> valueSetter, Function<Float, Component> textGetter, boolean clamp, Component name) {
            this.min = min;
            this.max = max;
            this.now = now;
            this.step = step;
            this.valueSetter = valueSetter;
            this.saveModeConsumer = saveModeConsumer;
            this.mode = mode;
            this.defaultValue = defaultValue;

            textField = new EditBox(Minecraft.getInstance().font, 0, 0, 0, 20, Text.literal(""));
            btnReset = UtilitiesClient.newButton(Text.translatable("gui.mtrsteamloco.brush_edit_rail.reset"), btn -> setValue(defaultValue));

            textField.setResponder(s -> {
                Float f = null;
                try {
                    f = Float.parseFloat(s);
                } catch (NumberFormatException e) {
                    textField.setTextColor(0xFF0000);
                    return;
                }
                if (clamp) {
                    boolean flag = f < min || f > max;
                    if (flag) {
                        textField.setTextColor(0xFF0000);
                        return;
                    }
                }
                textField.setTextColor(0xFFFFFF);
                setValue(f, false);
            });
            textField.setValue(Float.toString(now));
            textField.moveCursorToStart();

            slider = new WidgetSlider(20, step, getLevel(now), i -> {
                float f = getValue(i);
                Component c = textGetter.apply(f);
                setValue(f, false);
                return c;
            });
            btnChangeMode = UtilitiesClient.newButton(name, btn -> switchMode(this.mode + 1));

            btnReset.active = now == defaultValue;
            switchMode(mode);
        }

        private float now() {
            return now;
        }

        public void switchMode(int mode) {
            mode = mode % 2;
            if (mode == this.mode) return;
                
            this.mode = mode;
            saveModeConsumer.accept(mode);

            if (mode == 0) {
                textField.visible = true;
                slider.visible = false;
            } else {
                textField.visible = false;
                slider.visible = true;
            }
            setValue(now);
        }

        public void setValue(float value) {
            setValue(value, true);
        }

        public void setValue(float value, boolean flag) {
            if (textField == null || slider == null || btnReset == null || btnChangeMode == null) return;
            if (flag) {
                slider.setValue(getLevel(value));
                textField.setValue(Float.toString(value));
                textField.moveCursorToStart();
            }
            if (now == value) return;
            now = value;
            valueSetter.accept(value);
        }

        private int getLevel(float value) {
            return (int) Math.round((value - min) / (max - min) * step);
        }

        private float getValue(int level) {
            return min + level / (float) step * (max - min);
        }

    #if MC_VERSION >= "12000"
        public void render(GuiGraphics matrices, int x, int y, int width, int height, int mouseX, int mouseY, boolean isHovered, float delta) {
    #else 
        public void render(PoseStack matrices, int x, int y, int width, int height, int mouseX, int mouseY, boolean isHovered, float delta) {
    #endif 
            y += 2;
            int v1 = (int) Math.round(width * 0.5D);
            int v4 = Math.min(v1, 380);

            int v2 = (int) Math.round(width * 0.05D);

            int v3 = (int) Math.round(width * 0.3D);
            int v5 = Math.min(v3, 228);

            int v6 = (int) Math.round(width * 0.1D);
            int v7 = Math.min(v6, 100);

            btnReset.active = Math.abs(now - defaultValue) > 0.00001F;

            x += (width - v4 - v2 - v5 - v2 - v7) / 2;
            IDrawing.setPositionAndWidth(textField, x, y, v4);
            IDrawing.setPositionAndWidth(slider, x, y, v4);
            x += v4 + v2;
            IDrawing.setPositionAndWidth(btnChangeMode, x, y, v5);
            x += v5 + v2;
            IDrawing.setPositionAndWidth(btnReset, x, y, v7);

            if (mode == 0) textField.render(matrices, mouseX, mouseY, delta);
            else slider.render(matrices, mouseX, mouseY, delta);
            btnChangeMode.render(matrices, mouseX, mouseY, delta);
            btnReset.render(matrices, mouseX, mouseY, delta);
        }

        public List<GuiEventListener> children() {
            List<GuiEventListener> listeners = new ArrayList<>();
            if (mode == 0) listeners.add(textField);
            else listeners.add(slider);
            listeners.add(btnChangeMode);
            listeners.add(btnReset);
            return listeners;
        }

        public List<NarratableEntry> narratables() {
            List<NarratableEntry> narratables = new ArrayList<>();
            if (mode == 0) narratables.add(textField);
            else narratables.add(slider);
            narratables.add(btnChangeMode);
            narratables.add(btnReset);
            return narratables;
        }

        public void setEditable(boolean editable) {
            textField.moveCursorToStart();
            textField.setEditable(editable);
            slider.setEditable(editable);
            btnReset.active = editable;
            btnChangeMode.active = editable;
        }
    }
}