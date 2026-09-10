"""Restore the CC0 sculpt detail and bake subdivision before skinning, never a posed mesh.
The source detail offsets use Blender Studio v1.0's level-one Multires vertex order.
"""
import bpy, struct
from pathlib import Path
from mathutils import Vector, Matrix

def correct_forearm_twist():
    """Align pronation with the palm without changing elbow, wrist or grip paths."""
    rig=bpy.data.objects['Athlete rig'];scene=bpy.context.scene
    samples=[]
    for frame in range(scene.frame_start,scene.frame_end+1):
        scene.frame_set(frame)
        samples.append((frame,{name:rig.pose.bones[name].matrix.copy()
            for side in ['L','R'] for name in ['forearm.'+side,'hand.'+side]}))
    max_error=0.0
    previous_rotation={}
    for frame,matrices in samples:
        scene.frame_set(frame)
        for side in ['L','R']:
            fore=rig.pose.bones['forearm.'+side];hand=rig.pose.bones['hand.'+side]
            rest=fore.bone.matrix_local
            axis=(fore.bone.tail_local-fore.bone.head_local).normalized()
            original=matrices[fore.name];palm=matrices[hand.name]
            direction=(palm.translation-original.translation).normalized()
            palm_rotation=palm.to_3x3() @ hand.bone.matrix_local.to_3x3().inverted()
            # Swing the palm's rest-space rotation onto the elbow-to-wrist axis.
            # This keeps the shaft straight while giving it the palm's axial roll.
            swing=(palm_rotation @ axis).rotation_difference(direction).to_matrix()
            rotation=swing @ palm_rotation
            fore.matrix=Matrix.Translation(original.translation) @ (rotation @ rest.to_3x3()).to_4x4()
            bpy.context.view_layer.update()
            hand.matrix=palm
            bpy.context.view_layer.update()
            assert max(abs(hand.matrix[r][c]-palm[r][c]) for r in range(4) for c in range(4))<.0001, "Palm pose changed"
            max_error=max(max_error,(fore.head-original.translation).length,
                (fore.tail-palm.translation).length,(hand.head-palm.translation).length)
            for bone in [fore,hand]:
                bone.rotation_mode='QUATERNION'
                if bone.name in previous_rotation and bone.rotation_quaternion.dot(previous_rotation[bone.name])<0:
                    bone.rotation_quaternion.negate()
                previous_rotation[bone.name]=bone.rotation_quaternion.copy()
                for channel in ['location','rotation_quaternion','scale']:
                    bone.keyframe_insert(channel,frame=frame)
    assert max_error<.0001, ('Arm contact moved',max_error)
    for curve in rig.animation_data.action.fcurves:
        for key in curve.keyframe_points:key.interpolation='LINEAR'
    scene.frame_set(1)
    print('WRIST_ALIGNMENT_MAX_ERROR_METRES',max_error)

def prepare_runtime_meshes():
    body=bpy.data.objects['Athlete • continuous skinned mesh']
    if body.get('runtime_surface_version') == 3:
        return
    correct_forearm_twist()
    # Keep the shaft on the forearm bone. Blend only across the anatomical wrist,
    # not 7.5 cm up the forearm, which produced a second apparent joint.
    for side,sign in [('L',1),('R',-1)]:
        wrist=Vector((sign*.4,-.01,.99))
        axis=Vector((sign*.035,-.015,-.09)).normalized()
        for v in body.data.vertices:
            t=(v.co-wrist).dot(axis)
            if v.co.x*sign < .28 or t < -.12 or v.co.z > 1.14:
                continue
            u=max(0,min(1,(t+.005)/.035));weight=u*u*(3-2*u)
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
    from forearm_surface import replace_forearms
    replace_forearms(body)
    body['runtime_surface_version']=3
