"""Re-export existing animation sources with refined skin, without regenerating motion.
Blender -b --python assets/training3d/source/refresh_runtime_meshes.py -- [exercise IDs]
"""
import bpy, sys, json, hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(Path(__file__).parent));sys.path.insert(0,str(ROOT/'tools'))
from mesh_quality import prepare_runtime_meshes
from pack_training_glb import merge_animations
manifest_path=ROOT/'assets/training3d/manifest.json'
manifest=json.loads(manifest_path.read_text())
selected=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
for entry in manifest['exercises']:
    exercise=entry['id']
    if selected and exercise not in selected:continue
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/'assets/training3d/source'/f'{exercise}.blend'))
    scene=bpy.context.scene;scene.frame_set(1)
    prepare_runtime_meshes()
    bpy.ops.object.select_all(action='DESELECT')
    for obj in scene.objects:
        if obj.type in {'MESH','ARMATURE','CURVE','EMPTY'} and obj.name!='Studio ground':obj.select_set(True)
    path=ROOT/entry['runtimePath']
    bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='SCENE',export_frame_range=True,export_force_sampling=True,export_apply=True)
    merge_animations(path)
    entry['sha256']=hashlib.sha256(path.read_bytes()).hexdigest()
    entry['additionalSources']=[{
        'sourceUrl':'https://raw.githubusercontent.com/makehumancommunity/makehuman/master/makehuman/data/3dobjs/base.obj',
        'licensePath':'assets/training3d/licenses/makehuman-forearm.txt',
        'region':'Forearms and wrist transition'}]
    assert path.stat().st_size<=manifest['maxBytesPerAsset'], (exercise,path.stat().st_size)
    if exercise in {'overhead_press','floor_press','goblet_squat'}:
        scene.render.filepath=str(ROOT/'assets/training3d/previews'/f'{exercise}_refined.png')
        bpy.ops.render.render(write_still=True)
    print('REFINED',exercise,path.stat().st_size,flush=True)
manifest_path.write_text(json.dumps(manifest,indent=2)+'\n')
