import bpy,json
from pathlib import Path
from mathutils import Vector
root=Path(__file__).resolve().parents[3];bpy.ops.wm.open_mainfile(filepath=str(root/'assets/training3d/source/floor_press.blend'))
s=bpy.context.scene;rig=bpy.data.objects['Athlete rig'];body=bpy.data.objects['Athlete • continuous skinned mesh'];bell=bpy.data.objects['Kettlebell L']
for m in body.modifiers:
 if m.type=='SUBSURF':m.show_viewport=False
w=Vector((-.4,-.01,.99));D=Vector((-.035,-.015,-.09)).normalized();N=Vector((.93,0,-.36));N=(N-D*N.dot(D)).normalized()
ids=[v.index for v in body.data.vertices if v.co.x<-.355 and .04<(v.co-w).dot(D)<.10]
W=Vector((.4,-.01,.99));A=Vector((.035,-.015,-.09)).normalized();P=Vector((-.93,0,-.36));P=(P-A*P.dot(A)).normalized();B=P.cross(A);grip=W+A*.110+B*.050+P*.033
heights=[];alignment=[];errors=[]
for f in range(1,122):
 s.frame_set(f);dep=bpy.context.evaluated_depsgraph_get();mesh=body.evaluated_get(dep)
 heights.append(min((mesh.matrix_world @ mesh.data.vertices[i].co).z for i in ids))
 transform=rig.matrix_world @ rig.pose.bones['hand.R'].matrix @ rig.data.bones['hand.R'].matrix_local.inverted()
 alignment.append((transform.to_3x3() @ N).normalized().dot(Vector((0,0,-1))))
 left=rig.matrix_world @ rig.pose.bones['hand.L'].matrix @ rig.data.bones['hand.L'].matrix_local.inverted()
 errors.append(((left @ grip)-(bell.matrix_world @ Vector((0,0,.139)))).length)
result={'rightPalmHeightMeters':[min(heights),max(heights)],'matTopMeters':.022,'minimumPalmDownAlignment':min(alignment),'maximumGripHandleErrorMeters':max(errors)}
print(json.dumps(result,indent=2));(root/'assets/training3d/previews/floor_press_hand_validation.json').write_text(json.dumps(result,indent=2)+'\n')
assert min(alignment)>.99
assert max(errors)<.001
assert max(heights)-min(heights)<.0001

assert max(abs(h-.022) for h in heights)<.003
