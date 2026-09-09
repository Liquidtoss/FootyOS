"""Author the remaining exercise loops on the shared Blender athlete.
Blender -b --python assets/training3d/source/build_exercise_library.py -- row overhead_press
Omit IDs for all eleven. Sources, contact measurements and review frames are saved per ID.
"""
import bpy, math, sys, json
from pathlib import Path
from mathutils import Vector, Matrix
ROOT=Path(__file__).resolve().parents[3]
OUT=ROOT/'assets/training3d'; RUNTIME=ROOT/'app/src/main/assets/training3d'
sys.path.insert(0,str(ROOT/'tools'))
from pack_training_glb import merge_animations
IDS=['row','overhead_press','suitcase_carry','swing','goblet_squat','split_squat','single_leg_rdl','calf_raise','tibialis_raise','copenhagen','hamstring_slider']
UNILATERAL={'row','overhead_press','suitcase_carry','split_squat','single_leg_rdl','copenhagen'}
V=Vector
I=Matrix.Identity(4)
def rx(a):return Matrix.Rotation(a,4,'X')
def rz(a):return Matrix.Rotation(a,4,'Z')
def translate(p):return Matrix.Translation(V(p))
def smooth(t):return t*t*(3-2*t)

def material(name,color,rough=.7,metal=0):
    m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);m.use_nodes=True
    b=m.node_tree.nodes.get('Principled BSDF');b.inputs['Base Color'].default_value=(*color,1);b.inputs['Roughness'].default_value=rough;b.inputs['Metallic'].default_value=metal
    return m

def cube(name,center,size,mat):
    bpy.ops.mesh.primitive_cube_add(size=1,location=center);o=bpy.context.object;o.name=name;o.scale=size
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);o.data.materials.append(mat)
    b=o.modifiers.new('Rounded edges','BEVEL');b.width=.018;b.segments=3;o.modifiers.new('Smooth normals','WEIGHTED_NORMAL')
    return o

def bell(name,mat,lime,wide=False):
    parent=bpy.data.objects.new(name,None);bpy.context.collection.objects.link(parent)
    bpy.ops.mesh.primitive_uv_sphere_add(segments=28,ring_count=14);o=bpy.context.object;o.name='Cast iron body';o.scale=(.087,.078,.085);bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);o.data.materials.append(mat);o.parent=parent
    for p in o.data.polygons:p.use_smooth=True
    c=bpy.data.curves.new('Kettlebell handle','CURVE');c.dimensions='3D';c.bevel_depth=.014;c.bevel_resolution=3
    sp=c.splines.new('BEZIER');sp.bezier_points.add(4);width=.13 if wide else .075
    for p,co in zip(sp.bezier_points,[(-.055,0,.055),(-width,0,.115),(0,0,.151),(width,0,.115),(.055,0,.055)]):p.co=co;p.handle_left_type='AUTO';p.handle_right_type='AUTO'
    h=bpy.data.objects.new('Cast handle',c);bpy.context.collection.objects.link(h);h.data.materials.append(mat);h.parent=parent
    badge=cube('Lime badge',(0,-.079,0),(.05,.004,.02),lime);badge.parent=parent
    return parent

# Preserve original hand coordinates once so every asset can choose a closed or open hand.
hand_file=OUT/'source/neutral_hands.json'
if not hand_file.exists():
    with bpy.data.libraries.load('/tmp/footyos-human/human_base_meshes_bundle.blend') as (src,dst):dst.objects=['GEO-body_male_realistic']
    mesh=dst.objects[0]
    hand_file.write_text(json.dumps({str(v.index):list(v.co) for v in mesh.data.vertices if abs(v.co.x)>.25 and v.co.z<1.02},separators=(',',':')))
neutral_hands=json.loads(hand_file.read_text())

