package com.pixelclasses.client;

import com.pixelclasses.vfx.VfxSpec;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Builds the particle shapes on the client from a small description (rings, spirals, beams, bursts...). */
public final class VfxRenderer {
    private VfxRenderer() {}

    public static void play(Vec3 pos, Vec3 dir, List<VfxSpec> specs) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (VfxSpec spec : specs) {
            ParticleOptions options = spec.options();
            if (options != null) {
                spawn(level, options, spec, pos, dir);
            }
        }
    }

    private static void spawn(ClientLevel level, ParticleOptions o, VfxSpec s, Vec3 pos, Vec3 dirIn) {
        RandomSource r = level.random;
        double px = pos.x;
        double py = pos.y + s.yOffset;
        double pz = pos.z;
        Vec3 d = dirIn.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dirIn.normalize();
        Vec3 flat = new Vec3(d.x, 0, d.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
        int n = s.count;
        double sp = s.speed;

        switch (s.shape) {
            case "ring" -> {
                for (int i = 0; i < n; i++) {
                    double a = Math.PI * 2.0 * i / n;
                    double cx = Math.cos(a);
                    double cz = Math.sin(a);
                    level.addParticle(o, px + cx * s.radius, py, pz + cz * s.radius, cx * sp, 0, cz * sp);
                }
            }
            case "sphere" -> {
                for (int i = 0; i < n; i++) {
                    double y = 1.0 - 2.0 * (i + 0.5) / n;
                    double rr = Math.sqrt(Math.max(0.0, 1.0 - y * y));
                    double phi = i * 2.399963;
                    double x = Math.cos(phi) * rr;
                    double z = Math.sin(phi) * rr;
                    level.addParticle(o, px + x * s.radius, py + y * s.radius, pz + z * s.radius, x * sp, y * sp, z * sp);
                }
            }
            case "disc" -> {
                for (int i = 0; i < n; i++) {
                    double a = r.nextDouble() * Math.PI * 2.0;
                    double rr = s.radius * Math.sqrt(r.nextDouble());
                    level.addParticle(o, px + Math.cos(a) * rr, py, pz + Math.sin(a) * rr,
                            (r.nextDouble() - 0.5) * sp, r.nextDouble() * sp, (r.nextDouble() - 0.5) * sp);
                }
            }
            case "line" -> {
                for (int i = 0; i < n; i++) {
                    double t = (i + 0.5) / n * s.length;
                    level.addParticle(o, px + d.x * t + (r.nextDouble() - 0.5) * 2 * s.sx, py + d.y * t + (r.nextDouble() - 0.5) * 2 * s.sy,
                            pz + d.z * t + (r.nextDouble() - 0.5) * 2 * s.sz,
                            (r.nextDouble() - 0.5) * sp, (r.nextDouble() - 0.5) * sp, (r.nextDouble() - 0.5) * sp);
                }
            }
            case "spiral" -> {
                for (int i = 0; i < n; i++) {
                    double f = (i + 0.5) / n;
                    double a = s.turns * Math.PI * 2.0 * f;
                    level.addParticle(o, px + Math.cos(a) * s.radius, py + f * s.height, pz + Math.sin(a) * s.radius, 0, sp, 0);
                }
            }
            case "column" -> {
                for (int i = 0; i < n; i++) {
                    double a = r.nextDouble() * Math.PI * 2.0;
                    double rr = s.radius * Math.sqrt(r.nextDouble());
                    level.addParticle(o, px + Math.cos(a) * rr, py + r.nextDouble() * s.height, pz + Math.sin(a) * rr, 0, sp, 0);
                }
            }
            case "burst" -> {
                for (int i = 0; i < n; i++) {
                    double vx = r.nextGaussian();
                    double vy = r.nextGaussian();
                    double vz = r.nextGaussian();
                    double len = Math.max(1.0E-4, Math.sqrt(vx * vx + vy * vy + vz * vz));
                    level.addParticle(o, px, py, pz, vx / len * sp, vy / len * sp, vz / len * sp);
                }
            }
            case "arc" -> {
                double half = Math.toRadians(s.angle) / 2.0;
                double base = Math.atan2(flat.z, flat.x);
                for (int i = 0; i < n; i++) {
                    double a = base - half + 2.0 * half * (i + 0.5) / n;
                    double cx = Math.cos(a);
                    double cz = Math.sin(a);
                    level.addParticle(o, px + cx * s.radius, py + 1.0, pz + cz * s.radius, cx * sp, 0, cz * sp);
                }
            }
            case "slash" -> {
                // a crisp crescent streak facing the viewer; "roll" tilts it (0 = smile, 90 = vertical, 45/-45 = diagonal)
                Vec3 right = d.cross(new Vec3(0, 1, 0));
                right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
                Vec3 up = right.cross(d).normalize();
                double roll = Math.toRadians(s.roll);
                Vec3 ax = right.scale(Math.cos(roll)).add(up.scale(Math.sin(roll)));
                Vec3 ay = up.scale(Math.cos(roll)).subtract(right.scale(Math.sin(roll)));
                Vec3 center = new Vec3(px, py, pz).add(d.scale(s.length));
                double half = Math.toRadians(s.angle) / 2.0;
                for (int i = 0; i < n; i++) {
                    double a = -half + 2.0 * half * (i + 0.5) / n;
                    Vec3 p = center.add(ax.scale(Math.sin(a) * s.radius)).add(ay.scale((1.0 - Math.cos(a)) * s.radius * 0.6));
                    level.addParticle(o, p.x, p.y, p.z, 0, 0, 0);
                }
            }
            case "cone" -> {
                Vec3 side = d.cross(new Vec3(0, 1, 0));
                side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
                Vec3 up = side.cross(d).normalize();
                double tan = Math.tan(Math.toRadians(s.angle) / 2.0);
                for (int i = 0; i < n; i++) {
                    double t = r.nextDouble() * s.length;
                    double a = r.nextDouble() * Math.PI * 2.0;
                    double rr = t * tan * Math.sqrt(r.nextDouble());
                    Vec3 p = new Vec3(px, py, pz).add(d.scale(t)).add(side.scale(Math.cos(a) * rr)).add(up.scale(Math.sin(a) * rr));
                    level.addParticle(o, p.x, p.y, p.z, d.x * sp, d.y * sp, d.z * sp);
                }
            }
            default -> {
                for (int i = 0; i < n; i++) {
                    level.addParticle(o, px + (r.nextDouble() - 0.5) * 2 * s.sx, py + (r.nextDouble() - 0.5) * 2 * s.sy,
                            pz + (r.nextDouble() - 0.5) * 2 * s.sz,
                            (r.nextDouble() - 0.5) * sp * 2, (r.nextDouble() - 0.5) * sp * 2, (r.nextDouble() - 0.5) * sp * 2);
                }
            }
        }
    }
}
