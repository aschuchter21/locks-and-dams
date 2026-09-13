import bpy,math,json
from pathlib import Path
from mathutils import Vector,Matrix
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'design-review/Control-Desk-Design';OUT.mkdir(parents=True,exist_ok=True)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
def xyz(p):return Vector((p[0],-p[2],p[1]))
mats={}
for name,color,metal,rough in [('enamel','#334c55',.55,.33),('edge','#73858c',.8,.26),('panel','#17262e',.35,.42),('rubber','#10191d',0,.68),('letters','#d4dcd9',.1,.45),('yellow','#e8b842',.4,.34),('steel','#a4b2b9',.82,.24),('green','#4add80',.15,.25),('red','#d63432',.12,.27),('amber','#e9b33f',.15,.25)]:
 m=bpy.data.materials.new(name);m.use_nodes=True;c=[int(color[i:i+2],16)/255 for i in (1,3,5)];c=[v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in c];m.diffuse_color=(*c,1);bs=m.node_tree.nodes.get('Principled BSDF');bs.inputs['Base Color'].default_value=(*c,1);bs.inputs['Metallic'].default_value=metal;bs.inputs['Roughness'].default_value=rough
 if name in ['green','red','amber']:bs.inputs['Emission Color'].default_value=(*c,1);bs.inputs['Emission Strength'].default_value=.55 if name=='green' else .015
 mats[name]=m
def finish(o,name,mat,bevel=0):
 o.name=name;o.data.materials.append(mats[mat])
 if bevel:
  b=o.modifiers.new('Machined edge radii','BEVEL');b.width=bevel;b.segments=3;o.modifiers.new('Weighted surface normals','WEIGHTED_NORMAL')
 return o
def box(name,p,s,mat='enamel',bevel=.008):
 bpy.ops.mesh.primitive_cube_add(size=1,location=xyz(p));o=bpy.context.object;o.scale=(s[0],s[2],s[1]);bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);return finish(o,name,mat,bevel)
def cyl(name,a,b,r,mat='steel',n=48):
 av,bv=xyz(a),xyz(b);bpy.ops.mesh.primitive_cylinder_add(vertices=n,radius=r,depth=(bv-av).length,location=(av+bv)/2);o=bpy.context.object;o.rotation_euler=(bv-av).to_track_quat('Z','Y').to_euler()
 for f in o.data.polygons:f.use_smooth=len(f.vertices)==4
 return finish(o,name,mat,.002)
slope=.383;angle=math.atan(slope)
def py(z):return .545+(z+.5)*slope
def plate(name,x,z,w,d,mat='panel'):
 o=box(name,(x,py(z)+(.022 if mat=='panel' else .016),z),(w,.018,d),mat,.009);o.rotation_euler.x=-angle;return o
def disk(name,x,z,r,h,mat):
 c=Vector((x,py(z)+.034,z));normal=Vector((0,1,-slope)).normalized();return cyl(name,c,c+normal*h,r,mat)
def label(text,x,z,size=.032,mat='letters'):
 c=bpy.data.curves.new('Engraved '+text,'FONT');c.body=text;c.align_x='CENTER';c.align_y='CENTER';c.size=size;c.extrude=.00025;c.resolution_u=4;o=bpy.data.objects.new('Label '+text,c);bpy.context.collection.objects.link(o);o.location=xyz((x,py(z)+.033,z));o.rotation_euler=(Matrix.Rotation(-angle,4,'X') @ Matrix.Rotation(math.pi,4,'Z')).to_euler();o.data.materials.append(mats[mat]);return o
# Welded cabinet shell, with exactly the existing three-block control footprint.
verts=[]
for x in [-1.5,1.5]:
 for y,z in [(.07,-.49),(.07,.49),(py(.49),.49),(py(-.49),-.49)]:verts.append(xyz((x,y,z)))
mesh=bpy.data.meshes.new('Folded shell');mesh.from_pydata(verts,[],[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)]);mesh.update();o=bpy.data.objects.new('Three-section welded console',mesh);bpy.context.collection.objects.link(o);finish(o,o.name,'enamel',.012)
box('Recessed plinth',(0,.065,0),(2.91,.13,.83),'rubber',.015)
for x in [-1.36,1.36]:
 for z in [-.35,.35]:box('Anti-vibration foot',(x,.026,z),(.19,.045,.19),'rubber',.009)
for x in [-1,0,1]:
 box('Front access door',(x,.295,-.501),(.91,.35,.025),'panel',.013)
 for xx in [x-.395,x+.395]:
  for y in [.16,.43]:cyl('Captive access screw',(xx,y,-.517),(xx,y,-.526),.014,'steel',6)
 box('Door pull recess',(x+.31,.32,-.52),(.05,.13,.014),'rubber',.007)
 cyl('Flush quarter turn latch',(x+.31,.34,-.533),(x+.31,.34,-.538),.025,'steel',24)
 box('Rear service cover',(x,.49,.502),(.91,.62,.022),'panel',.01)
 for yy in range(5):box('Rear ventilation louvre',(x,.40+yy*.047,.52),(.61,.012,.026),'edge',.004)
 for xx in [x-.39,x+.39]:
  for y in [.23,.73]:cyl('Rear panel fastener',(xx,y,.515),(xx,y,.526),.012,'steel',6)
 # Sloped inset instrument panels retain original selector locations and E-stop zone.
 plate('Brushed panel surround',x,0,.97,.95,'edge');plate('Inset control plate',x,0,.925,.91,'panel')
 for xx in [x-.425,x+.425]:
  for z in [-.40,.40]:disk('Instrument panel screw',xx,z,.014,.008,'steel')
