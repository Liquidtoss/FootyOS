"""Fit the CC0 MakeHuman forearm surface to the athlete's actual wrist centre.
Only the exposed forearm is reprojected; hands, equipment and exercise paths stay intact.
"""
import bpy,json
from pathlib import Path
from mathutils import Vector,Matrix
from mathutils.bvhtree import BVHTree

def smooth(t):
    t=max(0,min(1,t));return t*t*(3-2*t)

def replace_forearms(body):
    rig=bpy.data.objects['Athlete rig'];scene=bpy.context.scene
    # The old rig wrist is not at the skin's wrist centre. Locate the surface
    # before fitting; driving the replacement to the old joint recreates the ridge.
    definitions={}
    for side,sign in [('L',1),('R',-1)]:
        wrist=rig.data.bones['hand.'+side].head_local.copy()
        axis=(rig.data.bones['hand.'+side].tail_local-wrist).normalized()
        ring=[v.co.copy() for v in body.data.vertices if v.co.x*sign>.3 and abs((v.co-wrist).dot(axis))<.006]
        centre=sum(ring,Vector())/len(ring)
        elbow=rig.data.bones['forearm.'+side].head_local.copy()
        definitions[side]=(elbow,centre)
    poses=[]
    for frame in range(scene.frame_start,scene.frame_end+1):
        scene.frame_set(frame);sample={}
        for side,(elbow,centre) in definitions.items():
            hand=rig.pose.bones['hand.'+side]
            transform=hand.matrix @ hand.bone.matrix_local.inverted()
            sample[side]=(rig.pose.bones['forearm.'+side].head.copy(),transform @ centre,transform.to_3x3())
        poses.append((frame,sample))
    bpy.ops.object.select_all(action='DESELECT');rig.select_set(True);bpy.context.view_layer.objects.active=rig
    bpy.ops.object.mode_set(mode='EDIT')
    for side,(elbow,centre) in definitions.items():
        bone=rig.data.edit_bones.new('surface_forearm.'+side);bone.head=elbow;bone.tail=centre
    bpy.ops.object.mode_set(mode='OBJECT')
    previous={};max_error=0
    for frame,sample in poses:
        scene.frame_set(frame)
        for side,(head,tail,hand_rotation) in sample.items():
            bone=rig.pose.bones['surface_forearm.'+side];rest=bone.bone
            axis=(rest.tail_local-rest.head_local).normalized();direction=(tail-head).normalized()
            rotation=(hand_rotation @ axis).rotation_difference(direction).to_matrix() @ hand_rotation
            # Longitudinal fitting ends at the visible wrist, not the misplaced old pivot.
            scale=(tail-head).length/rest.length
            bone.matrix=Matrix.Translation(head) @ (rotation @ rest.matrix_local.to_3x3()).to_4x4() @ Matrix.Diagonal((1,scale,1,1))
            bpy.context.view_layer.update();max_error=max(max_error,(bone.tail-tail).length)
            bone.rotation_mode='QUATERNION'
            if side in previous and bone.rotation_quaternion.dot(previous[side])<0:bone.rotation_quaternion.negate()
            previous[side]=bone.rotation_quaternion.copy()
            for channel in ['location','rotation_quaternion','scale']:bone.keyframe_insert(channel,frame=frame)
    assert max_error<.0001,('Visible wrist attachment drift',max_error)
    # Subdivide the downloaded anatomical surface before projecting our dense cage.
    data=json.loads(Path(__file__).with_name('makehuman_forearm.json').read_text())
    mesh=bpy.data.meshes.new('MakeHuman forearm reference');mesh.from_pydata(data['vertices'],[],data['faces']);mesh.update()
    reference=bpy.data.objects.new('MakeHuman forearm reference',mesh);bpy.context.collection.objects.link(reference)
    sub=reference.modifiers.new('Anatomical surface refinement','SUBSURF');sub.levels=2
    bpy.context.view_layer.update();evaluated=reference.evaluated_get(bpy.context.evaluated_depsgraph_get());surface=evaluated.to_mesh()
    bvh=BVHTree.FromPolygons([v.co for v in surface.vertices],[list(p.vertices) for p in surface.polygons])
    fitted=0;missed=0
    for side,sign in [('L',1),('R',-1)]:
        elbow,centre=definitions[side];length=(centre-elbow).length;axis=(centre-elbow).normalized()
        normal=Vector((0,-1,0));normal=(normal-axis*normal.dot(axis)).normalized();width=normal.cross(axis)
        group=body.vertex_groups.new(name='surface_forearm.'+side)
        for v in body.data.vertices:
            offset=v.co-elbow;t=offset.dot(axis)/length;radial=offset-axis*offset.dot(axis)
            if v.co.x*sign<.25 or not .02<t<1.16 or radial.length>.11:continue
            weight=smooth((t-.02)/.20)*(1-smooth((t-.92)/.22))
            direction=Vector((radial.dot(width),radial.dot(normal),0)).normalized()
            hit,_,_,_=bvh.ray_cast(Vector((0,0,t)),direction,.8)
            if hit is not None:
                target=elbow+axis*(t*length)+(width*hit.x+normal*hit.y)*length*1.3
                v.co=v.co.lerp(target,weight);fitted+=1
            elif weight>.01:missed+=1
            # Blend into existing elbow skinning; keep the shaft on one continuous
            # anatomical deformation and attach its distal end to the real palm.
            influence=smooth((t-.02)/.20)
            old=[(g.group,g.weight) for g in v.groups]
            for index,w in old:body.vertex_groups[index].add([v.index],w*(1-influence),'REPLACE')
            hand_weight=smooth((t-.95)/.12)
            group.add([v.index],influence*(1-hand_weight),'REPLACE')
            body.vertex_groups['hand.'+side].add([v.index],influence*hand_weight,'ADD')
    evaluated.to_mesh_clear();bpy.data.objects.remove(reference,do_unlink=True);bpy.data.meshes.remove(mesh)
    assert missed==0,('Incomplete anatomical projection',missed)
    for curve in rig.animation_data.action.fcurves:
        for key in curve.keyframe_points:key.interpolation='LINEAR'
    scene.frame_set(1)
    print('ANATOMICAL_FOREARM',fitted,'vertices; attachment error',max_error,flush=True)
