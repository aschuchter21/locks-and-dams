package com.aschuchter21.locksanddams;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
/** Invisible physical tiles inside Create; the hinge draws one complete leaf. */
public final class AssembledGateBlock extends Block {
    public static final IntegerProperty WIDTH=IntegerProperty.create("width",1,17),COLUMN=IntegerProperty.create("column",0,16);
    public static final DirectionProperty INWARD=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty TOP=BooleanProperty.create("top");
    public AssembledGateBlock(){super(Content.metal().noOcclusion());registerDefaultState(stateDefinition.any().setValue(WIDTH,1).setValue(COLUMN,0).setValue(INWARD,Direction.EAST).setValue(TOP,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(WIDTH,COLUMN,INWARD,TOP);}
    public static BlockState tile(GateSpec g,int column,int y){return Content.ASSEMBLED_GATE.get().defaultBlockState().setValue(WIDTH,g.width()).setValue(COLUMN,column).setValue(INWARD,g.inward()).setValue(TOP,y==g.height()-1);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        int col=s.getValue(COLUMN);double span=s.getValue(WIDTH)-.5,scale=Math.hypot(span,1)/span;
        double a=(col==0?0:col-.5)*scale-col,b=(col+.5)*scale-col;
        VoxelShape shape=part(s,a,b,0,1,-.22,.22);
        double deckStart=(.5*Math.hypot(span,1)+.48)/span+.02;double deckA=Math.max(a,deckStart-col);
        if(s.getValue(TOP)&&b>deckA)shape=Shapes.or(shape,part(s,deckA,b,.94,1,-.48,.48),part(s,deckA,b,1,1.95,-.48,-.42),part(s,deckA,b,1,1.95,.42,.48));
        return shape;
    }
    private static VoxelShape part(BlockState s,double a,double b,double y,double h,double n0,double n1){
        return switch(s.getValue(INWARD)){
            case WEST->Shapes.box(.5-b,y,.5-n1,.5-a,h,.5-n0);
            case SOUTH->Shapes.box(.5-n1,y,.5+a,.5-n0,h,.5+b);
            case NORTH->Shapes.box(.5+n0,y,.5-b,.5+n1,h,.5-a);
            default->Shapes.box(.5+a,y,.5+n0,.5+b,h,.5+n1);
        };
    }
}
