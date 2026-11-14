package com.pixelclasses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pixelclasses.PixelClassesMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Shared drawing helpers for the pixel-art windows. */
public final class UiDraw {
    public static final ResourceLocation PANEL = tex("panel");
    public static final ResourceLocation ROW = tex("row");
    public static final ResourceLocation ROW_HOVER = tex("row_hover");
    public static final ResourceLocation BANNER = tex("banner");
    public static final ResourceLocation SLOT = tex("slot");

    private UiDraw() {}

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(PixelClassesMod.MODID, "textures/gui/" + name + ".png");
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** 9-slice: fixed corners, stretched edges and centre. ts = texture size, b = border size. */
    public static void nineSlice(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int ts, int b) {
        int m = ts - 2 * b;
        g.blit(tex, x, y, 0, 0, b, b, ts, ts);
        g.blit(tex, x + w - b, y, ts - b, 0, b, b, ts, ts);
        g.blit(tex, x, y + h - b, 0, ts - b, b, b, ts, ts);
        g.blit(tex, x + w - b, y + h - b, ts - b, ts - b, b, b, ts, ts);
        g.blit(tex, x + b, y, w - 2 * b, b, (float) b, 0.0F, m, b, ts, ts);
        g.blit(tex, x + b, y + h - b, w - 2 * b, b, (float) b, (float) (ts - b), m, b, ts, ts);
        g.blit(tex, x, y + b, b, h - 2 * b, 0.0F, (float) b, b, m, ts, ts);
        g.blit(tex, x + w - b, y + b, b, h - 2 * b, (float) (ts - b), (float) b, b, m, ts, ts);
        g.blit(tex, x + b, y + b, w - 2 * b, h - 2 * b, (float) b, (float) b, m, m, ts, ts);
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        nineSlice(g, PANEL, x, y, w, h, 32, 8);
        g.fillGradient(x + 6, y + 6, x + w - 6, y + 40, 0x30FFFFFF, 0x00FFFFFF);
    }

    public static void row(GuiGraphics g, int x, int y, int w, int h, boolean hover) {
        nineSlice(g, hover ? ROW_HOVER : ROW, x, y, w, h, 16, 3);
    }

    public static void banner(GuiGraphics g, Font font, Component title, int centerX, int y) {
        g.blit(BANNER, centerX - 64, y, 0, 0, 128, 24, 128, 24);
        g.drawCenteredString(font, title, centerX, y + 8, 0xFFFFE9A8);
    }

    public static void button(GuiGraphics g, Font font, Component label, int x, int y, int w, int h, boolean hover, boolean enabled) {
        row(g, x, y, w, h, hover && enabled);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, enabled ? 0xFFFFFFFF : 0xFF777790);
    }

    /** 16x16 icon texture drawn at any size. */
    public static void icon(GuiGraphics g, ResourceLocation tex, int x, int y, int size) {
        g.blit(tex, x, y, size, size, 0.0F, 0.0F, 16, 16, 16, 16);
    }

    public static void scaled(GuiGraphics g, Font font, String text, int x, int y, float scale, int color, boolean shadow) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0F);
        g.drawString(font, text, 0, 0, color, shadow);
        pose.popPose();
    }

    public static void scaledCentered(GuiGraphics g, Font font, String text, int centerX, int y, float scale, int color, boolean shadow) {
        scaled(g, font, text, centerX - (int) (font.width(text) * scale / 2.0F), y, scale, color, shadow);
    }

    /** Word wrap that also works for Thai (no spaces): falls back to cutting by character. */
    public static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String rest = text.trim();
        while (!rest.isEmpty()) {
            if (font.width(rest) <= maxWidth) {
                lines.add(rest);
                break;
            }
            int fit = 1;
            while (fit < rest.length() && font.width(rest.substring(0, fit + 1)) <= maxWidth) {
                fit++;
            }
            int cut = fit;
            int space = rest.lastIndexOf(' ', fit);
            if (space > fit / 2) {
                cut = space;
            }
            lines.add(rest.substring(0, cut).trim());
            rest = rest.substring(cut).trim();
        }
        return lines;
    }
}
