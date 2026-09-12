package com.aschuchter21.locksanddams;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Visual water either side of a closed leaf; never joins the two fluid volumes. */
public final class GateWaterEntity extends BlockEntity{
    public int inside,outside;public Direction inward=Direction.NORTH;
    public GateWaterEntity(BlockPos p,BlockState s){super(Content.GATE_WATER_ENTITY.get(),p,s);}
    public static void update(Level l,BlockPos p,int inside,int outside,Direction inward){
        if(l.getBlockEntity(p) instanceof GateWaterEntity w&&(w.inside!=inside||w.outside!=outside||w.inward!=inward)){
            w.inside=inside;w.outside=outside;w.inward=inward;w.setChanged();l.sendBlockUpdated(p,w.getBlockState(),w.getBlockState(),2);
        }
    }
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.putInt("Inside",inside);n.putInt("Outside",outside);n.putInt("Inward",inward.get2DDataValue());}
    @Override public void load(CompoundTag n){super.load(n);inside=Math.max(0,Math.min(16,n.getInt("Inside")));outside=Math.max(0,Math.min(16,n.getInt("Outside")));inward=Direction.from2DDataValue(n.getInt("Inward"));}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
