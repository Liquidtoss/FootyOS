"""Restore the CC0 sculpt detail and bake subdivision before skinning, never a posed mesh.
The source detail offsets use Blender Studio v1.0's level-one Multires vertex order.
"""
import bpy, struct
from pathlib import Path
from mathutils import Vector

def prepare_runtime_meshes():
    body=bpy.data.objects['Athlete • continuous skinned mesh']
    if body.get('runtime_surface_version') == 1:
        return
    # Heat weights and a hard rigid-hand boundary produced a pinched wrist. Replace
    # that boundary with a continuous forearm-to-hand blend, leaving palms rigid.
    for side,sign in [('L',1),('R',-1)]:
        wrist=Vector((sign*.4,-.01,.99))
        axis=Vector((sign*.035,-.015,-.09)).normalized()
        for v in body.data.vertices:
            t=(v.co-wrist).dot(axis)
            if v.co.x*sign < .28 or t < -.12 or v.co.z > 1.14:
                continue
            u=max(0,min(1,(t+.075)/.11));weight=u*u*(3-2*u)
            for group in body.vertex_groups:group.remove([v.index])
            body.vertex_groups['forearm.'+side].add([v.index],1-weight,'REPLACE')
            body.vertex_groups['hand.'+side].add([v.index],weight,'REPLACE')
    for obj in list(bpy.context.scene.objects):
        if obj.type!='MESH':continue
        subs=[m for m in obj.modifiers if m.type=='SUBSURF']
        for modifier in subs:
            bpy.ops.object.select_all(action='DESELECT');obj.select_set(True)
            bpy.context.view_layer.objects.active=obj
            modifier.show_viewport=True;modifier.show_render=True;modifier.levels=1
            # Applying after the armature can freeze posed geometry. Subdivide the
            # rest mesh first, interpolating skin weights at the same time.
            bpy.ops.object.modifier_move_to_index(modifier=modifier.name,index=0)
            bpy.ops.object.modifier_apply(modifier=modifier.name)
        for face in obj.data.polygons:face.use_smooth=True
    data=(Path(__file__).parent/'skin_detail.bin').read_bytes()
    count=struct.unpack_from('<I',data)[0]
    assert len(body.data.vertices)==count, 'Sculpt topology changed; regenerate detail offsets'
    for v,offset in zip(body.data.vertices,struct.iter_unpack('<3f',data[4:])):
        # Keep established sole/palm/grip contact geometry; authored sculpt detail
        # belongs on the rest of the body, especially the forearms and knuckles.
        if v.co.z>1.53 or (abs(v.co.x)>.27 and .80<v.co.z<1.30):
            strength=1.0
            if abs(v.co.x)>.32 and v.co.z<1.01:
                strength=.25
            v.co+=Vector(offset)*strength
    body['runtime_surface_version']=1
