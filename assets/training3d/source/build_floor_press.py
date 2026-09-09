"""Blender 4.5.9: reproducible floor-press visual prototype from Blender Studio's CC0 mesh.
Run: Blender -b --python build_floor_press.py -- /path/to/human_base_meshes_bundle.blend
"""
import bpy, bmesh, math, sys, json
from pathlib import Path
from mathutils import Vector, Matrix
from mathutils.kdtree import KDTree
ROOT=Path(__file__).resolve().parents[3]
OUT=ROOT/'assets/training3d'
source=sys.argv[sys.argv.index('--')+1] if '--' in sys.argv else '/tmp/footyos-human/human_base_meshes_bundle.blend'
bpy.ops.wm.read_factory_settings(use_empty=True)
with bpy.data.libraries.load(source,link=False) as (src,dst):
    dst.objects=['GEO-body_male_realistic','GEO-body_male_realistic.eye.L','GEO-body_male_realistic.eye.R']
body=dst.objects[0]; origin=body.location.copy()
for o in dst.objects:
    bpy.context.collection.objects.link(o); o.location-=origin
    o.animation_data_clear()
    for m in list(o.modifiers): o.modifiers.remove(m)
body.name='Athlete • continuous skinned mesh'
bpy.context.preferences.filepaths.save_version=0

def material(name,color,rough=.6,metal=0):
    m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);m.use_nodes=True
    bs=m.node_tree.nodes.get('Principled BSDF');bs.inputs['Base Color'].default_value=(*color,1);bs.inputs['Roughness'].default_value=rough;bs.inputs['Metallic'].default_value=metal
    return m
skin=material('Warm neutral skin',(.49,.285,.17),.52)
shirt=material('Charcoal performance knit',(.038,.051,.057),.85)
shorts=material('Deep graphite shorts',(.018,.023,.028),.8)
lime=material('FootyOS lime',(.64,.88,.085),.5)
shoe=material('Training shoe rubber',(.065,.085,.095),.82)
white=material('Eye sclera',(.70,.72,.66),.4)
for m in [skin,shirt,shorts,lime,shoe]: body.data.materials.append(m)
for p in body.data.polygons:
    x,y,z=p.center if False else sum((body.data.vertices[i].co for i in p.vertices),Vector())/len(p.vertices)
    ax=abs(x)
    p.material_index=0
    # Fitted training top includes torso, shoulders and short sleeves.
    if .99 < z < 1.505 and (ax < .22 or z>1.30): p.material_index=1
    if .705 < z <= 1.01 and ax < .24: p.material_index=2
    if .985 < z < 1.009 and ax < .24: p.material_index=3
    if .708 < z < .735 and ax < .24: p.material_index=3
    if z < .115: p.material_index=4
    p.use_smooth=True
# Fitted clothing is a separate smooth shell with real cuffs and thickness.
clothing=[]
for name,indices,clothmat in [('Performance shirt',{1},shirt),('Training shorts',{2,3},shorts)]:
    obj=body.copy();obj.data=body.data.copy();obj.name=name;bpy.context.collection.objects.link(obj)
    bm=bmesh.new();bm.from_mesh(obj.data)
    bmesh.ops.delete(bm,geom=[f for f in bm.faces if f.material_index not in indices],context='FACES')
    # Smooth boundary loops to avoid polygonal paint-mask hems.
    border={v for edge in bm.edges if edge.is_boundary for v in edge.verts}
    for _ in range(6):
        update={v:sum((e.other_vert(v).co for e in v.link_edges if e.is_boundary),Vector())/max(1,sum(e.is_boundary for e in v.link_edges)) for v in border}
        for v,co in update.items():v.co=v.co.lerp(co,.45)
    bm.to_mesh(obj.data);bm.free()
    obj.data.materials.clear();obj.data.materials.append(clothmat)
    for polygon in obj.data.polygons:polygon.material_index=0
    clothing.append(obj)
