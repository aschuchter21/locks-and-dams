package com.aschuchter21.locksanddams;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Mountable upper/lower controller terminal: front CLUTCH, top REVERSE. */
public final class GateControlNodeBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty UPPER=BooleanProperty.create("upper");
    public GateControlNodeBlock(){super(Content.metal());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(UPPER,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,UPPER);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new GateControlNodeEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Content.NODE_ENTITY.get(),GateControlNodeEntity::tick);}
    @Override public boolean isSignalSource(BlockState s){return true;}
    @Override public int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction direction){
        if(!(l.getBlockEntity(p) instanceof GateControlNodeEntity node))return 0;
        Direction out=direction.getOpposite(),front=s.getValue(FACING);
        return out==front?node.clutch:out==Direction.UP?node.reverse:0;
    }
    @Override public int getDirectSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return getSignal(s,l,p,d);}
    public static Component settingsHint(){return Component.keybind("key.sneak").append(" + ").append(Component.keybind("key.use")).append(" for settings.");}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand h,BlockHitResult hit){
        if(!player.isShiftKeyDown()&&player.getItemInHand(h).is(Content.LINK_TOOL.get()))return InteractionResult.PASS;
        if(!l.isClientSide){if(player.isShiftKeyDown()&&player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer,new net.minecraft.world.SimpleMenuProvider((id,inventory,who)->new GateNodeMenu(id,inventory,p),Component.literal("Gate Control Node")),p);else player.displayClientMessage(Component.literal((s.getValue(UPPER)?"UPPER":"LOWER")+" GATE NODE | Front: CLUTCH (15 stops) | Top: REVERSE (15 closes) | ").append(settingsHint()),true);}
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
