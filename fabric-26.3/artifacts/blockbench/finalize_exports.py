"""Pack only solid colors and remove fully enclosed axis-aligned faces; never rebuild geometry."""
import base64
import copy
import io
import json
from pathlib import Path
import sys
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets'
OUT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / 'src/test/python'))
import preview_item_models as preview


def hidden(element, face, others):
    if element.get('rotation'): return False
    axis, positive = {'west': (0,False), 'east': (0,True), 'down': (1,False), 'up': (1,True), 'north': (2,False), 'south': (2,True)}[face]
    plane = element['to' if positive else 'from'][axis]
    for other in others:
        if other is element or other.get('rotation'): continue
        if not all(other['from'][i] <= element['from'][i] and other['to'][i] >= element['to'][i] for i in range(3) if i != axis): continue
        if positive and other['from'][axis] <= plane < other['to'][axis]: return True
        if not positive and other['from'][axis] < plane <= other['to'][axis]: return True
    return False


def main():
    for kind, folder in [('scanner','block'), ('lobby','item')]:
        path = ASSETS / f'poisoncircle/models/{folder}/poison_circle_{kind}.json'
        model = json.loads(path.read_text())
        baseline = copy.deepcopy(model)
        colors = {}
        for key, value in model['textures'].items():
            if key == 'particle': continue
            image = Image.open(ASSETS / (value.replace(':','/textures/')+'.png')).convert('RGBA')
            unique = image.getcolors()
            assert unique and len(unique) == 1 and unique[0][1][3] == 255
            colors[key] = unique[0][1]
        assert len(colors) == 3, 'Run once on original Blockbench export'
        atlas = Image.new('RGBA',(16,16), colors['0'])
        draw = ImageDraw.Draw(atlas)
        rects = [(0,0,7,7),(8,0,15,7),(0,8,7,15)]
        uv = {}
        for i,(key,color) in enumerate(colors.items()):
            draw.rectangle(rects[i],fill=color)
            x,y,_,_ = rects[i]; uv[key] = [x+2,y+2,x+6,y+6]
        removed = []
        for element in model['elements']:
            for face in list(element['faces']):
                if hidden(element,face,model['elements']):
                    removed.append([element['name'],face]); del element['faces'][face]
                else:
                    data = element['faces'][face]
                    data['uv'] = uv[data['texture'].lstrip('#')]
                    data['texture'] = '#atlas'
        ref = f'poisoncircle:{folder}/{kind}_atlas'
        model['textures'] = {'atlas':ref,'particle':ref}
        assert [{k:v for k,v in e.items() if k!='faces'} for e in model['elements']] == [{k:v for k,v in e.items() if k!='faces'} for e in baseline['elements']]
        atlas.save(ASSETS / f'poisoncircle/textures/{folder}/{kind}_atlas.png')
        path.write_text(json.dumps(model,indent=2)+'\n')
        # Keep the actual editor snapshot intact; create an independently editable packed counterpart.
        bb = json.loads((OUT/f'{kind}.bbmodel').read_text())
        by_name = {e['name']:e for e in model['elements']}
        for element in bb['elements']:
            runtime = by_name[element['name']]
            element['autouv'] = 0
            for face,data in element['faces'].items():
                if face not in runtime['faces']: data['texture'] = None
                else: data.update(uv=runtime['faces'][face]['uv'],texture=0)
        tex = bb['textures'][0]
        stream = io.BytesIO(); atlas.save(stream,format='PNG')
        tex.update(name=ref,id='0',path='',source='data:image/png;base64,'+base64.b64encode(stream.getvalue()).decode(),width=16,height=16,uv_width=16,uv_height=16)
        bb['textures'] = [tex]
        (OUT/f'{kind}-packed.bbmodel').write_text(json.dumps(bb,indent=2)+'\n')
        (OUT/f'{kind}-cleanup.json').write_text(json.dumps({'removed_fully_hidden_faces':removed,'original_faces':sum(len(e['faces']) for e in baseline['elements']),'final_faces':sum(len(e['faces']) for e in model['elements']),'geometry_unchanged':True,'atlas_size':[16,16]},indent=2)+'\n')
        # Render the existing cuboid source with its original element rotations baked into vertices.
        render_model = copy.deepcopy(model)
        texture = np.array(atlas.resize((32,32),Image.Resampling.NEAREST))
        sheet = Image.new('RGB',(960,528),(32,39,49)); labels=ImageDraw.Draw(sheet)
        original_render = preview.render
        for i,(label,rx,ry) in enumerate(preview.VIEWS):
            image = render_rotated(render_model,texture,(rx,ry,0))
            sheet.paste(image,((i%4)*240,(i//4)*264))
            labels.text(((i%4)*240+8,(i//4)*264+242),label,fill='white')
        sheet.save(OUT/f'{kind}-views.png')
        print(kind, len(model['elements']),'cuboids',sum(len(e['faces']) for e in model['elements']),'faces; removed',removed)


# Use the existing rasterizer, changing only the per-element vertex transform.
source = (ROOT/'src/test/python/preview_item_models.py').read_text()
start = source.index('def render('); end = source.index('\n\ndef main()',start)
render_source = source[start:end].replace('def render(', 'def render_rotated(').replace("vertices = (np.array(quads[face]) - 8) @ matrix.T", "points = np.array(quads[face], dtype=float)\n            if e.get('rotation'):\n                r = e['rotation']; angles = [0,0,0]; angles['xyz'.index(r['axis'])] = r['angle']\n                origin = np.array(r['origin'])\n                points = (points-origin) @ preview.rotation(*angles).T + origin\n            vertices = (points - 8) @ matrix.T")
rotation = preview.rotation
exec(render_source)
if __name__ == '__main__': main()
