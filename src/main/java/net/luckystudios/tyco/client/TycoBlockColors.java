package net.luckystudios.tyco.client;

import net.luckystudios.tyco.ModBlocks;
import net.luckystudios.tyco.block.BankerBlock;
import net.luckystudios.tyco.block.GeneratorBlock;
import net.luckystudios.tyco.block.SellerBlock;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public class TycoBlockColors {
    public static final int[] TIER_COLORS = {
            0xFFFFFF, // 0 = no upgrade, no tint
            0xE0995E, // 1 = copper - orange
            0xD8D8D8, // 2 = iron - light gray
            0xFFD700, // 3 = gold - yellow
            0x4DE1E7, // 4 = diamond - cyan
            0x5B3A5B  // 5 = netherite - dark purple-gray
    };

    public static void register(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    int tier = state.getValue(GeneratorBlock.UPGRADE_TIER);
                    return TIER_COLORS[tier];
                },
                ModBlocks.MINER.get(), ModBlocks.LUMBERJACK.get()
        );

        event.register(
                (state, level, pos, tintIndex) -> {
                    int tier = state.getValue(SellerBlock.UPGRADE_TIER);
                    return TIER_COLORS[tier];
                },
                ModBlocks.SELLER.get()
        );

        event.register(
                (state, level, pos, tintIndex) -> {
                    int tier = state.getValue(BankerBlock.UPGRADE_TIER);
                    return TIER_COLORS[tier];
                },
                ModBlocks.BANKER.get()
        );
    }
}