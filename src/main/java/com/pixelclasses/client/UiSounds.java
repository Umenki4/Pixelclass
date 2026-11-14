package com.pixelclasses.client;

import com.pixelclasses.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

/** Plays UI sounds by name (see ModSounds.NAMES). */
public final class UiSounds {
    private UiSounds() {}

    public static void play(String name) {
        play(name, 1.0F, 1.0F);
    }

    public static void play(String name, float pitch, float volume) {
        RegistryObject<SoundEvent> sound = ModSounds.get(name);
        if (sound != null && sound.isPresent()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound.get(), pitch, volume));
        }
    }
}
