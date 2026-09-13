"""Bake the approved editable Blender desk into multipart Minecraft OBJ assets."""
import bpy,bmesh,math,json,struct,zlib
from pathlib import Path
from mathutils import Vector,Matrix
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/locksanddams';M=A/'models/block'
bpy.ops.wm.open_mainfile(filepath=str(ROOT/'tools/approved-art/Industrial-Control-Desk.blend'))
# Keep the review master untouched. Production geometry omits microscopic ridges
# and multi-segment bevels that are expensive in Minecraft's chunk renderer.
for o in list(bpy.context.scene.objects):
 if o.name.startswith('Lens highlight'):
  bpy.data.objects.remove(o,do_unlink=True);continue
 if o.type=='FONT':
  o.data.extrude=0;o.data.resolution_u=2
  # Separate lettering from the instrument plate, including at oblique angles.
  o.location.z+=.006
 if o.type=='MESH':
  for modifier in list(o.modifiers):o.modifiers.remove(modifier)
  # Native cylinders have two rings. Dissolve alternate vertical edges evenly
  # to retain round silhouettes without dozens of invisible tiny facets.
  mesh=o.data
  if len(mesh.vertices)>=24 and len(mesh.polygons)==len(mesh.vertices)//2+2:
   bm=bmesh.new();bm.from_mesh(mesh);bm.verts.ensure_lookup_table()
   caps=[f for f in bm.faces if len(f.verts)>4]
   if len(caps)==2:
    ring=list(caps[0].verts)
    for v in ring[::2]:
     edges=[e for e in v.link_edges if all(len(f.verts)==4 for f in e.link_faces)]
     if edges:bmesh.ops.dissolve_edges(bm,edges=edges,use_verts=True)
    bm.normal_update();bm.to_mesh(mesh)
   bm.free()
bpy.ops.object.select_all(action='DESELECT')
for o in bpy.context.scene.objects:
 if o.type in ['MESH','FONT']:o.select_set(True)
bpy.context.view_layer.objects.active=next(o for o in bpy.context.scene.objects if o.type=='MESH');bpy.ops.object.convert(target='MESH')
objects=[o for o in bpy.context.scene.objects if o.type=='MESH'];materials={}
# The hand-authored cabinet shell had inward winding. Correct closed solids
# before section clipping; clipping first leaves open meshes with ambiguous inside.
for o in objects:
 if o.name.startswith('Three-section welded console'):
  bm=bmesh.new();bm.from_mesh(o.data)
  bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
  bm.normal_update();bm.to_mesh(o.data);bm.free()

for o in objects:
 for mat in o.data.materials:
  c=mat.diffuse_color[:3];rgb=[round(255*(v*12.92 if v<=.0031308 else 1.055*v**(1/2.4)-.055)) for v in c];materials[mat.name]=rgb
for name,rgb in list(materials.items()):
 for suffix,mult in [('',1),('_off',.23)]:
  rows=b''.join(bytes([0])+bytes(max(0,min(255,round(v*mult))) for _ in range(16) for v in rgb) for y in range(16))
  def chunk(t,d):return struct.pack('!I',len(d))+t+d+struct.pack('!I',zlib.crc32(t+d)&0xffffffff)
  (A/f'textures/block/desk_approved_{name}{suffix}.png').write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',16,16,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(rows))+chunk(b'IEND',b''))
mtl=[]
for name in materials:
 for suffix in ['', '_off','_lit']:
  texture=name+('_off' if suffix=='_off' else '');mtl.append(f'newmtl {name}{suffix}\nKa '+('1 1 1' if suffix=='_lit' else '0 0 0')+f'\nKd 1 1 1\nd 1\nmap_Kd #'+texture+'\n')
