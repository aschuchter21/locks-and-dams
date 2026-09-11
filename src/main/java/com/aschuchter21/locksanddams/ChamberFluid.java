package com.aschuchter21.locksanddams;

import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;

/** Still, non-spreading water. Only a validated controller changes its height. */
public final class ChamberFluid extends Fluid {
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 1, 16);
    public ChamberFluid() { registerDefaultState(stateDefinition.any().setValue(HEIGHT, 16)); }
    @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) { builder.add(HEIGHT); }
    @Override public FluidType getFluidType() { return ForgeMod.WATER_TYPE.get(); }
    @Override public Item getBucket() { return Items.AIR; }
    @Override protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) { return false; }
    @Override protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) { return Vec3.ZERO; }
    @Override public int getTickDelay(LevelReader level) { return 5; }
    @Override protected float getExplosionResistance() { return 100; }
    @Override public float getOwnHeight(FluidState state) { return state.getValue(HEIGHT) / 16f; }
    @Override public float getHeight(FluidState state, BlockGetter level, BlockPos pos) { return getOwnHeight(state); }
    @Override public boolean isSource(FluidState state) { return true; }
    @Override public int getAmount(FluidState state) { return 8; }
    @Override protected BlockState createLegacyBlock(FluidState state) { return Content.WATER_BLOCK.get().defaultBlockState().setValue(HEIGHT, state.getValue(HEIGHT)); }
    @Override public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) { return Shapes.box(0, 0, 0, 1, getOwnHeight(state), 1); }
}
