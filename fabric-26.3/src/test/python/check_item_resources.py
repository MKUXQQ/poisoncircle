"""Small dependency/geometry/UV checks for the actual generated resource files."""
import json
from PIL import Image
from generate_item_models import ASSETS, DISPLAY

for kind in ('controller', 'detector', 'airborne_manager', 'airborne_setter'):
    name = kind if kind.startswith('airborne_') else 'poison_circle_' + kind
    model = json.loads((ASSETS / 'models/item' / (name + '.json')).read_text(encoding='utf-8'))
    definition = json.loads((ASSETS / 'items' / (name + '.json')).read_text(encoding='utf-8'))
    assert definition == {'model': {'type': 'minecraft:model', 'model': 'poisoncircle:item/' + name}}
    assert 'parent' not in model and 6 <= len(model['elements']) <= 12
    assert model['display'] == DISPLAY
    for slot in ('firstperson_righthand', 'firstperson_lefthand'):
        assert model['display'][slot]['scale'] == [0.4] * 3
        assert abs(model['display'][slot]['rotation'][1]) == 155
    image = Image.open(ASSETS / 'textures/item' / (name + '.png')).convert('RGBA')
    assert image.size == (32, 32)
    seen = set()
    for element in model['elements']:
        a, b = element['from'], element['to']
        assert all(0 <= x < y <= 16 for x, y in zip(a, b))
        signature = tuple(a + b)
        assert signature not in seen
        seen.add(signature)
        for face in element['faces'].values():
            assert face['texture'] == '#atlas'
            u, v, U, V = face['uv']
            assert 0 <= u < U <= 16 and 0 <= v < V <= 16
            region = image.crop((int(u*2), int(v*2), int(U*2), int(V*2)))
            assert set(region.getchannel('A').tobytes()) == {255}, 'Every sampled item face must be opaque'
    print(kind, 'geometry, item definition, display and opaque UV dependencies OK')

from check_wall_pipeline import check
check()
