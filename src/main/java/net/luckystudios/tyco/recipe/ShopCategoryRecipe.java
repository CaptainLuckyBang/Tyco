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

public record ShopCategoryRecipe(
        String category,
        Optional<Item> icon,
        Optional<Integer> unlockPrice,     // coin cost to unlock, if using coins
        Optional<Item> unlockItem,         // item required to unlock, if using an item instead
        int unlockItemCount                // how many of unlockItem are needed (ignored if unlockItem absent)
) implements Recipe<SingleRecipeInput> {

    public boolean isLocked() {
        return unlockPrice.isPresent() || unlockItem.isPresent();
    }

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
                BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon").forGetter(ShopCategoryRecipe::icon),
                com.mojang.serialization.Codec.INT.optionalFieldOf("unlock_price").forGetter(ShopCategoryRecipe::unlockPrice),
                BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("unlock_item").forGetter(ShopCategoryRecipe::unlockItem),
                com.mojang.serialization.Codec.INT.optionalFieldOf("unlock_item_count", 1).forGetter(ShopCategoryRecipe::unlockItemCount)
        ).apply(inst, ShopCategoryRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ShopCategoryRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, recipe) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, recipe.category());
                    ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ITEM)).encode(buf, recipe.icon());
                    ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).encode(buf, recipe.unlockPrice());
                    ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ITEM)).encode(buf, recipe.unlockItem());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.unlockItemCount());
                },
                (buf) -> new ShopCategoryRecipe(
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ITEM)).decode(buf),
                        ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).decode(buf),
                        ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ITEM)).decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf)
                )
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