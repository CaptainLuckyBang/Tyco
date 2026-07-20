package net.luckystudios.tyco.item;

import net.luckystudios.tyco.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

public class CoinTiers {
    @SuppressWarnings("unchecked")
    private static final DeferredItem<Item>[] TIERS = new DeferredItem[] {
            ModItems.COAL_COIN, ModItems.COPPER_COIN, ModItems.IRON_COIN,
            ModItems.GOLD_COIN, ModItems.DIAMOND_COIN, ModItems.NETHERITE_COIN
    };

    // Returns 0-5 for a known Tyco coin, or -1 if the item isn't one of the official coins
    public static int indexOf(ItemStack stack) {
        for (int i = 0; i < TIERS.length; i++) {
            if (stack.is(TIERS[i].get())) return i;
        }
        return -1;
    }

    public static Item itemAt(int tierIndex) {
        return TIERS[tierIndex].get();
    }

    public static int size() {
        return TIERS.length;
    }
}