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

// Use the fixed translucent buffer: a temporary chunk-layer buffer flushes between
// block entities and can depth-occlude gate leaves rendered later in the frame.
public final class GateWaterRenderer implements BlockEntityRenderer<GateWaterEntity>{
    public GateWaterRenderer(BlockEntityRendererProvider.Context c){}
    @Override public void render(GateWaterEntity w,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(w.getLevel()==null)return;
        if(w.miter){renderMiter(w,pose,buffers,light);return;}
        if(w.getBlockState().getValue(GatePanelBlock.OPEN))return;
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("minecraft","block/water_still"));
        int tint=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(Content.WATER.get()).getTintColor(Content.WATER.get().defaultFluidState(),w.getLevel(),w.getBlockPos());
        light=waterLight(w);
        VertexConsumer v=buffers.getBuffer(RenderType.translucentNoCrumbling());
        for(int side=0;side<2;side++){
            int depth=side==0?w.inside:w.outside;if(depth==0)continue;Direction d=side==0?w.inward:w.inward.getOpposite();
            float x0=0,x1=1,z0=0,z1=1,y=depth/16f;
            boolean reverse=w.getBlockState().getValue(GatePanelBlock.REVERSED);
            float zLow=(reverse?8.76f:6.26f)/16,zHigh=(reverse?9.74f:7.24f)/16,xLow=(reverse?6.26f:8.76f)/16,xHigh=(reverse?7.24f:9.74f)/16;
            if(d==Direction.WEST)x1=xLow;else if(d==Direction.EAST)x0=xHigh;else if(d==Direction.NORTH)z1=zLow;else z0=zHigh;
            boolean above=w.getLevel().getBlockEntity(w.getBlockPos().above()) instanceof GateWaterEntity a&&(side==0?a.inside:a.outside)>0;
            if(!above)quad(w,v,pose,light,tint,sprite,new float[][]{{x0,y,z0},{x0,y,z1},{x1,y,z1},{x1,y,z0}},0,1,0);
            // End faces are only exposed at the end of a row, avoiding internal translucent seams.
            if(d!=Direction.EAST&&visible(w,Direction.WEST,depth))quad(w,v,pose,light,tint,sprite,new float[][]{{x0,0,z0},{x0,0,z1},{x0,y,z1},{x0,y,z0}},-1,0,0);
            if(d!=Direction.WEST&&visible(w,Direction.EAST,depth))quad(w,v,pose,light,tint,sprite,new float[][]{{x1,0,z1},{x1,0,z0},{x1,y,z0},{x1,y,z1}},1,0,0);
            if(d!=Direction.SOUTH&&visible(w,Direction.NORTH,depth))quad(w,v,pose,light,tint,sprite,new float[][]{{x1,0,z0},{x0,0,z0},{x0,y,z0},{x1,y,z0}},0,0,-1);
            if(d!=Direction.NORTH&&visible(w,Direction.SOUTH,depth))quad(w,v,pose,light,tint,sprite,new float[][]{{x0,0,z1},{x1,0,z1},{x1,y,z1},{x0,y,z1}},0,0,1);
        }
    }
    private void renderMiter(GateWaterEntity w,PoseStack pose,MultiBufferSource buffers,int light){
        if(w.highDepth==0&&w.lowDepth==0)return;
        if(w.highDepth==16&&(w.unsealed||w.lowDepth==16)&&w.getLevel().getBlockEntity(w.getBlockPos().above()) instanceof GateWaterEntity a&&a.miter&&a.highDepth>0&&(w.unsealed||a.lowDepth>0))return;
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("minecraft","block/water_still"));
        int tint=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(Content.WATER.get()).getTintColor(Content.WATER.get().defaultFluidState(),w.getLevel(),w.getBlockPos());
        light=waterLight(w);var out=buffers.getBuffer(RenderType.translucentNoCrumbling());
        for(int side=0;side<(w.unsealed?1:2);side++){
            int depth=w.unsealed?w.highDepth:side==0?w.highDepth:w.lowDepth;if(depth==0)continue;
            java.util.List<double[]> polygon=new java.util.ArrayList<>(java.util.List.of(new double[]{0,0},new double[]{0,1},new double[]{1,1},new double[]{1,0}));
            if(!w.unsealed){
                double sign=side==0?1:-1,offset=.149*Math.hypot(w.planeX,w.planeZ);
                var clipped=new java.util.ArrayList<double[]>();
                for(int i=0;i<polygon.size();i++){
                    double[] a=polygon.get(i),b=polygon.get((i+1)%polygon.size());double da=sign*(w.planeX*a[0]+w.planeZ*a[1]+w.planeC)-offset,db=sign*(w.planeX*b[0]+w.planeZ*b[1]+w.planeC)-offset;
                    if(da>=0)clipped.add(a);
                    if((da>=0)!=(db>=0)){double t=da/(da-db);clipped.add(new double[]{a[0]+t*(b[0]-a[0]),a[1]+t*(b[1]-a[1])});}
                }polygon=clipped;
            }
            if(polygon.size()<3)continue;float y=depth/16f-.001f;
            boolean covered=w.getLevel().getBlockEntity(w.getBlockPos().above()) instanceof GateWaterEntity above&&above.miter&&(side==0?above.highDepth:above.lowDepth)>0;
            // Vanilla canal source water has an 8/9 surface height when uncovered.
            boolean canalSide=!w.unsealed&&(side==0?!w.chamberCell:w.chamberCell);
            if(canalSide&&!covered&&depth==16)y=8f/9f-.001f;
            if(!covered)for(int i=1;i<polygon.size()-1;i++){
                double[] a=polygon.get(0),b=polygon.get(i),c=polygon.get(i+1);
                quad(w,out,pose,light,tint,sprite,new float[][]{{(float)a[0],y,(float)a[1]},{(float)b[0],y,(float)b[1]},{(float)c[0],y,(float)c[1]},{(float)c[0],y,(float)c[1]}},0,1,0);
            }
            for(int i=0;i<polygon.size();i++){
                double[] a=polygon.get(i),b=polygon.get((i+1)%polygon.size());Direction d=null;
                if(a[0]==0&&b[0]==0)d=Direction.WEST;else if(a[0]==1&&b[0]==1)d=Direction.EAST;else if(a[1]==0&&b[1]==0)d=Direction.NORTH;else if(a[1]==1&&b[1]==1)d=Direction.SOUTH;
                if(d==null)continue;var next=w.getBlockPos().relative(d);
                if(w.getLevel().getBlockEntity(next) instanceof GateWaterEntity other&&other.miter)continue;
                if(!visible(w,d,depth))continue;
                var neighborFluid=w.getLevel().getFluidState(next);
                if(!neighborFluid.isEmpty()&&neighborFluid.getHeight(w.getLevel(),next)>=y-.02f)continue;
                quad(w,out,pose,light,tint,sprite,new float[][]{{(float)a[0],0,(float)a[1]},{(float)b[0],0,(float)b[1]},{(float)b[0],y,(float)b[1]},{(float)a[0],y,(float)a[1]}},d.getStepX(),0,d.getStepZ());
            }
        }
    }
    private static boolean visible(GateWaterEntity w,Direction d,int depth){
        var p=w.getBlockPos().relative(d);var s=w.getLevel().getBlockState(p);
        if(s.is(Content.SEAL.get())||net.minecraft.world.level.block.Block.isShapeFullBlock(s.getCollisionShape(w.getLevel(),p)))return false;
        var fluid=s.getFluidState();return fluid.isEmpty()||fluid.getHeight(w.getLevel(),p)<depth/16f-.02f;
    }
    private static int waterLight(GateWaterEntity w){
        int a=LevelRenderer.getLightColor(w.getLevel(),w.getBlockPos());
        int b=LevelRenderer.getLightColor(w.getLevel(),w.getBlockPos().above());
        return LightTexture.pack(Math.max(LightTexture.block(a),LightTexture.block(b)),Math.max(LightTexture.sky(a),LightTexture.sky(b)));
    }
    private static void quad(GateWaterEntity w,VertexConsumer v,PoseStack p,int light,int color,net.minecraft.client.renderer.texture.TextureAtlasSprite t,float[][] points,float nx,float ny,float nz){
        float[] us={t.getU0(),t.getU0(),t.getU1(),t.getU1()},vs={t.getV1(),t.getV0(),t.getV0(),t.getV1()};
        float shade=w.getLevel().getShade(Direction.UP,true);
        if(ny==0)shade*=w.getLevel().getShade(nx==0?Direction.NORTH:Direction.WEST,true);
        float shrink=t.uvShrinkRatio();
        for(int i=0;i<4;i++){
            float u=us[i],vv=vs[i];
            if(ny==1){
                // Match the affine UV shrink of a complete vanilla water tile, then clip.
                u=t.getU((points[i][0]+(.5f-points[i][0])*shrink)*16);
                vv=t.getV((points[i][2]+(.5f-points[i][2])*shrink)*16);
            }
            v.vertex(p.last().pose(),points[i][0],points[i][1],points[i][2])
                .color(((color>>16)&255)/255f*shade,((color>>8)&255)/255f*shade,(color&255)/255f*shade,((color>>>24)&255)/255f)
                .uv(u,vv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),0,1,0).endVertex();
        }
    }
}
