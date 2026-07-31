package net.luckystudios.tyco.block.entity;

import net.luckystudios.tyco.ModBlocks;
import net.luckystudios.tyco.recipe.GeneratingRecipe;
import net.luckystudios.tyco.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class GeneratorBlockEntity extends BlockEntity {
    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 1;
    public static final int TOTAL_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;
    public static final int OUTPUT_SLOT_INDEX = INPUT_SLOTS;

    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < INPUT_SLOTS) return stack.is(net.luckystudios.tyco.ModTags.Items.COINS);
            return false;
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
    private int upgradeTier = 0;

    private static final double[] INTERVAL_MULT = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5};
    private static final double[] COST_MULT     = {1.0, 1.2, 1.4, 1.6, 1.8, 2.0};
    private static final int[] OUTPUT_MULT      = {1, 2, 3, 4, 5, 6};

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENERATOR_BE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getUpgradeTier() { return upgradeTier; }
    public void setUpgradeTier(int tier) { this.upgradeTier = tier; setChanged(); }

    public static int scaledCoinCount(int baseCount, int tier) {
        return (int) Math.ceil(baseCount * COST_MULT[tier]);
    }

    public static int scaledOutputCount(int baseCount, int tier) {
        return baseCount * OUTPUT_MULT[tier];
    }


    private static String machineFor(BlockState state) {
        if (state.is(ModBlocks.MINER.get())) return "miner";
        if (state.is(ModBlocks.LUMBERJACK.get())) return "lumberjack";
        return "unknown";
    }

    // Sums how many of a specific coin item are currently sitting across all input slots
    private static long sumOfItem(GeneratorBlockEntity be, Item item) {
        long total = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    // Removes `amount` of a specific coin item from across the input slots
    private static void consumeFromInputs(GeneratorBlockEntity be, Item item, long amount) {
        long remaining = amount;
        for (int i = 0; i < INPUT_SLOTS && remaining > 0; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            if (stack.is(item)) {
                int take = (int) Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GeneratorBlockEntity be) {
        if (level.isClientSide()) return;

        String machine = machineFor(state);
        BlockState belowState = level.getBlockState(pos.below());

        var allRecipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.GENERATING_TYPE.get());

        GeneratingRecipe matchedRecipe = null;
        Item requiredCoin = null;

        for (var r : allRecipes) {
            GeneratingRecipe recipe = r.value();
            if (!recipe.machine().equals(machine)) continue;
            if (!recipe.blocks().contains(belowState.getBlockHolder())) continue;

            var coinItems = recipe.coinInput().getItems();
            if (coinItems.length == 0) continue;
            Item coinItem = coinItems[0].getItem();

            int effectiveCoinCount = (int) Math.ceil(recipe.coinCount() * COST_MULT[be.upgradeTier]);
            if (sumOfItem(be, coinItem) >= effectiveCoinCount) {
                matchedRecipe = recipe;
                requiredCoin = coinItem;
                break;
            }
        }

        if (matchedRecipe == null) {
            be.cooldown = 0;
            return;
        }

        int effectiveCoinCount = (int) Math.ceil(matchedRecipe.coinCount() * COST_MULT[be.upgradeTier]);
        int effectiveInterval = Math.max(1, (int) Math.round(matchedRecipe.interval() * INTERVAL_MULT[be.upgradeTier]));

        be.cooldown++;
        if (be.cooldown < effectiveInterval) return;
        be.cooldown = 0;

        ItemStack output = be.inventory.getStackInSlot(OUTPUT_SLOT_INDEX);
        ItemStack result = matchedRecipe.assembleRandom(level.getRandom());
        result.setCount(result.getCount() * OUTPUT_MULT[be.upgradeTier]);

        boolean outputFits = output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());

        if (!outputFits) return;

        consumeFromInputs(be, requiredCoin, effectiveCoinCount);

        if (output.isEmpty()) {
            be.inventory.setStackInSlot(OUTPUT_SLOT_INDEX, result);
        } else {
            output.grow(result.getCount());
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
        inventory.setSize(TOTAL_SLOTS);
        cooldown = tag.getInt("cooldown");
        upgradeTier = tag.getInt("upgrade_tier");
    }
}