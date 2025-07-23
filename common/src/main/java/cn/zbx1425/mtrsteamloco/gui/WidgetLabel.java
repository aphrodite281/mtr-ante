package cn.zbx1425.mtrsteamloco.gui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
#if MC_VERSION >= "12000"
import net.minecraft.client.gui.GuiGraphics;
#endif
import net.minecraft.client.gui.components.AbstractWidget;
#if MC_VERSION >= "11700"
import net.minecraft.client.gui.narration.NarrationElementOutput;
#endif
import net.minecraft.network.chat.Component;

public class WidgetLabel extends AbstractWidget {

    public boolean alignR = false;
    public boolean centre = true;
    public int color = -1;

    private final Runnable onClick;

    public WidgetLabel(int x, int y, int width, Component text) {
        super(x, y, width, text.getString().split("\n", -1).length * 10, text);
        this.onClick = null;
    }

    public WidgetLabel(int x, int y, int width, Component text, Runnable onClick) {
        super(x, y, width, text.getString().split("\n", -1).length * 10, text);
        this.onClick = onClick;
    }

    @Override
#if MC_VERSION >= "12000"
    public void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
#elif MC_VERSION >= "11904"
    public void renderWidget(PoseStack ctx, int mouseX, int mouseY, float delta) {
#else
    public void render(PoseStack ctx, int mouseX, int mouseY, float delta) {
#endif
        if (!visible) return;
        String[] lines = this.getMessage().getString().split("\n");
        this.height = lines.length * 10;
        if (mouseX >= this.getX() && mouseX < this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY < this.getY() + this.getHeight()) {
            IGraphics.fill(ctx, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x88a8a9ad);
        }
        for (int i = 0; i < lines.length; ++i) {
            int textWidth = Minecraft.getInstance().font.width(lines[i]);
#if MC_VERSION >= "11903"
            int x = centre ?  this.getX() + (this.getWidth() - textWidth) / 2 : alignR ? this.getX() + this.getWidth() - textWidth : this.getX();
            int y = this.getY() + 10 * i;
#else
            int x = centre ?  this.getX() + (this.getWidth() - textWidth) / 2 : alignR ? this.getX() + this.width - textWidth : this.getX();
            int y = this.getY() + 10 * i;
#endif
            if (textWidth > this.width) {
                int offset = (int)(System.currentTimeMillis() / 25 % (textWidth + 40));
                AbstractScrollWidget.vcEnableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);
#if MC_VERSION >= "12000"
                ctx.drawString(Minecraft.getInstance().font, lines[i], x - offset, y, color);
                ctx.drawString(Minecraft.getInstance().font, lines[i], x + textWidth + 40 - offset, y, color);
#else
                drawString(ctx, Minecraft.getInstance().font, lines[i], x - offset, y, color);
                drawString(ctx, Minecraft.getInstance().font, lines[i], x + textWidth + 40 - offset, y, color);
#endif
                RenderSystem.disableScissor();
            } else {
#if MC_VERSION >= "12000"
                ctx.drawString(Minecraft.getInstance().font, lines[i], x, y, color);
#else
                drawString(ctx, Minecraft.getInstance().font, lines[i], x, y, color);
#endif
            }
            if (!isActive()) {
#if MC_VERSION >= "12000"
                ctx.drawString(Minecraft.getInstance().font, "▶", x - 8, y, 0xffff0000);
#else
                drawString(ctx, Minecraft.getInstance().font, "▶", x - 8, y, 0xffff0000);
#endif
            }
        }
    }

    @Override
    public void onClick(double d, double e) {
        super.onClick(d, e);
        if (onClick != null) onClick.run();
    }

#if MC_VERSION >= "11903"
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { }
#elif MC_VERSION >= "11700"
    @Override
    public void updateNarration(NarrationElementOutput arg) { }
#endif

#if MC_VERSION < "11903"
    protected int getX() {
        return x;
    }

    protected int getY() {
        return y;
    }
#endif

    public void setHeight(int height) {
        this.height = height;
    }
}
