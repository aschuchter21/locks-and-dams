package com.aschuchter21.locksanddams;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class LockEntity extends BlockEntity {
    private CustomLock custom;
    public AABB ownedBounds() {return custom==null?layout().bounds():custom.bounds();}
    private boolean assembled, lowerOpen, upperOpen, mechanical;
    private int units = LockLayout.LOW;
    private String message = "Unassembled. Build the standard chamber, then right-click to validate.";
    public LockEntity(BlockPos pos, BlockState state) { super(Content.LOCK.get(), pos, state); }
    public LockLayout layout() { return new LockLayout(worldPosition, getBlockState().getValue(ControllerBlock.FACING)); }
    public boolean assembled() { return assembled; }
    public int waterUnits() { return custom==null?units:custom.units; }
    public String status() { return custom!=null?custom.message+" | "+custom.width+" x "+(custom.length-1)+" | Level "+(custom.units-custom.low)/16.0+" / "+(custom.high-custom.low)/16.0:message + " | Level " + String.format(java.util.Locale.ROOT, "%.2f / 3.00", (units-LockLayout.LOW)/16.0) + " blocks"; }

    private boolean loaded(BlockPos pos) { return level != null && level.hasChunkAt(pos); }
    private boolean fullyLoaded() {
        AABB b = layout().box(-1,-3,-6,7,6,16);
        for (int x = ((int)b.minX)>>4; x <= (((int)b.maxX-1)>>4); x++)
            for (int z = ((int)b.minZ)>>4; z <= (((int)b.maxZ-1)>>4); z++)
                if (!level.hasChunk(x,z)) return false;
        return true;
    }
    private String require(BlockPos pos, Block block, String name) {
        return level.getBlockState(pos).is(block) ? null : "Missing " + name + " at " + pos.toShortString();
    }
    private boolean solid(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && Block.isShapeFullBlock(state.getCollisionShape(level,pos)) && !state.hasBlockEntity();
    }
    /** Read-only validation always precedes any water or panel writes. */
    public String validate() {
        if(custom!=null) {try {custom.validate(level,true);return null;}catch(IllegalArgumentException e){return e.getMessage();}}
        if (level == null || !fullyLoaded()) return "Paused: the entire lock and both approaches must be loaded.";
        LockLayout l = layout();
        for (int x=0;x<=6;x++) for(int z=0;z<=10;z++)
            if (!solid(l.at(x,-1,z))) return "Floor must be solid and dry at " + l.at(x,-1,z).toShortString();
        String error;
        if ((error=require(l.lowerDrive(),Content.DRIVE.get(),"lower gate drive"))!=null) return error;
        if ((error=require(l.upperDrive(),Content.DRIVE.get(),"upper gate drive"))!=null) return error;
        if ((error=require(l.fill(),Content.FILL.get(),"fill valve"))!=null) return error;
        if ((error=require(l.drain(),Content.DRAIN.get(),"drain valve"))!=null) return error;
        for(int x : new int[]{0,6}) for(int z=0;z<=10;z++) for(int y=0;y<6;y++) {
            BlockPos p=l.at(x,y,z);
            if (p.equals(worldPosition)||p.equals(l.lowerDrive())||p.equals(l.upperDrive())||p.equals(l.fill())||p.equals(l.drain())) continue;
            if(!solid(p)) return "Wall must be solid and dry at " + p.toShortString();
        }
        for(int z : new int[]{0,10}) {
            LockHingeEntity h=hinge(z);
            if(mechanical&&(h==null||!h.belongsTo(worldPosition)||!h.isRunning()||h.getMovedContraption()==null))return "Restore the bound, assembled lock hinge at "+l.at(1,-2,z).toShortString();
            for(int x=1;x<=5;x++) for(int y=0;y<6;y++)
                if(!level.getBlockState(l.at(x,y,z)).is(mechanical?Content.SEAL.get():Content.PANEL.get())) return "Missing gate panel/seal at " + l.at(x,y,z).toShortString();
        }
        for(int x=1;x<=5;x++) for(int z=1;z<=9;z++) for(int y=0;y<6;y++) {
            BlockPos p=l.at(x,y,z); BlockState s=level.getBlockState(p);
            if(!s.isAir()&&!s.is(Content.WATER_BLOCK.get())&&!s.is(Blocks.WATER)) return "Chamber obstructed at " + p.toShortString();
        }
        // Both canal surfaces must be present; this prototype does not consume a finite reservoir.
        for(int side=0;side<2;side++) for(int x=1;x<=5;x++) {
            BlockPos p=l.at(x,side==0?0:3,side==0?-1:11);
            var fluid=level.getFluidState(p);
            if(!fluid.is(FluidTags.WATER)||!fluid.isSource()||!level.getFluidState(p.above()).isEmpty()
                ||Math.abs(fluid.getHeight(level,p)-14/16f)>1/16f)
                return "Restore the " + (side==0?"lower":"upper") + " canal source-water surface at " + p.toShortString();
        }
        return null;
    }
    public boolean assemble() {
        if (level==null||level.isClientSide) return false;
        if (assembled) return true;
        LockHingeEntity low=hinge(0),high=hinge(10);
        mechanical=low!=null&&high!=null&&low.belongsTo(worldPosition)&&high.belongsTo(worldPosition)&&low.isRunning()&&high.isRunning();
        if(custom==null) {
            try {
                CustomLock found=CustomLock.discover(level,worldPosition,getBlockState().getValue(ControllerBlock.FACING));
                found.assemble(level);custom=found;assembled=true;sync();return true;
            }catch(IllegalArgumentException e) {
                // Older two-hinge layouts retain their original assembly path.
                if(validate()!=null) {message=e.getMessage();sync();return false;}
            }
        }
        String error=validate();
        if(error!=null) { message=error; sync(); return false; }
        // A second controller may not own a gate or water cell already used by a loaded lock.
        for(BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-20,-6,-20),worldPosition.offset(20,6,20))) {
            if(!p.equals(worldPosition)&&loaded(p)&&level.getBlockEntity(p) instanceof LockEntity other
                &&other.assembled&&layout().bounds().intersects(other.layout().bounds())) {
                message="Cannot assemble: this chamber overlaps another assembled lock."; sync(); return false;
            }
        }
        if(obstructed(0)||obstructed(10)) { message="Clear both gate openings before assembly."; sync(); return false; }
        int recovered=recoverWater();
        if(recovered==-2) { message="Managed water is inconsistent; restore missing water cells before reclaiming this chamber."; sync(); return false; }
        if(recovered<0&&!level.getEntities((Entity)null,layout().box(1,0,1,5,5,9),e -> e.isAlive()).isEmpty()) {
            message="Clear the chamber of boats and entities before initial assembly."; sync(); return false;
        }
        assembled=true; units=recovered<0?LockLayout.LOW:recovered; lowerOpen=mechanical&&low.open(); upperOpen=mechanical&&high.open();
        writeWater(); writeGate(0,lowerOpen); writeGate(10,upperOpen);
        message=recovered<0?"Ready. Power the lower gate drive to enter.":"Recovered the existing chamber water level."; sync(); return true;
    }
    private int recoverWater() {
        int found=-1;
        for(int x=1;x<=5;x++) for(int z=1;z<=9;z++) for(int y=0;y<6;y++) {
            BlockState s=level.getBlockState(layout().at(x,y,z));
            if(s.is(Content.WATER_BLOCK.get())) found=Math.max(found,y*16+s.getValue(ChamberFluid.HEIGHT));
        }
        if(found<0) return -1;
        if(found<LockLayout.LOW||found>LockLayout.HIGH) return -2;
        for(int x=1;x<=5;x++) for(int z=1;z<=9;z++) for(int y=0;y<6;y++) {
            BlockState s=level.getBlockState(layout().at(x,y,z));int depth=LockLayout.depth(found,y);
            if(depth==0?!s.isAir():!s.is(Content.WATER_BLOCK.get())||s.getValue(ChamberFluid.HEIGHT)!=depth) return -2;
        }
        return found;
    }
    private boolean obstructed(int z) { return !level.getEntities((Entity)null, layout().gateBox(z).inflate(.25), e -> e.isAlive()&&!e.isSpectator()&&!(e instanceof com.simibubi.create.content.contraptions.AbstractContraptionEntity)).isEmpty(); }
    private LockHingeEntity hinge(int z) {
        return level.getBlockEntity(layout().at(1,-2,z)) instanceof LockHingeEntity h?h:null;
    }
    private void connectHinges() {
        if(mechanical||lowerOpen||upperOpen||obstructed(0)||obstructed(10))return;
        LockHingeEntity a=hinge(0),b=hinge(10);
        if(a==null||b==null)return;
        a.bind(layout(),0);b.bind(layout(),10);a.assemble();b.assemble();
        mechanical=a.isRunning()&&b.isRunning();
        if(!mechanical) {a.disassemble();b.disassemble();}
    }
    private boolean signal(BlockPos pos) { return level.hasNeighborSignal(pos); }
    private void control(BlockPos pos, boolean open) {
        if(!loaded(pos)) return;
        BlockState s=level.getBlockState(pos);
        if(s.getBlock() instanceof ControlBlock) {
            BlockState next=s.setValue(ControlBlock.POWERED,signal(pos)).setValue(ControlBlock.OPEN,open);
            if(!s.equals(next)) level.setBlock(pos,next,Block.UPDATE_CLIENTS);
        }
    }
    private boolean gate(int z, boolean current, boolean wanted) {
        if(mechanical) {
            LockHingeEntity h=hinge(z);
            // Do not restore the closed seal while a boat occupies the opening.
            if(!wanted&&current&&obstructed(z))wanted=true;
            h.request(wanted);
            boolean open=h.open()&&wanted;
            writeGate(z,open);return open;
        }
        if(current==wanted) return current;
        if(!wanted&&obstructed(z)) return true;
        writeGate(z,wanted); return wanted;
    }
    private void writeGate(int z, boolean open) {
        LockLayout l=layout();
        for(int x=1;x<=5;x++) for(int y=0;y<6;y++) {
            BlockPos p=l.at(x,y,z); BlockState s=level.getBlockState(p);
            if(!s.is(Content.PANEL.get())&&!s.is(Content.SEAL.get())) continue;
            BlockState next=s.setValue(GatePanelBlock.AXIS,l.forward().getAxis()).setValue(GatePanelBlock.OPEN,open).setValue(GatePanelBlock.DEPTH,open?LockLayout.depth(units,y):0);
            if(!s.equals(next)) level.setBlock(p,next,Block.UPDATE_CLIENTS);
        }
    }
    private void writeWater() {
        LockLayout l=layout();
        for(int x=1;x<=5;x++) for(int z=1;z<=9;z++) for(int y=0;y<6;y++) {
            BlockPos p=l.at(x,y,z); int depth=LockLayout.depth(units,y);
            BlockState next=depth==0?Blocks.AIR.defaultBlockState():Content.WATER_BLOCK.get().defaultBlockState().setValue(ChamberFluid.HEIGHT,depth);
            if(!level.getBlockState(p).equals(next)) level.setBlock(p,next,Block.UPDATE_CLIENTS);
        }
    }
    public static void tick(Level level, BlockPos pos, BlockState state, LockEntity lock) {
        if(level.getGameTime()%5==0 && lock.assembled) lock.step();
    }
    private void step() {
        if(custom!=null) {custom.step(level);sync();return;}
        LockLayout l=layout();
        String error=validate();
        if(error!=null) {
            message="Paused: "+error; control(l.fill(),false); control(l.drain(),false); sync(); return;
        }
        connectHinges();
        boolean wantLower=signal(l.lowerDrive()),wantUpper=signal(l.upperDrive());
        boolean fill=signal(l.fill()),drain=signal(l.drain());
        lowerOpen=gate(0,lowerOpen,wantLower&&units==LockLayout.LOW);
        upperOpen=gate(10,upperOpen,wantUpper&&units==LockLayout.HIGH);
        boolean closed=!lowerOpen&&!upperOpen&&(!mechanical||(hinge(0).closed()&&hinge(10).closed()));
        String route=fill&&!drain?CulvertRoute.validate(level,l,true):drain&&!fill?CulvertRoute.validate(level,l,false):null;
        if(!closed) message="Gate open or moving: close both gates before moving water.";
        else if(fill&&drain) message="Paused: fill and drain are both powered.";
        else if(route!=null) message="Paused: "+route;
        else if(fill&&units<LockLayout.HIGH) { units++; message="Filling"; }
        else if(drain&&units>LockLayout.LOW) { units--; message="Draining"; }
        else if((wantLower&&units!=LockLayout.LOW)||(wantUpper&&units!=LockLayout.HIGH)) message="Gate interlock: water levels differ.";
        else message="Ready";
        control(l.lowerDrive(),lowerOpen); control(l.upperDrive(),upperOpen);
        control(l.fill(),fill&&!drain&&closed&&route==null&&units<LockLayout.HIGH);
        control(l.drain(),drain&&!fill&&closed&&route==null&&units>LockLayout.LOW);
        // Also repairs isolated vanilla water/air changes after successful structural validation.
        writeWater();
        if(lowerOpen) writeGate(0,true);
        if(upperOpen) writeGate(10,true);
        sync();
    }
    private String lastSynced="";
    private void sync() {
        setChanged();
        String current=assembled+":"+waterUnits()+":"+lowerOpen+":"+upperOpen+":"+status();
        if(level!=null&&!current.equals(lastSynced)) {
            lastSynced=current;
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putBoolean("Assembled",assembled); tag.putBoolean("Mechanical",mechanical); tag.putInt("WaterUnits",units);
        if(custom!=null)tag.put("CustomLock",custom.save());
        tag.putBoolean("LowerOpen",lowerOpen); tag.putBoolean("UpperOpen",upperOpen); tag.putString("Status",message);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); assembled=tag.getBoolean("Assembled"); mechanical=tag.getBoolean("Mechanical"); units=Math.max(LockLayout.LOW,Math.min(LockLayout.HIGH,tag.getInt("WaterUnits")));
        custom=tag.contains("CustomLock")?CustomLock.load(worldPosition,tag.getCompound("CustomLock")):null;
        lowerOpen=tag.getBoolean("LowerOpen"); upperOpen=tag.getBoolean("UpperOpen"); message=tag.getString("Status");
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
