package com.aschuchter21.locksanddams;

import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Drives a REAL local player through both gates in an isolated integrated world. */
@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID,value=Dist.CLIENT)
public final class ClientRideChecks {
    static boolean dispatched,initialized;
    static volatile boolean row,reverse,finished;
    static volatile String shot;
    static int ticks,stage,stageAt,endTicks;
    static ServerLevel level;
    static ServerPlayer player;
    static Boat boat;
    static LockEntity lock;
    static LockLayout layout;
    static void check(boolean ok,String message) { if(!ok) throw new IllegalStateException("tick "+ticks+", stage "+stage+": "+message); }
    static void power(int z,boolean on) {
        BlockPos p=layout.at(-1,1,z);
        level.setBlock(p,level.getBlockState(p).setValue(LeverBlock.POWERED,on),Block.UPDATE_ALL);
        level.updateNeighborsAt(p,Blocks.LEVER);level.updateNeighborsAt(layout.at(0,1,z),Blocks.LEVER);
    }
    static double progress() { return -(boat.getZ()-.5); }
    static void next(int value) { stage=value;stageAt=ticks; }
    static void end(boolean ok,Throwable error) {
        row=false;
        try {
            String result=ok?"PASS: real local player rode a vanilla boat from lower pool, through filling chamber, into upper pool, back through draining chamber, and out to lower pool; no passenger loss or boat breakage.\n":"FAIL: "+error+"\n";
            Files.writeString(Path.of("client-ride-result.txt"),result);
            com.mojang.logging.LogUtils.getLogger().info("CLIENT_RIDE_{} {}",ok?"PASS":"FAIL",result);
            if(error!=null) error.printStackTrace();
            if(ok) {
                player.stopRiding();player.setGameMode(GameType.SPECTATOR);player.teleportTo(level,-8,213,6,-135,36);
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
            }
        } catch(Exception e) { e.printStackTrace(); }
        finished=true;
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("locksanddams.preview")||event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getInstance();
        mc.options.pauseOnLostFocus=false;
        mc.options.keyUp.setDown(row);
        if(reverse&&mc.player!=null&&mc.player.getVehicle()!=null) {
            mc.player.setYRot(0);mc.player.setXRot(12);mc.player.getVehicle().setYRot(0);reverse=false;
        }
        if(shot!=null&&mc.level!=null) {
            Screenshot.grab(mc.gameDirectory,shot,mc.getMainRenderTarget(),c -> {});shot=null;
        }
        if(finished) {
            endTicks++;
            if(endTicks==45) Screenshot.grab(mc.gameDirectory,"04-lock-overview.png",mc.getMainRenderTarget(),c -> {});
            if(endTicks==75) mc.stop();
            return;
        }
        if(dispatched||mc.player==null||mc.getSingleplayerServer()==null) return;
        dispatched=true; mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        mc.getSingleplayerServer().execute(() -> {
            try {
                level=mc.getSingleplayerServer().overworld();player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                if(Boolean.getBoolean("locksanddams.overview")) {
                    player.stopRiding();player.setGameMode(GameType.SPECTATOR);player.teleportTo(level,-8,213,6,-135,36);
                    mc.execute(() -> { mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=true; });
                    finished=true;return;
                }
                layout=new LockLayout(new BlockPos(0,200,0),Direction.NORTH);
                for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) level.setChunkForced(x,z,true);
                String error=DemoLock.build(level,layout);check(error==null,"Demo failed: "+error);
                lock=(LockEntity)level.getBlockEntity(layout.origin());level.setDayTime(6000);
                player.setGameMode(GameType.CREATIVE);player.teleportTo(level,3.5,200.55,3.5,180,12);
                boat=new Boat(level,3.5,200.51,3.5);boat.setYRot(180);level.addFreshEntity(boat);
                check(player.startRiding(boat,true),"Player could not mount boat");initialized=true;
            } catch(Throwable e) { end(false,e); }
        });
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event) {
        if(!initialized||finished||event.phase!=TickEvent.Phase.END) return;
        ticks++;
        try {
            check(ticks<2000,"Timed out");check(boat.isAlive(),"Boat broke");check(player.getVehicle()==boat,"Player was ejected");
            if(progress()>1.5&&progress()<9) {
                double surface=200+lock.waterUnits()/16.0;
                check(boat.getY()>surface-.95&&boat.getY()<surface+.45,"Ridden boat lost surface: "+boat.getY()+" vs "+surface);
            }
            switch(stage) {
                case 0 -> { if(ticks==60) { power(0,true);next(1); } }
                case 1 -> { if(ticks-stageAt>15) row=true; if(progress()>2.5) { row=false;power(0,false);power(3,true);next(2); } }
                case 2 -> {
                    if(ticks-stageAt==130) shot="01-riding-up.png";
                    if(lock.waterUnits()==LockLayout.HIGH) { power(3,false);power(10,true);next(3); }
                }
                case 3 -> {
                    if(ticks-stageAt>20) row=true;
                    if(progress()>12.5) { row=false;shot="02-upper-pool.png";next(4); }
                }
                case 4 -> {
                    if(ticks-stageAt==40) { boat.setYRot(0);player.connection.send(new ClientboundMoveVehiclePacket(boat));reverse=true; }
                    if(ticks-stageAt>60) { row=true;next(5); }
                }
                case 5 -> { if(progress()<7.5) { row=false;power(10,false);power(7,true);next(6); } }
                case 6 -> {
                    if(ticks-stageAt==130) shot="03-riding-down.png";
                    if(lock.waterUnits()==LockLayout.LOW) { power(7,false);power(0,true);next(7); }
                }
                case 7 -> { if(ticks-stageAt>20) row=true; if(progress()<-2.5) end(true,null); }
                default -> throw new IllegalStateException("Unknown stage");
            }
        } catch(Throwable e) { end(false,e); }
    }
}
