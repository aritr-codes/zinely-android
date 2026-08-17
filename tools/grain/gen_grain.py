# -*- coding: utf-8 -*-
"""
Generate the V2 grain tile from the SVG 1.1 normative feTurbulence algorithm.

Frozen source (v2-bench.html:25 / v2-proof.html:25 / v2-library.html:18):
    <svg width='140' height='140'>
      <filter id='n'>
        <feTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='2' stitchTiles='stitch'/>
        <feColorMatrix type='saturate' values='0'/>
      </filter>
      <rect width='140' height='140' filter='url(#n)' [opacity='.5']/>
    </svg>

seed defaults to 0.  stitchTiles='stitch' is stitched over the 140x140 tile, which is what the
attribute exists to do and what a repeating background-image requires.

This is a transcription of the algorithm in the SVG 1.1 spec (Filter Effects, feTurbulence),
not a reimplementation of any particular browser.
"""
import struct, zlib, math

RAND_m, RAND_a, RAND_q, RAND_r = 2147483647, 16807, 127773, 2836
BSize, BM = 0x100, 0xff
PerlinN = 0x1000

# The two frozen grain sources this generator serves. Selected by name on the command line so that
# neither tile can be produced by editing a constant and forgetting to put it back -- the V2 tile is
# committed and guards 10 catalog goldens, and regenerating it with V2.1's parameters would repaint
# every V2 paper surface without a single test naming the cause.
#
# V2.1 differs in every field: a lower frequency and a third octave (coarser, more structured grain),
# a larger tile, and NO stitching, which the V2.1 source omits.
PRESETS = {
    # v2-{library,proof,bench}.html  -- baseFrequency .9, 2 octaves, stitched, 140px
    'v2': dict(tile=140, base_freq=0.9, octaves=2, stitch=True),
    # v21-{library,proof,bench}.html -- baseFrequency .85, 3 octaves, unstitched, 160px
    'v21': dict(tile=160, base_freq=0.85, octaves=3, stitch=False),
}

TILE = 140
BASE_FREQ = 0.9
OCTAVES = 2
STITCH = True
SEED = 0


def setup_seed(lSeed):
    if lSeed <= 0:
        lSeed = -(lSeed % (RAND_m - 1)) + 1
    if lSeed > RAND_m - 1:
        lSeed = RAND_m - 1
    return lSeed


