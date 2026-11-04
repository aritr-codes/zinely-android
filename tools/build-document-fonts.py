"""Fetch the four static Fraunces faces for the Book document voice, and measure them against Inter.

    python tools/build-document-fonts.py            fetch into render-android/src/main/assets/fonts/ and check the pinned hashes
    python tools/build-document-fonts.py --measure  no download: check the files on disk against the pins, then
                                                    print coverage and size of Book and Hand against Inter
    python tools/build-document-fonts.py --hand     rebuild the four Hand (Shantell Sans) faces from the pinned
                                                    release, and check them against the pins (needs fontTools, brotli)

The faces are upstream's own 9 pt statics, byte for byte: nothing is instanced, subset or renamed, so they are
not a Modified Version under the OFL. Upstream is undercasetype/Fraunces. (googlefonts/fraunces is an archived
fork that stopped at Version 1.000 / 1.001, whose static italics lack # $ £ and đ.) The 9 pt cut is the one the
interface already ships: Fraunces9pt-Regular.ttf here is the same file as core/ui/src/main/res/font/
fraunces_regular.ttf.

Fetching needs only the standard library; --measure needs fontTools.

The Hand faces (ADR-148, a third voice) are NOT upstream's own bytes: Shantell Sans ships static OpenType/CFF files
and one variable TrueType, and the app needs four static TrueType faces (minSdk 24 has no variable-font weight axis,
and PDF export embeds glyf outlines). So --hand instances the pinned release's variable font at wght 400/700,
ital 0/1, INFM 100 (the "Informal" letterforms, the Shantell look), BNCE 0, SPAC 0, then drops Greek, Cyrillic and
their extensions. Its OFL.txt names no Reserved Font Name, so a Modified Version may keep the family name
(OFL clause 3 binds only a Reserved Font Name); the copyright line and the licence are kept whole.
The faces are the Book document voice (ADR-126) and ship in the app. PINNED below is the one record of their
hashes: DocumentFontPinTest in :render-android reads this file and holds the bundled faces to it.
"""
import hashlib
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = INTER = ROOT / "render-android/src/main/assets/fonts"
LICENCE = ROOT / "feature/editor/src/main/assets/fonts"  # the one copy the app shows (Colophon, ADR-117)

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

# ---- Hand (Shantell Sans), built from a pinned release rather than copied ----
HAND_ZIP = ("https://github.com/arrowtype/shantell-sans/releases/download/1.011/Shantell_Sans_1.011.zip",
            "7562b90399eae6339f89079c917235823fca1cabda3bab624bbacaefa7abe558")  # release 1.011
HAND_VARIABLE = ("Shantell Sans 1.011/Desktop/ShantellSans[BNCE,INFM,SPAC,ital,wght].ttf",
                 "fbcd62f1c48f1d2cdf3f988f600c18137a4e42744370caf950c99d3b27567d25")
HAND_LICENCE = "Shantell Sans 1.011/2 - License.txt"  # written to feature/editor/.../fonts/OFL-ShantellSans.txt
HAND_FACES = {  # file -> (wght, ital, style name, macStyle); SHA-256 of what --hand builds, pinned by DocumentFontPinTest
    "ShantellSans-Regular.ttf": (400, 0, "Regular", 0b00),
    "ShantellSans-Bold.ttf": (700, 0, "Bold", 0b01),
    "ShantellSans-Italic.ttf": (400, 1, "Italic", 0b10),
    "ShantellSans-BoldItalic.ttf": (700, 1, "Bold Italic", 0b11),
}
HAND_PINNED = {  # same shape as PINNED, so the test reads both with one rule
    "ShantellSans-Regular.ttf": ("built",
                                "ff4807a120fa4debae944c49b92b878d106a6f6df69c33ec6d67658bf1133f95"),
    "ShantellSans-Bold.ttf": ("built",
                                "9807970e1ade20bac318757c1cefe59f216f41a0e388289dd5e03e0de7ce0dd5"),
    "ShantellSans-Italic.ttf": ("built",
                                "19fe242bed9224f68ff7625e008322d6ade63c56ba3c62683eb44b680a28c7c3"),
    "ShantellSans-BoldItalic.ttf": ("built",
                                "6264066b97a25b08e09d7e66d6cb5507ce8de6604af9b243352e255426595431"),
}
HAND_DROPPED = [(0x370, 0x52F), (0x1C80, 0x1C8F), (0x1F00, 0x1FFF), (0x2DE0, 0x2DFF), (0xA640, 0xA69F)]  # Greek, Cyrillic + extensions


def where(name):
    """The faces sit with the document fonts; the licence sits where the app reads it."""
    return (OUT if name.endswith(".ttf") else LICENCE) / name


def fetch():
    """Download every pinned file, and write none of them unless all of them match."""
    files = {}
    for name, (path, pinned) in PINNED.items():
        data = urllib.request.urlopen(BASE + path, timeout=60).read()
        actual = hashlib.sha256(data).hexdigest()
        if actual != pinned:
            sys.exit(f"{path}: upstream bytes are not the pinned ones ({actual}); nothing was written")
        files[name] = data
    for name, data in files.items():
        where(name).write_bytes(data)
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
    measure_family("Fraunces", FACES)
    measure_family("Shantell Sans", list(HAND_FACES))


