package net.luckystudios.tyco.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.luckystudios.tyco.ModBlocks;
import net.luckystudios.tyco.recipe.SellingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class SellerCategory implements IRecipeCategory<SellingRecipe> {
    public static final RecipeType<SellingRecipe> SELLER_TYPE =
            RecipeType.create("tyco", "seller", SellingRecipe.class);

    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable slot;

    public SellerCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.SELLER.get()));
        this.arrow = guiHelper.drawableBuilder(
                ResourceLocation.fromNamespaceAndPath("tyco", "textures/gui/jei_arrow.png"),
                0, 0, 24, 17
        ).setTextureSize(24, 17).build();
        this.slot = guiHelper.getSlotDrawable();
    }

    @Override
    public RecipeType<SellingRecipe> getRecipeType() {
        return SELLER_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Seller");
    }

    @Override
    public int getWidth() {
        return 120;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SellingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 15)
                .setBackground(slot, -1, -1)
                .addItemStack(new ItemStack(recipe.input().getItems()[0].getItem(), recipe.inputCount()));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 80, 15)
                .setBackground(slot, -1, -1)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(SellingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, "Time: " + (recipe.interval() / 20.0) + "s", 5, 38, 0x404040, false);

        arrow.draw(guiGraphics, 40, 17);
    }
}