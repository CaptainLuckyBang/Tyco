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

    public static void tick(Level level, BlockPos pos, BlockState state, BankerBlockEntity be) {
        if (level.isClientSide()) return;

        ItemStack inputStack = be.inventory.getStackInSlot(0);
        if (inputStack.isEmpty()) {
            be.cooldown = 0;
            return;
        }

        int tierIndex = CoinTiers.indexOf(inputStack);

        if (tierIndex != -1) {
            tickBuiltInConversion(be, inputStack, tierIndex);
            return;
        }

        // Fallback: generic data-driven banking recipes, for custom currencies via JSON/KubeJS
        BankerRecipe.Input input = new BankerRecipe.Input(be.direction, inputStack);

        var allRecipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.BANKING_TYPE.get());

        BankerRecipe matchedRecipe = null;
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

    private static void tickBuiltInConversion(BankerBlockEntity be, ItemStack inputStack, int tierIndex) {
        boolean goingUp = be.direction.equals("up");

        if (goingUp && tierIndex >= CoinTiers.size() - 1) {
            be.cooldown = 0;
            return; // already at the top tier
        }
        if (!goingUp && tierIndex <= 0) {
            be.cooldown = 0;
            return; // already at the bottom tier
        }

        int consumeCount;
        int produceCount;
        int resultTierIndex;

        if (goingUp) {
            int ratio = TycoConfig.getRatio(tierIndex);
            resultTierIndex = tierIndex + 1;
            consumeCount = ratio;
            produceCount = 1;
        } else {
            int ratio = TycoConfig.getRatio(tierIndex - 1);
            resultTierIndex = tierIndex - 1;
            consumeCount = 1;
            produceCount = ratio;
        }

        if (inputStack.getCount() < consumeCount) {
            be.cooldown = 0;
            return;
        }

        be.cooldown++;
        int baseInterval = TycoConfig.CONVERSION_INTERVAL_TICKS.get();
        int effectiveInterval = Math.max(1, (int) Math.round(baseInterval * INTERVAL_MULT[be.upgradeTier]));
        if (be.cooldown < effectiveInterval) return;
        be.cooldown = 0;

        ItemStack output = be.inventory.getStackInSlot(1);
        ItemStack result = new ItemStack(CoinTiers.itemAt(resultTierIndex), produceCount);

        boolean outputFits = output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());

        if (!outputFits) return;

        inputStack.shrink(consumeCount);

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