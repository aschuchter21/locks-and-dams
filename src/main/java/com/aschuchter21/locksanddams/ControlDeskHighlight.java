package com.aschuchter21.locksanddams;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Draw the cabinet silhouette instead of exposing every collision stair and switch box. */
@Mod.EventBusSubscriber(modid="locksanddams", value=Dist.CLIENT)
public final class ControlDeskHighlight {
    @SubscribeEvent
    public static void highlight(RenderHighlightEvent.Block event) {
        var level=Minecraft.getInstance().level;
        var pos=event.getTarget().getBlockPos();
        if(level==null)return;
        var state=level.getBlockState(pos);
        if(!state.is(Content.DESK.get()))return;
        event.setCanceled(true);
        var root=ControlDeskBlock.root(state,pos);
        var camera=event.getCamera().getPosition();
        var pose=event.getPoseStack();
        pose.pushPose();
        pose.translate(root.getX()+.5-camera.x,root.getY()-camera.y,root.getZ()+.5-camera.z);
        var facing=state.getValue(ControlDeskBlock.FACING);
        float[][] points=new float[8][3];
        for(int i=0;i<8;i++) {
            float x=(i<4?-1.5f:1.5f),z=(i%4==0||i%4==3?-.49f:.49f);
            float y=i%4<2?.07f:.545f+(z+.5f)*.383f;
            points[i]=switch(facing) {
                case EAST -> new float[]{-z,y,x};
                case SOUTH -> new float[]{-x,y,-z};
                case WEST -> new float[]{z,y,-x};
                default -> new float[]{x,y,z};
            };
        }
        VertexConsumer out=event.getMultiBufferSource().getBuffer(RenderType.lines());
        for(int[] edge:new int[][]{{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}}) {
            float[] a=points[edge[0]],b=points[edge[1]];
            float dx=b[0]-a[0],dy=b[1]-a[1],dz=b[2]-a[2];
            float length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
            for(float[] v:new float[][]{a,b})out.vertex(pose.last().pose(),v[0],v[1],v[2]).color(0,0,0,.4f)
                .normal(pose.last().normal(),dx/length,dy/length,dz/length).endVertex();
        }
        pose.popPose();
    }
}
