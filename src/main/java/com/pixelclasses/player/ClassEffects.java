package com.pixelclasses.player;

import com.pixelclasses.data.AttributeSpec;
import com.pixelclasses.data.ClassDef;
import com.pixelclasses.data.Definitions;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

/** Applies the class stat changes (attribute modifiers) from the class JSON to the player. Safe to call any time. */
public final class ClassEffects {
    private static final String PREFIX = "pixelclasses:";

    private ClassEffects() {}

    /** The player's class definition, or null if they have none (or it no longer exists). */
    public static ClassDef current(ServerPlayer player) {
        String id = ClassApi.get(player).getClassId();
        return id.isEmpty() ? null : Definitions.SERVER.classDef(ResourceLocation.tryParse(id));
    }

    public static void apply(ServerPlayer player) {
        float oldMax = player.getMaxHealth();

        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            for (AttributeModifier m : new ArrayList<>(instance.getModifiers())) {
                if (m.getName().startsWith(PREFIX)) {
                    instance.removeModifier(m.getId());
                }
            }
        }

        ClassDef cls = current(player);
        ClassData data = ClassApi.get(player);
        if (cls != null) {
            int i = 0;
            for (AttributeSpec spec : cls.attributes()) {
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(spec.attribute());
                AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
                if (instance != null) {
                    String name = PREFIX + cls.id() + "#" + i;
                    UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
                    instance.addPermanentModifier(new AttributeModifier(uuid, name, spec.amount(), spec.operation()));
                }
                i++;
            }
            data.setMana(Math.min(data.getMana(), (float) cls.maxMana()));
        } else {
            data.setMana(0.0F);
        }

        float newMax = player.getMaxHealth();
        if (player.isAlive()) {
            if (newMax > oldMax) {
                player.heal(newMax - oldMax);
            } else if (player.getHealth() > newMax) {
                player.setHealth(newMax);
            }
        }
    }
}
