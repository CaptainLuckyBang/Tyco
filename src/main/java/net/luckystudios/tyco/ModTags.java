package net.luckystudios.tyco;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {
    public static class Items {
        public static final TagKey<Item> COINS = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(TycoMod.MODID, "coins")
        );
    }
}