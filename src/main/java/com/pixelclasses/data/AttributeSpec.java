package com.pixelclasses.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** A class stat change: {"attribute": "minecraft:generic.max_health", "amount": 8.0, "operation": "addition"} */
public record AttributeSpec(ResourceLocation attribute, double amount, AttributeModifier.Operation operation) {

    public static AttributeSpec parse(JsonObject o) {
        ResourceLocation id = JsonUtil.id(GsonHelper.getAsString(o, "attribute"), "attribute");
        String op = GsonHelper.getAsString(o, "operation", "addition").toLowerCase(Locale.ROOT);
        AttributeModifier.Operation operation = switch (op) {
            case "addition", "add" -> AttributeModifier.Operation.ADDITION;
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> throw new JsonSyntaxException("Unknown operation '" + op + "' (use addition, multiply_base or multiply_total)");
        };
        return new AttributeSpec(id, GsonHelper.getAsDouble(o, "amount"), operation);
    }
}
