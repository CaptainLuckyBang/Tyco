package net.luckystudios.tyco.recipe;

import net.luckystudios.tyco.TycoMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, TycoMod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, TycoMod.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<GeneratingRecipe>> GENERATING_TYPE =
            RECIPE_TYPES.register("generating", () -> new RecipeType<>() {
                @Override public String toString() { return "tyco:generating"; }
            });

    public static final DeferredHolder<RecipeSerializer<?>, GeneratingRecipe.Serializer> GENERATING_SERIALIZER =
            RECIPE_SERIALIZERS.register("generating", GeneratingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<SellingRecipe>> SELLING_TYPE =
            RECIPE_TYPES.register("selling", () -> new RecipeType<>() {
                @Override public String toString() { return "tyco:selling"; }
            });

    public static final DeferredHolder<RecipeSerializer<?>, SellingRecipe.Serializer> SELLING_SERIALIZER =
            RECIPE_SERIALIZERS.register("selling", SellingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BankerRecipe>> BANKING_TYPE =
            RECIPE_TYPES.register("banking", () -> new RecipeType<>() {
                @Override public String toString() { return "tyco:banking"; }
            });

    public static final DeferredHolder<RecipeSerializer<?>, BankerRecipe.Serializer> BANKING_SERIALIZER =
            RECIPE_SERIALIZERS.register("banking", BankerRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ShopEntryRecipe>> SHOP_ENTRY_TYPE =
            RECIPE_TYPES.register("shop_entry", () -> new RecipeType<>() {
                @Override public String toString() { return "tyco:shop_entry"; }
            });

    public static final DeferredHolder<RecipeSerializer<?>, ShopEntryRecipe.Serializer> SHOP_ENTRY_SERIALIZER =
            RECIPE_SERIALIZERS.register("shop_entry", ShopEntryRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ShopCategoryRecipe>> SHOP_CATEGORY_TYPE =
            RECIPE_TYPES.register("shop_category", () -> new RecipeType<>() {
                @Override public String toString() { return "tyco:shop_category"; }
            });

    public static final DeferredHolder<RecipeSerializer<?>, ShopCategoryRecipe.Serializer> SHOP_CATEGORY_SERIALIZER =
            RECIPE_SERIALIZERS.register("shop_category", ShopCategoryRecipe.Serializer::new);
}