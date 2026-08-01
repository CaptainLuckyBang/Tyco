package net.luckystudios.tyco.client;

import net.luckystudios.tyco.shop.CategoryDisplay;
import net.luckystudios.tyco.shop.ShopEntry;
import net.minecraft.client.Minecraft;

import java.util.List;

public class ClientShopHandler {
    public static void openShopScreen(List<ShopEntry> entries, List<CategoryDisplay> categories) {
        var mc = Minecraft.getInstance();

        if (mc.screen instanceof ShopScreen existingScreen) {
            existingScreen.updateData(entries, categories);
        } else {
            mc.setScreen(new ShopScreen(entries, categories));
        }
    }
}