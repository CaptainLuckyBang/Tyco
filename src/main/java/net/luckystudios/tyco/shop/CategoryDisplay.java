package net.luckystudios.tyco.shop;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record CategoryDisplay(String category, Optional<ResourceLocation> iconItemId) {
}