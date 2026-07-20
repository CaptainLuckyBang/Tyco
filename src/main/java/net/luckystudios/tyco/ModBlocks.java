package net.luckystudios.tyco;

import java.util.function.Supplier;

import net.luckystudios.tyco.block.BankerBlock;
import net.luckystudios.tyco.block.GeneratorBlock;
import net.luckystudios.tyco.block.SellerBlock;
import net.luckystudios.tyco.block.ShopBlock;
import net.luckystudios.tyco.item.TycoBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;



public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(TycoMod.MODID);

    public static final DeferredBlock<GeneratorBlock> MINER = registerMachineBlock("miner",
            () -> new GeneratorBlock(metalProperties()));

    public static final DeferredBlock<GeneratorBlock> LUMBERJACK = registerMachineBlock("lumberjack",
            () -> new GeneratorBlock(metalProperties()));

    public static final DeferredBlock<SellerBlock> SELLER = registerMachineBlock("seller",
            () -> new SellerBlock(metalProperties()));

    public static final DeferredBlock<BankerBlock> BANKER = registerMachineBlock("banker",
            () -> new BankerBlock(metalProperties()));

    public static final DeferredBlock<ShopBlock> SHOP = registerMachineBlock("shop",
            () -> new ShopBlock(metalProperties()));

    private static BlockBehaviour.Properties metalProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(3.0f, 6.0f)
                .requiresCorrectToolForDrops();
    }

    // Registers a machine block with a TycoBlockItem, so it can show custom tooltip lines
    private static <T extends Block> DeferredBlock<T> registerMachineBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new TycoBlockItem(toReturn.get(), new Item.Properties(), name));
        return toReturn;
    }
}