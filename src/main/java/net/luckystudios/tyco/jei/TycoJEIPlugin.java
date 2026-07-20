package net.luckystudios.tyco.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.luckystudios.tyco.ModBlocks;
import net.luckystudios.tyco.TycoMod;
import net.luckystudios.tyco.recipe.GeneratingRecipe;
import net.luckystudios.tyco.recipe.ModRecipes;
import net.luckystudios.tyco.recipe.SellingRecipe;
import net.luckystudios.tyco.recipe.BankerRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@JeiPlugin
public class TycoJEIPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(TycoMod.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new MinerCategory(guiHelper),
                new LumberjackCategory(guiHelper),
                new SellerCategory(guiHelper),
                new BankerCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var recipeManager = Minecraft.getInstance().level.getRecipeManager();

        List<GeneratingRecipe> allGenerating = recipeManager
                .getAllRecipesFor(ModRecipes.GENERATING_TYPE.get())
                .stream().map(r -> r.value()).toList();

        registration.addRecipes(MinerCategory.MINER_TYPE,
                allGenerating.stream().filter(r -> r.machine().equals("miner")).toList());
        registration.addRecipes(LumberjackCategory.LUMBERJACK_TYPE,
                allGenerating.stream().filter(r -> r.machine().equals("lumberjack")).toList());

        List<SellingRecipe> selling = recipeManager
                .getAllRecipesFor(ModRecipes.SELLING_TYPE.get())
                .stream().map(r -> r.value()).toList();
        registration.addRecipes(SellerCategory.SELLER_TYPE, selling);

        List<BankerRecipe> banking = recipeManager
                .getAllRecipesFor(ModRecipes.BANKING_TYPE.get())
                .stream().map(r -> r.value()).toList();
        registration.addRecipes(BankerCategory.BANKER_TYPE, banking);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MINER.get()), MinerCategory.MINER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.LUMBERJACK.get()), LumberjackCategory.LUMBERJACK_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SELLER.get()), SellerCategory.SELLER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.BANKER.get()), BankerCategory.BANKER_TYPE);
    }
}