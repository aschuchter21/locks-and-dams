"""Native three-block sloped industrial desk, selectors, lamps and labeled terminals."""
import json,math
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
A=ROOT/'assets/locksanddams'
def write(path,obj):
    p=A/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,indent=2)+'\n',encoding='utf-8')
def data(path,obj):
    p=ROOT/'data'/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,indent=2)+'\n',encoding='utf-8')
def box(lo,hi,t,rotation=None):
    e={'from':lo,'to':hi,'faces':{d:{'texture':'#'+t} for d in ['north','south','east','west','up','down']}}
    if rotation:e['rotation']=rotation
    return e
textures={n:'locksanddams:block/'+n for n in ['steel','edge','dark','brass','teal','amber','green','red','ivory']}
textures['particle']=textures['steel']
def model(name,parts,extra=None):write('models/block/'+name+'.json',{'parent':'minecraft:block/block','textures':{**textures,**(extra or {})},'elements':parts})
font=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',11)
small=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',8)
def desk_text(im,xy,text,font,fill):
    # Minecraft's top-face UVs face away from the operator. Rotate each label
    # in place so it stays beside the corresponding physical switch or lamp.
    layer=Image.new('RGBA',(128,24));ImageDraw.Draw(layer).text((64,12),text,font=font,anchor='mm',fill=fill)
    im.paste(layer.rotate(180),(int(xy[0]-64),int(xy[1]-12)),layer.rotate(180))
for part,name in enumerate(['UPPER GATE','WATER CONTROL','LOWER GATE']):
    im=Image.new('RGB',(128,128),'#29353b');d=ImageDraw.Draw(im)
    d.rectangle((3,3,124,124),outline='#899da5',width=2)
    desk_text(im,(64,113),name,font,'#f3edcf')
    labels=['CLOSED','OPEN','MOVING'] if part!=1 else ['CLOSED','FLOW','WARN']
    for x,label in zip([22,64,106],labels):desk_text(im,(x,84),label,small,'#c4d3d7')
    desk_text(im,(20,48),'FILL' if part==1 else 'CLOSE',small,'#e7debd')
    desk_text(im,(108,48),'DRAIN' if part==1 else 'OPEN',small,'#e7debd')
    if part==1:desk_text(im,(64,9),'EMERGENCY STOP',small,'#f5d875')
    else:desk_text(im,(64,10),'LOCKS & DAMS',small,'#94a9b0')
    im.save(A/f'textures/block/desk_face_{part}.png')

# Model faces are authored north-facing. The front edge is lower, the rear higher.
tilt={'origin':[8,11,8],'axis':'x','angle':-22.5}
def panel_box(lo,hi,t):return box(lo,hi,t,tilt)
for part in range(3):
    p=[box([1,0,2],[15,2,14],'dark'),box([2,2,3],[14,8,13],'steel'),box([0,7,0],[16,8,16],'edge'),box([1,8,14],[15,13,15],'steel')]
    panel=panel_box([.25,11,.25],[15.75,11.75,15.75],'steel');panel['faces']['up']={'texture':'#face','uv':[0,0,16,16]};p.append(panel)
    for x in [1,14]:p.append(panel_box([x,11.75,1],[x+.75,12.1,1.75],'brass'))
    if part!=1:
        for y in [4,5.5,7]:p.append(box([3,y,2.8],[13,y+.4,3],'dark'))
    extra={'face':f'locksanddams:block/desk_face_{part}'}
    if part!=1:
        label='ALARM' if part==0 else 'HORN';im=Image.new('RGB',(128,128),'#29353b');d=ImageDraw.Draw(im);d.text((64,26),label,font=font,anchor='mm',fill='#ebd28c');d.ellipse((43,50,85,92),fill='#b18b46');im.save(A/f'textures/block/desk_end_{part}.png')
        plate=box([14,2,3],[15,8,13],'steel') if part==0 else box([1,2,3],[2,8,13],'steel')
        plate['faces']['east' if part==0 else 'west']={'texture':'#end','uv':[0,0,16,16]};p.append(plate);extra['end']=f'locksanddams:block/desk_end_{part}'
    model(f'desk_{part}',p,extra)

for on in [False,True]:
    # A short rectangular selector handle visibly turns between its two positions.
    knob=[panel_box([6.2,11.75,6],[9.8,12.4,9.6],'brass')]
    # Keep the handle on the sloping plane by rotating its endpoints within that plane.
    angle=math.radians(40 if on else -40)
    for i in range(5):
        dx=(i-2)*.6*math.cos(angle);dz=(i-2)*.6*math.sin(angle)
        knob.append(panel_box([7.4+dx,12.4,7.2+dz],[8.6+dx,13.25,8.4+dz],'dark'))
    model('desk_switch_'+str(on).lower(),knob)
for color,x in [('green',2),('red',7),('amber',12)]:
    model('desk_lamp_'+color,[panel_box([x,11.75,10],[x+2,12.2,12],color)])
model('desk_lamps_off',[panel_box([x,11.75,10],[x+2,12,12],'dark') for x in [2,7,12]])
model('desk_estop',[panel_box([5.5,11.75,2.5],[10.5,12.1,5.5],'amber'),panel_box([6.5,12.1,3],[9.5,13.5,5],'red')])
model('desk_estop_light',[panel_box([11.3,11.75,2],[13.3,12.3,4],'red')])
rotations={'north':0,'east':90,'south':180,'west':270}
parts=[]
def add(when,name):
    for facing,y in rotations.items():parts.append({'when':{'facing':facing,**when},'apply':{'model':'locksanddams:block/'+name,'y':y}})
