package net.luckystudios.tyco.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SellingRecipe(Ingredient input, int inputCount, ItemStack output, int interval) implements Recipe<SellingRecipe.Input> {

    public record Input(ItemStack slotStack) implements net.minecraft.world.item.crafting.RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return slotStack;
        }
        @Override
        public int size() {
            return 1;
        }
    }

    @Override
    public boolean matches(Input recipeInput, Level level) {
        return input.test(recipeInput.slotStack()) && recipeInput.slotStack().getCount() >= inputCount;
    }

    @Override
    public ItemStack assemble(Input recipeInput, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SELLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.SELLING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<SellingRecipe> {
        public static final MapCodec<SellingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC.fieldOf("input").forGetter(SellingRecipe::input),
                com.mojang.serialization.Codec.INT.fieldOf("input_count").forGetter(SellingRecipe::inputCount),
                ItemStack.CODEC.fieldOf("output").forGetter(SellingRecipe::output),
                com.mojang.serialization.Codec.INT.fieldOf("interval").forGetter(SellingRecipe::interval)
        ).apply(inst, SellingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SellingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, SellingRecipe::input,
                ByteBufCodecs.VAR_INT, SellingRecipe::inputCount,
                ItemStack.STREAM_CODEC, SellingRecipe::output,
                ByteBufCodecs.VAR_INT, SellingRecipe::interval,
                SellingRecipe::new
        );

        @Override
        public MapCodec<SellingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SellingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}