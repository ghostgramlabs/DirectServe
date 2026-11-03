"""Builds Play Store assets from raw emulator captures.

Usage: python -I make_store.py <raw_dir> <out_dir>
Outputs: phone screenshots (1080x1920), feature graphic (1024x500), hi-res icon (512x512).
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

BLUE = (3, 105, 161)
BLUE_DARK = (7, 89, 133)
SKY = (125, 211, 252)
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


def background(size):
    w, h = size
    bg = Image.new("RGB", size, BLUE)
    d = ImageDraw.Draw(bg)
    d.ellipse([w * 0.45, h * 0.55, w * 1.5, h * 1.35], fill=BLUE_DARK)
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
        d.text((W / 2, y), line, font=h2, fill=(224, 242, 254), anchor="ma")
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


def mark(draw, ox, oy, s, wave=SKY):
    """The launcher mark drawn at scale s (108-unit viewport) with origin (ox, oy)."""
    def p(x, y):
        return (ox + x * s, oy + y * s)
    # Android centres strokes on the path; PIL draws them inside the box, so grow boxes by half.
    h = 2.5
    sw = round(5 * s)
    draw.rounded_rectangle([*p(26 - h, 44 - h), *p(64 + h, 74 + h)], radius=(6 + h) * s, outline=WHITE, width=sw)
    draw.polygon([p(40.5, 52.5), p(40.5, 65.5), p(51, 59)], fill=WHITE)
    for r, color in ((8, WHITE), (15, wave)):
        draw.arc([*p(66 - r - h, 42 - r - h), *p(66 + r + h, 42 + r + h)], 270, 360, fill=color, width=sw)
        for cx, cy in ((66, 42 - r), (66 + r, 42)):  # round line caps
            draw.ellipse([*p(cx - h, cy - h), *p(cx + h, cy + h)], fill=color)


def icon(out: Path) -> None:
    """512px Play icon showing the same centre 72/108 region a launcher mask shows."""
    size, k = 512, 4
    s = size * k / 72
    off = -18 * s  # the visible region starts 18 units into the 108 canvas
    big = Image.new("RGB", (size * k, size * k), BLUE)
    d = ImageDraw.Draw(big)
    d.ellipse([off + 20 * s, off + 20 * s, off + 88 * s, off + 88 * s], fill=(11, 124, 189))
    mark(d, off, off, s)
    big.resize((size, size), Image.LANCZOS).save(out, "PNG")


def feature_graphic(home: Path, out: Path) -> None:
    W, H = 1024, 500
    canvas = background((W, H))
    big = Image.new("RGBA", (W * 2, H * 2), (0, 0, 0, 0))
    mark(ImageDraw.Draw(big), 40 * 2, 70 * 2, 2.1 * 2)
    canvas.alpha_composite(big.resize((W, H), Image.LANCZOS))
    d = ImageDraw.Draw(canvas)
    d.text((60, 270), "DirectServe", font=font("segoeuib.ttf", 64), fill=WHITE)
    d.text((62, 352), "Share files and stream videos", font=font("segoeui.ttf", 32), fill=(224, 242, 254))
    d.text((62, 394), "to any browser on your Wi-Fi", font=font("segoeui.ttf", 32), fill=(224, 242, 254))
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
