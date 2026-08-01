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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                BuyItemPayload.TYPE,
                BuyItemPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var player = context.player();
                    if (player == null || !(player.level() instanceof ServerLevel serverLevel)) return;

                    var entries = new ArrayList<ShopEntry>();
                    var allRecipes = serverLevel.getRecipeManager().getAllRecipesFor(ModRecipes.SHOP_ENTRY_TYPE.get());
                    for (var r : allRecipes) {
                        var recipe = r.value();
                        var id = BuiltInRegistries.ITEM.getKey(recipe.item().getItem());
                        entries.add(new ShopEntry(id, recipe.price(), recipe.category()));
                    }

                    int index = payload.entryIndex();
                    if (index < 0 || index >= entries.size()) return;

                    int quantity = Math.max(1, Math.min(64, payload.quantity()));

                    ShopEntry entry = entries.get(index);
                    long price = (long) entry.price() * quantity;

                    long[] coinValue = net.luckystudios.tyco.shop.CoinValue.computeValues();
                    int tierCount = coinValue.length;

                    int[] owned = new int[tierCount];
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        int tier = net.luckystudios.tyco.item.CoinTiers.indexOf(stack);
                        if (tier != -1) owned[tier] += stack.getCount();
                    }

                    long totalWealth = 0;
                    for (int t = 0; t < tierCount; t++) totalWealth += (long) owned[t] * coinValue[t];

                    if (totalWealth < price) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("Not enough coins!"), true);
                        return;
                    }

                    int[] toRemove = new int[tierCount];
                    long remaining = price;

                    for (int t = 0; t < tierCount && remaining > 0; t++) {
                        long useCount = Math.min(owned[t], remaining / coinValue[t]);
                        toRemove[t] = (int) useCount;
                        remaining -= useCount * coinValue[t];
                    }

                    if (remaining > 0) {
                        boolean covered = false;
                        for (int t = 0; t < tierCount; t++) {
                            if (owned[t] > toRemove[t]) {
                                toRemove[t]++;
                                covered = true;
                                break;
                            }
                        }
                        if (!covered) {
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Can't make that combination of coins work!"), true);
                            return;
                        }
                    }

                    long totalConsumedValue = 0;
                    for (int t = 0; t < tierCount; t++) totalConsumedValue += (long) toRemove[t] * coinValue[t];
                    long changeOwed = totalConsumedValue - price;

                    for (int t = 0; t < tierCount; t++) {
                        int remainingToRemove = toRemove[t];
                        if (remainingToRemove <= 0) continue;

                        var itemForTier = net.luckystudios.tyco.item.CoinTiers.itemAt(t);
                        for (int i = 0; i < player.getInventory().getContainerSize() && remainingToRemove > 0; i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (stack.is(itemForTier)) {
                                int take = Math.min(remainingToRemove, stack.getCount());
                                stack.shrink(take);
                                remainingToRemove -= take;
                            }
                        }
                    }

                    if (changeOwed > 0) {
                        for (ItemStack changeStack : net.luckystudios.tyco.shop.CoinValue.makeChangeStacks(changeOwed)) {
                            if (!player.getInventory().add(changeStack)) {
                                player.drop(changeStack, false);
                            }
                        }
                    }

                    Item boughtItem = BuiltInRegistries.ITEM.get(entry.itemId());
                    int remainingToGive = quantity;
                    while (remainingToGive > 0) {
                        int stackAmount = Math.min(remainingToGive, boughtItem.getDefaultMaxStackSize());
                        ItemStack result = new ItemStack(boughtItem, stackAmount);
                        if (!player.getInventory().add(result)) {
                            player.drop(result, false);
                        }
                        remainingToGive -= stackAmount;
                    }
                })
        );

        registrar.playToServer(
                UnlockCategoryPayload.TYPE,
                UnlockCategoryPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var player = context.player();
                    if (player == null || !(player.level() instanceof ServerLevel serverLevel)) return;
                    if (!(player instanceof ServerPlayer serverPlayer)) return;
                    if (!TycoConfig.ENABLE_CATEGORY_LOCKING.get()) return;

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