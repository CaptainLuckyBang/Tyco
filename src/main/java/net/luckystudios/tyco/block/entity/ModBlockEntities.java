package net.luckystudios.tyco.block.entity;

import net.luckystudios.tyco.ModBlocks;
import net.luckystudios.tyco.TycoMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TycoMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> GENERATOR_BE =
            BLOCK_ENTITIES.register("generator_be", () -> BlockEntityType.Builder.of(
                    GeneratorBlockEntity::new,
                    ModBlocks.MINER.get(),
                    ModBlocks.LUMBERJACK.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SellerBlockEntity>> SELLER_BE =
            BLOCK_ENTITIES.register("seller_be", () -> BlockEntityType.Builder.of(
                    SellerBlockEntity::new,
                    ModBlocks.SELLER.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BankerBlockEntity>> BANKER_BE =
            BLOCK_ENTITIES.register("banker_be", () -> BlockEntityType.Builder.of(
                    BankerBlockEntity::new,
                    ModBlocks.BANKER.get()
            ).build(null));
}