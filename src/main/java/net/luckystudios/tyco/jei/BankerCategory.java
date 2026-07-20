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
import net.luckystudios.tyco.recipe.BankerRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class BankerCategory implements IRecipeCategory<BankerRecipe> {
    public static final RecipeType<BankerRecipe> BANKER_TYPE =
            RecipeType.create("tyco", "banker", BankerRecipe.class);

    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable slot;

    public BankerCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.BANKER.get()));
        this.arrow = guiHelper
                .drawableBuilder(
                ResourceLocation.fromNamespaceAndPath("tyco", "textures/gui/jei_arrow.png"),
                0, 0, 24, 17
        ).setTextureSize(24, 17).build();
        this.slot = guiHelper.getSlotDrawable();
    }

    @Override
    public RecipeType<BankerRecipe> getRecipeType() {
        return BANKER_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Banker");
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
    public void setRecipe(IRecipeLayoutBuilder builder, BankerRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 15)
                .setBackground(slot, -1, -1)
                .addItemStack(new ItemStack(recipe.input().getItems()[0].getItem(), recipe.inputCount()));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 80, 15)
                .setBackground(slot, -1, -1)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(BankerRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, "Mode: " + recipe.direction(), 5, 2, 0x404040, false);
        guiGraphics.drawString(font, "Time: " + (recipe.interval() / 20.0) + "s", 5, 38, 0x404040, false);

        arrow.draw(guiGraphics, 40, 17);
    }
}