"""Fetch the four static Fraunces faces for the Book document voice, and measure them against Inter.

    python tools/build-document-fonts.py            fetch into docs/planning/voices/ and check the pinned hashes
    python tools/build-document-fonts.py --measure  no download: check the files on disk against the pins, then
                                                    print coverage and size against the bundled Inter faces

The faces are upstream's own 9 pt statics, byte for byte: nothing is instanced, subset or renamed, so they are
not a Modified Version under the OFL. Upstream is undercasetype/Fraunces. (googlefonts/fraunces is an archived
fork that stopped at Version 1.000 / 1.001, whose static italics lack # $ £ and đ.) The 9 pt cut is the one the
interface already ships: Fraunces9pt-Regular.ttf here is the same file as core/ui/src/main/res/font/
fraunces_regular.ttf.

Fetching needs only the standard library; --measure needs fontTools.
The output is a preparation asset. Nothing in the app build reads docs/planning/voices/.
"""
import hashlib
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/planning/voices"
INTER = ROOT / "render-android/src/main/assets/fonts"

COMMIT = "7ccdec31c6028118dce3e47fe864e3744460371d"  # undercasetype/Fraunces master, 2025-10-21; fonts are Version 1.003
BASE = f"https://raw.githubusercontent.com/undercasetype/Fraunces/{COMMIT}/"
PINNED = {  # file written to OUT -> (path upstream, SHA-256)
    "Fraunces9pt-Regular.ttf": ("fonts/ttf/Fraunces9pt-Regular.ttf",
                                "54f7ec6290e8ddb967e1ebddd2cadb706d6b448254e3489c04f2bcd265db5fa2"),
    "Fraunces9pt-Bold.ttf": ("fonts/ttf/Fraunces9pt-Bold.ttf",
                             "f27f6cf1c5f4454c257cbaa564199ef2e74462f55c43c9087b9a7636445a71cd"),
    "Fraunces9pt-Italic.ttf": ("fonts/ttf/Fraunces9pt-Italic.ttf",
                               "9e6c9152ac9f12351f42c9d0e578702966a1d0d83d0dfb69ae22b0eeee470f20"),
    "Fraunces9pt-BoldItalic.ttf": ("fonts/ttf/Fraunces9pt-BoldItalic.ttf",
                                   "63902967e568c654a3df60acdf3c35018be0df3f550c8c9a931c7a022b5a8bed"),
    "OFL-Fraunces.txt": ("OFL.txt", "bdf4c22802eaf804f998195871c6b8938aac2ac14b2d78a8bd66a6f1eced833b"),
}
FACES = [name for name in PINNED if name.endswith(".ttf")]


def fetch():
    """Download every pinned file, and write none of them unless all of them match."""
    files = {}
    for name, (path, pinned) in PINNED.items():
        data = urllib.request.urlopen(BASE + path, timeout=60).read()
        actual = hashlib.sha256(data).hexdigest()
        if actual != pinned:
            sys.exit(f"{path}: upstream bytes are not the pinned ones ({actual}); nothing was written")
        files[name] = data
    OUT.mkdir(parents=True, exist_ok=True)
    for name, data in files.items():
        (OUT / name).write_bytes(data)
        print(f"{PINNED[name][1]}  {name}")


SAMPLES = {
    "pangram": "The quick brown fox jumps over the lazy dog",
    "prose": "This zine is about the allotment at the end of our road, the people who keep it, and what grew "
             "there last summer. We folded it by hand and left copies at the library.",
    "capitals": "HOW TO FOLD A ZINE",
    "digits": "0123456789 14:30 2026",
    "Polish": "Zażółć gęślą jaźń — ściółka",
}
BLOCKS = {"Basic Latin": (0x20, 0x7F), "Latin-1": (0xA0, 0x100), "Latin Extended-A": (0x100, 0x180),
          "Greek": (0x370, 0x400), "Cyrillic": (0x400, 0x500)}


def advance(font, text):
    """Sum of advances in em, through the character map only: no kerning, no ligatures."""
    cmap, hmtx = font.getBestCmap(), font["hmtx"]
    return sum(hmtx[cmap[ord(c)]][0] for c in text) / font["head"].unitsPerEm


def x_top(font):
    """Top of the drawn x, in font units."""
    from fontTools.pens.boundsPen import BoundsPen
    glyphs = font.getGlyphSet()
    pen = BoundsPen(glyphs)
    glyphs[font.getBestCmap()[ord("x")]].draw(pen)
    return pen.bounds[3]


def measure():
    from fontTools.ttLib import TTFont
    pairs = [(file, TTFont(OUT / file), TTFont(INTER / f"Inter-{file.split('-')[1]}")) for file in FACES]
    print("\ncoverage (code points present): Fraunces | Inter")
    for file, book, plain in pairs:
        cells = []
        for block, (lo, hi) in BLOCKS.items():
            b, p = (sum(1 for c in range(lo, hi) if c in f.getBestCmap()) for f in (book, plain))
            cells.append(f"{block} {b}|{p} of {hi - lo}")
        print(f"  {file}: " + "; ".join(cells) + f"; all {len(book.getBestCmap())}|{len(plain.getBestCmap())}")
    common = set.intersection(*(set(book.getBestCmap()) for _, book, _ in pairs))
    inter = set.intersection(*(set(plain.getBestCmap()) for _, _, plain in pairs))
    print(f"  in all four Fraunces faces: {len(common)}; in all four Inter faces: {len(inter)}")
    print("  in Fraunces, not in Inter: " + (" ".join(f"U+{c:04X}" for c in sorted(common - inter)) or "none"))
    print("\nwidth of Fraunces against Inter, same style, same point size (advance sums: no kerning, no ligatures)")
    for label, text in SAMPLES.items():
        print(f"  {label:9}" + "".join(
            f"  {file.split('-')[1][:-4]} {advance(book, text) / advance(plain, text) - 1:+.1%}"
            for file, book, plain in pairs))
    print("\nheight of the drawn x at 10 pt, mm (Fraunces | Inter); line height as a share of the point size")
    for file, book, plain in pairs:
        mm = [x_top(f) / f["head"].unitsPerEm * 10 * 25.4 / 72 for f in (book, plain)]
        line = [(f["hhea"].ascent - f["hhea"].descent + f["hhea"].lineGap) / f["head"].unitsPerEm
                for f in (book, plain)]
        print(f"  {file}: {mm[0]:.2f} | {mm[1]:.2f}; line {line[0]:.3f} | {line[1]:.3f}")


def check():
    """Stop unless every file on disk is the pinned one."""
    for name, (_, pinned) in PINNED.items():
        if not (OUT / name).is_file():
            sys.exit(f"{name}: not on disk; run without arguments to fetch it")
        if hashlib.sha256((OUT / name).read_bytes()).hexdigest() != pinned:
            sys.exit(f"{name}: the file on disk is not the pinned one")


if __name__ == "__main__":
    args = sys.argv[1:]
    if args == ["--measure"]:
        check()
        measure()
    elif not args:
        fetch()
    else:  # a typo must not fetch and write into the repository
        sys.exit(f"unknown argument {args}; use no argument to fetch, or --measure")
