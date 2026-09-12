package com.aschuchter21.locksanddams;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;

/** Bounded, read-only routing. Unloaded or ambiguous plumbing always pauses the valve. */
public final class CulvertRoute {
    public static String validate(Level level,LockLayout l,boolean fill) {
        BlockPos valve=fill?l.fill():l.drain();
        BlockPos start=valve.below();
        Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();queue.add(start);
        int ports=0;
        while(!queue.isEmpty()) {
            BlockPos p=queue.removeFirst();if(!seen.add(p))continue;
            if(seen.size()>128||p.distManhattan(valve)>32)return "Culvert exceeds 128 blocks or 32-block reach.";
            if(!level.hasChunkAt(p))return "Culvert must be loaded.";
            var state=level.getBlockState(p);
            if(!state.is(Content.PIPE.get())&&!state.is(Content.PORT.get()))return "Connect a culvert pipe directly below the valve.";
            if(state.is(Content.PORT.get())) {
                Direction facing=state.getValue(CulvertPortBlock.FACING);
                if(!seen.contains(p.relative(facing.getOpposite())))return "Connect the back of the culvert port to its pipe.";
                BlockPos water=p.relative(facing);int surface=l.origin().getY()+(fill?3:0);
                if(water.getY()>surface||water.getY()<l.origin().getY()-8)return "Port mouth is above its canal or too deep.";
                // Ports must terminate outside the chamber, on the appropriate approach.
                int along=(water.getX()-l.origin().getX())*l.forward().getStepX()+(water.getZ()-l.origin().getZ())*l.forward().getStepZ();
                if(fill?along<=10:along>=0)return "Route the port to the "+(fill?"upper":"lower")+" canal.";
                for(int y=water.getY();y<=surface;y++) {
                    BlockPos q=new BlockPos(water.getX(),y,water.getZ());
                    if(!level.hasChunkAt(q)||!level.getFluidState(q).is(FluidTags.WATER)||!level.getFluidState(q).isSource())return "Port mouth needs a continuous canal source-water column.";
                }
                if(!level.getFluidState(new BlockPos(water.getX(),surface+1,water.getZ())).isEmpty())return "Port canal surface has the wrong height.";
                ports++;continue;
            }
            for(Direction d:Direction.values()) {
                BlockPos next=p.relative(d);if(next.equals(valve))continue;
                if(!level.hasChunkAt(next))return "Culvert must be loaded.";
                var n=level.getBlockState(next);
                if(n.is(Content.FILL.get())||n.is(Content.DRAIN.get()))return "Keep fill and drain culverts separate.";
                if(n.is(Content.PIPE.get())||n.is(Content.PORT.get()))queue.add(next);
            }
        }
        return ports==1?null:"Each valve needs exactly one canal port.";
    }
}
