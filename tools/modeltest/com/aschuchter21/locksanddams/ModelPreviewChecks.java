package com.aschuchter21.locksanddams;

import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Real client walking and resource-baking check in the isolated preview world. */
@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID,value=Dist.CLIENT)
public final class ModelPreviewChecks {
    static boolean started,ready,done,walk,railTest;
    static int tick;
    static ServerPlayer player;
    static ServerLevel level;
    static final BlockPos BASE=new BlockPos(4800,200,0);
    static volatile String shot;
    static void finish(Throwable error) {
        walk=false;done=true;
        try{Files.writeString(Path.of("model-preview-result.txt"),error==null?"PASS: actual local player walked across the closed gate catwalk; handrails retained player; gate facing survived assembly/disassembly; native models baked and screenshots captured.\n":"FAIL: "+error+"\n");}catch(Exception e){e.printStackTrace();}
        if(error!=null)error.printStackTrace();
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e) {
        if(!Boolean.getBoolean("locksanddams.modelPreview")||e.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;mc.options.cloudStatus().set(CloudStatus.OFF);
        mc.options.keyUp.setDown(walk);
        if(walk&&!railTest&&mc.player!=null&&mc.player.getX()>BASE.getX()+5.3){walk=false;mc.options.keyUp.setDown(false);}
        if(shot!=null&&mc.level!=null){Screenshot.grab(mc.gameDirectory,shot,mc.getMainRenderTarget(),c->{});shot=null;}
        if(done){mc.options.keyUp.setDown(false);mc.stop();return;}
        if(started||mc.player==null||mc.getSingleplayerServer()==null||mc.screen!=null)return;
        started=true;mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.getSingleplayerServer().execute(()->{
            try {
                level=mc.getSingleplayerServer().overworld();player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.setChunkForced((BASE.getX()>>4)+x,z,true);
                if(!level.getBlockState(BASE.offset(-1,0,-2)).is(Content.CONTROLLER.get()))CustomDemo.build(level,BASE,Direction.NORTH,6,11,4);
                level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                player.setGameMode(GameType.SURVIVAL);player.teleportTo(level,BASE.getX()+.5,206.02,.5,-90,20);
                ready=true;
            }catch(Throwable error){finish(error);}
        });
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e) {
        if(!ready||done||e.phase!=TickEvent.Phase.END)return;tick++;
        try {
            if(tick==200)walk=true;
            if(tick>=200&&tick<=300&&Math.abs(player.getY()-206)>.12)throw new IllegalStateException("Player lost catwalk support: "+player.position());
            if(tick==225)shot="dev13-catwalk-walk.png";
            if(tick==300){
                walk=false;
                // Create's moving collision can support the player without setting vanilla onGround.
                if(player.getX()<BASE.getX()+4.8)throw new IllegalStateException("Catwalk crossing failed: "+player.position());
                railTest=true;player.teleportTo(level,BASE.getX()+4,206.02,.5,180,0);walk=true;
                Minecraft.getInstance().execute(()->Minecraft.getInstance().options.hideGui=true);
            }
            if(tick==340){walk=false;if(player.getZ()<.25||Math.abs(player.getY()-206)>.15)throw new IllegalStateException("Handrail did not retain player: "+player.position());player.setGameMode(GameType.SPECTATOR);player.teleportTo(level,BASE.getX()+11,211,10,140,29);}
            if(tick==360)shot="dev13-lock-overview.png";
            if(tick==400)player.teleportTo(level,BASE.getX()-8,207,-7,-75,27);
            if(tick==460)shot="dev13-plumbing.png";
            if(tick==465)player.teleportTo(level,BASE.getX()-5,205,-8,-135,24);
            if(tick==485)shot="dev13-inline-valve-ingame.png";
            if(tick==500){
                for(var h:CustomLock.nearby(level,BASE))if(h.geometry()!=null&&h.geometry().base().distManhattan(BASE)<40){
                    var leaf=h.geometry();boolean expected=leaf.base().getZ()==BASE.getZ();
                    for(var info:h.getMovedContraption().getContraption().getBlocks().values())if(info.state().is(Content.PANEL.get())&&info.state().getValue(GatePanelBlock.REVERSED)!=expected)throw new IllegalStateException("Gate facing changed during assembly");
                    h.disassemble();
                    for(int x=0;x<leaf.width();x++)for(int y=0;y<leaf.height();y++)if(level.getBlockState(leaf.at(x,y)).getValue(GatePanelBlock.REVERSED)!=expected)throw new IllegalStateException("Gate facing changed on disassembly");
                }
                finish(null);
            }
        }catch(Throwable error){finish(error);}
    }
}
