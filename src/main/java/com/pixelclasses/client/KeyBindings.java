package com.pixelclasses.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    private static final String CATEGORY = "key.categories.pixelclasses";

    /** Skill slots 1-4: Z X C V (change in Options > Controls > Pixel Classes). */
    public static final KeyMapping[] CAST = new KeyMapping[4];
    /** Opens the skill window: J. */
    public static final KeyMapping OPEN = new KeyMapping("key.pixelclasses.open", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);

    static {
        int[] keys = {GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V};
        for (int i = 0; i < CAST.length; i++) {
            CAST[i] = new KeyMapping("key.pixelclasses.cast" + (i + 1), KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM, keys[i], CATEGORY);
        }
    }

    private KeyBindings() {}
}
