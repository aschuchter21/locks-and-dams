package com.aschuchter21.locksanddams;

import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID)
public final class DemoLock {
    public static final int FLAGS=Block.UPDATE_CLIENTS;
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("locks").requires(s -> s.hasPermission(2))
            .then(Commands.literal("demo").executes(ctx -> {
                var player=ctx.getSource().getPlayerOrException();
                LockLayout layout=new LockLayout(player.blockPosition().above(4).relative(player.getDirection(),6),player.getDirection());
                String error=build(ctx.getSource().getLevel(),layout);
                if(error!=null) { ctx.getSource().sendFailure(Component.literal(error)); return 0; }
                ctx.getSource().sendSuccess(() -> Component.literal("Demo lock created. Controller: "+layout.origin().toShortString()+". Valve levers: fill and drain. Positive hinge RPM opens; negative closes. Right-click controller for status. Enter either pool with your boat."),false);
                return 1;
            })));
    }
    public static String build(ServerLevel level, LockLayout l) {
        // Check the ENTIRE footprint, including both pools and the control walkway, before writing.
        for(int x=-4;x<=6;x++) for(int z=-6;z<=16;z++) for(int y=-3;y<=6;y++) {
            BlockPos p=l.at(x,y,z);
            if(p.getY()<level.getMinBuildHeight()||p.getY()>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(p)) return "Demo would exceed the world's build bounds.";
            if(!level.hasChunkAt(p)) return "Demo area must be loaded.";
            if(!level.getBlockState(p).isAir()) return "Demo needs clear air; blocked at "+p.toShortString()+". Move to an open area or fly higher.";
        }
        if(!level.getEntities(null,l.box(-4,-3,-6,6,6,16)).isEmpty()) return "Clear entities from the demo area first.";
        BlockState wall=Blocks.STONE_BRICKS.defaultBlockState(), water=Blocks.WATER.defaultBlockState();
        for(int x=0;x<=6;x++) for(int z=-6;z<=16;z++) {
            level.setBlock(l.at(x,-1,z),wall,FLAGS);
        }
        for(int x : new int[]{0,6}) for(int z=-6;z<=16;z++) for(int y=0;y<6;y++) level.setBlock(l.at(x,y,z),wall,FLAGS);
        for(int z : new int[]{-6,16}) for(int x=1;x<=5;x++) for(int y=0;y<6;y++) level.setBlock(l.at(x,y,z),wall,FLAGS);
        for(int x=1;x<=5;x++) {
            for(int z=-5;z<0;z++) level.setBlock(l.at(x,0,z),water,FLAGS);
            for(int z=11;z<16;z++) for(int y=0;y<=3;y++) level.setBlock(l.at(x,y,z),water,FLAGS);
            for(int z : new int[]{0,10}) for(int y=0;y<6;y++) level.setBlock(l.at(x,y,z),Content.PANEL.get().defaultBlockState(),FLAGS);
        }
        for(int x=-2;x<=-1;x++) for(int z=0;z<=10;z++) level.setBlock(l.at(x,0,z),wall,FLAGS);
        level.setBlock(l.origin(),Content.CONTROLLER.get().defaultBlockState().setValue(ControllerBlock.FACING,l.forward()),FLAGS);
        level.setBlock(l.lowerDrive(),wall,FLAGS);
        level.setBlock(l.upperDrive(),wall,FLAGS);
        level.setBlock(l.fill(),Content.FILL.get().defaultBlockState(),FLAGS);
        level.setBlock(l.drain(),Content.DRAIN.get().defaultBlockState(),FLAGS);
        for(int z : new int[]{3,7}) level.setBlock(l.at(-1,1,z),Blocks.LEVER.defaultBlockState()
            .setValue(LeverBlock.FACE,AttachFace.WALL).setValue(LeverBlock.FACING,l.forward().getCounterClockWise()),Block.UPDATE_ALL);
        plumbing(level,l);
        for(int z:new int[]{0,10}) {
            level.setBlock(l.at(1,-2,z),Content.HINGE.get().defaultBlockState(),Block.UPDATE_ALL);
            level.setBlock(l.at(1,-3,z),com.simibubi.create.AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING,Direction.UP),Block.UPDATE_ALL);
            if(level.getBlockEntity(l.at(1,-3,z)) instanceof com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity motor)motor.generatedSpeed.setValue(-8);
        }
        if(!(level.getBlockEntity(l.origin()) instanceof LockEntity lock)||!lock.assemble()) return "Demo created, but assembly failed; inspect its controller.";
        return null;
    }
    public static void plumbing(ServerLevel level,LockLayout l) {
        // Supply dives below the walkway so it cannot connect to the drain route.
        pipe(level,l,0,0,3);pipe(level,l,0,-1,3);pipe(level,l,0,-2,3);
        for(int x=-4;x<=0;x++)pipe(level,l,x,-2,3);
        for(int z=3;z<=12;z++)pipe(level,l,-4,-2,z);
        for(int y=-2;y<=3;y++)pipe(level,l,-4,y,12);
        for(int x=-4;x<0;x++)pipe(level,l,x,3,12);
        for(int x=-2;x<=0;x++)pipe(level,l,x,0,7);
        for(int z=-3;z<=7;z++)pipe(level,l,-2,0,z);
        pipe(level,l,-1,0,-3);
        for(int[] p:new int[][]{{0,3,12},{0,0,-3}})level.setBlock(l.at(p[0],p[1],p[2]),Content.PORT.get().defaultBlockState()
            .setValue(CulvertPortBlock.FACING,l.forward().getClockWise()),FLAGS);
    }
    private static void pipe(ServerLevel level,LockLayout l,int x,int y,int z) { level.setBlock(l.at(x,y,z),Content.PIPE.get().defaultBlockState(),FLAGS); }
}
