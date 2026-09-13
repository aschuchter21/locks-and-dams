package com.aschuchter21.locksanddams;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public final class GateWaterRenderer implements BlockEntityRenderer<GateWaterEntity>{
    public GateWaterRenderer(BlockEntityRendererProvider.Context c){}
    @Override public void render(GateWaterEntity w,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(w.getLevel()==null||w.getBlockState().getValue(GatePanelBlock.OPEN))return;
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("minecraft","block/water_still"));
        int tint=BiomeColors.getAverageWaterColor(w.getLevel(),w.getBlockPos());
        VertexConsumer v=buffers.getBuffer(RenderType.translucent());
        for(int side=0;side<2;side++){
            int depth=side==0?w.inside:w.outside;if(depth==0)continue;Direction d=side==0?w.inward:w.inward.getOpposite();
            float x0=0,x1=1,z0=0,z1=1,y=depth/16f;
            boolean reverse=w.getBlockState().getValue(GatePanelBlock.REVERSED);
            float zLow=(reverse?8.76f:6.26f)/16,zHigh=(reverse?9.74f:7.24f)/16,xLow=(reverse?6.26f:8.76f)/16,xHigh=(reverse?7.24f:9.74f)/16;
            if(d==Direction.WEST)x1=xLow;else if(d==Direction.EAST)x0=xHigh;else if(d==Direction.NORTH)z1=zLow;else z0=zHigh;
            boolean above=w.getLevel().getBlockEntity(w.getBlockPos().above()) instanceof GateWaterEntity a&&(side==0?a.inside:a.outside)>0;
            if(!above)quad(v,pose,light,tint,sprite,new float[][]{{x0,y,z0},{x0,y,z1},{x1,y,z1},{x1,y,z0}},0,1,0);
            // End faces are only exposed at the end of a row, avoiding internal translucent seams.
            if(d!=Direction.EAST&&visible(w,Direction.WEST,depth))quad(v,pose,light,tint,sprite,new float[][]{{x0,0,z0},{x0,0,z1},{x0,y,z1},{x0,y,z0}},-1,0,0);
            if(d!=Direction.WEST&&visible(w,Direction.EAST,depth))quad(v,pose,light,tint,sprite,new float[][]{{x1,0,z1},{x1,0,z0},{x1,y,z0},{x1,y,z1}},1,0,0);
            if(d!=Direction.SOUTH&&visible(w,Direction.NORTH,depth))quad(v,pose,light,tint,sprite,new float[][]{{x1,0,z0},{x0,0,z0},{x0,y,z0},{x1,y,z0}},0,0,-1);
            if(d!=Direction.NORTH&&visible(w,Direction.SOUTH,depth))quad(v,pose,light,tint,sprite,new float[][]{{x0,0,z1},{x1,0,z1},{x1,y,z1},{x0,y,z1}},0,0,1);
        }
    }
    private static boolean visible(GateWaterEntity w,Direction d,int depth){
        var p=w.getBlockPos().relative(d);var s=w.getLevel().getBlockState(p);
        if(s.is(Content.SEAL.get())||net.minecraft.world.level.block.Block.isShapeFullBlock(s.getCollisionShape(w.getLevel(),p)))return false;
        var fluid=s.getFluidState();return fluid.isEmpty()||fluid.getHeight(w.getLevel(),p)<depth/16f-.02f;
    }
    private static void quad(VertexConsumer v,PoseStack p,int light,int color,net.minecraft.client.renderer.texture.TextureAtlasSprite t,float[][] points,float nx,float ny,float nz){
        float[] us={t.getU0(),t.getU0(),t.getU1(),t.getU1()},vs={t.getV1(),t.getV0(),t.getV0(),t.getV1()};
        for(int i=0;i<4;i++)v.vertex(p.last().pose(),points[i][0],points[i][1],points[i][2]).color((color>>16)&255,(color>>8)&255,color&255,190).uv(us[i],vs[i]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),nx,ny,nz).endVertex();
    }
}
