#!/usr/bin/env python3
"""Summarise a MYANTS_FRAME_LOG file (one System.nanoTime() per line).

usage: python3 docs/perf/frames_to_fps.py /tmp/myants-frames.txt [warmup_s=10] [window_s=60]
Prints mean fps (frames / seconds) and 1% low (mean frame time of the slowest 1% of frames).
"""
import sys

path = sys.argv[1]
warm = float(sys.argv[2]) if len(sys.argv) > 2 else 10.0
win = float(sys.argv[3]) if len(sys.argv) > 3 else 60.0
ts = [int(l) for l in open(path) if l.strip()]
t0 = ts[0] + warm * 1e9
sel = [t for t in ts if t0 <= t <= t0 + win * 1e9]
if len(sel) < 2 or (ts[-1] - ts[0]) / 1e9 < warm + win:
    sys.exit(f"log too short: {(ts[-1] - ts[0]) / 1e9:.1f} s, need {warm + win:.0f} s")
dts = [(b - a) / 1e6 for a, b in zip(sel, sel[1:])]
span = (sel[-1] - sel[0]) / 1e9
slow = sorted(dts, reverse=True)
k = max(1, len(slow) // 100)
print(f"frames={len(dts)} span_s={span:.2f} mean_fps={len(dts) / span:.2f} "
      f"low1_fps={1000 / (sum(slow[:k]) / k):.2f} max_ms={slow[0]:.1f}")
