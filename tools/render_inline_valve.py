import bpy
from mathutils import Vector
from pathlib import Path
root=Path(__file__).resolve().parents[1]
bpy.ops.wm.open_mainfile(filepath=str(root/'tools/approved-art/Inline-Valve.blend'))
bpy.ops.object.camera_add(location=(2.1,1.8,-2.2));camera=bpy.context.object;camera.rotation_euler=(Vector((.5,.5,.5))-camera.location).to_track_quat('-Z','Y').to_euler();camera.data.type='ORTHO';camera.data.ortho_scale=1.8
scene=bpy.context.scene;scene.camera=camera;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.light='STUDIO';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_shadows=True;scene.display.shading.show_cavity=True;scene.display.shading.background_type='WORLD';scene.world.color=(.06,.07,.09)
scene.render.resolution_x=800;scene.render.resolution_y=800;scene.render.resolution_percentage=100;scene.render.filepath=str(root.parents[1]/'outputs/dev13-inline-valve.png');bpy.ops.render.render(write_still=True)
