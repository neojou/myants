# AntSim_V2 模擬模型參考

所有數值取自 `src/config.ts`（已查證，commit `cefa083`），除非另註。

## 1. 時間步進
- **固定步長 = 1 tick = 1 次 `world.update()`**，沒有 dt；所有速率皆「每幀」。設計假設 60 fps（`queenStarveDeathTicks: 1200 // ~20s`）。
- 主迴圈 `main.ts:1096`：`steps = floor(simSpeed)` 次 update，再以 `Math.random() < simSpeed - steps` 機率多一次（只在 0.5× 時有效；**這裡用 Math.random，非種子 RNG**，所以 0.5× 不完全可重現）。
- 速度檔位 `SPEED_STEPS = [0, 0.5, 1, 2, 4, 8]`（`main.ts:126`）。
- Kotlin 建議：sim 以 60 Hz 固定 tick 累加器跑（與渲染 fps 解耦），每畫面最多跑 N tick 防止死亡螺旋。注意：原版是「每渲染幀一個 tick」，螢幕 120Hz 時原版會跑兩倍快；我們改固定 60Hz 是**刻意偏離**，在文件標註。

## 2. RNG（已查證 `src/rng.ts`）
mulberry32，可逐位元移植到 Kotlin（Int 溢位語意與 `|0`/`Math.imul` 相同）：
```kotlin
object Rng {
    private var state: Int = 0x9e3779b9.toInt()
    fun seed(seed: Int) { state = if (seed == 0) 0x9e3779b9.toInt() else seed }
    fun rand(): Double {
        state += 0x6D2B79F5
        var t = (state xor (state ushr 15)) * (1 or state)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }
}
```
- 驗收：seed=12345 前 10 個值與 JS 版相同（用 node 產生對照表寫進 commonTest）。
- 注意：全 sim 共用**單一全域** RNG；呼叫順序完全一致才可能 bit-exact。JS number 是 double、費洛蒙是 Float32Array → Kotlin 用 `Double` 做運算、`FloatArray` 存格點，可達成「同 seed 同結果」（推測，需 golden 測試證明）；完全 bit-exact 整個 sim 是**延伸目標**，不是硬性需求。

## 3. 世界尺寸（已查證 `config.ts:1-40`）
- `WORLD_SCALE = 1.4`，目標面積 `1000×600×1.4² = 1,176,000` 邏輯 px²。
- 寬高依「畫布框」長寬比計算：landscape 時畫布框 = 視窗寬 ×0.74 × 視窗高。`h = sqrt(area/aspect)`, `w = h*aspect`。無 DOM 時用 1000×600 的長寬比（→ 約 1400×840）。
- `areaFactor = w*h/600000`；`scaleCount(base)=max(base, round(base*areaFactor))`。
- 巢穴：landscape `nestWidth = w*0.28`, `nestHeight = h`。
- Kotlin 建議：**固定**世界尺寸 1400×840（aspect 5:3，與 headless 一致），讓 golden 測試可重現；渲染時用相機 letterbox 適配視窗。

## 4. 費洛蒙（已查證 `PheromoneGrid.ts`）
- 每蟻群各自一張戶外場 `outdoorField` + 一張巢內場 `nestGrid`（`Colony.ts`），每張 4 個 FloatArray：toHome/toSugar/toProtein/toDanger。
- 解析度 `resolutionScale = 0.3`（格數 = ceil(w*0.3) × ceil(h*0.3) ≈ 420×252）。
- 每 `updateSkip = 4` tick 更新一次：
  - 衰減：HOME ×0.990、SUGAR/PROTEIN ×0.994、DANGER ×0.95；小於 `minThreshold 0.001` 歸零。
  - 擴散（`diffusionEnabled=true`，只對 HOME；`diffuseFood=false`）：可分離 box blur，`rate=0.12`  
    水平：`tmp[i] = (1-r)*c + r*(l+r')*0.5`；垂直同理寫回並套 minThreshold；邊界外用中心值。
- 沉積：`deposit` 單格、`depositCircle` 半徑 `trailRadius=3` px（×scale 轉格）；值上限 1.0（`min(1, v+amount)`）。
  - `depositTrail 0.5`（HOME/DANGER），`depositFood 1.0 × quality`；quality = clamp(source.amount/`qualityRef 300`, `minQuality 0.2`, 1)（推測公式，詳見 `Ant.ts`/`antStates.ts handleReturning`）。
- 下雨時戶外場每 tick `scaleAll(0.97)`。
- 取樣 `get(x,y)`：floor(x*scale)。

