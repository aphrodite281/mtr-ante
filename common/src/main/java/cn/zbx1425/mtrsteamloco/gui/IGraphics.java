package cn.zbx1425.mtrsteamloco.gui;

#if MC_VERSION >= "12000"
import net.minecraft.client.gui.GuiGraphics;
#else
import net.minecraft.client.gui.GuiComponent;
#endif

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Vec3i;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.text2speech.Narrator;

import mtr.MTR;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.UtilitiesClient;
import mtr.screen.WidgetBetterTextField;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.text2speech.Narrator;
import mtr.MTR;
import mtr.data.IGui;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.screen.WidgetBetterTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.List;

public interface IGraphics{

#if MC_VERSION >= "12000"
    default void drawStringWithFont(GuiGraphics gg, Font textRenderer, MultiBufferSource.BufferSource immediate, String text, float x, float y, int light) {
        drawStringWithFont(gg.pose(), textRenderer, immediate, text, x, y, light);
	}

	default void drawStringWithFont(GuiGraphics gg, Font textRenderer, MultiBufferSource.BufferSource immediate, String text, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColor, boolean shadow, int light, DrawingCallback drawingCallback) {
		drawStringWithFont(gg.pose(), textRenderer, immediate, text, horizontalAlignment, verticalAlignment, horizontalAlignment, x, y, maxWidth, maxHeight, scale, textColor, shadow, light, drawingCallback);
	}

	default void drawStringWithFont(GuiGraphics gg, Font textRenderer, MultiBufferSource.BufferSource immediate, String text, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, IGui.HorizontalAlignment xAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColor, boolean shadow, int light, DrawingCallback drawingCallback) {
		drawStringWithFont(gg.pose(), textRenderer, immediate, text, horizontalAlignment, verticalAlignment, xAlignment, x, y, maxWidth, maxHeight, scale, textColor, textColor, 2, shadow, light, drawingCallback);
	}

	default void drawStringWithFont(GuiGraphics gg, Font textRenderer, MultiBufferSource.BufferSource immediate, String text, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, IGui.HorizontalAlignment xAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColorCjk, int textColor, float fontSizeRatio, boolean shadow, int light, DrawingCallback drawingCallback) {
		drawStringWithFont(gg.pose(), textRenderer, immediate, text, horizontalAlignment, verticalAlignment, xAlignment, x, y, maxWidth, maxHeight, scale, textColorCjk, textColor, fontSizeRatio, shadow, light, drawingCallback);
	}

	default void drawLine(GuiGraphics matrices, MultiBufferSource vertexConsumers, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b) {
        drawLine(matrices.pose(), vertexConsumers, x1, y1, z1, x2, y2, z2, r, g, b);
	}

	default void drawTexture(GuiGraphics gg, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, Direction facing, int color, int light) {
		drawTexture(gg.pose(), vertexConsumer, x1, y1, z1, x2, y2, z2, 0, 0, 1, 1, facing, color, light);
	}

	default void drawTexture(GuiGraphics gg, VertexConsumer vertexConsumer, float x, float y, float width, float height, Direction facing, int light) {
		drawTexture(gg.pose(), vertexConsumer, x, y, 0, x + width, y + height, 0, 0, 0, 1, 1, facing, -1, light);
	}

	default void drawTexture(GuiGraphics gg, VertexConsumer vertexConsumer, float x, float y, float width, float height, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		drawTexture(gg.pose(), vertexConsumer, x, y, 0, x + width, y + height, 0, u1, v1, u2, v2, facing, color, light);
	}

	default void drawTexture(GuiGraphics gg, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		drawTexture(gg.pose(), vertexConsumer, x1, y2, z1, x2, y2, z2, x2, y1, z2, x1, y1, z1, u1, v1, u2, v2, facing, color, light);
	}

	default void drawTexture(GuiGraphics gg, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
        drawTexture(gg.pose(), vertexConsumer, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, u1, v1, u2, v2, facing, color, light);
	}
#endif

	default void drawStringWithFont(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, String text, float x, float y, int light) {
		drawStringWithFont(matrices, textRenderer, immediate, text, IGui.HorizontalAlignment.CENTER, IGui.VerticalAlignment.CENTER, x, y, -1, -1, 1, IGui.ARGB_WHITE, true, light, null);
	}

	default void drawStringWithFont(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, String text, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColor, boolean shadow, int light, DrawingCallback drawingCallback) {
		drawStringWithFont(matrices, textRenderer, immediate, text, horizontalAlignment, verticalAlignment, horizontalAlignment, x, y, maxWidth, maxHeight, scale, textColor, shadow, light, drawingCallback);
	}

