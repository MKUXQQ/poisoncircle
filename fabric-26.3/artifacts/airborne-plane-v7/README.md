# Goblin-inspired Tactical Dropship V7

Independent native Blockbench desktop project UUID ab62caa8-ab44-cb89-4332-e18582fb4878. Editor5.2.2/plugin1.10.1. V6 LIVE including embedded PNG was snapshotted here before creating V7; V5/V6 remain preserved. This is original cuboid geometry inspired by the Apex/Titanfall Goblin, not an extracted proprietary game model.

Reference: https://titanfall.fandom.com/wiki/Goblin and https://static.wikia.nocookie.net/titanfall/images/f/f7/Goblin_T2_1.jpg/revision/latest . Reference JPEG downloaded successfully, but current model does not support image input. Design follows parent-verified reference characteristics; no unsupported visual-quality approval is claimed.

## Final geometry

302 meaningful cuboids / 1,812 declared faces / 303 exported ModelParts / one opaque512 atlas / 6 cargo-door pieces. AABB including cube rotation: min(-9.081782079,-3,-14.9), max(9.081782079,5.65,15.02), width18.163564 / height8.65 / length29.92 worldblocks. No conventional wing, propeller or central tail fin. Short broad armoured wedge, downward segmented black canopy, opaque two-section pressure core, layered side armour, paired extended thrust pods, recessed intakes, eight-sided aft nozzle liners, underside lift outlets and paired pylon tails. Grey-white/graphite surfaces with restrained yellow/warm machinery. Painted panel seams and screws rather than fragmented geometry.

Four real rear-facing passenger seats, forward flight deck with two pilot seats/screens/consoles, short rear hold with structural ribs, lights, utility pipes, medical/fire equipment, cabinets and tied cargo. Two separated lanes preserve fixed runtime anchors. Aircraft scale is one geometry unit per worldblock with renderer16.

## Runtime contract

Floor top-2.05; standing feet-2. Seat feet(±1.9,-1.736875,-4), cushion top-1.15, yaw180. Rise to(±1.9,-2,-5.2), cross to lane±.55, walk to z-8.5. Rear aperture x±1.65/Y[-2.05,1.15]/z-9. Ramp feet end(±.55,-2.555674169,-12.151384810); release(±.55,-2.85,-13). One root cargo_door pivot(0,-2.05,-9), X rotation0→-100degrees. Native keys0/1.5/3/4.5sec. Actual native mesh X at1.5sec =-1.7453292519943295rad.

## Evidence and limits

verify.mjs checks all cube rotations (native ZYX Euler), full dual-lane .58×1.79 player envelope and seat-rise torso:15,012 samples, no blockers. Cushion contact feet+.586875=-1.15. Every cube has a distinct bounded UV island, no overlap; exported Java/PNG byte-identical to the two runtime resources. Native14 exterior/interior/ramp captures plus actual-animation screenshot and additional frontclose/cockpit views retained. Door restored closed/edit mode on exit. No timers remain.

Checks are sampled geometry, not Minecraft collision, gameplay, multiplayer, FPS or visual AI acceptance. No Gradle, GitHub, audio or game process action was performed by this task. Parent owns integration/testing/installation.

## Audit

From repository workspace: node poisoncircle/fabric-26.3/artifacts/airborne-plane-v7/verify.mjs

build.mjs and refine-nose.mjs are authoring history and must not be rerun over user work. export.mjs writes only the approved Java/PNG runtime paths and candidate copies. prepare-tools.mjs is historical scaffolding and would overwrite final verify constants; do not rerun it. The editable bbmodel embeds the texture and native animation, so it can be opened independently.