def measure_family(family, faces):
    from fontTools.ttLib import TTFont
    pairs = [(file, TTFont(OUT / file), TTFont(INTER / f"Inter-{file.split('-')[1]}")) for file in faces]
    print(f"\n== {family} ==\ncoverage (code points present): {family} | Inter")
    for file, book, plain in pairs:
        cells = []
        for block, (lo, hi) in BLOCKS.items():
            b, p = (sum(1 for c in range(lo, hi) if c in f.getBestCmap()) for f in (book, plain))
            cells.append(f"{block} {b}|{p} of {hi - lo}")
        print(f"  {file}: " + "; ".join(cells) + f"; all {len(book.getBestCmap())}|{len(plain.getBestCmap())}")
    common = set.intersection(*(set(book.getBestCmap()) for _, book, _ in pairs))
    inter = set.intersection(*(set(plain.getBestCmap()) for _, _, plain in pairs))
    print(f"  in all four {family} faces: {len(common)}; in all four Inter faces: {len(inter)}")
    print(f"  in {family}, not in Inter: " + (" ".join(f"U+{c:04X}" for c in sorted(common - inter)) or "none"))
    print(f"\nwidth of {family} against Inter, same style, same point size (advance sums: no kerning, no ligatures)")
    for label, text in SAMPLES.items():
        print(f"  {label:9}" + "".join(
            f"  {file.split('-')[1][:-4]} {advance(book, text) / advance(plain, text) - 1:+.1%}"
            for file, book, plain in pairs))
    print(f"\nheight of the drawn x at 10 pt, mm ({family} | Inter); line height as a share of the point size")
    for file, book, plain in pairs:
        mm = [x_top(f) / f["head"].unitsPerEm * 10 * 25.4 / 72 for f in (book, plain)]
        line = [(f["hhea"].ascent - f["hhea"].descent + f["hhea"].lineGap) / f["head"].unitsPerEm
                for f in (book, plain)]
        print(f"  {file}: {mm[0]:.2f} | {mm[1]:.2f}; line {line[0]:.3f} | {line[1]:.3f}")


def build_hand():
    """Fetch the pinned Shantell Sans release, build the four Hand faces, write them and the licence, and compare."""
    import io
    import zipfile
    from fontTools import subset
    from fontTools.ttLib import TTFont
    from fontTools.varLib import instancer

    url, zip_pin = HAND_ZIP
    data = urllib.request.urlopen(url, timeout=120).read()
    if hashlib.sha256(data).hexdigest() != zip_pin:
        sys.exit("the Shantell Sans release is not the pinned one; nothing was written")
    archive = zipfile.ZipFile(io.BytesIO(data))
    variable = archive.read(HAND_VARIABLE[0])
    if hashlib.sha256(variable).hexdigest() != HAND_VARIABLE[1]:
        sys.exit("the variable font inside the release is not the pinned one; nothing was written")
    built = {}
    for name, (wght, ital, style, mac) in HAND_FACES.items():
        font = instancer.instantiateVariableFont(
            TTFont(io.BytesIO(variable)), {"wght": wght, "ital": ital, "BNCE": 0, "INFM": 100, "SPAC": 0})
        opts = subset.Options()
        opts.layout_features = ["*"]
        opts.name_IDs = [0, 1, 2, 3, 4, 5, 6, 13, 14]
        opts.notdef_outline = True
        opts.hinting = False
        keep = [c for c in font.getBestCmap() if not any(lo <= c <= hi for lo, hi in HAND_DROPPED)]
        sub = subset.Subsetter(opts)
        sub.populate(unicodes=keep)
        sub.subset(font)
        names = font["name"]
        for rec in [r for r in names.names if r.nameID in (1, 2, 3, 4, 6, 16, 17, 25)]:
            names.removeNames(nameID=rec.nameID)
        full = "Shantell Sans" if style == "Regular" else f"Shantell Sans {style}"
        for nid, text in ((1, "Shantell Sans" if style in ("Regular", "Bold", "Italic", "Bold Italic") else full),
                          (2, style), (3, f"Zinely;ShantellSans-{style.replace(' ', '')}"), (4, full),
                          (6, f"ShantellSans-{style.replace(' ', '')}")):
            names.setName(text, nid, 3, 1, 0x409)
        font["head"].macStyle = mac
        font["OS/2"].fsSelection = (font["OS/2"].fsSelection & ~0b1100001) | (
            0b1 if mac & 2 else 0) | (0b100000 if mac & 1 else 0) | (0b1000000 if mac == 0 else 0)
        # Keep upstream's head.modified: fontTools otherwise stamps the build time and no two runs match the pin.
        font.recalcTimestamp = False
        buf = io.BytesIO()
        font.save(buf)
        built[name] = buf.getvalue()
    licence = archive.read(HAND_LICENCE)
    for name, blob in built.items():
        (OUT / name).write_bytes(blob)
        actual = hashlib.sha256(blob).hexdigest()
        pinned = HAND_PINNED[name][1]
        print(f"{actual}  {name}  ({len(blob)} bytes){'' if actual == pinned else '   <- DIFFERS from the pin'}")
    (LICENCE / "OFL-ShantellSans.txt").write_bytes(licence)
    print(f"{hashlib.sha256(licence).hexdigest()}  OFL-ShantellSans.txt")


def check():
    """Stop unless every file on disk is the pinned one."""
    for name, (_, pinned) in {**PINNED, **HAND_PINNED}.items():
        if not where(name).is_file():
            sys.exit(f"{name}: not on disk; run without arguments to fetch it")
        if hashlib.sha256(where(name).read_bytes()).hexdigest() != pinned:
            sys.exit(f"{name}: the file on disk is not the pinned one")


if __name__ == "__main__":
    args = sys.argv[1:]
    if args == ["--measure"]:
        check()
        measure()
    elif args == ["--hand"]:
        build_hand()
    elif not args:
        fetch()
    else:  # a typo must not fetch and write into the repository
        sys.exit(f"unknown argument {args}; use no argument to fetch, --measure or --hand")
