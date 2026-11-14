package com.pixelclasses.skill;

import com.mojang.logging.LogUtils;
import com.pixelclasses.config.ClassConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/**
 * Tiny scheduler: runs delayed / repeated skill steps on the server thread.
 * Every task is wrapped in try/catch and limited per player, so a bad skill can never crash or lag the server.
 */
public final class SkillRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final class Task {
        final int runAt;
        final UUID owner;
        final Runnable action;

        Task(int runAt, UUID owner, Runnable action) {
            this.runAt = runAt;
            this.owner = owner;
            this.action = action;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> ADDING = new ArrayList<>();
    private static final Map<UUID, Integer> COUNTS = new HashMap<>();
    private static int now;

    private SkillRuntime() {}

    /** Run {@code action} after {@code ticks} server ticks. Silently dropped if the player has too many pending effects. */
    public static void later(UUID owner, int ticks, Runnable action) {
        int count = COUNTS.getOrDefault(owner, 0);
        if (count >= ClassConfig.get(ClassConfig.MAX_TASKS_PER_PLAYER)) {
            return;
        }
        COUNTS.put(owner, count + 1);
        ADDING.add(new Task(now + Math.max(1, ticks), owner, action));
    }

    public static void tick(MinecraftServer server) {
        now = server.getTickCount();
        if (!ADDING.isEmpty()) {
            TASKS.addAll(ADDING);
            ADDING.clear();
        }
        if (TASKS.isEmpty()) {
            return;
        }
        List<Task> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.runAt <= now) {
                due.add(t);
                it.remove();
            }
        }
        for (Task t : due) {
            COUNTS.merge(t.owner, -1, Integer::sum);
            try {
                t.action.run();
            } catch (Throwable ex) {
                LOGGER.error("[PixelClasses] A skill step failed and was skipped", ex);
            }
        }
    }

    public static void clear() {
        TASKS.clear();
        ADDING.clear();
        COUNTS.clear();
    }

    /** Stops everything scheduled by one player (used when they log out). */
    public static void cancel(UUID owner) {
        TASKS.removeIf(t -> t.owner.equals(owner));
        ADDING.removeIf(t -> t.owner.equals(owner));
        COUNTS.remove(owner);
    }
}
