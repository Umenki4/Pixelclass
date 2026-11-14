package com.pixelclasses.client;

import com.pixelclasses.config.ClassConfig;
import com.pixelclasses.data.ClassDef;
import com.pixelclasses.data.Definitions;
import com.pixelclasses.data.SkillDef;
import com.pixelclasses.network.C2S;
import com.pixelclasses.network.ModNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * The skill window (key J): shows the 4 equipped slots and every skill of your class.
 * Press the small 1-4 buttons on a skill row to equip it in that slot; click a slot to empty it.
 */
public class SkillScreen extends Screen {
    private static final int PANEL_W = 296;
    private static final int ROW_W = PANEL_W - 28;
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 2;
    private static final int HEADER_H = 98;
    private static final int NUM = 16;

    private List<SkillDef> skills = List.of();
    private int left;
    private int top;
    private int panelH;
    private int visibleRows = 1;
    private int scroll;
    private String lastHover = "";

    public SkillScreen() {
        super(Component.translatable("screen.pixelclasses.skills_title"));
        UiSounds.play("ui_open");
    }

    @Override
    protected void init() {
        ClassDef cls = ClientData.currentClass();
        skills = cls == null ? List.of() : Definitions.CLIENT.skillsOf(cls.id());
        int space = height - 32 - HEADER_H - 30;
        visibleRows = Math.max(3, Math.min(Math.max(1, skills.size()), (space + ROW_GAP) / (ROW_H + ROW_GAP)));
        panelH = HEADER_H + visibleRows * (ROW_H + ROW_GAP) + 28;
        left = (width - PANEL_W) / 2;
        top = Math.max(16, (height - panelH) / 2);
        scroll = Math.max(0, Math.min(scroll, skills.size() - visibleRows));
    }

    private int slotX(int i) {
        int total = 4 * 24 + 3 * 34;
        return left + (PANEL_W - total) / 2 + i * (24 + 34);
    }

    private int slotY() {
        return top + 48;
    }

    private int rowY(int vi) {
        return top + HEADER_H + vi * (ROW_H + ROW_GAP);
    }

    private int numX(int rx, int i) {
        return rx + ROW_W - 4 * (NUM + 2) - 2 + i * (NUM + 2);
    }

    private int changeX() {
        return left + PANEL_W - 14 - 92;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        UiDraw.panel(g, left, top, PANEL_W, panelH);
        UiDraw.banner(g, font, title, left + PANEL_W / 2, top - 12);

        ClassDef cls = ClientData.currentClass();
        int accent = cls == null ? 0xFFAAAAAA : cls.color();

        // header: class + change button
        if (cls != null) {
            UiDraw.icon(g, cls.icon(), left + 14, top + 14, 24);
            g.drawString(font, ClientData.text(cls.name()), left + 44, top + 16, accent, true);
            String manaLine = ClientData.text(cls.manaName()) + " " + (int) cls.maxMana() + "  (+" + String.format(Locale.ROOT, "%.1f", cls.manaRegen()) + "/s)";
            UiDraw.scaled(g, font, manaLine, left + 44, top + 28, 0.75F, 0xFFB8BED8, false);
        } else {
            g.drawString(font, Component.translatable("screen.pixelclasses.no_class"), left + 14, top + 22, 0xFFFF9999, true);
        }
        boolean overChange = UiDraw.inside(mouseX, mouseY, changeX(), top + 16, 92, 18);
        UiDraw.button(g, font, Component.translatable(cls == null ? "screen.pixelclasses.choose_class" : "screen.pixelclasses.change_class"),
                changeX(), top + 16, 92, 18, overChange, true);

        // equipped slots
        UiDraw.scaledCentered(g, font, Component.translatable("screen.pixelclasses.equipped").getString(), left + PANEL_W / 2, top + 40, 0.8F, 0xFFFFE9A8, true);
        SkillDef hoveredSkill = null;
        String hoverKey = "";
        for (int i = 0; i < 4; i++) {
            int sx = slotX(i);
            g.blit(UiDraw.SLOT, sx, slotY(), 0, 0, 24, 24, 24, 24);
            String id = ClientData.equipped[i];
            SkillDef skill = id.isEmpty() ? null : Definitions.CLIENT.skillDef(ResourceLocation.tryParse(id));
            boolean over = UiDraw.inside(mouseX, mouseY, sx, slotY(), 24, 24);
            if (skill != null) {
                UiDraw.icon(g, skill.icon(), sx + 4, slotY() + 4, 16);
                if (over) {
                    hoveredSkill = skill;
                    hoverKey = "slot" + i;
                    g.fill(sx + 4, slotY() + 4, sx + 20, slotY() + 20, 0x40FF4040);
                }
            }
            UiDraw.scaledCentered(g, font, "[" + KeyBindings.CAST[i].getTranslatedKeyMessage().getString() + "]", sx + 12, slotY() + 26, 0.75F, 0xFFC8CCE8, true);
        }

        // skill rows
        for (int vi = 0; vi < visibleRows; vi++) {
            int idx = scroll + vi;
            if (idx >= skills.size()) {
                break;
            }
            SkillDef skill = skills.get(idx);
            String sid = skill.id().toString();
            int rx = left + 14;
            int ry = rowY(vi);
            boolean overRow = UiDraw.inside(mouseX, mouseY, rx, ry, ROW_W, ROW_H);
            UiDraw.row(g, rx, ry, ROW_W, ROW_H, overRow);
            UiDraw.icon(g, skill.icon(), rx + 4, ry + 3, 16);
            g.drawString(font, ClientData.text(skill.name()), rx + 24, ry + 3, 0xFFFFFFFF, true);
            String cost = String.format(Locale.ROOT, "CD %.1fs   %s %d", skill.cooldownTicks() / 20.0, cls == null ? "MP" : ClientData.text(cls.manaName()), (int) Math.round(skill.manaCost()));
            UiDraw.scaled(g, font, cost, rx + 24, ry + 13, 0.7F, 0xFF9CA4C8, false);
            if (overRow) {
                hoveredSkill = skill;
                hoverKey = "row" + idx;
            }
            for (int i = 0; i < 4; i++) {
                int nx = numX(rx, i);
                int ny = ry + (ROW_H - NUM) / 2;
                boolean here = sid.equals(ClientData.equipped[i]);
                boolean overNum = UiDraw.inside(mouseX, mouseY, nx, ny, NUM, NUM);
                UiDraw.row(g, nx, ny, NUM, NUM, overNum);
                if (here) {
                    g.fill(nx + 2, ny + 2, nx + NUM - 2, ny + NUM - 2, 0x66FFC040);
                }
                g.drawCenteredString(font, String.valueOf(i + 1), nx + NUM / 2, ny + 4, here ? 0xFFFFE070 : 0xFFFFFFFF);
            }
        }
        if (skills.size() > visibleRows) {
            if (scroll > 0) {
                g.drawCenteredString(font, "^", left + PANEL_W / 2, top + HEADER_H - 10, 0xFFFFE9A8);
            }
            if (scroll < skills.size() - visibleRows) {
                g.drawCenteredString(font, "v", left + PANEL_W / 2, top + panelH - 40, 0xFFFFE9A8);
            }
        }

        UiDraw.scaledCentered(g, font, Component.translatable("screen.pixelclasses.hint").getString(), left + PANEL_W / 2, top + panelH - 21, 0.75F, 0xFFE8D9A0, true);
        UiDraw.scaledCentered(g, font, Component.translatable("screen.pixelclasses.close_hint", KeyBindings.OPEN.getTranslatedKeyMessage()).getString(),
                left + PANEL_W / 2, top + panelH - 12, 0.7F, 0xFF8890B0, false);

        if (!hoverKey.equals(lastHover) && !hoverKey.isEmpty()) {
            UiSounds.play("ui_hover", 1.0F, 0.6F);
        }
        lastHover = hoverKey;

        if (hoveredSkill != null) {
            g.renderComponentTooltip(font, tooltip(hoveredSkill, accent), mouseX, mouseY);
        }
    }

