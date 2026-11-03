"""Original seamless ion storm: broad wandering currents, sparse hot pulses, no noise."""
import math
from pathlib import Path
from PIL import Image

SIZE = 128
ROOT = Path(__file__).resolve().parents[3]
TARGET = ROOT / 'src/main/resources/assets/poisoncircle/textures/environment/circle_wall.png'


def periodic_distance(a, b):
    return (a - b + 0.5) % 1.0 - 0.5


def sample(u, v):
    s, t = math.tau * u, math.tau * v
    # Hand-composed broad channels change direction and fork, rather than making parallel waves.
    paths = [([(-.10, .15), (.10, .23), (.26, .17), (.33, .41), (.55, .54), (.71, .48), (.81, .79), (.90, 1.15)], .037),
             ([(.33, .41), (.48, .19), (.70, .22), (.78, -.08)], .022)]
    distance = 1.0
    ridge = 0.0
    for points, width in paths:
        for (ax, ay), (bx, by) in zip(points, points[1:]):
            vx, vy = bx - ax, by - ay
            for tx in (-1, 0, 1):
                for ty in (-1, 0, 1):
                    px, py = u + tx - ax, v + ty - ay
                    along = max(0.0, min(1.0, (px * vx + py * vy) / (vx * vx + vy * vy)))
                    d = math.hypot(px - along * vx, py - along * vy)
                    distance = min(distance, d)
                    ridge = max(ridge, math.exp(-(d / (width * (0.75 + .5 * along))) ** 2))
    cloud = 0.5 + 0.5 * math.sin(s + .7 * math.sin(t)) * math.sin(t - .4 * math.cos(s))
    shoulder = math.exp(-(distance / .11) ** 2)
    flow = min(1.0, .78 * ridge + .18 * shoulder)
    # Two hot reaches are long along the current, not isolated pinpoints.
    pulse_gate = math.exp(-(periodic_distance(v, .30) / .075) ** 2) + .8 * math.exp(-(periodic_distance(v, .68) / .09) ** 2)
    pulse = min(1.0, ridge * pulse_gate)
    heat = min(1.0, .75 * flow + .6 * pulse)
    return (round(144 + 30 * cloud + 81 * min(1.0, flow + pulse)),
            round(23 + 14 * cloud + 100 * heat + 30 * pulse),
            round(18 + 5 * cloud + 13 * heat + 8 * pulse),
            round(48 + 9 * cloud + 32 * flow + 17 * pulse))


def generate():
    image = Image.new('RGBA', (SIZE, SIZE))
    image.putdata([sample(x / (SIZE - 1), y / (SIZE - 1))
                   for y in range(SIZE) for x in range(SIZE)])
    assert image.getchannel('A').getextrema()[0] > 0
    assert all(image.getpixel((0, n)) == image.getpixel((SIZE - 1, n)) for n in range(SIZE))
    assert all(image.getpixel((n, 0)) == image.getpixel((n, SIZE - 1)) for n in range(SIZE))
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    image.save(TARGET, optimize=True)
    print(f'Ion storm: {SIZE}x{SIZE}, RGBA, alpha={image.getchannel("A").getextrema()}, 100% coverage, one scrolling surface')


if __name__ == '__main__':
    generate()
