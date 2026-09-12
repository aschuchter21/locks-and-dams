package com.aschuchter21.locksanddams;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import static com.aschuchter21.locksanddams.WaterConnection.require;

/** Discovered rectangular chamber. Coordinates refer to the lower-left gate panel. */
public final class CustomLock {
    final BlockPos owner,base;
    final Direction forward;
    final int width,length,rise,low,high;
    final BlockPos fill,drain,fillPort,drainPort;
    final GateSpec[] leaves;
    int units;
    String message="Ready";
    CustomLock(BlockPos owner,BlockPos base,Direction forward,int width,int length,int rise,int low,int high,BlockPos fill,BlockPos drain,BlockPos fillPort,BlockPos drainPort,GateSpec[] leaves,int units) {
        this.owner=owner;this.base=base;this.forward=forward;this.width=width;this.length=length;this.rise=rise;this.low=low;this.high=high;
        this.fill=fill;this.drain=drain;this.fillPort=fillPort;this.drainPort=drainPort;this.leaves=leaves;this.units=units;
    }
    BlockPos at(int x,int y,int z) {return base.relative(forward.getClockWise(),x).relative(forward,z).above(y);}
    AABB bounds() {return box(-1,-1,0,width,(high+15)/16,length);}
    AABB box(int x0,int y0,int z0,int x1,int y1,int z1) {
        BlockPos a=at(x0,y0,z0),b=at(x1,y1,z1);
        return new AABB(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()),Math.max(a.getX(),b.getX())+1,Math.max(a.getY(),b.getY())+1,Math.max(a.getZ(),b.getZ())+1);
    }
    LockHingeEntity hinge(Level level,int i) {return level.getBlockEntity(leaves[i].base().below()) instanceof LockHingeEntity h?h:null;}
    static int along(BlockPos p,Direction d) {return p.getX()*d.getStepX()+p.getZ()*d.getStepZ();}
    static List<LockHingeEntity> nearby(Level level,BlockPos controller) {
        List<LockHingeEntity> hinges=new ArrayList<>();
        for(int cx=(controller.getX()-64)>>4;cx<=(controller.getX()+64)>>4;cx++)for(int cz=(controller.getZ()-64)>>4;cz<=(controller.getZ()+64)>>4;cz++) {
            if(!level.hasChunk(cx,cz))continue;
            for(var be:level.getChunk(cx,cz).getBlockEntities().values())if(be instanceof LockHingeEntity h&&Math.abs(h.getBlockPos().getY()-controller.getY())<=32)hinges.add(h);
        }
        require(hinges.size()<=64,"Too many nearby hinges; separate the lock builds.");return hinges;
    }
    public static CustomLock discover(Level level,BlockPos owner,Direction f) {
        int[] progress={0};
        try {return discover(level,owner,f,progress);}
        catch(IllegalArgumentException error) {throw new DiscoveryException(error.getMessage(),progress[0]);}
    }
    public static final class DiscoveryException extends IllegalArgumentException {
        final int progress;
        DiscoveryException(String message,int progress) {super(message);this.progress=progress;}
    }
    private static CustomLock discover(Level level,BlockPos owner,Direction f,int[] progress) {
        List<LockHingeEntity> hinges=nearby(level,owner);Direction right=f.getClockWise();List<BlockPos[]> candidates=new ArrayList<>();
        for(var a:hinges)for(var b:hinges) {
            BlockPos p=a.getBlockPos(),q=b.getBlockPos();int w=along(q,right)-along(p,right)+1;
            if(w<4||w>16||p.getY()!=q.getY()||along(p,f)!=along(q,f))continue;
            int ox=along(owner,right)-along(p,right),oz=along(owner,f)-along(p,f);
            if(ox!=-1&&ox!=w)continue;
            for(var c:hinges) {
                BlockPos r=c.getBlockPos();int len=along(r,f)-along(p,f),dy=r.getY()-p.getY();
                if(along(r,right)!=along(p,right)||len<4||len>48||dy<0||dy>16||oz<0||oz>len)continue;
                BlockPos s=r.relative(right,w-1);
                if(hinges.stream().anyMatch(h->h.getBlockPos().equals(s)))candidates.add(new BlockPos[]{p,q,r,s});
            }
        }
        if(!candidates.isEmpty())progress[0]=1;
        require(candidates.size()==1,candidates.isEmpty()?"Place four rectangular corner hinges and a controller in a side wall. Width 4-16, interior length 3-47, hinge rise 0-16.":"Multiple hinge rectangles match this controller; remove extra hinges or separate the locks.");
        BlockPos[] corners=candidates.get(0);BlockPos base=corners[0].above();int w=along(corners[1],right)-along(corners[0],right)+1,len=along(corners[2],f)-along(corners[0],f),rise=corners[2].getY()-corners[0].getY();
        BlockPos fill=null,drain=null;
        for(int x:new int[]{-1,w})for(int z=0;z<=len;z++)for(int y=0;y<=32;y++) {
            BlockPos p=base.relative(right,x).relative(f,z).above(y);require(level.hasChunkAt(p),"Load the chamber walls before assembly.");var s=level.getBlockState(p);
            if(s.is(Content.FILL.get())) {require(fill==null,"Use one fill valve per chamber.");fill=p;}
            if(s.is(Content.DRAIN.get())) {require(drain==null,"Use one drain valve per chamber.");drain=p;}
        }
        require(fill!=null&&drain!=null,"Place one fill valve and one drain valve in the chamber side walls.");
        WaterConnection upper=WaterConnection.discover(level,fill),lower=WaterConnection.discover(level,drain);
        int low=lower.surface()-base.getY()*16,high=upper.surface()-base.getY()*16;
        require(low>0&&high>low&&high<=32*16&&high-low<=16*16,"Ports must set a positive lower depth and a lift up to 16 blocks, within a 32-block chamber depth.");
        require(along(lower.mouth(),f)<along(base,f)&&along(upper.mouth(),f)>along(base,f)+len,"Fill port belongs beyond the upper gate; drain port beyond the lower gate.");
        progress[0]=2;
        require(rise*16<high,"Upper gate starts above the upper water surface.");
        require((long)w*(len-1)*((high+15)/16)<=16384,"Chamber exceeds 16,384 managed water blocks.");
        for(BlockPos p:new BlockPos[]{owner,fill,drain})require(p.getY()>=base.getY()&&p.getY()<base.getY()+(high+15)/16,"Place the controller and valves in the side walls below the upper water limit.");
        GateSpec[] leaves=new GateSpec[4];
        for(int i=0;i<4;i++) {
            Direction inward=i%2==0?right:right.getOpposite();int leafWidth=i%2==0?w/2:w-w/2,height=0;
            while(height<32) {
                boolean row=true;for(int x=0;x<leafWidth;x++) {BlockPos p=corners[i].above().relative(inward,x).above(height);require(level.hasChunkAt(p),"Load both gates.");var s=level.getBlockState(p);if(!s.is(Content.PANEL.get())&&!s.is(Content.SEAL.get()))row=false;}
                if(!row)break;height++;
            }
            require(height>0,"Build rectangular Gate Panel leaves directly above each hinge, meeting at the opening's center.");
            require((i<2?0:rise)*16+height*16>=high,"Lower gates must reach above the upper water level; upper gates must contain their canal.");
            leaves[i]=new GateSpec(corners[i].above(),inward,leafWidth,height,(i<2?-90:90)*(i%2==0?1:-1));
        }
        CustomLock result=new CustomLock(owner,base,f,w,len,rise,low,high,fill,drain,upper.port(),lower.port(),leaves,low);
        progress[0]=3;
        result.validate(level,false);return result;
    }
    boolean solid(Level level,BlockPos p) {
        BlockState s=level.getBlockState(p);
        return s.getFluidState().isEmpty()&&Block.isShapeFullBlock(s.getCollisionShape(level,p))&&(!s.hasBlockEntity()||s.getBlock() instanceof com.simibubi.create.content.kinetics.base.KineticBlock);
    }
    void validate(Level level,boolean running) {
        for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(bounds().minX,bounds().minY,bounds().minZ),BlockPos.containing(bounds().maxX-1,bounds().maxY-1,bounds().maxZ-1)))require(level.hasChunkAt(p),"Load the entire chamber.");
        require(level.getBlockState(fill).is(Content.FILL.get())&&level.getBlockState(drain).is(Content.DRAIN.get()),"Restore the linked valves.");
        int top=(high+15)/16;
        for(int x=-1;x<=width;x++)for(int z=0;z<=length;z++) {
            BlockPos floor=at(x,-1,z);boolean lowerHinge=(z==0||rise==0&&z==length)&&(x==0||x==width-1);
            require(lowerHinge?level.getBlockState(floor).is(Content.HINGE.get()):solid(level,floor),"Restore solid chamber floor at "+floor.toShortString());
        }
        for(int x:new int[]{-1,width})for(int z=0;z<=length;z++)for(int y=0;y<top;y++) {
            BlockPos p=at(x,y,z);if(p.equals(owner)||p.equals(fill)||p.equals(drain))continue;
            require(solid(level,p),"Restore solid side wall at "+p.toShortString());
        }
        for(int x=0;x<width;x++)for(int y=0;y<rise;y++) {
            BlockPos p=at(x,y,length);boolean hinge=y==rise-1&&(x==0||x==width-1);
            require(hinge?level.getBlockState(p).is(Content.HINGE.get()):solid(level,p),"Restore the solid wall below the upper gates at "+p.toShortString());
        }
        for(int i=0;i<4;i++) {
            LockHingeEntity h=hinge(level,i);require(h!=null,"Restore all four hinges.");
            if(running)require(h.belongsTo(owner)&&h.isRunning()&&h.getMovedContraption()!=null&&leaves[i].equals(h.geometry()),"Waiting for the linked gate contraption; restore/reassemble if its hinge was removed.");
            else {
                require(h.canBindTo(owner),"A hinge still belongs to another controller. Remove that controller and repair its wall opening first.");
                require(!h.isRunning()||h.getMovedContraption()!=null&&leaves[i].equals(h.geometry()),"A hinge has a different assembled leaf or its contraption is still loading.");
            }
            for(int x=0;x<leaves[i].width();x++)for(int y=0;y<leaves[i].height();y++) {
                BlockPos p=leaves[i].at(x,y);require(level.hasChunkAt(p),"Load both gate leaves.");var s=level.getBlockState(p);
                require(s.is(running||h.isRunning()?Content.SEAL.get():Content.PANEL.get()),"Restore gate panels/seals at "+p.toShortString());
            }
        }
        for(int x=0;x<width;x++)for(int z=1;z<length;z++)for(int y=0;y<top;y++) {
            BlockPos p=at(x,y,z);var s=level.getBlockState(p);require(s.isAir()||s.is(Content.WATER_BLOCK.get())||s.is(Blocks.WATER),"Chamber obstructed at "+p.toShortString());
        }
        for(int x=0;x<width;x++)for(int z=1;z<length;z++)require(!level.getBlockState(at(x,top,z)).is(Content.WATER_BLOCK.get()),"Existing managed water exceeds this port height; restore the original limits before reclaiming.");
        WaterConnection a=WaterConnection.discover(level,fill),b=WaterConnection.discover(level,drain);
        require(a.port().equals(fillPort)&&b.port().equals(drainPort)&&a.surface()==base.getY()*16+high&&b.surface()==base.getY()*16+low,"Canal port or water level changed; restore it before operating.");
        for(int side=0;side<2;side++)for(int x=0;x<width;x++) {
            int surface=side==0?low:high;BlockPos p=at(x,surface/16,side==0?-1:length+1);
            require(level.hasChunkAt(p),"Load both canal approaches.");var fluid=level.getFluidState(p);
            require(fluid.is(net.minecraft.tags.FluidTags.WATER)&&fluid.isSource()&&level.getFluidState(p.above()).isEmpty()&&p.getY()*16+Math.round(fluid.getHeight(level,p)*16)==base.getY()*16+surface,"Canal water across each gate must match its linked port's surface.");
        }
    }
    void assemble(Level level) {
        validate(level,false);
        require(level.getEntities((Entity)null,bounds(),e->e.isAlive()&&!e.isSpectator()&&!(e instanceof com.simibubi.create.content.contraptions.AbstractContraptionEntity)).isEmpty(),"Clear the chamber and gate openings before assembly.");
        for(int cx=(owner.getX()-80)>>4;cx<=(owner.getX()+80)>>4;cx++)for(int cz=(owner.getZ()-80)>>4;cz<=(owner.getZ()+80)>>4;cz++)if(level.hasChunk(cx,cz))
            for(var be:level.getChunk(cx,cz).getBlockEntities().values())if(be instanceof LockEntity other&&!other.getBlockPos().equals(owner)&&other.assembled())require(!bounds().intersects(other.ownedBounds()),"Chamber overlaps another assembled lock.");
        // Uniform managed water can be reclaimed after a controller replacement.
        int found=-1;for(int y=0;y<(high+15)/16;y++) {var s=level.getBlockState(at(0,y,1));if(s.is(Content.WATER_BLOCK.get()))found=y*16+s.getValue(ChamberFluid.HEIGHT);}
        boolean managed=false;for(int x=0;x<width;x++)for(int z=1;z<length;z++)for(int y=0;y<(high+15)/16;y++)if(level.getBlockState(at(x,y,z)).is(Content.WATER_BLOCK.get()))managed=true;
        if(managed) {
            require(found>=low&&found<=high,"Managed water is inconsistent.");
            for(int x=0;x<width;x++)for(int z=1;z<length;z++)for(int y=0;y<(high+15)/16;y++) {
                var s=level.getBlockState(at(x,y,z));int d=LockLayout.depth(found,y);
                require(d==0?s.isAir():s.is(Content.WATER_BLOCK.get())&&s.getValue(ChamberFluid.HEIGHT)==d,"Managed water is inconsistent; restore it before reclaiming.");
            }units=found;
        }
        for(int i=0;i<4;i++) {var h=hinge(level,i);h.bind(owner,leaves[i]);h.assemble();require(h.isRunning(),"Gate assembly failed.");}
        writeWater(level);for(int side=0;side<2;side++)writeGate(level,side,hinge(level,side*2).open()&&hinge(level,side*2+1).open());
        message="Assembled "+width+" x "+(length-1)+" chamber; lift "+(high-low)/16.0+" blocks.";
    }
    boolean occupied(Level level,int side) {return !level.getEntities((Entity)null,box(0,side==0?0:rise,side*length,width-1,(high+15)/16,side*length).inflate(.2),e->e.isAlive()&&!e.isSpectator()&&!(e instanceof com.simibubi.create.content.contraptions.AbstractContraptionEntity)).isEmpty();}
    void step(Level level,LockEntity controller) {
        try {validate(level,true);}catch(IllegalArgumentException error) {message="Paused: "+error.getMessage();control(level,fill,false);control(level,drain,false);return;}
        boolean allClosed=true;
        for(int side=0;side<2;side++) {
            var a=hinge(level,side*2);var b=hinge(level,side*2+1);
            boolean clear=!a.catwalkOccupied()&&!b.catwalkOccupied();
            boolean stop=controller.deskStop()||!clear;
            int command=controller.gateCommand(side);
            boolean mayOpen=units==(side==0?low:high);
            a.followRotation(mayOpen,stop,command);b.followRotation(mayOpen,stop,command);
            writeGate(level,side,a.open()&&b.open());allClosed&=a.closed()&&b.closed();
            for(int i=side*2;i<side*2+2;i++) {
                BlockPos drive=leaves[i].base().below().relative(leaves[i].inward().getOpposite());
                if(level.getBlockState(drive).is(Content.DRIVE.get()))control(level,drive,a.open()&&b.open());
            }
        }
        boolean f=!controller.deskStop()&&(controller.deskPosition()==null?level.hasNeighborSignal(fill):controller.waterCommand()==DeskControl.FILL);
        boolean d=!controller.deskStop()&&(controller.deskPosition()==null?level.hasNeighborSignal(drain):controller.waterCommand()==DeskControl.DRAIN);
        boolean filling=allClosed&&f&&!d&&units<high,draining=allClosed&&d&&!f&&units>low;
        if(filling)units++;if(draining)units--;
        message=!allClosed?"Gate open or moving; waiting for both pairs to close.":f&&d?"Paused: both valves powered.":filling?"Filling":draining?"Draining":"Ready";
        control(level,fill,filling);control(level,drain,draining);writeWater(level);
    }
    void control(Level level,BlockPos p,boolean open) {
        var s=level.getBlockState(p);
        if(s.getBlock() instanceof ControlBlock) {
            Direction right=forward.getClockWise();Direction outward=along(p,right)<along(base,right)?right.getOpposite():right;
            var next=s.setValue(ControlBlock.OPEN,open).setValue(ControlBlock.POWERED,level.hasNeighborSignal(p)).setValue(ControlBlock.FACING,outward);
            if(!s.equals(next))level.setBlock(p,next,Block.UPDATE_CLIENTS);
        }
    }
    void writeWater(Level level) {
        for(int x=0;x<width;x++)for(int z=1;z<length;z++)for(int y=0;y<(high+15)/16;y++) {
            int d=LockLayout.depth(units,y);var s=d==0?Blocks.AIR.defaultBlockState():Content.WATER_BLOCK.get().defaultBlockState().setValue(ChamberFluid.HEIGHT,d);BlockPos p=at(x,y,z);if(!level.getBlockState(p).equals(s))level.setBlock(p,s,Block.UPDATE_CLIENTS);
        }
    }
    void writeGate(Level level,int side,boolean open) {
        for(int i=side*2;i<side*2+2;i++)for(int x=0;x<leaves[i].width();x++)for(int y=0;y<leaves[i].height();y++) {
            BlockPos p=leaves[i].at(x,y);var s=level.getBlockState(p);if(!s.is(Content.SEAL.get()))continue;
            var n=s.setValue(GatePanelBlock.OPEN,open).setValue(GatePanelBlock.DEPTH,open?LockLayout.depth(units,p.getY()-base.getY()):0);if(!s.equals(n))level.setBlock(p,n,Block.UPDATE_CLIENTS);
        }
    }
    CompoundTag save() {
        CompoundTag n=new CompoundTag();n.put("Base",NbtUtils.writeBlockPos(base));n.putInt("Forward",forward.get2DDataValue());n.putInt("Width",width);n.putInt("Length",length);n.putInt("Rise",rise);n.putInt("Low",low);n.putInt("High",high);n.putInt("Units",units);
        n.put("Fill",NbtUtils.writeBlockPos(fill));n.put("Drain",NbtUtils.writeBlockPos(drain));n.put("FillPort",NbtUtils.writeBlockPos(fillPort));n.put("DrainPort",NbtUtils.writeBlockPos(drainPort));
        for(int i=0;i<4;i++)n.put("Leaf"+i,leaves[i].save());n.putString("Message",message);return n;
    }
    static CustomLock load(BlockPos owner,CompoundTag n) {
        GateSpec[] leaves=new GateSpec[4];for(int i=0;i<4;i++)leaves[i]=GateSpec.load(n.getCompound("Leaf"+i));
        CustomLock lock=new CustomLock(owner,NbtUtils.readBlockPos(n.getCompound("Base")),Direction.from2DDataValue(n.getInt("Forward")),n.getInt("Width"),n.getInt("Length"),n.getInt("Rise"),n.getInt("Low"),n.getInt("High"),NbtUtils.readBlockPos(n.getCompound("Fill")),NbtUtils.readBlockPos(n.getCompound("Drain")),NbtUtils.readBlockPos(n.getCompound("FillPort")),NbtUtils.readBlockPos(n.getCompound("DrainPort")),leaves,n.getInt("Units"));lock.message=n.getString("Message");return lock;
    }
}
