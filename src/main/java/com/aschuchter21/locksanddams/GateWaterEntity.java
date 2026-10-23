package com.aschuchter21.locksanddams;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Visual water either side of a closed leaf; never joins the two fluid volumes. */
public final class GateWaterEntity extends BlockEntity{
    public boolean chamberCell; public BlockPos hinge; public boolean miter; public double planeX,planeZ,planeC; public int highDepth,lowDepth; public boolean unsealed; public BlockState original;
    public int inside,outside;public Direction inward=Direction.NORTH;
    public GateWaterEntity(BlockPos p,BlockState s){super(Content.GATE_WATER_ENTITY.get(),p,s);}
    public static void update(Level l,BlockPos p,int inside,int outside,Direction inward){
        if(l.getBlockEntity(p) instanceof GateWaterEntity w&&(w.inside!=inside||w.outside!=outside||w.inward!=inward)){
            w.inside=inside;w.outside=outside;w.inward=inward;w.setChanged();l.sendBlockUpdated(p,w.getBlockState(),w.getBlockState(),2);
        }
    }
    public static void boundary(Level l,BlockPos p,GateSpec g,int high,int low,boolean open,BlockState original,boolean chamberCell){
        if(!(l.getBlockEntity(p) instanceof GateWaterEntity w))return;
        double ratio=g.inset()/g.span(),a=g.upstream().getStepX()-g.inward().getStepX()*ratio,b=g.upstream().getStepZ()-g.inward().getStepZ()*ratio;
        double c=a*(p.getX()-g.base().getX()-.5)+b*(p.getZ()-g.base().getZ()-.5);
        if(w.miter&&w.planeX==a&&w.planeZ==b&&w.planeC==c&&w.highDepth==high&&w.lowDepth==low&&w.unsealed==open)return;
        w.chamberCell=chamberCell;w.hinge=g.base().below();w.miter=true;w.planeX=a;w.planeZ=b;w.planeC=c;w.highDepth=high;w.lowDepth=low;w.unsealed=open;if(w.original==null)w.original=original;
        w.setChanged();l.sendBlockUpdated(p,w.getBlockState(),w.getBlockState(),2);
    }
    public double surfaceAt(double x,double z){int d=unsealed?highDepth:planeX*x+planeZ*z+planeC>=0?highDepth:lowDepth;return d==0?Double.NaN:d/16.0;}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.putBoolean("Miter",miter);n.putBoolean("ChamberCell",chamberCell);if(hinge!=null)n.put("Hinge",net.minecraft.nbt.NbtUtils.writeBlockPos(hinge));n.putDouble("PX",planeX);n.putDouble("PZ",planeZ);n.putDouble("PC",planeC);n.putInt("HighDepth",highDepth);n.putInt("LowDepth",lowDepth);n.putBoolean("Unsealed",unsealed);if(original!=null)n.put("Original",net.minecraft.nbt.NbtUtils.writeBlockState(original));n.putInt("Inside",inside);n.putInt("Outside",outside);n.putInt("Inward",inward.get2DDataValue());}
    @Override public void load(CompoundTag n){super.load(n);miter=n.getBoolean("Miter");chamberCell=n.getBoolean("ChamberCell");hinge=n.contains("Hinge")?net.minecraft.nbt.NbtUtils.readBlockPos(n.getCompound("Hinge")):null;planeX=n.getDouble("PX");planeZ=n.getDouble("PZ");planeC=n.getDouble("PC");highDepth=n.getInt("HighDepth");lowDepth=n.getInt("LowDepth");unsealed=n.getBoolean("Unsealed");original=n.contains("Original")?net.minecraft.nbt.NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),n.getCompound("Original")):null;inside=Math.max(0,Math.min(16,n.getInt("Inside")));outside=Math.max(0,Math.min(16,n.getInt("Outside")));inward=Direction.from2DDataValue(n.getInt("Inward"));}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
