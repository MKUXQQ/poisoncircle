# Atlas C-94 Cabin V4

Native Blockbench 5.2.2 desktop / MCP plugin 1.10.1. Project UUID `5d029c06-da23-83c4-c9f5-75d160fcb179`. The untouched V3 live project and atlas were snapshotted before creating this independent candidate. V3 files were not modified.

269 cuboids / 1614 declared faces / 8 editor groups / 270 unique Java parts (269 cube parts plus the empty cargo_door parent). One opaque 1024×1024 atlas, one material/frame, 1,048,576 pixels, 4,194,304 uncompressed base RGBA bytes. 137 rotated cubes. Actual FPS and Minecraft gameplay were not measured by this model task.

Cabin additions: six structural frames, roof beams, hydraulic and cable runs, service junctions, twelve individual wall-side troop seats/harnesses, two strapped cargo loads at forward z17.3..21.6, door warning frame and exit indicator. Panel lines and rivets remain painted rather than extruded. No speculative hidden-face deletion: cabin/door/underside faces can become visible.

## Runtime door contract

Root direct child `cargo_door`, pivot `(-7,2.35,-18)`. Exactly six children comprise the moving door assembly. Closed `zRot=0`; fully open `zRot=-Math.PI/2`. Door bottom swings outward toward negative X, not into the aisle. Do not rotate each child independently. No static closed-door duplicate exists.

At -90°, every moving door corner is above Y1.99. Standing feet remain Y-2; tested standing head Y-0.21. Closed panel covers the left aperture. Feet positions `(±1.6,-2,-5)`; original exit `(-9,-2,-17.5)` and left-door x=-7,z[-21,-15] retained. The parent automatic route to z=-18 and outward to x=-10 has 6021 sampled player-envelope points (width .58,height1.79) without solid rendered cuboid intersections. This is a geometric sampling check, not Minecraft physics execution.

## Editable animation

`Atlas C-94 Cabin V4.bbmodel` contains native `animation.cargo_door_open_close`, 4.5s, four linear rotation keys at 0,1.5,3,4.5s with Z 0,-90,-90,0 degrees. Also supplied as `cargo-door.animation.json` native undo-copy metadata. `native-animation-open.png` was captured using the actual animation timeline at1.5s; measured mesh rotation exactly -PI/2 in ZYX order. Default project geometry is closed, Edit mode restored, playback stopped. Java game animation is driven by renderer; no new animation library dependency.

## Reproduce

Run from repository workspace root:

```
node poisoncircle/fabric-26.3/artifacts/airborne-plane-v4/verify.mjs
node poisoncircle/fabric-26.3/artifacts/airborne-plane-v4/review.mjs
node poisoncircle/fabric-26.3/artifacts/airborne-plane-v4/animation-review.mjs
```

`build.mjs` is a native desktop reconstruction tool; it creates a new project and should not be rerun over an active editing session. `export.mjs` writes the two authorized runtime geometry/texture files. `animate.mjs` authors the saved editable animation; avoid rerunning to create duplicate animations.

Verification: zero UV rectangle overlaps or out-of-bounds nets; 270 unique names preserve negative number signs; exported Java and atlas byte-identical to installed source resources; six nested moving door components; no static door; native animation pose verified. Skill structural audit has no errors and dependencies complete; runtime-animation warning is addressed by native pose/export checks, not game execution.

Modlens reviewed full iso (complete frame, visible connected hull/wings/tail), open cabin (ribs/beams/utility runs and broad foreground aisle visible), and open door (upper horizontal outward panel and opening below visible). Screenshots cannot establish full collision clearance or hidden geometry correctness; the route/corner checks provide separate evidence. Single-view uncertainties include occluded areas and visual identification of small lamps/harnesses.
