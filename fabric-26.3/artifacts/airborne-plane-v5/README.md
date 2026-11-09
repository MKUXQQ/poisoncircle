# Atlas C-94 Rear Cargo V5 — frozen

Native Blockbench desktop project UUID `5dee7cbb-20e8-8719-9f2d-d0073c4b55d5`, modded_entity format. The actual live V4 project (269 cubes, UUID `5d029c06-da23-83c4-c9f5-75d160fcb179`) was compiled and its embedded PNG saved as baseline-live.bbmodel / baseline-live.png before the independent V5 project was created. V4 files were not modified.

328 cuboids, 1968 declared faces, 8 editor groups, 329 unique Java parts including the empty cargo_door parent. One opaque 1024×1024 atlas / one material / one frame / 1,048,576 pixels / 4,194,304 base RGBA bytes. No meshes, zero-thickness planes, inverted cubes or per-frame geometry copies. 144 rotated cubes. Budget ≤360. Six individual seats have real cushions, backrests, two mounting supports, arms, angled restraint webbing and buckle; structural ribs, overhead lamps/utility runs, four restrained two-level cargo racks and a forward partition make the interior purposeful and more compact. Small seams/rivets are painted. Existing exterior wing/engine/tail character retained. Hidden faces were not speculatively deleted because cabin, ramp, underside and back surfaces can become visible.

## Locked runtime contract

Coordinates are model/world block units, +Y up, +Z nose, -Z tail; renderer scales Minecraft model geometry by 16.

- Occupied seats: center `(-4,-1.736875,-5)` and `(4,-1.736875,-5)` are entity feet anchors. Both face local yaw 180° / aft. Cushion top Y=-1.15; Minecraft sitting leg bottom is feetY+.586875, exactly matching this surface. Cushion x=center±.65, z[-5.65,-4.35], thickness .20. Backrest is forward of the occupant at z[-4.34,-4.12]. Three seats on each side; two at z=-5 are occupied.
- Seat rise waypoint: `(±4,-2,-6.2)`. Center route lanes x=±.55, Y=-2, from z=-6.2 to -28.5. Floor top=-2.05 with .05 feet allowance.
- Rear opening: x[-3,3], Y[-2.05,2.95], z=-29. `cargo_door` remains a root direct child. Pivot `(0,-2.05,-29)`. Closed xRot=0, fully open xRot=-100° = -1.7453292519943295 radians. Seven moving panel/edge/stiffener parts are children of this one group; no static door duplicate. Rotate the parent only.
- Five-unit inner ramp surface ends at `(0,-2.918240888,-33.924038765)`. Feet route endpoint `(±.55,-2.868240888,-33.924038765)`, then departure `(±.55,-3.15,-35)`.
- Former left side door was removed and its opening replaced by a continuous hull panel. Aft bulkhead and lower original tail-shoulder facets were replaced by a real rear frame, threshold and lifted tail-root cheeks with an open center. This is a newly built rear exit, not a renamed side door.

## Native editable motion and views

`Atlas C-94 Rear Cargo V5.bbmodel` embeds one native cargo_door_open_close animation, 4.5s, four linear X-rotation keys: t0=0°, t1.5=-100°, t3=-100°, t4.5=0°. `cargo-door.animation.json` is the corresponding native animation metadata. Runtime requires only renderer cargo_door.xRot; no animation library.

`native-animation-pose.json` records actual `animation_timeline set_time 1.5` and bone mesh rotation `[-1.7453292519943295,0,0,"ZYX"]`. `native-animation-open.png` uses that actual timeline pose from a clear rear camera. Animation playback stopped and Edit mode / closed mesh restored. No timers or terminal sessions were created; MCP connections closed, their private offscreen views disposed at session end.

Full iso/front/rear/side/top and cabin/seat/door views were captured natively, with closed/half/full door poses. `rear-clearance.png` looks through the open rear frame into the seats and central aisle; `ramp-underside.png` checks the back of the assembly. Modlens inspected actual images: full aircraft stays in frame with connected wings/tail; seat view shows back, cushion, supports and arms; rear-clearance and native animation-open clearly show open rectangular aft doorway, lowered outward ramp and visible interior without a full-width blocking panel. Initial oblique exterior view is partially occluded by the tail root, so it is not treated as clearance proof. Seat restraint identity is less visually certain at this texture density; its geometry is independently named and present.

## Reproduce verification

Run from `D:/dsh项目/项目终极对决`:

```
node poisoncircle/fabric-26.3/artifacts/airborne-plane-v5/verify.mjs
node poisoncircle/fabric-26.3/artifacts/airborne-plane-v5/review.mjs
node poisoncircle/fabric-26.3/artifacts/airborne-plane-v5/door-closeup.mjs
```

verify-report.json: PASS, 328 cubes, 329 unique names, 7 nested door pieces, zero overlapping or out-of-bounds UV nets, Java and PNG byte-identical to installed resources, exact seat surface alignment, closed aperture coverage, actual native X animation pose, 22,140 standing route envelope samples plus 2,214 seat-rise torso samples clear of all rendered cuboids. Player envelope .58 wide, 1.79 high, both lane paths tested in true 3D. Rise samples cover upper torso/head; the initial lower seated body intentionally contacts its supporting cushion. Ramp contact sampling starts .08 above feet to accommodate a sloping sole footprint; this is not an exact collider solver.

skill-audit-generic.json: no structural errors, dependencies complete. skill-audit.json retains the first Java-item-profile result: it reports java_coordinate_overflow because item-model [-16,32] coordinate restrictions do not apply to this custom Java modded entity ModelPart export. That report is not claimed as a pass. Dedicated export/UV/door/route verification covers the actual Java geometry. The generic audit retains runtime_animation_not_verified warning, addressed at the editor/export level by native timeline sampling and renderer contract; actual gameplay is separate.

`build.mjs` creates a new independent project and is not a safe command to run over arbitrary ongoing editing. `refine.mjs` is a one-time native restraint refinement, already applied; do not rerun it. `export.mjs` writes only the authorized geometry Java and entity texture resources. No Game/Entity/Renderer/mixin source was changed by this model delegate, and it did not run Gradle, GitHub, or start/kill Minecraft.

Model, Java and atlas are frozen for parent integration. Game visual/collision/FPS testing was not performed by this delegate; sampling does not prove Minecraft physics behavior.
