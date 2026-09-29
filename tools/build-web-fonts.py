"""Rebuild the website's self-hosted fonts from the app's bundled font files.

    python tools/build-web-fonts.py     (needs fontTools and brotli)

Averia Sans Libre carries the Reserved Font Names 'Averia' and 'Averia Libre', and subsetting or converting it
would make a Modified Version that may not use that name (docs/design/ZINE-DIRECTION.md §16.5), so
its TTFs are copied byte for byte. Inter and Fraunces reserve no names, so they are subset to the characters a
European-language page needs and saved as WOFF2 (a Modified Version the OFL permits). Their name tables are kept.
Each licence text is copied beside the fonts, as the OFL requires.
"""
import shutil
from pathlib import Path

from fontTools import subset

ROOT = Path(__file__).resolve().parent.parent
APP_FONTS = ROOT / "core/ui/src/main/res/font"
LICENCES = ROOT / "feature/editor/src/main/assets/fonts"
OUT = ROOT / "website/assets/fonts"

# Basic Latin, Latin-1 and Latin Extended-A, plus the punctuation, arrows and symbols the website text uses.
UNICODES = [*range(0x20, 0x7F), *range(0xA0, 0x180), 0x2013, 0x2014, 0x2018, 0x2019, 0x201C, 0x201D,
            0x2022, 0x2026, 0x2032, 0x2039, 0x203A, 0x2190, 0x2192, 0x2212, 0x20AC, 0x2122, 0x2713, 0x00D7]


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for weight in ("regular", "bold"):
        shutil.copyfile(APP_FONTS / f"averia_sans_libre_{weight}.ttf", OUT / f"averia-sans-libre-{weight}.ttf")
    for family, weights in (("fraunces", ("regular", "medium")), ("inter", ("regular", "medium", "semibold", "bold"))):
        for weight in weights:
            options = subset.Options()
            options.flavor = "woff2"
            options.layout_features = ["*"]
            options.name_IDs = ["*"]
            options.name_languages = ["*"]
            options.notdef_outline = True
            font = subset.load_font(str(APP_FONTS / f"{family}_{weight}.ttf"), options)
            subsetter = subset.Subsetter(options)
            subsetter.populate(unicodes=UNICODES)
            subsetter.subset(font)
            subset.save_font(font, str(OUT / f"{family}-{weight}.woff2"), options)
    for licence in ("OFL-AveriaSansLibre.txt", "OFL-Fraunces.txt", "OFL-Inter.txt"):
        shutil.copyfile(LICENCES / licence, OUT / licence)


if __name__ == "__main__":
    main()
