package com.aschuchter21.locksanddams;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GateControlNodeEntity extends BlockEntity {
    private BlockPos controller;
    int clutch=15,reverse=0;
    public GateControlNodeEntity(BlockPos p,BlockState s){super(Content.NODE_ENTITY.get(),p,s);}
    public boolean link(BlockPos p){if(level==null||p.distManhattan(worldPosition)>128||!level.hasChunkAt(p)||!(level.getBlockEntity(p) instanceof LockEntity))return false;controller=p.immutable();setChanged();return true;}
    public static void tick(Level l,BlockPos p,BlockState s,GateControlNodeEntity node){
        int stop=15,reverse=node.reverse;
        if(node.controller!=null&&l.hasChunkAt(node.controller)&&l.getBlockEntity(node.controller) instanceof LockEntity lock&&lock.deskPosition()!=null){
            Direction f=lock.getBlockState().getValue(ControllerBlock.FACING);boolean upper=s.getValue(GateControlNodeBlock.UPPER);
            stop=lock.deskOutput(upper?f:f.getOpposite());reverse=lock.deskOutput(upper?f.getClockWise():f.getCounterClockWise());
        }
        if(node.clutch!=stop||node.reverse!=reverse){node.clutch=stop;node.reverse=reverse;node.setChanged();l.updateNeighborsAt(p,s.getBlock());l.updateNeighborsAt(p.above(),s.getBlock());l.updateNeighborsAt(p.relative(s.getValue(GateControlNodeBlock.FACING)),s.getBlock());}
    }
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);if(controller!=null)n.put("Controller",NbtUtils.writeBlockPos(controller));n.putInt("Reverse",reverse);}
    @Override public void load(CompoundTag n){super.load(n);controller=n.contains("Controller")?NbtUtils.readBlockPos(n.getCompound("Controller")):null;clutch=15;reverse=n.getInt("Reverse");}
}
