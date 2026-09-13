package com.aschuchter21.locksanddams;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Two flanged connections on the selected pipe axis; controller supplies the interlocked command. */
public final class InlineValveBlock extends ControlBlock {
    public static final EnumProperty<Direction.Axis> AXIS=RotatedPillarBlock.AXIS;
    public InlineValveBlock(){super();registerDefaultState(defaultBlockState().setValue(AXIS,Direction.Axis.Z));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(AXIS);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(AXIS,c.getClickedFace().getAxis());}
}
