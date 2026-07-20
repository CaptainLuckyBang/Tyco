package net.luckystudios.tyco.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class TycoBlockItem extends BlockItem {
    private final String tooltipKeyPrefix;

    public TycoBlockItem(Block block, Properties properties, String tooltipKeyPrefix) {
        super(block, properties);
        this.tooltipKeyPrefix = tooltipKeyPrefix;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int line = 1;
        while (true) {
            String key = "tooltip.tyco." + tooltipKeyPrefix + ".line" + line;
            Component translated = Component.translatable(key);
            // If the key doesn't exist in the lang file, Minecraft just echoes the key back as literal text -
            // this checks for that so we stop adding lines once we run out of real translations.
            if (translated.getString().equals(key)) break;
            tooltip.add(translated);
            line++;
        }
    }
}