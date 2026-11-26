#!/usr/bin/env bash
# 重跑已提交圖上的按鈕對比與（若有 nophero2）P5 色相／ΔE。只讀本目錄的圖。
set -euo pipefail
cd "$(dirname "$0")"
python3 buttons.py pr_day.png
echo '======= day hover Analysis ======='
python3 buttons.py pr_day_hover.png
echo '======= day selected Params ======='
python3 buttons.py pr_params_open.png
echo '======= night buttons ======='
python3 buttons.py pr_night.png
if [[ -f pr_day_nophero2.png ]]; then
  echo '======= P5 day (pr vs nophero2) ======='
  python3 p5.py pr_day.png pr_day_nophero2.png
fi
if [[ -f pr_night_nophero2.png ]]; then
  echo '======= P5 night (pr vs nophero2) ======='
  python3 p5.py pr_night.png pr_night_nophero2.png
fi
