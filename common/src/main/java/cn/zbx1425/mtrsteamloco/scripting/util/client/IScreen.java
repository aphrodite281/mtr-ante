package cn.zbx1425.mtrsteamloco.scripting.util.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import cn.zbx1425.mtrsteamloco.Main;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
#if MC_VERSION >= "12000"
import net.minecraft.client.gui.GuiGraphics;
#else
import com.mojang.blaze3d.vertex.PoseStack;
#endif
#if MC_VERSION >= "11903"
import net.minecraft.client.gui.components.Renderable;
#else
import net.minecraft.client.gui.components.Widget;
#endif
import me.shedaniel.clothconfig2.api.*;
import cn.zbx1425.mtrsteamloco.gui.entries.*;
import net.minecraft.client.gui.components.Button;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.*;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IScreen {

    public static interface ClothConfig2 {
        public static ConfigBuilder createConfigBuilder() {
            return ConfigBuilder.create();
        }

        public static ButtonListEntry newButtonListEntry(Component name, Button button, ButtonListEntry.Processor processor, Supplier<Optional<Component[]>> tooltipSupplier, boolean requiresRestart) {
            return new ButtonListEntry(name, button, processor, tooltipSupplier, requiresRestart);
        }

        public static ButtonCycleListEntry newButtonCycleListEntry(Component fieldName, int index, List<String> list, Component resetButtonKey, Supplier<Integer> defaultValue, Consumer<Integer> saveConsumer, Supplier<Optional<Component[]>> tooltipSupplier, boolean requiresRestart) {
            return new ButtonCycleListEntry(fieldName, index, list, resetButtonKey, defaultValue, saveConsumer, tooltipSupplier, requiresRestart);
        }
    }

#if MC_VERSION >= "11903"
    public static Button newButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return new Button.Builder(text, onPress).pos(x, y).size(width, height).build();
    }
#else
    public static Button newButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return new Button(x, y, width, height, text, onPress);
    }
