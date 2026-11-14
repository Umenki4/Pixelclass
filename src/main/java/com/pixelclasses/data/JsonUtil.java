package com.pixelclasses.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/** Small JSON helpers shared by the parsers. */
public final class JsonUtil {
    private JsonUtil() {}

    /** "#RRGGBB" (or "RRGGBB") -> 0xFFRRGGBB. */
    public static int color(JsonObject o, String key, int defaultRgb) {
        if (!o.has(key)) {
            return 0xFF000000 | defaultRgb;
        }
        String s = GsonHelper.getAsString(o, key).trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        try {
            return 0xFF000000 | (Integer.parseInt(s, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            throw new JsonSyntaxException("Bad colour '" + s + "' in '" + key + "'");
        }
    }

    public static ResourceLocation id(String s, String what) {
        ResourceLocation id = ResourceLocation.tryParse(s);
        if (id == null) {
            throw new JsonSyntaxException("Bad " + what + " id '" + s + "'");
        }
        return id;
    }

    public static JsonArray array(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) {
            return new JsonArray();
        }
        if (e.isJsonArray()) {
            return e.getAsJsonArray();
        }
        JsonArray single = new JsonArray();
        single.add(e);
        return single;
    }
}
