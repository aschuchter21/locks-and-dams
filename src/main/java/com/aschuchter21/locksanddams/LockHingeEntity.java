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
    private Direction forward=Direction.NORTH;
    private int side;
    private float target;
    private long permitUntil;
    private boolean obstructed;
    private String obstruction="";
    public LockHingeEntity(BlockPos p,BlockState s) { super(Content.HINGE_ENTITY.get(),p,s); }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);movementMode.setValue(RotationMode.ROTATE_NEVER_PLACE.ordinal());
    }
    public boolean belongsTo(BlockPos p) { return owner!=null&&owner.equals(p); }
    public boolean closed() { return running&&movedContraption!=null&&Math.abs(angle)<.01&&Math.abs(target)<.01; }
    public boolean open() { return running&&movedContraption!=null&&Math.abs(angle-openAngle())<.01; }
    public float gateAngle() { return angle; }
    private float openAngle() { return side==0?-90:90; }
    public boolean blocked() { return obstructed; }
    public String obstruction() { return obstruction; }
    public void bind(LockLayout l,int z) { owner=l.origin();forward=l.forward();side=z;setChanged(); }
    public void request(boolean open) {
        float requested=open?openAngle():0;
        if(target!=requested) { target=requested;sendData(); }
        permitUntil=level.getGameTime()+6;
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
        LockLayout l=new LockLayout(owner,forward);
        Vec3 pivot=Vec3.atCenterOf(l.at(1,0,side));
        Vec3 u=VecHelper.rotate(Vec3.atLowerCornerOf(new BlockPos(forward.getClockWise().getNormal())),a,Direction.Axis.Y);
        Vec3 v=new Vec3(-u.z,0,u.x);Vec3 center=pivot.add(u.scale(2));
        AABB search=new AABB(center.x-4,owner.getY(),center.z-4,center.x+4,owner.getY()+6,center.z+4);
        for(Entity e:level.getEntities((Entity)null,search,e -> !e.isSpectator()&&(e instanceof Boat||e instanceof LivingEntity))) {
            if(intersects(e.getBoundingBox().inflate(.18),center,u,v))return true;
        }
        for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(search.minX,search.minY,search.minZ),BlockPos.containing(search.maxX,search.maxY-.001,search.maxZ))) {
            if(!level.hasChunkAt(p))return true;
            BlockState state=level.getBlockState(p);
            if(state.is(Content.SEAL.get()))continue;
            for(AABB box:state.getCollisionShape(level,p).toAabbs())
                if(intersects(box.move(p),center,u,v)) {obstruction=p.toShortString()+" "+state;return true;}
        }
        return false;
    }
    private static boolean intersects(AABB box,Vec3 center,Vec3 u,Vec3 v) {
        double dx=box.getCenter().x-center.x,dz=box.getCenter().z-center.z;
        double ex=box.getXsize()/2,ez=box.getZsize()/2;
        return Math.abs(dx)<ex+2.46875*Math.abs(u.x)+.125*Math.abs(v.x)-.0001
            &&Math.abs(dz)<ez+2.46875*Math.abs(u.z)+.125*Math.abs(v.z)-.0001
            &&Math.abs(dx*u.x+dz*u.z)<2.46875+ex*Math.abs(u.x)+ez*Math.abs(u.z)-.0001
            &&Math.abs(dx*v.x+dz*v.z)<.125+ex*Math.abs(v.x)+ez*Math.abs(v.z)-.0001;
    }
    @Override public void assemble() {
        if(level==null||level.isClientSide||owner==null||running)return;
        LockLayout l=new LockLayout(owner,forward);
        GateContraption gate=new GateContraption(l,side);
        if(!gate.assemble(level,worldPosition))return;
        gate.removeBlocksFromWorld(level,BlockPos.ZERO);
        for(int x=1;x<=5;x++)for(int y=0;y<6;y++)level.setBlock(l.at(x,y,side),Content.SEAL.get().defaultBlockState().setValue(GatePanelBlock.AXIS,forward.getAxis()),Block.UPDATE_CLIENTS);
        movedContraption=ControlledContraptionEntity.create(level,this,gate);
        BlockPos anchor=worldPosition.above();movedContraption.setPos(anchor.getX(),anchor.getY(),anchor.getZ());
        movedContraption.setRotationAxis(Direction.Axis.Y);level.addFreshEntity(movedContraption);
        running=true;angle=0;target=0;assembleNextTick=false;sendData();
    }
    /** Return only reserved cells; never place a rotated gate over somebody's construction. */
    @Override public void disassemble() {
        if(level!=null&&!level.isClientSide&&movedContraption!=null&&owner!=null) {
            LockLayout l=new LockLayout(owner,forward);
            for(int x=1;x<=5;x++)for(int y=0;y<6;y++) {
                BlockPos p=l.at(x,y,side);BlockState s=level.getBlockState(p);
                if(s.is(Content.SEAL.get()))level.setBlock(p,Content.PANEL.get().defaultBlockState()
                    .setValue(GatePanelBlock.OPEN,s.getValue(GatePanelBlock.OPEN)).setValue(GatePanelBlock.DEPTH,s.getValue(GatePanelBlock.DEPTH))
                    .setValue(GatePanelBlock.AXIS,forward.getAxis()),Block.UPDATE_CLIENTS);
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
        if(level!=null&&!level.isClientSide&&running&&level.getGameTime()%5==0) {setChanged();sendData();}
    }
    @Override public void write(CompoundTag tag,boolean packet) {
        super.write(tag,packet);if(owner!=null)tag.put("LockOwner",NbtUtils.writeBlockPos(owner));
        tag.putInt("LockDirection",forward.get2DDataValue());tag.putInt("LockSide",side);tag.putFloat("LockTarget",target);
        tag.putBoolean("LockBlocked",obstructed||level!=null&&level.getGameTime()>permitUntil);
    }
    @Override protected void read(CompoundTag tag,boolean packet) {
        super.read(tag,packet);owner=tag.contains("LockOwner")?NbtUtils.readBlockPos(tag.getCompound("LockOwner")):null;
        forward=Direction.from2DDataValue(tag.getInt("LockDirection"));side=tag.getInt("LockSide");target=tag.getFloat("LockTarget");
        if(packet) { angle=tag.getFloat("Angle");clientAngleDiff=0;obstructed=tag.getBoolean("LockBlocked"); }
    }
    private static final class GateContraption extends BearingContraption {
        private final LockLayout layout;private final int side;
        GateContraption(LockLayout l,int z) { super(false,Direction.UP);layout=l;side=z; }
        @Override public boolean assemble(Level level,BlockPos pos) {
            anchor=pos.above();bounds=new AABB(BlockPos.ZERO);
            for(int x=1;x<=5;x++)for(int y=0;y<6;y++)if(!level.getBlockState(layout.at(x,y,side)).is(Content.PANEL.get()))return false;
            for(int x=1;x<=5;x++)for(int y=0;y<6;y++) {
                BlockPos p=layout.at(x,y,side);
                level.setBlock(p,level.getBlockState(p).setValue(GatePanelBlock.OPEN,false).setValue(GatePanelBlock.DEPTH,0).setValue(GatePanelBlock.AXIS,layout.forward().getAxis()),Block.UPDATE_CLIENTS);
                addBlock(level,p,capture(level,p));
            }
            startMoving(level);expandBoundsAroundAxis(Direction.Axis.Y);return true;
        }
    }
}
