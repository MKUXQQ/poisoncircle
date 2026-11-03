"""Draw an original orange-red energy curtain with bright flowing bands."""
import math
from pathlib import Path
from PIL import Image


def generate():
    size = 64
    image = Image.new('RGBA', (size, size))
    wrap = lambda n: math.tau * n / (size - 1)
    for x in range(size):
        for y in range(size):
            u, v = wrap(x), wrap(y)
            phase = v + 0.8 * math.sin(u) + 0.24 * math.sin(2 * u - v)
            # Bright orange crests sit inside a continuous, low-opacity warm haze.
            band = (0.5 + 0.5 * math.cos(phase)) ** 5
            fine = (0.5 + 0.5 * math.cos(2 * phase + 0.3 * math.sin(u))) ** 9
            glow = min(1.0, 0.82 * band + 0.18 * fine)
            red = round(224 + 31 * glow)
            green = round(54 + 108 * glow)
            blue = round(8 + 24 * glow)
            alpha = round(44 + 40 * glow)
            image.putpixel((x, y), (red, green, blue, alpha))
    alpha = image.getchannel('A').tobytes()
    assert 44 <= min(alpha) < max(alpha) <= 84
    assert len(set(alpha)) >= 30
    root = Path(__file__).resolve().parents[3]
    target = root / 'src/main/resources/assets/poisoncircle/textures/environment/circle_wall.png'
    image.save(target, optimize=True)
    print(f'Orange-red energy curtain: {size}x{size}, 100% coverage, alpha={min(alpha)}..{max(alpha)}, one scrolling surface')


if __name__ == '__main__':
    generate()
