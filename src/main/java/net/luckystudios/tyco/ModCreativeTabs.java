package net.luckystudios.tyco;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TycoMod.MODID);

    public static final Supplier<CreativeModeTab> TYCO_TAB = CREATIVE_MODE_TABS.register("tyco_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.GOLD_COIN.get()))
                    .title(Component.translatable("itemGroup.tyco.tyco_tab"))
                    .displayItems((parameters, output) -> {
                        // Coins
                        output.accept(ModItems.COAL_COIN.get());
                        output.accept(ModItems.COPPER_COIN.get());
                        output.accept(ModItems.IRON_COIN.get());
                        output.accept(ModItems.GOLD_COIN.get());
                        output.accept(ModItems.DIAMOND_COIN.get());
                        output.accept(ModItems.NETHERITE_COIN.get());
                        // Function blocks
                        output.accept(ModBlocks.MINER.get());
                        output.accept(ModBlocks.LUMBERJACK.get());
                        output.accept(ModBlocks.SELLER.get());
                        output.accept(ModBlocks.BANKER.get());
                        output.accept(ModBlocks.SHOP.get());
                        // Upgrades
                        output.accept(ModItems.COPPER_UPGRADE.get());
                        output.accept(ModItems.IRON_UPGRADE.get());
                        output.accept(ModItems.GOLD_UPGRADE.get());
                        output.accept(ModItems.DIAMOND_UPGRADE.get());
                        output.accept(ModItems.NETHERITE_UPGRADE.get());
                    })
                    .build());
}