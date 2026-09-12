package com.aschuchter21.locksanddams;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID)
public final class DeskChecks {
    static class Fixture {BlockPos desk;LockEntity lock;CustomLock c;float angle;int water;int firstOpen=-1,hornStart=-1,hornLength;Fixture(BlockPos p,LockEntity l){desk=p;lock=l;c=CustomLock.load(l.getBlockPos(),l.saveWithoutMetadata().getCompound("CustomLock"));}}
    static MinecraftServer server;static ServerLevel level;static int tick;static boolean done;static List<Fixture> fixtures=new ArrayList<>();
    static void check(boolean ok,String msg){if(!ok)throw new IllegalStateException("tick "+tick+": "+msg);}
    static void finish(Throwable error){done=true;try{Files.writeString(Path.of("desk-checks-result.txt"),error==null?"PASS: four orientations; real Create clutches and gearshifts; signed gate motion; latched emergency stop during movement and flow; reset without restart; 200-tick warning and restart; independent alarm/horn outputs; 80-tick departure delay and 40-tick pulse; full fill/drain; desk removal stops and unlinks.\n":"FAIL: "+error+"\n");}catch(Exception ignored){}if(error!=null)error.printStackTrace();server.halt(false);}
    @SubscribeEvent public static void start(ServerStartedEvent e){
        if(!Boolean.getBoolean("locksanddams.deskChecks"))return;server=e.getServer();level=server.overworld();
        try{int index=0;for(Direction f:Direction.Plane.HORIZONTAL){BlockPos base=new BlockPos(100+160*index++,200,100);for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)level.setChunkForced((base.getX()>>4)+x,6+z,true);BlockPos desk=DeskDemo.build(level,base,f,null);fixtures.add(new Fixture(desk,((ControlDeskEntity)level.getBlockEntity(desk)).controller()));}}catch(Throwable error){finish(error);}
    }
    static int beneathSignal(Fixture f,boolean alarm){Direction out=level.getBlockState(f.desk).getValue(ControlDeskBlock.FACING).getCounterClockWise();if(alarm)out=out.getOpposite();BlockPos dust=f.desk.relative(out).below(2);return level.getBlockState(dust).getValue(RedStoneWireBlock.POWER);}
    static void gate(Fixture f,int side,int command){f.lock.deskControl().selectGate(side,command);f.lock.deskChanged();}
    static void water(Fixture f,int mode){f.lock.deskControl().selectWater(mode,level.getGameTime());f.lock.deskChanged();((ControlDeskEntity)level.getBlockEntity(f.desk)).signalChanged();}
    static int endSignal(Fixture f,boolean alarm){Direction out=level.getBlockState(f.desk).getValue(ControlDeskBlock.FACING).getCounterClockWise();if(alarm)out=out.getOpposite();return level.getBestNeighborSignal(f.desk.relative(out,2));}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(server==null||done||e.phase!=TickEvent.Phase.END)return;tick++;
        try{for(Fixture f:fixtures){var c=f.c;var controls=f.lock.deskControl();var desk=(ControlDeskEntity)level.getBlockEntity(f.desk);
            if(tick==2){Direction out=level.getBlockState(f.desk).getValue(ControlDeskBlock.FACING).getCounterClockWise();for(int end:new int[]{-1,1}){BlockPos dust=f.desk.relative(out,end).below(2);level.setBlock(dust.below(),Blocks.STONE_BRICKS.defaultBlockState(),3);level.setBlock(dust,Blocks.REDSTONE_WIRE.defaultBlockState(),3);}}
            if(tick==10){check(f.lock.gateLamp(0)==1&&f.lock.gateLamp(1)==1,"Initial gates not closed");for(int i=0;i<4;i++)check(Math.abs(c.hinge(level,i).getSpeed())<.01,"Clutch did not stop initial rotation: "+c.hinge(level,i).getSpeed());}
            if(tick==20)gate(f,0,DeskControl.OPEN);
            if(tick==40){check(f.lock.gateLamp(0)==3,"Create clutch/gearshift did not move lower gate: "+c.hinge(level,0).getSpeed()+" / "+c.hinge(level,0).gateAngle()+" / "+c.hinge(level,0).obstruction());controls.stop();f.lock.deskChanged();f.angle=c.hinge(level,0).gateAngle();}
            if(tick==90){check(c.hinge(level,0).gateAngle()==f.angle&&controls.stopped,"Emergency stop failed to hold gate");controls.reset();f.lock.deskChanged();}
            if(tick==110){check(c.hinge(level,0).gateAngle()==f.angle,"Reset restarted gate");gate(f,0,DeskControl.CLOSE);}
            if(tick==200)check(f.lock.gateLamp(0)==1,"Reverse gearshift did not close gate: "+c.hinge(level,0).getSpeed()+" "+c.hinge(level,0).gateAngle());
            if(tick==210)gate(f,0,DeskControl.OPEN);
            if(tick>210&&tick<500){
                if(f.lock.gateLamp(0)==2&&f.firstOpen<0)f.firstOpen=tick;
                if(desk.output(false)>0){check(beneathSignal(f,false)==15,"Horn did not power dust through supporting block");if(f.hornStart<0)f.hornStart=tick;f.hornLength++;check(f.firstOpen>=0&&tick-f.firstOpen>=80,"Horn sounded early");check(endSignal(f,false)==15&&endSignal(f,true)==0,"Horn did not reach its independent end contact");}
            }
            if(tick==490){check(f.firstOpen>0&&f.hornStart-f.firstOpen>=80&&f.hornStart-f.firstOpen<=82&&f.hornLength==40,"Horn timing: opened="+f.firstOpen+" horn="+f.hornStart+" length="+f.hornLength);}
            if(tick==500)gate(f,0,DeskControl.CLOSE);
            if(tick==640){check(f.lock.gateLamp(0)==1,"Lower gate not closed for filling");f.water=f.lock.waterUnits();water(f,DeskControl.FILL);}
            if(tick>640&&tick<840)check(f.lock.waterUnits()==f.water,"Water moved during initial warning");
            if(tick==800)water(f,DeskControl.DRAIN);
            if(tick==900)water(f,DeskControl.FILL);
            if(tick>=900&&tick<1100){check(f.lock.waterUnits()==f.water,"Water moved before restarted 10-second warning");check(f.lock.deskOutput(Direction.DOWN)==15&&desk.output(true)==15,"Warning output missing");check(beneathSignal(f,true)==15&&beneathSignal(f,false)==0,"Alarm did not reach dust under supporting block");check(endSignal(f,true)==15&&endSignal(f,false)==0,"Alarm did not reach its independent end contact");check(level.getSignal(f.lock.getBlockPos(),Direction.UP)==15,"Controller bottom alarm missing");}
            if(tick==1130){
                var lower=(GateWaterEntity)level.getBlockEntity(c.leaves[0].base());var upper=(GateWaterEntity)level.getBlockEntity(c.leaves[2].base());
                check(lower!=null&&lower.inside==LockLayout.depth(f.lock.waterUnits(),0)&&lower.outside==LockLayout.depth(c.low,0),"Closed lower gate water does not match both levels");
                check(upper!=null&&upper.outside==LockLayout.depth(c.high,c.rise)&&upper.inside==LockLayout.depth(f.lock.waterUnits(),c.rise),"Closed upper gate water does not match canal and chamber");
                check(level.getFluidState(c.leaves[0].base()).isEmpty(),"Closed gate joined fluid volumes");
            }
            if(tick==1120){check(f.lock.waterUnits()>f.water&&desk.output(true)==0,"Fill did not begin after warning");}
            if(tick==1140){controls.stop();f.lock.deskChanged();f.water=f.lock.waterUnits();}
            if(tick==1180){check(f.lock.waterUnits()==f.water&&desk.output(true)==0&&desk.output(false)==0,"Emergency stop did not stop flow/outputs");controls.reset();f.lock.deskChanged();}
            if(tick==1200)check(f.lock.waterUnits()==f.water,"Reset restarted flow");
            if(tick==1210)water(f,DeskControl.FILL);
            if(tick==1800)check(f.lock.waterUnits()==c.high,"Fill did not reach upper target: "+f.lock.status());
            if(tick==1810)gate(f,1,DeskControl.OPEN);
            if(tick==1950)check(f.lock.gateLamp(1)==2,"Upper gate did not open");
            if(tick==2110)gate(f,1,DeskControl.CLOSE);
            if(tick==2250){check(f.lock.gateLamp(1)==1,"Upper gate did not close");water(f,DeskControl.DRAIN);}
            if(tick==2820)check(f.lock.waterUnits()==c.low,"Drain did not reach lower target");
            if(tick==2821){water(f,DeskControl.FILL);controls.stop();f.lock.deskChanged();desk.signalChanged();check(endSignal(f,true)==0&&controls.waterCommand()==0,"E-stop did not cancel warning");var saved=controls.save();controls.reset();controls.load(saved);check(controls.stopped&&controls.warningTicks==0,"Reload lost latched stop");controls.reset();water(f,DeskControl.FILL);saved=controls.save();controls.load(saved);check(!controls.stopped&&controls.waterCommand()==0&&controls.warningTicks==0&&controls.gate[0]==DeskControl.HOLD,"Reload restarted old commands");}
            if(tick==2830){BlockPos end=f.desk.relative(level.getBlockState(f.desk).getValue(ControlDeskBlock.FACING).getCounterClockWise());level.destroyBlock(end,false);check(!level.getBlockState(f.desk).is(Content.DESK.get())&&f.lock.deskPosition()==null&&controls.stopped,"Desk removal did not unlink and stop");}
        }if(tick==2850)finish(null);}catch(Throwable error){finish(error);}
    }
}
