package com.aschuchter21.locksanddams;
import java.util.*;
import java.nio.file.*;
import net.minecraft.core.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID)
public final class CustomChecks {
    static MinecraftServer server;static ServerLevel level;static int tick;static boolean done;
    static final List<CustomLock> locks=new ArrayList<>();static final List<ChestBoat> boats=new ArrayList<>();static final List<Villager> riders=new ArrayList<>();static final Map<BlockPos,Integer> paused=new HashMap<>();
    static LockLayout legacy=new LockLayout(new BlockPos(1000,200,100),Direction.NORTH);
    static void check(boolean ok,String msg){if(!ok)throw new IllegalStateException("tick "+tick+": "+msg);}
    static void finish(Throwable e){done=true;try{Files.writeString(Path.of(Boolean.getBoolean("locksanddams.customRestore")?"custom-restart-result.txt":"custom-checks-result.txt"),e==null?"PASS: custom chamber checks completed\n":"FAIL: "+e+"\n");}catch(Exception x){x.printStackTrace();}if(e!=null)e.printStackTrace();server.halt(false);}
    static void power(BlockPos p,boolean on){level.setBlock(p,level.getBlockState(p).setValue(LeverBlock.POWERED,on),3);level.updateNeighborsAt(p,Blocks.LEVER);}
    static void gate(CustomLock c,int side,boolean on){power(c.at(-2,(side==0?0:c.rise)-1,side*c.length),on);}
    @SubscribeEvent public static void start(ServerStartedEvent e) {
        if(!Boolean.getBoolean("locksanddams.customChecks"))return;server=e.getServer();level=server.overworld();
        try {
            int index=0;for(Direction f:Direction.Plane.HORIZONTAL) {
                BlockPos base=new BlockPos(100+index*160,200,100);int w=new int[]{4,7,10,16}[index],length=new int[]{5,11,17,23}[index],lift=new int[]{1,3,5,8}[index++];
                for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)level.setChunkForced((base.getX()>>4)+x,(base.getZ()>>4)+z,true);
                BlockPos owner=new LockLayout(base,f).at(-1,0,2);
                if(!Boolean.getBoolean("locksanddams.customRestore")) {
                    check(CustomDemo.build(level,base,f,w,length,lift).equals(owner),"Wrong controller");
                    boolean refused=false;try{CustomDemo.build(level,base,f,w,length,lift);}catch(IllegalArgumentException expected){refused=true;}check(refused,"Demo overwrote build");
                }
                LockEntity be=(LockEntity)level.getBlockEntity(owner);check(be!=null&&be.assembled(),"Controller not assembled/restored");
                CustomLock c=CustomLock.load(owner,be.saveWithoutMetadata().getCompound("CustomLock"));locks.add(c);
                check(c.width==w&&c.length==length+1&&c.rise==lift&&c.low==14&&c.high==lift*16+14,"Wrong detected dimensions/levels");
                if(Boolean.getBoolean("locksanddams.customRestore"))continue;
                ChestBoat boat=new ChestBoat(level,0,0,0);BlockPos p=c.at(w/2,0,length/2);boat.setPos(p.getX()+.5,p.getY()+.5,p.getZ()+.5);boat.setItem(0,new ItemStack(Items.DIAMOND,17));level.addFreshEntity(boat);boats.add(boat);
                Villager v=EntityType.VILLAGER.create(level);v.setNoAi(true);v.moveTo(boat.position());level.addFreshEntity(v);v.startRiding(boat,true);riders.add(v);
            }
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.setChunkForced((1000>>4)+x,6+z,true);
            if(!Boolean.getBoolean("locksanddams.customRestore"))check(DemoLock.build(level,legacy)==null,"Legacy demo broke");
        }catch(Throwable error){finish(error);}
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(server==null||done||e.phase!=TickEvent.Phase.END)return;tick++;
        try {
            if(Boolean.getBoolean("locksanddams.customRestore")) {
                if(tick==50) {
                    for(CustomLock c:locks){LockEntity be=(LockEntity)level.getBlockEntity(c.owner);check(be.validate()==null,"Reload validation: "+be.status());for(int i=0;i<4;i++)check(c.hinge(level,i).closed(),"Saved gate lost");check(be.waterUnits()==c.low,"Saved water level lost");
                        var savedBoats=level.getEntitiesOfClass(ChestBoat.class,c.bounds());check(savedBoats.size()==1&&savedBoats.get(0).getItem(0).getCount()==17&&savedBoats.get(0).getFirstPassenger() instanceof Villager,"Saved boat/cargo/passenger lost");
                        level.setBlock(c.fillPort,level.getBlockState(c.fillPort).setValue(CulvertPortBlock.FACING,c.forward.getCounterClockWise()),3);power(c.fill.relative(c.forward.getCounterClockWise()),true);
                    }
                    level.removeBlockEntity(legacy.origin());LockEntity recovered=new LockEntity(legacy.origin(),level.getBlockState(legacy.origin()));level.setBlockEntity(recovered);check(recovered.assemble(),"Legacy controller replacement failed: "+recovered.status());
                }
                if(tick==70)for(CustomLock c:locks){LockEntity be=(LockEntity)level.getBlockEntity(c.owner);check(be.waterUnits()==c.low&&be.validate()!=null,"Dry port failed to pause");level.setBlock(c.fillPort,level.getBlockState(c.fillPort).setValue(CulvertPortBlock.FACING,c.forward.getClockWise()),3);}
                if(tick==90) {for(CustomLock c:locks){LockEntity be=(LockEntity)level.getBlockEntity(c.owner);check(be.waterUnits()>c.low&&be.validate()==null,"Repaired port did not resume filling");power(c.fill.relative(c.forward.getCounterClockWise()),false);}finish(null);}return;
            }
            if(tick==20)power(legacy.at(-1,1,0),true);
            if(tick==160)check(((LockHingeEntity)level.getBlockEntity(legacy.at(1,-2,0))).open(),"Legacy hinge stopped working");
            int i=0;for(CustomLock c:locks) {
                LockEntity be=(LockEntity)level.getBlockEntity(c.owner);
                if(tick<=310||tick>330)check(be.validate()==null,"Validation: "+be.validate()+" "+be.status());
                if(tick>20&&tick%25==0){var b=boats.get(i);check(b.isAlive()&&riders.get(i).getVehicle()==b&&b.getItem(0).getCount()==17,"Lost boat, passenger or cargo");check(Math.abs(b.getY()-(c.base.getY()+be.waterUnits()/16.0-.36))<.6,"Boat lost surface");}i++;
                if(tick==20){gate(c,0,true);power(c.fill.relative(c.forward.getCounterClockWise()),true);}
                if(tick==60)check(c.hinge(level,0).gateAngle()<0&&c.hinge(level,1).gateAngle()>0,"Paired gates not swinging apart");
                if(tick==150){check(c.hinge(level,0).open()&&c.hinge(level,1).open(),"Lower pair failed opening "+c.hinge(level,0).gateAngle()+" "+c.hinge(level,0).obstruction());check(be.waterUnits()==c.low,"Filled with gate open");gate(c,0,false);}
                if(tick==165)check(be.waterUnits()==c.low,"Filled before both leaves closed");
                if(tick==290){power(c.drain.relative(c.forward.getCounterClockWise()),true);paused.put(c.owner,be.waterUnits());}
                if(tick==300){check(be.waterUnits()==paused.get(c.owner),"Conflicting valves moved water");power(c.drain.relative(c.forward.getCounterClockWise()),false);}
                if(tick==310){check(be.waterUnits()>c.low,"Did not fill "+be.status());level.setBlock(c.at(-5,c.rise-1,c.length),Blocks.AIR.defaultBlockState(),3);paused.put(c.owner,be.waterUnits());}
                if(tick==330){check(be.waterUnits()==paused.get(c.owner),"Broken pipe did not pause");level.setBlock(c.at(-5,c.rise-1,c.length),Content.PIPE.get().defaultBlockState(),3);}
                if(tick==1100){check(be.waterUnits()==c.high,"Wrong upper target "+be.status());power(c.fill.relative(c.forward.getCounterClockWise()),false);gate(c,1,true);}
                if(tick==1240){check(c.hinge(level,2).open()&&c.hinge(level,3).open(),"Raised upper pair failed opening");gate(c,1,false);power(c.drain.relative(c.forward.getCounterClockWise()),true);}
                if(tick==1250)check(be.waterUnits()==c.high,"Drained before both upper leaves closed");
                if(tick==2150){check(be.waterUnits()==c.low,"Wrong lower target "+be.status());power(c.drain.relative(c.forward.getCounterClockWise()),false);}
            }
            if(tick==2160)finish(null);
        }catch(Throwable error){finish(error);}
    }
}
