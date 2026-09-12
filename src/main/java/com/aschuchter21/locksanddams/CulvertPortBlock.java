package com.aschuchter21.locksanddams;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** The marked face is the water mouth; pipe connects to the opposite face. */
public final class CulvertPortBlock extends Block {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public CulvertPortBlock() { super(Content.metal());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH)); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING); }
}
