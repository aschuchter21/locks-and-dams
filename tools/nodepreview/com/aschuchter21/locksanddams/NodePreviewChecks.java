package com.aschuchter21.locksanddams;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.platform.InputConstants;

/** Drive Minecraft's real Use key path; never call Block.use directly. */
@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID,value=Dist.CLIENT)
public final class NodePreviewChecks {
    static boolean started,done,waiting;static volatile boolean prepared;static int age,mode,screenAge;static ServerPlayer player;static ServerLevel level;
    static final BlockPos NODE=new BlockPos(5100,201,0);
    static InputConstants.Key original;static final InputConstants.Key R=InputConstants.Type.KEYSYM.getOrCreate(82);
    static void finish(Throwable error){
        if(done)return;done=true;var mc=Minecraft.getInstance();mc.options.keyShift.setDown(false);mc.options.keyUse.setDown(false);if(original!=null){mc.options.keyUse.setKey(original);KeyMapping.resetMapping();}
        try{Files.writeString(Path.of("node-preview-result.txt"),error==null?"PASS: actual Shift + remapped R opens settings with empty hand, held block, link tool and occupied offhand; key hint resolves to R; menu selection round-trips to server.\n":"FAIL: "+error+"\n");}catch(Exception ignored){}if(error!=null)error.printStackTrace();mc.stop();
    }
    static void prepare(){
        var mc=Minecraft.getInstance();prepared=false;age=screenAge=0;waiting=false;mc.options.keyShift.setDown(false);
        mc.getSingleplayerServer().execute(()->{
            try{
                player.closeContainer();player.setItemInHand(InteractionHand.MAIN_HAND,mode==1?new ItemStack(Blocks.STONE_BRICKS):mode==2?new ItemStack(Content.LINK_TOOL.get()):ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND,mode==3?new ItemStack(Blocks.STONE_BRICKS):ItemStack.EMPTY);
                player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();player.teleportTo(level,NODE.getX()+.5,201,3,180,24);prepared=true;
            }catch(Throwable e){mc.execute(()->finish(e));}
        });
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("locksanddams.nodePreview")||event.phase!=TickEvent.Phase.END||done)return;
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        try{
            if(!started){
                if(mc.player==null||mc.getSingleplayerServer()==null||mc.screen!=null)return;
                started=true;original=mc.options.keyUse.getKey();mc.options.keyUse.setKey(R);KeyMapping.resetMapping();mc.options.hideGui=false;
                if(!GateControlNodeBlock.settingsHint().getString().contains("R"))throw new IllegalStateException("Hint did not resolve remapped key: "+GateControlNodeBlock.settingsHint().getString());
                mc.getSingleplayerServer().execute(()->{try{
                    level=mc.getSingleplayerServer().overworld();player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);level.setChunkForced(NODE.getX()>>4,0,true);level.setChunkForced(NODE.getX()>>4,-1,true);
                    for(int x=-3;x<=3;x++)for(int z=-3;z<=5;z++)level.setBlock(NODE.offset(x,-1,z),Blocks.STONE_BRICKS.defaultBlockState(),3);
                    level.setBlock(NODE,Content.NODE.get().defaultBlockState(),3);player.setGameMode(GameType.CREATIVE);mc.execute(NodePreviewChecks::prepare);
                }catch(Throwable e){mc.execute(()->finish(e));}});return;
            }
            if(!prepared)return;age++;
            if(mc.screen instanceof GateNodeScreen screen){
                if(++screenAge==8){Screenshot.grab(mc.gameDirectory,"dev14-node-settings-"+mode+".png",mc.getMainRenderTarget(),c->{});mc.gameMode.handleInventoryButtonClick(screen.getMenu().containerId,mode%2);}
                if(screenAge==20&&!waiting){waiting=true;mc.getSingleplayerServer().execute(()->{
                    Throwable error=null;
                    if(level.getBlockState(NODE).getValue(GateControlNodeBlock.UPPER)!=(mode%2==1))error=new IllegalStateException("Menu change did not reach server");
                    if(!level.getBlockState(NODE.south()).isAir())error=new IllegalStateException("Held block placed instead of opening settings");
                    final Throwable result=error;mc.execute(()->{if(result!=null){finish(result);return;}mc.player.closeContainer();if(++mode==4)finish(null);else prepare();});
                });}return;
            }
            if(age==20&&mode==0){KeyMapping.click(R);}
            if(age==30&&mode==0)Screenshot.grab(mc.gameDirectory,"dev14-remapped-key-hint.png",mc.getMainRenderTarget(),c->{});
            if(age==40)mc.options.keyShift.setDown(true);
            if(age==65){
                if(!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)||!hit.getBlockPos().equals(NODE))throw new IllegalStateException("Use ray missed node: "+mc.hitResult);
                KeyMapping.click(R);
            }
            if(age>160)throw new IllegalStateException("Shift + R failed with hand case "+mode);
        }catch(Throwable error){finish(error);}
    }
}
