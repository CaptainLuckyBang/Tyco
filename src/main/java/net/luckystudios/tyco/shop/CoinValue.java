package net.luckystudios.tyco.shop;

import net.luckystudios.tyco.config.TycoConfig;
import net.luckystudios.tyco.item.CoinTiers;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CoinValue {
    public static long[] computeValues() {
        long[] values = new long[CoinTiers.size()];
        values[0] = 1;
        for (int i = 1; i < CoinTiers.size(); i++) {
            values[i] = values[i - 1] * TycoConfig.getRatio(i - 1);
        }
        return values;
    }

    // Breaks a coal-coin-equivalent amount into actual coin stacks, largest denomination first
    public static List<ItemStack> makeChangeStacks(long amount) {
        List<ItemStack> result = new ArrayList<>();
        long[] values = computeValues();

        for (int t = values.length - 1; t >= 0; t--) {
            long count = amount / values[t];
            while (count > 0) {
                int take = (int) Math.min(count, 64);
                result.add(new ItemStack(CoinTiers.itemAt(t), take));
                count -= take;
            }
            amount -= (amount / values[t]) * values[t];
        }

        return result;
    }
}