## 5. 感測與轉向（已查證 `Ant.ts:239`）
- 三感測器：角度 0、±`antSensorAngle = π/4`，距離 `antSensorDist = 40`（戶外 × activityFactor）。
- 三者皆 < 0.05 → 回傳 false（呼叫端 wander）。
- center 最大 → `angle += (rand()-0.5)*0.05`；center 最小 → 隨機 ±`antTurnSpeed 0.15`；left>right → −0.15；right>left → +0.15。
- 移動：`x += cos(angle)*speed`, `y += sin(angle)*speed`；`antSpeed = 2.5` × 多型速度係數 × 夜間/雨天係數。
- 多型速度係數 `1/(0.45 + 0.55*sizeVar)`；耗能係數 `0.60 + 0.40*sizeVar`。
- 碰撞：圓-圓，阻擋時沿切線滑動（`Terrain.ts`）。
- 分散：入口 `dispersalRadius = round(280*1.4)=392` 內無蹤跡的覓食蟻被往外推，強度 0.10。site fidelity `memoryBias 0.3`。

## 6. 螞蟻 FSM（已查證 `Ant.ts:180-219`、`antStates.ts`）
```
           ┌──────── energy<hungry(200) ───────► HUNGRY ──吃到──┐
           │                                                   ▼
 IDLE/NURSING ──age urge / 緊急存量──► FORAGING ──找到食物──► HARVESTING ──► RETURNING ──入巢交貨──► NURSING/IDLE
   │   ▲                              │  │ └─遇蚜蟲─► MILKING ──► RETURNING
   │   └── RESTING ◄─ 夜/累            │  └─DANGER/敵人─► ATTACKING ◄──► FLEEING
   ├─► TRANSPORTING（搬育幼/屍體）      │
   └─► DIGGING（挖房）               兵蟻：PATROLLING ──強勢且冷卻到──► RAIDING（對敵巢）
```
（圖為簡化；確切轉換條件需讀 `antStates.ts` 各 handler，移植時逐一照搬。）
- Handler 位置：Nursing:11, Resting:73, NurseIdle:144, Transporting:285, Patrolling:331, Raiding:453, Fleeing:511, Combat:560, Foraging:722, Harvesting:974, Returning:1021, Hungry:1167, Milking:1207, Digging:1238。
- ARCHITECTURE 規則：目標消失時同幀回預設狀態；追蹤梯度訊號歸零立即放棄；腳動畫速度與移動速度耦合。

