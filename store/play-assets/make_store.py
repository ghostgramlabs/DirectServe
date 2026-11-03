"""Builds Play Store assets from raw emulator captures.

Usage: python -I make_store.py <raw_dir> <out_dir>
Outputs: phone screenshots (1080x1920), feature graphic (1024x500), hi-res icon (512x512).
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

BLUE = (30, 41, 59)
BLUE_DARK = (15, 23, 42)
SKY = (125, 211, 252)
TEAL = (13, 148, 136)
TEAL_DARK = (17, 94, 89)
WHITE = (255, 255, 255)
FONTS = "C:/Windows/Fonts/"

# (raw file, headline, sub line)
SHOTS = [
    ("home.png", "Share in 2 taps", "Pick what to share, tap Start. That's it."),
    ("session.png", "Scan and open in any browser", "TV, laptop, tablet or phone. No app needed."),
    ("laptop.png", "Open it on any screen", "The same share on a laptop or TV browser."),
    ("browser.png", "Watch and download in the browser", "Videos, photos, music and files."),
    ("player.png", "Plays straight from your phone", "Direct play first, prepared only when needed."),
    ("mirror.png", "Mirror your screen live", "Show anything on a bigger screen."),
    ("library.png", "Always know what you're sharing", "Search, filter, remove in one tap."),
    ("dark.png", "Private and offline", "Everything stays on your own Wi-Fi."),
]


def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    for candidate in (name, "segoeuib.ttf", "arialbd.ttf"):
        try:
            return ImageFont.truetype(FONTS + candidate, size)
        except OSError:
            continue
    return ImageFont.load_default()


def wrap(draw, text, fnt, width):
    words, lines, line = text.split(), [], ""
    for w in words:
        trial = (line + " " + w).strip()
        if draw.textlength(trial, font=fnt) <= width:
            line = trial
        else:
            lines.append(line)
            line = w
    lines.append(line)
    return lines


def rounded(im: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, im.width - 1, im.height - 1], radius, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def phone(shot: Image.Image, height: int) -> Image.Image:
    """Screenshot inside a simple dark bezel with a drop shadow."""
    scale = height / shot.height
    screen = shot.convert("RGB").resize((round(shot.width * scale), height), Image.LANCZOS)
    bezel = 14
    body = Image.new("RGBA", (screen.width + bezel * 2, screen.height + bezel * 2), (0, 0, 0, 0))
    ImageDraw.Draw(body).rounded_rectangle([0, 0, body.width - 1, body.height - 1], 62, fill=(15, 23, 42, 255))
    body.alpha_composite(rounded(screen, 50), (bezel, bezel))
    shadow = Image.new("RGBA", (body.width + 80, body.height + 80), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([40, 52, body.width + 40, body.height + 52], 62, fill=(0, 0, 0, 90))
    shadow = shadow.filter(ImageFilter.GaussianBlur(22))
    shadow.alpha_composite(body, (40, 40))
    return shadow


def laptop(shot: Image.Image, width: int) -> Image.Image:
    """Landscape browser capture on a simple laptop: dark lid, screen, and a base."""
    bezel = 18
    screen_w = width - bezel * 2
    screen = shot.convert("RGB").resize((screen_w, round(shot.height * screen_w / shot.width)), Image.LANCZOS)
    lid_h = screen.height + bezel * 2
    base_h = 26
    out = Image.new("RGBA", (width, lid_h + base_h + 40), (0, 0, 0, 0))
    d = ImageDraw.Draw(out)
    d.rounded_rectangle([0, 0, width - 1, lid_h], 26, fill=(15, 23, 42, 255))
    out.alpha_composite(rounded(screen, 8), (bezel, bezel))
    d.rounded_rectangle([-40 + 40, lid_h, width - 1, lid_h + base_h], 12, fill=(203, 213, 225, 255))
    d.rounded_rectangle([width // 2 - 70, lid_h, width // 2 + 70, lid_h + 8], 4, fill=(148, 163, 184, 255))
    return out


def background(size, base=None, shade=None):
    w, h = size
    bg = Image.new("RGB", size, base or BLUE)
    d = ImageDraw.Draw(bg)
    d.ellipse([w * 0.45, h * 0.55, w * 1.5, h * 1.35], fill=shade or BLUE_DARK)
    return bg.convert("RGBA")


def screenshot(raw: Path, headline: str, sub: str, out: Path) -> None:
    W, H = 1080, 1920
    canvas = background((W, H))
    d = ImageDraw.Draw(canvas)
    h1, h2 = font("segoeuib.ttf", 76), font("segoeui.ttf", 40)
    y = 110
    for line in wrap(d, headline, h1, W - 140):
        d.text((W / 2, y), line, font=h1, fill=WHITE, anchor="ma")
        y += 92
    y += 10
    for line in wrap(d, sub, h2, W - 160):
        d.text((W / 2, y), line, font=h2, fill=(203, 213, 225), anchor="ma")
        y += 54
    shot = Image.open(raw)
    if shot.width > shot.height:
        # Laptop near the top, with the phone's QR screen overlapping its corner: "scan here, open there".
        device = laptop(shot, W - 40)
        canvas.alpha_composite(device, (20, y + 40))
        qr = raw.parent / "session.png"
        if qr.exists():
            handset = phone(Image.open(qr), H - y - device.height + 260)
            canvas.alpha_composite(handset, (W - handset.width + 10, H - handset.height + 40))
    else:
        device = phone(shot, H - y - 40)
        canvas.alpha_composite(device, ((W - device.width) // 2, y + 10))
    canvas.convert("RGB").save(out, "PNG", optimize=True)


def mark(draw, ox, oy, s, cut=TEAL):
    """The launcher mark at scale s (108-unit viewport) with origin (ox, oy): a phone handing off
    to a big screen. The play triangle and home bar are drawn in `cut` so they read as holes."""
    def p(x, y):
        return (ox + x * s, oy + y * s)
    draw.rounded_rectangle([*p(46, 30.5), *p(82, 60.5)], radius=4 * s, fill=WHITE)
    draw.polygon([p(59, 38.5), p(59, 52.5), p(70, 45.5)], fill=cut)
    draw.rounded_rectangle([*p(28.5, 48), *p(43.5, 75)], radius=3 * s, fill=WHITE)
    draw.rounded_rectangle([*p(33, 69.5), *p(39, 71.5)], radius=1 * s, fill=cut)


def icon(out: Path) -> None:
    """512px Play icon showing the same centre 72/108 region a launcher mask shows."""
    size, k = 512, 4
    s = size * k / 72
    off = -18 * s  # the visible region starts 18 units into the 108 canvas
    big = Image.new("RGB", (size * k, size * k), TEAL)
    mark(ImageDraw.Draw(big), off, off, s)
    big.resize((size, size), Image.LANCZOS).save(out, "PNG")


def feature_graphic(home: Path, out: Path) -> None:
    W, H = 1024, 500
    canvas = background((W, H), TEAL, TEAL_DARK)
    big = Image.new("RGBA", (W * 2, H * 2), (0, 0, 0, 0))
    mark(ImageDraw.Draw(big), 10 * 2, 20 * 2, 3.2 * 2)
    canvas.alpha_composite(big.resize((W, H), Image.LANCZOS))
    d = ImageDraw.Draw(canvas)
    d.text((60, 270), "DirectServe", font=font("segoeuib.ttf", 64), fill=WHITE)
    d.text((62, 352), "Cast to TV, mirror your screen", font=font("segoeui.ttf", 32), fill=(204, 251, 241))
    d.text((62, 394), "and share files over Wi-Fi", font=font("segoeui.ttf", 32), fill=(204, 251, 241))
    device = phone(Image.open(home), 470)
    canvas.alpha_composite(device.crop((0, 0, device.width, H - 10)), (W - device.width - 30, 20))
    canvas.convert("RGB").save(out, "PNG", optimize=True)


def main(raw_dir: str, out_dir: str) -> None:
    raw, out = Path(raw_dir), Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    n = 0
    for name, headline, sub in SHOTS:
        if (raw / name).exists():
            n += 1
            screenshot(raw / name, headline, sub, out / f"phone_{n:02d}_{name}")
        else:
            print("skip (no capture):", name)
    icon(out / "icon_512.png")
    if (raw / "home.png").exists():
        feature_graphic(raw / "home.png", out / "feature_graphic_1024x500.png")
    print("wrote", sorted(p.name for p in out.iterdir()))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
