# Kestrel T-33 V6 — compact candidate, superseded

This independent candidate was authored through the native Blockbench desktop MCP, version 5.2.2/plugin 1.10.1. No live project was open at entry; capabilities/modes/export lists and empty baseline snapshot are retained. V5 remains unchanged. Native V6 UUID: 299cf0f4-57f4-86d9-2cab-3d1e24579e04.

The parent subsequently requested an Apex-inspired sci-fi dropship. **V6 is a frozen candidate, not the approved final release; do not install it as final.** No Gradle, GitHub, or game process action was performed here.

## Geometry and resources

- 200 meaningful cuboids / 1,200 declared faces / 201 Java model parts including cargo_door.
- Independent short bluff nose, visible windshield and cockpit, high wing, twin propeller engines, elevated empennage, four actual aft-facing seats, short cargo hold with ribs/lights/utility pipes/lockers/crates.
- World local closed bounds including cube rotations: min (-14.063408,-3.5,-17.382613), max (14.063408,7.4,16.1). Span 28.126816, height 10.9, length 33.482613.
- Renderer contract: one model unit = one world block at renderer scale16. No gameplay code changed here.
- One 512×512 opaque atlas, separate nonoverlapping UV island for each cuboid. Repacked from initial 1024; actual atlas occupied rows end at52. Low pixel density is intentional for large painted structural surfaces; no material batches or texture duplication added.
- Six rear door pieces under root cargo_door, pivot (0,-2.05,-9), closed X0/open X-100°. Native four keys at 0/1.5/3/4.5 seconds, actual native mesh rotation at1.5 is -1.7453292519943295 radians.

## Passenger contract and checks

Seat feet (±1.9,-1.736875,-4); cushion top -1.15, yaw180. Rise to (±1.9,-2,-5.2), cross to lanes ±.55, continue to z-8.5; ramp feet end (-2.555674169,-12.151384810), release (-2.85,-13). Floor top -2.05. Rear aperture x±1.65, Y[-2.05,1.15], z-9.

verify.mjs checks all rotated cuboids, both lanes at width .58 / height1.79, seat-rise torso and cushion-contact math; 15,012 sampled envelope points, no blockers. UV bounds/overlap and resource Java/PNG byte identity pass. Export preserves per-cube PartPose rotation and door-local origin. All native full external views plus cabin/seat/open ramp and actual timeline capture are retained.

Limits: sampled geometry is not Minecraft physical collision, multiplayer or FPS validation. AI image input was unavailable; native captures are evidence of actual editor output but no unsupported visual-quality pass is claimed.

## Reproduce audit

node artifacts/airborne-plane-v6/verify.mjs (from fabric-26.3)

Scripts build.mjs and compact-atlas.mjs are native authoring history, not safe generic importers: do not rerun over a live user project. export.mjs writes the two explicitly owned runtime resources. review.mjs temporarily poses only this exact UUID, restores its door and animation mode on exit, and leaves no timer.
