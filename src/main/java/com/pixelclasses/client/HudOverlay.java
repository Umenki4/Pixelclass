package com.pixelclasses.client;

import com.pixelclasses.config.ClassConfig;
import com.pixelclasses.data.ClassDef;
import com.pixelclasses.data.Definitions;
import com.pixelclasses.data.SkillDef;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Mana bar + the 4 skill slots (icon, key, cooldown sweep) in the bottom-right corner. */
public class HudOverlay implements IGuiOverlay {
    private static final int SLOT = 24;
    private static final int GAP = 3;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        ClassDef cls = ClientData.currentClass();
        if (mc.player == null || mc.options.hideGui || cls == null) {
            return;
        }
        Font font = mc.font;
        int totalW = 4 * SLOT + 3 * GAP;
        int x0 = width - totalW - 8;
        int y0 = height - SLOT - 8;

        // mana bar
        int barH = 9;
        int by = y0 - barH - 6;
        float frac = ClientData.maxMana > 0 ? Mth.clamp(ClientData.mana / ClientData.maxMana, 0.0F, 1.0F) : 0.0F;
        int fillW = (int) (totalW * frac);
        g.fill(x0 - 1, by - 1, x0 + totalW + 1, by + barH + 1, 0xFF0A0812);
        g.fill(x0, by, x0 + totalW, by + barH, 0xFF1A1630);
        g.fill(x0, by, x0 + fillW, by + barH, cls.manaColor());
        g.fillGradient(x0, by, x0 + fillW, by + barH / 2, 0x66FFFFFF, 0x00FFFFFF);
        String label = ClientData.text(cls.manaName()) + " " + (int) ClientData.mana + "/" + (int) ClientData.maxMana;
        UiDraw.scaledCentered(g, font, label, x0 + totalW / 2, by + 2, 0.75F, 0xFFFFFFFF, true);

        // skill slots
        for (int i = 0; i < 4; i++) {
            int sx = x0 + i * (SLOT + GAP);
            g.blit(UiDraw.SLOT, sx, y0, 0, 0, SLOT, SLOT, SLOT, SLOT);
            String id = ClientData.equipped[i];
            SkillDef skill = id.isEmpty() ? null : Definitions.CLIENT.skillDef(ResourceLocation.tryParse(id));
            if (skill != null) {
                UiDraw.icon(g, skill.icon(), sx + 4, y0 + 4, 16);
                int left = ClientData.cooldownLeft(id);
                double cost = skill.manaCost() * ClassConfig.get(ClassConfig.MANA_COST_MULT);
                if (left > 0) {
                    float f = left / (float) ClientData.cooldownTotal(id);
                    g.fill(sx + 4, y0 + 4, sx + 20, y0 + 4 + Math.max(1, (int) (16 * f)), 0xB0000000);
                    UiDraw.scaledCentered(g, font, String.format(Locale.ROOT, "%.1f", left / 20.0F), sx + SLOT / 2, y0 + 9, 0.75F, 0xFFFFFFFF, true);
                } else if (ClientData.mana < cost) {
                    g.fill(sx + 4, y0 + 4, sx + 20, y0 + 20, 0x80203070);
                }
            }
            UiDraw.scaled(g, font, KeyBindings.CAST[i].getTranslatedKeyMessage().getString(), sx + 3, y0 + 2, 0.7F, 0xFFFFE9A8, true);
        }
    }
}
