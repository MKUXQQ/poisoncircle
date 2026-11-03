"""Check storm texture facts plus existing single-surface, clipping and fog contracts."""
from pathlib import Path
from PIL import Image
from generate_wall_texture import sample

ROOT = Path(__file__).resolve().parents[3]
TEXTURE = ROOT / 'src/main/resources/assets/poisoncircle/textures/environment/circle_wall.png'


def check():
    with Image.open(TEXTURE) as source:
        assert source.format == 'PNG' and source.mode == 'RGBA'
        source.verify()
    image = Image.open(TEXTURE).convert('RGBA')
    assert image.size == (128, 128)
    size = image.width
    pixels = list(image.get_flattened_data())
    alpha = [p[3] for p in pixels]
    assert 48 <= min(alpha) < max(alpha) <= 104 and len(set(alpha)) >= 45
    assert all(144 <= r <= 255 and 23 <= g <= 168 and 18 <= b <= 44 and r > g > b
               for r, g, b, _ in pixels)
    assert 0.01 <= sum(g >= 130 for _, g, _, _ in pixels) / len(pixels) <= 0.04
    # Every pixel carries haze: bright currents never cut holes into the wall.
    assert sum(a > 0 for a in alpha) == size * size
    for n in range(size):
        assert image.getpixel((0, n)) == image.getpixel((size - 1, n))
        assert image.getpixel((n, 0)) == image.getpixel((n, size - 1))
    # Verify the actual delivered PNG, not only a generator's claimed parameters.
    assert pixels == [sample(x / (size - 1), y / (size - 1))
                      for y in range(size) for x in range(size)]
    renderer = (ROOT / 'src/client/java/com/poisoncircle/PoisonCircleVisualRenderer.java').read_text(encoding='utf8')
    geometry = (ROOT / 'src/main/java/com/poisoncircle/WallGeometry.java').read_text(encoding='utf8')
    assert 'RenderTypes.entityTranslucentEmissive(' in renderer
    assert 'entityCutout' not in renderer
    assert renderer.count('submitCustomGeometry(') == 1
    assert renderer.count('vertex(vertices, matrix,') == 4
    assert 'frustum.isVisible(new AABB(' in renderer
    assert 'WallGeometry.bandsPerSide(far)' in renderer and 'band < bands' in renderer
    assert 'BAND_HEIGHT = 32' in geometry
    assert 'Math.ceil(far / BAND_HEIGHT)' in geometry
    assert 'float v0 = scroll -' in renderer and 'float v1 = scroll -' in renderer
    assert '/ 16)' in renderer and '% 24000L' in renderer
    # Use vanilla material fog; no custom fog bypass is introduced by the wall.
    assert 'setShaderFog' not in renderer and 'FogParameters' not in renderer
    print(f'wall pipeline OK: 128x128 RGBA; seamless both axes; no holes; alpha {min(alpha)}..{max(alpha)}; sparse pulses; one surface; 32-block bands; vanilla material/fog path')


if __name__ == '__main__':
    check()
