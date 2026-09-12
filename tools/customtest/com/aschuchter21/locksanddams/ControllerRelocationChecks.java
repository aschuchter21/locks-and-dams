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
public final class ControllerRelocationChecks {
    static class Fixture {CustomLock chamber;BlockPos owner;Fixture(CustomLock c){chamber=c;owner=c.owner;}}
    static final List<Fixture> fixtures=new ArrayList<>();static MinecraftServer server;static ServerLevel level;static int ticks;static boolean done;
    static void check(boolean ok,String text){if(!ok)throw new IllegalStateException("tick "+ticks+": "+text);}
    static void power(BlockPos valve,Direction f,boolean on){BlockPos p=valve.relative(f.getCounterClockWise());level.setBlock(p,level.getBlockState(p).setValue(LeverBlock.POWERED,on),3);level.updateNeighborsAt(p,Blocks.LEVER);}
    static LockEntity controller(BlockPos p){return (LockEntity)level.getBlockEntity(p);}
    static void finish(Throwable e){done=true;try{Files.writeString(Path.of("controller-relocation-result.txt"),e==null?"PASS: 12 controller relocations across both walls and three heights in four orientations; live-controller takeover refused; old wall-hole diagnosed; water and gate entity IDs preserved; filling and draining after relocation.\n":"FAIL: "+e+"\n");}catch(Exception x){x.printStackTrace();}if(e!=null)e.printStackTrace();server.halt(false);}
    @SubscribeEvent public static void start(ServerStartedEvent e){
        if(!Boolean.getBoolean("locksanddams.controllerRelocation"))return;server=e.getServer();level=server.overworld();
        try {int index=0;for(Direction f:Direction.Plane.HORIZONTAL){BlockPos base=new BlockPos(100+160*index++,200,100);for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)level.setChunkForced((base.getX()>>4)+x,6+z,true);
            BlockPos owner=CustomDemo.build(level,base,f,6,11,4);fixtures.add(new Fixture(CustomLock.load(owner,controller(owner).saveWithoutMetadata().getCompound("CustomLock"))));
        }}catch(Throwable error){finish(error);}
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(server==null||done||e.phase!=TickEvent.Phase.END)return;ticks++;
        try {for(Fixture f:fixtures){CustomLock c=f.chamber;
            if(ticks==20||ticks==70)power(c.fill,c.forward,ticks==20);
            if(ticks==90||ticks==120||ticks==150){
                int move=(ticks-90)/30;BlockPos next=c.at(move==1?c.width:-1,move+1,4+move*2);BlockPos old=f.owner;int water=controller(old).waterUnits();check(water>c.low&&water<c.high,"Fixture not partly filled");
                List<UUID> ids=new ArrayList<>();for(int i=0;i<4;i++)ids.add(c.hinge(level,i).getMovedContraption().getUUID());
                level.setBlock(next,Content.CONTROLLER.get().defaultBlockState().setValue(ControllerBlock.FACING,c.forward.getClockWise()),3);
                check(!controller(next).assemble(),"New controller stole a live chamber");for(int i=0;i<4;i++)check(c.hinge(level,i).belongsTo(old),"Failed assembly changed ownership");
                level.setBlock(old,Blocks.AIR.defaultBlockState(),3);
                check(!controller(next).assemble(),"Unrepaired old wall hole accepted");check(controller(next).status().contains("Restore solid side wall at "+old.toShortString()),"Wrong wall-hole message: "+controller(next).status());
                for(int i=0;i<4;i++)check(c.hinge(level,i).belongsTo(old),"Wall failure changed ownership");
                level.setBlock(old,Blocks.STONE_BRICKS.defaultBlockState(),3);
                check(controller(next).assemble(),"Relocation failed: "+controller(next).status());check(controller(next).waterUnits()==water,"Relocation reset water");
                for(int i=0;i<4;i++)check(c.hinge(level,i).belongsTo(next)&&c.hinge(level,i).getMovedContraption().getUUID().equals(ids.get(i)),"Gate state was replaced or not reassigned");f.owner=next;
            }
            if(ticks==200)power(c.fill,c.forward,true);
            if(ticks==550){check(controller(f.owner).waterUnits()==c.high,"Relocated controller did not fill");power(c.fill,c.forward,false);power(c.drain,c.forward,true);}
            if(ticks==900){check(controller(f.owner).waterUnits()==c.low,"Relocated controller did not drain");power(c.drain,c.forward,false);}
        }if(ticks==920)finish(null);}catch(Throwable error){finish(error);}
    }
}
