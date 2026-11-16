package com.pixelclasses.client.fx;

/**
 * Extra settings (size multiplier, roll angle, tint) for the NEXT custom particle(s).
 * Vanilla's addParticle() has no room for them, so VfxRenderer sets them here right before spawning and resets afterwards.
 * Everything runs on the client thread, one effect after another, so this is safe.
 */
public final class FxParams {
    public static float sizeMul = 1.0F;
    public static float rollDeg = 0.0F;
    public static float r = 1.0F;
    public static float g = 1.0F;
    public static float b = 1.0F;

    private FxParams() {}

    public static void set(float size, float roll, int argb) {
        sizeMul = size;
        rollDeg = roll;
        r = ((argb >> 16) & 255) / 255.0F;
        g = ((argb >> 8) & 255) / 255.0F;
        b = (argb & 255) / 255.0F;
    }

    public static void reset() {
        set(1.0F, 0.0F, 0xFFFFFFFF);
    }
}
