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
import net.luckystudios.tyco.recipe.GeneratingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class LumberjackCategory implements IRecipeCategory<GeneratingRecipe> {
    public static final RecipeType<GeneratingRecipe> LUMBERJACK_TYPE =
            RecipeType.create("tyco", "lumberjack", GeneratingRecipe.class);

    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable slot;

    public LumberjackCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.LUMBERJACK.get()));
        this.arrow = guiHelper.drawableBuilder(
                ResourceLocation.fromNamespaceAndPath("tyco", "textures/gui/jei_arrow.png"),
                0, 0, 24, 17
        ).setTextureSize(24, 17).build();
        this.slot = guiHelper.getSlotDrawable();
    }

    @Override
    public RecipeType<GeneratingRecipe> getRecipeType() {
        return LUMBERJACK_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Lumberjack");
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 60;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GeneratingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 20)
                .setBackground(slot, -1, -1)
                .addItemStack(new ItemStack(recipe.coinInput().getItems()[0].getItem(), recipe.coinCount()));

        List<ItemStack> blockIcons = recipe.blocks().stream()
                .map(holder -> new ItemStack(holder.value().asItem()))
                .filter(stack -> !stack.isEmpty())
                .toList();
        builder.addSlot(RecipeIngredientRole.CATALYST, 30, 20)
                .setBackground(slot, -1, -1)
                .addItemStacks(blockIcons);

        List<ItemStack> outputs;
        if (!recipe.outputs().isEmpty()) {
            outputs = recipe.outputs().stream()
                    .map(w -> new ItemStack(w.item(), Math.max(w.minCount(), 1)))
                    .toList();
        } else {
            ItemStack out = recipe.output().copy();
            out.setCount(Math.max(recipe.minCount(), 1));
            outputs = List.of(out);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 110, 20)
                .setBackground(slot, -1, -1)
                .addItemStacks(outputs);
    }

    @Override
    public void draw(GeneratingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, "Cost: " + recipe.coinCount(), 5, 5, 0x404040, false);
        guiGraphics.drawString(font, "Time: " + (recipe.interval() / 20.0) + "s", 5, 45, 0x404040, false);

        arrow.draw(guiGraphics, 65, 21);
    }
}