# Skin below shells, with a short cropped hairstyle and lime waist trim.
for polygon in body.data.polygons:
    center=sum((body.data.vertices[i].co for i in polygon.vertices),Vector())/len(polygon.vertices)
    polygon.material_index=1 if center.z>1.72 and center.y>-.09 else 0
# Body is already an anatomical continuous surface; no primitive replacement.
arm=bpy.data.armatures.new('FootyOS athlete skeleton');rig=bpy.data.objects.new('Athlete rig',arm);bpy.context.collection.objects.link(rig)
bpy.context.view_layer.objects.active=rig;rig.select_set(True);bpy.ops.object.mode_set(mode='EDIT')
spec={
 'pelvis':((0,0,.9),(0,0,1.07),None),
 'spine':((0,0,1.07),(0,0,1.28),'pelvis'),
 'chest':((0,0,1.28),(0,0,1.47),'spine'),
 'neck':((0,0,1.47),(0,0,1.58),'chest'),
 'head':((0,0,1.58),(0,0,1.77),'neck'),
}
for side,sign in [('L',1),('R',-1)]:
    spec.update({
      'clavicle.'+side:((0,0,1.44),(sign*.19,0,1.44),'chest'),
      'upper_arm.'+side:((sign*.19,0,1.44),(sign*.315,0,1.205),'clavicle.'+side),
      'forearm.'+side:((sign*.315,0,1.205),(sign*.4,-.01,.99),'upper_arm.'+side),
      'hand.'+side:((sign*.4,-.01,.99),(sign*.435,-.025,.90),'forearm.'+side),
      'thigh.'+side:((sign*.1,0,.95),(sign*.137,-.02,.51),'pelvis'),
      'shin.'+side:((sign*.137,-.02,.51),(sign*.15,0,.105),'thigh.'+side),
      'foot.'+side:((sign*.15,0,.105),(sign*.17,-.14,.04),'shin.'+side),
    })
for name,(head,tail,parent) in spec.items():
    b=arm.edit_bones.new(name);b.head=head;b.tail=tail
    if parent:b.parent=arm.edit_bones[parent]
bpy.ops.object.mode_set(mode='OBJECT')
bpy.ops.object.select_all(action='DESELECT');body.select_set(True);rig.select_set(True);bpy.context.view_layer.objects.active=rig
bpy.ops.object.parent_set(type='ARMATURE_AUTO')
# Rigid sole weighting avoids ankle blend weights lifting the heel or toe.
for vertex in body.data.vertices:
    if vertex.co.z < .13:
        for group in body.vertex_groups: group.remove([vertex.index])
        body.vertex_groups['foot.' + ('L' if vertex.co.x > 0 else 'R')].add([vertex.index],1,'REPLACE')
# Bend a volume-preserving finger cross-section around the handle, including thumb opposition.
grip_wrist=Vector((.4,-.01,.99));grip_axis=Vector((.035,-.015,-.09)).normalized()
grip_normal=Vector((-.93,0,-.36));grip_normal=(grip_normal-grip_axis*grip_normal.dot(grip_axis)).normalized()
grip_width=grip_normal.cross(grip_axis)
for vertex in body.data.vertices:
    v=vertex.co
    if v.x>.25 and v.z<1.02:
        offset=v-grip_wrist
        t=offset.dot(grip_axis);width=offset.dot(grip_width);normal=offset.dot(grip_normal)
        if width>.10 and t<.14:
            weight=min(1,max(0,(width-.095)/.025));weight=weight*weight*(3-2*weight)
            angle=1.8
            b=width-.09;n=normal-.01
            width=width*(1-weight)+(.09+b*math.cos(angle)-n*math.sin(angle))*weight
            normal=normal*(1-weight)+(.01+b*math.sin(angle)+n*math.cos(angle))*weight
            t-=.030*weight
        elif t>.110:
            distance=t-.110;radius=.028;angle=distance/radius
            # Remove the original relaxed finger curve before applying the grasp arc.
            thickness=normal-(.005+.50*distance)
            t=.110+(radius-thickness)*math.sin(angle)
            normal=.005+radius-(radius-thickness)*math.cos(angle)
        vertex.co=grip_wrist+grip_axis*t+grip_width*width+grip_normal*normal
        if offset.dot(grip_axis)>.035:
            for group in body.vertex_groups:group.remove([vertex.index])
            body.vertex_groups['hand.L'].add([vertex.index],1,'REPLACE')