#endif

    public static int getGuiScaledHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public static int getGuiScaledWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public static class WithTextrue extends WithTexture {
        public WithTextrue(Component title) {
            super(title);
        }
    }

    public static class WithTexture extends Screen {
        public GraphicsTexture texture;
        public ProxyObject state = ProxyObject.fromMap(new HashMap<>()); 

        public InitFunction initFunction = (screen, width, height) -> {};
        public KeyPressResponder keyPressResponder = (screen, p_96552_, p_96553_, p_96554_) -> false;
        public InsertTextFunction insertTextFunction = (screen, text, p_96588_) -> {};
        public RenderFunction renderFunction = (screen, mouseX, mouseY, delta) -> {};
        public Consumer<WithTexture> tickFunction = screen -> {};
        public BiConsumer<WithTexture, List<Path>> onFilesDropFunction = (screen, paths) -> {};
        public Consumer<WithTexture> onCloseFunction = screen -> Minecraft.getInstance().setScreen(null);
        public MouseClickedFunction mouseClickedFunction = (screen, p_94695_, p_94696_, p_94697_) -> false;
        public MouseMovedFunction mouseMovedFunction = (screen, p_94758_, p_94759_) -> {};
        public IsMouseOverFunction isMouseOverFunction = (screen, p_94748_, p_94749_) -> false;
        public CharTypedFunction charTypedFunction = (screen, p_94732_, p_94733_) -> false;
        public KeyReleasedFunction keyReleasedFunction = (screen, p_94750_, p_94751_, p_94752_) -> false;
        public MouseScrolledFunction mouseScrolledFunction = (screen, p_94734_, p_94735_, p_94736_) -> false;
        public MouseDraggedFunction mouseDraggedFunction = (screen, p_94737_, p_94738_, p_94739_, p_94740_, p_94741_) -> false;
        public MouseReleasedFunction mouseReleasedFunction = (screen, p_94742_, p_94743_, p_94744_) -> false;
        public boolean isPauseScreen = false;

        public WithTexture(Component title) {
            super(title);
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

#if MC_VERSION >= "11903"
        public <T extends GuiEventListener & Renderable & NarratableEntry> T _addRenderableWidget(T p_169406_) {
#else
        public <T extends GuiEventListener & Widget & NarratableEntry> T _addRenderableWidget(T p_169406_) {
#endif
            return super.addRenderableWidget(p_169406_);
        }

#if MC_VERSION >= "11903"
        public <T extends Renderable> T _addRenderableOnly(T p_169395_) {
#else
        public <T extends Widget> T _addRenderableOnly(T p_169395_) {
#endif
            return super.addRenderableOnly(p_169395_);
        }

        public <T extends GuiEventListener & NarratableEntry> T _addWidget(T p_96625_) {
            return super.addWidget(p_96625_);
        }

        public void _removeWidget(GuiEventListener p_169412_) {
            super.removeWidget(p_169412_);
        }

        @Override
        protected void init() {
            super.init();
            Window window = Minecraft.getInstance().getWindow();
            int widthO = window.getWidth();
            int heightO = window.getHeight();
            if (texture != null) texture.close();
            texture = new GraphicsTexture(widthO, heightO);
            texture.upload();
            try {
                initFunction.init(this, width, height);
            } catch (Exception e) {
                print ("initFunction error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        public boolean keyPressed(int p_96552_, int p_96553_, int p_96554_) {
            boolean flag = false;
            try {
                flag = keyPressResponder.keyPressed(this, p_96552_, p_96553_, p_96554_);
            } catch (Exception e) {
                print ("keyPressResponder error: " + e.getMessage());
                e.printStackTrace();
            }

            return flag || super.keyPressed(p_96552_, p_96553_, p_96554_);
        }

        @Override
        protected void insertText(String p_96587_, boolean p_96588_) {
            try {
                insertTextFunction.insertText(this, p_96587_, p_96588_);
            } catch (Exception e) {
                print ("insertTextFunction error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        public void onFilesDrop(List<Path> p_96591_) {
            try {
                onFilesDropFunction.accept(this, p_96591_);
            } catch (Exception e) {
                print ("onFilesDropFunction error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
#if MC_VERSION >= "12000"
        public void render(GuiGraphics matrices, int mouseX, int mouseY, float delta) {
#else
        public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
#endif
            super.render(matrices, mouseX, mouseY, delta);

            try {
                if (texture != null) {
#if MC_VERSION >= "12000"
                    matrices.blit(texture.identifier, 0, 0, width, height, 0, 0, 1, 1, 1, 1);
#else
                    RenderSystem.setShaderTexture(0, texture.identifier);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    blit(matrices, 0, 0, 0f, 0f, texture.width, texture.height, width, height);
#endif
                }
            } catch (Exception e) {
                print ("blit error: " + e.getMessage());
                e.printStackTrace();
            }

            try {
                renderFunction.render(this, mouseX, mouseY, delta);
            } catch (Exception e) {
                print ("renderFunction error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        public void tick() {
            try {
                tickFunction.accept(this);
            } catch (Exception e) {
                print ("tickFunction error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        public void onClose() {
            try {
                onCloseFunction.accept(this);
            } catch (Exception e) {
                print ("onClose error: " + e.getMessage());
                e.printStackTrace();
            }
            if (texture != null) texture.close();
        }

        public boolean mouseClicked(double p_94695_, double p_94696_, int p_94697_) {
            boolean flag = false;
            try {
                flag = mouseClickedFunction.mouseClicked(this, p_94695_, p_94696_, p_94697_);
            } catch (Exception e) {
                print ("mouseClickedFunction error: " + e.getMessage());
                e.printStackTrace();
            }
            return flag || super.mouseClicked(p_94695_, p_94696_, p_94697_);
        }

        void print(String s) {
            Main.LOGGER.info(s);
        }

        @Override
        public boolean isPauseScreen() {
            return isPauseScreen;
        }

        @Override
        public void mouseMoved(double p_94758_, double p_94759_) {
            try {
                mouseMovedFunction.mouseMoved(this, p_94758_, p_94759_);
            } catch (Exception e) {
                print ("mouseMovedFunction error: " + e.getMessage());
                e.printStackTrace();
            }
            super.mouseMoved(p_94758_, p_94759_);
        }

        @Override
        public boolean isMouseOver(double p_94748_, double p_94749_) {
            boolean flag = false;
            try {
                flag = isMouseOverFunction.isMouseOver(this, p_94748_, p_94749_);
            } catch (Exception e) {
                print ("isMouseOverFunction error: " + e.getMessage());
                e.printStackTrace();
            }
            return flag || super.isMouseOver(p_94748_, p_94749_);
        }

        @Override
        public boolean charTyped(char p_94732_, int p_94733_) {
            boolean flag = false;
            try {
                flag = charTypedFunction.charTyped(this, p_94732_, p_94733_);
            } catch (Exception e) {
                print ("charTypedFunction error: " + e.getMessage());
                e.printStackTrace();
            }
            return flag || super.charTyped(p_94732_, p_94733_);
        }

        @Override
        public boolean keyReleased(int p_94750_, int p_94751_, int p_94752_) {
            boolean flag = false;
            try {
                flag = keyReleasedFunction.keyReleased(this, p_94750_, p_94751_, p_94752_);
            } catch (Exception e) {
                print ("keyReleasedFunction error: " + e.getMessage());
                e.printStackTrace();
            }
            return flag || super.keyReleased(p_94750_, p_94751_, p_94752_);
        }

        @Override
        public boolean mouseScrolled(double p_94734_, double p_94735_, double p_94736_) {
            boolean flag = false;
            try {
                flag = mouseScrolledFunction.mouseScrolled(this, p_94734_, p_94735_, p_94736_);
            } catch (Exception e) {
                print ("mouseScrolledFunction error: " + e.getMessage());
                e.printStackTrace();
            }
            return flag || super.mouseScrolled(p_94734_, p_94735_, p_94736_);
        }

        @Override
        public boolean mouseDragged(double p_94740_, double p_94741_, int p_94742_, double p_94743_, double p_94744_) {
            boolean flag = false;
            try {
                flag = mouseDraggedFunction.mouseDragged(this, p_94740_, p_94741_, p_94742_, p_94743_, p_94744_);
            } catch (Exception e) {
                print ("mouseDraggedFunction error: " + e.getMessage());
                e.printStackTrace();
            }
            return flag || super.mouseDragged(p_94740_, p_94741_, p_94742_, p_94743_, p_94744_);
        }

        @Override
        public boolean mouseReleased(double p_94753_, double p_94754_, int p_94755_) {
            boolean flag = false;
            try {
                flag = mouseReleasedFunction.mouseReleased(this, p_94753_, p_94754_, p_94755_);
            } catch (Exception e) {
                print ("mouseReleasedFunction error: " + e.getMessage());
                e.printStackTrace();
            }
            return flag || super.mouseReleased(p_94753_, p_94754_, p_94755_);
        }

        public interface InitFunction {
            void init(WithTexture screen, int width, int height);
        }

        public interface KeyPressResponder {
            boolean keyPressed(WithTexture screen, int p_96552_, int p_96553_, int p_96554_);
        }

        public interface RenderFunction {
            void render(WithTexture screen, int mouseX, int mouseY, float delta);
        }

        public interface InsertTextFunction {
            void insertText(WithTexture screen, String text, boolean p_96588_);
        }

        public interface MouseClickedFunction {
            boolean mouseClicked(WithTexture screen, double p_94695_, double p_94696_, int p_94697_);
        }

        public interface MouseMovedFunction {
            void mouseMoved(WithTexture screen, double p_94758_, double p_94759_);
        }

        public interface MouseReleasedFunction {
            boolean mouseReleased(WithTexture screen, double p_94753_, double p_94754_, int p_94755_);
        }

        public interface MouseDraggedFunction {
            boolean mouseDragged(WithTexture screen, double p_94740_, double p_94741_, int p_94742_, double p_94743_, double p_94744_);
        }

        public interface MouseScrolledFunction {
            boolean mouseScrolled(WithTexture screen, double p_94734_, double p_94735_, double p_94736_);
        }

        public interface KeyReleasedFunction {
            boolean keyReleased(WithTexture screen, int p_94750_, int p_94751_, int p_94752_);
        }

        public interface CharTypedFunction {
            boolean charTyped(WithTexture screen, char p_94732_, int p_94733_);
        }

        public interface IsMouseOverFunction {
            boolean isMouseOver(WithTexture screen, double p_94748_, double p_94749_);
        }


    }
}