package com.pixelclasses.registry;

import com.pixelclasses.PixelClassesMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Custom particle types (textures in assets/pixelclasses/textures/particle, sprite lists in assets/pixelclasses/particles).
 * Use them in skill JSON as "pixelclasses:slash", "pixelclasses:swirl", "pixelclasses:sparkle", "pixelclasses:shard", "pixelclasses:glow".
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, PixelClassesMod.MODID);

    public static final RegistryObject<SimpleParticleType> SLASH = PARTICLES.register("slash", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SWIRL = PARTICLES.register("swirl", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SPARKLE = PARTICLES.register("sparkle", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SHARD = PARTICLES.register("shard", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BURST = PARTICLES.register("burst", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> GLOW = PARTICLES.register("glow", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
