package com.aschuchter21.locksanddams;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class ControllerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public ControllerBlock() { super(Content.metal()); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Content.LOCK.get(), LockEntity::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(player.getItemInHand(hand).is(Content.LINK_TOOL.get()))return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LockEntity lock) {
            if(player.isShiftKeyDown()&&lock.deskPosition()==null&&lock.deskControl().stopped){lock.deskControl().reset();lock.deskChanged();}
            if (!lock.assembled()) lock.assemble();
            player.displayClientMessage(Component.literal(lock.status()), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public boolean isSignalSource(BlockState s){return true;}
    @Override public int getSignal(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,Direction side){return l.getBlockEntity(p) instanceof LockEntity lock?lock.deskOutput(side.getOpposite()):0;}
    @Override public int getDirectSignal(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,Direction side){return getSignal(s,l,p,side);}
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof LockEntity lock)lock.deskChangedStop();super.onRemove(s,l,p,next,moving);}
}
