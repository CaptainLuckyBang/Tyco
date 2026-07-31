package net.luckystudios.tyco.block;

import net.luckystudios.tyco.block.entity.GeneratorBlockEntity;
import net.luckystudios.tyco.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class GeneratorBlock extends Block implements EntityBlock {
    public static final IntegerProperty UPGRADE_TIER = IntegerProperty.create("upgrade_tier", 0, 5);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public GeneratorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(UPGRADE_TIER, 0)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UPGRADE_TIER, FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Front (north face of the model) points toward the player who placed it.
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.GENERATOR_BE.get(), GeneratorBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        int tier = tierFor(heldItem);
        if (tier == 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity be) {
            int currentTier = be.getUpgradeTier();

            if (tier <= currentTier) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Already at this tier or better!"), true);
                return ItemInteractionResult.CONSUME;
            }

            if (tier != currentTier + 1) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("You need to upgrade one tier at a time!"), true);
                return ItemInteractionResult.CONSUME;
            }

            be.setUpgradeTier(tier);
            level.setBlock(pos, state.setValue(UPGRADE_TIER, tier), Block.UPDATE_ALL);
            heldItem.shrink(1);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Upgraded to tier " + tier + "!"), true);
        }

        return ItemInteractionResult.CONSUME;
    }

    private static int tierFor(ItemStack stack) {
        if (stack.is(net.luckystudios.tyco.ModItems.NETHERITE_UPGRADE.get())) return 5;
        if (stack.is(net.luckystudios.tyco.ModItems.DIAMOND_UPGRADE.get())) return 4;
        if (stack.is(net.luckystudios.tyco.ModItems.GOLD_UPGRADE.get())) return 3;
        if (stack.is(net.luckystudios.tyco.ModItems.IRON_UPGRADE.get())) return 2;
        if (stack.is(net.luckystudios.tyco.ModItems.COPPER_UPGRADE.get())) return 1;
        return 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity be) {
            var inventory = be.getInventory();

            int slotTaken = -1;
            for (int i = GeneratorBlockEntity.INPUT_SLOTS; i < GeneratorBlockEntity.TOTAL_SLOTS; i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) {
                    slotTaken = i;
                    break;
                }
            }
            if (slotTaken == -1) {
                for (int i = 0; i < GeneratorBlockEntity.INPUT_SLOTS; i++) {
                    if (!inventory.getStackInSlot(i).isEmpty()) {
                        slotTaken = i;
                        break;
                    }
                }
            }

            if (slotTaken != -1) {
                ItemStack toGive = inventory.getStackInSlot(slotTaken);
                inventory.setStackInSlot(slotTaken, ItemStack.EMPTY);
                if (!player.getInventory().add(toGive)) {
                    player.drop(toGive, false);
                }
                be.setChanged();
            } else {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Nothing to retrieve."), true);
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity be) {
                var inventory = be.getInventory();
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Block.popResource(level, pos, stack);
                    }
                }

                for (ItemStack upgradeItem : net.luckystudios.tyco.item.UpgradeItems.upgradeItemsUpToTier(be.getUpgradeTier())) {
                    Block.popResource(level, pos, upgradeItem);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @SuppressWarnings("unchecked")
    private static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> serverType, BlockEntityType<E> clientType, BlockEntityTicker<? super E> ticker) {
        return clientType == serverType ? (BlockEntityTicker<A>) ticker : null;
    }
}