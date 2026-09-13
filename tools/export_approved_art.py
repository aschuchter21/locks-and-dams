"""Blender production export of the approved modular gate and flanged pipe design.
Run Blender --background --python tools/export_approved_art.py.
Keeps OBJ meshes baked into Create's normal block/contraption rendering path.
"""
import bpy, math, json, itertools, struct,zlib
from pathlib import Path
from mathutils import Vector,Matrix
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/locksanddams';M=A/'models/block'
approved=json.loads((ROOT/'tools/approved-art/design.json').read_text())
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
materials={}
for name,(color,metal,rough) in approved['materials'].items():
    mat=bpy.data.materials.new(name);mat.diffuse_color=tuple(int(color[i:i+2],16)/255 for i in (1,3,5))+(1,);materials[name]=mat
    rgb=[int(color[i:i+2],16) for i in (1,3,5)];rows=[]
    for y in range(16):
        row=bytearray([0])
        for x in range(16):
            noise=((x*17+y*11)%5)-2;row.extend([max(0,min(255,c+noise)) for c in rgb]+[255])
        rows.append(row)
    def chunk(t,d):return struct.pack('!I',len(d))+t+d+struct.pack('!I',zlib.crc32(t+d)&0xffffffff)
    (A/f'textures/block/approved_{name}.png').write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',16,16,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows)))+chunk(b'IEND',b''))
def box(name,p,s,mat='paint',angle=0):
    bpy.ops.mesh.primitive_cube_add(size=1,location=p);o=bpy.context.object;o.scale=s;bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);o.rotation_euler.z=angle;o.name=name;o.data.materials.append(materials[mat]);return o
def cylinder(name,a,b,r,mat='steel',segments=16):
    a,b=Vector(a),Vector(b);v=b-a;bpy.ops.mesh.primitive_cylinder_add(vertices=segments,radius=r,depth=v.length,location=(a+b)/2);o=bpy.context.object;o.rotation_euler=v.to_track_quat('Z','Y').to_euler();o.name=name;o.data.materials.append(materials[mat]);
    for face in o.data.polygons:face.use_smooth=len(face.vertices)==4
    return o
def clear():
    bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
