"""Offline item inspection; reuses the existing Java-cuboid preview projection."""
import json
import numpy as np
from PIL import Image, ImageDraw
from generate_item_models import ASSETS, ROOT
from preview_item_models import render, VIEWS

out = ROOT / 'artifacts/airborne-items'
out.mkdir(parents=True, exist_ok=True)
for name in ('airborne_manager', 'airborne_setter'):
    model = json.loads((ASSETS / 'models/item' / (name + '.json')).read_text(encoding='utf-8'))
    texture = np.array(Image.open(ASSETS / 'textures/item' / (name + '.png')).convert('RGBA'))
    sheet = Image.new('RGB', (960, 1056), (32, 39, 49))
    draw = ImageDraw.Draw(sheet)
    for i, (label, rx, ry) in enumerate(VIEWS):
        for gray, row in ((False, 0), (True, 2)):
            col, line = i % 4, i // 4
            sheet.paste(render(model, texture, (rx, ry, 0), gray), (col * 240, (line + row) * 264))
            draw.text((col * 240 + 8, (line + row) * 264 + 242), label + (' / geometry' if gray else ''), fill='white')
    sheet.save(out / (name + '-views.png'))
    slots = Image.new('RGB', (960, 528), (32, 39, 49))
    draw = ImageDraw.Draw(slots)
    for i, (slot, display) in enumerate(model['display'].items()):
        angles = list(display['rotation']); angles[1] -= 180
        slots.paste(render(model, texture, angles, scale=display['scale'][0]), ((i % 4) * 240, (i // 4) * 264))
        draw.text(((i % 4) * 240 + 8, (i // 4) * 264 + 242), slot, fill='white')
    slots.save(out / (name + '-display.png'))
print(out)
