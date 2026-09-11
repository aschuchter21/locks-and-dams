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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Real dedicated-server integration fixture; never part of the release JAR. */
@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID)
public final class LockChecks {
    static MinecraftServer server;
    static ServerLevel level;
    static int ticks;
    static boolean finished;
    static boolean restoring;
    static final List<String[]> restoreRows=new ArrayList<>();
    static final List<Fixture> fixtures=new ArrayList<>();
    static final class Fixture {
        LockLayout layout;
        LockEntity lock;
        Boat boat;
        ChestBoat chest;
        Villager rider;
        int paused;
    }
    static void check(boolean ok,String message) { if(!ok) throw new IllegalStateException("tick "+ticks+": "+message); }
    static void finish(boolean success,Throwable error) {
        finished=true;
        try {
            String result=success?"PASS: four orientations; lower-canal entry; full lift and drain; still-fluid buoyancy; chest-boat cargo and villager passenger; gate interlocks and obstruction; conflicting inputs; interrupted power; controller NBT reload and replacement recovery; broken-wall and missing-canal pause; non-destructive demo rejection.\n":"FAIL: "+error+"\n";
            Files.writeString(Path.of("lock-checks-result.txt"),result);
            if(success&&!restoring) {
                StringBuilder saved=new StringBuilder();
                for(Fixture f:fixtures) saved.append(f.layout.origin().getX()).append(',').append(f.lock.waterUnits())
                    .append(',').append(f.chest.getUUID()).append(',').append(f.rider.getUUID()).append('\n');
                Files.writeString(Path.of("lock-restore-state.txt"),saved.toString());
            }
            com.mojang.logging.LogUtils.getLogger().info("LOCK_CHECKS_{} {}",success?"PASS":"FAIL",result);
            if(error!=null) error.printStackTrace();
        } catch(Exception e) { e.printStackTrace(); }
        server.halt(false);
    }
    @SubscribeEvent public static void start(ServerStartedEvent event) {
        if(!Boolean.getBoolean("locksanddams.checks")) return;
        server=event.getServer();level=server.overworld();
        try {
            if(Boolean.getBoolean("locksanddams.restore")) {
                restoring=true;
                for(String line:Files.readAllLines(Path.of("lock-restore-state.txt"))) {
                    String[] row=line.split(",");restoreRows.add(row);int x=Integer.parseInt(row[0]);
                    for(int cx=-2;cx<=2;cx++) for(int cz=-2;cz<=2;cz++) level.setChunkForced((x>>4)+cx,(100>>4)+cz,true);
                    LockEntity restored=(LockEntity)level.getBlockEntity(new BlockPos(x,200,100));
                    check(restored!=null&&restored.assembled(),"Controller not restored on server restart");
                    check(restored.waterUnits()==Integer.parseInt(row[1]),"Water level changed across server restart");
                    Fixture f=new Fixture();f.lock=restored;f.layout=restored.layout();power(f,3,false);power(f,7,false);
                }
                return;
            }
            int i=0;
            for(Direction direction : Direction.Plane.HORIZONTAL) {
                Fixture f=new Fixture(); f.layout=new LockLayout(new BlockPos(100+(i++)*64,200,100),direction);
                for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) level.setChunkForced((f.layout.origin().getX()>>4)+x,(f.layout.origin().getZ()>>4)+z,true);
                String error=DemoLock.build(level,f.layout); check(error==null,"Demo failed: "+error);
                f.lock=(LockEntity)level.getBlockEntity(f.layout.origin());
                check(f.lock.assembled()&&f.lock.validate()==null,"Lock did not validate");
                f.boat=new Boat(level,0,0,0); f.boat.setVariant(Boat.Type.OAK);
                move(f.boat,f.layout,3,0,-2); level.addFreshEntity(f.boat);
                f.chest=new ChestBoat(level,0,0,0); move(f.chest,f.layout,3,0,6);
                f.chest.setItem(0,new ItemStack(Items.DIAMOND,17));level.addFreshEntity(f.chest);
                f.rider=EntityType.VILLAGER.create(level);f.rider.setNoAi(true);f.rider.setInvulnerable(true);
                f.rider.moveTo(f.chest.position());level.addFreshEntity(f.rider);check(f.rider.startRiding(f.chest,true),"Cannot mount passenger");
                fixtures.add(f);
                // A second demo must refuse to overwrite the first one.
                check(DemoLock.build(level,f.layout)!=null,"Overlapping demo accepted");
                check(level.getBlockEntity(f.layout.origin())==f.lock,"Demo replaced existing controller");
            }
        } catch(Throwable e) { finish(false,e); }
    }
    static void move(Entity entity,LockLayout l,int x,int y,int z) {
        BlockPos p=l.at(x,y,z); entity.setPos(p.getX()+.5,p.getY()+.5,p.getZ()+.5);entity.setDeltaMovement(Vec3.ZERO);
    }
    static void power(Fixture f,int z,boolean on) {
        BlockPos p=f.layout.at(-1,1,z);
        level.setBlock(p,level.getBlockState(p).setValue(LeverBlock.POWERED,on),Block.UPDATE_ALL);
        level.updateNeighborsAt(p,Blocks.LEVER);level.updateNeighborsAt(f.layout.at(0,1,z),Blocks.LEVER);
    }
    static boolean open(Fixture f,int z) { return level.getBlockState(f.layout.at(3,2,z)).getValue(GatePanelBlock.OPEN); }
    static void floating(Fixture f) {
        double surface=f.layout.origin().getY()+f.lock.waterUnits()/16.0;
        check(f.chest.isAlive()&&f.boat.isAlive(),"Boat broke");
        check(f.rider.getVehicle()==f.chest,"Passenger ejected");
        check(f.chest.getItem(0).is(Items.DIAMOND)&&f.chest.getItem(0).getCount()==17,"Cargo changed");
        check(f.chest.getY()>surface-.85&&f.chest.getY()<surface+.35,"Chest boat lost surface: boat="+f.chest.getY()+" water="+surface);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(server==null||finished||event.phase!=TickEvent.Phase.END) return;
        ticks++;
        try {
            if(restoring) {
                if(ticks==30) {
                    for(String[] row:restoreRows) {
                        var chest=level.getEntity(UUID.fromString(row[2]));var rider=level.getEntity(UUID.fromString(row[3]));
                        check(chest instanceof ChestBoat&&rider!=null&&rider.getVehicle()==chest,"Boat/passenger lost across restart");
                        check(((ChestBoat)chest).getItem(0).getCount()==17,"Cargo lost across restart");
                    }
                    Files.writeString(Path.of("lock-restart-result.txt"),"PASS: actual server stop/start restored four chamber levels, boats, cargo and mounted passengers.\n");
                    finished=true;server.halt(false);
                }
                return;
            }
            for(Fixture f : fixtures) {
                if(ticks>15) floating(f);
                if(ticks==20) power(f,0,true);
                if(ticks==25) { check(open(f,0),"Lower gate did not open");power(f,3,true); }
                if(ticks>=25&&ticks<=57) {
                    Direction d=f.layout.forward();f.boat.setDeltaMovement(d.getStepX()*.16, f.boat.getDeltaMovement().y,d.getStepZ()*.16);
                }
                if(ticks==58) {
                    check(f.lock.waterUnits()==LockLayout.LOW,"Filled with gate open");
                    check(f.boat.getBoundingBox().intersects(f.layout.box(1,0,1,5,5,9)),"Boat failed canal entry");
                    f.boat.setDeltaMovement(Vec3.ZERO);power(f,0,false);
                }
                if(ticks==75) check(f.lock.waterUnits()>LockLayout.LOW,"Did not start filling");
                if(ticks==100) { power(f,7,true);f.paused=f.lock.waterUnits(); }
                if(ticks==110) { check(f.lock.waterUnits()==f.paused,"Conflicting valves moved water");power(f,7,false); }
                if(ticks==130) { power(f,3,false);f.paused=f.lock.waterUnits(); }
                if(ticks==145) { check(f.lock.waterUnits()==f.paused,"Power loss did not pause");power(f,3,true);power(f,10,true); }
                if(ticks==165) check(!open(f,10),"Upper gate opened at wrong level");
                if(ticks==350) {
                    check(f.lock.waterUnits()==LockLayout.HIGH,"Did not reach high level: "+f.lock.status());
                    check(open(f,10),"Held upper gate signal was not re-evaluated");
                    move(f.boat,f.layout,3,3,10);power(f,10,false);
                }
                if(ticks==365) {
                    check(open(f,10),"Gate closed on boat");move(f.boat,f.layout,3,3,4);
                }
                if(ticks==380) { check(!open(f,10),"Gate did not close after obstruction cleared");power(f,3,false);power(f,7,true); }
                if(ticks==430) {
                    var saved=f.lock.saveWithFullMetadata(); int expected=f.lock.waterUnits();
                    level.removeBlockEntity(f.layout.origin());
                    LockEntity restored=new LockEntity(f.layout.origin(),level.getBlockState(f.layout.origin()));
                    restored.load(saved);level.setBlockEntity(restored);f.lock=restored;
                    check(restored.assembled()&&restored.waterUnits()==expected,"Saved water level lost");
                }
                if(ticks==640) {
                    check(f.lock.waterUnits()==LockLayout.LOW,"Did not finish draining after reload: "+f.lock.status());
                    power(f,7,false);power(f,0,true);
                }
                if(ticks==650) check(open(f,0),"Lower gate did not reopen");
                if(ticks==660) { power(f,0,false);power(f,3,true); }
                if(ticks==690) {
                    level.setBlock(f.layout.at(6,1,5),Blocks.AIR.defaultBlockState(),3);f.paused=f.lock.waterUnits();
                }
                if(ticks==710) {
                    check(f.lock.waterUnits()==f.paused,"Broken wall did not pause");
                    check(level.isEmptyBlock(f.layout.at(6,1,5)),"Validation overwrote broken wall");
                    level.setBlock(f.layout.at(6,1,5),Blocks.STONE_BRICKS.defaultBlockState(),3);
                }
                if(ticks==730) {
                    check(f.lock.waterUnits()>f.paused,"Did not resume after repair");
                    level.setBlock(f.layout.at(3,3,11),Blocks.STONE.defaultBlockState(),3);f.paused=f.lock.waterUnits();
                }
                if(ticks==750) {
                    check(f.lock.waterUnits()==f.paused,"Missing canal did not pause");
                    level.setBlock(f.layout.at(3,3,11),Blocks.WATER.defaultBlockState(),3);
                }
                if(ticks==770) check(f.lock.waterUnits()>f.paused,"Did not resume after restoring canal");
                if(ticks==780) {
                    int expected=f.lock.waterUnits();
                    BlockPos p=f.layout.origin();
                    var original=level.getBlockState(p);
                    level.setBlock(p,Blocks.AIR.defaultBlockState(),3);level.setBlock(p,original,3);
                    f.lock=(LockEntity)level.getBlockEntity(p);
                    check(f.lock.assemble(),"Could not reclaim orphaned managed water: "+f.lock.status());
                    check(f.lock.waterUnits()==expected,"Replacement controller reset water level");
                }
            }
            if(ticks==790) finish(true,null);
        } catch(Throwable e) { finish(false,e); }
    }
}
