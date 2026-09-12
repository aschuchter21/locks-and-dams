package com.aschuchter21.locksanddams;

import net.minecraft.core.*;
import net.minecraft.nbt.*;

/** A rectangular leaf, starting directly above its pivot and extending inward. */
public record GateSpec(BlockPos base,Direction inward,int width,int height,float openAngle) {
    public BlockPos at(int x,int y) {return base.relative(inward,x).above(y);}
    public Direction.Axis normal() {return inward.getClockWise().getAxis();}
    public CompoundTag save() {
        CompoundTag n=new CompoundTag();n.put("Base",NbtUtils.writeBlockPos(base));n.putInt("Inward",inward.get2DDataValue());
        n.putInt("Width",width);n.putInt("Height",height);n.putFloat("OpenAngle",openAngle);return n;
    }
    public static GateSpec load(CompoundTag n) {return new GateSpec(NbtUtils.readBlockPos(n.getCompound("Base")),Direction.from2DDataValue(n.getInt("Inward")),n.getInt("Width"),n.getInt("Height"),n.getFloat("OpenAngle"));}
}
