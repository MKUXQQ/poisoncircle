# Original poison-circle resources

Target: MC 26.3 Fabric, vanilla Java cuboid model JSON plus item definitions. No game, build, install, or Blockbench action was performed.

- Controller: original beige/brown framed command panel, recessed display, two raised keys, sealed back and short aerial. 9 cuboids, 39 declared faces, no zero-thickness elements. Model bounds: x 3–13, y 2–16, z 4.3–9.
- Detector: original teal/blue radar scanner, thick housing, recessed radar, real lower grip, shoulder and offset aerial. 9 cuboids, 38 declared faces, no zero-thickness elements. Model bounds: x 3.5–12.5, y 1–16, z 5–10.
- Each item uses one independent 32×32 atlas (1024 pixels, 4096 uncompressed base RGBA bytes). One static frame; no custom animation, emissive shader or extra material. All actually sampled face UV regions are fully opaque; 128 unused atlas pixels are transparent.
- Wall: newly painted 64×64 transparent canvas, horizontal/vertical red 3px grid borders every 32px. Entire 29×29 cell interiors are transparent, including every center. Alpha only 0/255, coverage 17.87109375%. 4096 pixels / 16384 base RGBA bytes. No alpha blending gradient or center ornament.

All drawings start from blank RGBA canvases; no old texture is loaded or recolored. Display entries explicitly cover GUI, ground, fixed, head, left/right first person and left/right third person. Low component budget is 6–12 per item. Only structure uses geometry; display marks, radar and rear labels are painted. Occluded back/contact faces of projecting features and screen edges fully touching their bezels were removed conservatively. Partially covered housing faces remain because the exposed portions are required and splitting them would add needless geometry.

Editable sources:
- `src/test/python/generate_item_models.py`: explicit cuboid source, UV, original palette/display, item definitions and PNG generation.
- `src/test/python/generate_wall_texture.py`: blank-canvas seamless empty-cell fence grid generation.
- The exported `src/main/resources/assets/poisoncircle/models/item/poison_circle_*.json` files themselves are directly editable vanilla geometry sources.
- `src/test/python/check_item_resources.py`: dependency, real volume, nonduplicate bounds, opaque UV, display contexts, binary-alpha and fully empty grid-interior self-checks.
- `src/test/python/preview_item_models.py`: deterministic NumPy/Pillow orthographic rasterizer; no new dependencies.

Validation completed:
- Both `audit_model.py --profile java-26.1 --assets-root src/main/resources/assets` reports have errors=[], warnings=[], dependencies_complete=true.
- `check_item_resources.py` passed actual model JSON, item-definition JSON, PNG and UV checks.
- Color and untextured orthographic front/back/left/right/top/bottom and oblique sheets are `controller-views.png` and `detector-views.png`.
- `controller-display.png` and `detector-display.png` show all eight display orientations. All objects remain visible without clipping in these approximate previews.
- `wall-tiled.png` shows a 4×4 repeated texture (8×8 empty cells), with continuous grid and no center motifs.

Limitations: java-26.1 audit covers common vanilla JSON structure, not formal 26.3 compatibility certification. Offline renderer approximates UV orientation and display rotations/scale, omits game culling, Minecraft lighting, display translations, player hands and contextual base transforms; it is not a Minecraft or Blockbench screenshot. Hand-context previews show the housing back under this simplified camera, so final in-game hand alignment requires user-side review. No client visual/FPS/VRAM measurement was made; byte counts exclude mipmaps, atlas alignment and GPU copies. Structure audit is not a complete hidden-face solver or full game schema validator. Actual game performance remains unmeasured.
