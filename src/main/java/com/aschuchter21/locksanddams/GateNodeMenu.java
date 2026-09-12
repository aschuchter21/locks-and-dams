package com.aschuchter21.locksanddams;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class GateNodeMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    private final Inventory inventory;
    private final ContainerData data;
    public GateNodeMenu(int id,Inventory inventory,BlockPos pos){
        super(Content.NODE_MENU.get(),id);this.inventory=inventory;this.pos=pos;
        data=new SimpleContainerData(1);addDataSlots(data);
        if(!inventory.player.level().isClientSide)refresh();
    }
    private void refresh(){var s=inventory.player.level().getBlockState(pos);if(s.is(Content.NODE.get()))data.set(0,s.getValue(GateControlNodeBlock.UPPER)?1:0);}
    public boolean upper(){return data.get(0)==1;}
    @Override public void broadcastChanges(){if(!inventory.player.level().isClientSide)refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player p){return p.level().getBlockState(pos).is(Content.NODE.get())&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
    @Override public boolean clickMenuButton(Player p,int id){
        if(!stillValid(p)||(id!=0&&id!=1))return false;
        var s=p.level().getBlockState(pos);p.level().setBlock(pos,s.setValue(GateControlNodeBlock.UPPER,id==1),3);refresh();broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
