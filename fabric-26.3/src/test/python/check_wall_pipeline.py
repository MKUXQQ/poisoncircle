"""Check continuous wall texture coverage, colors, periodicity and scan motion."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
TEXTURE = ROOT / 'src/main/resources/assets/poisoncircle/textures/environment/circle_wall.png'


def check():
    image = Image.open(TEXTURE).convert('RGBA')
    assert image.size == (64, 64)
    pixels = list(image.get_flattened_data())
    alpha = [p[3] for p in pixels]
    assert min(alpha) >= 44 and max(alpha) <= 88 and len(set(alpha)) >= 30
    assert all(r >= 224 and 54 <= g <= 162 and 8 <= b <= 32 and r > g for r, g, b, _ in pixels)
    # The wrapping shader scrolls the one surface; both tile edges must join.
    assert max(abs(image.getpixel((0, y))[c] - image.getpixel((63, y))[c])
               for y in range(64) for c in range(4)) <= 8
    assert max(abs(image.getpixel((x, 0))[c] - image.getpixel((x, 63))[c])
               for x in range(64) for c in range(4)) <= 8
    # Check broad band movement diagonally and reject regular grid-like bars.
    luminance = [[sum(image.getpixel((x, y))[:3]) / 3 for x in range(64)] for y in range(64)]
    assert len({round(v) for row in luminance for v in row}) > 20
    horiz = sum(abs(luminance[y][x] - luminance[y][(x+1)%64]) for y in range(64) for x in range(64))
    vert = sum(abs(luminance[y][x] - luminance[(y+1)%64][x]) for y in range(64) for x in range(64))
    diag = sum(abs(luminance[y][x] - luminance[(y+1)%64][(x+1)%64]) for y in range(64) for x in range(64))
    assert horiz > 0 and vert > 0 and diag < (horiz + vert) * 0.8
    print('wall pipeline OK: 64x64, continuous red alpha field, seamless wrap, oblique scan bands')


if __name__ == '__main__':
    check()
