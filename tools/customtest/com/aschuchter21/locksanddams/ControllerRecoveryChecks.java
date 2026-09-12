package com.aschuchter21.locksanddams;
import java.util.*;
import java.nio.file.*;
import net.minecraft.core.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID)
public final class ControllerRecoveryChecks {
    record Fixture(BlockPos controller,Direction forward,BlockPos valve,boolean custom) {}
    static final List<Fixture> fixtures=new ArrayList<>();static MinecraftServer server;static ServerLevel level;static int ticks;static boolean done;
    static void check(boolean ok,String message){if(!ok)throw new IllegalStateException("tick "+ticks+": "+message);}
    static void finish(Throwable e){done=true;try{Files.writeString(Path.of("controller-recovery-result.txt"),e==null?"PASS: 32 controller break/place/reassemble cases; both demo types in four orientations; all four replacement facings; water levels and contraption identities preserved.\n":"FAIL: "+e+"\n");}catch(Exception x){x.printStackTrace();}if(e!=null)e.printStackTrace();server.halt(false);}
    @SubscribeEvent public static void start(ServerStartedEvent event) {
        if(!Boolean.getBoolean("locksanddams.controllerRecovery"))return;server=event.getServer();level=server.overworld();
        try {
            int i=0;for(Direction f:Direction.Plane.HORIZONTAL)for(boolean custom:new boolean[]{false,true}) {
                BlockPos base=new BlockPos(100+160*i++,200,100);for(int cx=-4;cx<=4;cx++)for(int cz=-4;cz<=4;cz++)level.setChunkForced((base.getX()>>4)+cx,6+cz,true);
                LockLayout g=new LockLayout(base,f);BlockPos owner;
                if(custom)owner=CustomDemo.build(level,base,f,7,11,4);else {check(DemoLock.build(level,g)==null,"Legacy demo failed");owner=base;}
                fixtures.add(new Fixture(owner,f,custom?g.at(-1,4,11):g.fill(),custom));
            }
        }catch(Throwable e){finish(e);}
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(server==null||done||event.phase!=TickEvent.Phase.END)return;ticks++;
        try {
            for(Fixture f:fixtures) {
                BlockPos lever=f.valve.relative(f.forward.getCounterClockWise());
                if(ticks==20||ticks==70) {level.setBlock(lever,level.getBlockState(lever).setValue(LeverBlock.POWERED,ticks==20),3);level.updateNeighborsAt(lever,Blocks.LEVER);}
                if(ticks>=90&&ticks<=180&&ticks%30==0) {
                    LockEntity old=(LockEntity)level.getBlockEntity(f.controller);int water=old.waterUnits();check(water>14,"Fixture never filled");
                    List<java.util.UUID> entities=new ArrayList<>();List<LockHingeEntity> hinges=CustomLock.nearby(level,f.controller).stream().filter(h->h.belongsTo(f.controller)).toList();
                    for(var h:hinges)entities.add(h.getMovedContraption().getUUID());
                    level.setBlock(f.controller,Blocks.AIR.defaultBlockState(),3);
                    Direction facing=Direction.from2DDataValue((ticks-90)/30);
                    level.setBlock(f.controller,Content.CONTROLLER.get().defaultBlockState().setValue(ControllerBlock.FACING,facing),3);
                    LockEntity replacement=(LockEntity)level.getBlockEntity(f.controller);
                    check(replacement.assemble(),"Recovery failed custom="+f.custom+" facing="+facing+": "+replacement.status());
                    check(replacement.waterUnits()==water,"Water reset during recovery");check(replacement.getBlockState().getValue(ControllerBlock.FACING)==f.forward,"Incorrect inferred direction");
                    for(int n=0;n<hinges.size();n++)check(hinges.get(n).getMovedContraption().getUUID().equals(entities.get(n)),"Replaced a live contraption");
                }
            }
            if(ticks==200)finish(null);
        }catch(Throwable e){finish(e);}
    }
}
