package com.aschuchter21.locksanddams;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.simibubi.create.AllBlocks;

/** A wired example: real Create clutches and gearshifts respond to gate terminals. */
@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID)
public final class DeskDemo {
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("locks").requires(s->s.hasPermission(2)).then(Commands.literal("demo_desk").executes(c->{
            var player=c.getSource().getPlayerOrException();BlockPos base=player.blockPosition().above(6).relative(player.getDirection(),12);
            try{BlockPos desk=build(c.getSource().getLevel(),base,player.getDirection(),player);c.getSource().sendSuccess(()->Component.literal("Wired control-desk demo built at "+desk.toShortString()+". Selectors are on the sloped top; the red button is emergency stop."),false);return 1;}
            catch(IllegalArgumentException error){c.getSource().sendFailure(Component.literal(error.getMessage()));return 0;}
        })));
    }
    public static BlockPos build(ServerLevel level,BlockPos base,Direction forward,net.minecraft.world.entity.player.Player player){
        LockLayout grid=new LockLayout(base,forward);
        CustomDemo.AABBCheck.check(level,grid,-7,-4,-7,8,8,20);
        BlockPos controller=CustomDemo.build(level,base,forward,6,11,4);
        BlockPos desk=grid.at(-6,1,6);var deskState=Content.DESK.get().defaultBlockState().setValue(ControlDeskBlock.FACING,forward.getCounterClockWise());
        for(int part=0;part<3;part++){
            BlockPos p=desk.relative(deskState.getValue(ControlDeskBlock.FACING).getCounterClockWise(),part-1);
            level.setBlock(p.below(),Blocks.STONE_BRICKS.defaultBlockState(),3);level.setBlock(p,deskState.setValue(ControlDeskBlock.PART,part),3);
        }
        var deskEntity=(ControlDeskEntity)level.getBlockEntity(desk);
        if(player!=null)WaterConnection.require(deskEntity.link(controller,player),"Could not link desk.");
        else deskEntity.linkForDemo(controller);
        for(int side=0;side<2;side++)for(int x:new int[]{0,7}){
            int z=side*12;BlockPos motor=grid.at(x,-4,z),clutch=grid.at(x,-3,z),gear=grid.at(x,-2,z);
            level.setBlock(gear,AllBlocks.GEARSHIFT.getDefaultState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Y),3);
            level.setBlock(clutch,AllBlocks.CLUTCH.getDefaultState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Y),3);
            level.setBlock(motor,AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING,Direction.UP),3);
            ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)level.getBlockEntity(motor)).generatedSpeed.setValue(8);
            Direction approach=side==0?forward.getOpposite():forward;BlockPos node=clutch.relative(approach);
            level.setBlock(node,Content.NODE.get().defaultBlockState().setValue(GateControlNodeBlock.FACING,approach.getOpposite()).setValue(GateControlNodeBlock.UPPER,side==1),3);
            ((GateControlNodeEntity)level.getBlockEntity(node)).link(controller);
            level.setBlock(node.above(),Blocks.STONE_BRICKS.defaultBlockState(),3);
            level.updateNeighborsAt(node,Content.NODE.get());level.updateNeighborsAt(clutch,Content.NODE.get());
        }
        // The chamber starts low: the first water-selector click should request FILL.
        var lock=(LockEntity)level.getBlockEntity(controller);
        lock.deskControl().waterSelection=DeskControl.DRAIN;lock.deskChanged();deskEntity.signalChanged();
        return desk;
    }
}
