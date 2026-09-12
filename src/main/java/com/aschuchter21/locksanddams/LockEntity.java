package com.aschuchter21.locksanddams;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
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
    private BlockPos desk;
    private final DeskControl deskControl=new DeskControl();
    private boolean waterMoving;
    private int lastOutputs;
    private boolean deskHealthy=true;
    public BlockPos deskPosition(){return desk;}
    public DeskControl deskControl(){return deskControl;}
    public boolean waterMoving(){return waterMoving;}
    public boolean deskAvailable(){return desk!=null&&level.hasChunkAt(desk)&&level.getBlockEntity(desk) instanceof ControlDeskEntity d&&d.controller()==this;}
    public boolean deskStop(){return deskControl.stopped||desk!=null&&(!deskAvailable()||!deskHealthy);}
    public int gateCommand(int side){return desk==null?-1:deskControl.gate[side];}
    public int waterCommand(){return desk==null?0:deskControl.waterCommand();}
    public void linkDesk(BlockPos p){desk=p.immutable();deskControl.reset();deskChanged();}
    public void unlinkDesk(){deskChangedStop();desk=null;deskChanged();}
    public void deskChangedStop(){deskControl.stop();deskChanged();}
    public void deskChanged(){
        if(level==null)return;
        if(deskControl.stopped||deskControl.gate[0]==DeskControl.HOLD&&deskControl.gate[1]==DeskControl.HOLD) {
            for(int side=0;side<2;side++)for(var h:gateHinges(side))if(h!=null)h.hold();
        }
        if(deskControl.stopped){waterMoving=false;if(custom!=null){custom.control(level,custom.fill,false);custom.control(level,custom.drain,false);}else{control(layout().fill(),false);control(layout().drain(),false);}}
        setChanged();level.updateNeighborsAt(worldPosition,Content.CONTROLLER.get());sync();
    }
    private LockHingeEntity[] gateHinges(int side){return custom==null?new LockHingeEntity[]{hinge(side*10)}:new LockHingeEntity[]{custom.hinge(level,side*2),custom.hinge(level,side*2+1)};}
    public int gateLamp(int side){
        boolean closed=true,open=true,moving=false;
        for(var h:gateHinges(side)){if(h==null||!h.isRunning()||h.getMovedContraption()==null)return 0;closed&=h.closed();open&=h.open();moving|=h.travelling();}
        return closed?1:open?2:moving?3:4;
    }
    public int deskOutput(Direction outward){
        if(desk==null)return 0;
        if(outward==Direction.DOWN)return !deskStop()&&deskControl.warningTicks>0?15:0;
        if(outward==Direction.UP)return 0;
        Direction f=getBlockState().getValue(ControllerBlock.FACING);
        int side=outward==f||outward==f.getClockWise()?1:0;
        boolean reverse=outward==f.getClockWise()||outward==f.getCounterClockWise();
        if(reverse)return deskControl.gate[side]==DeskControl.CLOSE?15:0;
        int command=deskControl.gate[side];boolean stopped=deskStop()||!assembled||command==DeskControl.HOLD||gateLamp(side)==0;
        if(command==DeskControl.OPEN)stopped|=gateLamp(side)==2||waterUnits()!=(custom==null?(side==0?LockLayout.LOW:LockLayout.HIGH):(side==0?custom.low:custom.high));
        if(command==DeskControl.CLOSE)stopped|=gateLamp(side)==1;
        for(var h:gateHinges(side))stopped|=h==null||h.blockedFor(command)||h.catwalkOccupied();
        return stopped?15:0;
    }
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
        if ((error=require(l.fill(),Content.FILL.get(),"fill valve"))!=null) return error;
        if ((error=require(l.drain(),Content.DRAIN.get(),"drain valve"))!=null) return error;
        for(int x : new int[]{0,6}) for(int z=0;z<=10;z++) for(int y=0;y<6;y++) {
            BlockPos p=l.at(x,y,z);
            if (p.equals(worldPosition)||p.equals(l.fill())||p.equals(l.drain())) continue;
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
        // Placement faces the player into the wall. Infer the canal axis from the
        // actual build instead of requiring that incidental direction to match.
        java.util.List<CustomLock> matches=new java.util.ArrayList<>();
        String discoveryError=null;
        int discoveryProgress=-1;
        for(Direction direction:Direction.Plane.HORIZONTAL) {
            try {matches.add(CustomLock.discover(level,worldPosition,direction));}
            catch(IllegalArgumentException e) {
                int progress=e instanceof CustomLock.DiscoveryException failure?failure.progress:0;
                if(progress>discoveryProgress||progress==discoveryProgress&&direction==getBlockState().getValue(ControllerBlock.FACING)) {discoveryError=e.getMessage();discoveryProgress=progress;}
            }
        }
        if(matches.size()>1) {message="Multiple valid chambers match this controller; separate their hinges.";sync();return false;}
        if(matches.size()==1) {
            CustomLock found=matches.get(0);
            try {found.assemble(level);}catch(IllegalArgumentException e){message=e.getMessage();sync();return false;}
            face(found.forward);custom=found;assembled=true;sync();return true;
        }
        BlockState original=getBlockState();
        java.util.List<Direction> legacyMatches=new java.util.ArrayList<>();
        try {
            for(Direction direction:Direction.Plane.HORIZONTAL) {
                setBlockState(original.setValue(ControllerBlock.FACING,direction));
                LockHingeEntity a=hinge(0),b=hinge(10);
                mechanical=a!=null&&b!=null&&a.belongsTo(worldPosition)&&b.belongsTo(worldPosition)&&a.isRunning()&&b.isRunning();
                if(validate()==null)legacyMatches.add(direction);
            }
        }finally {setBlockState(original);mechanical=false;}
        if(legacyMatches.size()!=1) {message=legacyMatches.size()>1?"Multiple legacy chambers match this controller.":discoveryError==null?"No complete chamber found. Restore its hinges, walls and canal connections.":discoveryError;sync();return false;}
        face(legacyMatches.get(0));
        LockHingeEntity low=hinge(0),high=hinge(10);
        mechanical=low!=null&&high!=null&&low.belongsTo(worldPosition)&&high.belongsTo(worldPosition)&&low.isRunning()&&high.isRunning();
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
    private void face(Direction direction) {
        BlockState state=getBlockState().setValue(ControllerBlock.FACING,direction);
        level.setBlock(worldPosition,state,Block.UPDATE_CLIENTS);setBlockState(state);
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
            BlockState next=s.setValue(ControlBlock.POWERED,signal(pos)).setValue(ControlBlock.OPEN,open).setValue(ControlBlock.FACING,layout().forward().getCounterClockWise());
            if(!s.equals(next)) level.setBlock(pos,next,Block.UPDATE_CLIENTS);
        }
    }
    private boolean gate(int z, boolean current, boolean wanted) {
        if(mechanical) {
            LockHingeEntity h=hinge(z);
            // Do not restore the closed seal while a boat occupies the opening.
            if(!wanted&&current&&obstructed(z))wanted=true;
            int side=z==0?0:1;
            h.followRotation(units==(side==0?LockLayout.LOW:LockLayout.HIGH),deskStop(),gateCommand(side));
            boolean open=h.open();
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
        if(lock.desk!=null) {
            lock.deskControl.tick(lock.deskAvailable()&&lock.assembled&&lock.deskHealthy,lock.gateLamp(0)==2,lock.gateLamp(1)==2,level.getGameTime());
            if(!lock.deskAvailable()||lock.deskControl.stopped)for(int side=0;side<2;side++)for(var h:lock.gateHinges(side))if(h!=null)h.hold();
            int outputs=0;for(Direction d:Direction.values())outputs=outputs*16+lock.deskOutput(d);if(lock.deskControl.hornTicks>0)outputs|=1<<24;
            if(outputs!=lock.lastOutputs){lock.lastOutputs=outputs;level.updateNeighborsAt(pos,Content.CONTROLLER.get());if(level.hasChunkAt(lock.desk)&&level.getBlockEntity(lock.desk) instanceof ControlDeskEntity d)d.signalChanged();}
        }
        int before=lock.waterUnits();
        if(level.getGameTime()%5==0 && lock.assembled) lock.step();
        if(level.getGameTime()%5==0){lock.waterMoving=before!=lock.waterUnits();}
    }
    private void step() {
        deskHealthy=validate()==null;
        if(custom!=null) {custom.step(level,this);sync();return;}
        LockLayout l=layout();
        String error=validate();
        if(error!=null) {
            message="Paused: "+error; control(l.fill(),false); control(l.drain(),false); sync(); return;
        }
        connectHinges();
        boolean wantLower=hinge(0)!=null&&hinge(0).getSpeed()>0,wantUpper=hinge(10)!=null&&hinge(10).getSpeed()>0;
        boolean fill=!deskStop()&&(desk==null?signal(l.fill()):waterCommand()==DeskControl.FILL),drain=!deskStop()&&(desk==null?signal(l.drain()):waterCommand()==DeskControl.DRAIN);
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
        tag.putBoolean("RotationControls",true);
        if(desk!=null)tag.put("Desk",NbtUtils.writeBlockPos(desk));tag.put("DeskControl",deskControl.save());
        super.saveAdditional(tag); tag.putBoolean("Assembled",assembled); tag.putBoolean("Mechanical",mechanical); tag.putInt("WaterUnits",units);
        if(custom!=null)tag.put("CustomLock",custom.save());
        tag.putBoolean("LowerOpen",lowerOpen); tag.putBoolean("UpperOpen",upperOpen); tag.putString("Status",message);
    }
    @Override public void load(CompoundTag tag) {
        desk=tag.contains("Desk")?NbtUtils.readBlockPos(tag.getCompound("Desk")):null;deskControl.load(tag.getCompound("DeskControl"));
        if(tag.getBoolean("Assembled")&&!tag.getBoolean("RotationControls"))deskControl.stop();
        super.load(tag); assembled=tag.getBoolean("Assembled"); mechanical=tag.getBoolean("Mechanical"); units=Math.max(LockLayout.LOW,Math.min(LockLayout.HIGH,tag.getInt("WaterUnits")));
        custom=tag.contains("CustomLock")?CustomLock.load(worldPosition,tag.getCompound("CustomLock")):null;
        lowerOpen=tag.getBoolean("LowerOpen"); upperOpen=tag.getBoolean("UpperOpen"); message=tag.getString("Status");
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
