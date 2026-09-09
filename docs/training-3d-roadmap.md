# Training 3D overhaul — incremental handoff

## Current checkpoint

Milestone 2 is complete as a visual prototype: one clothed, skinned athlete and a four-second single-arm kettlebell floor-press animation (bell outside the working forearm) were authored in Blender 4.5.9 LTS. Editable scenes, a 1.58 MiB GLB, preview renders, and source/license records are saved. See `assets/training3d/previews/floor_press_review.md` for review evidence, measurements and limitations. The other eleven exercises remain pending.

Native interactive playback is now implemented for this one exercise using pinned SceneView 2.3.0: the reference dialog loops automatically; the guided card follows workout playback. Drag orbits, pinch zooms, and controls offer front/side/reset, pause, and half speed. It loads the bundled GLB offline, retains form cues/reference on model-load failure, and offers retry. Lifecycle pause prevents animation advancing in the background. The demo remains explicitly left-arm only.

Validation: debug build and unit tests passed; five emulator tests covering viewer controls and Training flow passed on the API 36 FootyOS_Preview emulator. Visually confirmed rendered character and orbit from the opposite side. A final gesture fix prevents the parent scroll container intercepting drags. Hardware-device frame-time profiling and mirrored right-side clips remain follow-ups; no performance certification or coaching approval is claimed.

## Platform decision

Use Blender to author a reusable human mesh, skinning, equipment, and exercise-specific skeletal animation. Export self-contained glTF 2.0 binary (.glb). Use SceneView over Filament for native Compose playback. This is the chosen fit for FootyOS, not a claim that one platform is universally best.

Use a proportional, clothed athletic character: charcoal fitted shirt/shorts, lime accents, neutral skin shading, visible hand and foot placement. Real mesh surfaces, smooth skeletal deformation, grounded feet, equipment contact, and soft studio lighting are required. Replacing lines with disconnected spheres/capsules does not meet the visual target.

Starting asset candidate: Blender's Human Base Meshes library. Its official demo-file listing identifies the bundle as CC0. Verify and retain the license bundled with the exact downloaded version before use; do not confuse it with the separately listed CC-BY realistic-human training asset. The base mesh still needs clothing, rigging, skinning, and exercise animations.

Mixamo can help with humanoid rigging, but Adobe ID access is required. Its generic motion library is not evidence that a movement teaches correct kettlebell technique. Author and inspect exercise-specific movement in Blender; do not substitute an unrelated stock animation.

## Bounded milestones

| Task | Deliverable | Finish checkpoint |
| --- | --- | --- |
| 1 — Foundation | Platform choice, visual target, 12-exercise inventory, asset intake checker | Complete in this task; no visual replacement claimed |
| 2 — Character + one animation | Install a pinned Blender LTS toolchain; acquire and record source/license; rig one clothed character; animate `floor_press` with kettlebell and floor contact | Save editable `.blend`, GLB, license, and front/side preview. Inspect shoulder/wrist alignment, floor contact, interpolation, and loop seam. This is a visual prototype, not certified coaching. |
| 3 — Native player | Add a verified, pinned SceneView version; show the floor-press GLB in the reference dialog and guided card | Orbit, front/side presets, reset, 0.5×/1× preview, looping, timer pause/resume, background pause, offline load, failure fallback; run build and emulator tests |
| 4 — Upper-body pair | `row`, `overhead_press` using the same rig | Inspect both sides and equipment contact; export, validate, integrate only these two |
| 5 — Carry + squat | `suitcase_carry`, `goblet_squat` | Grounded walking cycle; planted squat feet, controlled knee tracking |
| 6 — Power + hinge | `swing`, `single_leg_rdl` | Hip-driven swing with continuous bell trajectory; stable single-leg balance and square pelvis |
| 7 — Split squat + calf | `split_squat`, `calf_raise` | Bench/foot contact and smooth depth; ankle alignment and controlled heel motion |
| 8 — Remaining prehab | `tibialis_raise`, `copenhagen`, `hamstring_slider` (split into separate runs if needed) | Wall, bench, floor and heel contact; appropriate supported Copenhagen variant |
| 9 — Performance + rollout | Full-library quality and device check | No blank viewport, no memory leak between exercises, predictable cameras, phone-size readability, APK size/frame-time measurements; replace remaining diagrams only where reviewed assets exist |

## Asset and playback contract

- Inventory: `assets/training3d/manifest.json`. Pending entries are intentional; no placeholder is marked ready.
- Source: `assets/training3d/source/<exercise_id>.blend`; preserve the reusable rig in `character.blend`.
- Runtime: `app/src/main/assets/training3d/<exercise_id>.glb`.
- Clip: `rep` for bilateral work; `left` and `right` for unilateral work. Carries use left/right walking loops; holds use left/right held positions with subtle breathing, not repeated full raises.
- Units: metres; glTF Y-up; character grounded. Keep root motion in place for this fixed-camera teaching viewport.
- Export baked skeletal animation, embedded textures, and physically based materials. No remote buffer/image references. One clip must cover a full return to its starting pose for a seamless loop.
- Initial engineering targets, to be measured: <=8 MiB per exercise, <=40k visible triangles, <=2k textures. These are budgets, not measured results.
- Keep the kettlebell attached to the correct grip throughout movement. Use inverse kinematics/contact constraints in the source scene, bake the result for export.
- Demo playback is illustrative and never counts actual reps or advances a rep set. Existing GuidedDrill logic remains the authority for workout state.
- During preparation and rest, show the setup/next side. During work, play the correct clip. Pause animation when paused, backgrounded, or a guidance/session overlay is open.
- A load failure retains the existing reference guidance and a retry action; never leave an empty area. Do not fetch models on every exercise change.
- Technical validation is not a form-quality review. Review anatomy, range, contact, and loop continuity visually in Blender and on a phone. Record actual reviewer/evidence; never label an unreviewed asset approved.

## Next-task prompt

“Continue milestone 3 in docs/training-3d-roadmap.md only: integrate the existing floor_press.glb into the native Android Training Guided player and reference dialog. Preserve current Training changes. Add orbit/front/side camera controls, slow playback and pause synchronization, verify loading fallback, and measure frame performance. Use the saved Blender source if adjustments are necessary. Do not animate the remaining exercises yet.”

## Research sources (checked September 7, 2026)

- Blender animation/rigging: https://www.blender.org/features/animation/
- Blender export formats: https://www.blender.org/features/pipeline/
- Human Base Meshes and license listing: https://www.blender.org/download/demo-files/
- Blender LTS: https://www.blender.org/download/lts/
- Native Compose/Filament GLB loading: https://sceneview.github.io/docs/quickstart/
- Adobe Mixamo access and rigging limitations: https://helpx.adobe.com/creative-cloud/faq/mixamo-faq.html
