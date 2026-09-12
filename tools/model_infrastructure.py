"""Author native Minecraft cuboid models and small original pixel materials.

No external resource pack, renderer or runtime library is required by the mod.
Run this after create_resources.py when regenerating all historical resources.
"""
import json, random, struct, zlib, math
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/locksanddams'
def write(path,obj):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(obj,indent=2)+'\n',encoding='utf-8')

COLORS={'steel':(56,67,74),'edge':(93,109,117),'dark':(23,30,34),'brass':(192,145,68),
        'copper':(162,87,55),'teal':(53,137,132),'amber':(209,126,46),'wood':(91,60,39),
        'green':(95,190,104),'red':(182,65,52),'ivory':(222,215,186)}
def texture(name,color):
    rng=random.Random(name);rows=[]
    for y in range(32):
        row=bytearray([0])
        for x in range(32):
            noise=rng.randrange(-5,6)
            if name=='wood':noise+=(x%8==0)*-22+(x%8==1)*10+(y%13==0)*-3
            elif name in ('steel','edge','copper','teal'):noise+=(y%8==0)*5-(y%8==7)*5
            row.extend(max(0,min(255,c+noise)) for c in color)
        rows.append(row)
    def chunk(t,b):return struct.pack('!I',len(b))+t+b+struct.pack('!I',zlib.crc32(t+b)&0xffffffff)
    p=ROOT/f'textures/block/{name}.png';p.parent.mkdir(parents=True,exist_ok=True)
    p.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',32,32,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows)))+chunk(b'IEND',b''))
for n,c in COLORS.items():texture(n,c)

def box(lo,hi,tex,rotation=None):
    e={'from':lo,'to':hi,'faces':{d:{'texture':'#'+tex} for d in ('north','south','east','west','up','down')}}
    if rotation:e['rotation']=rotation
    return e
def model(name,parts):
    write(f'models/block/{name}.json',{'parent':'minecraft:block/block','textures':{**{n:'locksanddams:block/'+n for n in COLORS},'particle':'locksanddams:block/steel'},'elements':parts})
def item(name,block=None):write(f'models/item/{name}.json',{'parent':'locksanddams:block/'+(block or name)})
def bolts(z,y0=2,y1=13):
    return [box([x,y,z],[x+1,y+1,z+.6],'brass') for x in (1,14) for y in (y0,y1)]

# Timber boards in a riveted steel frame. Top tiles receive the walking deck.
panel=[box([0,0,6.25],[16,16,9.75],'wood')]
for x in (0,14):panel.append(box([x,0,5],[x+2,16,11],'steel'))
for y in (0,7,14):panel.append(box([0,y,5.5],[16,y+2,10.5],'edge'))
for z in (5,10.5):panel+=bolts(z)
model('gate_panel',panel)
for edge in range(5):
    lo,hi=(2 if edge==1 else 1 if edge==4 else 0),(14 if edge==2 else 15 if edge==3 else 16)
    deck=list(panel)+[box([lo,14,3],[hi,15,13],'dark')]
    for z in (3,12):deck.append(box([lo,15,z],[hi,16,z+1],'brass'))
    for x in range(lo,hi,2):deck.append(box([x,15,4],[x+1,16,12],'edge'))
    for x in (3,12):
        for z in (3,11):deck.append(box([x,11,z],[x+1,14,z+2],'steel'))
    model('gate_panel_top'+('' if edge==0 else '_'+str(edge)),deck)
item('gate_panel','gate_panel_top')
write('blockstates/gate_panel.json',{'variants':{f'axis={axis},top={str(top).lower()},edge={edge}':{'model':'locksanddams:block/gate_panel'+(('_top'+('' if edge==0 else '_'+str(edge))) if top else ''),**({'y':90} if axis=='x' else {})} for axis in ('x','y','z') for top in (False,True) for edge in range(5)}})

# The existing kinetic bearing renderer supplies the turning plate and shaft.
hinge=[box([0,0,0],[16,16,16],'steel'),box([1,3,1],[15,10,15],'edge'),box([3,10,3],[13,13,13],'brass'),box([4,13,4],[12,15,12],'steel')]
for x in (1,13):
    for z in (1,13):hinge.append(box([x,3,z],[x+2,4,z+2],'brass'))
for z in (0,15):hinge.append(box([4,4,z],[12,8,z+1],'dark'))
model('lock_hinge',hinge);item('lock_hinge')
write('blockstates/lock_hinge.json',{'variants':{'':{'model':'locksanddams:block/lock_hinge'}}})

def wheel(y,open):
    p=[box([7,y-2,7],[9,y+1,9],'brass')]
    for a in (0,45):
        rot={'origin':[8,y+.5,8],'axis':'y','angle':a}
        for lo,hi in [([3,y,3],[13,y+1,4]),([3,y,12],[13,y+1,13]),([3,y,4],[4,y+1,12]),([12,y,4],[13,y+1,12])]:p.append(box(lo,hi,'brass',rot if a else None))
    p.append(box([3,y,7.5],[13,y+1,8.5],'edge',{'origin':[8,y+.5,8],'axis':'y','angle':45 if open else 0}))
    p.append(box([7.5,y,3],[8.5,y+1,13],'edge'))
    return p

