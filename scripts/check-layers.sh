#!/usr/bin/env bash
# render3d 不得依賴 AppSession、session、ui、input（#47）。
# 同時掃 import 與 FQN（例如 com.neojou.myants.AppSession 直接寫在型別上）。
# 允許清單內的檔案只回報；清單外出現依賴即失敗。
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT=koolApp/src/commonMain/kotlin/com/neojou/myants/render3d
ALLOW=scripts/render3d-layer-allowlist.txt
PATTERN='com\.neojou\.myants\.(AppSession|session\.|ui\.|input\.)'
fail=0
while IFS= read -r file; do
  rel=${file#"$ROOT"/}
  if grep -qxF "$rel" <(grep -v '^#' "$ALLOW"); then
    echo "允許清單（待移除）: $rel"
  else
    echo "違規: $rel 依賴 session/ui/input" >&2
    grep -nE "$PATTERN" "$file" >&2
    fail=1
  fi
done < <(grep -rlE "$PATTERN" "$ROOT" | sort)
exit $fail
