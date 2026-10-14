"""Generate the Android launcher icon from tools/app-icon/source.png.

Usage (from the repo root; needs Pillow):
    python3 tools/app-icon/generate.py tools/app-icon/source.png \
        app/src/main/res /tmp/icon-preview.png

Writes an adaptive icon (minSdk is 26, so every device uses it):
  mipmap-*dpi/ic_launcher_background.png  gradient from the tile's corner blues
  mipmap-*dpi/ic_launcher_foreground.png  the tile, border feathered, at SCALE
                                          of the 108dp canvas
and a preview of the icon under circle and rounded-square launcher masks.
mipmap-anydpi-v26/ic_launcher{,_round}.xml pair the two layers.
"""
import sys, os
from PIL import Image, ImageChops, ImageDraw, ImageFilter
src, res, preview = sys.argv[1], sys.argv[2], sys.argv[3]
im = Image.open(src).convert("RGBA")
im = im.crop(im.getbbox())
w, h = im.size
# Square it: pad the shorter side with transparency.
s = max(w, h)
sq = Image.new("RGBA", (s, s)); sq.paste(im, ((s - w)//2, (s - h)//2)); im = sq

# Background: bilinear gradient from the tile's own corner blues.
def avg(x0, y0, x1, y1):
    px = [im.getpixel((x, y)) for x in range(x0, x1) for y in range(y0, y1)]
    px = [p for p in px if p[3] == 255]
    return tuple(sum(c[i] for c in px)//len(px) for i in range(3))
m = 14
tl, tr = avg(m, m, m+8, m+8), avg(s-m-8, m, s-m, m+8)
bl, br = avg(m, s-m-8, m+8, s-m), avg(s-m-8, s-m-8, s-m, s-m)
print("corners", tl, tr, bl, br)

# Foreground: the tile with its rounded border feathered to transparent,
# so it melts into the gradient behind it.
fa = Image.new("L", (s, s), 0)
inset = 6
ImageDraw.Draw(fa).rounded_rectangle((inset, inset, s-inset, s-inset), radius=int(s*0.16), fill=255)
fa = fa.filter(ImageFilter.GaussianBlur(5))
fg_src = im.copy()
fg_src.putalpha(ImageChops.multiply(im.getchannel("A"), fa))

SCALE = 0.68  # tile size as a fraction of the 108dp adaptive canvas
dens = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
for name, f in dens.items():
    n = round(108 * f)
    bg = Image.new("RGB", (n, n))
    for y in range(n):
        for x in range(n):
            u, v = x/(n-1), y/(n-1)
            bg.putpixel((x, y), tuple(round(tl[i]*(1-u)*(1-v) + tr[i]*u*(1-v) + bl[i]*(1-u)*v + br[i]*u*v) for i in range(3)))
    t = round(n * SCALE)
    fg = Image.new("RGBA", (n, n))
    fg.paste(fg_src.resize((t, t), Image.LANCZOS), ((n-t)//2, (n-t)//2))
    d = os.path.join(res, f"mipmap-{name}"); os.makedirs(d, exist_ok=True)
    bg.save(os.path.join(d, "ic_launcher_background.png"), optimize=True)
    fg.save(os.path.join(d, "ic_launcher_foreground.png"), optimize=True)
    if name == "xxxhdpi":
        comp = bg.convert("RGBA"); comp.alpha_composite(fg)
        # Preview: circle mask and rounded-square mask, as launchers apply them.
        out = Image.new("RGBA", (2*n + 60, n + 40), (235, 235, 235, 255))
        for k, shape in enumerate(("circle", "squircle")):
            mk = Image.new("L", (n, n), 0); dr = ImageDraw.Draw(mk)
            vis = (n - n*72//108)//2
            if shape == "circle": dr.ellipse((vis, vis, n-vis, n-vis), fill=255)
            else: dr.rounded_rectangle((vis, vis, n-vis, n-vis), radius=n//6, fill=255)
            out.paste(comp, (20 + k*(n+20), 20), mk)
        out.save(preview)
