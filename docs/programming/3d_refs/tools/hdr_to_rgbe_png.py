#!/usr/bin/env python3
"""Convert a Radiance .hdr (RGBE) file into kool's *.rgbe.png format.

kool's EnvironmentMap.fromHdriTexture / Assets.hdriEnvironment expects a PNG whose
RGBA bytes are the raw RGBE quadruplets (A = shared exponent). A Radiance .hdr
already stores RGBE, so this only decodes the run-length encoding and writes a PNG.

Usage: python3 hdr_to_rgbe_png.py in.hdr out.rgbe.png
Needs Pillow (pip install pillow). No numpy.
"""
import sys
from PIL import Image


def read_hdr(path):
    with open(path, "rb") as f:
        data = f.read()
    pos = 0
    # header ends with an empty line
    while True:
        end = data.index(b"\n", pos)
        line = data[pos:end]
        pos = end + 1
        if line == b"":
            break
        if line.startswith(b"FORMAT=") and b"32-bit_rle_rgbe" not in line:
            raise ValueError("only 32-bit_rle_rgbe is supported")
    end = data.index(b"\n", pos)
    res = data[pos:end].split()
    pos = end + 1
    if res[0] != b"-Y" or res[2] != b"+X":
        raise ValueError("unsupported orientation %r" % res)
    h, w = int(res[1]), int(res[3])
    out = bytearray(w * h * 4)
    for y in range(h):
        row = y * w * 4
        if w < 8 or w > 0x7FFF or data[pos] != 2 or data[pos + 1] != 2 or data[pos + 2] & 0x80:
            # flat (non-RLE) scanline
            out[row:row + w * 4] = data[pos:pos + w * 4]
            pos += w * 4
            continue
        if ((data[pos + 2] << 8) | data[pos + 3]) != w:
            raise ValueError("bad scanline width")
        pos += 4
        for c in range(4):
            x = 0
            while x < w:
                n = data[pos]
                pos += 1
                if n > 128:
                    n -= 128
                    v = data[pos]
                    pos += 1
                    for i in range(n):
                        out[row + (x + i) * 4 + c] = v
                else:
                    for i in range(n):
                        out[row + (x + i) * 4 + c] = data[pos + i]
                    pos += n
                x += n
    return w, h, bytes(out)


if __name__ == "__main__":
    w, h, px = read_hdr(sys.argv[1])
    Image.frombytes("RGBA", (w, h), px).save(sys.argv[2], optimize=True)
    print(f"{sys.argv[2]}: {w}x{h}")
