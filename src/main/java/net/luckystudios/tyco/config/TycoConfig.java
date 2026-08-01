package net.luckystudios.tyco.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class TycoConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue COAL_TO_COPPER;
    public static final ModConfigSpec.IntValue COPPER_TO_IRON;
    public static final ModConfigSpec.IntValue IRON_TO_GOLD;
    public static final ModConfigSpec.IntValue GOLD_TO_DIAMOND;
    public static final ModConfigSpec.IntValue DIAMOND_TO_NETHERITE;
    public static final ModConfigSpec.IntValue CONVERSION_INTERVAL_TICKS;
    public static final ModConfigSpec.BooleanValue ENABLE_CATEGORY_LOCKING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Tyco Coin Tier Conversion Settings").push("banker");

        COAL_TO_COPPER = builder
                .comment("How many Coal Coins are needed to convert into 1 Copper Coin")
                .defineInRange("coalToCopperRatio", 10, 1, 1000000);

        COPPER_TO_IRON = builder
                .comment("How many Copper Coins are needed to convert into 1 Iron Coin")
                .defineInRange("copperToIronRatio", 10, 1, 1000000);

        IRON_TO_GOLD = builder
                .comment("How many Iron Coins are needed to convert into 1 Gold Coin")
                .defineInRange("ironToGoldRatio", 10, 1, 1000000);

        GOLD_TO_DIAMOND = builder
                .comment("How many Gold Coins are needed to convert into 1 Diamond Coin")
                .defineInRange("goldToDiamondRatio", 10, 1, 1000000);

        DIAMOND_TO_NETHERITE = builder
                .comment("How many Diamond Coins are needed to convert into 1 Netherite Coin")
                .defineInRange("diamondToNetheriteRatio", 10, 1, 1000000);

        CONVERSION_INTERVAL_TICKS = builder
                .comment("How many ticks the Banker takes to perform one coin tier conversion (20 ticks = 1 second)")
                .defineInRange("conversionIntervalTicks", 20, 1, 72000);

        builder.pop();

        builder.comment("Tyco Shop Settings").push("shop");

        ENABLE_CATEGORY_LOCKING = builder
                .comment("If false, shop categories are always unlocked regardless of unlock_price/unlock_item defined on any tyco:shop_category recipe.")
                .define("enableCategoryLocking", true);

        builder.pop();

        SPEC = builder.build();
    }

    // lowerTierIndex 0 = coal->copper, 1 = copper->iron, etc.
    public static int getRatio(int lowerTierIndex) {
        return switch (lowerTierIndex) {
            case 0 -> COAL_TO_COPPER.get();
            case 1 -> COPPER_TO_IRON.get();
            case 2 -> IRON_TO_GOLD.get();
            case 3 -> GOLD_TO_DIAMOND.get();
            case 4 -> DIAMOND_TO_NETHERITE.get();
            default -> 8;
        };
    }
}