	default void drawStringWithFont(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, String text, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, IGui.HorizontalAlignment xAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColor, boolean shadow, int light, DrawingCallback drawingCallback) {
		drawStringWithFont(matrices, textRenderer, immediate, text, horizontalAlignment, verticalAlignment, xAlignment, x, y, maxWidth, maxHeight, scale, textColor, textColor, 2, shadow, light, drawingCallback);
	}

	default void drawStringWithFont(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, String text, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, IGui.HorizontalAlignment xAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColorCjk, int textColor, float fontSizeRatio, boolean shadow, int light, DrawingCallback drawingCallback) {
		final Style style = Style.EMPTY;

		while (text.contains("||")) {
			text = text.replace("||", "|");
		}
		final String[] stringSplit = text.split("\\|");

		final List<Boolean> isCJKList = new ArrayList<>();
		final List<FormattedCharSequence> orderedTexts = new ArrayList<>();
		int totalHeight = 0, totalWidth = 0;
		for (final String stringSplitPart : stringSplit) {
			final boolean isCJK = IGui.isCjk(stringSplitPart);
			isCJKList.add(isCJK);

			final FormattedCharSequence orderedText = Text.literal(stringSplitPart).setStyle(style).getVisualOrderText();
			orderedTexts.add(orderedText);

			totalHeight += IGui.LINE_HEIGHT * (isCJK ? fontSizeRatio : 1);
			final int width = (int) Math.ceil(textRenderer.width(orderedText) * (isCJK ? fontSizeRatio : 1));
			if (width > totalWidth) {
				totalWidth = width;
			}
		}

		if (maxHeight >= 0 && totalHeight / scale > maxHeight) {
			scale = totalHeight / maxHeight;
		}

		matrices.pushPose();

		final float totalWidthScaled;
		final float scaleX;
		if (maxWidth >= 0 && totalWidth > maxWidth * scale) {
			totalWidthScaled = maxWidth * scale;
			scaleX = totalWidth / maxWidth;
		} else {
			totalWidthScaled = totalWidth;
			scaleX = scale;
		}
		matrices.scale(1 / scaleX, 1 / scale, 1 / scale);

		float offset = verticalAlignment.getOffset(y * scale, totalHeight);
		for (int i = 0; i < orderedTexts.size(); i++) {
			final boolean isCJK = isCJKList.get(i);
			final float extraScale = isCJK ? fontSizeRatio : 1;
			if (isCJK) {
				matrices.pushPose();
				matrices.scale(extraScale, extraScale, 1);
			}

			final float xOffset = horizontalAlignment.getOffset(xAlignment.getOffset(x * scaleX, totalWidth), textRenderer.width(orderedTexts.get(i)) * extraScale - totalWidth);

			final float shade = light == IGui.MAX_LIGHT_GLOWING ? 1 : Math.min(LightTexture.block(light) / 16F * 0.1F + 0.7F, 1);
			final int a = ((isCJK ? textColorCjk : textColor) >> 24) & 0xFF;
			final int r = (int) ((((isCJK ? textColorCjk : textColor) >> 16) & 0xFF) * shade);
			final int g = (int) ((((isCJK ? textColorCjk : textColor) >> 8) & 0xFF) * shade);
			final int b = (int) (((isCJK ? textColorCjk : textColor) & 0xFF) * shade);

			if (immediate != null) {
				UtilitiesClient.drawInBatch(textRenderer, orderedTexts.get(i), xOffset / extraScale, offset / extraScale, (a << 24) + (r << 16) + (g << 8) + b, shadow, matrices.last().pose(), immediate, 0, light);
			}

			if (isCJK) {
				matrices.popPose();
			}

			offset += IGui.LINE_HEIGHT * extraScale;
		}

		matrices.popPose();

		if (drawingCallback != null) {
			final float x1 = xAlignment.getOffset(x, totalWidthScaled / scale);
			final float y1 = verticalAlignment.getOffset(y, totalHeight / scale);
			drawingCallback.drawingCallback(x1, y1, x1 + totalWidthScaled / scale, y1 + totalHeight / scale);
		}
	}

