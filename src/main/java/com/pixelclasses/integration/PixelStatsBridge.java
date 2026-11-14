package com.pixelclasses.integration;

import java.lang.reflect.Method;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/**
 * Optional link to the "Pixel Stats" mod (DEX/STR/CON/INT/LUC points), done by reflection so nothing breaks when it is absent.
 * Returns 0 if the mod is missing or anything goes wrong.
 */
public final class PixelStatsBridge {
    private static boolean failed;
    private static Method getStats;
    private static Method getPoints;
    private static Class<?> typeClass;

    private PixelStatsBridge() {}

    public static int points(Player player, String stat) {
        if (failed || stat == null || stat.isEmpty() || !ModList.get().isLoaded("pixelstats")) {
            return 0;
        }
        try {
            if (getStats == null) {
                Class<?> api = Class.forName("com.pixelstats.stats.StatsApi");
                typeClass = Class.forName("com.pixelstats.stats.StatType");
                Class<?> playerStats = Class.forName("com.pixelstats.stats.PlayerStats");
                getStats = api.getMethod("get", Player.class);
                getPoints = playerStats.getMethod("get", typeClass);
            }
            Object type = null;
            for (Object constant : typeClass.getEnumConstants()) {
                if (((Enum<?>) constant).name().equalsIgnoreCase(stat)) {
                    type = constant;
                }
            }
            if (type == null) {
                return 0;
            }
            Object stats = getStats.invoke(null, player);
            return (Integer) getPoints.invoke(stats, type);
        } catch (Throwable t) {
            failed = true;
            return 0;
        }
    }
}
