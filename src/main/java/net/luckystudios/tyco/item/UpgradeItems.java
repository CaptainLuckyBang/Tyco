package net.luckystudios.tyco.item;

import net.luckystudios.tyco.ModItems;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class UpgradeItems {
    public static int tierFor(ItemStack stack) {
        if (stack.is(ModItems.NETHERITE_UPGRADE.get())) return 5;
        if (stack.is(ModItems.DIAMOND_UPGRADE.get())) return 4;
        if (stack.is(ModItems.GOLD_UPGRADE.get())) return 3;
        if (stack.is(ModItems.IRON_UPGRADE.get())) return 2;
        if (stack.is(ModItems.COPPER_UPGRADE.get())) return 1;
        return 0;
    }

    public static List<ItemStack> upgradeItemsUpToTier(int tier) {
        List<ItemStack> items = new ArrayList<>();
        for (int t = 1; t <= tier; t++) {
            ItemStack item = switch (t) {
                case 1 -> new ItemStack(ModItems.COPPER_UPGRADE.get());
                case 2 -> new ItemStack(ModItems.IRON_UPGRADE.get());
                case 3 -> new ItemStack(ModItems.GOLD_UPGRADE.get());
                case 4 -> new ItemStack(ModItems.DIAMOND_UPGRADE.get());
                case 5 -> new ItemStack(ModItems.NETHERITE_UPGRADE.get());
                default -> ItemStack.EMPTY;
            };
            if (!item.isEmpty()) items.add(item);
        }
        return items;
    }
}