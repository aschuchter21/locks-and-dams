package com.aschuchter21.locksanddams;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

public final class ControlLinkItem extends Item {
    public ControlLinkItem(){super(new Properties().stacksTo(1));}
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
        var level=c.getLevel();var player=c.getPlayer();if(player==null)return InteractionResult.PASS;
        var state=level.getBlockState(c.getClickedPos());CompoundTag n=c.getItemInHand().getOrCreateTag();
        if(state.is(Content.CONTROLLER.get())){n.put("Controller",NbtUtils.writeBlockPos(c.getClickedPos()));n.putString("Dimension",level.dimension().location().toString());player.displayClientMessage(Component.literal("Controller selected. Right-click the control desk to link it."),true);return InteractionResult.SUCCESS;}
        if(state.is(Content.DESK.get())&&level.getBlockEntity(ControlDeskBlock.root(state,c.getClickedPos())) instanceof ControlDeskEntity desk){
            if(player.isShiftKeyDown()){desk.detach();player.displayClientMessage(Component.literal("Desk unlinked; commands stopped."),true);}
            else if(n.contains("Controller")&&n.getString("Dimension").equals(level.dimension().location().toString()))desk.link(NbtUtils.readBlockPos(n.getCompound("Controller")),player);
            else player.displayClientMessage(Component.literal("Select a controller in this dimension first."),true);
            return InteractionResult.SUCCESS;
        }
        if(state.is(Content.NODE.get())&&level.getBlockEntity(c.getClickedPos()) instanceof GateControlNodeEntity node){
            boolean linked=n.contains("Controller")&&n.getString("Dimension").equals(level.dimension().location().toString())&&node.link(NbtUtils.readBlockPos(n.getCompound("Controller")));
            player.displayClientMessage(linked?Component.literal("Gate terminal linked. ").append(GateControlNodeBlock.settingsHint()):Component.literal("Select a loaded controller in this dimension within 128 blocks first."),true);return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