    private List<Component> tooltip(SkillDef skill, int accent) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(ClientData.text(skill.name())).withStyle(Style.EMPTY.withColor(accent & 0xFFFFFF).withBold(true)));
        for (String line : UiDraw.wrap(font, ClientData.text(skill.description()), 190)) {
            lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.empty());
        double cooldown = skill.cooldownTicks() / 20.0 * ClassConfig.get(ClassConfig.COOLDOWN_MULT);
        lines.add(Component.translatable("screen.pixelclasses.tt_cooldown", String.format(Locale.ROOT, "%.1f", cooldown)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("screen.pixelclasses.tt_cost", (int) Math.round(skill.manaCost() * ClassConfig.get(ClassConfig.MANA_COST_MULT))).withStyle(ChatFormatting.BLUE));
        if (!skill.scalingStat().isEmpty() && skill.scalingPerPoint() > 0) {
            lines.add(Component.translatable("screen.pixelclasses.tt_scales", skill.scalingStat(),
                    String.format(Locale.ROOT, "%.1f%%", skill.scalingPerPoint() * 100.0)).withStyle(ChatFormatting.DARK_GREEN));
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (UiDraw.inside(mouseX, mouseY, changeX(), top + 16, 92, 18)) {
                UiSounds.play("ui_click");
                Minecraft.getInstance().setScreen(new ClassSelectScreen());
                return true;
            }
            for (int i = 0; i < 4; i++) {
                if (UiDraw.inside(mouseX, mouseY, slotX(i), slotY(), 24, 24) && !ClientData.equipped[i].isEmpty()) {
                    equip(i, "");
                    return true;
                }
            }
            for (int vi = 0; vi < visibleRows; vi++) {
                int idx = scroll + vi;
                if (idx >= skills.size()) {
                    break;
                }
                int rx = left + 14;
                int ry = rowY(vi);
                for (int i = 0; i < 4; i++) {
                    if (UiDraw.inside(mouseX, mouseY, numX(rx, i), ry + (ROW_H - NUM) / 2, NUM, NUM)) {
                        String sid = skills.get(idx).id().toString();
                        equip(i, sid.equals(ClientData.equipped[i]) ? "" : sid);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void equip(int slot, String skillId) {
        ModNetwork.sendToServer(new C2S.Equip(slot, skillId));
        UiSounds.play(skillId.isEmpty() ? "ui_click" : "ui_equip");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (skills.size() > visibleRows) {
            scroll = Math.max(0, Math.min(skills.size() - visibleRows, scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.OPEN.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        UiSounds.play("ui_close");
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
