package com.pixelclasses.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Reads data/<namespace>/pixelclasses/classes/*.json and data/<namespace>/pixelclasses/skills/*.json
 * from the mod jar AND from datapacks (datapacks override / add - this is how addons work). Runs again on /reload.
 */
public class DefinitionManager extends SimpleJsonResourceReloadListener {
    public DefinitionManager() {
        super(new Gson(), "pixelclasses");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> classJson = new HashMap<>();
        Map<ResourceLocation, JsonElement> skillJson = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            ResourceLocation key = e.getKey();
            String path = key.getPath();
            if (path.startsWith("classes/")) {
                classJson.put(new ResourceLocation(key.getNamespace(), path.substring("classes/".length())), e.getValue());
            } else if (path.startsWith("skills/")) {
                skillJson.put(new ResourceLocation(key.getNamespace(), path.substring("skills/".length())), e.getValue());
            }
        }
        Definitions.SERVER.load(classJson, skillJson);
    }
}
