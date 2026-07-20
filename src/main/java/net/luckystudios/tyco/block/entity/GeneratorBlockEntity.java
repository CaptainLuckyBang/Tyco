package net.luckystudios.tyco.block.entity;

import net.luckystudios.tyco.ModBlocks;
import net.luckystudios.tyco.recipe.GeneratingRecipe;
import net.luckystudios.tyco.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class GeneratorBlockEntity extends BlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return stack.is(net.luckystudios.tyco.ModTags.Items.COINS);
            return false;
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

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENERATOR_BE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    // Determines which recipes this specific block is allowed to use
    private static String machineFor(BlockState state) {
        if (state.is(ModBlocks.MINER.get())) return "miner";
        if (state.is(ModBlocks.LUMBERJACK.get())) return "lumberjack";
        return "unknown";
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GeneratorBlockEntity be) {
        if (level.isClientSide()) return;

        BlockState belowState = level.getBlockState(pos.below());
        ItemStack coinStack = be.inventory.getStackInSlot(0);
        GeneratingRecipe.Input input = new GeneratingRecipe.Input(machineFor(state), belowState, coinStack);

        var allRecipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.GENERATING_TYPE.get());

        GeneratingRecipe matchedRecipe = null;
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

        int effectiveCoinCount = (int) Math.ceil(matchedRecipe.coinCount() * COST_MULT[be.upgradeTier]);
        int effectiveInterval = Math.max(1, (int) Math.round(matchedRecipe.interval() * INTERVAL_MULT[be.upgradeTier]));

        if (coinStack.getCount() < effectiveCoinCount) {
            be.cooldown = 0;
            return;
        }

        be.cooldown++;
        if (be.cooldown < effectiveInterval) return;
        be.cooldown = 0;

        ItemStack output = be.inventory.getStackInSlot(1);
        ItemStack result = matchedRecipe.assembleRandom(level.getRandom());
        result.setCount(result.getCount() * OUTPUT_MULT[be.upgradeTier]);

        boolean outputFits = output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());

        if (!outputFits) return;

        coinStack.shrink(effectiveCoinCount);

        if (output.isEmpty()) {
            be.inventory.setStackInSlot(1, result);
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
        upgradeTier = tag.getInt("upgrade_tier");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        cooldown = tag.getInt("cooldown");
    }

    private int upgradeTier = 0; // 0 = none, 1 = copper, 2 = iron, 3 = gold, 4 = diamond, 5 = netherite

    private static final double[] INTERVAL_MULT = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5};
    private static final double[] COST_MULT     = {1.0, 1.2, 1.4, 1.6, 1.8, 2.0};
    private static final int[] OUTPUT_MULT      = {1, 2, 3, 4, 5, 6};

    public int getUpgradeTier() { return upgradeTier; }
    public void setUpgradeTier(int tier) { this.upgradeTier = tier; setChanged(); }

    public static java.util.List<ItemStack> upgradeItemsUpToTier(int tier) {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int t = 1; t <= tier; t++) {
            ItemStack item = switch (t) {
                case 1 -> new ItemStack(net.luckystudios.tyco.ModItems.COPPER_UPGRADE.get());
                case 2 -> new ItemStack(net.luckystudios.tyco.ModItems.IRON_UPGRADE.get());
                case 3 -> new ItemStack(net.luckystudios.tyco.ModItems.GOLD_UPGRADE.get());
                case 4 -> new ItemStack(net.luckystudios.tyco.ModItems.DIAMOND_UPGRADE.get());
                case 5 -> new ItemStack(net.luckystudios.tyco.ModItems.NETHERITE_UPGRADE.get());
                default -> ItemStack.EMPTY;
            };
            if (!item.isEmpty()) items.add(item);
        }
        return items;
    }

}