package com.aschuchter21.locksanddams;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.*;

public final class ChamberWaterBlock extends Block {
    public ChamberWaterBlock() {
        super(Properties.copy(Blocks.WATER).noCollission().noOcclusion().strength(-1, 100).noLootTable());
        registerDefaultState(stateDefinition.any().setValue(ChamberFluid.HEIGHT, 16));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(ChamberFluid.HEIGHT); }
    @Override public FluidState getFluidState(BlockState state) { return Content.WATER.get().defaultFluidState().setValue(ChamberFluid.HEIGHT, state.getValue(ChamberFluid.HEIGHT)); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) { BoatSupport.inside(level,entity); }
}