for x,key,title in [(1,'upper','UPPER GATE'),(0,'water','FILL / DRAIN'),(-1,'lower','LOWER GATE')]:
 label(title,x,.36,.052)
 for dx,color,caption in ([ (.23,'amber','WARN'),(0,'red','FLOW'),(-.23,'green','CLOSED')] if key=='water' else [(.23,'green','CLOSED'),(0,'red','OPEN'),(-.23,'amber','MOVING')]):
  label(caption,x+dx,.298,.026)
  disk('Lamp metal bezel '+key+' '+color,x+dx,.205,.057,.021,'steel')
  disk('Lamp gasket '+key+' '+color,x+dx,.205,.047,.028,'rubber')
  disk('Lamp lens '+key+' '+color,x+dx,.205,.039,.044,color)
  for k in range(4):
   # Concentric molded lens ridges catch the studio lights.
   disk('Lens highlight '+key+' '+color,x+dx,.205,.039-k*.006,.045+k*.0005,color)
 disk('Selector brushed escutcheon '+key,x,-.03,.107,.017,'steel')
 disk('Selector black bezel '+key,x,-.03,.084,.033,'rubber')
 disk('Selector spindle '+key,x,-.03,.054,.051,'panel')
 handle=box('Selector handle '+key,(x,py(-.03)+.113,-.055),(.04,.068,.15),'rubber',.012);handle.rotation_euler.x=-angle
 box('Selector pointer '+key,(x,py(-.03)+.150,-.098),(.015,.009,.028),'letters',.002).rotation_euler.x=-angle
 label('FILL' if key=='water' else 'CLOSE',x+.24,-.035,.031)
 label('DRAIN' if key=='water' else 'OPEN',x-.24,-.035,.031)
 if key!='water':
  label('GATE SELECTOR',x,-.23,.031)
  label('CANAL OPERATIONS',x,-.355,.020,'edge')
# The red mushroom stays in the center/front hit region.
disk('Emergency stop yellow collar',0,-.31,.113,.022,'yellow')
disk('Emergency stop rubber boot',0,-.31,.066,.051,'rubber')
disk('Emergency stop red mushroom',0,-.31,.083,.093,'red')
label('EMERGENCY\nSTOP',.235,-.31,.026)
disk('Emergency stop flashing bezel',-.225,-.31,.038,.018,'steel')
disk('Emergency stop flashing indicator',-.225,-.31,.026,.038,'red')
label('STOP',-.225,-.405,.024)
# Protective front wrist edge and end output terminals.
box('Rounded front wrist edge',(0,.535,-.50),(2.98,.045,.055),'rubber',.019)
for side,name in [(1,'ALARM'),(-1,'HORN')]:
 x=side*1.503
 box(name+' terminal plate',(x,.31,.18),(.026,.28,.27),'panel',.011)
 cyl(name+' socket collar',(x,.31,.18),(x+side*.035,.31,.18),.071,'steel')
 cyl(name+' insulated output',(x+side*.034,.31,.18),(x+side*.051,.31,.18),.051,'rubber')
 cyl(name+' brass contact',(x+side*.05,.31,.18),(x+side*.061,.31,.18),.026,'yellow',12)
 label(name,side*1.33,-.34,.028,'yellow')
# Preserve editable lettering in the Blender source; GLB contains real text meshes.
scene=bpy.context.scene;scene.world.color=(.11,.13,.16)
scene.render.engine='CYCLES';scene.cycles.samples=24
bpy.ops.object.camera_add(location=(3.3,4.0,3.0));camera=bpy.context.object;camera.rotation_euler=(xyz((0,.47,0))-camera.location).to_track_quat('-Z','Y').to_euler();camera.data.type='ORTHO';camera.data.ortho_scale=3.9;scene.camera=camera
for p,power,size in [((0,2,4),650,4),((-3,-1,2),450,3),((3,-2,3),650,3)]:
 bpy.ops.object.light_add(type='AREA',location=p);o=bpy.context.object;o.data.energy=power;o.data.shape='DISK';o.data.size=size;o.rotation_euler=(xyz((0,.45,0))-o.location).to_track_quat('-Z','Y').to_euler()
scene.render.resolution_x=1500;scene.render.resolution_y=1000;scene.render.resolution_percentage=100
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'Industrial-Control-Desk.blend'))
bpy.ops.object.select_all(action='DESELECT')
for o in scene.objects:
 if o.type in ['MESH','FONT']:o.select_set(True)
bpy.context.view_layer.objects.active=next(o for o in scene.objects if o.type=='MESH');bpy.ops.object.convert(target='MESH')
bpy.ops.export_scene.gltf(filepath=str(OUT/'control-desk.glb'),use_selection=True,export_apply=True)
scene.render.filepath=str(OUT/'Desk-Overview.png');bpy.ops.render.render(write_still=True)
print('DESK DESIGN COMPLETE')
