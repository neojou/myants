#!/bin/bash
# verify.sh — 用本目錄已提交的圖重跑 #86 的全部數字，不需要重拍。
# pr_{r1,r2}_<色票>_*：PR build（實作 commit 0842d88；之後只加測試），兩次獨立啟動。Copper 不寫 localStorage（新預設），其他色票以 localStorage antsim.ui.v1 預寫 antColor。
# k0_r1_copper_*：同一 build 只把 RIM_DAY、RIM_NIGHT 設 0 的量測用 build（未提交）。
# main_<色票>_specimen.png：修改前（main b405ae4 的 CI js-distribution，Artist 規格量測圖，各 1 輪）。
cd "$(dirname "$0")"; R=..; K=$R/84-rim; N=$R/91-night-rim
SW="copper black gold sage blue"
echo "## 1. 正午 specimen：D1 ≥3.0、hot% ≤2%、D2 3.31、瓢蟲 shell/gnd 1.62（每張單獨判）"
for r in r1 r2; do for s in $SW; do f=pr_${r}_${s}_specimen.png
  python3 $R/81-beetle/measure.py $f; python3 $N/rim.py $K/pr_r1_specimen.png $f | sed 's/^/   /'
done; done
echo "## 2. 甲蟲構圖 D3 3.22（Copper r1/r2，其他色票 r1）"
for f in pr_r1_copper_specimen_beetle.png pr_r2_copper_specimen_beetle.png pr_r1_{black,gold,sage,blue}_specimen_beetle.png; do python3 $R/81-beetle/measure.py --beetle $f | head -1; done
echo "## 3. Black 與 main b405ae4 預設（黑殼）specimen 逐像素比對（排除 FPS 區 y0–59,x0–199）"
for r in r1 r2; do python3 cmpfps.py main_copper_specimen.png pr_${r}_black_specimen.png; done
python3 cmpfps.py main_copper_specimen_beetle.png pr_r1_black_specimen_beetle.png
echo "## 4. 修改前（main b405ae4）對照數字"
for s in $SW; do python3 $R/81-beetle/measure.py main_${s}_specimen.png; python3 $N/rim.py $K/pr_r1_specimen.png main_${s}_specimen.png | sed 's/^/   /'; done
echo "## 5. Copper 正午 contour.py --body（同 build k0；本體帶 1.15–1.35、內部中位 1.000、平均 ≤1.01）"
for r in r1 r2; do python3 $K/contour.py k0_r1_copper_specimen.png --body pr_${r}_copper_specimen.png; done
echo "# 2px 預設帶（紀錄，不判）"
for r in r1 r2; do python3 $K/contour.py k0_r1_copper_specimen.png pr_${r}_copper_specimen.png; done
echo "## 6. Copper 夜間（--mask 同 build 正午 k0；亮度比同 build 夜 k0）：工蟻本體帶 1.75–2.55、內中 ≤1.10、內均 ≤1.20、core/gnd ≤1.5；細結構 ≤1.05；頭前 ≤k0；N4 兩項 B−R ≥0"
for r in r1 r2; do echo "# $r"
  python3 $K/contour.py k0_r1_copper_specimen-night.png --mask k0_r1_copper_specimen.png --body pr_${r}_copper_specimen-night.png
  python3 $N/night.py k0_r1_copper_specimen.png k0_r1_copper_specimen-night.png pr_${r}_copper_specimen-night.png
  python3 $N/rim.py $K/pr_r1_specimen.png pr_${r}_copper_specimen-night.png
  python3 $R/81-beetle/measure.py pr_${r}_copper_specimen-night.png
done
echo "# 夜 k0 參考"; python3 $N/rim.py $K/pr_r1_specimen.png k0_r1_copper_specimen-night.png
python3 $N/night.py k0_r1_copper_specimen.png k0_r1_copper_specimen-night.png k0_r1_copper_specimen-night.png
