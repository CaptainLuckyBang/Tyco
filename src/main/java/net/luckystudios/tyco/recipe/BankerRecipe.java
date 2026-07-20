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

public record BankerRecipe(String direction, Ingredient input, int inputCount, ItemStack output, int interval) implements Recipe<BankerRecipe.Input> {

    // direction + item both carried, so matches() can filter by the block's current mode
    public record Input(String direction, ItemStack slotStack) implements net.minecraft.world.item.crafting.RecipeInput {
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
        if (!direction.equals(recipeInput.direction())) return false;
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
        return ModRecipes.BANKING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.BANKING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<BankerRecipe> {
        public static final MapCodec<BankerRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                com.mojang.serialization.Codec.STRING.fieldOf("direction").forGetter(BankerRecipe::direction),
                Ingredient.CODEC.fieldOf("input").forGetter(BankerRecipe::input),
                com.mojang.serialization.Codec.INT.fieldOf("input_count").forGetter(BankerRecipe::inputCount),
                ItemStack.CODEC.fieldOf("output").forGetter(BankerRecipe::output),
                com.mojang.serialization.Codec.INT.fieldOf("interval").forGetter(BankerRecipe::interval)
        ).apply(inst, BankerRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, BankerRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, BankerRecipe::direction,
                Ingredient.CONTENTS_STREAM_CODEC, BankerRecipe::input,
                ByteBufCodecs.VAR_INT, BankerRecipe::inputCount,
                ItemStack.STREAM_CODEC, BankerRecipe::output,
                ByteBufCodecs.VAR_INT, BankerRecipe::interval,
                BankerRecipe::new
        );

        @Override
        public MapCodec<BankerRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BankerRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}