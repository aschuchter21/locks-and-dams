"""Export matching Blender flanged butterfly valve, with closed/open internal discs."""
from pathlib import Path
source=Path(__file__).with_name('export_approved_art.py').read_text()
exec(source[:source.index('for top in [False,True]:')])
exec(source[source.index('def ring('):source.index('def boolean(')])
for opened in [False,True]:
    clear()
    ring('Cast valve body',(0,0,1),(0,0,0),.36,.30,.86,'pipe')
    for d in [Vector((0,0,-1)),Vector((0,0,1))]:flange(d)
    cylinder('Butterfly disc',(-.018,0,0) if opened else (0,0,-.018),(.018,0,0) if opened else (0,0,.018),.298,'steel',48)
    cylinder('Valve spindle',(0,-.32,0),(0,.41,0),.045,'steel',16)
    box('Sealed gearbox',(0,.36,0),(.22,.17,.23),'paint')
    cylinder('Handwheel shaft',(0,.39,0),(0,.445,0),.027,'steel',16)
    ring('Safety yellow handwheel',(0,1,0),(0,.45,0),.20,.177,.035,'yellow')
    for k in range(3):
        a=k*math.tau/3;cylinder('Handwheel spoke',(0,.45,0),(.182*math.cos(a),.45,.182*math.sin(a)),.017,'yellow',12)
    box('Position indicator',(0,.465,0),(.10,.018,.025) if opened else (.025,.018,.10),'yellow')
    for o in bpy.context.scene.objects:o.location+=Vector((.5,.5,.5))
    export('inline_valve_'+('open' if opened else 'closed'))
    if not opened:bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'tools/approved-art/Inline-Valve.blend'))
variants={}
for axis,rotation in [('z',{}),('x',{'y':90}),('y',{'x':90})]:
    for opened in [False,True]:variants[f'axis={axis},open={str(opened).lower()}']={'model':'locksanddams:block/inline_valve_'+('open' if opened else 'closed'),**rotation}
(A/'blockstates/inline_valve.json').write_text(json.dumps({'variants':variants},indent=2))
(A/'models/item/inline_valve.json').write_text(json.dumps({'parent':'locksanddams:block/inline_valve_closed'}))
