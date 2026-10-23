package com.aschuchter21.locksanddams;

import net.minecraft.core.*;
import net.minecraft.nbt.*;

/** A rectangular leaf, starting directly above its pivot and extending inward. */
public record GateSpec(BlockPos base,Direction inward,int width,int height,float openAngle,Direction upstream) {
    public GateSpec(BlockPos base,Direction inward,int width,int height,float openAngle){this(base,inward,width,height,openAngle,null);}
    public boolean mitered(){return upstream!=null;}
    public double span(){return width-.5;}
    public double inset(){return mitered()?1:0;}
    public double length(){return Math.hypot(span(),inset());}
    public double deckStart(){return mitered()?(.5*length()+.48)/span()+.02:0;}
    public double stretch(){return length()/span();}
    public float closedAngle(){
        if(!mitered())return 0;
        var rotated=net.createmod.catnip.math.VecHelper.rotate(net.minecraft.world.phys.Vec3.atLowerCornerOf(new BlockPos(inward.getNormal())),90,Direction.Axis.Y);
        return (float)Math.copySign(Math.toDegrees(Math.atan2(inset(),span())),rotated.x*upstream.getStepX()+rotated.z*upstream.getStepZ());
    }
    public BlockPos at(int x,int y) {return base.relative(inward,x).above(y);}
    public Direction.Axis normal() {return inward.getClockWise().getAxis();}
    public CompoundTag save() {
        CompoundTag n=new CompoundTag();n.put("Base",NbtUtils.writeBlockPos(base));n.putInt("Inward",inward.get2DDataValue());
        n.putInt("Width",width);n.putInt("Height",height);n.putFloat("OpenAngle",openAngle);if(upstream!=null)n.putInt("Upstream",upstream.get2DDataValue());return n;
    }
    public static GateSpec load(CompoundTag n) {return new GateSpec(NbtUtils.readBlockPos(n.getCompound("Base")),Direction.from2DDataValue(n.getInt("Inward")),n.getInt("Width"),n.getInt("Height"),n.getFloat("OpenAngle"),n.contains("Upstream")?Direction.from2DDataValue(n.getInt("Upstream")):null);}
}
