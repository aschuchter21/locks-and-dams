package com.aschuchter21.locksanddams;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.*;
public final class PlumbingChecks {
    static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    static void rejected(Runnable task,String message){try{task.run();}catch(IllegalArgumentException expected){return;}throw new IllegalStateException(message);}
    static BlockPos p(BlockPos base,int[] v){return base.offset(v[0],v[1],v[2]);}
    static void run(ServerLevel level){
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        BlockPos site=new BlockPos(130,250,130);
        for(Direction direction:Direction.Plane.HORIZONTAL){
            player.setYRot(direction.toYRot());player.setXRot(0);player.setPos(130,250,130);
            var context=new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(Content.PANEL.get()),new BlockHitResult(Vec3.atCenterOf(site.below()),Direction.UP,site.below(),false));
            var state=Content.PANEL.get().getStateForPlacement(context);var expected=GatePanelBlock.facing(Content.PANEL.get().defaultBlockState(),direction.getOpposite());
            check(state.equals(expected),"Canal face does not face placing player: "+direction);
            level.setBlock(site.below(),GatePanelBlock.facing(Content.PANEL.get().defaultBlockState(),direction),3);
            check(Content.PANEL.get().getStateForPlacement(context).equals(expected),"Neighbor overrides player-facing placement");
        }
        level.setBlock(site.below(),Blocks.AIR.defaultBlockState(),3);
        int index=0;
        for(Direction.Axis axis:Direction.Axis.values()){
            BlockPos base=site.above(5+index++*5);
            int[][] path=axis==Direction.Axis.Y?new int[][]{{0,0,0},{1,0,0},{2,0,0},{2,1,0},{2,2,0},{3,2,0},{4,2,0}}:
                axis==Direction.Axis.X?new int[][]{{0,0,0},{1,0,0},{2,0,0},{3,0,0},{4,0,0}}:new int[][]{{0,0,0},{0,0,1},{0,0,2},{0,0,3},{0,0,4}};
            Direction end=axis==Direction.Axis.Z?Direction.SOUTH:Direction.EAST;
            for(int[] v:path)level.setBlock(p(base,v),Content.PIPE.get().defaultBlockState(),3);
            BlockPos canal=p(base,path[path.length-1]),valve=p(base,path[axis==Direction.Axis.Y?3:2]);
            level.setBlock(base,Content.PORT.get().defaultBlockState().setValue(CulvertPortBlock.FACING,end.getOpposite()),3);
            level.setBlock(canal,Content.PORT.get().defaultBlockState().setValue(CulvertPortBlock.FACING,end),3);
            level.setBlock(canal.relative(end),Blocks.WATER.defaultBlockState(),3);
            var valveState=Content.INLINE.get().defaultBlockState().setValue(InlineValveBlock.AXIS,axis);level.setBlock(valve,valveState,3);
            var circuit=WaterCircuit.discover(level,base);check(circuit.valve().equals(valve)&&circuit.canal().port().equals(canal),"Valid circuit rejected on "+axis);
            level.setBlock(valve,Blocks.AIR.defaultBlockState(),3);rejected(()->WaterCircuit.discover(level,base),"Missing valve accepted");
            level.setBlock(valve,valveState.setValue(InlineValveBlock.AXIS,axis==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X),3);rejected(()->WaterCircuit.discover(level,base),"Wrong flange orientation accepted");level.setBlock(valve,valveState,3);
            if(axis==Direction.Axis.X){
                for(int x=1;x<=3;x++)level.setBlock(base.offset(x,0,1),Content.PIPE.get().defaultBlockState(),3);
                rejected(()->WaterCircuit.discover(level,base),"Bypass around valve accepted");
                for(int x=1;x<=3;x++)level.setBlock(base.offset(x,0,1),Blocks.AIR.defaultBlockState(),3);
            }
            for(int[] v:path)level.setBlock(p(base,v),Blocks.AIR.defaultBlockState(),3);level.setBlock(canal.relative(end),Blocks.AIR.defaultBlockState(),3);
        }
    }
}