# Transfer the body's skin weights so fabric follows precisely the same deformation.
tree=KDTree(len(body.data.vertices))
for v in body.data.vertices:tree.insert(v.co,v.index)
tree.balance()
for cloth in clothing:
    cloth.parent=rig
    for group in list(cloth.vertex_groups):cloth.vertex_groups.remove(group)
    for group in body.vertex_groups:cloth.vertex_groups.new(name=group.name)
    for vertex in cloth.data.vertices:
        _,nearest,_=tree.find(vertex.co)
        for weight in body.data.vertices[nearest].groups:
            cloth.vertex_groups[weight.group].add([vertex.index],weight.weight,'REPLACE')
    mod=cloth.modifiers.new('Shared athlete skin','ARMATURE');mod.object=rig
    subcloth=cloth.modifiers.new('Fabric smoothing','SUBSURF');subcloth.levels=1;subcloth.render_levels=1
    solid=cloth.modifiers.new('Fabric thickness','SOLIDIFY');solid.thickness=.006;solid.offset=1

# Eyes follow the skull rigidly; preserve original local placement.
for eye in dst.objects[1:]:
    eye.data.materials.clear();eye.data.materials.append(white)
    group=eye.vertex_groups.new(name='head');group.add(list(range(len(eye.data.vertices))),1,'REPLACE')
    mod=eye.modifiers.new('Head skin','ARMATURE');mod.object=rig;eye.parent=rig
    for p in eye.data.polygons:p.use_smooth=True
# A subdivision level for the editable source; export the mobile base topology.
sub=body.modifiers.new('Preview skin smoothing','SUBSURF');sub.levels=1;sub.render_levels=1
rig.rotation_euler=(math.radians(-90),0,0);rig.location=(0,-1.0,.15)
rig.show_in_front=True
bpy.context.view_layer.update()
# Save the reusable rig in neutral pose before authoring the floor exercise.
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'source/character.blend'))

def set_bone(name,head,tail):
    b=arm.bones[name];pb=rig.pose.bones[name];head=Vector(head);tail=Vector(tail)
    rot=(b.tail_local-b.head_local).rotation_difference(tail-head).to_matrix() @ b.matrix_local.to_3x3()
    pb.matrix=Matrix.Translation(head) @ rot.to_4x4()
    bpy.context.view_layer.update()

def toward(head,direction,length):return Vector(head)+Vector(direction).normalized()*length

def sphere(name,loc,scale,mat):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=32,ring_count=16,location=loc)
    o=bpy.context.object;o.name=name;o.scale=scale;bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);o.data.materials.append(mat)
    for p in o.data.polygons:p.use_smooth=True
    return o

def bevel_cube(name,loc,scale,mat,bevel=.04):
    bpy.ops.mesh.primitive_cube_add(size=1,location=loc);o=bpy.context.object;o.name=name;o.scale=scale;bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    o.data.materials.append(mat);mod=o.modifiers.new('Soft edges','BEVEL');mod.width=bevel;mod.segments=4
    o.modifiers.new('Weighted normals','WEIGHTED_NORMAL');return o
