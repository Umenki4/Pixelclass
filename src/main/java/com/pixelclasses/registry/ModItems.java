package com.pixelclasses.registry;

import com.pixelclasses.PixelClassesMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Items of the mod. The Shadow Dagger is a fast, light weapon for the Assassin (hold one in each hand for the look). */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PixelClassesMod.MODID);

    public static final RegistryObject<Item> SHADOW_DAGGER =
            ITEMS.register("shadow_dagger", () -> new SwordItem(Tiers.IRON, 1, -1.4F, new Item.Properties()));

    private ModItems() {}
}