# Outward-facing wheel and gauge remain visible when embedded in a chamber wall.
for name,color in [('fill_valve','teal'),('drain_valve','amber'),('gate_drive','steel')]:
    for opened in (False,True):
        parts=[box([0,0,0],[16,16,16],'steel'),box([1,1,-.25],[15,15,0],color),box([0,0,-.5],[16,1,0],'edge'),box([0,15,-.5],[16,16,0],'edge'),box([6,6,-2],[10,10,0],'dark')]
        # Eight straight segments form an octagonal brass handwheel.
        for i in range(8):
            a,b=math.radians(i*45),math.radians((i+1)*45)
            x1,y1=8+5*math.cos(a),8+5*math.sin(a);x2,y2=8+5*math.cos(b),8+5*math.sin(b)
            cx,cy=(x1+x2)/2,(y1+y2)/2;length=math.hypot(x2-x1,y2-y1)+.35
            angle=math.degrees(math.atan2(y2-y1,x2-x1))%180
            vertical=45<angle<135;angle=angle-90 if vertical else angle-180 if angle>90 else angle
            angle=round(angle*2)/2
            lo=[cx-.5,cy-length/2,-2.5] if vertical else [cx-length/2,cy-.5,-2.5]
            hi=[cx+.5,cy+length/2,-1.5] if vertical else [cx+length/2,cy+.5,-1.5]
            parts.append(box(lo,hi,'brass',{'origin':[cx,cy,-2],'axis':'z','angle':angle}))
        for lo,hi in [([3,7.6,-2.4],[13,8.4,-1.6]),([7.6,3,-2.4],[8.4,13,-1.6])]:parts.append(box(lo,hi,'edge',{'origin':[8,8,-2],'axis':'z','angle':45 if opened else 0}))
        parts += [box([1.5,11,-.6],[5.5,15,-.3],'brass'),box([2,11.5,-.8],[5,14.5,-.6],'ivory'),box([3.3,12,-1],[3.7,14,-.8],'dark'),box([12,12,-.8],[14,14,-.3],'green' if opened else 'red')]
        model(name+('_open' if opened else ''),parts)
    item(name)
    write(f'blockstates/{name}.json',{'variants':{f'facing={d},open={str(opened).lower()}':{'model':f'locksanddams:block/{name}'+('_open' if opened else ''),'y':r} for d,r in [('north',0),('east',90),('south',180),('west',270)] for opened in (False,True)}})

# Heavy flanged culvert modules. Their full-block sealed envelope is retained.
core=[box([3,3,3],[13,13,13],'copper')]
for axis in (0,1,2):
    lo=[2,2,2];hi=[14,14,14];lo[axis]=6;hi[axis]=10;core.append(box(lo,hi,'steel'))
model('culvert_pipe_core',core)
branch=[box([4,4,0],[12,12,4],'copper'),box([2,2,0],[14,14,2],'edge'),box([3,3,0],[13,13,.5],'dark')]
for x in (2,12):
    for y in (2,12):branch.append(box([x,y,0],[x+2,y+2,1],'brass'))
model('culvert_pipe_branch',branch)
directions={'north':{},'east':{'y':90},'south':{'y':180},'west':{'y':270},'up':{'x':90},'down':{'x':270}}
write('blockstates/culvert_pipe.json',{'multipart':[{'apply':{'model':'locksanddams:block/culvert_pipe_core'}}]+[{'when':{d:'true'},'apply':{'model':'locksanddams:block/culvert_pipe_branch',**r}} for d,r in directions.items()]})
model('culvert_pipe',core+branch+[box([4,4,12],[12,12,16],'copper'),box([2,2,14],[14,14,16],'edge')]);item('culvert_pipe')

# The same surface-port item becomes a screened intake or a louvred outlet.
for role,color in [(0,'brass'),(1,'teal'),(2,'amber')]:
    p=[box([0,0,1],[16,16,16],'steel'),box([2,2,.5],[14,14,1],'dark')]
    for x in (0,14):p.append(box([x,0,0],[x+2,16,2],'edge'))
    for y in (0,14):p.append(box([0,y,0],[16,y+2,2],'edge'))
    p+=bolts(-.1,1,14)
    if role!=2:
        for x in (3,6,9,12):p.append(box([x,2,0],[x+1,14,1],'edge'))
        p.append(box([2,7,0],[14,8,1],'steel'))
    else:
        for y in (3,6,9,12):p.append(box([2,y,-.3],[14,y+1.5,1],'edge'))
    p += [box([5,13,-.2],[11,15,.5],color)]
    # Inlet points down into the culvert; outlet points up toward the canal.
    for i,width in enumerate((6,4,2) if role==1 else (2,4,6)):
        p.append(box([8-width/2,4+i,-.5],[8+width/2,5+i,-.2],color))
    model('culvert_port'+('' if role==0 else '_intake' if role==1 else '_outlet'),p)
write('blockstates/culvert_port.json',{'variants':{f'facing={d},role={role}':{'model':'locksanddams:block/culvert_port'+('' if role==0 else '_intake' if role==1 else '_outlet'),**r} for d,r in directions.items() if d not in ('up','down') for role in (0,1,2)}})
item('culvert_port')
print('Generated gate/catwalk, hinge, valves, drive, connected culverts, screened intake and louvred outlet models.')
