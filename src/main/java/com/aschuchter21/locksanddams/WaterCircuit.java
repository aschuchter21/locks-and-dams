package com.aschuchter21.locksanddams;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import static com.aschuchter21.locksanddams.WaterConnection.require;

/** Two terminal ports with exactly one inline valve. No bypass around that valve is allowed. */
public record WaterCircuit(BlockPos chamber,BlockPos valve,WaterConnection canal) {
    static boolean accepts(BlockState s,Direction face){
        return s.is(Content.PIPE.get())||s.is(Content.INLINE.get())&&s.getValue(InlineValveBlock.AXIS)==face.getAxis()
            ||s.is(Content.PORT.get())&&s.getValue(CulvertPortBlock.FACING).getOpposite()==face;
    }
    static Set<BlockPos> walk(Level level,BlockPos start,BlockPos blocked){
        Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();queue.add(start);
        while(!queue.isEmpty()){
            BlockPos p=queue.removeFirst();if(p.equals(blocked)||!seen.add(p))continue;
            require(seen.size()<=256&&p.distManhattan(start)<=96,"Culvert exceeds 256 blocks or 96-block reach.");
            require(level.hasChunkAt(p),"Load the entire culvert.");var s=level.getBlockState(p);
            for(Direction d:Direction.values())if(accepts(s,d)){
                BlockPos q=p.relative(d);require(level.hasChunkAt(q),"Load the entire culvert.");
                if(accepts(level.getBlockState(q),d.getOpposite()))queue.add(q);
            }
        }
        return seen;
    }
    public static WaterCircuit discover(Level level,BlockPos chamber){
        require(level.getBlockState(chamber).is(Content.PORT.get()),"Restore chamber port at "+chamber.toShortString());
        var seen=walk(level,chamber,null);BlockPos valve=null,canal=null;
        for(BlockPos p:seen){var s=level.getBlockState(p);
            if(s.is(Content.INLINE.get())){require(valve==null,"Use exactly one inline valve per circuit.");valve=p;}
            if(s.is(Content.PORT.get())&&!p.equals(chamber)){require(canal==null,"Each circuit needs exactly two ports.");canal=p;}
        }
        require(valve!=null&&canal!=null,"Connect chamber port to canal port through one inline valve; connect to each port's back and both valve flanges.");
        var axis=level.getBlockState(valve).getValue(InlineValveBlock.AXIS);
        for(Direction d:Direction.values())if(d.getAxis()==axis)require(seen.contains(valve.relative(d)),"Connect pipe to both inline valve flanges.");
        require(!walk(level,chamber,valve).contains(canal),"Remove the pipe bypass around the inline valve.");
        BlockPos mouth=canal.relative(level.getBlockState(canal).getValue(CulvertPortBlock.FACING));
        require(level.hasChunkAt(mouth)&&level.hasChunkAt(mouth.above()),"Load the canal beside the port.");var fluid=level.getFluidState(mouth);
        require(fluid.is(FluidTags.WATER)&&fluid.isSource()&&!fluid.is(Content.WATER.get())&&level.getFluidState(mouth.above()).isEmpty(),"Place the canal port mouth at the source-water surface.");
        if(!level.isClientSide)for(BlockPos p:seen){var s=level.getBlockState(p);if(s.is(Content.PIPE.get())){var n=CulvertPipeBlock.connected(s,level,p);if(!n.equals(s))level.setBlock(p,n,2);}}
        return new WaterCircuit(chamber,valve,new WaterConnection(canal,mouth,mouth.getY()*16+Math.round(fluid.getHeight(level,mouth)*16)));
    }
    public void mark(Level level,boolean fill){
        if(level.isClientSide)return;
        for(BlockPos p:new BlockPos[]{chamber,canal.port()}){var s=level.getBlockState(p);int role=p.equals(chamber)?(fill?2:1):(fill?1:2);var n=s.setValue(CulvertPortBlock.ROLE,role);if(!n.equals(s))level.setBlock(p,n,2);}
    }
}
