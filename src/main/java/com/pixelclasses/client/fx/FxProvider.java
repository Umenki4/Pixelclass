package com.pixelclasses.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/** Creates FxParticle instances; one provider per particle type. */
public class FxProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;
    private final FxParticle.Kind kind;

    public FxProvider(SpriteSet sprites, FxParticle.Kind kind) {
        this.sprites = sprites;
        this.kind = kind;
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                   double dx, double dy, double dz) {
        return new FxParticle(level, x, y, z, dx, dy, dz, sprites, kind);
    }
}
