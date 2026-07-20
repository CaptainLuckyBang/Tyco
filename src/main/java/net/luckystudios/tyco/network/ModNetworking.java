package net.luckystudios.tyco.network;

import net.luckystudios.tyco.shop.ShopEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

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

                    var entries = new java.util.ArrayList<ShopEntry>();
                    var allRecipes = serverLevel.getRecipeManager().getAllRecipesFor(net.luckystudios.tyco.recipe.ModRecipes.SHOP_ENTRY_TYPE.get());
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
    }
}