(M/'desk_approved.mtl').write_text('\n'.join(mtl))
textures={name+suffix:'locksanddams:block/desk_approved_'+name+suffix for name in materials for suffix in ['', '_off']};textures['particle']=textures['enamel']
def selected_part(o):return round(o.matrix_world.translation.x)
def knob(o):return o.name.startswith(('Selector handle','Selector pointer'))
def lens(o):return o.name.startswith(('Lamp lens','Lens highlight'))
def stoplight(o):return o.name.startswith('Emergency stop flashing indicator')
def export(name,items,part=None,suffix='',rotate=0):
 center=0 if part is None else 1-part;lo=-100 if part in [None,2] else center-.5;hi=100 if part in [None,0] else center+.5
 lines=['# Approved Blender desk mesh','mtllib desk_approved.mtl'];vi=1
 pivot=Vector((center,.03,.545+.47*.383+.084));normal=Vector((0,.383,1)).normalized();rot=Matrix.Translation(pivot)@Matrix.Rotation(math.radians(rotate),4,normal)@Matrix.Translation(-pivot)
 for o in items:
  mesh=o.data.copy();mesh.transform((rot if rotate else Matrix.Identity(4))@o.matrix_world)
  if max(v.co.x for v in mesh.vertices)<lo or min(v.co.x for v in mesh.vertices)>hi:bpy.data.meshes.remove(mesh);continue
  bm=bmesh.new();bm.from_mesh(mesh)
  for x,n in [(lo,(1,0,0)),(hi,(-1,0,0))]:bmesh.ops.bisect_plane(bm,geom=list(bm.verts)+list(bm.edges)+list(bm.faces),dist=.000001,plane_co=(x,0,0),plane_no=n,clear_inner=True)
  bm.normal_update();bm.to_mesh(mesh);bm.free();mesh.calc_loop_triangles()
  for tri in mesh.loop_triangles:
   mat=o.data.materials[tri.material_index].name+suffix;lines.append('usemtl '+mat)
   for i,loop in enumerate(tri.loops):
    p=mesh.vertices[mesh.loops[loop].vertex_index].co;n=mesh.corner_normals[loop].vector
    lines.append(f'v {p.x-center+.5:.5f} {p.z:.5f} {-.0-p.y+.5:.5f}');lines.append(f'vn {n.x:.5f} {n.z:.5f} {-n.y:.5f}');lines.append('vt '+['0 0','1 0','1 1'][i])
   lines.append('f '+' '.join(f'{v}/{v}/{v}' for v in range(vi,vi+3)));vi+=3
  bpy.data.meshes.remove(mesh)
 (M/(name+'.obj')).write_text('\n'.join(lines)+'\n')
 display={'gui':{'rotation':[25,225,0],'scale':[.32,.32,.32]},'ground':{'scale':[.20,.20,.20]},'fixed':{'scale':[.3,.3,.3]},'thirdperson_righthand':{'scale':[.25,.25,.25]}}
 (M/(name+'.json')).write_text(json.dumps({'loader':'forge:obj','model':'locksanddams:models/block/'+name+'.obj','automatic_culling':False,'shade_quads':True,'emissive_ambient':True,'ambientocclusion':False,'textures':textures,'display':display},indent=2))
 print('EXPORTED',name,vi//3,'triangles')
parts=[]
def add(part,condition,name):
 for direction,angle in [('north',0),('east',90),('south',180),('west',270)]:
  when={'facing':direction,'part':str(part)}
  if condition:when={'AND':[when,condition]}
  parts.append({'when':when,'apply':{'model':'locksanddams:block/'+name,'y':angle}})
for part in range(3):
 center=1-part
 export('desk_approved_'+str(part),[o for o in objects if not knob(o) and not lens(o) and not stoplight(o)],part);add(part,{},'desk_approved_'+str(part))
 for state in [False,True]:
  angle=(45 if state else -45)*(-1 if part==1 else 1);name=f'desk_approved_switch_{part}_{str(state).lower()}'
  export(name,[o for o in objects if knob(o) and selected_part(o)==center],part,rotate=angle);add(part,{'switch':str(state).lower()},name)
 for color in ['green','red','amber']:
  on={'lamp':'1'} if color=='green' else ({'OR':[{'lamp':'2'},{'active':'true'}]} if part==1 else {'lamp':'2'}) if color=='red' else {'OR':[{'lamp':'4'},{'lamp':'3','flash':'true'}]}
  off={'lamp':'0|2|3|4'} if color=='green' else ({'lamp':'0|1|3|4','active':'false'} if part==1 else {'lamp':'0|1|3|4'}) if color=='red' else {'OR':[{'lamp':'0|1|2'},{'lamp':'3','flash':'false'}]}
  for lit,condition in [(False,off),(True,on)]:
   name=f'desk_approved_lens_{part}_{color}_{str(lit).lower()}';export(name,[o for o in objects if lens(o) and selected_part(o)==center and o.data.materials[0].name==color],part,suffix='_lit' if lit else '_off');add(part,condition,name)
for lit,condition in [(False,{'OR':[{'alert':'false'},{'flash':'false'}]}),(True,{'alert':'true','flash':'true'})]:
 name='desk_approved_stop_'+str(lit).lower();export(name,[o for o in objects if stoplight(o)],1,suffix='_lit' if lit else '_off');add(1,condition,name)
export('desk_approved_item',objects)
(A/'blockstates/control_desk.json').write_text(json.dumps({'multipart':parts},indent=2))
(A/'models/item/control_desk.json').write_text(json.dumps({'parent':'locksanddams:block/desk_approved_item'}))