for part in range(3):add({'part':str(part)},'desk_'+str(part))
add({},'desk_lamps_off')
for on in [False,True]:add({'switch':str(on).lower()},'desk_switch_'+str(on).lower())
add({'lamp':'1'},'desk_lamp_green');add({'lamp':'2'},'desk_lamp_red')
add({'lamp':'3','flash':'true'},'desk_lamp_amber');add({'lamp':'4'},'desk_lamp_amber')
add({'part':'1','active':'true'},'desk_lamp_red');add({'part':'1'},'desk_estop')
add({'part':'1','alert':'true','flash':'true'},'desk_estop_light')
write('blockstates/control_desk.json',{'multipart':parts})
model('control_desk_item',json.loads((A/'models/block/desk_1.json').read_text())['elements']+json.loads((A/'models/block/desk_switch_false.json').read_text())['elements']+json.loads((A/'models/block/desk_estop.json').read_text())['elements'],{'face':'locksanddams:block/desk_face_1'})
write('models/item/control_desk.json',{'parent':'locksanddams:block/control_desk_item'})
write('models/item/control_link.json',{'parent':'minecraft:item/handheld','textures':{'layer0':'minecraft:item/recovery_compass'}})

# Four physical faces provide the two independent contacts in each gate node.
for face,label in [('north','UPPER CLUTCH'),('east','UPPER REVERSE'),('south','LOWER CLUTCH'),('west','LOWER REVERSE')]:
    im=Image.new('RGB',(128,128),'#29353b');d=ImageDraw.Draw(im)
    d.rectangle((4,4,123,123),outline='#bca170',width=4)
    d.text((64,17),label,font=small,anchor='mm',fill='#f3edcf')
    d.text((64,112),'15 = STOP' if 'CLUTCH' in label else '15 = CLOSE',font=small,anchor='mm',fill='#b0c4cc')
    d.ellipse((39,39,89,89),fill='#b68a48',outline='#e1c48a',width=4);d.ellipse((51,51,77,77),fill='#182127')
    im.save(A/f'textures/block/controller_{face}.png')
p=box([0,0,0],[16,16,16],'steel')
for face in ['north','east','south','west']:p['faces'][face]={'texture':'#'+face,'uv':[0,0,16,16]}
model('lock_controller',[p,box([5,0,5],[11,.5,11],'amber')],{f:f'locksanddams:block/controller_{f}' for f in ['north','east','south','west']})
write('blockstates/lock_controller.json',{'variants':{'facing='+f:{'model':'locksanddams:block/lock_controller','y':r} for f,r in rotations.items()}})
for upper in [False,True]:
    name='upper' if upper else 'lower';p=box([0,0,0],[16,16,16],'steel')
    p['faces']['north']={'texture':'#clutch','uv':[0,0,16,16]};p['faces']['up']={'texture':'#reverse','uv':[0,0,16,16]}
    model('gate_node_'+name,[p],{'clutch':'locksanddams:block/controller_'+('north' if upper else 'south'),'reverse':'locksanddams:block/controller_'+('east' if upper else 'west')})
write('blockstates/gate_control_node.json',{'variants':{f'facing={f},upper={str(upper).lower()}':{'model':'locksanddams:block/gate_node_'+('upper' if upper else 'lower'),'y':r} for f,r in rotations.items() for upper in [False,True]}})
write('models/item/gate_control_node.json',{'parent':'locksanddams:block/gate_node_upper'})
lang=json.loads((A/'lang/en_us.json').read_text());lang.update({'block.locksanddams.control_desk':'Lock Control Desk','block.locksanddams.gate_control_node':'Gate Control Node','item.locksanddams.control_link':'Control Link Tool','block.locksanddams.gate_drive':'Legacy Gate Drive'});write('lang/en_us.json',lang)
data('locksanddams/recipes/control_desk.json',{'type':'minecraft:crafting_shaped','pattern':['CRC','III','I I'],'key':{'C':{'item':'minecraft:copper_ingot'},'R':{'item':'minecraft:redstone'},'I':{'item':'minecraft:iron_ingot'}},'result':{'item':'locksanddams:control_desk'}})
data('locksanddams/recipes/control_link.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'minecraft:compass'},{'item':'minecraft:redstone'}],'result':{'item':'locksanddams:control_link'}})
data('locksanddams/loot_tables/blocks/control_desk.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'locksanddams:control_desk'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
data('locksanddams/loot_tables/blocks/gate_control_node.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'locksanddams:gate_control_node'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
data('locksanddams/recipes/gate_control_node.json',{'type':'minecraft:crafting_shaped','pattern':['IRI',' C '],'key':{'C':{'item':'minecraft:copper_ingot'},'R':{'item':'minecraft:redstone'},'I':{'item':'minecraft:iron_ingot'}},'result':{'item':'locksanddams:gate_control_node','count':2}})
pick=ROOT/'data/minecraft/tags/blocks/mineable/pickaxe.json';n=json.loads(pick.read_text());
if 'locksanddams:control_desk' not in n['values']:n['values'].append('locksanddams:control_desk')
if 'locksanddams:gate_control_node' not in n['values']:n['values'].append('locksanddams:gate_control_node')
pick.write_text(json.dumps(n,indent=2)+'\n')
print('Generated labeled sloped desk, rotary selectors, indicators and controller terminal faces.')
