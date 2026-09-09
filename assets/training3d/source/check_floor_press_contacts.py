import bpy,json
from pathlib import Path
root=Path(__file__).resolve().parents[3]
bpy.ops.wm.open_mainfile(filepath=str(root/'assets/training3d/source/floor_press.blend'))
body=bpy.data.objects['Athlete • continuous skinned mesh'];rig=bpy.data.objects['Athlete rig']
for m in body.modifiers:
 if m.type=='SUBSURF':m.show_viewport=False
regions={}
for side,sign in [('L',1),('R',-1)]:
 for name,condition in [('heel',lambda v:v.y>-.025),('forefoot',lambda v:v.y<-.10)]:
  regions[side+'_'+name]=[v.index for v in body.data.vertices if v.co.x*sign>0 and v.co.z<.06 and condition(v.co)]
contacts={key:[] for key in regions};reference=None;max_drift=0
for frame in range(1,122):
 bpy.context.scene.frame_set(frame)
 evaluated=body.evaluated_get(bpy.context.evaluated_depsgraph_get())
 for name,indices in regions.items():contacts[name].append(min((evaluated.matrix_world @ evaluated.data.vertices[i].co).z for i in indices))
 pose=[v for name in ['pelvis','spine','chest','foot.L','foot.R'] for row in rig.pose.bones[name].matrix for v in row]
 if reference is None:reference=pose
 max_drift=max(max_drift,max(abs(a-b) for a,b in zip(pose,reference)))
result={'matTopMeters':.022,'soleHeights':{key:[min(v),max(v)] for key,v in contacts.items()},'maxCoreAndFootMatrixDrift':max_drift}
print(json.dumps(result,indent=2))
(root/'assets/training3d/previews/floor_press_contact_validation.json').write_text(json.dumps(result,indent=2)+'\n')
assert max_drift<1e-5
assert all(abs(height-.022)<.012 for heights in contacts.values() for height in heights)
