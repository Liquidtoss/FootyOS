# Interactive exercise library

All twelve catalog entries use the native SceneView player and bundled GLBs. Each new animation is authored on the existing continuous, skinned Blender character, with its own `rep` clip, source scene, start/finish review renders and validation report. Assets remain identified as prototypes; technical validation is not a human coaching sign-off.

## Demonstrated variations

| Exercise | Demonstration |
| --- | --- |
| Floor press | Existing single-arm press, outside bell, closed grip and resting support palm |
| Row | Single-arm row from a staggered hip hinge, with the free hand supported near the front thigh |
| Overhead press | Single-arm press from rack to overhead, with the bell outside the forearm |
| Suitcase carry | Single-sided load while walking a closed circular path; stance feet stay in world position between steps |
| Swing | Two-hand hip hinge to chest-height swing; long arms and a wide handle |
| Goblet squat | Bell held close with both hands; bent knees track toward the feet |
| Bulgarian split squat | Bodyweight rear-foot-elevated split squat with a bench |
| Single-leg RDL | Same-side kettlebell and stance leg, free leg reaches backward with the hinge |
| Calf raise | Bilateral heel raise with forefoot support |
| Tibialis raise | Wall-supported forefoot raise with planted heels |
| Copenhagen | Knee-supported isometric side-plank variation, with subtle ribcage breathing rather than repeated hip drops |
| Hamstring slider | Bridged heel-slider curl and controlled extension |

Unilateral clips demonstrate one side. The viewer explicitly asks users to repeat on the other side; it does not label a left-side asset as a right-side clip. Mirrored, separately authored side clips remain a possible future extension.

## Playback

Drag to orbit, pinch to zoom, switch front/side/reset views, pause, and choose 0.5× or 1× speed. The guided preview loops before starting and during unpaused preparation, work and rest. Workout pauses and overlays pause the demonstration; the reference dialog has independent preview controls. The renderer pauses in the background. Demonstration motion does not count reps or advance workout timers.

Standing and walking assets use a larger scale than floor exercises so the character remains readable. Models load from the APK without downloading assets. A failed model load retains form cues and the reference-video link, with a retry control.

## Rebuilding and checking

Run Blender 4.5.9 with `assets/training3d/source/build_exercise_library.py -- <exercise_id> ...`. Each exercise exports independently, making a correction resumable without rebuilding the whole library. `character.blend` supplies the shared rig; `neutral_hands.json` retains the original CC0 hand coordinates so open and closed hand shapes can be selected per exercise. The existing floor-press builder stays separate.

Run `python3 tools/verify_training3d.py` for inventory/provenance and `python3 tools/check_training_library.py` for actual animated channels, matching loop endpoints, embedded resources, sizes and checksums. Per-exercise JSON reports record flat-foot and grip attachment measurements. Those are targeted checks, not exhaustive mesh collision detection.

Review evidence lives in `assets/training3d/previews/<id>_start.png` and `<id>_finish.png`. The floor press retains its existing detailed contact checks and close-ups. `ExerciseLibraryViewerTest` loads every asset in the emulator, compares rendered frames for visible motion, and exercises camera presets. The timed Copenhagen hold is excluded from large-pixel-motion assertions because its motion is intentionally limited to breathing.

## Movement reference notes

References checked while authoring on September 8, 2026:

- [ACE: getting started with kettlebells](https://www.acefitness.org/resources/pros/expert-articles/5269/how-to-get-started-with-kettlebells/) — swing and goblet-squat mechanics.
- [StrongFirst: mastering the press](https://www.strongfirst.com/mastering-your-press-part-ii/) — rack, wrist and grip position.
- [StrongFirst: loaded carries](https://www.strongfirst.com/two-important-carries-clinician/) — carry posture.
- [Human Kinetics: rear-foot-elevated split squat for soccer](https://us.humankinetics.com/blogs/excerpt/building-strength-for-soccer-with-the-rear-foot-elevated-split-squat) — supported rear-foot setup.
- [E3 Rehab: Copenhagen planks](https://e3rehab.com/how-to-perform-copenhagen-planks/) — knee-supported regression and timed holds.
- [Hawkes Physiotherapy: tibialis raises](https://hawkesphysiotherapy.co.uk/exercise/tibialis-raises/) — heel-supported forefoot motion.
- Existing exercise-specific reference links remain available in the app's form dialog for full setup and technique.

## Verification results

The debug build and unit tests passed. The existing Training flow and floor-press control tests passed. The full-library emulator test then passed on the API 36 FootyOS_Preview emulator, loading all twelve assets, observing visible motion for moving exercises, and changing camera presets. The initial narrow screenshot sample was replaced with a full viewport comparison over multiple animation phases to accommodate first-use rendering and pauses within the loop. All twelve GLBs passed motion, loop endpoint, checksum and embedded-resource checks. Total bundled GLB size is approximately 19 MiB.

### Refined runtime surfaces and camera framing

The runtime now retains one baked Catmull–Clark subdivision level (about
161,000–163,500 triangles per complete scene, previously 43,000–46,000).
`skin_detail.bin` contains the level-one Multires sculpt offsets from the same
Blender Studio CC0 realistic male asset linked in the manifest. This restores
source detail rather than replacing the athlete with an unrelated rig. Detail is
restricted to exposed skin to prevent the body protruding through clothing;
established finger and sole geometry is largely retained. Smooth wrist weights
replace the abrupt automatic-weight/rigid-hand boundary. Subdivision is applied
**before** the armature so existing motion and skinning stay live. The initial surface-only revision preserved every animation sampler byte. The
wrist correction below subsequently updates forearm roll and compensates hand
local transforms while preserving their world-space paths.

To refresh surfaces without reauthoring animation:

```
Blender -b --python assets/training3d/source/refresh_runtime_meshes.py
```

The authoring scripts call the same refinement function on export. Editable
`.blend` files retain the coarse cage. Regenerate sculpt offsets, if needed, with
`extract_skin_detail.py -- /path/to/human_base_meshes_bundle.blend` in Blender.
The source and CC0 details are in `licenses/blender-human-base-meshes.txt`.
Each runtime GLB remains below the 8 MiB budget (~5.2 MiB); the bundled library
increases from about 19 MiB to 63 MiB.

Camera presets now use the assets' original metre scale and target the athlete
at 1 m above the mat (0.45 m for floor exercises). Default, side, front, and reset
share this target; the carry has extra distance for its walking path. This avoids
framing the mat's origin or scaling from equipment-heavy bind-pose bounds.
The emulator library test captures each preset in its external `viewer-review`
folder for visual review in addition to checking actual rendered motion.

### Anatomical wrist correction

The broad wrist blend in the initial refinement let hand rotation bend 7.5 cm
of the forearm shaft. The export now keeps the shaft on the forearm bone and
limits the blend to a 3.5 cm wrist transition (starting 5 mm before the joint).
Forearm axial rotation follows the palm, avoiding an abrupt twist at the wrist.
The hand's original pose matrix is restored on each sampled frame, preserving
palm contact and the kettlebell grip. `correct_forearm_twist` checks the elbow,
wrist, and forearm endpoint against the original path to within 0.1 mm on every
frame before export. This correction is applied to both arms in every exercise.

Validation of the rebuilt library: all 12 clips retain seamless endpoints and
real motion. Maximum endpoint deviation during the correction was 0.062 mm
(the supported Copenhagen hold); the other clips stayed below 0.001 mm.
The complete palm pose matrix is checked on every authored frame. Quaternion
signs are made continuous before baking to avoid interpolation jumps. A close-up
of the corrected surface is saved at `assets/training3d/previews/wrist_alignment.png`.
