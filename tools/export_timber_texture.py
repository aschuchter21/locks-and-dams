import bpy
from pathlib import Path
root=Path(__file__).resolve().parents[1]
bpy.ops.wm.open_mainfile(filepath=str(root/'tools/approved-art/Timber-Miter-Gates.blend'))
source=bpy.data.images['Timber 0'];pixels=list(source.pixels);atlas=bpy.data.images.new('timber_gate',width=128,height=256);out=[]
for y in range(256):
 for x in range(128):out.extend(pixels[(y*64+x)*4:(y*64+x)*4+4] if x<64 else [1,1,1,1])
atlas.pixels=out;atlas.filepath_raw=str(root/'src/main/resources/assets/locksanddams/textures/entity/timber_gate.png');atlas.file_format='PNG';atlas.save()
