package net.luckystudios.tyco;

import net.luckystudios.tyco.block.entity.ModBlockEntities;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ModCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.GENERATOR_BE.get(),
                (be, side) -> be.getInventory()
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SELLER_BE.get(),
                (be, side) -> be.getInventory()
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.BANKER_BE.get(),
                (be, side) -> be.getInventory()
        );
    }
}