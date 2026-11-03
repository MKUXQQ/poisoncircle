"""Draw the scanner's single 64px opaque pixel-art atlas; no geometry generation."""
from pathlib import Path
from PIL import Image, ImageDraw
ROOT = Path(__file__).resolve().parents[2]
image = Image.new('RGBA', (64,64), '#263744'); d = ImageDraw.Draw(image)
colors = ['#344b5d','#748892','#142632','#16363a','#1b4954','#b87338','#67ddcc','#d4b779','#21343d','#394652','#21343d','#263744','#263744','#263744','#263744','#263744']
for i,c in enumerate(colors):
    x,y=i%4*16,i//4*16
    d.rectangle((x,y,x+15,y+15), fill=c)
    d.line((x,y,x+15,y),fill='#9baeb2')
    d.line((x,y,x,y+15),fill='#647f8b')
    d.line((x,y+15,x+15,y+15),fill='#101f2b')
    d.line((x+15,y,x+15,y+15),fill='#101f2b')
# Armor: inset panel seam and purposeful fasteners.
for x,y in ((0,0),(16,0),(0,32),(16,32)):
    d.rectangle((x+3,y+4,x+12,y+11),outline='#45616d')
    for xx,yy in ((x+2,y+2),(x+13,y+13)):
        d.point((xx,yy),fill='#b7c6c3'); d.point((xx+1,yy),fill='#111f28')
d.line((5,8,10,8),fill='#65818b')
# Radar: concentric indexed rings, sweep and plotted contacts.
d.rectangle((48,0,63,15),fill='#092c30')
d.ellipse((49,1,62,14),outline='#3b8d80'); d.ellipse((52,4,59,11),outline='#28695f')
d.line((55,1,55,14),fill='#1a514d'); d.line((49,8,62,8),fill='#1a514d')
d.polygon([(55,8),(59,3),(61,5)],fill='#276f63');d.line((55,8,59,3),fill='#8bffe0')
d.rectangle((51,5,52,6),fill='#e6b879');d.point((59,10),fill='#91ffe5');d.point((55,8),fill='#d5fff1')
# Vent bank: black slots inset into metallic plate.
for y in (19,22,25,28):
    d.rectangle((2,y,13,y+1),fill='#071b24');d.line((3,y+2,12,y+2),fill='#3a6a73')
# Amber hazard plate has deliberate diagonals, no random noise.
for x in range(17,31,5): d.line((x,29,min(x+8,30),18),fill='#e8bb62',width=2)
# Light strip has a dark enclosure and independently indexed segments.
d.rectangle((32,16,47,31),fill='#12383d')
for x in range(34,46,3):d.rectangle((x,21,x+1,26),fill='#78f6d1')
# Keyboard and operating panel.
d.rectangle((48,16,63,31),fill='#172b37')
for y in (20,24,28):
    for x in (50,54,58):d.rectangle((x,y,x+2,y+1),fill='#8b9b98')
d.rectangle((61,20,62,25),fill='#e6b76a')
# Back identification panel, side service panel and top dial.
d.rectangle((2,35,13,44),fill='#13232e');d.line((4,37,11,37),fill='#d8b576',width=2)
d.line((4,41,9,41),fill='#718b91');d.point((12,42),fill='#77f4ce')
d.rectangle((19,36,28,43),outline='#8b9e9e');d.line((21,39,26,39),fill='#182d35')
d.ellipse((35,35,44,44),fill='#152a35',outline='#b0bdb5');d.line((39,39,42,36),fill='#edb26a',width=2)
# Pure interior patches are padded for thin structural parts.
d.rectangle((49,33,62,46),fill='#142632'); d.rectangle((33,49,46,62),fill='#76e9cc')
target=ROOT/'src/main/resources/assets/poisoncircle/textures/block/scanner_atlas.png'
image.save(target, optimize=True)
print(target)
