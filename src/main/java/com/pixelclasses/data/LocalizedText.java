package com.pixelclasses.data;

import com.google.gson.JsonElement;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** A text that can be a plain string or {"en": "...", "th": "..."} in the JSON files. */
public final class LocalizedText {
    private final Map<String, String> map = new LinkedHashMap<>();

    public static LocalizedText parse(JsonElement e, String fallback) {
        LocalizedText t = new LocalizedText();
        if (e != null && e.isJsonPrimitive()) {
            t.map.put("en", e.getAsString());
        } else if (e != null && e.isJsonObject()) {
            for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
                t.map.put(en.getKey().toLowerCase(Locale.ROOT), en.getValue().getAsString());
            }
        }
        if (t.map.isEmpty()) {
            t.map.put("en", fallback);
        }
        return t;
    }

    /** langCode like "en_us" or "th_th" (the Minecraft language code). */
    public String get(String langCode) {
        String lang = langCode == null ? "en" : langCode.toLowerCase(Locale.ROOT);
        String s = map.get(lang);
        if (s != null) {
            return s;
        }
        int i = lang.indexOf('_');
        if (i > 0 && (s = map.get(lang.substring(0, i))) != null) {
            return s;
        }
        s = map.get("en");
        return s != null ? s : map.values().iterator().next();
    }

    public String english() {
        return get("en");
    }
}
