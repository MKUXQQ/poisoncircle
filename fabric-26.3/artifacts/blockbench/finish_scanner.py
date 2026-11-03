"""Finish the native-MCP blockout with deliberate UVs and a shared tilted console."""
import base64
import copy
import json
from pathlib import Path
import sys
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/'src/main/resources/assets/poisoncircle'
OUT=Path(__file__).parent
SOURCE=OUT/'Poison Circle Scanner - Survey Console.bbmodel'
bb=json.loads(SOURCE.read_text(encoding='utf-8'))
old=json.loads((ASSETS/'models/block/poison_circle_scanner.json').read_text())
if not (OUT/'scanner-before-redesign.json').exists():
    (OUT/'scanner-before-redesign.json').write_text(json.dumps(old,indent=2))

def tile(i):
    x,y=i%4*4,i//4*4
    return [x+.25,y+.25,x+3.75,y+3.75]
for e in bb['elements']:
    n=e['name']; material=0
    if any(s in n for s in ('plinth','chassis','housing','hood','deck')): material=0
    if any(s in n for s in ('bezel','guard','column','base','shaft')):material=1
    if 'foot' in n:material=2
    if 'side armor' in n:material=9
    if 'fascia' in n:material=4
    if 'service plate' in n:material=8
    if 'hatch' in n:material=8
    if 'vent' in n:material=4
    if 'control bank' in n or 'toggle' in n:material=7
    if 'Status light' in n or 'status rail' in n or 'power core' in n:material=6
    if 'Amber' in n:material=5
    if 'sensor port' in n:material=10
    if 'tip' in n:material=6
    if 'glass' in n:material=3
    e['autouv']=0
    for f in e['faces'].values(): f['uv']=tile(material);f['texture']=0
    if n=='Forward raised control hood':
        e['from']=[2.7,11.15,2.3];e['to']=[13.3,12.1,8.7]
    if n=='Radar display glass':e['from']=[4,12.12,3.3];e['to']=[12,12.25,7.7]
    if 'display bezel' in n:
        e['from'][1]=12.1;e['to'][1]=12.65
    if 'display' in n.lower() or n=='Forward raised control hood':
        e['rotation']=[-18,0,0];e['origin']=[8,11.7,5.5]
    if 'stabilizer column' in n:
        e['from'][1]=2;e['to'][1]=10
        e['rotation']=[0,0,-12 if n.startswith('West') else 12];e['origin']=[8,6,8]
    if 'vent plate' in n:
        e['from'][1]=7;e['to'][1]=8.6;e['from'][2]=2.25;e['to'][2]=2.4
    if n=='Deck status rail':e['from']=[4,10.1,8.8];e['to']=[12,10.6,9.2]
    if 'glass' in n: e['faces']['up']['uv']=[12.25,.25,15.75,3.75]
    if 'side armor' in n:e['faces']['west' if n.startswith('West') else 'east']['uv']=tile(4)
# Remove only faces proven fully buried by one opaque axis-aligned element.
removed=[]
for a in bb['elements']:
    if a.get('rotation'):continue
    for face in list(a['faces']):
        axis={'west':0,'east':0,'down':1,'up':1,'north':2,'south':2}[face]
        positive=face in ('east','up','south')
        plane=a['to' if positive else 'from'][axis];other=[i for i in range(3) if i!=axis]
        for b in bb['elements']:
            if b is a or b.get('rotation'):continue
            inside=(b['from'][axis]<=plane<b['to'][axis]) if positive else (b['from'][axis]<plane<=b['to'][axis])
            if inside and all(b['from'][i]<=a['from'][i] and b['to'][i]>=a['to'][i] for i in other):
                del a['faces'][face];removed.append([a['name'],face,b['name']]);break
faceless=[e['uuid'] for e in bb['elements'] if not e['faces']]
bb['elements']=[e for e in bb['elements'] if e['faces']]
bb['outliner']=[e for e in bb['outliner'] if e not in faceless]
texture=ASSETS/'textures/block/scanner_atlas.png'
bb['textures'][0]['source']='data:image/png;base64,'+base64.b64encode(texture.read_bytes()).decode()
bb['textures'][0].update(namespace='poisoncircle',folder='block',relative_path='../../src/main/resources/assets/poisoncircle/textures/block/scanner_atlas.png')
bb['display']=old.get('display',{})
SOURCE.write_text(json.dumps(bb,indent=2),encoding='utf-8')
model={'format_version':'26.3','credit':'Original Survey Console; native Blockbench blockout, explicit drawn atlas UV','textures':{'atlas':'poisoncircle:block/scanner_atlas','particle':'poisoncircle:block/scanner_atlas'},'elements':[],'display':bb['display']}
for e in bb['elements']:
    v={k:copy.deepcopy(e[k]) for k in ('name','from','to','faces')}
    for f in v['faces'].values():f['texture']='#atlas'
    if e.get('rotation'):
        axis=next(i for i,a in enumerate(e['rotation']) if a)
        v['rotation']={'origin':e['origin'],'axis':'xyz'[axis],'angle':e['rotation'][axis]}
    model['elements'].append(v)
(ASSETS/'models/block/poison_circle_scanner.json').write_text(json.dumps(model,indent=2),encoding='utf-8')
(OUT/'scanner-redesign-cleanup.json').write_text(json.dumps({'cubes':len(model['elements']),'declared_faces':sum(len(e['faces']) for e in model['elements']),'removed_fully_buried_faces':removed,'material_textures':1,'texture_size':[64,64],'runtime_item_definition_untouched':True,'limitations':['No client run or FPS measurement; angled opaque surfaces retained conservatively.']},indent=2))
# Reuse the project's deterministic cuboid rasterizer with rotation and 64px UV support.
sys.path.insert(0,str(ROOT/'src/test/python'))
source=(ROOT/'src/test/python/preview_item_models.py').read_text()
source=source.replace("vertices = (np.array(quads[face]) - 8) @ matrix.T", "vertices = np.array(quads[face], dtype=float)\n            if 'rotation' in e:\n                r=e['rotation']; origin=np.array(r['origin']); angles=[0,0,0]; angles['xyz'.index(r['axis'])]=r['angle']\n                vertices=(vertices-origin) @ rotation(*angles).T + origin\n            vertices = (vertices - 8) @ matrix.T")
source=source.replace("np.array(data['uv']) * 2","np.array(data['uv']) * 4").replace('np.clip(tex.astype(int),0,31)','np.clip(tex.astype(int),0,63)')
namespace={'__name__':'scanner_preview'};exec(compile(source,'scanner_preview','exec'),namespace)
render=namespace['render'];texture_array=namespace['np'].array(Image.open(texture).convert('RGBA'))
views=[('front',0,0),('rear',0,180),('west',0,90),('east',0,-90),('top',-90,0),('bottom',90,0),('front perspective',-26,30),('rear perspective',-26,210)]
sheet=Image.new('RGB',(960,1056),(32,39,49));d=ImageDraw.Draw(sheet)
for i,(label,rx,ry) in enumerate(views):
    for gray,row in ((False,0),(True,2)):
        x,y=i%4*240,(i//4+row)*264
        sheet.paste(render(model,texture_array,(rx,ry,0),gray),(x,y));d.text((x+8,y+242),label+(' / geometry' if gray else ''),fill='white')
sheet.save(OUT/'scanner-redesign-views.png')
hero=render(model,texture_array,(-28,30,0)).resize((960,960),Image.Resampling.NEAREST);hero.save(OUT/'scanner-redesign-hero.png')
print('Saved 39-cube scanner, explicit UV atlas, conservative buried-face cleanup, 8 views + geometry.')
