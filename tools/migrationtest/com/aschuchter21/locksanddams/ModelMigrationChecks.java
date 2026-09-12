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

/** Loads the actual dev.6 relocation fixture save, whose gates have no catwalk properties. */
@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID)
public final class ModelMigrationChecks {
    static MinecraftServer server;static ServerLevel level;static int tick;static boolean done;
    @SubscribeEvent public static void start(ServerStartedEvent e) {
        if(!Boolean.getBoolean("locksanddams.modelMigration"))return;server=e.getServer();level=server.overworld();
        for(int i=0;i<4;i++)for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)level.setChunkForced(((100+160*i)>>4)+x,6+z,true);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(server==null||done||e.phase!=TickEvent.Phase.END||++tick<100)return;done=true;
        try {
            int index=0;
            for(Direction f:Direction.Plane.HORIZONTAL) {
                BlockPos base=new BlockPos(100+160*index++,200,100);BlockPos owner=new LockLayout(base,f).at(-1,3,8);
                if(!(level.getBlockEntity(owner) instanceof LockEntity lock))throw new IllegalStateException("Saved controller absent at "+owner+"; state="+level.getBlockState(owner));
                if(!lock.assembled()||lock.validate()!=null)throw new IllegalStateException("Saved controller invalid at "+owner+"; "+lock.status()+"; "+lock.validate());
                CustomLock c=CustomLock.load(owner,lock.saveWithoutMetadata().getCompound("CustomLock"));
                if(lock.waterUnits()!=c.low)throw new IllegalStateException("Saved water changed");
                for(int i=0;i<4;i++) {
                    var h=c.hinge(level,i);if(!h.closed()||!h.belongsTo(owner))throw new IllegalStateException("Saved gate lost");
                    int count=0;for(var entry:h.getMovedContraption().getContraption().getBlocks().entrySet()) {
                        boolean top=entry.getKey().getY()==h.geometry().height()-1;
                        if(entry.getValue().state().getValue(GatePanelBlock.TOP)!=top)throw new IllegalStateException("Saved gate catwalk not upgraded");if(top)count++;
                    }
                    if(count!=h.geometry().width())throw new IllegalStateException("Saved gate missing top blocks");
                }
            }
            Files.writeString(Path.of("model-migration-result.txt"),"PASS: 16 gates from the actual dev.6 world acquired top catwalks; four relocated controllers, water levels and gate ownership survived loading.\n");
        }catch(Throwable error){try{Files.writeString(Path.of("model-migration-result.txt"),"FAIL: "+error+"\n");}catch(Exception ignored){}error.printStackTrace();}
        server.halt(false);
    }
}
