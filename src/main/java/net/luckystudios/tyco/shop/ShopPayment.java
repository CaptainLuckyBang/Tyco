package net.luckystudios.tyco.shop;

import net.luckystudios.tyco.item.CoinTiers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ShopPayment {
    // Attempts to deduct `price` (in coal-coin value) from the player across all coin tiers,
    // giving change back in the largest denominations that fit. Returns false if they can't afford it.
    public static boolean tryDeductCoins(ServerPlayer player, long price) {
        long[] coinValue = CoinValue.computeValues();
        int tierCount = coinValue.length;

        int[] owned = new int[tierCount];
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            int tier = CoinTiers.indexOf(stack);
            if (tier != -1) owned[tier] += stack.getCount();
        }

        long totalWealth = 0;
        for (int t = 0; t < tierCount; t++) totalWealth += (long) owned[t] * coinValue[t];
        if (totalWealth < price) return false;

        int[] toRemove = new int[tierCount];
        long remaining = price;

        for (int t = 0; t < tierCount && remaining > 0; t++) {
            long useCount = Math.min(owned[t], remaining / coinValue[t]);
            toRemove[t] = (int) useCount;
            remaining -= useCount * coinValue[t];
        }

        if (remaining > 0) {
            boolean covered = false;
            for (int t = 0; t < tierCount; t++) {
                if (owned[t] > toRemove[t]) {
                    toRemove[t]++;
                    covered = true;
                    break;
                }
            }
            if (!covered) return false;
        }

        long totalConsumedValue = 0;
        for (int t = 0; t < tierCount; t++) totalConsumedValue += (long) toRemove[t] * coinValue[t];
        long changeOwed = totalConsumedValue - price;

        for (int t = 0; t < tierCount; t++) {
            int remainingToRemove = toRemove[t];
            if (remainingToRemove <= 0) continue;

            var itemForTier = CoinTiers.itemAt(t);
            for (int i = 0; i < player.getInventory().getContainerSize() && remainingToRemove > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(itemForTier)) {
                    int take = Math.min(remainingToRemove, stack.getCount());
                    stack.shrink(take);
                    remainingToRemove -= take;
                }
            }
        }

        if (changeOwed > 0) {
            for (ItemStack changeStack : CoinValue.makeChangeStacks(changeOwed)) {
                if (!player.getInventory().add(changeStack)) {
                    player.drop(changeStack, false);
                }
            }
        }

        return true;
    }
}