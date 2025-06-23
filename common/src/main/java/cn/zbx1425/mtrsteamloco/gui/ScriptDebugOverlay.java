package cn.zbx1425.mtrsteamloco.gui;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.render.scripting.AbstractScriptContext;
import cn.zbx1425.mtrsteamloco.render.scripting.ScriptContextManager;
import cn.zbx1425.mtrsteamloco.render.scripting.ScriptHolderBase;
import cn.zbx1425.mtrsteamloco.render.scripting.util.client.GraphicsTexture;
import com.google.common.base.Splitter;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
#if MC_VERSION >= "12000"
import net.minecraft.client.gui.GuiGraphics;
#else
import net.minecraft.client.gui.GuiComponent;
#endif
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import cn.zbx1425.mtrsteamloco.render.scripting.util.OrderedMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptDebugOverlay {

    public static final OrderedMap<String, Object> STATIC = new OrderedMap<>();

#if MC_VERSION >= "12000"
    public synchronized static void render(GuiGraphics vdStuff) {
        PoseStack matrices = vdStuff.pose();
#else
    public synchronized static void render(PoseStack vdStuff) {
        PoseStack matrices = vdStuff;
#endif
        if (!ClientConfig.enableScriptDebugOverlay) return;
        if (Minecraft.getInstance().screen != null) return;

        matrices.pushPose();
        matrices.translate(10, 10, 0);

        Map<ScriptHolderBase, List<AbstractScriptContext>> contexts = new HashMap<>();
        for (Map.Entry<AbstractScriptContext, ScriptHolderBase> entry : ScriptContextManager.livingContexts.entrySet()) {
            contexts.computeIfAbsent(entry.getValue(), k -> new java.util.ArrayList<>()).add(entry.getKey());
        }

        int y = 0;
        Font font = Minecraft.getInstance().font;
        int lineHeight = Mth.ceil(font.lineHeight * 1.2f);

        for (Map.Entry<String, Object> entry : STATIC.entryList()) {
            y = drawText(vdStuff, font, entry.getKey() + ": " + entry.getValue(), 20, y, 0xFFFFFFFF);
        }

        boolean outOfBounds = false;
        for (Map.Entry<ScriptHolderBase, List<AbstractScriptContext>> entry : contexts.entrySet()) {
            ScriptHolderBase holder = entry.getKey();
            if (holder.failTime > 0) {
                y = drawText(vdStuff, font, holder.name + " FAILED", 0, y, 0xFFFF0000);
                if (holder.failException != null) {
                    y = drawText(vdStuff, font, holder.failException.getMessage(), 5, y, 0xFFFF8888);
                }
            } else {
                y = drawText(vdStuff, font, holder.name, 0, y, 0xFFAAAAFF);
            }
            for (AbstractScriptContext context : entry.getValue()) {
                y = drawText(vdStuff, font,
                    String.format("#%08X (%.2f ms)", context.hashCode(), context.lastExecuteDuration / 1e6),
                    10, y, 0xFFCCCCFF);
                List<Map.Entry<String, Object>> debugInfos = context.getDebugInfo().entryList();
                for (Map.Entry<String, Object> debugInfo : debugInfos) {
                    Object value = debugInfo.getValue();
                    if (value instanceof GraphicsTexture) {
                        GraphicsTexture texture = (GraphicsTexture) value;
                        float scale0 = (Minecraft.getInstance().getWindow().getGuiScaledWidth() - 40) / (float) texture.width;
                        float scale1 = font.lineHeight * 5 / (float) texture.height;
                        float scale = Math.min(scale0, scale1);
                        y = drawText(vdStuff, font, debugInfo.getKey() + ": GraphicsTexture", 20, y, 0xFFFFFFFF);
                        blit(vdStuff, texture.identifier, 20, y, (int)(texture.width * scale), (int)(texture.height * scale));
                        y += (int)(texture.height * scale);
                    } else {
                        y = drawText(vdStuff, font, debugInfo.getKey() + ": " + debugInfo.getValue(), 20, y, 0xFFFFFFFF);
                    }
                    y += Mth.ceil(font.lineHeight * 0.2f);
                    if (y > Minecraft.getInstance().getWindow().getGuiScaledHeight()) {
                        outOfBounds = true;
                        break;
                    }
                }
                if (outOfBounds) break;
            }
            if (outOfBounds) break;
        }

        matrices.popPose();
    }

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
#endif
}
