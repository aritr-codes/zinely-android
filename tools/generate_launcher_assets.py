"""Generate deterministic Android launcher assets from the owner-supplied APP_LOGO.png.

The source artwork is never recoloured or redrawn. Legacy density assets are resized copies;
round variants only add the platform-expected circular alpha mask. Adaptive and splash resources
consume a lossless WebP projection whose decoded pixels are verified against the source.
"""

from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageStat


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "APP_LOGO.png"
RES = ROOT / "app" / "src" / "main" / "res"
LEGACY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def edge_colour(image: Image.Image) -> str:
    """Return the mean RGB of a narrow perimeter strip for a seamless splash ground."""
    rgb = image.convert("RGB")
    width, height = rgb.size
    strip = max(1, min(width, height) // 128)
    edge = Image.new("RGB", (width * 2 + height * 2, strip))
    cursor = 0
    for crop in (
        rgb.crop((0, 0, width, strip)),
        rgb.crop((0, height - strip, width, height)),
        rgb.crop((0, 0, strip, height)).rotate(90, expand=True),
        rgb.crop((width - strip, 0, width, height)).rotate(90, expand=True),
    ):
        edge.paste(crop, (cursor, 0))
        cursor += crop.width
    mean = tuple(round(channel) for channel in ImageStat.Stat(edge.crop((0, 0, cursor, strip))).mean)
    return "#" + "".join(f"{channel:02X}" for channel in mean)


def main() -> None:
    source = Image.open(SOURCE).convert("RGB")
    if source.width != source.height:
        raise ValueError(f"APP_LOGO.png must be square, got {source.size}")

    drawable = RES / "drawable-nodpi"
    drawable.mkdir(parents=True, exist_ok=True)
    adaptive_artwork = drawable / "app_logo.webp"
    source.save(adaptive_artwork, "WEBP", lossless=True, method=6)
    with Image.open(adaptive_artwork) as decoded:
        difference = ImageChops.difference(source, decoded.convert("RGB"))
        if difference.getbbox() is not None:
            raise ValueError("Lossless WebP launcher projection changed decoded artwork pixels")

    # The resource name stays app_logo; remove the superseded generated format so Android cannot
    # package both projections if the generator is rerun over an older checkout.
    legacy_png = drawable / "app_logo.png"
    if legacy_png.exists():
        legacy_png.unlink()

    for density, size in LEGACY_SIZES.items():
        target = RES / f"mipmap-{density}"
        target.mkdir(parents=True, exist_ok=True)
        resized = source.resize((size, size), Image.Resampling.LANCZOS)
        resized.save(target / "ic_launcher.webp", "WEBP", quality=95, method=6)

        round_icon = resized.convert("RGBA")
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
        round_icon.putalpha(mask)
        round_icon.save(target / "ic_launcher_round.webp", "WEBP", quality=95, method=6)

    print(
        f"source={source.size[0]}x{source.size[1]} edge={edge_colour(source)} "
        f"adaptive={adaptive_artwork.name}:{adaptive_artwork.stat().st_size}B pixel_identical=true"
    )


if __name__ == "__main__":
    main()
