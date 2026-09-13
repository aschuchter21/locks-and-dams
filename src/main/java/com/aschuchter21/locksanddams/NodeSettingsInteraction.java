package com.aschuchter21.locksanddams;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Sneak-use is the node's settings gesture, even when either hand holds an item. */
@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID)
public final class NodeSettingsInteraction {
    @SubscribeEvent(priority=EventPriority.LOW)
    public static void use(PlayerInteractEvent.RightClickBlock event){
        if(event.getEntity().isSpectator()||!event.getEntity().isShiftKeyDown()
            ||event.getUseBlock()==Event.Result.DENY||!event.getLevel().getBlockState(event.getPos()).is(Content.NODE.get()))return;
        event.setUseBlock(Event.Result.ALLOW);
        event.setUseItem(Event.Result.DENY);
    }
}