def random(lSeed):
    result = RAND_a * (lSeed % RAND_q) - RAND_r * (lSeed // RAND_q)
    if result <= 0:
        result += RAND_m
    return result


def init(lSeed):
    uLatticeSelector = [0] * (BSize + BSize + 2)
    fGradient = [[[0.0, 0.0] for _ in range(BSize + BSize + 2)] for _ in range(4)]
    lSeed = setup_seed(lSeed)
    # k OUTER, i inner -- exactly as the spec's `for(k...){for(i...){...}}`. The nesting is not
    # cosmetic: it fixes the order the PRNG is drawn in, so inverting it yields a different tile
    # from the same seed. (uLatticeSelector[i] = i sits inside the inner loop in the published
    # code too, redundantly re-run on all four passes; kept as written.)
    for k in range(4):
        for i in range(BSize):
            uLatticeSelector[i] = i
            lSeed = random(lSeed)
            a = float((lSeed % (BSize + BSize)) - BSize) / BSize
            lSeed = random(lSeed)
            b = float((lSeed % (BSize + BSize)) - BSize) / BSize
            s = math.sqrt(a * a + b * b)
            if s == 0:
                s = 1.0
            fGradient[k][i][0] = a / s
            fGradient[k][i][1] = b / s
    i = BSize - 1
    while i > 0:
        lSeed = random(lSeed)
        j = lSeed % BSize
        uLatticeSelector[i], uLatticeSelector[j] = uLatticeSelector[j], uLatticeSelector[i]
        i -= 1
    for i in range(BSize + 2):
        uLatticeSelector[BSize + i] = uLatticeSelector[i]
        for k in range(4):
            fGradient[k][BSize + i][0] = fGradient[k][i][0]
            fGradient[k][BSize + i][1] = fGradient[k][i][1]
    return uLatticeSelector, fGradient


LAT, GRAD = init(SEED)

# Counts how often the stitch wrap actually fires. Instrumentation, not algorithm: the first
# version of this file was wrong in a way that left it at zero while still producing a plausible
# tile, so the count is printed and asserted rather than assumed.
WRAPS = [0]


def s_curve(t):
    return t * t * (3.0 - 2.0 * t)


def lerp(t, a, b):
    return a + t * (b - a)


def noise2(channel, vx, vy, stitch):
    # NOTE the ordering: the lattice points are computed UNMASKED, the stitch wrap is subtracted,
    # and only then are they masked with & BM -- which is what the spec does. Masking first (as an
    # earlier draft of this file did) clamps bx0 to 0..255 while nWrapX is ~4222, so the wrap
    # branches never fire and the generator silently produces noStitch output.
    t = vx + PerlinN
    bx0 = int(t)
    bx1 = bx0 + 1
    rx0 = t - int(t)
    rx1 = rx0 - 1.0
    t = vy + PerlinN
    by0 = int(t)
    by1 = by0 + 1
    ry0 = t - int(t)
    ry1 = ry0 - 1.0

    if stitch is not None:
        wrapx, wrapy, width, height = stitch
        if bx0 >= wrapx:
            bx0 -= width; WRAPS[0] += 1
        if bx1 >= wrapx:
            bx1 -= width; WRAPS[0] += 1
        if by0 >= wrapy:
            by0 -= height; WRAPS[0] += 1
        if by1 >= wrapy:
            by1 -= height; WRAPS[0] += 1

    bx0 &= BM
    bx1 &= BM
    by0 &= BM
    by1 &= BM

    i = LAT[bx0]
    j = LAT[bx1]
    b00 = LAT[i + by0]
    b10 = LAT[j + by0]
    b01 = LAT[i + by1]
    b11 = LAT[j + by1]
    sx = s_curve(rx0)
    sy = s_curve(ry0)

    q = GRAD[channel][b00]; u = rx0 * q[0] + ry0 * q[1]
    q = GRAD[channel][b10]; v = rx1 * q[0] + ry0 * q[1]
    a = lerp(sx, u, v)
    q = GRAD[channel][b01]; u = rx0 * q[0] + ry1 * q[1]
    q = GRAD[channel][b11]; v = rx1 * q[0] + ry1 * q[1]
    b = lerp(sx, u, v)
    return lerp(sy, a, b)


def turbulence(channel, x, y, fx, fy, octaves, tile_w, tile_h):
    # stitchTiles: 'stitch' for V2, absent (the initial value, 'noStitch') for V2.1. When it is
    # absent the frequency is NOT snapped and no wrap is tracked -- the function is simply sampled,
    # and the tile's edges do not meet. That is what the V2.1 source asks for, so it is what is
    # generated; the visible consequence at these frequencies is nil (see the seam note at the
    # bottom of this file), but generating a stitched tile for an unstitched source would be
    # substituting our judgement for the specification's.
    if not STITCH:
        fSum, vx, vy, ratio = 0.0, x * fx, y * fy, 1.0
        for _ in range(octaves):
            fSum += noise2(channel, vx, vy, None) / ratio
            vx *= 2
            vy *= 2
            ratio *= 2
        return fSum

    # stitchTiles='stitch': adjust the base frequency so an integral number of periods
    # spans the tile, then track the wrap points per octave (SVG 1.1, feTurbulence).
    lo = math.floor(tile_w * fx) / tile_w
    hi = math.ceil(tile_w * fx) / tile_w
    fx = lo if (fx / lo < hi / fx) else hi
    lo = math.floor(tile_h * fy) / tile_h
    hi = math.ceil(tile_h * fy) / tile_h
    fy = lo if (fy / lo < hi / fy) else hi

    width = int(tile_w * fx + 0.5)
    height = int(tile_h * fy + 0.5)
    wrapx = 0 + width + PerlinN      # tile origin x = 0
    wrapy = 0 + height + PerlinN     # tile origin y = 0

    fSum = 0.0
    vx, vy = x * fx, y * fy
    ratio = 1.0
    stitch = [wrapx, wrapy, width, height]
    for _ in range(octaves):
        fSum += noise2(channel, vx, vy, tuple(stitch)) / ratio
        vx *= 2
        vy *= 2
        ratio *= 2
        stitch[2] *= 2
        stitch[0] = 2 * (stitch[0] - PerlinN) + PerlinN
        stitch[3] *= 2
        stitch[1] = 2 * (stitch[1] - PerlinN) + PerlinN
    return fSum


def srgb_encode(c):
    """linearRGB -> sRGB, the standard transfer function."""
    return 12.92 * c if c <= 0.0031308 else 1.055 * (c ** (1 / 2.4)) - 0.055


def build():
    px = bytearray()
    lum = []
    alpha = []
    for y in range(TILE):
        for x in range(TILE):
            # Pixel centres, as the spec samples them.
            fx, fy = x + 0.5, y + 0.5
            chans = []
            for c in range(4):
                t = turbulence(c, fx, fy, BASE_FREQ, BASE_FREQ, OCTAVES, TILE, TILE)
                # fractalNoise: (t + 1) / 2
                chans.append(min(1.0, max(0.0, (t + 1.0) / 2.0)))
            r, g, b, a = chans
            # feColorMatrix type="saturate" values="0"  ->  luminance, RGB only; alpha untouched.
            # These are the *linear-light* luminance coefficients, which is the tell: the whole
            # filter chain runs in linearRGB.
            l = 0.2126 * r + 0.7152 * g + 0.0722 * b
            # `color-interpolation-filters` has an initial value of **linearRGB** (SVG 1.1 §11.7.1)
            # and none of the three frozen files overrides it. So feTurbulence generates in linear
            # light and the filter *result* is encoded to sRGB on the way out to the display. Skip
            # this and the tile is ~45% too dark: linear 0.5 is sRGB 0.735, not 0.5. Alpha is never
            # colour-managed, so it is deliberately not encoded.
            l = srgb_encode(l)
            lum.append(l)
            alpha.append(a)
    return lum, alpha


def write_png(path, lum, alpha, w, h):
    raw = bytearray()
    for y in range(h):
        raw.append(0)  # filter type 0
        for x in range(w):
            i = y * w + x
            v = int(round(lum[i] * 255))
            # Colour type 6 (RGBA) rather than 4 (grey+alpha), deliberately: an 8-bit greyscale PNG
            # leaves "what does this grey mean" to the decoder, and decoders disagree — Java's ImageIO
            # converts it through a linear-grey colour space and reads a mid-grey tile as ~187.
            # Writing R=G=B in sRGB removes the ambiguity at the cost of two bytes per pixel.
            raw.append(v); raw.append(v); raw.append(v)
            raw.append(int(round(alpha[i] * 255)))

    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    ihdr = struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)  # colour type 6 = RGBA
    png = (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr)
           + chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b''))
    open(path, 'wb').write(png)
    return len(png)


