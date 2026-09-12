package com.aschuchter21.locksanddams;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
/** Reserved gate opening: the visible panels belong to a Create contraption. */
public final class GateSealBlock extends GatePanelBlock {
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return s.getValue(OPEN)?Shapes.empty():Shapes.block(); }
}
