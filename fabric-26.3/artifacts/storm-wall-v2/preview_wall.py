"""Offline illustrative ray/cylinder preview, not a Minecraft screenshot or FPS test."""
from pathlib import Path
import json
import math
import shutil
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent
TEXTURE = ROOT / 'src/main/resources/assets/poisoncircle/textures/environment/circle_wall.png'


def background(w, h):
    img = Image.new('RGB', (w, h), '#6d8e9b')
    d = ImageDraw.Draw(img)
    d.rectangle((0, h * .6, w, h), fill='#58624a')
    for x, height in [(-100, 140), (130, 85), (420, 125), (730, 95), (970, 155)]:
        d.polygon([(x, h * .6), (x, h * .6 - height), (x + 130, h * .6 - height), (x + 130, h * .6 - height / 2), (x + 240, h * .6 - height / 2), (x + 240, h * .6)], fill='#435c5c')
    for x in range(0, w, 160):
        d.rectangle((x + 20, h * .6 + 50, x + 36, h * .6 + 160), fill='#343e34')
        d.rectangle((x - 10, h * .6 + 25, x + 64, h * .6 + 90), fill='#3b5740')
    return img


def label(img, title, subtitle):
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, img.width, 48), fill='#111c25')
    d.text((16, 8), title, fill='#ffcfac')
    d.text((16, 28), subtitle, fill='#c6d0d7')
    return img


def previews():
    texture = Image.open(TEXTURE).convert('RGBA')
    w, h = 1280, 640
    # Four repeats horizontally, two vertically, nearest filtering; the background stays visible.
    flat = background(w, h).convert('RGBA')
    tile = texture.resize((320, 320), Image.Resampling.NEAREST)
    for y in range(0, h, 320):
        for x in range(0, w, 320):
            flat.alpha_composite(tile, (x, y))
    label(flat, 'ION STORM / HORIZONTAL REPEAT', 'OFFLINE ILLUSTRATION / 4 x 2 tiles / single alpha surface / no bloom').convert('RGB').save(OUT / 'horizontal-repeat.png')
    # Camera outside a radius-40 cylinder. Each ray hits only the nearest surface.
    yy, xx = np.mgrid[:h, :w]
    dx = (xx - w / 2) / 520
    dy = -(yy - h * .47) / 520
    dz = np.ones_like(dx)
    oz, oy, radius = -61.0, 10.0, 40.0
    aa = dx * dx + dz * dz
    bb = 2 * oz * dz
    cc = oz * oz - radius * radius
    disc = bb * bb - 4 * aa * cc
    distance = (-bb - np.sqrt(np.maximum(0, disc))) / (2 * aa)
    px, py, pz = distance * dx, oy + distance * dy, oz + distance * dz
    mask = (disc >= 0) & (distance > 0) & (py >= 0) & (py <= 16)
    repeats = round(math.tau * radius / 16)
    u = (np.arctan2(pz, px) / math.tau * repeats) % 1
    v = (-py / 16) % 1
    tex = np.array(texture)
    sampled = tex[(v * 127).astype(int), (u * 127).astype(int)]
    alpha = sampled[..., 3:4] / 255 * mask[..., None]
    bg = np.array(background(w, h)).astype(float)
    result = np.rint(bg * (1 - alpha) + sampled[..., :3] * alpha).astype('uint8')
    curve = Image.fromarray(result)
    label(curve, 'ION STORM / CURVED WALL', 'OFFLINE RAY-CYLINDER ILLUSTRATION / 16-block UV / one near surface / no game fog or bloom').save(OUT / 'curved-wall.png')
    # Audit imagery includes opaque color and alpha independently; not a second runtime asset.
    sheet = Image.new('RGB', (768, 430), '#111c25')
    sheet.paste(texture.convert('RGB').resize((384, 384), Image.Resampling.NEAREST), (0, 46))
    sheet.paste(texture.getchannel('A').convert('RGB').resize((384, 384), Image.Resampling.NEAREST), (384, 46))
    label(sheet, 'SOURCE COLOR (LEFT) / ALPHA (RIGHT)', '128 x 128 source / exact paired edges / grayscale alpha, not opacity on black').save(OUT / 'texture-audit.png')
    values = np.array(texture)
    report = {'size': list(texture.size), 'png_bytes': TEXTURE.stat().st_size,
              'rgba_base_bytes': texture.width * texture.height * 4,
              'channel_min': values.min(axis=(0, 1)).tolist(),
              'channel_max': values.max(axis=(0, 1)).tolist(),
              'alpha_mean': float(values[..., 3].mean()),
              'coverage_percent': float((values[..., 3] > 0).mean() * 100),
              'bright_pulse_percent': float((values[..., 1] >= 74).mean() * 100),
              'axis_edge_max_delta': [int(np.abs(values[:, 0].astype(int) - values[:, -1]).max()), int(np.abs(values[0].astype(int) - values[-1]).max())],
              'runtime': 'one existing texture/surface, BAND_HEIGHT=32, geometry unchanged; FPS unmeasured',
              'preview_limitations': 'offline illustrative alpha blend; does not emulate Minecraft fog, shader, sampling or frame timing'}
    (OUT / 'audit.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf8')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    import sys
    if '--baseline' in sys.argv:
        destination = OUT / 'baseline-circle-wall.png'
        if not destination.exists():
            shutil.copy2(TEXTURE, destination)
        print('Baseline saved')
    else:
        previews()