def build(exercise):
    bpy.ops.wm.open_mainfile(filepath=str(OUT/'source/character.blend'))
    bpy.context.preferences.filepaths.save_version=0
    rig=bpy.data.objects['Athlete rig'];body=bpy.data.objects['Athlete • continuous skinned mesh'];arm=rig.data
    rig.animation_data_clear();rig.matrix_world=I
    for pb in rig.pose.bones:pb.matrix_basis=I
    for obj in bpy.context.scene.objects:obj.animation_data_clear()
    bpy.context.view_layer.update()
    closed={'L'} if exercise in {'row','overhead_press','suitcase_carry','single_leg_rdl'} else {'L','R'} if exercise in {'swing','goblet_squat'} else set()
    grip_data={};palm_data={}
    for side,sign in [('L',1),('R',-1)]:
        w=V((sign*.4,-.01,.99));axis=V((sign*.035,-.015,-.09)).normalized()
        normal=V((-sign*.93,0,-.36));normal=(normal-axis*normal.dot(axis)).normalized();width=normal.cross(axis)
        # Mirrored width coordinates are negated so the same sculpt works on both hands.
        for index,co in neutral_hands.items():
            v=body.data.vertices[int(index)]
            if co[0]*sign<=0:continue
            v.co=co;off=v.co-w;t=off.dot(axis);b=off.dot(width)*sign;n=off.dot(normal)
            if side in closed:
                if b>.10 and t<.14:
                    weight=smooth(min(1,max(0,(b-.095)/.025)));angle=1.8
                    bb=b-.09;nn=n-.01
                    b=b*(1-weight)+(.09+bb*math.cos(angle)-nn*math.sin(angle))*weight
                    n=n*(1-weight)+(.01+bb*math.sin(angle)+nn*math.cos(angle))*weight;t-=.03*weight
                elif t>.110:
                    distance=t-.110;radius=.028;angle=distance/radius;thickness=n-(.005+.5*distance)
                    t=.110+(radius-thickness)*math.sin(angle);n=.005+radius-(radius-thickness)*math.cos(angle)
            elif exercise in {'hamstring_slider','copenhagen'} and t>.10:n-=.5*(t-.10)
            v.co=w+axis*t+width*b*sign+normal*n
            if off.dot(axis)>.035:
                for g in body.vertex_groups:g.remove([v.index])
                body.vertex_groups['hand.'+side].add([v.index],1,'REPLACE')
        grip_data[side]=(w,axis,normal,width,w+axis*.110+width*.050*sign+normal*.033)
        palm_data[side]=[v.index for v in body.data.vertices if v.co.x*sign>.25 and .04<(v.co-w).dot(axis)<.10]
    mat=material('Charcoal equipment',(.033,.045,.048),.94);lime=material('FootyOS lime',(.64,.88,.085));iron=material('Cast iron',(.027,.035,.041),.42,.65)
    floor_z=.022
    cube('Training mat',(0,0,.006),(3.4,3.4,.032) if exercise=='suitcase_carry' else (1.6,2.8,.032),mat)
    equipment=[]
    if exercise=='split_squat':equipment.append(cube('Rear-foot bench',(0,.59,.18),(.8,.42,.36),mat))
    if exercise=='tibialis_raise':equipment.append(cube('Support wall',(0,.32,.95),(1.05,.10,1.9),mat))
    if exercise=='copenhagen':equipment.append(cube('Padded knee support',(.44,.17,.27),(.38,.65,.54),mat))
    sliders=[]
    if exercise=='hamstring_slider':
        for sign in [1,-1]:sliders.append(cube('Heel slider',(sign*.17,-.7,.03),(.19,.22,.016),lime))
    prop=bell('Kettlebell',iron,lime,exercise=='swing') if closed else None
    bones=rig.pose.bones
    def setbone(name,head,tail):
        head=V(head);tail=V(tail);b=arm.bones[name]
        rotation=(b.tail_local-b.head_local).rotation_difference(tail-head).to_matrix() @ b.matrix_local.to_3x3()
        bones[name].matrix=translate(head) @ rotation.to_4x4();bpy.context.view_layer.update()
    def bone_transform(name,transform):
        bones[name].matrix=transform @ arm.bones[name].matrix_local;bpy.context.view_layer.update()
    def ik(head,end,l1,l2,pole):
        head=V(head);end=V(end);delta=end-head;distance=delta.length
        if distance>l1+l2+.004:raise ValueError(f'{exercise}: unreachable limb {distance:.3f} > {l1+l2:.3f}')
        distance=min(distance,l1+l2-.00001);direction=delta.normalized();along=(l1*l1-l2*l2+distance*distance)/(2*distance)
        p=V(pole);p=(p-direction*p.dot(direction)).normalized()
        return head+direction*along+p*math.sqrt(max(0,l1*l1-along*along))
    def chain(side,kind,head,end,pole):
        names=('thigh.','shin.') if kind=='leg' else ('upper_arm.','forearm.')
        joint=ik(head,end,arm.bones[names[0]+side].length,arm.bones[names[1]+side].length,pole)
        setbone(names[0]+side,head,joint);setbone(names[1]+side,joint,end);return joint
    def hand_rotation(side,direction,normal):
        _,a,n,b,_=grip_data[side];d=V(direction).normalized();nn=V(normal);nn=(nn-d*nn.dot(d)).normalized()
        return Matrix((d,nn.cross(d),nn)).transposed() @ Matrix((a,b,n)).transposed().transposed()
    def hand(side,wrist,direction,normal):
        w,a,n,b,g=grip_data[side];rotation=hand_rotation(side,direction,normal)
        tr=translate(wrist) @ rotation.to_4x4() @ translate(-w);bone_transform('hand.'+side,tr)
        return tr @ g
    def wrist_for_grip(side,target,direction,normal):
        w,_,_,_,g=grip_data[side];return V(target)-hand_rotation(side,direction,normal) @ (g-w)
    feet={}
    for side,sign in [('L',1),('R',-1)]:
        foot_ids=[v.index for v in body.data.vertices if v.co.z<.13 and v.co.x*sign>0]
        sole=min(body.data.vertices[i].co.z for i in foot_ids)
        feet[side]=(V((sign*.15,0,.105)),sole,foot_ids)
    def foot(side,ankle,rotation=None):
        r=rotation or I;rest,_,_=feet[side];tr=translate(ankle) @ r @ translate(-rest)
        bone_transform('foot.'+side,tr);return tr
    def grounded_foot(side,x,y,angle=0,pivot_y=None,height=floor_z,heading=None):
        rest,sole,indices=feet[side];r=rx(angle);pivot=V((rest.x,pivot_y if pivot_y is not None else 0,sole))
        tr=translate((x,y,height)) @ (heading or I) @ r @ translate(-pivot)
        lowest=min((tr @ body.data.vertices[i].co).z for i in indices)
        tr.translation.z+=height-lowest
        bone_transform('foot.'+side,tr)
        return tr @ rest,tr
    scene=bpy.context.scene;scene.frame_start=1;scene.render.fps=30
    duration=8 if exercise=='suitcase_carry' else 2 if exercise=='swing' else 4
    end=duration*30+1;scene.frame_end=end
    foot_contacts=[];limb_errors=[];grips=[]
    for frame in range(1,end+1):
        phase=(frame-1)/(end-1);a=(1-math.cos(2*math.pi*phase))/2
        hip=V((0,0,.89));torso_rotation=I;foot_positions={};foot_transforms={}
        if exercise=='row':hip=V((0,.13,.76));torso_rotation=rx(.9)
        if exercise=='swing':hip=V((0,.20*(1-a),.89-.17*(1-a)));torso_rotation=rx(.92*(1-a))
        if exercise=='goblet_squat':hip=V((0,.11*a,.89-.44*a));torso_rotation=rx(.30*a)
        if exercise=='split_squat':hip=V((0,.00,.78-.24*a));torso_rotation=rx(.12)
        if exercise=='single_leg_rdl':hip=V((.14,.12*a,.88-.08*a));torso_rotation=rx(1.16*a)
        if exercise=='calf_raise':hip.z=.89+.096*a
        if exercise=='tibialis_raise':hip=V((0,.12,.80));torso_rotation=rx(-.10)
        if exercise=='suitcase_carry':
            theta=2*math.pi*phase;hip=V((.5*math.cos(theta),.5*math.sin(theta),.86+.006*math.sin(4*math.pi*phase*6)))
            torso_rotation=rz(theta+math.pi)
        if exercise=='hamstring_slider':
            shoulder=V((0,.56,.16));axis=V((0,.95,-.31-.08*a)).normalized()
            torso_rotation=rx(-math.atan2(axis.y,axis.z));hip=shoulder-axis*.54
        if exercise=='copenhagen':hip=V((0,0,.52));torso_rotation=Matrix.Rotation(-math.pi/2,4,'Y')
        torso=translate(hip) @ torso_rotation @ translate((0,0,-.9))
        for name in ['pelvis','spine','chest','neck','head','clavicle.L','clavicle.R']:bone_transform(name,torso)
        if exercise=='copenhagen':
            # Timed hold: subtle ribcage breathing, without repeatedly dropping the hips.
            bones['chest'].scale.x*=1+.006*math.sin(2*math.pi*phase);bpy.context.view_layer.update()
        for side,sign in [('L',1),('R',-1)]:
            hj=torso @ V((sign*.1,0,.95));x=sign*.18;y=0;angle=0;pivot=0;height=floor_z;heading=None
            if exercise in {'row','swing','goblet_squat'}:x=sign*.23
            if exercise=='goblet_squat':heading=rz(sign*.15)
            if exercise=='row':y=.12 if side=='L' else -.22
            if exercise=='split_squat':
                y=-.35 if side=='L' else .56
                if side=='R':angle=.65;pivot=-.12;height=.36
            if exercise=='single_leg_rdl' and side=='R':
                ankle=hj+torso_rotation.to_3x3() @ V((0,0,-.80))
                chain(side,'leg',hj,ankle,torso_rotation.to_3x3() @ V((0,-1,0)));foot(side,ankle,torso_rotation);continue
            if exercise=='single_leg_rdl':x=.16
            if exercise=='calf_raise':angle=.55*a;pivot=-.16
            if exercise=='tibialis_raise':angle=-.35*a;pivot=.045;y=-.15
            if exercise=='suitcase_carry':
                g=phase*6+(0 if side=='L' else .5);step=math.floor(g);u=g-step
                def contact(k):
                    theta=2*math.pi*(k-(0 if side=='L' else .5))/6
                    root=V((.5*math.cos(theta),.5*math.sin(theta),0));rot=rz(theta+math.pi)
                    return root+rot.to_3x3() @ V((sign*.16,-.18,floor_z)),theta+math.pi
                p0,h0=contact(step);p1,h1=contact(step+1)
                v=0 if u<.60 else smooth((u-.60)/.40)
                p=p0.lerp(p1,v);p.z+=.065*math.sin(math.pi*v)
                x,y,height=p;heading=rz(h0+(h1-h0)*v)
            if exercise=='hamstring_slider':y=-.35-.35*a;angle=-.45;pivot=.04;height=.041
            if exercise=='copenhagen':
                if side=='L':
                    knee=hj+V((arm.bones['thigh.L'].length,0,0));ankle=knee+V((0,.36,-.19)).normalized()*arm.bones['shin.L'].length
                else:
                    knee=hj+V((.80,-.45,-.20)).normalized()*arm.bones['thigh.R'].length;ankle=knee+V((-.15,-.95,-.20)).normalized()*arm.bones['shin.R'].length
                setbone('thigh.'+side,hj,knee);setbone('shin.'+side,knee,ankle);foot(side,ankle,rx(-.4));continue
            ankle,tr=grounded_foot(side,x,y,angle,pivot,height,heading)
            foot_positions[side]=ankle;foot_transforms[side]=tr
            knee=chain(side,'leg',hj,ankle,(sign*.25 if exercise=='goblet_squat' else 0,-1,0) if exercise!='suitcase_carry' else torso_rotation.to_3x3() @ V((0,-1,0)))
            bone_transform('foot.'+side,tr)
            if exercise=='hamstring_slider':
                sliders[0 if side=='L' else 1].location=(x,y,.03)
            if angle==0 and exercise not in {'suitcase_carry'}:
                foot_contacts.append(min((tr @ body.data.vertices[i].co).z for i in feet[side][2])-height)
        shoulders={side:torso @ V((sign*.19,0,1.44)) for side,sign in [('L',1),('R',-1)]}
        if exercise=='overhead_press':
            sh=shoulders['L'];elbow=sh+V((.07+.08*(1-a),-.055,-.21*(1-a)+.23*a)).normalized()*arm.bones['upper_arm.L'].length
            wrist=elbow+V((0,0,arm.bones['forearm.L'].length));setbone('upper_arm.L',sh,elbow);setbone('forearm.L',elbow,wrist)
            g=hand('L',wrist,(0,0,1),(1,0,0));prop.matrix_world=translate(g) @ Matrix.Rotation(-.95,4,'Y') @ rz(math.pi/2) @ translate((0,0,-.151));grips.append((g-(prop.matrix_world @ V((0,0,.151)))).length)
        for side,sign in [('L',1),('R',-1)]:
            sh=shoulders[side];direction=V((0,0,-1));normal=V((-sign,0,0));wrist=sh+V((sign*.07,0,-.48));pole=(sign,-.2,0)
            if exercise=='overhead_press' and side=='L':continue
            if exercise=='row':
                if side=='L':wrist=sh+V((.035,.15*a,-.46+.25*a));pole=(.2,1,0)
                else:wrist=V((-.23,-.24,.66));direction=V((0,0,-1));normal=V((0,1,0))
            if exercise=='suitcase_carry':wrist=sh+torso_rotation.to_3x3() @ V((sign*.08, .07*math.sin(2*math.pi*phase*6)*(1 if side=='R' else .1),-.475));normal=torso_rotation.to_3x3() @ V((-sign,0,0))
            if exercise=='single_leg_rdl':wrist=sh+V((sign*.07,0,-.47))
            if exercise=='swing':
                # Long arms swing from the hinge to chest height; the hips lead the ascent.
                direction=V((0,-math.sin(1.45*a),-math.cos(1.45*a)))
                g=V((sign*.065,sh.y,sh.z))+direction*.595
                normal=V((0,-direction.z,direction.y));wrist=wrist_for_grip(side,g,direction,normal);pole=(0,0,-1)
                if side=='L':prop.matrix_world=translate((0,g.y,g.z)) @ rx(-1.45*a) @ translate((0,0,-.151))
            if exercise=='goblet_squat':
                center=torso @ V((0,-.26,1.22));g=center+V((sign*.075,0,.113))
                direction=V((-sign,0,0));normal=V((0,-1,0));wrist=wrist_for_grip(side,g,direction,normal);pole=(sign*.4,0,-1)
                if side=='L':prop.matrix_world=translate(center)
            if exercise=='split_squat':
                wrist=sh+torso_rotation.to_3x3() @ V((sign*.10,-.03,-.47))
            if exercise=='tibialis_raise':wrist=sh+V((sign*.055,0,-.475))
            if exercise=='hamstring_slider':
                wrist=V((sign*.43,.15,.049));direction=V((0,-1,0));normal=V((0,0,-1));pole=(sign,0,-1)
            if exercise=='copenhagen':
                if side=='R':
                    elbow=sh+V((0,0,-arm.bones['upper_arm.R'].length));wrist=elbow+V((0,-arm.bones['forearm.R'].length,0))
                    setbone('upper_arm.R',sh,elbow);setbone('forearm.R',elbow,wrist);hand('R',wrist,(0,-1,0),(0,0,-1));continue
                wrist=V((-.10,-.07,.65));direction=V((1,0,-.3));normal=V((0,0,-1));pole=(-.2,-1,1)
            chain(side,'arm',sh,wrist,pole);g=hand(side,wrist,direction,normal)
            if prop and side=='L' and exercise in {'row','suitcase_carry','single_leg_rdl'}:
                # Hanging bell: horizontal handle passes through the closed palm.
                heading=torso_rotation if exercise=='suitcase_carry' else I
                prop.matrix_world=translate(g) @ heading @ rz(math.pi/2) @ translate((0,0,-.151));grips.append((g-(prop.matrix_world @ V((0,0,.151)))).length)
        for pb in bones:
            pb.rotation_mode='QUATERNION';pb.keyframe_insert('location',frame=frame);pb.keyframe_insert('rotation_quaternion',frame=frame);pb.keyframe_insert('scale',frame=frame)
        for obj in ([prop] if prop else [])+sliders:
            obj.rotation_mode='QUATERNION'
            for path in ['location','rotation_quaternion','scale']:obj.keyframe_insert(path,frame=frame)
    for obj in scene.objects:
        if obj.animation_data and obj.animation_data.action:
            for curve in obj.animation_data.action.fcurves:
                for key in curve.keyframe_points:key.interpolation='LINEAR'
    scene.frame_set(1)
    # Consistent studio lighting and review cameras, excluded from the runtime asset.
    world=bpy.data.worlds.new('Studio');world.use_nodes=True;scene.world=world;world.node_tree.nodes['Background'].inputs[0].default_value=(.18,.22,.25,1);world.node_tree.nodes['Background'].inputs[1].default_value=.4
    for name,loc,power,size in [('Key',(2,-3,4),500,4),('Fill',(-3,-1,2),350,3),('Rim',(0,3,3),450,3)]:
        data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=size;o=bpy.data.objects.new(name,data);bpy.context.collection.objects.link(o);o.location=loc;o.rotation_euler=(V((0,0,.8))-o.location).to_track_quat('-Z','Y').to_euler()
    data=bpy.data.cameras.new('Review camera');camera=bpy.data.objects.new('Review camera',data);bpy.context.collection.objects.link(camera);scene.camera=camera
    target=V((0,0,.8));camera.location=(2.8,-4,2.5);camera.data.type='ORTHO';camera.data.ortho_scale=2.6
    if exercise in {'copenhagen','hamstring_slider'}:target.z=.35;camera.data.ortho_scale=2.5
    if exercise=='suitcase_carry':camera.data.ortho_scale=3.6
    camera.rotation_euler=(target-camera.location).to_track_quat('-Z','Y').to_euler()
    scene.render.engine='CYCLES';scene.cycles.samples=12;scene.cycles.use_denoising=True
    scene.render.resolution_x=720;scene.render.resolution_y=650;scene.render.resolution_percentage=100;scene.view_settings.view_transform='AgX'
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'source'/f'{exercise}.blend'))
    for frame,label in [(1,'start'),(int((end-1)/2)+1,'finish')]:
        scene.frame_set(frame);scene.render.filepath=str(OUT/'previews'/f'{exercise}_{label}.png');bpy.ops.render.render(write_still=True)
    scene.frame_set(1)
    for obj in scene.objects:
        for mod in obj.modifiers:
            if mod.type=='SUBSURF':mod.show_viewport=False;mod.show_render=False
    bpy.ops.object.select_all(action='DESELECT')
    for obj in scene.objects:
        if obj.type in {'MESH','ARMATURE','CURVE','EMPTY'}:obj.select_set(True)
    path=RUNTIME/f'{exercise}.glb'
    bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='SCENE',export_frame_range=True,export_force_sampling=True,export_apply=True)
    merge_animations(path)
    report={'exercise':exercise,'frames':end,'durationSeconds':duration,'maxFlatFootContactErrorMeters':max([abs(v) for v in foot_contacts] or [0]),'maxGripAttachmentErrorMeters':max(grips or [0]),'variant':'Left-side demonstration' if exercise in UNILATERAL else 'Bilateral demonstration'}
    (OUT/'previews'/f'{exercise}_validation.json').write_text(json.dumps(report,indent=2)+'\n')
    print('EXERCISE_COMPLETE',exercise,flush=True)

if __name__=='__main__':
    selected=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else IDS
    for exercise in selected:
        if exercise not in IDS:raise ValueError(exercise)
        build(exercise)
