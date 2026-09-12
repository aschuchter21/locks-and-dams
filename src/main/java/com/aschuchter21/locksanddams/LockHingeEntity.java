package com.aschuchter21.locksanddams;

import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.contraptions.bearing.*;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.util.List;

public final class LockHingeEntity extends MechanicalBearingBlockEntity {
    private BlockPos owner;
    private GateSpec leaf;
    private Direction forward=Direction.NORTH;
    private int side;
    private float target;
    private long permitUntil;
    private boolean obstructed;
    private String obstruction="";
    private AbstractContraptionEntity modeledContraption;
    public LockHingeEntity(BlockPos p,BlockState s) { super(Content.HINGE_ENTITY.get(),p,s); }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);movementMode.setValue(RotationMode.ROTATE_NEVER_PLACE.ordinal());
    }
    public boolean belongsTo(BlockPos p) { return owner!=null&&owner.equals(p); }
    /** An unloaded or still-present controller retains its gates. */
    public boolean canBindTo(BlockPos controller) {
        return owner==null||owner.equals(controller)||level!=null&&level.hasChunkAt(owner)
            &&!level.getBlockState(owner).is(Content.CONTROLLER.get())&&!(level.getBlockEntity(owner) instanceof LockEntity);
    }
    public boolean closed() { return running&&movedContraption!=null&&Math.abs(angle)<.01&&Math.abs(target)<.01; }
    public boolean open() { return running&&movedContraption!=null&&Math.abs(angle-openAngle())<.01; }
    public float gateAngle() { return angle; }
    private float openAngle() { return leaf==null?(side==0?-90:90):leaf.openAngle(); }
    public boolean blocked() { return obstructed; }
    public String obstruction() { return obstruction; }
    public void bind(LockLayout l,int z) { forward=l.forward();side=z;bind(l.origin(),new GateSpec(l.at(1,0,z),forward.getClockWise(),5,6,z==0?-90:90)); }
    public void bind(BlockPos controller,GateSpec geometry) { owner=controller;leaf=geometry;setChanged(); }
    public GateSpec geometry() {return leaf;}
    public void preparePanelEdit(){
        if(level!=null&&owner!=null&&level.getBlockEntity(owner) instanceof LockEntity lock)lock.beginGateEdit();
        disassemble();
    }
    public boolean catwalkOccupied() {
        if(level==null||leaf==null)return false;
        Vec3 u=VecHelper.rotate(Vec3.atLowerCornerOf(new BlockPos(leaf.inward().getNormal())),angle,Direction.Axis.Y);
        Vec3 v=new Vec3(-u.z,0,u.x),center=Vec3.atCenterOf(leaf.base()).add(u.scale((leaf.width()-1)/2.0));
        double y=leaf.base().getY()+leaf.height(),r=leaf.width()/2.0+1;
        AABB search=new AABB(center.x-r,y-.1,center.z-r,center.x+r,y+2,center.z+r);
        for(Entity e:level.getEntities((Entity)null,search,e->!e.isSpectator()&&e instanceof LivingEntity))
            if(e.getBoundingBox().minY<=y+.2&&intersects(e.getBoundingBox().inflate(.05),center,u,v,.3125))return true;
        return false;
    }
    public void hold() {if(target!=angle){target=angle;sendData();}permitUntil=level.getGameTime()+6;}
    public void request(boolean open) {
        if(catwalkOccupied()){hold();return;}
        float requested=open?openAngle():0;
        if(target!=requested) { target=requested;sendData(); }
        permitUntil=level.getGameTime()+6;
    }
    public boolean travelling(){return running&&Math.abs(getSpeed())>.001&&Math.abs(target-angle)>.01&&!obstructed&&level!=null&&level.getGameTime()<=permitUntil;}
    public boolean blockedFor(int command){
        if(leaf==null||level==null||command==DeskControl.HOLD)return false;
        float destination=command==DeskControl.OPEN?openAngle():0;
        if(Math.abs(destination-angle)<.01)return false;
        return blockedAt(angle)||blockedAt(angle+Math.copySign(Math.min(.5f,Math.abs(destination-angle)),destination-angle));
    }
    public void followRotation(boolean mayOpen,boolean stopped,int command) {
        if(stopped||Math.abs(getSpeed())<.001){hold();return;}
        boolean opening=getSpeed()>0;
        if(opening&&!mayOpen||command==DeskControl.HOLD||command>=0&&(opening!=(command==DeskControl.OPEN))){hold();return;}
        request(opening);
    }
    @Override public float calculateStressApplied() { return lastStressApplied=16; }
    @Override public float getAngularSpeed() {
        if(level==null||!running||owner==null||Math.abs(target-angle)<.001)return 0;
        if(!level.isClientSide&&level.getGameTime()>permitUntil)return 0;
        if(level.isClientSide&&obstructed)return 0;
        float speed=Math.copySign(Math.min(2,Math.abs(convertToAngular(getSpeed()))),target-angle);
        speed=Math.copySign(Math.min(Math.abs(speed),Math.abs(target-angle)),speed);
        if(!level.isClientSide) {
            obstructed=blockedAt(angle)||blockedAt(angle+speed);
            if(obstructed)return 0;
        }
        return speed;
    }
    private boolean blockedAt(float a) {
        Vec3 pivot=Vec3.atCenterOf(leaf.base());
        Vec3 u=VecHelper.rotate(Vec3.atLowerCornerOf(new BlockPos(leaf.inward().getNormal())),a,Direction.Axis.Y);
        Vec3 v=new Vec3(-u.z,0,u.x);Vec3 center=pivot.add(u.scale((leaf.width()-1)/2.0));
        double radius=leaf.width()/2.0+1;
        double deckY=leaf.base().getY()+leaf.height();
        AABB search=new AABB(center.x-radius,leaf.base().getY(),center.z-radius,center.x+radius,deckY+2,center.z+radius);
        for(Entity e:level.getEntities((Entity)null,search,e -> !e.isSpectator()&&(e instanceof Boat||e instanceof LivingEntity))) {
            AABB body=e.getBoundingBox().inflate(.18);
            if(body.minY<=deckY+.1&&intersects(body,center,u,v,body.minY>=deckY-.2?.3125:.1875))return true;
        }
        for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(search.minX,search.minY,search.minZ),BlockPos.containing(search.maxX,search.maxY-.001,search.maxZ))) {
            if(!level.hasChunkAt(p))return true;
            BlockState state=level.getBlockState(p);
            if(state.is(Content.SEAL.get())||p.getY()>=deckY)continue;
            for(AABB box:state.getCollisionShape(level,p).toAabbs())
                if(intersects(box.move(p),center,u,v,p.getY()==deckY-1?.3125:.1875)) {obstruction=p.toShortString()+" "+state;return true;}
        }
        return false;
    }
    private boolean intersects(AABB box,Vec3 center,Vec3 u,Vec3 v,double halfDepth) {
        if(halfDepth>.2)center=center.add(u.scale(.03125));
        double halfWidth=leaf.width()/2.0-(halfDepth>.2?.09375:.0625);
        double dx=box.getCenter().x-center.x,dz=box.getCenter().z-center.z;
        double ex=box.getXsize()/2,ez=box.getZsize()/2;
        return Math.abs(dx)<ex+halfWidth*Math.abs(u.x)+halfDepth*Math.abs(v.x)-.0001
            &&Math.abs(dz)<ez+halfWidth*Math.abs(u.z)+halfDepth*Math.abs(v.z)-.0001
            &&Math.abs(dx*u.x+dz*u.z)<halfWidth+ex*Math.abs(u.x)+ez*Math.abs(u.z)-.0001
            &&Math.abs(dx*v.x+dz*v.z)<halfDepth+ex*Math.abs(v.x)+ez*Math.abs(v.z)-.0001;
    }
    @Override public void assemble() {
        if(level==null||level.isClientSide||owner==null||running)return;
        if(leaf==null)return;
        GateContraption gate=new GateContraption(leaf);
        if(!gate.assemble(level,worldPosition))return;
        gate.removeBlocksFromWorld(level,BlockPos.ZERO);
        for(int x=0;x<leaf.width();x++)for(int y=0;y<leaf.height();y++)level.setBlock(leaf.at(x,y),Content.SEAL.get().defaultBlockState().setValue(GatePanelBlock.AXIS,leaf.normal()),Block.UPDATE_CLIENTS);
        movedContraption=ControlledContraptionEntity.create(level,this,gate);
        BlockPos anchor=worldPosition.above();movedContraption.setPos(anchor.getX(),anchor.getY(),anchor.getZ());
        movedContraption.setRotationAxis(Direction.Axis.Y);level.addFreshEntity(movedContraption);
        running=true;angle=0;target=0;assembleNextTick=false;sendData();
    }
    /** Return only reserved cells; never place a rotated gate over somebody's construction. */
    @Override public void disassemble() {
        if(level!=null&&!level.isClientSide&&movedContraption!=null&&owner!=null) {
            for(int x=0;x<leaf.width();x++)for(int y=0;y<leaf.height();y++) {
                BlockPos p=leaf.at(x,y);BlockState s=level.getBlockState(p);
                if(s.is(Content.SEAL.get()))level.setBlock(p,Content.PANEL.get().defaultBlockState()
                    .setValue(GatePanelBlock.OPEN,false).setValue(GatePanelBlock.DEPTH,0)
                    .setValue(GatePanelBlock.AXIS,leaf.normal()),Block.UPDATE_CLIENTS);
                else Block.popResource(level,worldPosition,new net.minecraft.world.item.ItemStack(Content.PANEL.get()));
            }
            movedContraption.discard();
        }
        movedContraption=null;running=false;assembleNextTick=false;angle=0;target=0;
        if(level!=null)sendData();
    }
    @Override public void tick() {
        if(movementMode!=null&&movementMode.get()!=RotationMode.ROTATE_NEVER_PLACE)movementMode.setValue(RotationMode.ROTATE_NEVER_PLACE.ordinal());
        // Saved contraptions attach after their controller loads; never discard that pending state.
        if(running&&movedContraption==null)assembleNextTick=false;
        super.tick();
        if(level!=null&&!level.isClientSide&&leaf!=null&&movedContraption!=null&&modeledContraption!=movedContraption) {
            // Upgrade saved gate entities in place, preserving their angle and identity.
            var contraption=movedContraption.getContraption();
            for(var entry:List.copyOf(contraption.getBlocks().entrySet())) {
                var info=entry.getValue();
                if(info.state().is(Content.PANEL.get())) {
                    int column=Math.abs(entry.getKey().getX())+Math.abs(entry.getKey().getZ());
                    int topY=leaf.base().getY()-worldPosition.getY()-1+leaf.height()-1;
                    var state=info.state().setValue(GatePanelBlock.TOP,entry.getKey().getY()==topY).setValue(GatePanelBlock.EDGE,edge(leaf,column));
                    if(!state.equals(info.state()))movedContraption.setBlock(entry.getKey(),new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(info.pos(),state,info.nbt()));
                }
            }
            contraption.invalidateColliders();modeledContraption=movedContraption;
        }
        if(level!=null&&!level.isClientSide&&running&&level.getGameTime()%5==0) {setChanged();sendData();}
    }
    private static int edge(GateSpec leaf,int column) {
        boolean positive=leaf.inward()==Direction.EAST||leaf.inward()==Direction.SOUTH;
        return column==0?(positive?1:2):column==leaf.width()-1?(positive?3:4):0;
    }
    @Override public void write(CompoundTag tag,boolean packet) {
        super.write(tag,packet);if(leaf!=null)tag.put("Leaf",leaf.save());if(owner!=null)tag.put("LockOwner",NbtUtils.writeBlockPos(owner));
        tag.putInt("LockDirection",forward.get2DDataValue());tag.putInt("LockSide",side);tag.putFloat("LockTarget",target);
        tag.putBoolean("LockBlocked",obstructed||level!=null&&level.getGameTime()>permitUntil);
    }
    @Override protected void read(CompoundTag tag,boolean packet) {
        super.read(tag,packet);owner=tag.contains("LockOwner")?NbtUtils.readBlockPos(tag.getCompound("LockOwner")):null;
        forward=Direction.from2DDataValue(tag.getInt("LockDirection"));side=tag.getInt("LockSide");target=tag.getFloat("LockTarget");
        leaf=tag.contains("Leaf")?GateSpec.load(tag.getCompound("Leaf")):owner==null?null:new GateSpec(new LockLayout(owner,forward).at(1,0,side),forward.getClockWise(),5,6,side==0?-90:90);
        if(packet) { angle=tag.getFloat("Angle");clientAngleDiff=0;obstructed=tag.getBoolean("LockBlocked"); }
    }
    private static final class GateContraption extends BearingContraption {
        private final GateSpec leaf;
        GateContraption(GateSpec geometry) { super(false,Direction.UP);leaf=geometry; }
        @Override public boolean assemble(Level level,BlockPos pos) {
            anchor=pos.above();bounds=new AABB(BlockPos.ZERO);
            for(int x=0;x<leaf.width();x++)for(int y=0;y<leaf.height();y++)if(!level.getBlockState(leaf.at(x,y)).is(Content.PANEL.get()))return false;
            for(int x=0;x<leaf.width();x++)for(int y=0;y<leaf.height();y++) {
                BlockPos p=leaf.at(x,y);
                level.setBlock(p,level.getBlockState(p).setValue(GatePanelBlock.OPEN,false).setValue(GatePanelBlock.TOP,y==leaf.height()-1).setValue(GatePanelBlock.EDGE,edge(leaf,x)).setValue(GatePanelBlock.DEPTH,0).setValue(GatePanelBlock.AXIS,leaf.normal()),Block.UPDATE_CLIENTS);
                addBlock(level,p,capture(level,p));
            }
            startMoving(level);expandBoundsAroundAxis(Direction.Axis.Y);return true;
        }
    }
}
