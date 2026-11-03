"""Original low-component Java item models and 32px atlases; no source image is read.
Run with bundled Python/Pillow. JSON is the editable geometry source as well.
"""
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/poisoncircle'
DISPLAY = {
    'gui': {'rotation': [24, 210, 0], 'translation': [0, 0, 0], 'scale': [0.88]*3},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5]*3},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.8]*3},
    'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 0], 'scale': [0.8]*3},
    'firstperson_righthand': {'rotation': [0, 155, 0], 'translation': [0, 1, -1.5], 'scale': [0.40]*3},
    'firstperson_lefthand': {'rotation': [0, -155, 0], 'translation': [0, 1, -1.5], 'scale': [0.40]*3},
    'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 2, 1], 'scale': [0.65]*3},
    'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 2, 1], 'scale': [0.65]*3},
}


def atlas(kind):
    image = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(image)
    palette = (['#795540', '#dfc68f', '#f5dfa9', '#342d26', '#dd653c', '#93bf6e'] if kind == 'controller'
               else ['#254c61', '#318da1', '#90dfd7', '#102c3b', '#f39b63', '#61efcb'])
    # Padded solid swatches: no sampling across neighboring materials.
    for i, color in enumerate(palette):
        x, y = (i % 4)*8, (i // 4)*8
        d.rectangle((x, y, x+7, y+7), fill=color)
    d.rectangle((0, 16, 15, 31), fill=palette[3])
    if kind == 'controller':
        # A readable control display, with no fake geometry stamped into pixels.
        d.rectangle((2, 18, 13, 20), fill='#82b77a')
        d.rectangle((2, 23, 9, 24), fill='#d9c68b')
        d.rectangle((2, 27, 6, 28), fill='#d9c68b')
        d.line((11, 23, 11, 28), fill='#c45f3c', width=2)
    else:
        # Radar circles and sweep are surface graphics inside a solid bezel.
        d.ellipse((2, 18, 13, 29), outline='#36898d', width=1)
        d.ellipse((5, 21, 10, 26), outline='#36898d', width=1)
        d.line((8, 24, 12, 20), fill='#a0ffe4', width=1)
        d.rectangle((10, 25, 11, 26), fill='#f4b070')
    # Back label is distinct from the screen; no added cubes for printed marks.
    d.rectangle((16, 16, 31, 31), fill=palette[0])
    d.rectangle((19, 21, 28, 22), fill=palette[1])
    d.rectangle((19, 25, 25, 25), fill=palette[1])
    return image


def swatch(i):
    x, y = (i % 4)*4, (i // 4)*4
    return [x+1, y+1, x+3, y+3]


def cube(name, a, b, material, hidden=(), front=None, back=None):
    faces = {face: {'uv': swatch(material), 'texture': '#atlas'}
             for face in ('north', 'south', 'east', 'west', 'up', 'down') if face not in hidden}
    if front is not None:
        faces['north']['uv'] = front
    if back is not None:
        faces['south']['uv'] = back
    return {'name': name, 'from': a, 'to': b, 'faces': faces}


def models():
    controller = [
        cube('brown sealed housing', [3, 2, 6], [13, 14, 9], 0, back=[8, 8, 16, 16]),
        cube('ivory left frame', [3, 4, 5], [4, 13, 6], 1, ('south', 'up', 'down')),
        cube('ivory right frame', [12, 4, 5], [13, 13, 6], 1, ('south', 'up', 'down')),
        cube('ivory top frame', [3, 13, 5], [13, 14, 6], 2, ('south',)),
        cube('ivory lower panel', [3, 2, 5], [13, 4, 6], 1, ('south',)),
        cube('recessed control screen', [4, 6, 5.5], [12, 13, 6], 3, ('south', 'up', 'east', 'west'), [0, 8, 8, 16]),
        cube('raised orange command key', [5, 2.5, 4.3], [7, 3.5, 5], 4, ('south',)),
        cube('raised green confirmation key', [9, 2.5, 4.3], [11, 3.5, 5], 5, ('south',)),
        cube('short transmitter', [10.5, 14, 7], [11.5, 16, 8], 0, ('down',)),
    ]
    detector = [
        cube('blue scanner housing', [3.5, 5, 6], [12.5, 13, 10], 0, back=[8, 8, 16, 16]),
        cube('upper receiver shoulder', [5, 13, 6.5], [11, 14, 9.5], 1, ('down',)),
        cube('deep grip handle', [6, 1, 6.5], [10, 5, 9.5], 0, ('up',)),
        cube('recessed radar glass', [4.5, 6, 5.5], [11.5, 12, 6], 3, ('south', 'up', 'down', 'east', 'west'), [0, 8, 8, 16]),
        cube('cyan left bumper', [3.5, 6, 5], [4.5, 12, 6], 1, ('south', 'up', 'down')),
        cube('cyan right bumper', [11.5, 6, 5], [12.5, 12, 6], 1, ('south', 'up', 'down')),
        cube('bright top bezel', [3.5, 12, 5], [12.5, 13, 6], 2, ('south',)),
        cube('lower bezel', [3.5, 5, 5], [12.5, 6, 6], 1, ('south',)),
        cube('offset receiver aerial', [4, 13, 7.5], [5, 16, 8.5], 2, ('down',)),
    ]
    return {'controller': controller, 'detector': detector}


def main():
    for kind, elements in models().items():
        name = 'poison_circle_' + kind
        model = {'credit': 'Original blank-canvas design; generate_item_models.py',
                 'texture_size': [32, 32], 'gui_light': 'front',
                 'textures': {'atlas': 'poisoncircle:item/' + name, 'particle': 'poisoncircle:item/' + name},
                 'elements': elements, 'display': DISPLAY}
        assert 6 <= len(elements) <= 12
        assert all(all(b > a for a, b in zip(e['from'], e['to'])) for e in elements)
        assert set(DISPLAY) == {'gui', 'ground', 'fixed', 'head', 'firstperson_righthand', 'firstperson_lefthand', 'thirdperson_righthand', 'thirdperson_lefthand'}
        (ASSETS / 'models/item' / (name + '.json')).write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')
        definition = {'model': {'type': 'minecraft:model', 'model': 'poisoncircle:item/' + name}}
        (ASSETS / 'items' / (name + '.json')).write_text(json.dumps(definition, indent=2) + '\n', encoding='utf-8')
        image = atlas(kind)
        assert image.size == (32, 32)
        image.save(ASSETS / 'textures/item' / (name + '.png'), optimize=True)
        print(kind, len(elements), 'cuboids,', sum(len(e['faces']) for e in elements), 'faces, 1x32x32 atlas')


if __name__ == '__main__':
    main()
