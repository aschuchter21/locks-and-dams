package com.aschuchter21.locksanddams;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ControlDeskEntity extends BlockEntity {
    private BlockPos controller;
    private int alarm,horn;
    public ControlDeskEntity(BlockPos p,BlockState s){super(Content.DESK_ENTITY.get(),p,s);}
    public LockEntity controller(){return level!=null&&controller!=null&&level.hasChunkAt(controller)&&level.getBlockEntity(controller) instanceof LockEntity lock&&worldPosition.equals(lock.deskPosition())?lock:null;}
    void linkForDemo(BlockPos p){controller=p.immutable();((LockEntity)level.getBlockEntity(p)).linkDesk(worldPosition);setChanged();refresh();}
    public boolean link(BlockPos p,Player player){
        if(level==null||p.distManhattan(worldPosition)>128||!level.hasChunkAt(p)||!(level.getBlockEntity(p) instanceof LockEntity lock)){say(player,"Controller must be loaded and within 128 blocks.");return false;}
        if(!lock.assembled()&&!lock.assemble()){say(player,lock.status());return false;}
        if(lock.deskPosition()!=null&&!lock.deskPosition().equals(worldPosition)){say(player,"That controller already has a desk. Unlink its old desk first.");return false;}
        detach();controller=p.immutable();lock.linkDesk(worldPosition);setChanged();refresh();say(player,"Desk linked. Controls are idle until you operate a switch.");return true;
    }
    public void detach(){LockEntity lock=controller();if(lock!=null)lock.unlinkDesk();controller=null;alarm=horn=0;if(level!=null)notifyOutputs();setChanged();}
    public void operate(Player player,int part,boolean front,boolean sneaking){
        LockEntity lock=controller();
        if(lock==null){say(player,"Use the Control Link Tool on a controller, then this desk.");return;}
        DeskControl controls=lock.deskControl();
        if(part==1&&front) {
            if(controls.stopped&&sneaking){controls.reset();say(player,"Emergency stop RESET. Operate a switch to start a new command.");}
            else {controls.stop();say(player,"EMERGENCY STOP. Shift-right-click this button to reset.");}
        } else if(controls.stopped){say(player,"Emergency stop is latched. Reset the red button first.");return;}
        else if(part==1){controls.selectWater(controls.waterSelection==DeskControl.FILL?DeskControl.DRAIN:DeskControl.FILL,level.getGameTime());say(player,(controls.waterSelection==DeskControl.FILL?"FILL":"DRAIN")+": 10-second warning before water movement.");}
        else {int side=part==0?1:0;controls.selectGate(side,controls.gateSelection[side]?DeskControl.CLOSE:DeskControl.OPEN);say(player,(side==1?"UPPER":"LOWER")+" gate: "+(controls.gate[side]==DeskControl.OPEN?"OPEN":"CLOSE"));}
        lock.deskChanged();refresh();
    }
    private static void say(Player p,String s){p.displayClientMessage(Component.literal(s),true);}
    public int output(boolean warning){return warning?alarm:horn;}
    public void signalChanged(){refresh();notifyOutputs();}
    public static void tick(Level l,BlockPos p,BlockState s,ControlDeskEntity desk){if(s.getValue(ControlDeskBlock.PART)==1&&l.getGameTime()%5==0)desk.refresh();}
    private void refresh(){
        if(level==null)return;LockEntity lock=controller();DeskControl c=lock==null?null:lock.deskControl();
        int nextAlarm=c!=null&&!c.stopped&&c.warningTicks>0?15:0,nextHorn=c!=null&&!c.stopped&&c.hornTicks>0?15:0;
        if(alarm!=nextAlarm||horn!=nextHorn){alarm=nextAlarm;horn=nextHorn;notifyOutputs();}
        for(int part=0;part<3;part++) {
            BlockPos p=worldPosition.relative(getBlockState().getValue(ControlDeskBlock.FACING).getCounterClockWise(),part-1);var s=level.getBlockState(p);if(!s.is(Content.DESK.get()))continue;
            boolean water=part==1,active=lock!=null&&water&&lock.waterMoving();
            int lamp=lock==null?0:water?(active||c.warningTicks>0?3:1):lock.gateLamp(part==0?1:0);
            boolean selection=c!=null&&(water?c.waterSelection==DeskControl.FILL:c.gateSelection[part==0?1:0]);
            var n=s.setValue(ControlDeskBlock.LAMP,lamp).setValue(ControlDeskBlock.SWITCH,selection).setValue(ControlDeskBlock.FLASH,level.getGameTime()%20<10).setValue(ControlDeskBlock.ALERT,c!=null&&c.stopped).setValue(ControlDeskBlock.ACTIVE,active);
            if(!s.equals(n))level.setBlock(p,n,Block.UPDATE_CLIENTS);
        }
        setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
    }
    private void notifyOutputs(){for(int part:new int[]{0,2}){BlockPos p=worldPosition.relative(getBlockState().getValue(ControlDeskBlock.FACING).getCounterClockWise(),part-1);level.updateNeighborsAt(p,Content.DESK.get());level.updateNeighborsAt(p.below(),Content.DESK.get());}}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);if(controller!=null)n.put("Controller",NbtUtils.writeBlockPos(controller));n.putInt("Alarm",alarm);n.putInt("Horn",horn);}
    @Override public void load(CompoundTag n){super.load(n);controller=n.contains("Controller")?NbtUtils.readBlockPos(n.getCompound("Controller")):null;alarm=horn=0;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
