"""Generates the launcher PNGs (legacy devices) + 512px store icon.
The adaptive vector icon (res/drawable/ic_launcher_*.xml) uses the same geometry (108-unit grid)."""
import numpy as np
from PIL import Image, ImageDraw
import os, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")
SS = 4  # supersampling

def hexc(h): return tuple(int(h[i:i+2], 16) for i in (1, 3, 5))
A, B = hexc("#6A5AE0"), hexc("#18B4D8")
INDIGO, AMBER, DARK = hexc("#4F46E5"), hexc("#FFB020"), hexc("#3B2F9E")

def render(size, shape):
    n = size * SS
    k = n / 72.0           # crop window 18..90 of the 108 grid
    def P(x, y): return ((x - 18) * k, (y - 18) * k)
    # diagonal gradient
    yy, xx = np.mgrid[0:n, 0:n]
    t = (xx + yy) / (2.0 * (n - 1))
    arr = np.zeros((n, n, 4), dtype=np.uint8)
    for c in range(3):
        arr[..., c] = (A[c] + (B[c] - A[c]) * t).astype(np.uint8)
    arr[..., 3] = 255
    img = Image.fromarray(arr, "RGBA")
    d = ImageDraw.Draw(img)
    # soft highlight blobs
    ov = Image.new("RGBA", (n, n), (0, 0, 0, 0)); od = ImageDraw.Draw(ov)
    cx, cy = P(92, 14); r = 38 * k
    od.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(255, 255, 255, 34))
    cx, cy = P(10, 100); r = 30 * k
    od.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(255, 255, 255, 22))
    img = Image.alpha_composite(img, ov); d = ImageDraw.Draw(img)
    # bubble + tail
    x0, y0 = P(25, 27); x1, y1 = P(83, 71)
    d.rounded_rectangle([x0, y0, x1, y1], radius=14 * k, fill=(255, 255, 255, 255))
    d.polygon([P(40, 70), P(40, 84), P(50, 70)], fill=(255, 255, 255, 255))
    # letter E
    E = [(40,36),(66,36),(66,41),(47,41),(47,46.5),(62,46.5),(62,51.5),(47,51.5),(47,57),(66,57),(66,62),(40,62)]
    d.polygon([P(*p) for p in E], fill=INDIGO)
    # code badge
    bx, by = P(76, 69); r = 11 * k
    d.ellipse([bx - r - 1.6 * k, by - r - 1.6 * k, bx + r + 1.6 * k, by + r + 1.6 * k], fill=INDIGO)
    d.ellipse([bx - r, by - r, bx + r, by + r], fill=AMBER)
    w = max(1, int(2.2 * k))
    def line(pts):
        d.line([P(*p) for p in pts], fill=DARK, width=w, joint="curve")
        for p in (pts[0], pts[-1]):
            q = P(*p); d.ellipse([q[0]-w/2, q[1]-w/2, q[0]+w/2, q[1]+w/2], fill=DARK)
    line([(71.5,64.5),(68,69),(71.5,73.5)])
    line([(80.5,64.5),(84,69),(80.5,73.5)])
    line([(77.2,63.5),(74.8,74.5)])
    # mask
    mask = Image.new("L", (n, n), 0); md = ImageDraw.Draw(mask)
    if shape == "round": md.ellipse([0, 0, n - 1, n - 1], fill=255)
    else: md.rounded_rectangle([0, 0, n - 1, n - 1], radius=int(n * 0.22), fill=255)
    img.putalpha(mask)
    return img.resize((size, size), Image.LANCZOS)

dens = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for dname, px in dens.items():
    folder = os.path.join(ROOT, f"mipmap-{dname}"); os.makedirs(folder, exist_ok=True)
    render(px, "square").save(os.path.join(folder, "ic_launcher.png"))
    render(px, "round").save(os.path.join(folder, "ic_launcher_round.png"))
render(512, "square").save(os.path.join(ROOT, "..", "..", "..", "..", "icon-512.png"))
print("ok")
