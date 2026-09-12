"""Render a contact sheet from the shipped model cuboids for visual review."""
import json,math
import numpy as np
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/locksanddams'
out=ROOT.parent.parent/'outputs';out.mkdir(exist_ok=True)
canvas=Image.new('RGB',(1500,1050),'#141e25');draw=ImageDraw.Draw(canvas)
depth_buffer=np.full((1050,1500),-np.inf)
font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',23)
title=ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf',34)
draw.text((45,25),'LOCKS & DAMS  /  INDUSTRIAL INFRASTRUCTURE',font=title,fill='#e3dec9')
draw.text((45,76),'Native model geometry • Steel, timber, copper and brass • dev.7',font=font,fill='#94abb7')
def render(name,origin,scale,offset=(0,0,0),faces=None):
    parts=json.loads((A/f'models/block/{name}.json').read_text())['elements']
    faces=[] if faces is None else faces
    for e in parts:
        lo,hi=e['from'],e['to'];v=[(x,y,z) for x in (lo[0],hi[0]) for y in (lo[1],hi[1]) for z in (lo[2],hi[2])]
        if 'rotation' in e:
            r=e['rotation'];axis='xyz'.index(r['axis']);other=[n for n in range(3) if n!=axis];angle=math.radians(r['angle']);new=[]
            for pt in v:
                q=[pt[i]-r['origin'][i] for i in range(3)];i,j=other;a,b=q[i],q[j];q[i]=a*math.cos(angle)-b*math.sin(angle);q[j]=a*math.sin(angle)+b*math.cos(angle);new.append(tuple(q[k]+r['origin'][k] for k in range(3)))
            v=new
        v=[tuple(p[k]+offset[k] for k in range(3)) for p in v]
        for direction,inds,shade in [('north',[0,4,6,2],.82),('east',[4,5,7,6],.66),('up',[2,6,7,3],1.15)]:
            color=Image.open(A/('textures/block/'+e['faces'][direction]['texture'][1:]+'.png')).resize((1,1)).getpixel((0,0))
            pts=[v[k] for k in inds];depth=sum(x+y-z for x,y,z in pts)/4
            poly=[(origin[0]+(x+z)*.866*scale,origin[1]+((x-z)*.5-y)*scale) for x,y,z in pts]
            faces.append((depth,poly,tuple(min(255,int(c*shade)) for c in color),[x+y-z for x,y,z in pts]))
    return faces
def paint(faces):
    pixels=np.asarray(canvas).copy()
    for _,poly,c,depths in faces:
        for indices in ((0,1,2),(0,2,3)):
            tri=np.array([poly[i] for i in indices]);z=np.array([depths[i] for i in indices])
            x0,y0=np.maximum(np.floor(tri.min(axis=0)).astype(int),0);x1,y1=np.minimum(np.ceil(tri.max(axis=0)).astype(int),(1499,1049))
            if x1<x0 or y1<y0:continue
            yy,xx=np.mgrid[y0:y1+1,x0:x1+1];xx=xx+.5;yy=yy+.5
            (ax,ay),(bx,by),(cx,cy)=tri;den=(by-cy)*(ax-cx)+(cx-bx)*(ay-cy)
            if abs(den)<1e-9:continue
            a=((by-cy)*(xx-cx)+(cx-bx)*(yy-cy))/den;b=((cy-ay)*(xx-cx)+(ax-cx)*(yy-cy))/den;d=1-a-b
            depths_here=a*z[0]+b*z[1]+d*z[2];old=depth_buffer[y0:y1+1,x0:x1+1]
            mask=(a>=0)&(b>=0)&(d>=0)&(depths_here>=old-.00001)
            pixels[y0:y1+1,x0:x1+1][mask]=c;old[mask]=depths_here[mask]
    canvas.paste(Image.fromarray(pixels))
tiles=[('GATE + AUTOMATIC TOP CATWALK','gate'),('FILL VALVE • TEAL','fill_valve'),('DRAIN VALVE • AMBER','drain_valve'),('SCREENED UPPER INTAKE','culvert_port_intake'),('LOUVRED LOWER OUTLET','culvert_port_outlet'),('FLANGED CULVERT PIPE','culvert_pipe'),('KINETIC GATE HINGE','lock_hinge'),('GATE DRIVE','gate_drive'),('GRATED WALKING SURFACE','gate_panel_top')]
for i,(label,name) in enumerate(tiles):
    col,row=i%3,i//3;x,y=35+col*495,132+row*300
    draw.rounded_rectangle((x,y,x+465,y+280),radius=10,fill='#202e38')
    if name=='gate':
        f=[]
        for gx in range(3):
            for gy in range(3):f=render('gate_panel_top' if gy==2 else 'gate_panel',(x+100,y+180),2.8,(gx*16,gy*16,0),f)
        paint(f)
    else:paint(render(name,(x+130,y+183),7))
    draw.text((x+18,y+245),label,font=font,fill='#d8dedb')
canvas.save(out/'Infrastructure-Models-Preview.png')
print(out/'Infrastructure-Models-Preview.png')
