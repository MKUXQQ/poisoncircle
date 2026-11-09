"""Two original low-component admin tools; Java cuboids and one 32px atlas each."""
import json
from PIL import Image, ImageDraw
from generate_item_models import ASSETS, DISPLAY, cube, models

def create():
    # Reuse the existing controller shell and its verified display transforms.
    manager = models()['controller']
    setter = [
        cube('survey grip', [6, 1, 6], [10, 7, 9], 0, ('up',)),
        cube('survey head', [3, 7, 5], [13, 13, 10], 1, back=[8, 8, 16, 16]),
        cube('coordinate display', [4, 8, 4.5], [12, 12, 5], 3, ('south', 'east', 'west', 'up', 'down'), [0, 8, 8, 16]),
        cube('receiver stalk', [7, 13, 7], [9, 15, 9], 0, ('up', 'down')),
        cube('survey receiver', [5, 15, 5.5], [11, 16, 10.5], 2, ('down',)),
        cube('orange trigger', [10, 4, 6], [11, 6, 8], 4, ('west',)),
    ]
    for name, elements in [('airborne_manager', manager), ('airborne_setter', setter)]:
        image = Image.new('RGBA', (32, 32), (0, 0, 0, 255))
        draw = ImageDraw.Draw(image)
        palette = ['#344858', '#a7c1cc', '#d5e8e8', '#162c39', '#e18b44', '#79c4b0']
        for i, color in enumerate(palette):
            x, y = (i % 4) * 8, (i // 4) * 8
            draw.rectangle((x, y, x + 7, y + 7), fill=color)
        draw.rectangle((0, 16, 15, 31), fill=palette[3])
        draw.rectangle((16, 16, 31, 31), fill=palette[0])
        if name.endswith('manager'):
            # Printed aircraft icon, not extra decorative cuboids.
            draw.line((8, 18, 8, 28), fill=palette[2], width=2)
            draw.line((2, 23, 13, 23), fill=palette[2], width=2)
            draw.line((5, 28, 11, 28), fill=palette[5], width=1)
        else:
            draw.line((8, 18, 8, 29), fill=palette[5], width=1)
            draw.line((2, 24, 13, 24), fill=palette[5], width=1)
            draw.rectangle((6, 22, 10, 26), outline=palette[2])
            draw.point((8, 24), fill=palette[4])
        draw.rectangle((19, 21, 28, 22), fill=palette[1])
        draw.rectangle((19, 25, 25, 25), fill=palette[1])
        model = {'credit': 'Airborne tools; shared existing controller display grammar', 'texture_size': [32, 32],
                 'gui_light': 'front', 'textures': {'atlas': 'poisoncircle:item/' + name, 'particle': 'poisoncircle:item/' + name},
                 'elements': elements, 'display': DISPLAY}
        (ASSETS / 'models/item' / (name + '.json')).write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')
        (ASSETS / 'items' / (name + '.json')).write_text(json.dumps({'model': {'type': 'minecraft:model', 'model': 'poisoncircle:item/' + name}}, indent=2) + '\n', encoding='utf-8')
        image.save(ASSETS / 'textures/item' / (name + '.png'))
        print(name, len(elements), 'cuboids', sum(len(e['faces']) for e in elements), 'faces, 1 opaque 32px atlas')

if __name__ == '__main__':
    create()
