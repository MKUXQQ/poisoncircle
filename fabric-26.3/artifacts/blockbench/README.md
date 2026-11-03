# Blockbench scanner and lobby exports

The original `scanner.bbmodel` and `lobby.bbmodel` are the actual Blockbench snapshots, retaining original embedded solid textures and geometry. The separate `scanner-packed.bbmodel` and `lobby-packed.bbmodel` retain that editable geometry with packed atlas UVs and hidden faces disabled. Runtime JSON exports use the packed atlas versions. No live Blockbench project was switched or modified during this finishing pass.

| Export | Cuboids | Faces before → after | Runtime texture |
| --- | ---: | ---: | --- |
| Scanner | 11 | 66 → 54 | one 16×16 RGBA atlas |
| Lobby | 9 | 54 → 49 | one 16×16 RGBA atlas |

Only faces fully covered by the volume of another unrotated cuboid were removed. No rotated cuboid faces were removed. Exact removals are listed in the cleanup JSON reports. Geometry, element rotations, colors and display settings remain unchanged. Three opaque 16×16 solid textures per model were packed into one 16×16 atlas with padded swatches: 768 → 256 texture pixels per model, 3072 → 1024 base RGBA bytes. Original source textures remain for the original editable snapshots.

The seven-view offline rasterizer applies actual element rotations. Original-face and cleaned-face images compared pixel-exactly across front, back, left, right, top, bottom and three-quarter views: zero changed pixels for both exports. The view PNGs show the packed results. This is an offline cuboid rasterizer, not Minecraft or Blockbench rendering.

Both `java-26.1` structural audits pass with no errors or warnings and complete texture dependencies. This profile is a limited structural check; it does not prove all Minecraft 26.3 schemas, in-game display fit, lighting or FPS. No game/build/install was run during this pass.

`finalize_exports.py` is a one-shot finishing script for original three-texture exports. Its assertion prevents re-packing already packed runtime JSON. Preserve original `.bbmodel` snapshots if re-exporting.
