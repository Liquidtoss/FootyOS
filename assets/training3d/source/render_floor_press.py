"""Render the saved prototype's side/front checks and a four-second looping sequence."""
import bpy, json
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[3]
OUT=ROOT/'assets/training3d/previews'
bpy.ops.wm.open_mainfile(filepath=str(ROOT/'assets/training3d/source/floor_press.blend'))
s=bpy.context.scene;camera=s.camera
# Validate this specific single-arm variation before spending time rendering.
rig=bpy.data.objects['Athlete rig'];bell=bpy.data.objects['Kettlebell L']
assert len([o for o in s.objects if o.name.startswith('Kettlebell ')]) == 1
outside=[];rest_reference=None
for frame in range(1,122):
    s.frame_set(frame)
    wrist=rig.matrix_world @ rig.pose.bones['hand.L'].matrix.translation
    outside.append(bell.location.x-wrist.x)
    resting=[value for name in ['upper_arm.R','forearm.R','hand.R'] for row in rig.pose.bones[name].matrix for value in row]
    if rest_reference is None:rest_reference=resting
    else:assert max(abs(a-b) for a,b in zip(rest_reference,resting))<1e-5
assert min(outside)>.05, 'Bell must stay outside the working forearm throughout the press'
(OUT/'floor_press_pose_validation.json').write_text(json.dumps({'workingArm':'left','kettlebellCount':1,'restingArmStatic':True,'minimumOutsideOffsetMeters':min(outside)},indent=2)+'\n')
hero_location=camera.location.copy();hero_rotation=camera.rotation_euler.copy()
s.cycles.samples=16
for name,location in [('side',(3,-.15,.95)),('front',(0,-3.5,1.7))]:
    camera.location=location;camera.rotation_euler=(Vector((0,-.15,.28))-camera.location).to_track_quat('-Z','Y').to_euler()
    camera.data.ortho_scale=2.5
    for frame,phase in [(1,'start'),(36,'top')]:
        s.frame_set(frame);s.render.filepath=str(OUT/f'floor_press_{name}_{phase}.png');bpy.ops.render.render(write_still=True)
camera.location=hero_location;camera.rotation_euler=hero_rotation;camera.data.ortho_scale=2.65
s.render.resolution_x=720;s.render.resolution_y=548;s.cycles.samples=12
s.frame_end=119;s.frame_step=2;s.render.image_settings.file_format='PNG'
frames=OUT/'frames';frames.mkdir(exist_ok=True)
s.render.filepath=str(frames/'rep_');bpy.ops.render.render(animation=True)
print('FLOOR_PRESS_PREVIEWS_COMPLETE')
# Encode the rendered frames through Blender's bundled FFmpeg (no extra dependency).
files=sorted(frames.glob('rep_*.png'))
video=bpy.data.scenes.new('Preview encode');bpy.context.window.scene=video
video.render.resolution_x=720;video.render.resolution_y=548;video.render.resolution_percentage=100
video.render.fps=15;video.frame_start=1;video.frame_end=len(files)
editor=video.sequence_editor_create()
strip=editor.strips.new_image('Floor press',filepath=str(files[0]),channel=1,frame_start=1)
for file in files[1:]:strip.elements.append(file.name)
strip.frame_final_duration=len(files)
video.render.image_settings.file_format='FFMPEG';video.render.ffmpeg.format='MPEG4';video.render.ffmpeg.codec='H264';video.render.ffmpeg.constant_rate_factor='HIGH'
video.render.filepath=str(OUT/'floor_press.mp4');video.view_settings.view_transform='Standard'
bpy.ops.render.render(animation=True,scene=video.name)
print('FLOOR_PRESS_VIDEO_COMPLETE')
