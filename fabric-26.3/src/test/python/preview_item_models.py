"""Deterministic offline orthographic Java-cuboid previews (not a game renderer)."""
import json
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw
from generate_item_models import ASSETS, ROOT

OUT = ROOT / 'artifacts/item-models'
VIEWS = [('front', 0, 0), ('back', 0, 180), ('left', 0, 90), ('right', 0, -90),
         ('top', 90, 0), ('bottom', -90, 0), ('three-quarter', 24, 210)]


def rotation(rx, ry, rz=0):
    x, y, z = np.radians([rx, ry, rz])
    a = np.array([[1, 0, 0], [0, np.cos(x), -np.sin(x)], [0, np.sin(x), np.cos(x)]])
    b = np.array([[np.cos(y), 0, np.sin(y)], [0, 1, 0], [-np.sin(y), 0, np.cos(y)]])
    c = np.array([[np.cos(z), -np.sin(z), 0], [np.sin(z), np.cos(z), 0], [0, 0, 1]])
    return c @ a @ b


def render(model, texture, angles, gray=False, scale=1):
    size, zoom = 240, 11 * scale
    pixels = np.full((size, size, 3), [32, 39, 49], dtype=np.uint8)
    depth = np.full((size, size), np.inf)
    matrix = rotation(*angles)
    lighting = {'north': 1, 'south': .8, 'east': .8, 'west': .7, 'up': 1, 'down': .6}
    for e in model['elements']:
        x, y, z = e['from']; X, Y, Z = e['to']
        quads = {'north': [(X,Y,z),(x,Y,z),(x,y,z),(X,y,z)],
                 'south': [(x,Y,Z),(X,Y,Z),(X,y,Z),(x,y,Z)],
                 'west': [(x,Y,z),(x,Y,Z),(x,y,Z),(x,y,z)],
                 'east': [(X,Y,Z),(X,Y,z),(X,y,z),(X,y,Z)],
                 'up': [(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],
                 'down': [(x,y,Z),(X,y,Z),(X,y,z),(x,y,z)]}
        for face, data in e['faces'].items():
            vertices = (np.array(quads[face]) - 8) @ matrix.T
            vertices[:, 0] = vertices[:, 0] * zoom + size/2
            vertices[:, 1] = -vertices[:, 1] * zoom + size/2
            u, v, U, V = np.array(data['uv']) * 2
            uv = np.array([(u,v),(U,v),(U,V),(u,V)])
            for indexes in ((0,1,2),(0,2,3)):
                p = vertices[list(indexes)]
                lo = np.maximum(np.floor(p[:,:2].min(axis=0)).astype(int),0)
                hi = np.minimum(np.ceil(p[:,:2].max(axis=0)).astype(int),size-1)
                xx, yy = np.meshgrid(np.arange(lo[0], hi[0]+1),np.arange(lo[1],hi[1]+1))
                den = (p[1,1]-p[2,1])*(p[0,0]-p[2,0])+(p[2,0]-p[1,0])*(p[0,1]-p[2,1])
                if abs(den) < 1e-8: continue
                w0 = ((p[1,1]-p[2,1])*(xx+.5-p[2,0])+(p[2,0]-p[1,0])*(yy+.5-p[2,1]))/den
                w1 = ((p[2,1]-p[0,1])*(xx+.5-p[2,0])+(p[0,0]-p[2,0])*(yy+.5-p[2,1]))/den
                w2 = 1-w0-w1
                zz = w0*p[0,2]+w1*p[1,2]+w2*p[2,2]
                mask = (w0>=-1e-8)&(w1>=-1e-8)&(w2>=-1e-8)&(zz<depth[yy,xx])
                tex = w0[...,None]*uv[indexes[0]]+w1[...,None]*uv[indexes[1]]+w2[...,None]*uv[indexes[2]]
                tx, ty = np.clip(tex.astype(int),0,31).transpose(2,0,1)
                colors = texture[ty,tx,:3] if not gray else np.full((*xx.shape,3),190)
                mask &= texture[ty,tx,3] == 255
                pixels[yy[mask],xx[mask]] = np.clip(colors[mask]*lighting[face],0,255).astype(np.uint8)
                depth[yy[mask],xx[mask]] = zz[mask]
    return Image.fromarray(pixels)


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    for kind in ('controller','detector'):
        name = 'poison_circle_'+kind
        model = json.loads((ASSETS/'models/item'/(name+'.json')).read_text())
        texture = np.array(Image.open(ASSETS/'textures/item'/(name+'.png')).convert('RGBA'))
        sheet = Image.new('RGB',(240*4,264*4),(32,39,49))
        draw = ImageDraw.Draw(sheet)
        for i,(label,rx,ry) in enumerate(VIEWS):
            for gray, row in ((False,0),(True,2)):
                index = i
                col, line = index%4, index//4
                image = render(model,texture,(rx,ry,0),gray)
                sheet.paste(image,(col*240,(line+row)*264))
                draw.text((col*240+8,(line+row)*264+242),label+(' / geometry' if gray else ''),fill='white')
        sheet.save(OUT/(kind+'-views.png'))
        slots = Image.new('RGB',(240*4,264*2),(32,39,49)); sd = ImageDraw.Draw(slots)
        for i,(slot,display) in enumerate(model['display'].items()):
            angles = list(display['rotation'])
            # Front camera faces north; GUI/fixed standard transforms face the screen.
            angles[1] = angles[1]-180
            image = render(model,texture,angles,scale=display['scale'][0])
            slots.paste(image,((i%4)*240,(i//4)*264))
            sd.text(((i%4)*240+8,(i//4)*264+242),slot,fill='white')
        slots.save(OUT/(kind+'-display.png'))
    wall = Image.open(ASSETS/'textures/environment/circle_wall.png')
    fence = Image.new('RGBA',(256,256),(32,39,49,255))
    for x in range(0,256,64):
        for y in range(0,256,64): fence.alpha_composite(wall,(x,y))
    fence.save(OUT/'wall-tiled.png')
    print('Offline views, geometry and eight display orientation previews:',OUT)


if __name__ == '__main__':
    main()
