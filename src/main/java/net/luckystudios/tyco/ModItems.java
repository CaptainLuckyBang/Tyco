package net.luckystudios.tyco;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(TycoMod.MODID);

    public static final DeferredItem<Item> COAL_COIN = ITEMS.registerSimpleItem(
            "coal_coin", new Item.Properties()
    );

    public static final DeferredItem<Item> COPPER_COIN = ITEMS.registerSimpleItem(
            "copper_coin", new Item.Properties()
    );

    public static final DeferredItem<Item> IRON_COIN = ITEMS.registerSimpleItem(
            "iron_coin", new Item.Properties()
    );

    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem(
            "gold_coin", new Item.Properties()
    );

    public static final DeferredItem<Item> DIAMOND_COIN = ITEMS.registerSimpleItem(
            "diamond_coin", new Item.Properties()
    );

    public static final DeferredItem<Item> NETHERITE_COIN = ITEMS.registerSimpleItem(
            "netherite_coin", new Item.Properties()
    );

    public static final DeferredItem<Item> COPPER_UPGRADE = ITEMS.registerSimpleItem(
            "copper_upgrade", new Item.Properties()
    );

    public static final DeferredItem<Item> IRON_UPGRADE = ITEMS.registerSimpleItem(
            "iron_upgrade", new Item.Properties()
    );

    public static final DeferredItem<Item> GOLD_UPGRADE = ITEMS.registerSimpleItem(
            "gold_upgrade", new Item.Properties()
    );

    public static final DeferredItem<Item> DIAMOND_UPGRADE = ITEMS.registerSimpleItem(
            "diamond_upgrade", new Item.Properties()
    );

    public static final DeferredItem<Item> NETHERITE_UPGRADE = ITEMS.registerSimpleItem(
            "netherite_upgrade", new Item.Properties()
    );

}