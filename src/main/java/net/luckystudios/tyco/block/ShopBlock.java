package net.luckystudios.tyco.block;

import net.luckystudios.tyco.config.TycoConfig;
import net.luckystudios.tyco.network.OpenShopPayload;
import net.luckystudios.tyco.recipe.ModRecipes;
import net.luckystudios.tyco.recipe.ShopCategoryRecipe;
import net.luckystudios.tyco.shop.CategoryDisplay;
import net.luckystudios.tyco.shop.ShopEntry;
import net.luckystudios.tyco.shop.UnlockedCategoriesData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ShopBlock extends Block {
    public ShopBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            List<ShopEntry> entries = new ArrayList<>();
            var allEntryRecipes = serverLevel.getRecipeManager().getAllRecipesFor(ModRecipes.SHOP_ENTRY_TYPE.get());
            for (var r : allEntryRecipes) {
                var recipe = r.value();
                var id = BuiltInRegistries.ITEM.getKey(recipe.item().getItem());
                entries.add(new ShopEntry(id, recipe.price(), recipe.category()));
            }

            var unlockedData = UnlockedCategoriesData.get(serverLevel);

            Map<String, Optional<ResourceLocation>> categoryIcons = new LinkedHashMap<>();
            for (ShopEntry entry : entries) {
                categoryIcons.putIfAbsent(entry.category(), Optional.empty());
            }

            Map<String, ShopCategoryRecipe> categoryRecipes = new LinkedHashMap<>();
            var allCategoryRecipes = serverLevel.getRecipeManager().getAllRecipesFor(ModRecipes.SHOP_CATEGORY_TYPE.get());
            for (var r : allCategoryRecipes) {
                var recipe = r.value();
                categoryIcons.put(recipe.category(), recipe.icon().map(BuiltInRegistries.ITEM::getKey));
                categoryRecipes.put(recipe.category(), recipe);
            }

            List<CategoryDisplay> categories = new ArrayList<>();
            for (var entry : categoryIcons.entrySet()) {
                String categoryName = entry.getKey();
                ShopCategoryRecipe catRecipe = categoryRecipes.get(categoryName);

                boolean isLocked = TycoConfig.ENABLE_CATEGORY_LOCKING.get()
                        && catRecipe != null && catRecipe.isLocked()
                        && !unlockedData.isUnlocked(player.getUUID(), categoryName);

                Optional<Integer> unlockPrice = catRecipe != null ? catRecipe.unlockPrice() : Optional.empty();
                Optional<ResourceLocation> unlockItemId = catRecipe != null
                        ? catRecipe.unlockItem().map(BuiltInRegistries.ITEM::getKey)
                        : Optional.empty();
                int unlockItemCount = catRecipe != null ? catRecipe.unlockItemCount() : 1;

                categories.add(new CategoryDisplay(categoryName, entry.getValue(), isLocked, unlockPrice, unlockItemId, unlockItemCount));
            }

            PacketDistributor.sendToPlayer(serverPlayer, new OpenShopPayload(entries, categories));
        }
        return InteractionResult.CONSUME;
    }
}