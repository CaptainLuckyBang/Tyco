package net.luckystudios.tyco.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ShopEntryRecipe(ItemStack item, int price, String category) implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return item.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return item.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SHOP_ENTRY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.SHOP_ENTRY_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ShopEntryRecipe> {
        public static final MapCodec<ShopEntryRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ItemStack.CODEC.fieldOf("item").forGetter(ShopEntryRecipe::item),
                com.mojang.serialization.Codec.INT.fieldOf("price").forGetter(ShopEntryRecipe::price),
                com.mojang.serialization.Codec.STRING.optionalFieldOf("category", "Misc").forGetter(ShopEntryRecipe::category)
        ).apply(inst, ShopEntryRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ShopEntryRecipe> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, ShopEntryRecipe::item,
                ByteBufCodecs.VAR_INT, ShopEntryRecipe::price,
                ByteBufCodecs.STRING_UTF8, ShopEntryRecipe::category,
                ShopEntryRecipe::new
        );

        @Override
        public MapCodec<ShopEntryRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShopEntryRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}