iron=material('Powder-coated cast iron',(.027,.035,.041),.42,.65)
props=[]
for side,sign in [('L',1)]:
    parent=bpy.data.objects.new('Kettlebell '+side,None);bpy.context.collection.objects.link(parent)
    bell=sphere('Cast bell '+side,(0,0,0),(.078,.071,.075),iron);bell.parent=parent
    # Flat bottom and a broad cast handle with a clear horizontal grip.
    curve=bpy.data.curves.new('Cast handle '+side,'CURVE');curve.dimensions='3D';curve.bevel_depth=.013;curve.bevel_resolution=3
    spline=curve.splines.new('BEZIER');spline.bezier_points.add(4)
    for p,co in zip(spline.bezier_points,[(-.054,0,.045),(-.069,0,.11),(0,0,.139),(.069,0,.11),(.054,0,.045)]):
        p.co=co;p.handle_left_type='AUTO';p.handle_right_type='AUTO'
    handle=bpy.data.objects.new('Steel handle '+side,curve);bpy.context.collection.objects.link(handle);handle.data.materials.append(iron);handle.parent=parent
    band=bevel_cube('Lime equipment badge '+side,(0,-.070,0),(.055,.006,.019),lime,.005);band.parent=parent
    props.append((parent,sign))
mat=material('Mat • charcoal foam',(.033,.045,.048),.96)
bevel_cube('Training mat',(0,-.20,.006),(1.35,2.1,.032),mat,.025)
bevel_cube('Mat edge accent',(0,-1.235,.014),(1.26,.014,.010),lime,.003)
scene=bpy.context.scene;scene.frame_start=1;scene.frame_end=121;scene.render.fps=30
# Keep the complete foot surface in its upright rest orientation, with sole on mat.
# Bone endpoints alone do not guarantee sole contact after skinning.
mat_top=.022
foot_targets={}
for side,sign in [('L',1),('R',-1)]:
    sole=min(v.co.z for v in body.data.vertices if v.co.z<.13 and v.co.x*sign>0)
    rest_ankle=arm.bones['foot.'+side].head_local
    ankle_world=Vector((sign*.17,-.70,rest_ankle.z + mat_top-sole))
    foot_targets[side]=(ankle_world, Matrix.Translation(ankle_world-rest_ankle))
# Map the resting hand's anatomical palm normal down toward the mat.
rest_wrist=arm.bones['hand.R'].head_local
hand_axis=Vector((-.035,-.015,-.09)).normalized()
palm_normal=Vector((.93,0,-.36));palm_normal=(palm_normal-hand_axis*palm_normal.dot(hand_axis)).normalized()
# Uncurl the relaxed fingers so the palm and finger pads can share the mat plane.
for vertex in body.data.vertices:
    t=(vertex.co-rest_wrist).dot(hand_axis)
    if vertex.co.x<-.25 and vertex.co.z<1.02 and t>.10:
        vertex.co-=palm_normal*(.50*(t-.10))
hand_width=palm_normal.cross(hand_axis).normalized()
local_basis=Matrix((hand_axis,hand_width,palm_normal)).transposed()
world_axis=Vector((-.10,-1,0)).normalized();world_normal=Vector((0,0,-1))
world_basis=Matrix((world_axis,world_normal.cross(world_axis),world_normal)).transposed()
rest_hand_rotation=world_basis @ local_basis.transposed()
hand_indices=[v.index for v in body.data.vertices if v.co.x<-.25 and (v.co-rest_wrist).dot(hand_axis)>.025]
sole=min((rest_hand_rotation @ (body.data.vertices[i].co-rest_wrist)).z for i in hand_indices)
rest_wrist_world=Vector((-.49,.10,mat_top-sole))
rest_hand_transform=Matrix.Translation(rest_wrist_world) @ rest_hand_rotation.to_4x4() @ Matrix.Translation(-rest_wrist)
# Rigid palm/finger weights preserve contact instead of inheriting forearm twist.
for i in hand_indices:
    for group in body.vertex_groups:group.remove([i])
    body.vertex_groups['hand.R'].add([i],1,'REPLACE')
