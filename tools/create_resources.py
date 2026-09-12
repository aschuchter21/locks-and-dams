"""Generate simple vanilla-textured models for the Create integration prototype."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources'
a=root/'assets/locksanddams'
def write(p,obj):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(obj,indent=2)+'\n',encoding='utf-8')
def element(lo,hi,tex):
    return {'from':lo,'to':hi,'faces':{d:{'texture':'#'+tex} for d in ['north','south','east','west','up','down']}}
write(a/'models/block/gate_panel.json',{'textures':{'metal':'minecraft:block/iron_block','particle':'minecraft:block/iron_block'},'elements':[element([.5,0,6],[15.5,16,10],'metal')]})
write(a/'blockstates/gate_panel.json',{'variants':{f'axis={axis}':{'model':'locksanddams:block/gate_panel',**({'y':90} if axis=='x' else {})} for axis in ['x','y','z']}})
write(a/'blockstates/gate_seal.json',{'variants':{'':{'model':'minecraft:block/air'}}})
write(a/'blockstates/lock_hinge.json',{'variants':{'':{'model':'create:block/mechanical_bearing'}}})
write(a/'models/item/lock_hinge.json',{'parent':'create:item/mechanical_bearing'})
for name,texture in [('culvert_pipe','cut_copper'),('culvert_port','deepslate_tiles')]:
    model={'parent':'minecraft:block/cube_all','textures':{'all':'minecraft:block/'+texture}}
    if name=='culvert_port':
        model={'parent':'minecraft:block/orientable','textures':{'side':'minecraft:block/cut_copper','front':'minecraft:block/dispenser_front','top':'minecraft:block/cut_copper'}}
    write(a/f'models/block/{name}.json',model)
    variants={'':{'model':f'locksanddams:block/{name}'}} if name=='culvert_pipe' else {f'facing={d}':{'model':f'locksanddams:block/{name}','y':r} for d,r in [('north',0),('east',90),('south',180),('west',270)]}
    write(a/f'blockstates/{name}.json',{'variants':variants})
    write(a/f'models/item/{name}.json',{'parent':f'locksanddams:block/{name}'})
for name,metal in [('fill_valve','oxidized_copper'),('drain_valve','cut_copper')]:
    for active in [False,True]:
        # Full outer wall flange, inset metal body and a raised cross-shaped wheel.
        write(a/f'models/block/{name}{"_open" if active else ""}.json',{'textures':{'body':'minecraft:block/'+metal,'wheel':'minecraft:block/iron_block','light':'minecraft:block/'+('emerald_block' if active else 'redstone_block'),'particle':'minecraft:block/'+metal},'elements':[
            element([0,0,0],[16,13,16],'body'),element([6,13,6],[10,15,10],'light'),
            element([2,15,6],[14,16,10],'wheel'),element([6,15,2],[10,16,14],'wheel')]})
for name,ingredient,count in [('lock_hinge',{'item':'create:mechanical_bearing'},1),('culvert_pipe',{'item':'minecraft:copper_ingot'},4),('culvert_port',{'item':'locksanddams:culvert_pipe'},1)]:
    write(root/f'data/locksanddams/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'locksanddams:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    write(root/f'data/locksanddams/recipes/{name}.json',{'type':'minecraft:crafting_shaped','pattern':[' I ','IRI',' I '],'key':{'I':ingredient,'R':{'item':'minecraft:redstone'}},'result':{'item':'locksanddams:'+name,'count':count}})
lang=json.loads((a/'lang/en_us.json').read_text())
lang.update({'block.locksanddams.lock_hinge':'Lock Hinge','block.locksanddams.culvert_pipe':'Culvert Pipe','block.locksanddams.culvert_port':'Culvert Port'})
write(a/'lang/en_us.json',lang)
