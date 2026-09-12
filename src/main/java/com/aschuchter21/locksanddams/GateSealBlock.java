package com.aschuchter21.locksanddams;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
/** Reserved gate opening: the visible panels belong to a Create contraption. */
public final class GateSealBlock extends GatePanelBlock implements net.minecraft.world.level.block.EntityBlock {
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos p,BlockState s){return new GateWaterEntity(p,s);}
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockGetter l,BlockPos p,BlockState s){return new net.minecraft.world.item.ItemStack(Content.PANEL.get());}
    @Override public void playerWillDestroy(net.minecraft.world.level.Level l,BlockPos p,BlockState s,net.minecraft.world.entity.player.Player player){
        if(!l.isClientSide)for(var h:CustomLock.nearby(l,p)){
            var leaf=h.geometry();if(leaf==null)continue;
            boolean contains=false;for(int x=0;x<leaf.width();x++)for(int y=0;y<leaf.height();y++)contains|=leaf.at(x,y).equals(p);
            if(contains){h.preparePanelEdit();break;}
        }
        super.playerWillDestroy(l,p,s,player);
    }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return s.getValue(OPEN)?Shapes.empty():Shapes.block(); }
}
