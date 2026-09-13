package com.aschuchter21.locksanddams;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ControlBlock extends Block {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public ControlBlock() {
        super(Content.metal().noOcclusion().lightLevel(s -> s.getValue(OPEN) ? 8 : 0));
        registerDefaultState(stateDefinition.any().setValue(OPEN, false).setValue(POWERED, false).setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(OPEN, POWERED,FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) player.displayClientMessage(Component.literal("Redstone: " + (level.hasNeighborSignal(pos) ? "ON" : "OFF") + " | " + (state.getValue(OPEN) ? "OPEN" : "CLOSED") + " | Right-click the lock controller for interlock status."), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
