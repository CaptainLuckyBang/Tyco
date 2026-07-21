package net.luckystudios.tyco.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

// Fired whenever the Seller block completes a sale, right before the payout is actually granted.
// Other mods (like Tyco: Dynamic Pricing) can listen to this and modify the result to adjust the payout.
public class SellerSaleEvent extends Event {
    private final Level level;
    private final BlockPos pos;
    private final ItemStack soldItem; // the exact item + count that was consumed
    private ItemStack result;         // mutable - listeners can replace this to change the payout

    public SellerSaleEvent(Level level, BlockPos pos, ItemStack soldItem, ItemStack result) {
        this.level = level;
        this.pos = pos;
        this.soldItem = soldItem;
        this.result = result;
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ItemStack getSoldItem() {
        return soldItem;
    }

    public ItemStack getResult() {
        return result;
    }

    public void setResult(ItemStack result) {
        this.result = result;
    }
}