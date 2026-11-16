package com.pixelclasses.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

/** One glowing sprite particle (slash streak, swirl ring, sparkle, shard, glow dot). Always full-bright and translucent. */
public class FxParticle extends TextureSheetParticle {
    public enum Kind {
        SLASH(1.2F, 7, 1),
        SWIRL(1.4F, 12, 2),
        SPARKLE(0.18F, 14, 6),
        SHARD(0.12F, 18, 8),
        GLOW(0.25F, 10, 3);

        final float size;
        final int life;
        final int jitter;

        Kind(float size, int life, int jitter) {
            this.size = size;
            this.life = life;
            this.jitter = jitter;
        }
    }

    private final Kind kind;
    private final float baseSize;
    private final float spin;

    protected FxParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, Kind kind) {
        super(level, x, y, z, vx, vy, vz);
        this.kind = kind;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.9F;
        this.baseSize = kind.size * FxParams.sizeMul;
        this.quadSize = this.baseSize;
        this.lifetime = kind.life + (kind.jitter > 0 ? this.random.nextInt(kind.jitter + 1) : 0);
        boolean oriented = kind == Kind.SLASH || kind == Kind.SWIRL;
        this.roll = oriented ? (float) Math.toRadians(FxParams.rollDeg) : this.random.nextFloat() * (float) Math.PI;
        this.oRoll = this.roll;
        this.spin = kind == Kind.SHARD ? (this.random.nextFloat() - 0.5F) * 0.3F : 0.0F;
        this.setColor(FxParams.r, FxParams.g, FxParams.b);
        this.pickSprite(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 15728880;   // full bright: the effects glow even in the dark
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        this.roll += this.spin;
        super.tick();
        float t = Math.min(1.0F, (float) this.age / (float) this.lifetime);
        this.alpha = switch (kind) {
            case SLASH -> 1.0F - t * t;
            case SWIRL -> 1.0F - t;
            case SPARKLE -> Mth.sin(t * (float) Math.PI);
            case SHARD -> 1.0F - t;
            case GLOW -> 0.85F * (1.0F - t);
        };
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float t = Mth.clamp((this.age + partialTicks) / (float) this.lifetime, 0.0F, 1.0F);
        float f = switch (kind) {
            case SLASH -> 0.85F + 0.3F * t;
            case SWIRL -> 0.5F + 0.9F * t;
            case SPARKLE -> Mth.sin(t * (float) Math.PI);
            case SHARD, GLOW -> 1.0F - 0.6F * t;
        };
        return this.baseSize * Math.max(0.05F, f);
    }
}
