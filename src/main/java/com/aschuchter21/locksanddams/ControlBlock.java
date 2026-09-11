package com.aschuchter21.locksanddams;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class ControlBlock extends Block {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public ControlBlock() {
        super(Content.metal().lightLevel(s -> s.getValue(OPEN) ? 8 : 0));
        registerDefaultState(stateDefinition.any().setValue(OPEN, false).setValue(POWERED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(OPEN, POWERED); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) player.displayClientMessage(Component.literal("Redstone: " + (level.hasNeighborSignal(pos) ? "ON" : "OFF") + " | " + (state.getValue(OPEN) ? "OPEN" : "CLOSED") + " | Right-click the lock controller for interlock status."), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
