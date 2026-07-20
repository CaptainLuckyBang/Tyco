package net.luckystudios.tyco.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

// Purely a data holder loaded via the recipe system - defines how a category tab should look.
public record ShopCategoryRecipe(String category, Optional<Item> icon) implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return icon.map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return icon.map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SHOP_CATEGORY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.SHOP_CATEGORY_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ShopCategoryRecipe> {
        public static final MapCodec<ShopCategoryRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                com.mojang.serialization.Codec.STRING.fieldOf("category").forGetter(ShopCategoryRecipe::category),
                BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon").forGetter(ShopCategoryRecipe::icon)
        ).apply(inst, ShopCategoryRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ShopCategoryRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ShopCategoryRecipe::category,
                ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ITEM)), ShopCategoryRecipe::icon,
                ShopCategoryRecipe::new
        );

        @Override
        public MapCodec<ShopCategoryRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShopCategoryRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}