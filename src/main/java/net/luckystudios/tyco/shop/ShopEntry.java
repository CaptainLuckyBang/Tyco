package net.luckystudios.tyco.shop;

import net.minecraft.resources.ResourceLocation;

public record ShopEntry(ResourceLocation itemId, int price, String category) {
}