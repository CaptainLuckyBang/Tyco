package net.luckystudios.tyco.block.entity;

import net.luckystudios.tyco.recipe.ModRecipes;
import net.luckystudios.tyco.recipe.SellingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class SellerBlockEntity extends BlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 0) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int cooldown = 0;
    private int upgradeTier = 0;

    private static final double[] INTERVAL_MULT = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5};

    public SellerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SELLER_BE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getUpgradeTier() { return upgradeTier; }
    public void setUpgradeTier(int tier) { this.upgradeTier = tier; setChanged(); }

    public static void tick(Level level, BlockPos pos, BlockState state, SellerBlockEntity be) {
        if (level.isClientSide()) return;

        ItemStack inputStack = be.inventory.getStackInSlot(0);
        if (inputStack.isEmpty()) {
            be.cooldown = 0;
            return;
        }

        SellingRecipe.Input input = new SellingRecipe.Input(inputStack);

        var allRecipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.SELLING_TYPE.get());

        SellingRecipe matchedRecipe = null;
        for (var r : allRecipes) {
            if (r.value().matches(input, level)) {
                matchedRecipe = r.value();
                break;
            }
        }

        if (matchedRecipe == null) {
            be.cooldown = 0;
            return;
        }

        int effectiveInterval = Math.max(1, (int) Math.round(matchedRecipe.interval() * INTERVAL_MULT[be.upgradeTier]));

        be.cooldown++;
        if (be.cooldown < effectiveInterval) return;
        be.cooldown = 0;

        ItemStack current = be.inventory.getStackInSlot(1);
        ItemStack result = matchedRecipe.assemble(input, level.registryAccess());

        ItemStack soldItemCopy = inputStack.copy();
        soldItemCopy.setCount(matchedRecipe.inputCount());

        net.luckystudios.tyco.api.SellerSaleEvent saleEvent =
                new net.luckystudios.tyco.api.SellerSaleEvent(level, pos, soldItemCopy, result);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(saleEvent);
        result = saleEvent.getResult();

        boolean outputFits = current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, result)
                && current.getCount() + result.getCount() <= current.getMaxStackSize());

        if (!outputFits) return;

        inputStack.shrink(matchedRecipe.inputCount());

        if (current.isEmpty()) {
            be.inventory.setStackInSlot(1, result);
        } else {
            current.grow(result.getCount());
        }

        be.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("cooldown", cooldown);
        tag.putInt("upgrade_tier", upgradeTier);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        cooldown = tag.getInt("cooldown");
        upgradeTier = tag.getInt("upgrade_tier");
    }
}