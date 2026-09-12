package com.aschuchter21.locksanddams;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class ControlDeskBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART=IntegerProperty.create("part",0,2),LAMP=IntegerProperty.create("lamp",0,4);
    public static final BooleanProperty SWITCH=BooleanProperty.create("switch"),FLASH=BooleanProperty.create("flash"),ALERT=BooleanProperty.create("alert"),ACTIVE=BooleanProperty.create("active");
    private static final java.util.Set<BlockPos> removing=new java.util.HashSet<>();
    public ControlDeskBlock(){super(Content.metal().noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,1).setValue(LAMP,0).setValue(SWITCH,false).setValue(FLASH,false).setValue(ALERT,false).setValue(ACTIVE,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,PART,LAMP,SWITCH,FLASH,ALERT,ACTIVE);}
    public static BlockPos root(BlockState s,BlockPos p){return p.relative(s.getValue(FACING).getCounterClockWise(),1-s.getValue(PART));}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var s=defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());
        for(int i:new int[]{-1,1}){BlockPos p=c.getClickedPos().relative(s.getValue(FACING).getCounterClockWise(),i);if(!c.getLevel().isInWorldBounds(p)||!c.getLevel().getWorldBorder().isWithinBounds(p)||!c.getLevel().getBlockState(p).canBeReplaced())return null;}
        return s;
    }
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){if(!l.isClientSide)for(int i:new int[]{0,2})l.setBlock(p.relative(s.getValue(FACING).getCounterClockWise(),i-1),s.setValue(PART,i),3);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ControlDeskEntity(p,s);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        VoxelShape shape=box(0,0,0,16,8,16);Direction facing=s.getValue(FACING);
        for(int z=0;z<16;z++)shape=Shapes.or(shape,rotatedBox(0,8,z,16,8.7+(z+1)*.383,z+1,facing));
        shape=Shapes.or(shape,rotatedBox(6,10,6,10,14,10,facing));
        if(s.getValue(PART)==1)shape=Shapes.or(shape,rotatedBox(5.5,9,2.5,10.5,12,5.5,facing));
        return shape;
    }
    private static VoxelShape rotatedBox(double x0,double y0,double z0,double x1,double y1,double z1,Direction f){
        return switch(f){case EAST->box(16-z1,y0,x0,16-z0,y1,x1);case SOUTH->box(16-x1,y0,16-z1,16-x0,y1,16-z0);case WEST->box(z0,y0,16-x1,z1,y1,16-x0);default->box(x0,y0,z0,x1,y1,z1);};
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Content.DESK_ENTITY.get(),ControlDeskEntity::tick);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.getItemInHand(hand).is(Content.LINK_TOOL.get()))return InteractionResult.PASS;
        if(!l.isClientSide&&l.getBlockEntity(root(s,p)) instanceof ControlDeskEntity desk) {
            Vec3 relative=hit.getLocation().subtract(Vec3.atCenterOf(p));
            double towardFront=relative.x*s.getValue(FACING).getStepX()+relative.z*s.getValue(FACING).getStepZ();
            desk.operate(player,s.getValue(PART),towardFront>.15,player.isShiftKeyDown());
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public boolean isSignalSource(BlockState s){return true;}
    @Override public int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction side){
        if(!(l.getBlockEntity(root(s,p)) instanceof ControlDeskEntity desk))return 0;
        int part=s.getValue(PART);Direction out=part==0?s.getValue(FACING).getClockWise():s.getValue(FACING).getCounterClockWise();
        return part!=1&&side==out.getOpposite()?desk.output(part==0):0;
    }
    @Override public int getDirectSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return getSignal(s,l,p,d);}
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        if(!s.is(next.getBlock())&&!l.isClientSide) {
            BlockPos root=root(s,p);
            if(removing.add(root)) {
                if(l.getBlockEntity(root) instanceof ControlDeskEntity desk)desk.detach();
                for(int i=0;i<3;i++){BlockPos q=root.relative(s.getValue(FACING).getCounterClockWise(),i-1);if(!q.equals(p)&&l.getBlockState(q).is(this))l.destroyBlock(q,false);}
                removing.remove(root);
            }
        }
        super.onRemove(s,l,p,next,moving);
    }
}
