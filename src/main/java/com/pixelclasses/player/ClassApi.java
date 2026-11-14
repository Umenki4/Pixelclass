package com.pixelclasses.player;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/** Reads a player's class data. Never returns null. */
public final class ClassApi {
    public static final Capability<ClassData> CAP = CapabilityManager.get(new CapabilityToken<ClassData>() {});

    private ClassApi() {}

    public static ClassData get(Player player) {
        return player.getCapability(CAP).resolve().orElseGet(ClassData::new);
    }
}
