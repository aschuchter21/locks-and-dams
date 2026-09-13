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
    boolean recessed;
    BlockPos fillChamber,drainChamber;
    int units;
    int minX(){return recessed?1:0;}
    int maxX(){return width-1-minX();}
    int wallLeft(){return minX()-1;}
    int wallRight(){return maxX()+1;}
    int navigationWidth(){return width-2*minX();}
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
            if(w<4||w>18||p.getY()!=q.getY()||along(p,f)!=along(q,f))continue;
            int ox=along(owner,right)-along(p,right),oz=along(owner,f)-along(p,f);
            if(ox!=-1&&ox!=w&&ox!=0&&ox!=w-1)continue;
            if((ox==0||ox==w-1)?w<6:w>16)continue;
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
        boolean recessed=along(owner,right)-along(base,right)==0||along(owner,right)-along(base,right)==w-1;
        int wallLeft=recessed?0:-1,wallRight=recessed?w-1:w;
        BlockPos fill=null,drain=null,fillChamber=null,drainChamber=null;
        WaterConnection upper=null,lower=null;
        for(int x:new int[]{wallLeft,wallRight})for(int z=0;z<=len;z++)for(int y=0;y<=32;y++) {
            BlockPos p=base.relative(right,x).relative(f,z).above(y);require(level.hasChunkAt(p),"Load the chamber walls before assembly.");var s=level.getBlockState(p);
            if(s.is(Content.PORT.get())) {
                require(z>0&&z<len&&s.getValue(CulvertPortBlock.FACING)==(x==wallLeft?right:right.getOpposite()),"Chamber port mouths must face into the chamber between the gates.");
                WaterCircuit circuit=WaterCircuit.discover(level,p);int end=along(circuit.canal().mouth(),f)-along(base,f);
                if(end>len){require(fill==null,"Use one fill circuit per chamber.");fill=circuit.valve();fillChamber=p;upper=circuit.canal();circuit.mark(level,true);}
                else {require(end<0,"Canal port belongs beyond a gate.");require(drain==null,"Use one drain circuit per chamber.");drain=circuit.valve();drainChamber=p;lower=circuit.canal();circuit.mark(level,false);}
            }
            if(s.is(Content.FILL.get())) {require(fill==null,"Use one fill valve per chamber.");fill=p;}
            if(s.is(Content.DRAIN.get())) {require(drain==null,"Use one drain valve per chamber.");drain=p;}
        }
        require(fill!=null&&drain!=null,"Place two chamber ports and connect each to its canal port through an inline valve.");
        if(upper==null)upper=WaterConnection.discover(level,fill);if(lower==null)lower=WaterConnection.discover(level,drain);
        int low=lower.surface()-base.getY()*16,high=upper.surface()-base.getY()*16;
        require(low>0&&high>low&&high<=32*16&&high-low<=16*16,"Ports must set a positive lower depth and a lift up to 16 blocks, within a 32-block chamber depth.");
        require(along(lower.mouth(),f)<along(base,f)&&along(upper.mouth(),f)>along(base,f)+len,"Fill port belongs beyond the upper gate; drain port beyond the lower gate.");
        progress[0]=2;
        require(rise*16<high,"Upper gate starts above the upper water surface.");
        require((long)(recessed?w-2:w)*(len-1)*((high+15)/16)<=16384,"Chamber exceeds 16,384 managed water blocks.");
        for(BlockPos p:new BlockPos[]{owner,fillChamber==null?fill:fillChamber,drainChamber==null?drain:drainChamber})require(p.getY()>=base.getY()&&p.getY()<base.getY()+(high+15)/16,"Place the controller and valves in the side walls below the upper water limit.");
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
        result.recessed=recessed;result.fillChamber=fillChamber;result.drainChamber=drainChamber;
        progress[0]=3;
        result.validate(level,false);return result;
    }
    boolean solid(Level level,BlockPos p) {
        BlockState s=level.getBlockState(p);
        return s.getFluidState().isEmpty()&&Block.isShapeFullBlock(s.getCollisionShape(level,p))&&(!s.hasBlockEntity()||s.getBlock() instanceof com.simibubi.create.content.kinetics.base.KineticBlock);
    }
    void validate(Level level,boolean running) {
        for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(bounds().minX,bounds().minY,bounds().minZ),BlockPos.containing(bounds().maxX-1,bounds().maxY-1,bounds().maxZ-1)))require(level.hasChunkAt(p),"Load the entire chamber.");
        require(level.getBlockState(fill).is(fillChamber==null?Content.FILL.get():Content.INLINE.get())&&level.getBlockState(drain).is(drainChamber==null?Content.DRAIN.get():Content.INLINE.get()),"Restore the linked valves.");
        int top=(high+15)/16;
        for(int x=wallLeft();x<=wallRight();x++)for(int z=0;z<=length;z++) {
            BlockPos floor=at(x,-1,z);boolean lowerHinge=(z==0||rise==0&&z==length)&&(x==0||x==width-1);
            require(lowerHinge?level.getBlockState(floor).is(Content.HINGE.get()):solid(level,floor),"Restore solid chamber floor at "+floor.toShortString());
        }
        for(int x:new int[]{wallLeft(),wallRight()})for(int z=0;z<=length;z++)for(int y=0;y<top;y++) {
            BlockPos p=at(x,y,z);if(recessed&&(z==0||z==length)&&y>=(z==0?0:rise))continue;if(p.equals(owner)||p.equals(fill)||p.equals(drain)||p.equals(fillChamber)||p.equals(drainChamber))continue;
            require(solid(level,p),"Restore solid side wall at "+p.toShortString());
        }
        for(int x=0;x<width;x++)for(int y=0;y<rise;y++) {
            BlockPos p=at(x,y,length);boolean hinge=y==rise-1&&(x==0||x==width-1);
            require(hinge?level.getBlockState(p).is(Content.HINGE.get()):solid(level,p),"Restore the solid wall below the upper gates at "+p.toShortString());
        }
        for(int i=0;i<4;i++) {
            if(recessed){
                GateSpec leaf=leaves[i];Direction approach=i<2?forward.getOpposite():forward;
                for(int step=1;step<=leaf.width();step++){
                    BlockPos floor=leaf.base().relative(approach,step).below();
                    require(level.hasChunkAt(floor)&&solid(level,floor),"Build a solid floor under the gate recess at "+floor.toShortString());
                }
                for(int step=0;step<=leaf.width();step++)for(int y=0;y<leaf.height();y++){
                    BlockPos back=leaf.base().relative(leaf.inward().getOpposite()).relative(approach,step).above(y);
                    require(level.hasChunkAt(back)&&solid(level,back),"Build a solid backing wall behind the gate recess at "+back.toShortString());
                }
            }
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
        for(int x=minX();x<=maxX();x++)for(int z=1;z<length;z++)for(int y=0;y<top;y++) {
            BlockPos p=at(x,y,z);var s=level.getBlockState(p);require(s.isAir()||s.is(Content.WATER_BLOCK.get())||s.is(Blocks.WATER),"Chamber obstructed at "+p.toShortString());
        }
        for(int x=minX();x<=maxX();x++)for(int z=1;z<length;z++)require(!level.getBlockState(at(x,top,z)).is(Content.WATER_BLOCK.get()),"Existing managed water exceeds this port height; restore the original limits before reclaiming.");
        WaterConnection a=connection(level,true),b=connection(level,false);
        require(a.port().equals(fillPort)&&b.port().equals(drainPort)&&a.surface()==base.getY()*16+high&&b.surface()==base.getY()*16+low,"Canal port or water level changed; restore it before operating.");
        for(int side=0;side<2;side++)for(int x=minX();x<=maxX();x++) {
            int surface=side==0?low:high;BlockPos p=at(x,surface/16,side==0?-1:length+1);
            require(level.hasChunkAt(p),"Load both canal approaches.");var fluid=level.getFluidState(p);
            require(fluid.is(net.minecraft.tags.FluidTags.WATER)&&fluid.isSource()&&level.getFluidState(p.above()).isEmpty()&&p.getY()*16+Math.round(fluid.getHeight(level,p)*16)==base.getY()*16+surface,"Canal water across each gate must match its linked port's surface.");
        }
    }
    WaterConnection connection(Level level,boolean filling) {
        BlockPos chamber=filling?fillChamber:drainChamber,valve=filling?fill:drain;
        if(chamber==null)return WaterConnection.discover(level,valve);
        Direction face=level.getBlockState(chamber).is(Content.PORT.get())?level.getBlockState(chamber).getValue(CulvertPortBlock.FACING):Direction.UP;
        BlockPos mouth=chamber.relative(face);
        require(bounds().contains(mouth.getX()+.5,mouth.getY()+.5,mouth.getZ()+.5)&&along(mouth,forward)>along(base,forward)&&along(mouth,forward)<along(base,forward)+length
            &&along(mouth,forward.getClockWise())>=along(at(minX(),0,0),forward.getClockWise())&&along(mouth,forward.getClockWise())<=along(at(maxX(),0,0),forward.getClockWise()),"Chamber port must face into the chamber.");
        require(chamber.getY()*16<base.getY()*16+(filling?high:low),"Keep the chamber intake below the lower water surface and the outlet below the upper surface.");
        var circuit=WaterCircuit.discover(level,chamber);require(circuit.valve().equals(valve),"Inline valve moved; reassemble the controller to relink it.");circuit.mark(level,filling);return circuit.canal();
    }
    void assemble(Level level) {
        validate(level,false);
        require(level.getEntities((Entity)null,bounds(),e->e.isAlive()&&!e.isSpectator()&&!(e instanceof com.simibubi.create.content.contraptions.AbstractContraptionEntity)).isEmpty(),"Clear the chamber and gate openings before assembly.");
        for(int cx=(owner.getX()-80)>>4;cx<=(owner.getX()+80)>>4;cx++)for(int cz=(owner.getZ()-80)>>4;cz<=(owner.getZ()+80)>>4;cz++)if(level.hasChunk(cx,cz))
            for(var be:level.getChunk(cx,cz).getBlockEntities().values())if(be instanceof LockEntity other&&!other.getBlockPos().equals(owner)&&other.assembled())require(!bounds().intersects(other.ownedBounds()),"Chamber overlaps another assembled lock.");
        // Uniform managed water can be reclaimed after a controller replacement.
        int found=-1;for(int y=0;y<(high+15)/16;y++) {var s=level.getBlockState(at(minX(),y,1));if(s.is(Content.WATER_BLOCK.get()))found=y*16+s.getValue(ChamberFluid.HEIGHT);}
        boolean managed=false;for(int x=minX();x<=maxX();x++)for(int z=1;z<length;z++)for(int y=0;y<(high+15)/16;y++)if(level.getBlockState(at(x,y,z)).is(Content.WATER_BLOCK.get()))managed=true;
        if(managed) {
            require(found>=low&&found<=high,"Managed water is inconsistent.");
            for(int x=minX();x<=maxX();x++)for(int z=1;z<length;z++)for(int y=0;y<(high+15)/16;y++) {
                var s=level.getBlockState(at(x,y,z));int d=LockLayout.depth(found,y);
                require(d==0?s.isAir():s.is(Content.WATER_BLOCK.get())&&s.getValue(ChamberFluid.HEIGHT)==d,"Managed water is inconsistent; restore it before reclaiming.");
            }units=found;
        }
        for(int i=0;i<4;i++) {var h=hinge(level,i);h.bind(owner,leaves[i]);h.assemble();require(h.isRunning(),"Gate assembly failed.");}
        writeWater(level);for(int side=0;side<2;side++)writeGate(level,side,hinge(level,side*2).open()&&hinge(level,side*2+1).open());
        message="Assembled "+navigationWidth()+" x "+(length-1)+" chamber; lift "+(high-low)/16.0+" blocks.";
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
            allClosed&=a.closed()&&b.closed();
            for(int i=side*2;i<side*2+2;i++) {
                BlockPos drive=leaves[i].base().below().relative(leaves[i].inward().getOpposite());
                if(level.getBlockState(drive).is(Content.DRIVE.get()))control(level,drive,a.open()&&b.open());
            }
        }
        boolean f=!controller.deskStop()&&(controller.deskPosition()==null?level.hasNeighborSignal(fill):controller.waterCommand()==DeskControl.FILL);
        boolean d=!controller.deskStop()&&(controller.deskPosition()==null?level.hasNeighborSignal(drain):controller.waterCommand()==DeskControl.DRAIN);
        boolean filling=allClosed&&f&&!d&&units<high,draining=allClosed&&d&&!f&&units>low;
        if(filling)units++;if(draining)units--;
        var desk=controller.deskControl();
        if(controller.deskPosition()!=null&&desk.stopped)message="Emergency stop latched; shift-right-click the red button to reset.";
        else if(controller.deskPosition()!=null&&desk.warningTicks>0)message=(desk.waterSelection==DeskControl.FILL?"Fill":"Drain")+" warning: "+((desk.warningTicks+19)/20)+" seconds remaining; switching restarts the warning.";
        else if(!allClosed)message="Gate open or moving; waiting for both pairs to close.";
        else if(f&&d)message="Paused: both valves powered.";
        else if(filling)message="Filling";
        else if(draining)message="Draining";
        else if(f&&units>=high)message="At upper water level; select DRAIN to lower the chamber.";
        else if(d&&units<=low)message="At lower water level; select FILL to raise the chamber.";
        else message=controller.deskPosition()!=null?"Idle; operate the FILL / DRAIN selector to start a new command.":"Ready";
        control(level,fill,filling);control(level,drain,draining);writeWater(level);
        for(int side=0;side<2;side++)writeGate(level,side,hinge(level,side*2).open()&&hinge(level,side*2+1).open());
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
        for(int x=minX();x<=maxX();x++)for(int z=1;z<length;z++)for(int y=0;y<(high+15)/16;y++) {
            int d=LockLayout.depth(units,y);var s=d==0?Blocks.AIR.defaultBlockState():Content.WATER_BLOCK.get().defaultBlockState().setValue(ChamberFluid.HEIGHT,d);BlockPos p=at(x,y,z);if(!level.getBlockState(p).equals(s))level.setBlock(p,s,Block.UPDATE_CLIENTS);
        }
    }
    void writeGate(Level level,int side,boolean open) {
        for(int i=side*2;i<side*2+2;i++)for(int x=0;x<leaves[i].width();x++)for(int y=0;y<leaves[i].height();y++) {
            BlockPos p=leaves[i].at(x,y);var s=level.getBlockState(p);if(!s.is(Content.SEAL.get()))continue;
            var n=s.setValue(GatePanelBlock.OPEN,open).setValue(GatePanelBlock.DEPTH,open?LockLayout.depth(units,p.getY()-base.getY()):0);if(!s.equals(n))level.setBlock(p,n,Block.UPDATE_CLIENTS);
            GateWaterEntity.update(level,p,LockLayout.depth(units,p.getY()-base.getY()),LockLayout.depth(side==0?low:high,p.getY()-base.getY()),side==0?forward:forward.getOpposite());
        }
    }
    CompoundTag save() {
        CompoundTag n=new CompoundTag();n.put("Base",NbtUtils.writeBlockPos(base));n.putInt("Forward",forward.get2DDataValue());n.putInt("Width",width);n.putInt("Length",length);n.putInt("Rise",rise);n.putInt("Low",low);n.putInt("High",high);n.putInt("Units",units);n.putBoolean("Recessed",recessed);
        n.put("Fill",NbtUtils.writeBlockPos(fill));n.put("Drain",NbtUtils.writeBlockPos(drain));n.put("FillPort",NbtUtils.writeBlockPos(fillPort));n.put("DrainPort",NbtUtils.writeBlockPos(drainPort));
        if(fillChamber!=null)n.put("FillChamber",NbtUtils.writeBlockPos(fillChamber));if(drainChamber!=null)n.put("DrainChamber",NbtUtils.writeBlockPos(drainChamber));
        for(int i=0;i<4;i++)n.put("Leaf"+i,leaves[i].save());n.putString("Message",message);return n;
    }
    static CustomLock load(BlockPos owner,CompoundTag n) {
        GateSpec[] leaves=new GateSpec[4];for(int i=0;i<4;i++)leaves[i]=GateSpec.load(n.getCompound("Leaf"+i));
        CustomLock lock=new CustomLock(owner,NbtUtils.readBlockPos(n.getCompound("Base")),Direction.from2DDataValue(n.getInt("Forward")),n.getInt("Width"),n.getInt("Length"),n.getInt("Rise"),n.getInt("Low"),n.getInt("High"),NbtUtils.readBlockPos(n.getCompound("Fill")),NbtUtils.readBlockPos(n.getCompound("Drain")),NbtUtils.readBlockPos(n.getCompound("FillPort")),NbtUtils.readBlockPos(n.getCompound("DrainPort")),leaves,n.getInt("Units"));lock.recessed=n.getBoolean("Recessed");lock.message=n.getString("Message");lock.fillChamber=n.contains("FillChamber")?NbtUtils.readBlockPos(n.getCompound("FillChamber")):null;lock.drainChamber=n.contains("DrainChamber")?NbtUtils.readBlockPos(n.getCompound("DrainChamber")):null;return lock;
    }
}
