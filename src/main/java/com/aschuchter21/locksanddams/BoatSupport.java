package com.aschuchter21.locksanddams;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Supplements vanilla buoyancy as a surface crosses a whole-block boundary. */
public final class BoatSupport {
    public static void inside(Level level, Entity entity) {
        if(!(entity instanceof Boat boat)||!boat.isControlledByLocalInstance()) return;
        // A hull can intersect several water cells. Correct once per boat tick, including
        // recursive block-collision callbacks from move(). Do not fight a remote driver.
        var data=boat.getPersistentData();
        String key="locksanddams_support_tick";
        if(data.contains(key)&&data.getInt(key)==boat.tickCount) return;
        BlockPos base=BlockPos.containing(boat.getX(),boat.getY()-.5,boat.getZ());
        double surface=Double.NaN;
        for(int i=0;i<8;i++) {
            BlockPos p=base.above(i);
            if(!level.hasChunkAt(p)) return;
            var fluid=level.getFluidState(p);
            if(fluid.getType()==Content.WATER.get()) surface=p.getY()+fluid.getOwnHeight();
            else if(!Double.isNaN(surface)) break;
        }
        if(Double.isNaN(surface)||boat.getY()>surface+.3) return;
        data.putInt(key,boat.tickCount);
        double delta=Mth.clamp(surface-.36-boat.getY(),-.125,.125);
        boat.resetFallDistance();
        boat.move(MoverType.SELF,new Vec3(0,delta,0));
        Vec3 velocity=boat.getDeltaMovement();
        boat.setDeltaMovement(velocity.x,0,velocity.z);
        boat.resetFallDistance();
    }
}
