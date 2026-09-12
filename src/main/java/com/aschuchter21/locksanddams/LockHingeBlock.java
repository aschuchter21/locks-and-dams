package com.aschuchter21.locksanddams;

import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A Create bearing with a lock-controlled target and a shaft connection below. */
public final class LockHingeBlock extends BearingBlock implements IBE<LockHingeEntity> {
    public LockHingeBlock() { super(Content.metal());registerDefaultState(defaultBlockState().setValue(FACING,Direction.UP)); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState(); }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,net.minecraft.core.BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return net.minecraft.world.phys.shapes.Shapes.block();}
    @Override public InteractionResult onWrenched(BlockState s,UseOnContext c) { return InteractionResult.PASS; }
    @Override public Class<LockHingeEntity> getBlockEntityClass() { return LockHingeEntity.class; }
    @Override public BlockEntityType<? extends LockHingeEntity> getBlockEntityType() { return Content.HINGE_ENTITY.get(); }
}
