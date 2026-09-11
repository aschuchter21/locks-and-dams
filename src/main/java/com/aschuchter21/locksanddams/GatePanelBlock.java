package com.aschuchter21.locksanddams;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class GatePanelBlock extends Block {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final IntegerProperty DEPTH = IntegerProperty.create("depth", 0, 16);
    public GatePanelBlock() {
        super(Content.metal().noOcclusion().isSuffocating((s, l, p) -> !s.getValue(OPEN)));
        registerDefaultState(stateDefinition.any().setValue(OPEN, false).setValue(DEPTH, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(OPEN, DEPTH); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return state.getValue(OPEN) ? Shapes.empty() : Shapes.block(); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    @Override public RenderShape getRenderShape(BlockState state) { return state.getValue(OPEN) ? RenderShape.INVISIBLE : RenderShape.MODEL; }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) { if(state.getValue(OPEN)) BoatSupport.inside(level,entity); }
    @Override public FluidState getFluidState(BlockState state) {
        int depth = state.getValue(DEPTH);
        return state.getValue(OPEN) && depth > 0 ? Content.WATER.get().defaultFluidState().setValue(ChamberFluid.HEIGHT, depth) : Fluids.EMPTY.defaultFluidState();
    }
}
