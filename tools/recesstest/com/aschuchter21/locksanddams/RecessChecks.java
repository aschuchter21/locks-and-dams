package com.aschuchter21.locksanddams;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID)
public final class RecessChecks {
    static MinecraftServer server;static ServerLevel level;static int tick;static boolean done;
    static final List<CustomLock> locks=new ArrayList<>();
    static void check(boolean ok,String message){if(!ok)throw new IllegalStateException("tick "+tick+": "+message);}
    static void finish(Throwable error){done=true;try{Files.writeString(Path.of("recess-checks-result.txt"),error==null?"PASS: inline circuits at minimum chamber length and lifts 1/16; recessed widths 4, 7, 10, 16; four orientations; saved geometry; full lower gate opening and closing with signed rotation; open gates parallel to walls.\n":"FAIL: "+error+"\n");}catch(Exception ignored){}if(error!=null)error.printStackTrace();server.halt(false);}
    @SubscribeEvent public static void start(ServerStartedEvent e){
        if(!Boolean.getBoolean("locksanddams.recessChecks"))return;server=e.getServer();level=server.overworld();
        try{int index=0;for(Direction f:Direction.Plane.HORIZONTAL){int w=new int[]{4,7,10,16}[index];BlockPos base=new BlockPos(1600+160*index++,200,100);
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)level.setChunkForced((base.getX()>>4)+x,(base.getZ()>>4)+z,true);
            BlockPos owner=CustomDemo.build(level,base,f,w,11,4);var lock=(LockEntity)level.getBlockEntity(owner);
            var c=CustomLock.load(owner,lock.saveWithoutMetadata().getCompound("CustomLock"));
            check(c.recessed&&c.navigationWidth()==w,"Saved recessed dimensions changed");c.validate(level,true);locks.add(c);
        }
            for(int lift:new int[]{1,16}){
                BlockPos base=new BlockPos(2600+lift*20,200,100);
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)level.setChunkForced((base.getX()>>4)+x,(base.getZ()>>4)+z,true);
                BlockPos owner=CustomDemo.build(level,base,Direction.NORTH,4,3,lift);var lock=(LockEntity)level.getBlockEntity(owner);
                var c=CustomLock.load(owner,lock.saveWithoutMetadata().getCompound("CustomLock"));c.validate(level,true);locks.add(c);
            }
        }catch(Throwable error){finish(error);}
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(server==null||done||e.phase!=TickEvent.Phase.END)return;tick++;
        try{for(var c:locks){
            if(tick==10||tick==100)for(int i=0;i<2;i++)((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)level.getBlockEntity(c.hinge(level,i).getBlockPos().below())).generatedSpeed.setValue(tick==10?8:-8);
            if(tick==90)for(int i=0;i<2;i++){
                var h=c.hinge(level,i);check(h.open(),"Recess blocked width "+c.navigationWidth()+": "+h.obstruction());
                check(Math.abs(h.gateAngle())==90,"Gate not parallel to wall");
            }
            if(tick==190)for(int i=0;i<2;i++)check(c.hinge(level,i).closed(),"Gate did not close at width "+c.navigationWidth());
        }if(tick==200)finish(null);}catch(Throwable error){finish(error);}
    }
}
