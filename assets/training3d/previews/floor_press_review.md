# Floor press — milestone 2 prototype review

## Delivered

- Editable character and exercise scenes: `assets/training3d/source/character.blend` and `floor_press.blend`.
- Reproducible Blender scripts: `build_floor_press.py` and `render_floor_press.py` in the same source folder.
- Runtime asset: `app/src/main/assets/training3d/floor_press.glb`.
- Front, side and three-quarter PNGs; four-second MP4 and looping GIF previews.
- Asset provenance: `assets/training3d/licenses/blender-human-base-meshes.txt`.

Blender 4.5.9 LTS was run from the official signed DMG mounted at `/tmp/footyos-blender-mount`. The downloaded runtime and original model archive are temporary, not bundled into the Android app. Editable .blend files contain the required geometry, rig, materials and animation.

## Visual review performed by Codex

Inspected the three-quarter start/top poses, front top view, and side start pose. The prototype has a continuous anatomical mesh, smooth skeletal deformation, fitted clothing shells, one kettlebell attached at the working palm, with its body outside the forearm, planted feet, settled hips/head, and upper arms returning to the mat. The front and side views were used to correct the original hand placement, floating head/hips and clothing intersections.

This is a visual prototype for product review. The finger curl is a sculpted grip, not a complete articulated finger rig; further close-up hand polish remains possible. It has not received a human coach's movement-quality sign-off. No automatic rep counting is inferred from playback.

## Technical checks

`python3 tools/check_floor_press.py` verifies the GLB's embedded resources, skinned mesh, named `rep` clip, and equality of every animation channel's first/last sample (including quaternion sign equivalence). Measurements are saved in `floor_press_validation.json`.

- 1,655,184 bytes (about 1.58 MiB), below the 8 MiB asset budget.
- 46,122 triangles. This exceeds the initial 40k target; profile/optimize the character/eyes during native integration before copying this asset across a whole library.
- One skinned skeleton, one combined `rep` clip, 58 synchronized channels.
- 121 baked samples spanning 4 seconds at 30 fps. Blender's exported timestamps start at frame 1 / 30 and end at 121 / 30, so the GLB timeline ends at approximately 4.033 seconds.
- No detected differences between the first and last values of any animated channel.

The rendered preview samples every second frame at 15 fps, so it plays the intended four-second cycle. The playback clock never determines the user's actual exercise speed.

## Remaining milestone

Native interactive playback is implemented for this exercise with orbit/presets, speed controls and pause/resume. Hardware device profiling and the other eleven exercises remain pending.

## Corrected variation

The user specified a single-arm press with the kettlebell outside the working forearm. The earlier bilateral version was incorrect and is superseded. The corrected prototype animates the left arm only and keeps the non-working right arm stationary, with exactly one kettlebell. The bell rotation is composed in world space so its mass stays lateral to the arm rather than over the chest. `floor_press_pose_validation.json` records checks over all 121 frames: one bell, a static resting arm, and a positive outside offset relative to the working wrist.

The exercise catalog now prescribes 3 × 8–12 / side; its guided plan has two sides per set. This prototype's `rep` clip demonstrates the left side only. Produce the mirrored right-side clip during native integration before mapping this asset to both sides of the guided session.

## Foot contact and bracing correction

The user identified lifted soles in the interactive model. The rig now uses explicit world-space foot transforms and rigid sole weights, with both ankles anchored over the mat. The torso uses a stable rib/pelvis posture with reduced arch. `check_floor_press_contacts.py` checks evaluated mesh heel/forefoot heights and pelvis/spine/chest/foot transforms over all 121 frames. Heel clearance is 2.12 mm and forefoot clearance is 0.08 mm above the nominal mat top; transform drift is zero. The corrected side render was visually inspected. The coach cue now instructs planted feet, braced core, ribs down and still hips. Muscle tension itself cannot be measured from an animated surface.

## Support palm and closed grip correction

The right hand is oriented using an anatomical palm normal mapped to world-down, with the upper arm/forearm solved to a stationary support pose. The relaxed fingers are uncurled and palm weights held rigid. The evaluated palm surface is at the 0.022 m mat top across the loop. The left fingers bend from the knuckles with preserved cross-section thickness, and the thumb opposes the fingers around the handle. The handle is aligned to that grip center rather than the old open-palm position.

Close-up evidence: `floor_press_grip_closeup.png` and `floor_press_support_palm_closeup.png`. `source/check_floor_press_hands.py` verifies palm-down alignment, palm height/stability and grip-to-handle attachment over 121 frames. These technical checks supplement visual inspection; they do not measure grip force.
