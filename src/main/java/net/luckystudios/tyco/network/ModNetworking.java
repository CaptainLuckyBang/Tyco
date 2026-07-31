package net.luckystudios.tyco.network;

import net.luckystudios.tyco.config.TycoConfig;
import net.luckystudios.tyco.recipe.ModRecipes;
import net.luckystudios.tyco.shop.CategoryDisplay;
import net.luckystudios.tyco.shop.ShopEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.*;

public class ModNetworking {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                OpenShopPayload.TYPE,
                OpenShopPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        net.luckystudios.tyco.client.ClientShopHandler.openShopScreen(payload.entries(), payload.categories())
                )
        );

        registrar.playToServer(
                UnlockCategoryPayload.TYPE,
                UnlockCategoryPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var player = context.player();
                    if (player == null || !(player.level() instanceof ServerLevel serverLevel)) return;
                    if (!(player instanceof ServerPlayer serverPlayer)) return;
                    if (!net.luckystudios.tyco.config.TycoConfig.ENABLE_CATEGORY_LOCKING.get()) return;

                    var allCategoryRecipes = serverLevel.getRecipeManager().getAllRecipesFor(ModRecipes.SHOP_CATEGORY_TYPE.get());
                    net.luckystudios.tyco.recipe.ShopCategoryRecipe target = null;
                    for (var r : allCategoryRecipes) {
                        if (r.value().category().equals(payload.category())) {
                            target = r.value();
                            break;
                        }
                    }

                    if (target == null || !target.isLocked()) return;

                    var unlockedData = net.luckystudios.tyco.shop.UnlockedCategoriesData.get(serverLevel);
                    if (unlockedData.isUnlocked(player.getUUID(), payload.category())) return;

                    boolean success;
                    if (target.unlockItem().isPresent()) {
                        Item requiredItem = target.unlockItem().get();
                        int requiredCount = target.unlockItemCount();
                        int owned = 0;
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (stack.is(requiredItem)) owned += stack.getCount();
                        }
                        if (owned < requiredCount) {
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal("You don't have enough items to unlock this!"), true);
                            return;
                        }
                        int remaining = requiredCount;
                        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (stack.is(requiredItem)) {
                                int take = Math.min(remaining, stack.getCount());
                                stack.shrink(take);
                                remaining -= take;
                            }
                        }
                        success = true;
                    } else {
                        success = net.luckystudios.tyco.shop.ShopPayment.tryDeductCoins(serverPlayer, target.unlockPrice().get());
                        if (!success) {
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Not enough coins!"), true);
                            return;
                        }
                    }

                    if (success) {
                        unlockedData.unlock(player.getUUID(), payload.category());
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("Category unlocked: " + payload.category()), true);

                        // Rebuild and resend the shop data so the client sees the tab unlocked immediately, no reopen needed
                        List<ShopEntry> entries = new ArrayList<>();
                        var allEntryRecipes = serverLevel.getRecipeManager().getAllRecipesFor(ModRecipes.SHOP_ENTRY_TYPE.get());
                        for (var r : allEntryRecipes) {
                            var recipe = r.value();
                            var id = BuiltInRegistries.ITEM.getKey(recipe.item().getItem());
                            entries.add(new ShopEntry(id, recipe.price(), recipe.category()));
                        }

                        Map<String, Optional<ResourceLocation>> categoryIcons = new LinkedHashMap<>();
                        for (ShopEntry entry : entries) {
                            categoryIcons.putIfAbsent(entry.category(), Optional.empty());
                        }

                        Map<String, net.luckystudios.tyco.recipe.ShopCategoryRecipe> categoryRecipes = new LinkedHashMap<>();
                        for (var r : allCategoryRecipes) {
                            var recipe = r.value();
                            categoryIcons.put(recipe.category(), recipe.icon().map(BuiltInRegistries.ITEM::getKey));
                            categoryRecipes.put(recipe.category(), recipe);
                        }

                        List<CategoryDisplay> freshCategories = new ArrayList<>();
                        for (var entry : categoryIcons.entrySet()) {
                            String categoryName = entry.getKey();
                            var catRecipe = categoryRecipes.get(categoryName);

                            boolean stillLocked = TycoConfig.ENABLE_CATEGORY_LOCKING.get()
                                    && catRecipe != null && catRecipe.isLocked()
                                    && !unlockedData.isUnlocked(player.getUUID(), categoryName);

                            Optional<Integer> unlockPrice = catRecipe != null ? catRecipe.unlockPrice() : Optional.empty();
                            Optional<ResourceLocation> unlockItemId = catRecipe != null
                                    ? catRecipe.unlockItem().map(BuiltInRegistries.ITEM::getKey)
                                    : Optional.empty();
                            int unlockItemCount = catRecipe != null ? catRecipe.unlockItemCount() : 1;

                            freshCategories.add(new CategoryDisplay(categoryName, entry.getValue(), stillLocked, unlockPrice, unlockItemId, unlockItemCount));
                        }

                        PacketDistributor.sendToPlayer(serverPlayer, new OpenShopPayload(entries, freshCategories));
                    }
                })
        );
    }
}