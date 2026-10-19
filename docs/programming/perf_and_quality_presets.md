# 效能與品質檔位

## 1. 模擬與渲染分離（沿用 v0.7.0）
- sim：固定 60 Hz，`AppSession.tick(dt)` 累加器，每幀最多補 64 tick（`myants_antsim_deviations.md`）。3D 版**不改**。
- render：每幀讀 sim 快照，寫實例緩衝。為了平滑，螞蟻位置在「上一 tick」與「本 tick」間插值（`alpha = accumulator / tickDt`）。sim 需提供 `prevX/prevY/prevAngle`（只讀快照欄位，由 render 端在每 tick 前複製，不改 sim 類別也可以：render 自己保存上一個快照的 FloatArray）。
- 每幀 0 配置：所有 `MeshInstanceList` 預先配置到檔位上限；`clear()` 後 `addInstances(n) { buf -> … }` 填；文字 HUD 每 250 ms 更新 state。
- 費洛蒙貼圖：420×252 RGBA，依 `PerformanceProfile.pheromoneUpdateSkip`（2/4/6 幀）上傳一次，兩個固定 id 輪流（避開 GL 雜湊 id 問題）。

## 2. 檔位（Grafik → Qualität；預設 Medium）
既有 sim 檔位 `PerformanceManager.profile`：maxAnts 80/120/300/600/1200（`S/Performance.kt:17-22`）照舊；3D 參數新增在 `render3d/quality/QualityPreset.kt`：

| 參數 | Ultra Low | Low | Medium | High | Ultra |
|---|---|---|---|---|---|
| 管線 | forward、無陰影 | forward + SimpleShadowMap 1024 | deferred | deferred | deferred |
| `renderResolution` | 0.6 | 0.75 | 1.0 | 1.0 | 1.0 |
| 陰影 | 無 | 1024 | 2048（world bounds） | 2048（world bounds） | CSM 3×2048 或 4096 |
| SSAO | 關 | 關 | 開（`aoMapSize` 0.5） | 開（0.75） | 開（1.0） |
| Bloom（需 compute） | 關 | 關 | 開 | 開 | 開 |
| IBL | 單色/漸層 `EnvironmentMap.fromGradientColor` | 1 張預烘 HDRI | 預烘 2 張混合 | SkyCube 每 2 s 更新 | SkyCube 每 0.5 s 更新 |
| 食物點光 | 0 | 0 | 16 | 32 | 64 |
| 細草數 | 0 | 2k | 6k | 12k | 20k |
| 草擺動 | 否 | 否 | 是 | 是 | 是（+ 投影） |
| 小石/落葉/花 | 300/0/30 | 800/100/60 | 2k/300/120 | 4k/600/200 | 5k/1k/300 |
| 螞蟻 | 程序簡模、無腳動畫 | 程序 + 腳動畫 | glTF LOD1 或程序，姿態桶 4 | 桶 6 | 桶 9 + 近距 LOD0 |
| 雨絲 | 500 | 1k | 3k | 5k | 8k |
| 巢穴剖面 pass | 0.5× 解析度、無點光 | 0.75× | 1× | 1× | 1× + AO（選配） |
| MSAA（`numSamples`） | 1 | 2 | 4 | 4 | 4 |

- 「Performance-Modus」（sim）與「Qualität」（3D）建議**預設連動**（同一個下拉），進階可拆開。
- 切檔位時重建 deferred pipeline 是重操作（要 release 舊 passes）；只在使用者按「套用」時做，滑桿類（bloom 強度、草密度顯示比例）即時生效。
- 啟動自動降級：前 5 秒平均 FPS < 40 → 降一檔並在 HUD 角落提示 3 秒（可關）。

## 3. 目標（推測，Mac 實機驗）
| 情境 | 目標 |
|---|---|
| Apple Silicon（M1 以上）、1600×900、Medium、雙群 300 蟻 | ≥ 60 fps |
| 同上、High 600 蟻 | ≥ 50 fps |
| Ultra 1200 蟻 | ≥ 30 fps |
| 第一幀出現 | < 2 s（素材背景載入，程序幾何先頂） |
| JS（Chrome WebGPU）Medium | ≥ 40 fps；WebGL2 退路自動關 bloom |

## 4. 量測
- HUD FPS 已有；新增 Grafik 底部小字：`GPU x.x ms · CPU x.x ms · draws N · inst N`。GPU 時間：`ctx.backend.frameGpuTime`（介面 `K/pipeline/backend/RenderBackend.kt:24`，Vulkan 用 timestamp query 實作 `KD/pipeline/backend/vk/RenderBackendVk.kt:110`，已查證）。
- kool 內建 `DebugOverlay`（`K/util/DebugOverlay.kt`）可在 `--debug` 時開。
- 壓力測試：`--quality=ULTRA --colonies=2 --seed=12345`，跑 3 分鐘記錄平均/1% low（log 輸出）。

## 5. 主要成本與對策
| 成本 | 對策 |
|---|---|
| 細草數量 | 實例化 + 距離 LOD（`InstancedLodController`）+ 不投影 + 視錐剔除（chunk 層級 `isFrustumChecked`）|
| 陰影 pass 重畫所有東西 | 草/小石不投影；陰影只在太陽角度變化 > 0.5° 時更新（推測：kool 每幀都畫 shadow pass，需要時可 `isEnabled=false` 跳幀）|
| skinned 動畫 | 姿態桶：動畫計算 K 次而不是 N 次 |
| 點光 | deferred 才開；Medium 以下 0 盞，用 emissive + bloom 假裝 |
| 動態 IBL | 低頻更新或預烘 |
| 巢穴 pass | 小解析度、只在巢穴內容變化時重畫（暫停時可跳過）|
