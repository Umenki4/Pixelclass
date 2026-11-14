package com.pixelclasses.client;

import com.pixelclasses.config.ClassConfig;
import com.pixelclasses.data.ClassDef;
import com.pixelclasses.data.Definitions;
import com.pixelclasses.data.SkillDef;
import com.pixelclasses.network.C2S;
import com.pixelclasses.network.ModNetwork;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Origins-style "choose your class" window: one card per class (icon, description, skill list). */
public class ClassSelectScreen extends Screen {
    private static final int CARD_W = 92;
    private static final int CARD_H = 176;
    private static final int GAP = 6;
    private static final int BTN_W = 96;
    private static final int BTN_H = 18;

    private List<ClassDef> classes = List.of();
    private int selected = -1;
    private int scroll;
    private int visible = 1;
    private int left;
    private int top;
    private int panelW;
    private int panelH;
    private int lastHover = -1;

    public ClassSelectScreen() {
        super(Component.translatable("screen.pixelclasses.select_title"));
        UiSounds.play("ui_open");
    }

    @Override
    protected void init() {
        classes = Definitions.CLIENT.classes();
        visible = Math.max(1, Math.min(Math.max(1, classes.size()), (width - 40) / (CARD_W + GAP)));
        panelW = Math.max(visible * (CARD_W + GAP) - GAP + 32, 2 * BTN_W + 48);
        panelH = CARD_H + 78;
        left = (width - panelW) / 2;
        top = Math.max(16, (height - panelH) / 2);
        scroll = Math.max(0, Math.min(scroll, classes.size() - visible));
        if (selected < 0) {
            for (int i = 0; i < classes.size(); i++) {
                if (classes.get(i).id().toString().equals(ClientData.classId)) {
                    selected = i;
                }
            }
        }
    }

    private int cardsLeft() {
        int cardsW = visible * (CARD_W + GAP) - GAP;
        return left + (panelW - cardsW) / 2;
    }

    private boolean locked() {
        Minecraft mc = Minecraft.getInstance();
        return ClientData.currentClass() != null && !ClassConfig.get(ClassConfig.ALLOW_CLASS_CHANGE)
                && (mc.player == null || !mc.player.hasPermissions(2));
    }

    private int confirmX() {
        return left + panelW / 2 - BTN_W - 6;
    }

    private int cancelX() {
        return left + panelW / 2 + 6;
    }

    private int buttonsY() {
        return top + panelH - 26;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        UiDraw.panel(g, left, top, panelW, panelH);
        UiDraw.banner(g, font, title, left + panelW / 2, top - 12);

        if (classes.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.pixelclasses.none_loaded"), left + panelW / 2, top + panelH / 2, 0xFFFF8888);
        }

        int hover = -1;
        for (int vi = 0; vi < visible; vi++) {
            int idx = scroll + vi;
            if (idx >= classes.size()) {
                break;
            }
            ClassDef cls = classes.get(idx);
            int x = cardsLeft() + vi * (CARD_W + GAP);
            int y = top + 24;
            boolean over = UiDraw.inside(mouseX, mouseY, x, y, CARD_W, CARD_H);
            if (over) {
                hover = idx;
            }
            UiDraw.row(g, x, y, CARD_W, CARD_H, over || idx == selected);
            if (idx == selected) {
                int c = cls.color();
                g.fill(x, y, x + CARD_W, y + 1, c);
                g.fill(x, y + CARD_H - 1, x + CARD_W, y + CARD_H, c);
                g.fill(x, y, x + 1, y + CARD_H, c);
                g.fill(x + CARD_W - 1, y, x + CARD_W, y + CARD_H, c);
            }

            UiDraw.icon(g, cls.icon(), x + (CARD_W - 32) / 2, y + 8, 32);
            g.drawCenteredString(font, ClientData.text(cls.name()), x + CARD_W / 2, y + 44, cls.color());
            if (cls.id().toString().equals(ClientData.classId)) {
                UiDraw.scaledCentered(g, font, Component.translatable("screen.pixelclasses.current").getString(), x + CARD_W / 2, y + 55, 0.7F, 0xFFAAFFAA, false);
            }

            int ty = y + 66;
            for (String line : UiDraw.wrap(font, ClientData.text(cls.description()), (int) ((CARD_W - 12) / 0.75F))) {
                if (ty > y + 112) {
                    break;
                }
                UiDraw.scaled(g, font, line, x + 6, ty, 0.75F, 0xFFC4C8E0, false);
                ty += 7;
            }

            int sy = y + 114;
            UiDraw.scaled(g, font, Component.translatable("screen.pixelclasses.skills_header").getString(), x + 6, sy, 0.75F, 0xFFFFE9A8, false);
            sy += 9;
            for (SkillDef skill : Definitions.CLIENT.skillsOf(cls.id())) {
                if (sy > y + CARD_H - 8) {
                    break;
                }
                UiDraw.icon(g, skill.icon(), x + 6, sy - 1, 8);
                UiDraw.scaled(g, font, ClientData.text(skill.name()), x + 17, sy, 0.7F, 0xFFE0E4F8, false);
                sy += 9;
            }
        }
        if (hover != lastHover && hover >= 0) {
            UiSounds.play("ui_hover", 1.0F, 0.6F);
        }
        lastHover = hover;

        if (classes.size() > visible) {
            if (scroll > 0) {
                g.drawString(font, "<", left + 8, top + 24 + CARD_H / 2, 0xFFFFE9A8, true);
            }
            if (scroll < classes.size() - visible) {
                g.drawString(font, ">", left + panelW - 14, top + 24 + CARD_H / 2, 0xFFFFE9A8, true);
            }
        }

        boolean canConfirm = selected >= 0 && !locked();
        if (locked()) {
            g.drawCenteredString(font, Component.translatable("screen.pixelclasses.locked"), left + panelW / 2, buttonsY() - 12, 0xFFFF8888);
        }
        UiDraw.button(g, font, Component.translatable("screen.pixelclasses.confirm"), confirmX(), buttonsY(), BTN_W, BTN_H,
                UiDraw.inside(mouseX, mouseY, confirmX(), buttonsY(), BTN_W, BTN_H), canConfirm);
        UiDraw.button(g, font, Component.translatable("screen.pixelclasses.close"), cancelX(), buttonsY(), BTN_W, BTN_H,
                UiDraw.inside(mouseX, mouseY, cancelX(), buttonsY(), BTN_W, BTN_H), true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int vi = 0; vi < visible; vi++) {
                int idx = scroll + vi;
                if (idx < classes.size() && UiDraw.inside(mouseX, mouseY, cardsLeft() + vi * (CARD_W + GAP), top + 24, CARD_W, CARD_H)) {
                    selected = idx;
                    UiSounds.play("ui_select");
                    return true;
                }
            }
            if (UiDraw.inside(mouseX, mouseY, confirmX(), buttonsY(), BTN_W, BTN_H)) {
                if (selected >= 0 && !locked()) {
                    ModNetwork.sendToServer(new C2S.SelectClass(classes.get(selected).id().toString()));
                    UiSounds.play("ui_equip");
                    onClose();
                } else {
                    UiSounds.play("ui_deny");
                }
                return true;
            }
            if (UiDraw.inside(mouseX, mouseY, cancelX(), buttonsY(), BTN_W, BTN_H)) {
                UiSounds.play("ui_click");
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (classes.size() > visible) {
            scroll = Math.max(0, Math.min(classes.size() - visible, scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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
