package com.aschuchter21.locksanddams;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.bearing.BearingRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** One cached continuous timber/steel mesh per leaf size, independent of building tiles. */
public final class TimberGateRenderer extends BearingRenderer<LockHingeEntity> {
    private static final ResourceLocation TEXTURE=new ResourceLocation("locksanddams","textures/entity/timber_gate.png");
    private static final Map<String,List<float[]>> CACHE=new LinkedHashMap<>(){
        @Override protected boolean removeEldestEntry(Map.Entry<String,List<float[]>> e){return size()>64;}
    };
    private record Lighting(List<float[]> mesh,long tick,int[] values) {}
    private static final Map<LockHingeEntity,Lighting> LIGHTS=new WeakHashMap<>();
    public TimberGateRenderer(BlockEntityRendererProvider.Context c){super(c);}
    @Override protected void renderSafe(LockHingeEntity hinge,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        super.renderSafe(hinge,partial,pose,buffers,light,overlay);
        GateSpec g=hinge.geometry();if(g==null||!g.mitered()||!hinge.isRunning())return;
        List<float[]> mesh=CACHE.computeIfAbsent(g.width()+":"+g.height(),key->build(g.length(),g.height()));
        pose.pushPose();pose.translate(.5,1,.5);
        float direction=switch(g.inward()){case SOUTH->-90;case NORTH->90;case WEST->180;default->0;};
        pose.mulPose(Axis.YP.rotationDegrees(direction+hinge.getInterpolatedAngle(partial)));
        VertexConsumer out=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        long tick=hinge.getLevel().getGameTime();
        Lighting cached=LIGHTS.get(hinge);
        if(cached==null||cached.mesh()!=mesh||tick<cached.tick()||tick-cached.tick()>=5){
            int[] values=new int[mesh.size()];int index=0;
        var worldLights=new HashMap<net.minecraft.core.BlockPos,Integer>();
        double radians=Math.toRadians(direction+hinge.getInterpolatedAngle(partial));
        double cos=Math.cos(radians),sin=Math.sin(radians);
        for(float[] v:mesh){
            var sample=net.minecraft.core.BlockPos.containing(hinge.getBlockPos().getX()+.5+v[0]*cos+v[2]*sin,
                hinge.getBlockPos().getY()+1+v[1],hinge.getBlockPos().getZ()+.5-v[0]*sin+v[2]*cos);
            values[index++]=worldLights.computeIfAbsent(sample,p->{
                int block=0,sky=0;
                for(Direction d:Direction.values()){
                    var q=p.relative(d);
                    int packed=LevelRenderer.getLightColor(hinge.getLevel(),q);
                    block=Math.max(block,LightTexture.block(packed));sky=Math.max(sky,LightTexture.sky(packed));
                }
                return LightTexture.pack(block,sky);
            });
        }
            cached=new Lighting(mesh,tick,values);LIGHTS.put(hinge,cached);
        }
        int index=0;
        for(float[] v:mesh){
            out.vertex(pose.last().pose(),v[0],v[1],v[2]).color(v[8],v[9],v[10],1).uv(v[6],v[7]).overlayCoords(overlay).uv2(cached.values()[index++]).normal(pose.last().normal(),v[3],v[4],v[5]).endVertex();
        }
        pose.popPose();
    }
    private static final float[] STEEL={.19f,.34f,.36f},EDGE={.37f,.48f,.49f},BRASS={.86f,.63f,.24f},DARK={.07f,.10f,.10f},WOOD={1,1,1};
    private static List<float[]> build(double width,double height){
        Builder b=new Builder();
        b.box(width/2,height/2,0,width,.0+height,.22,DARK,false);
        int planks=Math.max(4,(int)Math.ceil(width/.205));
        for(int i=0;i<planks;i++){float shade=.88f+(i%7)*.02f;b.box((i+.5)*width/planks,height/2,0,width/planks-.008,height,.30,new float[]{shade,shade,shade},true);}
        for(double x:new double[]{.10,width-.10})b.box(x,height/2,0,.23,height+.03,.43,STEEL,false);
        for(int row=0;row<5;row++){
            double y=.12+(height-.24)*row/4;
            for(double z:new double[]{-.21,.21})b.box(width/2,y,z,width,.18,.12,STEEL,false);
            for(double x:new double[]{.10,Math.min(.44,width/3),Math.max(width-.44,width*2/3),width-.10})for(double z:new double[]{-.285,.285}){
                b.box(x,y,z,.10,.10,.025,BRASS,false);b.cylinder(x,y,z,x,y,z+Math.copySign(.025,z),.031,EDGE,6);
            }
        }
        // Structural braces are whole-leaf members, not repeating block decorations.
        for(double z:new double[]{-.28,.28})b.beam(.23,height-.32,z,width-.23,.32,z,.17,.10,STEEL);
        double start=(.5*width+.48)/Math.sqrt(width*width-1)+.02,walk=width-start;
        for(double z:new double[]{-.46,.46})b.box(start+walk/2,height-.045,z,walk,.09,.06,STEEL,false);
        int grates=Math.max(8,(int)Math.ceil(walk/.09));
        for(int i=0;i<=grates;i++)b.box(start+i*walk/grates,height-.025,0,.026,.045,.90,EDGE,false);
        for(double z:new double[]{-.32,-.16,0,.16,.32})b.box(start+walk/2,height-.04,z,walk,.06,.018,EDGE,false);
        int posts=Math.max(2,(int)Math.ceil(walk));
        for(int i=0;i<=posts;i++){
            double x=start+.05+(walk-.10)*i/posts;
            for(double z:new double[]{-.45,.45}){
                b.cylinder(x,height,z,x,height+.92,z,.027,BRASS,8);
                b.box(x,height,z,.09,.09,.09,STEEL,false);
            }
        }
        for(double z:new double[]{-.45,.45})for(double y:new double[]{height+.46,height+.92})b.cylinder(start+.05,y,z,width-.05,y,z,.027,BRASS,8);
        return List.copyOf(b.vertices);
    }
    private static final class Builder {
        final List<float[]> vertices=new ArrayList<>();
        void quad(double[][] p,float[] c,boolean wood){
            double ax=p[1][0]-p[0][0],ay=p[1][1]-p[0][1],az=p[1][2]-p[0][2],bx=p[2][0]-p[0][0],by=p[2][1]-p[0][1],bz=p[2][2]-p[0][2];
            double nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,len=Math.sqrt(nx*nx+ny*ny+nz*nz);if(len<1e-9)return;
            double minU=Double.POSITIVE_INFINITY,maxU=Double.NEGATIVE_INFINITY,minY=Double.POSITIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY;
            for(double[] v:p){double u=Math.abs(nz)>Math.abs(nx)?v[0]:v[2];minU=Math.min(minU,u);maxU=Math.max(maxU,u);minY=Math.min(minY,v[1]);maxY=Math.max(maxY,v[1]);}
            for(int i=0;i<4;i++)vertices.add(new float[]{(float)p[i][0],(float)p[i][1],(float)p[i][2],(float)(nx/len),(float)(ny/len),(float)(nz/len),wood?(float)(.005+.485*((Math.abs(nz)>Math.abs(nx)?p[i][0]:p[i][2])-minU)/Math.max(.001,maxU-minU)):.75f,wood?(float)(.995-.99*(p[i][1]-minY)/Math.max(.001,maxY-minY)):.5f,c[0],c[1],c[2]});
        }
        void box(double x,double y,double z,double w,double h,double d,float[] c,boolean wood){
            double[][] v=new double[8][3];for(int i=0;i<8;i++)v[i]=new double[]{x+((i&1)==0?-w:w)/2,y+((i&2)==0?-h:h)/2,z+((i&4)==0?-d:d)/2};
            for(int[] f:new int[][]{{0,2,3,1},{4,5,7,6},{0,4,6,2},{1,3,7,5},{0,1,5,4},{2,6,7,3}})quad(new double[][]{v[f[0]],v[f[1]],v[f[2]],v[f[3]]},c,wood);
        }
        void beam(double x0,double y0,double z0,double x1,double y1,double z1,double w,double d,float[] c){
            double dx=x1-x0,dy=y1-y0,len=Math.hypot(dx,dy),nx=-dy/len*w/2,ny=dx/len*w/2;
            double[][] v={{x0-nx,y0-ny,z0-d/2},{x0+nx,y0+ny,z0-d/2},{x1+nx,y1+ny,z1-d/2},{x1-nx,y1-ny,z1-d/2},{x0-nx,y0-ny,z0+d/2},{x0+nx,y0+ny,z0+d/2},{x1+nx,y1+ny,z1+d/2},{x1-nx,y1-ny,z1+d/2}};
            for(int[] f:new int[][]{{0,3,2,1},{4,5,6,7},{0,1,5,4},{1,2,6,5},{2,3,7,6},{3,0,4,7}})quad(new double[][]{v[f[0]],v[f[1]],v[f[2]],v[f[3]]},c,false);
        }
        void cylinder(double x0,double y0,double z0,double x1,double y1,double z1,double r,float[] c,int sides){
            var axis=new org.joml.Vector3d(x1-x0,y1-y0,z1-z0).normalize();var u=new org.joml.Vector3d(Math.abs(axis.y)<.9?0:1,Math.abs(axis.y)<.9?1:0,0).cross(axis).normalize();var v=new org.joml.Vector3d(axis).cross(u);
            double[][] a=new double[sides][3],b=new double[sides][3];
            for(int i=0;i<sides;i++){double t=i*2*Math.PI/sides,dx=r*(u.x*Math.cos(t)+v.x*Math.sin(t)),dy=r*(u.y*Math.cos(t)+v.y*Math.sin(t)),dz=r*(u.z*Math.cos(t)+v.z*Math.sin(t));a[i]=new double[]{x0+dx,y0+dy,z0+dz};b[i]=new double[]{x1+dx,y1+dy,z1+dz};}
            for(int i=0;i<sides;i++){int j=(i+1)%sides;quad(new double[][]{a[i],a[j],b[j],b[i]},c,false);quad(new double[][]{{x0,y0,z0},a[j],a[i],a[i]},c,false);quad(new double[][]{{x1,y1,z1},b[i],b[j],b[j]},c,false);}
        }
    }
}
