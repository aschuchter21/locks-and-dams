package com.aschuchter21.locksanddams;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
/** A clipped water cell alongside a mitered gate. Its original state is saved for restoration. */
public final class GateBoundaryBlock extends BaseEntityBlock {
    public GateBoundaryBlock(){super(Properties.copy(Blocks.IRON_BLOCK).dynamicShape().isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false).forceSolidOn().strength(-1,100).noLootTable());}
    // noCollission() also disables canOcclude; preserve occlusion and override collision instead.
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
    @Override public VoxelShape getOcclusionShape(BlockState s,BlockGetter l,BlockPos p){return waterOcclusion(l,p);}
    @Override public int getLightBlock(BlockState s,BlockGetter l,BlockPos p){return 0;}
    @Override public boolean propagatesSkylightDown(BlockState s,BlockGetter l,BlockPos p){return true;}
    // Hide only faces shared with ordinary water, never adjacent masonry faces.
    static VoxelShape waterOcclusion(BlockGetter l,BlockPos p){
        VoxelShape shape=Shapes.empty();
        for(net.minecraft.core.Direction d:net.minecraft.core.Direction.Plane.HORIZONTAL){
            if(l.getFluidState(p.relative(d)).isEmpty())continue;
            shape=Shapes.or(shape,switch(d){
                case WEST->Shapes.box(0,0,0,.001,1,1);
                case EAST->Shapes.box(.999,0,0,1,1,1);
                case NORTH->Shapes.box(0,0,0,1,1,.001);
                default->Shapes.box(0,0,.999,1,1,1);
            });
        }
        return shape;
    }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new GateWaterEntity(p,s);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
    @Override public void entityInside(BlockState s,Level l,BlockPos p,net.minecraft.world.entity.Entity e){BoatSupport.inside(l,e);}
    public static net.minecraft.world.level.material.FluidState originalFluid(BlockGetter l,BlockPos p){
        return l.getBlockEntity(p) instanceof GateWaterEntity w&&w.original!=null?w.original.getFluidState():l.getFluidState(p);
    }
}