if __name__ == '__main__':
    import sys, hashlib
    if len(sys.argv) < 3 or sys.argv[1] not in PRESETS:
        sys.exit('usage: gen_grain.py {%s} <out.png>' % '|'.join(PRESETS))
    preset = PRESETS[sys.argv[1]]
    TILE, BASE_FREQ = preset['tile'], preset['base_freq']
    OCTAVES, STITCH = preset['octaves'], preset['stitch']
    out = sys.argv[2]
    print('preset           : %s  tile=%d freq=%s octaves=%d stitch=%s'
          % (sys.argv[1], TILE, BASE_FREQ, OCTAVES, STITCH))

    lum, alpha = build()
    size = write_png(out, lum, alpha, TILE, TILE)

    # --- verification -------------------------------------------------------
    print('bytes            :', size)
    print('sha256           :', hashlib.sha256(open(out, 'rb').read()).hexdigest())
    print('luma  mean/min/max: %.4f %.4f %.4f' % (sum(lum) / len(lum), min(lum), max(lum)))
    print('alpha mean       : %.4f' % (sum(alpha) / len(alpha)))

    # Stitching, checked on the CONTINUOUS FUNCTION rather than on the pixels.
    #
    # The property stitchTiles buys is that the turbulence function is periodic with the tile's
    # period, so the limit approaching the far edge meets the value at the near edge. That is
    # measured here in the function's own [-1, 1] units -- it is NOT a pixel measurement, and the
    # two are easy to confuse (an earlier version of this file quoted this number as though it
    # described the PNG).
    #
    # Do not try to move this check into the PNG. At baseFrequency 0.9 the lattice cell is ~1.1px,
    # so adjacent pixels are effectively uncorrelated and NO pixel statistic separates a stitched
    # tile from an unstitched one -- measured, the stitched tile's wrap edge scores slightly WORSE
    # (ratio 1.355) than the unstitched one's (1.227). Stitching is implemented because the frozen
    # source asks for it and it is what the spec means, not because it is observable here.
    def unstitched(ch, x, y):
        vx, vy, total, ratio = x * BASE_FREQ, y * BASE_FREQ, 0.0, 1.0
        for _ in range(OCTAVES):
            total += noise2(ch, vx, vy, None) / ratio
            vx *= 2; vy *= 2; ratio *= 2
        return total

    eps = 1e-6
    probes = [0.0, 7.3, 31.7, 70.0, 111.1, TILE - 1.0]
    f = lambda x, y: turbulence(0, x, y, BASE_FREQ, BASE_FREQ, OCTAVES, TILE, TILE)
    seam = max(max(abs(f(TILE - eps, t) - f(0.0, t)), abs(f(t, TILE - eps) - f(t, 0.0))) for t in probes)
    control = max(abs(unstitched(0, TILE - eps, t) - unstitched(0, 0.0, t)) for t in probes)
    print('function seam    : %.3e   (control, stitching off: %.3e)' % (seam, control))
    print('stitch wraps fired:', WRAPS[0])

    if STITCH:
        assert WRAPS[0] > 0, 'stitchTiles requested but the wrap never fired'
        assert seam < 1e-4, 'the stitched function is not continuous across the tile boundary'
        assert control > 1e-2, 'the control is not discriminating -- the seam check proves nothing'
    else:
        # The mirror assertions. An unstitched source must produce an unstitched tile: if the wrap
        # ever fires here, the preset has leaked into the stitched path and the tile is not the one
        # the frozen SVG describes -- silently better-behaved, and silently not the specification.
        assert WRAPS[0] == 0, 'stitchTiles absent but the wrap fired anyway'
        assert seam > 1e-2, 'an unstitched function should NOT be continuous across the boundary'
