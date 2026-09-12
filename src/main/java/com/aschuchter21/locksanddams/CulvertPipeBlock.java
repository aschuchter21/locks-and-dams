package com.aschuchter21.locksanddams;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** A sealed culvert module; its flanged branches follow the actual pipe network. */
public final class CulvertPipeBlock extends Block {
    public static final BooleanProperty[] CONNECTIONS={BooleanProperty.create("down"),BooleanProperty.create("up"),BooleanProperty.create("north"),BooleanProperty.create("south"),BooleanProperty.create("west"),BooleanProperty.create("east")};
    public CulvertPipeBlock() {
        super(Content.metal().noOcclusion());
        BlockState s=stateDefinition.any();for(var p:CONNECTIONS)s=s.setValue(p,false);registerDefaultState(s);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CONNECTIONS);}
    public static BlockState connected(BlockState state,LevelAccessor level,BlockPos pos) {
        for(Direction d:Direction.values()) {
            BlockState n=level.getBlockState(pos.relative(d));
            boolean joins=n.is(Content.PIPE.get())||n.is(Content.PORT.get())&&n.getValue(CulvertPortBlock.FACING)==d
                ||d==Direction.UP&&(n.is(Content.FILL.get())||n.is(Content.DRAIN.get()));
            state=state.setValue(CONNECTIONS[d.ordinal()],joins);
        }
        return state;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return connected(defaultBlockState(),c.getLevel(),c.getClickedPos());}
    @Override public BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos q){return connected(s,l,p);}
}
