package com.aschuchter21.locksanddams;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Isolated real-Create server checks. Never shipped. */
@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID)
public final class CreateChecks {
    static MinecraftServer server;static ServerLevel level;static int ticks;static boolean done;
    static final List<LockLayout> layouts=new ArrayList<>();static final List<ChestBoat> boats=new ArrayList<>();
    static final List<Villager> riders=new ArrayList<>();static final Map<BlockPos,Integer> paused=new HashMap<>();
    static void check(boolean ok,String message) {if(!ok)throw new IllegalStateException("tick "+ticks+": "+message);}
    static LockEntity lock(LockLayout l) {return (LockEntity)level.getBlockEntity(l.origin());}
    static LockHingeEntity hinge(LockLayout l,int z) {return (LockHingeEntity)level.getBlockEntity(l.at(1,-2,z));}
    static void power(LockLayout l,int z,boolean on) {BlockPos p=l.at(-1,1,z);level.setBlock(p,level.getBlockState(p).setValue(LeverBlock.POWERED,on),Block.UPDATE_ALL);level.updateNeighborsAt(l.at(0,1,z),Blocks.LEVER);}
    static void motor(LockLayout l,int rpm) {((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)level.getBlockEntity(l.at(1,-3,0))).generatedSpeed.setValue(rpm);}
    static void finish(Throwable error) {
        done=true;String result=error==null?"PASS: Create 6.0.8; four orientations; real 30-panel contraptions; slow opening and closing; shaft power loss; gate interlocks; full fill/drain; separate physical canal routes; broken pipe pause; boats, passengers and cargo retained.":"FAIL: "+error;
        try {Files.writeString(Path.of(Boolean.getBoolean("locksanddams.createRestore")?"create-restart-result.txt":"create-checks-result.txt"),result+"\n");}catch(Exception e){e.printStackTrace();}
        System.out.println(result);if(error!=null)error.printStackTrace();server.halt(false);
    }
    @SubscribeEvent public static void start(ServerStartedEvent e) {
        if(!Boolean.getBoolean("locksanddams.createChecks"))return;server=e.getServer();level=server.overworld();
        try {
            int i=0;for(Direction d:Direction.Plane.HORIZONTAL) {
                LockLayout l=new LockLayout(new BlockPos(100+64*i++,200,100),d);layouts.add(l);
                for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.setChunkForced((l.origin().getX()>>4)+x,6+z,true);
                if(Boolean.getBoolean("locksanddams.createRestore"))continue;
                String error=DemoLock.build(level,l);check(error==null,"Demo "+error);
                check(CulvertRoute.validate(level,l,true)==null,"Fill route "+CulvertRoute.validate(level,l,true));
                check(CulvertRoute.validate(level,l,false)==null,"Drain route "+CulvertRoute.validate(level,l,false));
                ChestBoat boat=new ChestBoat(level,0,0,0);BlockPos p=l.at(3,0,5);boat.setPos(p.getX()+.5,p.getY()+.5,p.getZ()+.5);boat.setItem(0,new ItemStack(Items.DIAMOND,17));level.addFreshEntity(boat);boats.add(boat);
                Villager rider=EntityType.VILLAGER.create(level);rider.setNoAi(true);rider.moveTo(boat.position());level.addFreshEntity(rider);rider.startRiding(boat,true);riders.add(rider);
            }
        }catch(Throwable error){finish(error);}
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(server==null||done||e.phase!=TickEvent.Phase.END)return;ticks++;
        try {
            if(Boolean.getBoolean("locksanddams.createRestore")) {
                if(ticks==50) {for(LockLayout l:layouts){check(lock(l).validate()==null,"Reload invalid: "+lock(l).status());check(hinge(l,0).open(),"Reload angle lost");check(lock(l).waterUnits()==LockLayout.LOW,"Reload level lost");power(l,0,false);}}
                if(ticks==200)for(LockLayout l:layouts) {check(hinge(l,0).closed(),"Reloaded gate did not close");level.setBlock(l.at(3,2,-2),Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);power(l,0,true);}
                if(ticks==350)for(LockLayout l:layouts) {check(!hinge(l,0).open()&&hinge(l,0).blocked(),"Gate ignored terrain obstruction");level.setBlock(l.at(3,2,-2),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);}
                if(ticks==500) {for(LockLayout l:layouts)check(hinge(l,0).open(),"Gate did not resume after obstruction removed");finish(null);}return;
            }
            int i=0;for(LockLayout l:layouts) {
                LockEntity lock=lock(l);LockHingeEntity h=hinge(l,0);
                if(ticks>20) {
                    check(lock.validate()==null,"Invalid "+lock.status());ChestBoat boat=boats.get(i);double surface=l.origin().getY()+lock.waterUnits()/16.0;
                    check(boat.isAlive()&&riders.get(i).getVehicle()==boat&&boat.getItem(0).getCount()==17,"Boat/cargo/passenger lost");
                    check(Math.abs(boat.getY()-(surface-.36))<.6,"Boat lost surface "+boat.getY()+" "+surface);
                }i++;
                if(ticks==20){check(h.closed(),"Hinge not assembled");check(h.getMovedContraption().getContraption().getBlocks().size()==30,"Wrong panel count");power(l,0,true);power(l,3,true);}
                if(ticks==40){check(h.gateAngle()<0&&h.gateAngle()>-90,"No slow swing: "+h.gateAngle()+" speed="+h.getSpeed()+" obstacle="+h.obstruction());check(lock.waterUnits()==LockLayout.LOW,"Filled during swing");motor(l,0);}
                if(ticks==50)paused.put(l.origin(),Math.round(h.gateAngle()*1000));
                if(ticks==65){check(Math.round(h.gateAngle()*1000)==paused.get(l.origin()),"Powerless hinge moved");motor(l,8);}
                if(ticks==200){check(h.open(),"Lower failed opening "+h.gateAngle());check(lock.waterUnits()==LockLayout.LOW,"Filled with gate open");power(l,0,false);}
                if(ticks==210)check(lock.waterUnits()==LockLayout.LOW,"Filled before closed");
                if(ticks==360){check(h.closed(),"Failed closing");check(lock.waterUnits()>LockLayout.LOW,"Did not fill "+lock.status());level.setBlock(l.at(-4,-2,6),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);paused.put(l.origin(),lock.waterUnits());}
                if(ticks==380){check(lock.waterUnits()==paused.get(l.origin()),"Broken pipe still filled");level.setBlock(l.at(-4,-2,6),Content.PIPE.get().defaultBlockState(),Block.UPDATE_ALL);power(l,10,true);}
                if(ticks==650){check(lock.waterUnits()==LockLayout.HIGH,"No high level "+lock.status());check(hinge(l,10).open(),"Upper not open "+hinge(l,10).gateAngle());power(l,10,false);power(l,3,false);power(l,7,true);}
                if(ticks==660)check(lock.waterUnits()==LockLayout.HIGH,"Drained before upper shut");
                if(ticks==1050){check(lock.waterUnits()==LockLayout.LOW,"No low level "+lock.status());power(l,7,false);power(l,0,true);}
                if(ticks==1200)check(h.open(),"Final open failed");
            }
            if(ticks==1200)finish(null);
        }catch(Throwable error){finish(error);}
    }
}
