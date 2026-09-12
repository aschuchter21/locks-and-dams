package com.aschuchter21.locksanddams;

import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Isolated real-client resource and interaction preview. */
@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID,value=Dist.CLIENT)
public final class DeskPreviewChecks {
    static boolean started,ready,done;static int tick;
    static ServerPlayer player;static ServerLevel level;static BlockPos desk;static LockEntity lock;
    static final BlockPos BASE=new BlockPos(3000,200,0);
    static volatile String shot;
    static void finish(Throwable error){done=true;try{Files.writeString(Path.of("desk-preview-result.txt"),error==null?"PASS: native desk models loaded; selector and emergency-stop interactions checked; screenshots captured.\n":"FAIL: "+error+"\n");}catch(Exception ignored){}if(error!=null)error.printStackTrace();}
    static void clickCenter(boolean stop,boolean reset){
        Direction f=level.getBlockState(desk).getValue(ControlDeskBlock.FACING);
        var target=net.minecraft.world.phys.Vec3.atCenterOf(desk).add(f.getStepX()*(stop?.27:0),stop?.17:.36,f.getStepZ()*(stop?.27:0));
        var hit=level.clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),target,net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
        if(!hit.getBlockPos().equals(desk))throw new IllegalStateException("Player ray missed desk: "+hit);
        player.setShiftKeyDown(reset);
        level.getBlockState(desk).use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        player.setShiftKeyDown(false);
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("locksanddams.deskPreview")||e.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;mc.options.cloudStatus().set(CloudStatus.OFF);
        if(shot!=null&&mc.level!=null){Screenshot.grab(mc.gameDirectory,shot,mc.getMainRenderTarget(),c->{});shot=null;}
        if(done){mc.stop();return;}
        if(started||mc.player==null||mc.getSingleplayerServer()==null||mc.screen!=null)return;
        started=true;mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.getSingleplayerServer().execute(()->{try{
            level=mc.getSingleplayerServer().overworld();player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.setChunkForced((BASE.getX()>>4)+x,z,true);
            desk=new LockLayout(BASE,Direction.NORTH).at(-6,1,6);
            if(!(level.getBlockEntity(desk) instanceof ControlDeskEntity))desk=DeskDemo.build(level,BASE,Direction.NORTH,null);
            lock=((ControlDeskEntity)level.getBlockEntity(desk)).controller();lock.deskControl().reset();lock.deskControl().waterSelection=DeskControl.FILL;lock.deskControl().gateSelection[0]=lock.deskControl().gateSelection[1]=false;lock.deskChanged();
            level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);player.setGameMode(GameType.SPECTATOR);player.teleportTo(level,desk.getX()-2.1,desk.getY()+1.0,desk.getZ()+.5,-90,37);ready=true;
        }catch(Throwable error){finish(error);}});
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ready||done||e.phase!=TickEvent.Phase.END)return;tick++;
        try{
            var d=(ControlDeskEntity)level.getBlockEntity(desk);
            if(tick==240)shot="dev8-desk-idle.png";
            if(tick==260){clickCenter(false,false);if(lock.deskControl().waterSelection!=DeskControl.DRAIN||lock.deskControl().warningTicks!=200)throw new IllegalStateException("Water selector failed: selection="+lock.deskControl().waterSelection+" stopped="+lock.deskControl().stopped+" warning="+lock.deskControl().warningTicks);}
            if(tick==290)shot="dev8-desk-warning.png";
            if(tick==320){clickCenter(true,false);if(!lock.deskControl().stopped)throw new IllegalStateException("E-stop failed");}
            if(tick==350)shot="dev8-desk-estop.png";
            if(tick==370){clickCenter(true,true);if(lock.deskControl().stopped||lock.deskControl().waterRequest!=0)throw new IllegalStateException("Reset restarted water");}
            if(tick==400)player.teleportTo(level,BASE.getX()-14,211,7,-137,29);
            if(tick==470)shot="dev8-wired-lock.png";
            if(tick==480){lock.deskControl().selectGate(0,DeskControl.OPEN);lock.deskChanged();player.teleportTo(level,BASE.getX()+4,205,7,180,28);}
            if(tick==550)shot="dev9-open-recess.png";
            if(tick==560){lock.deskControl().selectGate(0,DeskControl.CLOSE);lock.deskChanged();}
            if(tick==620){
                var geometry=CustomLock.load(lock.getBlockPos(),lock.saveWithoutMetadata().getCompound("CustomLock"));
                var h=geometry.hinge(level,0);var leaf=h.geometry();BlockPos panel=leaf.at(1,1);
                player.setGameMode(GameType.CREATIVE);
                if(!player.gameMode.destroyBlock(panel)||!level.getBlockState(panel).isAir()||h.isRunning()||lock.assembled())throw new IllegalStateException("Mining left a ghost gate");
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Content.PANEL.get()));
                player.setYRot(90);
                var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(panel.below()).add(0,.5,0),Direction.UP,panel.below(),false);
                player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
                if(!level.getBlockState(panel).is(Content.PANEL.get())||level.getBlockState(panel).getValue(GatePanelBlock.AXIS)!=leaf.normal())throw new IllegalStateException("Replacement panel did not align with its neighbors");
                if(!lock.assemble())throw new IllegalStateException("Repaired gate did not reassemble: "+lock.status());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            }
            if(tick==650){
                player.setGameMode(GameType.CREATIVE);
                BlockPos p=desk.offset(-6,0,0);
                level.setBlock(p.below(),net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(),3);
                player.teleportTo(level,p.getX()+.5,p.getY(),p.getZ()+3,180,20);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Content.DESK.get()));
                var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p.below()).add(0,.5,0),Direction.UP,p.below(),false);
                player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
                if(!(level.getBlockEntity(p) instanceof ControlDeskEntity fresh))throw new IllegalStateException("Desk item placement failed");
                Direction right=level.getBlockState(p).getValue(ControlDeskBlock.FACING).getCounterClockWise();
                if(!level.getBlockState(p.relative(right)).is(Content.DESK.get())||!level.getBlockState(p.relative(right.getOpposite())).is(Content.DESK.get()))throw new IllegalStateException("Missing desk end after item placement");
                d.detach();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Content.LINK_TOOL.get()));
                for(BlockPos target:new BlockPos[]{lock.getBlockPos(),p}){
                    var toolHit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(target),Direction.UP,target,false);
                    player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,toolHit));
                }
                if(fresh.controller()!=lock)throw new IllegalStateException("Link tool did not link placed desk");
                level.destroyBlock(p,false);d.linkForDemo(lock.getBlockPos());
                for(BlockPos q:new BlockPos[]{p,p.relative(right),p.relative(right.getOpposite())})if(level.getBlockState(q).is(Content.DESK.get()))throw new IllegalStateException("Desk removal left orphan section");
                level.removeBlock(p.below(),false);finish(null);
            }
        }catch(Throwable error){finish(error);}
    }
}
