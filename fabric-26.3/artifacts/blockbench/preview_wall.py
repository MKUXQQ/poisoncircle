"""Show the continuous single-surface wall over a neutral environment."""
from pathlib import Path
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[2]
wall=Image.open(ROOT/'src/main/resources/assets/poisoncircle/textures/environment/circle_wall.png').convert('RGBA')
scene=Image.new('RGBA',(768,384),'#222e37');d=ImageDraw.Draw(scene)
for x in range(0,768,96):
    d.polygon([(x,230),(x+48,160),(x+96,230)],fill='#4b6268')
d.rectangle((0,260,768,384),fill='#5f6251')
for x in range(0,768,64):
    for y in range(0,384,64):scene.alpha_composite(wall,(x,y))
d=ImageDraw.Draw(scene);d.text((16,16),'RED DANGER FIELD / continuous single translucent surface',fill='#f5d8cd')
scene.convert('RGB').save(ROOT/'artifacts/blockbench/wall-redesign-preview.png')
