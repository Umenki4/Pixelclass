package com.pixelclasses.skill;

import com.pixelclasses.config.ClassConfig;
import com.pixelclasses.data.ClassDef;
import com.pixelclasses.data.Definitions;
import com.pixelclasses.data.SkillDef;
import com.pixelclasses.integration.PixelStatsBridge;
import com.pixelclasses.network.ModNetwork;
import com.pixelclasses.player.ClassApi;
import com.pixelclasses.player.ClassData;
import com.pixelclasses.player.ClassEffects;
import com.pixelclasses.skill.action.SkillAction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/** Validates and starts a skill (cooldown, mana, equipped?) on the server. */
public final class SkillCaster {
    private SkillCaster() {}

    /** Player pressed the key of an equipped slot (0-3). */
    public static void castSlot(ServerPlayer player, int slot) {
        ClassDef cls = ClassEffects.current(player);
        if (cls == null || slot < 0 || slot >= ClassData.SLOTS) {
            return;
        }
        String skillId = ClassApi.get(player).loadout(cls.id().toString())[slot];
        if (skillId.isEmpty()) {
            return;
        }
        SkillDef skill = Definitions.SERVER.skillDef(ResourceLocation.tryParse(skillId));
        if (skill == null || !skill.classId().equals(cls.id())) {
            return;
        }
        cast(player, skill, false);
    }

    /** ignoreCosts = true is used by the /pixelclasses cast test command. */
    public static boolean cast(ServerPlayer player, SkillDef skill, boolean ignoreCosts) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        ClassData data = ClassApi.get(player);
        String id = skill.id().toString();
        long now = player.level().getGameTime();
        int cooldown = Math.max(1, (int) Math.round(skill.cooldownTicks() * ClassConfig.get(ClassConfig.COOLDOWN_MULT)));

        if (!ignoreCosts) {
            if (now < data.cooldownEnd(id)) {
                return false;
            }
            float cost = (float) (skill.manaCost() * ClassConfig.get(ClassConfig.MANA_COST_MULT));
            if (data.getMana() < cost) {
                player.displayClientMessage(Component.translatable("message.pixelclasses.no_mana"), true);
                return false;
            }
            data.setMana(data.getMana() - cost);
            data.setCooldown(id, now + cooldown);
            ModNetwork.sendCooldown(player, id, cooldown);
            ModNetwork.sendMana(player);
        }

        double power = (1.0 + PixelStatsBridge.points(player, skill.scalingStat()) * skill.scalingPerPoint())
                * ClassConfig.get(ClassConfig.DAMAGE_MULT);
        SkillContext context = SkillContext.create(player, power);
        player.swing(InteractionHand.MAIN_HAND, true);
        for (SkillAction action : skill.actions()) {
            action.schedule(context);
        }
        return true;
    }
}
