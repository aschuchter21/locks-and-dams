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

public class GatePanelBlock extends Block {
    public static final EnumProperty<net.minecraft.core.Direction.Axis> AXIS = EnumProperty.create("axis", net.minecraft.core.Direction.Axis.class);
    public static final BooleanProperty REVERSED = BooleanProperty.create("reversed");
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final IntegerProperty EDGE = IntegerProperty.create("edge",0,4);
    public static final IntegerProperty DEPTH = IntegerProperty.create("depth", 0, 16);
    public GatePanelBlock() {
        super(Content.metal().noOcclusion().isSuffocating((s, l, p) -> !s.getValue(OPEN)));
        registerDefaultState(stateDefinition.any().setValue(REVERSED,false).setValue(OPEN, false).setValue(TOP, false).setValue(EDGE,0).setValue(DEPTH, 0).setValue(AXIS,net.minecraft.core.Direction.Axis.Z));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(OPEN, DEPTH, AXIS, TOP, EDGE, REVERSED); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c) {
        return facing(defaultBlockState(),c.getHorizontalDirection().getOpposite());
    }
    public static BlockState facing(BlockState state,net.minecraft.core.Direction canal) {
        return state.setValue(AXIS,canal.getAxis()).setValue(REVERSED,canal==net.minecraft.core.Direction.SOUTH||canal==net.minecraft.core.Direction.WEST);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if(state.getValue(OPEN))return Shapes.empty();
        boolean acrossZ=state.getValue(AXIS)==net.minecraft.core.Direction.Axis.X;
        VoxelShape panel=acrossZ?box(5,0,0,11,16,16):box(0,0,5,16,16,11);
        int edge=state.getValue(EDGE),lo=edge==1?5:edge==4?1:0,hi=edge==2?11:edge==3?15:16;
        VoxelShape deck=acrossZ?box(1,14,lo,15,16,hi):box(lo,14,1,hi,16,15);
        VoxelShape rails=acrossZ?Shapes.or(box(1,16,lo,2,33,hi),box(14,16,lo,15,33,hi))
            :Shapes.or(box(lo,16,1,hi,33,2),box(lo,16,14,hi,33,15));
        return state.getValue(TOP)?Shapes.or(panel,deck,rails):panel;
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    @Override public RenderShape getRenderShape(BlockState state) { return state.getValue(OPEN) ? RenderShape.INVISIBLE : RenderShape.MODEL; }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) { if(state.getValue(OPEN)) BoatSupport.inside(level,entity); }
    @Override public FluidState getFluidState(BlockState state) {
        int depth = state.getValue(DEPTH);
        return state.getValue(OPEN) && depth > 0 ? Content.WATER.get().defaultFluidState().setValue(ChamberFluid.HEIGHT, depth) : Fluids.EMPTY.defaultFluidState();
    }
}