## 7. 全部參數（預設值；括號為 Params 面板滑桿 min–max/step，`main.ts:690-712`）
### 頂層
| key | 預設 | 滑桿 |
|---|---|---|
| initialWorkers | 15 | |
| soldierUnlockThreshold | 30 | |
| colonyCount | 1（config）；UI 預設 2 | Rivalenkolonie 勾選 |
| sugarValue | round(10*1.4)=14 | 1–40/1 |
| proteinValue | round(7*1.4)=10 | 1–30/1 |
| startSugar / startProtein | 600 / 300 | |
| sugarEnergyValue | 100 | 20–300/10 |
| colonyUpkeep | 0.001 | 0–0.01/0.0005 |
| queenSugarRegen | 0.3 | |
| broodProteinUpkeep | 0.0006 | 0–0.01/0.0005（另一個 0–0.005）|
| antSpeed | 2.5 | 0.5–6/0.1 |
| antSensorAngle | π/4 | |
| antSensorDist | 40 | 10–120/5 |
| antTurnSpeed | 0.15 | |
| workerHealth / soldierHealth | 20 / 60 | |
| workerDamage / soldierDamage | 3 / 8 | |
| antMaxEnergy | 2000 | |
| antEnergyDecay | 0.30 | 0.05–1/0.01 |
| queenPosition | 世界中心 | |
| eggCost | 20 | 5–60/1 |
| queenLayInterval | 180 | 50–800/10 |
| queenStarveDeathTicks | 1200 | |
| obstacleCount | 12 | |
| sugarSourceCount | scaleCount(4) | 1–10/1 |
| maxPrey / preySpawnRate | scaleCount(7) / 0.005*areaFactor | |
| gracePeriod | 4000 | |
| predator/spider/beetle/ladybugSpawnRate | 0.0005/0.0003/0.0003/0.0005 | predator 0–0.005/0.0001 |
| maxSpiders/Beetles/Ladybugs/Predators | 1/1/2/2 | |
### pheromone.*
resolutionScale 0.3, updateSkip 4, decay 0.990（0.9–0.999）, foodDecay 0.994（0.9–0.999）, dangerDecay 0.95, minThreshold 0.001, diffusionEnabled true, diffuseFood false, diffusionRate 0.12（0–0.5）, depositTrail 0.5, depositFood 1.0, trailRadius 3, qualityRef 300, minQuality 0.2。（頂層另有遺留 pheromoneDecay 0.990, evaporationRate 0.02。）
### brood.*
eggDuration 1000, larvaDuration 2000, pupaDuration 1500, larvaStarveLimit 200, hungerRate 0.005, soldierProteinLevel 150（0–500/25）, soldierFoodThreshold 1700（100–2000/100）, maxSoldierFraction 0.20。
### ant.*
lifespan 38000（6000–80000/2000）, lifespanJitter 12000, detectEnemyRangeSq 10000, attackRangeSq 900, arriveRangeSq 400, queenFeedAmount 500, larvaFeedAmount 50, hungryThreshold 200, foragingHungerThreshold 600, nurseEatThreshold 1000, restWakeThreshold 500, queenCriticalEnergy 1500, queenMaintainEnergy 1900, queenHungryEnergy 1800, dispersalRadius 392（0–600/20）, dispersalStrength 0.10（0–0.4/0.02）, nurseAgeFraction 0.2, forageAgeFraction 0.6, forageUrgeYoung 0.001, forageUrgeOld 0.02, memoryBias 0.3（0–1/0.05）, forageEmergencySugar 200, forageEmergencyProtein 40, proteinForagerShare 0.4（0–0.8/0.05）。
- poly：workerSize 0.80+0.25, soldierSize 1.25+0.25, speedBias 0.45, upkeepBase 0.60。
- troph：cropCapacity 600, donorMinEnergy 1400, recipientHungry 1700, donateRadius 16, donateChunk 200, interval 6, larvaHungry 25, larvaRelief 30, larvaCropCost 60。
### combat.*
alarmThreshold 0.04, mobMinAllies 2, mobSuperiority 1.5, grappleRadius 22, grappleSlowPerAnt 0.15, grappleMaxSlow 0.8, patrolLongChance 0.2, patrolLongRangeFrac 0.35, mobRushAllies 5, soldierRushAllies 2, rallyStandoffSq 48², soldierSightRangeSq 170², raidPopEdge 1.05, raidMercyRatio 1.25, raidMinSoldiers 3, raidPartySize 6, raidCooldown 1500, raidArriveRangeSq 70²。
### enemy.*
predator 42 HP/6 dmg, spider 32/7, beetle 88/12。
### environment.*
nightActivityMin 0.5, rainChance 0.00015, rainMinDuration 400, rainMaxDuration 1000, rainWashout 0.97, rainSpeed 0.7。World：dayLength 6000（`World.ts:62`）。
### nest.* / 其他
excavateEvery 10, maxExtraChambers scaleCount(14), storageBaseCapacity 1200, storagePerGranary 600, digWorkPerChamber 600, maxDiggers 4；nestScaleRef 300*1.4*(0.28/0.20)；nestRouteClearance 6；clock radius 150, fontSize 68, font Courier New。
### 品質預設 maxAnts（`PerformanceManager.ts`）
ULTRA_LOW 80, LOW 120, MEDIUM 300, HIGH 600, ULTRA 1200。注意 README 稱 sim 與品質解耦，但 maxAnts 是否影響 sim 需查（推測：影響蟻口上限 → 會影響 sim；移植時固定用 MEDIUM 300 當 sim cap）。

## 8. World.update 順序（已查證 `World.ts:267-600`）
1. `age++`；`timeOfDay = (age % 6000)/6000`
2. 天氣：下雨倒數／`rand() < rainChance` 開始下雨（duration 400 + floor(rand()*600)）；下雨時 scaleAll(0.97)
3. 每 4 tick 各蟻群 outdoorField/nestGrid `update()`
4. 各蟻后 `update`
5. 重建 spatialGrid（所有蟻）、foodGrid、insectGrid
6. 昆蟲生成/更新（:409）、食物（:460）、墓地（:468）
7. 育幼更新與孵化（:557）
8. 螞蟻 update（:585）
**移植必須保持此順序與每一個 `rand()` 呼叫點**，否則 golden 會偏。

## 9. 蟻群（已查證 `Colony.ts`）
每群：queen, nest, nestGrid, outdoorField, ants[], brood[], sugar/proteinStockpile, trophallaxisCount, raidCooldown, digSite/digProgress, entranceSide（RIGHT/LEFT/BOTTOM/TOP）, workerTint/soldierTint/teamColor, entranceWorld/worldExitPoint/... 。Colony 0 入口在右側（landscape），rival 在對側（推測 LEFT）。
