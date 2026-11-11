#!/bin/bash
# verify.sh — 用本目錄與 84-rim 已提交的圖重跑 #91 的數字（不需要重拍）。
cd "$(dirname "$0")"; R=..; K=$R/84-rim
echo "## 正午回歸（PR r1；r2 與 r1 位元組相同）"
python3 $R/81-beetle/measure.py pr_r1_specimen.png; python3 $R/81-beetle/measure.py --beetle pr_r1_specimen_beetle.png | head -1
python3 $K/contour.py $K/k0_r1_specimen.png pr_r1_specimen.png; python3 $K/contour.py $K/k0_r1_specimen_beetle.png --beetle pr_r1_specimen_beetle.png
python3 rim.py $K/pr_r1_specimen.png pr_r1_specimen.png; python3 rim.py $K/pr_r1_specimen_beetle.png --beetle pr_r1_specimen_beetle.png
cmp -s pr_r1_specimen.png $K/pr_r1_specimen.png && echo "pr_r1_specimen == 84-rim/pr_r1_specimen (#98)"
echo "## 夜間 contour（夜 k0=main_r1，--mask 正午 k0 幾何）"
for r in main_r1 pr_r1 pr_r2; do
  python3 $K/contour.py main_r1_specimen-night.png --mask $K/k0_r1_specimen.png ${r}_specimen-night.png
  python3 $K/contour.py main_r1_specimen-night_beetle.png --beetle --mask $K/k0_r1_specimen_beetle.png ${r}_specimen-night_beetle.png
done
echo "## 夜間 rim.py（N4/N5/N6、瓢蟲；遮罩基準＝正午 main 圖）"
for r in main_r1 pr_r1 pr_r2; do
  python3 rim.py $K/pr_r1_specimen.png ${r}_specimen-night.png
  python3 rim.py $K/pr_r1_specimen_beetle.png --beetle ${r}_specimen-night_beetle.png
done
echo "## 夜間 measure.py（只參考）"
for r in main_r1 pr_r1 pr_r2; do python3 $R/81-beetle/measure.py ${r}_specimen-night.png; python3 $R/81-beetle/measure.py --beetle ${r}_specimen-night_beetle.png | head -1; done
