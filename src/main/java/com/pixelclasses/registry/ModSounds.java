package com.pixelclasses.registry;

import com.pixelclasses.PixelClassesMod;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registers every sound in assets/pixelclasses/sounds.json (names must match the generated list). */
public final class ModSounds {
    public static final String[] NAMES = {
            "ui_open", "ui_close", "ui_hover", "ui_click", "ui_select", "ui_equip", "ui_deny",
            "cast_fire", "boom", "cast_ice", "ice_break", "cast_arcane", "zap", "blink",
            "arrow_shot", "arrow_hit", "whoosh", "slam", "warcry", "shield", "heal_chime", "charge", "slash", "buff",
            "vanish", "shadow_step", "dagger_slash", "dagger_crit", "knife_throw"
    };

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, PixelClassesMod.MODID);
    private static final Map<String, RegistryObject<SoundEvent>> BY_NAME = new HashMap<>();

    static {
        for (String name : NAMES) {
            BY_NAME.put(name, SOUNDS.register(name,
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(PixelClassesMod.MODID, name))));
        }
    }

    private ModSounds() {}

    public static RegistryObject<SoundEvent> get(String name) {
        return BY_NAME.get(name);
    }
}
