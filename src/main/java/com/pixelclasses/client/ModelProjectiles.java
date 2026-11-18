package com.pixelclasses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.pixelclasses.PixelClassesMod;
import com.pixelclasses.registry.ModItems;
import com.pixelclasses.vfx.VfxSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * Draws flying 3D item models (the void dagger) client-side. No entity is involved: the server only sends position updates
 * (through VFX packets with a "model" shape) and the client keeps the model flying between them, spinning like the
 * GeckoLib "fly" animation (one full turn every 0.6 s).
 * VFX fields for shape "model": particle = model id, size = scale (1 = 2 blocks long), speed = blocks/tick to glide between
 * packets, angle = spin degrees per tick, roll = start angle, turns = how many ticks it stays visible without an update.
 */
public final class ModelProjectiles {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation DAGGER_MODEL = new ResourceLocation(PixelClassesMod.MODID, "item/void_dagger");

    private static final class Instance {
        ResourceLocation model;
        Vec3 pos;
        Vec3 dir;
        float speed;
        float scale;
        float spinPerTick;
        float spinStart;
        float life;
        int born;

        float spinAt(float age) {
            return spinStart + spinPerTick * age;
        }
    }

    private static final Map<Integer, Instance> FLIGHTS = new HashMap<>();
    private static final List<Instance> LOOSE = new ArrayList<>();
    private static int tick;
    private static boolean broken;

    private ModelProjectiles() {}

    public static void clientTick() {
        tick++;
        FLIGHTS.values().removeIf(i -> tick - i.born > i.life + 2);
        LOOSE.removeIf(i -> tick - i.born > i.life + 2);
    }

    public static void spawn(VfxSpec s, Vec3 pos, Vec3 dir, int flightId) {
        ResourceLocation model = ResourceLocation.tryParse(s.particle);
        if (model == null || broken) {
            return;
        }
        Instance i = new Instance();
        i.model = model;
        i.pos = pos;
        i.dir = dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize();
        i.speed = s.speed;
        i.scale = s.size;
        i.spinPerTick = s.angle;
        i.life = Math.max(1.0F, s.turns);
        i.born = tick;
        i.spinStart = s.roll;
        if (flightId != 0) {
            Instance old = FLIGHTS.get(flightId);
            if (old != null) {
                i.spinStart = old.spinAt(tick - old.born);   // keep the spin continuous from packet to packet
            }
            FLIGHTS.put(flightId, i);
        } else {
            LOOSE.add(i);
        }
    }

    public static void render(PoseStack pose, Vec3 camera, float partialTick) {
        if (broken || (FLIGHTS.isEmpty() && LOOSE.isEmpty())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        try {
            MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
            ItemStack stack = new ItemStack(ModItems.SHADOW_DAGGER.get());
            List<Instance> all = new ArrayList<>(FLIGHTS.values());
            all.addAll(LOOSE);
            for (Instance i : all) {
                BakedModel model = mc.getModelManager().getModel(i.model);
                if (model == null || model == mc.getModelManager().getMissingModel()) {
                    continue;
                }
                float age = (tick - i.born) + partialTick;
                Vec3 p = i.pos.add(i.dir.scale(i.speed * age));
                pose.pushPose();
                pose.translate(p.x - camera.x, p.y - camera.y, p.z - camera.z);
                pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), new Vector3f((float) i.dir.x, (float) i.dir.y, (float) i.dir.z)));
                pose.mulPose(Axis.YP.rotationDegrees(i.spinAt(age)));
                pose.scale(i.scale, i.scale, i.scale);
                pose.translate(-0.5, -0.625, -0.5);   // the dagger model's centre is (8, 10, 8) / 16
                mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, pose, buffer, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, model);
                pose.popPose();
            }
            buffer.endBatch();
        } catch (RuntimeException ex) {
            broken = true;   // never crash the game over a visual effect: switch the 3D daggers off and keep the particles
            LOGGER.error("[PixelClasses] 3D projectile rendering failed and was disabled", ex);
        }
    }
}
