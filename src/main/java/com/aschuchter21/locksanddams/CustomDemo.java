package com.aschuchter21.locksanddams;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID)
public final class CustomDemo {
    @SubscribeEvent public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("locks").requires(s->s.hasPermission(2)).then(Commands.literal("demo_custom")
            .executes(c->run(c.getSource(),6,11,4))
            .then(Commands.argument("width",IntegerArgumentType.integer(4,16)).then(Commands.argument("length",IntegerArgumentType.integer(3,47)).then(Commands.argument("lift",IntegerArgumentType.integer(1,16)).executes(c->run(c.getSource(),IntegerArgumentType.getInteger(c,"width"),IntegerArgumentType.getInteger(c,"length"),IntegerArgumentType.getInteger(c,"lift"))))))));
    }
    static int run(CommandSourceStack source,int w,int length,int lift) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player=source.getPlayerOrException();Direction f=player.getDirection();BlockPos base=player.blockPosition().above(4).relative(f,10);
        try {BlockPos controller=build(source.getLevel(),base,f,w,length,lift);source.sendSuccess(()->Component.literal("Custom lock built: "+w+" x "+length+", lift "+lift+". Controller "+controller.toShortString()+". Positive hinge RPM opens; negative closes. Valve levers are beside the inline pipe valves. Use /locks demo_desk for a wired control desk."),false);return 1;}
        catch(IllegalArgumentException error){source.sendFailure(Component.literal(error.getMessage()));return 0;}
    }
    public static BlockPos build(ServerLevel level,BlockPos base,Direction f,int w,int interior,int lift) {
        w+=2; // Include the two wall-mounted hinge columns around the navigable width.
        int len=interior+1,pool=(w+1)/2+3,top=lift+3;LockLayout grid=new LockLayout(base,f);
        WaterConnection.require((long)(w-2)*interior*(lift+1)<=16384,"Demo exceeds the chamber volume limit.");
        AABBCheck.check(level,grid,-5,-2,-pool,w,top+2,len+pool);
        var wall=Blocks.STONE_BRICKS.defaultBlockState();
        for(int x=-1;x<=w;x++)for(int z=-pool;z<=len+pool;z++) {
            level.setBlock(grid.at(x,-1,z),wall,2);
            if(z>len)for(int y=0;y<lift;y++)level.setBlock(grid.at(x,y,z),wall,2);
        }
        for(int x:new int[]{0,w-1})for(int z=-pool;z<=len+pool;z++)for(int y=0;y<=top;y++)level.setBlock(grid.at(x,y,z),wall,2);
        for(int z:new int[]{-pool,len+pool})for(int x=0;x<w;x++)for(int y=0;y<=top;y++)level.setBlock(grid.at(x,y,z),wall,2);
        for(int x=1;x<w-1;x++) {
            for(int z=-pool+1;z<0;z++)level.setBlock(grid.at(x,0,z),Blocks.WATER.defaultBlockState(),2);
            for(int z=len+1;z<len+pool;z++)level.setBlock(grid.at(x,lift,z),Blocks.WATER.defaultBlockState(),2);
            for(int y=0;y<lift;y++)level.setBlock(grid.at(x,y,len),wall,2);
            for(int y=0;y<lift+2;y++)level.setBlock(grid.at(x,y,0),Content.PANEL.get().defaultBlockState(),2);
            for(int y=lift;y<lift+3;y++)level.setBlock(grid.at(x,y,len),Content.PANEL.get().defaultBlockState(),2);
        }
        // Recesses sit in the approach walls, outside the managed chamber.
        for(int side=0;side<2;side++)for(int x:new int[]{0,w-1}){
            int bottom=side==0?0:lift,gateZ=side*len,reach=(x==0?w/2:w-w/2);
            for(int y=bottom;y<(side==0?lift+2:lift+3);y++){
                level.setBlock(grid.at(x,y,gateZ),Content.PANEL.get().defaultBlockState(),2);
                level.setBlock(grid.at(x==0?-1:w,y,gateZ),wall,2);
                for(int step=1;step<=reach;step++){
                    int z=gateZ+(side==0?-step:step);
                    level.setBlock(grid.at(x,y,z),y==bottom?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
                    level.setBlock(grid.at(x==0?-1:w,y,z),wall,2);
                }
            }
            for(int step=1;step<=reach;step++)level.setBlock(grid.at(x,bottom-1,gateZ+(side==0?-step:step)),wall,2);
        }
        for(int side=0;side<2;side++)for(int x:new int[]{0,w-1}) {
            int y=(side==0?0:lift)-1,z=side*len;
            level.setBlock(grid.at(x,y,z),Content.HINGE.get().defaultBlockState(),3);
            for(int sy=-1;sy<y;sy++)level.setBlock(grid.at(x,sy,z),com.simibubi.create.AllBlocks.ANDESITE_ENCASED_SHAFT.getDefaultState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Y),3);
            level.setBlock(grid.at(x,-2,z),com.simibubi.create.AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING,Direction.UP),3);
            ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)level.getBlockEntity(grid.at(x,-2,z))).generatedSpeed.setValue(-8);
        }
        BlockPos controller=grid.at(0,0,2),fill=grid.at(-3,lift-1,interior),drain=grid.at(-2,0,1);
        level.setBlock(controller,Content.CONTROLLER.get().defaultBlockState().setValue(ControllerBlock.FACING,f),2);
        // The chamber ports connect through separate external circuits to canal surface ports.
        for(int x=-5;x<=-1;x++)pipe(level,grid.at(x,lift-1,interior));pipe(level,grid.at(-1,lift,interior));
        for(int z=interior;z<=len+pool-1;z++)pipe(level,grid.at(-5,lift-1,z));
        pipe(level,grid.at(-5,lift,len+pool-1));for(int x=-5;x<0;x++)pipe(level,grid.at(x,lift,len+pool-1));
        for(int x=-3;x<0;x++)pipe(level,grid.at(x,0,1));
        for(int z=-pool+1;z<=1;z++)pipe(level,grid.at(-3,0,z));for(int x=-2;x<0;x++)pipe(level,grid.at(x,0,-pool+1));
        for(int[] p:new int[][]{{0,lift,interior},{0,0,1},{0,lift,len+pool-1},{0,0,-pool+1}})
            level.setBlock(grid.at(p[0],p[1],p[2]),Content.PORT.get().defaultBlockState().setValue(CulvertPortBlock.FACING,f.getClockWise()),2);
        for(BlockPos valve:new BlockPos[]{fill,drain}) {
            level.setBlock(valve,Content.INLINE.get().defaultBlockState().setValue(InlineValveBlock.AXIS,f.getClockWise().getAxis()),2);
            // A separate support keeps the lever clear of both pipe flanges.
            BlockPos support=valve.relative(f);level.setBlock(support,wall,2);
            level.setBlock(support.above(),Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE,AttachFace.FLOOR).setValue(LeverBlock.FACING,f),3);
        }
        for(int side=0;side<2;side++)for(int x=0;x<w;x++)for(int y=side==0?0:lift;y<(side==0?lift+2:lift+3);y++) {
            BlockPos p=grid.at(x,y,side*len);level.setBlock(p,GatePanelBlock.facing(level.getBlockState(p),side==0?f.getOpposite():f),2);
        }
        // Step the wall walkway down to each gate's deck, without touching the wet wall below.
        for(int side=0;side<2;side++)for(int x:new int[]{0,w-1}) {
            int gateZ=side*len,deck=lift+(side==0?2:3),steps=top+1-deck;
            for(int dz=-steps;dz<=steps;dz++) {
                int y=deck+Math.max(0,Math.abs(dz)-1);
                for(int clear=y;clear<=top;clear++)level.setBlock(grid.at(x,clear,gateZ+dz),Blocks.AIR.defaultBlockState(),2);
                if(dz!=0)level.setBlock(grid.at(x,y,gateZ+dz),Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,dz>0?f:f.getOpposite()),2);
            }
        }
        // The approved handrails need headroom throughout each approach-wall pocket.
        for(int side=0;side<2;side++)for(int x:new int[]{0,w-1}) {
            int deck=lift+(side==0?2:3),reach=x==0?w/2:w-w/2;
            for(int step=0;step<=reach;step++)for(int y=deck;y<=deck+1;y++)
                level.setBlock(grid.at(x,y,side*len+(side==0?-step:step)),Blocks.AIR.defaultBlockState(),2);
        }
        LockEntity lock=(LockEntity)level.getBlockEntity(controller);WaterConnection.require(lock.assemble(),"Demo assembly: "+lock.status());return controller;
    }
    static void pipe(ServerLevel level,BlockPos p){level.setBlock(p,Content.PIPE.get().defaultBlockState(),2);}
    static void lever(ServerLevel level,BlockPos p,Direction face){level.setBlock(p,Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE,AttachFace.WALL).setValue(LeverBlock.FACING,face),3);}
    static class AABBCheck {
        static void check(ServerLevel level,LockLayout g,int x0,int y0,int z0,int x1,int y1,int z1) {
            for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++) {
                BlockPos p=g.at(x,y,z);WaterConnection.require(level.isInWorldBounds(p)&&level.getWorldBorder().isWithinBounds(p)&&level.hasChunkAt(p)&&level.getBlockState(p).isAir(),"Demo needs loaded, empty space within world bounds at "+p.toShortString());
            }
            WaterConnection.require(level.getEntities(null,g.box(x0,y0,z0,x1,y1,z1)).isEmpty(),"Clear entities from the demo footprint.");
        }
    }
}