# 4-second loop: press 1s, settle .3s, controlled return 2s, floor pause .7s.
for frame in range(1,122):
    t=(frame-1)/30
    if t<1: a=.5-.5*math.cos(math.pi*t)
    elif t<1.3:a=1
    elif t<3.3:a=.5+.5*math.cos(math.pi*(t-1.3)/2)
    else:a=0
    set_bone('pelvis',(0,.028,.9),(0,.012,1.07))
    set_bone('spine',(0,.012,1.07),(0,.018,1.28))
    set_bone('chest',(0,.018,1.28),(0,0,1.47))
    set_bone('neck',(0,0,1.47),(0,.05,1.58))
    set_bone('head',(0,.05,1.58),(0,.07,1.77))
    for side,sign in [('L',1),('R',-1)]:
        hip=Vector((sign*.1,.028,.95))
        ankle=rig.matrix_world.inverted() @ foot_targets[side][0]
        direction=(ankle-hip).normalized();distance=(ankle-hip).length
        thigh=arm.bones['thigh.'+side].length;shin=arm.bones['shin.'+side].length
        along=(thigh*thigh-shin*shin+distance*distance)/(2*distance)
        pole=Vector((0,-1,0));bend=(pole-direction*pole.dot(direction)).normalized()
        knee=hip+direction*along+bend*math.sqrt(max(0,thigh*thigh-along*along))
        set_bone('thigh.'+side,hip,knee);set_bone('shin.'+side,knee,ankle)
        rig.pose.bones['foot.'+side].matrix=(rig.matrix_world.inverted() @ foot_targets[side][1] @ arm.bones['foot.'+side].matrix_local)
        bpy.context.view_layer.update()
        shoulder=Vector((sign*.19,0,1.44))
        if side == 'L':
            low=Vector((sign*.17,.061,-.195)).normalized();high=Vector((sign*.025,-1,-.025)).normalized()
            upper=(low*(1-a)+high*a).normalized()
            elbow=shoulder+upper*arm.bones['upper_arm.'+side].length
            wrist=elbow+Vector((0,-arm.bones['forearm.'+side].length,0))
            hand=wrist+Vector((0,-arm.bones['hand.'+side].length,0))
        else:
            # Solve a relaxed elbow position with the whole palm facing the mat.
            shoulder_world=rig.matrix_world @ shoulder
            elbow_z=.075
            upper_length=arm.bones['upper_arm.R'].length
            fore_length=arm.bones['forearm.R'].length
            r1=math.sqrt(upper_length**2-(elbow_z-shoulder_world.z)**2)
            r2=math.sqrt(fore_length**2-(elbow_z-rest_wrist_world.z)**2)
            delta=rest_wrist_world-shoulder_world;delta.z=0
            distance=delta.length;direction=delta.normalized()
            along=(r1*r1-r2*r2+distance*distance)/(2*distance)
            perpendicular=Vector((direction.y,-direction.x,0))
            elbow_world=shoulder_world+direction*along+perpendicular*math.sqrt(max(0,r1*r1-along*along))
            elbow_world.z=elbow_z
            elbow=rig.matrix_world.inverted() @ elbow_world
            wrist=rig.matrix_world.inverted() @ rest_wrist_world
            hand=wrist+Vector((0,0,-arm.bones['hand.R'].length))
        set_bone('upper_arm.'+side,shoulder,elbow);set_bone('forearm.'+side,elbow,wrist);set_bone('hand.'+side,wrist,hand)
        if side == 'R':
            rig.pose.bones['hand.R'].matrix=rig.matrix_world.inverted() @ rest_hand_transform @ arm.bones['hand.R'].matrix_local
            bpy.context.view_layer.update()
        if side == 'L':
            prop=props[0][0]
            palm=grip_wrist+grip_axis*.110+grip_width*.050+grip_normal*.033
            grip_world=rig.matrix_world @ rig.pose.bones['hand.'+side].matrix @ arm.bones['hand.'+side].matrix_local.inverted() @ palm
            # Compose world-Y tilt AFTER handle rotation: the cast body sits laterally
            # outside the working forearm, not toward the chest or inside the wrist.
            prop.rotation_mode='QUATERNION'
            prop.rotation_quaternion=(Matrix.Rotation(-.95,4,'Y') @ Matrix.Rotation(math.pi/2,4,'Z')).to_quaternion()
            prop.location=grip_world-prop.rotation_quaternion @ Vector((0,0,.139))
            prop.keyframe_insert('location',frame=frame)
    for pb in rig.pose.bones:
        pb.rotation_mode='QUATERNION';pb.keyframe_insert('location',frame=frame);pb.keyframe_insert('rotation_quaternion',frame=frame);pb.keyframe_insert('scale',frame=frame)
