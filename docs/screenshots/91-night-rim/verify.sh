#!/bin/bash
# verify.sh — 用本目錄已提交的圖重跑 #91（含 Artist FAIL 修正：觸角／大顎不套 rim）的數字，不需要重拍。
# k0_r1_*：同一 build（c928041）只把 RIM_DAY、RIM_NIGHT 設 0 的量測用 build（未提交）；夜 k0 與修正前 main 2eaa03b 的夜圖位元組相同。
# pr_r{1,2}_*：修正後 PR build（c928041），兩次獨立啟動。正午「main」對照＝84-rim/pr_r1_*（#98，main 正午圖）。
cd "$(dirname "$0")"; R=..; K=$R/84-rim
for f in specimen specimen_beetle; do cmp -s k0_r1_$f.png $K/k0_r1_$f.png && echo "k0_r1_$f == 84-rim/k0_r1_$f" || echo "k0_r1_$f 與 84-rim/k0_r1_$f 只差 FPS 區（框外）"; done
for r in r1; do echo "## 正午 $r（2px，對同 build k0；r2 兩張與 r1 位元組相同，未另存）"
  python3 $R/81-beetle/measure.py pr_${r}_specimen.png; python3 $R/81-beetle/measure.py --beetle pr_${r}_specimen_beetle.png | head -1
  python3 $K/contour.py k0_r1_specimen.png pr_${r}_specimen.png; python3 $K/contour.py k0_r1_specimen_beetle.png --beetle pr_${r}_specimen_beetle.png
  python3 $K/contour.py k0_r1_specimen.png --body pr_${r}_specimen.png
  python3 rim.py $K/pr_r1_specimen.png pr_${r}_specimen.png; python3 rim.py $K/pr_r1_specimen_beetle.png --beetle pr_${r}_specimen_beetle.png
done
for r in r1 r2; do echo "## 夜間 $r（--mask 同 build 正午 k0；亮度比夜 k0）"
  echo "# 本體帶（正式）"
  python3 $K/contour.py k0_r1_specimen-night.png --mask k0_r1_specimen.png --body pr_${r}_specimen-night.png
  python3 $K/contour.py k0_r1_specimen-night_beetle.png --beetle --mask k0_r1_specimen_beetle.png --body pr_${r}_specimen-night_beetle.png
  echo "# 2px 預設帶（紀錄，不判）"
  python3 $K/contour.py k0_r1_specimen-night.png --mask k0_r1_specimen.png pr_${r}_specimen-night.png
  python3 $K/contour.py k0_r1_specimen-night_beetle.png --beetle --mask k0_r1_specimen_beetle.png pr_${r}_specimen-night_beetle.png
  echo "# night.py（細結構、頭前 ≥3:1、N4）"
  python3 night.py k0_r1_specimen.png k0_r1_specimen-night.png pr_${r}_specimen-night.png
  python3 night.py k0_r1_specimen_beetle.png k0_r1_specimen-night_beetle.png --beetle pr_${r}_specimen-night_beetle.png
  echo "# rim.py（core/gnd、N5、夜 B/R、瓢蟲）"
  python3 rim.py $K/pr_r1_specimen.png pr_${r}_specimen-night.png; python3 rim.py $K/pr_r1_specimen_beetle.png --beetle pr_${r}_specimen-night_beetle.png
  echo "# measure.py（夜 D2；D1/D3 只參考）"
  python3 $R/81-beetle/measure.py pr_${r}_specimen-night.png
done
echo "## 夜 k0 參考"; python3 rim.py $K/pr_r1_specimen.png k0_r1_specimen-night.png; python3 rim.py $K/pr_r1_specimen_beetle.png --beetle k0_r1_specimen-night_beetle.png
python3 night.py k0_r1_specimen_beetle.png k0_r1_specimen-night_beetle.png --beetle k0_r1_specimen-night_beetle.png