def export(name):
    lines=['# Approved Blender production mesh; block units','mtllib approved.mtl'];vi=1
    dg=bpy.context.evaluated_depsgraph_get()
    for o in list(bpy.context.scene.objects):
        if o.type!='MESH':continue
        ev=o.evaluated_get(dg);mesh=ev.to_mesh();mesh.calc_loop_triangles();transform=o.matrix_world
        normalmatrix=transform.to_3x3().inverted().transposed()
        lines+=['g '+o.name.replace(' ','_'),'usemtl '+o.data.materials[0].name]
        for triangle in mesh.loop_triangles:
            for j,loop in enumerate(triangle.loops):
                p=transform@mesh.vertices[mesh.loops[loop].vertex_index].co;n=(normalmatrix@mesh.corner_normals[loop].vector).normalized()
                lines.append('v '+' '.join(f'{v:.5f}' for v in p));lines.append('vn '+' '.join(f'{v:.5f}' for v in n));lines.append('vt '+['0 0','1 0','1 1'][j])
            lines.append('f '+' '.join(f'{i}/{i}/{i}' for i in range(vi,vi+3)));vi+=3
        ev.to_mesh_clear()
    (M/(name+'.obj')).write_text('\n'.join(lines)+'\n')
    (M/(name+'.json')).write_text(json.dumps({'loader':'forge:obj','model':'locksanddams:models/block/'+name+'.obj','flip_v':True,'automatic_culling':False,'shade_quads':True,'ambientocclusion':False,'textures':{**{k:'locksanddams:block/approved_'+k for k in materials},'particle':'locksanddams:block/approved_paint'},'display':{'gui':{'rotation':[25,225,0],'scale':[.65,.65,.65]},'ground':{'scale':[.35,.35,.35]},'fixed':{'scale':[.6,.6,.6]}}},indent=2)+'\n')
    print('Exported',name,vi//3,'triangles')
(M/'approved.mtl').write_text('\n'.join(f'newmtl {m}\nKd 1 1 1\nd 1\nmap_Kd #{m}\n' for m in materials))
for top in [False,True]:
    for edge in range(5) if top else [0]:
        clear();box('Continuous watertight skin',[.5,.5,.421875],[1,1,.0625])
        for x in [.035,.965]:box('Vertical steel stiffener',[x,.5,.51],[.07,1,.18],'edge')
        box('Horizontal girder web',[.5,.12,.53],[.94,.18,.045])
        for y in [.025,.215]:box('Girder flange',[.5,y,.54],[1,.035,.25],'edge')
        # One diagonal per tile keeps the structural design readable at every player-built size.
        box('Diagonal brace',[.5,.61,.61],[1.08,.042,.035],'edge',math.radians(45))
        for x in [.09,.91]:
            box('Bolt plate',[x,.12,.67],[.12,.19,.012])
            for y in [.055,.185]:cylinder('Hex bolt',[x,y,.676],[x,y,.693],.019,'steel',6)
        if top:
            lo={1:.3125,4:.0625}.get(edge,0);hi={2:.6875,3:.9375}.get(edge,1);mid=(lo+hi)/2
            for z in [.08,.92]:
                box('Catwalk edge channel',[mid,.95,z],[hi-lo,.1,.045],'edge')
                box('Safety toe plate',[mid,1.07,z],[hi-lo,.14,.018],'yellow')
                for y in [1.54,2.03]:cylinder('Handrail',[lo,y,z],[hi,y,z],.022,'yellow',12)
                for x in [lo+.05,hi-.05]:
                    box('Railing foot',[x,1.015,z],[.085,.025,.08],'dark')
                    cylinder('Handrail post',[x,1.025,z],[x,2.03,z],.022,'yellow',12)
            for i in range(10):
                x=lo+(hi-lo)*(i+.5)/10;box('Open grating bearing bar',[x,.97,.5],[(hi-lo)*.2/10,.06,.82],'steel')
            for z in [.2,.35,.5,.65,.8]:box('Grating cross bar',[mid,.985,z],[hi-lo,.024,.016],'steel')
        export('gate_panel'+(('_top'+('' if edge==0 else '_'+str(edge))) if top else ''))

# Directions are Minecraft's ordinal order. Every network state gets a baked rounded fitting.
directions=[Vector(v) for v in [(0,-1,0),(0,1,0),(0,0,-1),(0,0,1),(-1,0,0),(1,0,0)]]
def ring(name,axis,center,outer,inner,length,mat='edge'):
    axis=Vector(axis);p=Vector(center);u=axis.cross(Vector((0,1,0)) if abs(axis.y)<.9 else Vector((1,0,0))).normalized();v=axis.cross(u)
    verts=[];faces=[];n=32
    for z in [-length/2,length/2]:
        for radius in [outer,inner]:
            for k in range(n):verts.append(p+axis*z+radius*(u*math.cos(k*math.tau/n)+v*math.sin(k*math.tau/n)))
    for k in range(n):
        j=(k+1)%n
        faces += [(k,j,2*n+j,2*n+k),(n+j,n+k,3*n+k,3*n+j),(j,k,n+k,n+j),(2*n+k,2*n+j,3*n+j,3*n+k)]
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(verts,[],faces);mesh.update();o=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(o);o.data.materials.append(materials[mat])
    for polygon in mesh.polygons:polygon.use_smooth=polygon.index%4<2
def flange(d):
    ring('Flange',d,d*.437,.475,.312,.065);ring('Flange gasket',d,d*.475,.415,.312,.012,'rubber')
    u=d.cross(Vector((0,1,0)) if abs(d.y)<.9 else Vector((1,0,0))).normalized();v=d.cross(u)
    for k in range(12):
        center=d*.437+.411*(u*math.cos((k+.5)*math.tau/12)+v*math.sin((k+.5)*math.tau/12))
        cylinder('Washer',center-d*.04,center-d*.049,.033,'dark',12)
        cylinder('Hex flange bolt',center-d*.049,center-d*.079,.024,'steel',6)
def boolean(body,tool,operation):
    bpy.context.view_layer.objects.active=body;mod=body.modifiers.new(operation,'BOOLEAN');mod.operation=operation;mod.object=tool;mod.solver='EXACT';bpy.ops.object.modifier_apply(modifier=mod.name);bpy.data.objects.remove(tool,do_unlink=True)
for mask in range(64):
    clear();arms=[directions[i] for i in range(6) if mask&(1<<i)]
    if not arms:arms=[directions[2],directions[3]]
    if len(arms)==2 and arms[0].dot(arms[1])==0:
        # Quarter-circle elbow with genuinely curved inner and outer surfaces.
        a,b=arms;path=[a*.5*(1-math.sin(i*math.pi/48))+b*.5*(1-math.cos(i*math.pi/48)) for i in range(25)]
        verts=[];faces=[];n=32;normal=a.cross(b)
        for i,p in enumerate(path):
            tangent=(path[min(i+1,24)]-path[max(i-1,0)]).normalized();u=normal;v=tangent.cross(u)
            for radius in [.34,.312]:
                for k in range(n):verts.append(p+radius*(u*math.cos(k*math.tau/n)+v*math.sin(k*math.tau/n)))
        for i in range(24):
            for layer in range(2):
                for k in range(n):
                    j=(k+1)%n;base=i*2*n+layer*n;f=(base+k,base+j,base+2*n+j,base+2*n+k);faces.append(f if layer==0 else tuple(reversed(f)))
        mesh=bpy.data.meshes.new('Swept elbow');mesh.from_pydata(verts,[],faces);mesh.update();body=bpy.data.objects.new('Swept elbow',mesh);bpy.context.collection.objects.link(body);body.data.materials.append(materials['pipe'])
        for p in mesh.polygons:p.use_smooth=True
        # Recalculate the tube's consistent face orientation.
        import bmesh
        bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(mesh);bm.free()
    else:
        # Boolean cast junction: joined shells minus joined bores, including 3D tees/crosses.
        end=arms[0]*.5;body=cylinder('Cast pipe body',-arms[0]*.12,end,.34,'pipe',32)
        for d in arms[1:]:boolean(body,cylinder('Outer branch',-d*.12,d*.5,.34,'pipe',32),'UNION')
        for d in arms:boolean(body,cylinder('Open bore',-d*.15,d*.6,.312,'pipe',32),'DIFFERENCE')
        for p in body.data.polygons:p.use_smooth=True
        mod=body.modifiers.new('Crisp cast junction','EDGE_SPLIT');mod.split_angle=math.radians(35)
    for d in arms:flange(d)
    for o in bpy.context.scene.objects:o.location+=Vector((.5,.5,.5))
    export('culvert_round_'+str(mask))
names=['down','up','north','south','west','east']
(A/'blockstates/culvert_pipe.json').write_text(json.dumps({'variants':{','.join(f'{d}={str(bool(mask&(1<<i))).lower()}' for i,d in enumerate(names)):{'model':'locksanddams:block/culvert_round_'+str(mask)} for mask in range(64)}},indent=2))
(A/'models/item/culvert_pipe.json').write_text(json.dumps({'parent':'locksanddams:block/culvert_round_12'}))
print('APPROVED ART EXPORT COMPLETE')

variants={}
for axis in ['x','y','z']:
    for top in [False,True]:
        for edge in range(5):
            for reverse in [False,True]:
                model_edge=({1:2,2:1,3:4,4:3}.get(edge,edge) if reverse else edge)
                model='gate_panel'+(('_top'+('' if model_edge==0 else '_'+str(model_edge))) if top else '')
                variants[f'axis={axis},top={str(top).lower()},edge={edge},reversed={str(reverse).lower()}']={'model':'locksanddams:block/'+model,'y':((90 if axis=='x' else 0)+(180 if reverse else 0))%360}
(A/'blockstates/gate_panel.json').write_text(json.dumps({'variants':variants},indent=2)+'\n')