rig.animation_data.action.name='rep'
# Make the baked animation linear between dense samples, preserving smooth timing.
for obj in [rig]+[p[0] for p in props]:
    if obj.animation_data and obj.animation_data.action:
        for curve in obj.animation_data.action.fcurves:
            for key in curve.keyframe_points:key.interpolation='LINEAR'
# Studio presentation.
floor=material('Studio floor',(.016,.024,.028),.92)
bevel_cube('Studio ground',(0,0,-.06),(200,200,.07),floor,.005)
world=bpy.data.worlds.new('Soft studio');scene.world=world;world.use_nodes=True;world.node_tree.nodes['Background'].inputs[0].default_value=(.13,.17,.20,1);world.node_tree.nodes['Background'].inputs[1].default_value=.35

def area(name,loc,power,size,color):
    data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=size;data.color=color
    o=bpy.data.objects.new(name,data);bpy.context.collection.objects.link(o);o.location=loc;o.rotation_euler=(Vector((0,-.1,.2))-o.location).to_track_quat('-Z','Y').to_euler()
area('Large warm key',(1,-1,4),450,4,(1,.88,.76));area('Cool fill',(-2,-.3,2),300,3,(.67,.82,1));area('Rim',(0,2,3),500,3,(.86,1,.69))
cam_data=bpy.data.cameras.new('Three-quarter camera');camera=bpy.data.objects.new('Three-quarter camera',cam_data);bpy.context.collection.objects.link(camera)
camera.location=(2.6,-3.7,2.7);target=Vector((0,-.15,.22));camera.rotation_euler=(target-camera.location).to_track_quat('-Z','Y').to_euler();cam_data.type='ORTHO';cam_data.ortho_scale=2.65;scene.camera=camera
scene.name='rep'
scene.render.engine='CYCLES';scene.cycles.samples=24;scene.cycles.use_denoising=True
scene.render.resolution_x=1000;scene.render.resolution_y=760;scene.render.resolution_percentage=100
scene.view_settings.view_transform='AgX'
scene.frame_set(1)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'source/floor_press.blend'))
scene.render.filepath=str(OUT/'previews/floor_press_start.png');bpy.ops.render.render(write_still=True)
scene.frame_set(36);scene.render.filepath=str(OUT/'previews/floor_press_top.png');bpy.ops.render.render(write_still=True)
# Side-view contact check before exporting.
hero_location=camera.location.copy();hero_rotation=camera.rotation_euler.copy()
camera.location=(3,-.15,.95);camera.rotation_euler=(Vector((0,-.15,.28))-camera.location).to_track_quat('-Z','Y').to_euler()
scene.frame_set(1);scene.render.filepath=str(OUT/'previews/floor_press_side_start.png');bpy.ops.render.render(write_still=True)
camera.location=hero_location;camera.rotation_euler=hero_rotation
# Exclude studio lighting/camera/floor; keep athlete, equipment, mat.
bpy.ops.object.select_all(action='DESELECT')
for o in scene.objects:
    if o.type in {'MESH','ARMATURE','CURVE','EMPTY'} and o.name!='Studio ground':o.select_set(True)
for obj in scene.objects:
    for modifier in obj.modifiers:
        if modifier.type=='SUBSURF': modifier.show_viewport=False;modifier.show_render=False
runtime=ROOT/'app/src/main/assets/training3d';runtime.mkdir(parents=True,exist_ok=True)
bpy.ops.export_scene.gltf(filepath=str(runtime/'floor_press.glb'),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='SCENE',export_frame_range=True,export_force_sampling=True,export_apply=True)
sys.path.insert(0,str(ROOT/'tools'))
from pack_training_glb import merge_animations
merge_animations(runtime/'floor_press.glb')
print('FOOTYOS_PROTOTYPE_COMPLETE')
