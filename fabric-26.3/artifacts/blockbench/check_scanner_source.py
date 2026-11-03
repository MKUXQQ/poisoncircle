"""Verify actual runtime geometry, UVs, display and atlas match the editable source."""
import base64
import json
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/'src/main/resources/assets/poisoncircle'
bb=json.loads((Path(__file__).parent/'Poison Circle Scanner - Survey Console.bbmodel').read_text(encoding='utf-8'))
model=json.loads((ASSETS/'models/block/poison_circle_scanner.json').read_text(encoding='utf-8'))
assert len(bb['elements']) == len(model['elements']) == 38
assert sum(len(e['faces']) for e in model['elements']) == 201
for source,runtime in zip(bb['elements'],model['elements']):
    for key in ('name','from','to'):assert source[key] == runtime[key], key
    assert source['faces'].keys() == runtime['faces'].keys()
    for face,s in source['faces'].items():
        r=runtime['faces'][face]
        assert r['texture']=='#atlas' and r['uv']==s['uv']
    angles=source.get('rotation',[0,0,0])
    if any(angles):
        rot=runtime['rotation'];i='xyz'.index(rot['axis'])
        assert source['origin']==rot['origin'] and angles[i]==rot['angle']
        assert sum(a!=0 for a in angles)==1
    else:assert 'rotation' not in runtime
assert bb['display']==model['display']
texture=ASSETS/'textures/block/scanner_atlas.png'
assert base64.b64decode(bb['textures'][0]['source'].split(',',1)[1])==texture.read_bytes()
assert Image.open(texture).size==(64,64)
assert json.loads((ASSETS/'items/poison_circle_scanner.json').read_text())=={'model':{'type':'minecraft:model','model':'poisoncircle:block/poison_circle_scanner'}}
print('Scanner source/runtime match: 38 cubes, 201 faces, 8 rotations; UV, display, embedded atlas and stable item ID checked.')
