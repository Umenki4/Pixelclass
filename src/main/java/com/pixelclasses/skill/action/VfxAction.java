package com.pixelclasses.skill.action;

import com.pixelclasses.skill.SkillContext;
import net.minecraft.server.level.ServerPlayer;

/** Only plays its "vfx" and "sound" (the common fields do all the work). */
public final class VfxAction extends SkillAction {
    @Override
    protected void execute(SkillContext ctx, ServerPlayer caster) {
        // nothing else to do
    }
}
