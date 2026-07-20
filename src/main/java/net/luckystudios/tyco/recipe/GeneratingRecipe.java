package net.luckystudios.tyco.recipe;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

public record GeneratingRecipe(
        String machine,
        HolderSet<Block> blocks,
        Ingredient coinInput,
        int coinCount,
        ItemStack output,
        int minCount,
        int maxCount,
        double bonusChance,
        int bonusCount,
        List<WeightedOutput> outputs,
        int interval
) implements Recipe<GeneratingRecipe.Input> {

    public record Input(String machine, BlockState belowState, ItemStack coinStack) implements net.minecraft.world.item.crafting.RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return coinStack;
        }
        @Override
        public int size() {
            return 1;
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (!machine.equals(input.machine())) return false;

        Holder<Block> holder = input.belowState().getBlockHolder();
        boolean blockMatches = blocks.contains(holder);
        boolean coinMatches = coinInput.test(input.coinStack()) && input.coinStack().getCount() >= coinCount;
        return blockMatches && coinMatches;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        if (!outputs.isEmpty()) {
            return new ItemStack(outputs.get(0).item()); // display representative; real roll happens in assembleRandom
        }
        ItemStack result = output.copy();
        result.setCount(minCount);
        return result;
    }

    // Real production method used by the block entity. Rolls the weighted pool if present, else uses the legacy single-output range/bonus.
    public ItemStack assembleRandom(RandomSource random) {
        if (!outputs.isEmpty()) {
            int totalWeight = outputs.stream().mapToInt(WeightedOutput::weight).sum();
            int roll = random.nextInt(Math.max(totalWeight, 1));
            int cumulative = 0;
            for (WeightedOutput w : outputs) {
                cumulative += w.weight();
                if (roll < cumulative) {
                    return w.roll(random);
                }
            }
            return outputs.get(outputs.size() - 1).roll(random);
        }

        int count;
        if (bonusChance > 0 && random.nextDouble() < bonusChance) {
            count = bonusCount;
        } else {
            count = minCount == maxCount ? minCount : minCount + random.nextInt(maxCount - minCount + 1);
        }
        ItemStack result = output.copy();
        result.setCount(count);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        if (!outputs.isEmpty()) return new ItemStack(outputs.get(0).item());
        return output.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GENERATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.GENERATING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<GeneratingRecipe> {
        public static final MapCodec<GeneratingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                com.mojang.serialization.Codec.STRING.fieldOf("machine").forGetter(GeneratingRecipe::machine),
                RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks").forGetter(GeneratingRecipe::blocks),
                Ingredient.CODEC.fieldOf("coin_input").forGetter(GeneratingRecipe::coinInput),
                com.mojang.serialization.Codec.INT.fieldOf("coin_count").forGetter(GeneratingRecipe::coinCount),
                ItemStack.CODEC.optionalFieldOf("output", ItemStack.EMPTY).forGetter(GeneratingRecipe::output),
                com.mojang.serialization.Codec.INT.optionalFieldOf("min_count", 1).forGetter(GeneratingRecipe::minCount),
                com.mojang.serialization.Codec.INT.optionalFieldOf("max_count", 1).forGetter(GeneratingRecipe::maxCount),
                com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("bonus_chance", 0.0).forGetter(GeneratingRecipe::bonusChance),
                com.mojang.serialization.Codec.INT.optionalFieldOf("bonus_count", 0).forGetter(GeneratingRecipe::bonusCount),
                WeightedOutput.CODEC.listOf().optionalFieldOf("outputs", List.of()).forGetter(GeneratingRecipe::outputs),
                com.mojang.serialization.Codec.INT.fieldOf("interval").forGetter(GeneratingRecipe::interval)
        ).apply(inst, GeneratingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, GeneratingRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, recipe) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, recipe.machine());
                    ByteBufCodecs.holderSet(Registries.BLOCK).encode(buf, recipe.blocks());
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.coinInput());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.coinCount());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.output());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.minCount());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.maxCount());
                    ByteBufCodecs.DOUBLE.encode(buf, recipe.bonusChance());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.bonusCount());
                    ByteBufCodecs.collection(ArrayList::new, WeightedOutput.STREAM_CODEC).encode(buf, new ArrayList<>(recipe.outputs()));
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.interval());
                },
                (buf) -> {
                    String machine = ByteBufCodecs.STRING_UTF8.decode(buf);
                    HolderSet<Block> blocks = ByteBufCodecs.holderSet(Registries.BLOCK).decode(buf);
                    Ingredient coinInput = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                    int coinCount = ByteBufCodecs.VAR_INT.decode(buf);
                    ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                    int minCount = ByteBufCodecs.VAR_INT.decode(buf);
                    int maxCount = ByteBufCodecs.VAR_INT.decode(buf);
                    double bonusChance = ByteBufCodecs.DOUBLE.decode(buf);
                    int bonusCount = ByteBufCodecs.VAR_INT.decode(buf);
                    List<WeightedOutput> outputs = ByteBufCodecs.collection(ArrayList::new, WeightedOutput.STREAM_CODEC).decode(buf);
                    int interval = ByteBufCodecs.VAR_INT.decode(buf);
                    return new GeneratingRecipe(machine, blocks, coinInput, coinCount, output, minCount, maxCount, bonusChance, bonusCount, outputs, interval);
                }
        );

        @Override
        public MapCodec<GeneratingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GeneratingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}