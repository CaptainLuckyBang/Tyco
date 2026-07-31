package net.luckystudios.tyco.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.luckystudios.tyco.block.GeneratorBlock;
import net.luckystudios.tyco.block.entity.GeneratorBlockEntity;
import net.luckystudios.tyco.recipe.GeneratingRecipe;
import net.luckystudios.tyco.recipe.ModRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class GeneratorScreenRenderer implements BlockEntityRenderer<GeneratorBlockEntity> {
    public GeneratorScreenRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GeneratorBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos pos = be.getBlockPos();
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(GeneratorBlock.FACING);

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) return;
        if (!blockHit.getBlockPos().equals(pos)) return;
        if (blockHit.getDirection() != facing) return;

        ClientLevel level = (ClientLevel) be.getLevel();
        if (level == null) return;

        BlockState belowState = level.getBlockState(pos.below());
        String machine = state.is(net.luckystudios.tyco.ModBlocks.MINER.get()) ? "miner" : "lumberjack";

        GeneratingRecipe matched = null;
        var allRecipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.GENERATING_TYPE.get());
        for (var r : allRecipes) {
            GeneratingRecipe recipe = r.value();
            if (!recipe.machine().equals(machine)) continue;
            if (recipe.blocks().contains(belowState.getBlockHolder())) {
                matched = recipe;
                break;
            }
        }
        if (matched == null) return;

        int tier = state.getValue(GeneratorBlock.UPGRADE_TIER);

        Item coinItem = matched.coinInput().getItems()[0].getItem();
        int coinCount = GeneratorBlockEntity.scaledCoinCount(matched.coinCount(), tier);

        Item outputItem;
        int baseOutputCount;
        if (!matched.outputs().isEmpty()) {
            var firstOutput = matched.outputs().get(0);
            outputItem = firstOutput.item();
            baseOutputCount = Math.max(firstOutput.minCount(), 1);
        } else {
            outputItem = matched.output().getItem();
            baseOutputCount = Math.max(matched.minCount(), 1);
        }
        int outputCount = GeneratorBlockEntity.scaledOutputCount(baseOutputCount, tier);

        int fullBright = net.minecraft.client.renderer.LightTexture.pack(15, 15);

        poseStack.pushPose();
        float yRotation = facing.toYRot();
        poseStack.translate(0.5, 0.675, 0.5); // overall vertical position (lower = further down the face)
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f - yRotation));
        poseStack.translate(0, 0, -0.5);

        renderIconAndCount(mc, poseStack, bufferSource, fullBright, packedOverlay, level,
                new ItemStack(coinItem), coinCount, 0.15f);

        renderIconAndCount(mc, poseStack, bufferSource, fullBright, packedOverlay, level,
                new ItemStack(outputItem), outputCount, -0.05f);

        poseStack.popPose();
    }

    private void renderIconAndCount(Minecraft mc, PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay, ClientLevel level,
                                    ItemStack icon, int count, float verticalOffset) {
        Font font = mc.font;

        // Shared anchor for this row so the icon and its "xN" label move together.
        poseStack.pushPose();
        poseStack.translate(0.35, verticalOffset, 0);

        // --- Item icon ---
        poseStack.pushPose();
        poseStack.scale(0.12f, 0.12f, 0.12f);
        poseStack.translate(-0.5, -0.5, 0);
        mc.getItemRenderer().renderStatic(icon, ItemDisplayContext.FIXED,
                packedLight, packedOverlay, poseStack, bufferSource, level, 0);
        poseStack.popPose();

        // --- "xN" label, just left of the icon ---
        poseStack.pushPose();
        poseStack.translate(-0.16, -0.07, -0.01); // left of the icon, a hair proud of the face
        // Negate BOTH x and y: keeps the text upright and readable while leaving the matrix
        // determinant positive, so the glyph quads are not back-face culled on flush.
        poseStack.scale(-0.016f, -0.016f, 0.016f); // label size (smaller than the icons)
        String text = "x" + count;
        font.drawInBatch(text, 0, -font.lineHeight / 2f, 0x55CCFF, false, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.SEE_THROUGH, 0, packedLight);
        poseStack.popPose();

        poseStack.popPose();
    }
}