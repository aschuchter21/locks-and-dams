package com.aschuchter21.locksanddams;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;

/** Finds a single surface port without loading chunks or changing canal water. */
public record WaterConnection(BlockPos port,BlockPos mouth,int surface) {
    public static WaterConnection discover(Level level,BlockPos valve) {
        Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();queue.add(valve.below());
        WaterConnection result=null;
        while(!queue.isEmpty()) {
            BlockPos p=queue.removeFirst();if(!seen.add(p))continue;
            require(seen.size()<=256&&p.distManhattan(valve)<=96,"Culvert exceeds 256 blocks or 96-block reach.");
            require(level.hasChunkAt(p),"Load the entire culvert.");var s=level.getBlockState(p);
            require(s.is(Content.PIPE.get())||s.is(Content.PORT.get()),"Place Culvert Pipe directly below each valve.");
            if(s.is(Content.PORT.get())) {
                Direction d=s.getValue(CulvertPortBlock.FACING);require(seen.contains(p.relative(d.getOpposite())),"Connect pipe to the back of the port at "+p.toShortString()+". Valves need pipe beneath them, not a port.");
                require(result==null,"Each valve needs exactly one port.");BlockPos water=p.relative(d);
                require(level.hasChunkAt(water),"Load the canal beside the port.");var fluid=level.getFluidState(water);
                require(fluid.is(FluidTags.WATER)&&fluid.isSource()&&!fluid.is(Content.WATER.get())&&level.getFluidState(water.above()).isEmpty(),"Place the port mouth at the canal source-water surface.");
                result=new WaterConnection(p,water,water.getY()*16+Math.round(fluid.getHeight(level,water)*16));continue;
            }
            for(Direction d:Direction.values()) {
                BlockPos next=p.relative(d);if(next.equals(valve))continue;
                require(level.hasChunkAt(next),"Load the entire culvert.");var n=level.getBlockState(next);
                require(!n.is(Content.FILL.get())&&!n.is(Content.DRAIN.get()),"Keep fill and drain culverts separate.");
                if(n.is(Content.PIPE.get())||n.is(Content.PORT.get()))queue.add(next);
            }
        }
        require(result!=null,"Connect each culvert to a canal surface port.");
        if(!level.isClientSide) {
            for(BlockPos p:seen) {
                var state=level.getBlockState(p);
                if(state.is(Content.PIPE.get())) {
                    var next=CulvertPipeBlock.connected(state,level,p);
                    if(!next.equals(state))level.setBlock(p,next,2);
                }
            }
            var state=level.getBlockState(result.port());
            var next=state.setValue(CulvertPortBlock.ROLE,level.getBlockState(valve).is(Content.FILL.get())?1:2);
            if(!next.equals(state))level.setBlock(result.port(),next,2);
        }
        return result;
    }
    static void require(boolean ok,String message) {if(!ok)throw new IllegalArgumentException(message);}
}
