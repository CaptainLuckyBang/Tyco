package net.luckystudios.tyco.block.entity;

import net.luckystudios.tyco.config.TycoConfig;
import net.luckystudios.tyco.item.CoinTiers;
import net.luckystudios.tyco.recipe.BankerRecipe;
import net.luckystudios.tyco.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class BankerBlockEntity extends BlockEntity {
    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 9;
    public static final int TOTAL_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;

    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUT_SLOTS;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < INPUT_SLOTS) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int cooldown = 0;
    private String direction = "up";
    private int upgradeTier = 0;

    private static final double[] INTERVAL_MULT = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5};

    public BankerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BANKER_BE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public String getDirection() {
        return direction;
    }

    public void toggleDirection() {
        direction = direction.equals("up") ? "down" : "up";
        cooldown = 0;
        setChanged();
    }

    public int getUpgradeTier() { return upgradeTier; }
    public void setUpgradeTier(int tier) { this.upgradeTier = tier; setChanged(); }

    // Sums how many of a specific coin tier are currently sitting across all 9 input slots
    private static long totalOfTier(BankerBlockEntity be, int tierIndex) {
        var coinItem = CoinTiers.itemAt(tierIndex);
        long total = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            if (stack.is(coinItem)) total += stack.getCount();
        }
        return total;
    }

    // Removes `amount` of a specific coin tier from across the input slots, assuming enough is already confirmed present
    private static void consumeFromTier(BankerBlockEntity be, int tierIndex, long amount) {
        var coinItem = CoinTiers.itemAt(tierIndex);
        long remaining = amount;
        for (int i = 0; i < INPUT_SLOTS && remaining > 0; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            if (stack.is(coinItem)) {
                int take = (int) Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    // Attempts to place a result stack into any output slot (stacking onto a matching one if possible).
    // Returns false if every output slot is full/incompatible, so the caller can bail out without losing the input.
    private static boolean tryPlaceOutput(BankerBlockEntity be, ItemStack result) {
        for (int i = INPUT_SLOTS; i < TOTAL_SLOTS; i++) {
            ItemStack existing = be.inventory.getStackInSlot(i);
            if (existing.isEmpty()) {
                be.inventory.setStackInSlot(i, result.copy());
                return true;
            }
            if (ItemStack.isSameItemSameComponents(existing, result)
                    && existing.getCount() + result.getCount() <= existing.getMaxStackSize()) {
                existing.grow(result.getCount());
                return true;
            }
        }
        return false;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BankerBlockEntity be) {
        if (level.isClientSide()) return;

        int baseInterval = TycoConfig.CONVERSION_INTERVAL_TICKS.get();
        int effectiveInterval = Math.max(1, (int) Math.round(baseInterval * INTERVAL_MULT[be.upgradeTier]));

        be.cooldown++;
        if (be.cooldown < effectiveInterval) return;
        be.cooldown = 0;

        boolean goingUp = be.direction.equals("up");

        if (goingUp) {
            for (int tierIndex = 0; tierIndex < CoinTiers.size() - 1; tierIndex++) {
                int ratio = TycoConfig.getRatio(tierIndex);
                if (totalOfTier(be, tierIndex) >= ratio) {
                    ItemStack result = new ItemStack(CoinTiers.itemAt(tierIndex + 1), 1);
                    if (tryPlaceOutput(be, result)) {
                        consumeFromTier(be, tierIndex, ratio);
                        be.setChanged();
                        return;
                    }
                }
            }
        } else {
            for (int tierIndex = CoinTiers.size() - 1; tierIndex >= 1; tierIndex--) {
                if (totalOfTier(be, tierIndex) >= 1) {
                    int ratio = TycoConfig.getRatio(tierIndex - 1);
                    ItemStack result = new ItemStack(CoinTiers.itemAt(tierIndex - 1), ratio);
                    if (tryPlaceOutput(be, result)) {
                        consumeFromTier(be, tierIndex, 1);
                        be.setChanged();
                        return;
                    }
                }
            }
        }

        // Fallback: generic data-driven banking recipes, for custom currencies via JSON/KubeJS.
        // Checks the first non-empty input slot's item type, same spirit as before but now
        // summed across all input slots holding that same item.
        ItemStack sampleStack = ItemStack.EMPTY;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                sampleStack = stack;
                break;
            }
        }
        if (sampleStack.isEmpty()) return;

        BankerRecipe.Input input = new BankerRecipe.Input(be.direction, sampleStack);
        var allRecipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.BANKING_TYPE.get());

        BankerRecipe matchedRecipe = null;
        for (var r : allRecipes) {
            if (r.value().matches(input, level)) {
                matchedRecipe = r.value();
                break;
            }
        }
        if (matchedRecipe == null) return;

        long totalOfSample = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(stack, sampleStack)) {
                totalOfSample += stack.getCount();
            }
        }
        if (totalOfSample < matchedRecipe.inputCount()) return;

        ItemStack result = matchedRecipe.assemble(input, level.registryAccess());
        if (!tryPlaceOutput(be, result)) return;

        long remaining = matchedRecipe.inputCount();
        for (int i = 0; i < INPUT_SLOTS && remaining > 0; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(stack, sampleStack)) {
                int take = (int) Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }

        be.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("cooldown", cooldown);
        tag.putString("direction", direction);
        tag.putInt("upgrade_tier", upgradeTier);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        cooldown = tag.getInt("cooldown");
        direction = tag.contains("direction") ? tag.getString("direction") : "up";
        upgradeTier = tag.getInt("upgrade_tier");
    }
}