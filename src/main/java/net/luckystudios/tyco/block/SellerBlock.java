package net.luckystudios.tyco.block;

import net.luckystudios.tyco.block.entity.ModBlockEntities;
import net.luckystudios.tyco.block.entity.SellerBlockEntity;
import net.luckystudios.tyco.item.UpgradeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SellerBlock extends Block implements EntityBlock {
    public static final IntegerProperty UPGRADE_TIER = IntegerProperty.create("upgrade_tier", 0, 5);

    public SellerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(UPGRADE_TIER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UPGRADE_TIER);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SellerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SELLER_BE.get(), SellerBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        int tier = UpgradeItems.tierFor(heldItem);
        if (tier == 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (level.getBlockEntity(pos) instanceof SellerBlockEntity be) {
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof SellerBlockEntity be) {
            var inventory = be.getInventory();
            ItemStack toGive = ItemStack.EMPTY;
            int slotTaken = -1;

            ItemStack slot1 = inventory.getStackInSlot(1);
            if (!slot1.isEmpty()) {
                toGive = slot1;
                slotTaken = 1;
            } else {
                ItemStack slot0 = inventory.getStackInSlot(0);
                if (!slot0.isEmpty()) {
                    toGive = slot0;
                    slotTaken = 0;
                }
            }

            if (slotTaken != -1) {
                inventory.setStackInSlot(slotTaken, ItemStack.EMPTY);
                if (!player.getInventory().add(toGive)) {
                    player.drop(toGive, false);
                }
                be.setChanged();
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof SellerBlockEntity be) {
                var inventory = be.getInventory();
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Block.popResource(level, pos, stack);
                    }
                }
                for (ItemStack upgradeItem : UpgradeItems.upgradeItemsUpToTier(be.getUpgradeTier())) {
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