	default void drawLine(PoseStack matrices, MultiBufferSource vertexConsumers, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b) {
		final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderType.lines());
		final PoseStack.Pose pose = matrices.last();
		vertexConsumer.vertex(pose.pose(), x1, y1, z1).color(r, g, b, 0xFF).normal(pose.normal(), 0, 1, 0).endVertex();
		vertexConsumer.vertex(pose.pose(), x2, y2, z2).color(r, g, b, 0xFF).normal(pose.normal(), 0, 1, 0).endVertex();
	}

	default void drawRectangle(VertexConsumer vertexConsumer, double x1, double y1, double x2, double y2, int color) {
		final int a = (color >> 24) & 0xFF;
		final int r = (color >> 16) & 0xFF;
		final int g = (color >> 8) & 0xFF;
		final int b = color & 0xFF;
		if (a == 0) {
			return;
		}
		vertexConsumer.vertex(x1, y1, 0).color(r, g, b, a).endVertex();
		vertexConsumer.vertex(x1, y2, 0).color(r, g, b, a).endVertex();
		vertexConsumer.vertex(x2, y2, 0).color(r, g, b, a).endVertex();
		vertexConsumer.vertex(x2, y1, 0).color(r, g, b, a).endVertex();
	}

	default void drawTexture(PoseStack matrices, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, Direction facing, int color, int light) {
		drawTexture(matrices, vertexConsumer, x1, y1, z1, x2, y2, z2, 0, 0, 1, 1, facing, color, light);
	}

	default void drawTexture(PoseStack matrices, VertexConsumer vertexConsumer, float x, float y, float width, float height, Direction facing, int light) {
		drawTexture(matrices, vertexConsumer, x, y, 0, x + width, y + height, 0, 0, 0, 1, 1, facing, -1, light);
	}

	default void drawTexture(PoseStack matrices, VertexConsumer vertexConsumer, float x, float y, float width, float height, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		drawTexture(matrices, vertexConsumer, x, y, 0, x + width, y + height, 0, u1, v1, u2, v2, facing, color, light);
	}

	default void drawTexture(PoseStack matrices, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		drawTexture(matrices, vertexConsumer, x1, y2, z1, x2, y2, z2, x2, y1, z2, x1, y1, z1, u1, v1, u2, v2, facing, color, light);
	}

	default void drawTexture(PoseStack matrices, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		final Vec3i vec3i = facing.getNormal();
		final PoseStack.Pose pose = matrices.last();
		final int a = (color >> 24) & 0xFF;
		final int r = (color >> 16) & 0xFF;
		final int g = (color >> 8) & 0xFF;
		final int b = color & 0xFF;
		if (a == 0) {
			return;
		}
		vertexConsumer.vertex(pose.pose(), x1, y1, z1).color(r, g, b, a).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), vec3i.getX(), vec3i.getY(), vec3i.getZ()).endVertex();
		vertexConsumer.vertex(pose.pose(), x2, y2, z2).color(r, g, b, a).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), vec3i.getX(), vec3i.getY(), vec3i.getZ()).endVertex();
		vertexConsumer.vertex(pose.pose(), x3, y3, z3).color(r, g, b, a).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), vec3i.getX(), vec3i.getY(), vec3i.getZ()).endVertex();
		vertexConsumer.vertex(pose.pose(), x4, y4, z4).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), vec3i.getX(), vec3i.getY(), vec3i.getZ()).endVertex();
	}

	default void setPositionAndWidth(AbstractWidget widget, int x, int y, int widgetWidth) {
		UtilitiesClient.setWidgetX(widget, x);
		UtilitiesClient.setWidgetY(widget, y);
		widget.setWidth(Mth.clamp(widgetWidth, 0, 380 - (widget instanceof WidgetBetterTextField ? IGui.TEXT_FIELD_PADDING : 0)));
	}

	@FunctionalInterface
	interface DrawingCallback {
		void drawingCallback(float x1, float y1, float x2, float y2);
	}

#if MC_VERSION >= "12000"
    default void drawText(GuiGraphics guiGraphics, Font font, FormattedCharSequence text, int x, int y, int color) {
        guiGraphics.drawString(font, text, x, y, color);
    }

    default void blit(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height) {
        guiGraphics.blit(texture, x, y, width, height, 0, 0, 1, 1, 1, 1);
    }

    default void IFill(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, width, height, color);
    }

    default void drawCenteredString(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color) {
        guiGraphics.drawCenteredString(font, text, x, y, color);
    }

    default void renderDirtBackground(Screen screen, GuiGraphics guiGraphics) {
        screen.renderDirtBackground(guiGraphics);
    }
#else
    default void drawText(PoseStack matrices, Font font, FormattedCharSequence text, int x, int y, int color) {
        font.drawShadow(matrices, text, x, y, color);
    }

    default void blit(PoseStack matrices, ResourceLocation texture, int x, int y, int width, int height) {
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(matrices, x, y, width, height, 0, 0, 1, 1, 1, 1);
    }

	default void IFill(PoseStack matrices, int x, int y, int width, int height, int color) {
		GuiComponent.fill(matrices, x, y, width, height, color);
	}

#if MC_VERSION >= "11904"
    default void renderDirtBackground(Screen screen, PoseStack matrices) {
        screen.renderDirtBackground(matrices);
    }
#else 
    default void renderDirtBackground(Screen screen, PoseStack matrices) {
        screen.renderDirtBackground(0);
    }
#endif
#endif
}
