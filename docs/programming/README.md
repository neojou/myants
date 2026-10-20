# 設計文件索引

產品目標與現在做到哪裡在 [`../PLAN.md`](../PLAN.md)。這份只回答「該讀哪一份」。與 overview 衝突時，已落地的行為以 `myants_antsim_deviations.md` 為準。

不要一次讀完整個目錄。先讀 PLAN 點名的檔。

## 還算數

| 檔 | 角色 |
|---|---|
| `myants_antsim_deviations.md` | 程式實際與 AntSim_V2 的差異，含已踩過的 kool 坑。 |
| `antsim_simulation_model_ref.md`、`antsim_feature_inventory_ref.md`、`antsim_ui_ux_ref.md` | 上游行為、功能與 UI。文案維持英／德。 |
| `3d_visual_overhaul_overview.md` | 3D 目標、架構與分期。開頭寫的 v0.7.0「現況」已過期，現況看 PLAN。 |
| `rendering_pipeline_kool.md` | kool 0.19.0 渲染路徑與已查證 API。 |
| `camera_modes_and_ant_pov.md` | 俯視相機，以及將來第一人稱的預留（該檔 P6）。介面在，模式未接上。 |
| `world_scene_composition.md` | sim 座標對到 3D 場景。 |
| `assets_gltf_pipeline.md` | glTF 素材、授權與程序幾何退路。 |
| `ui_hud_aesthetics.md` | HUD 版面與色票。 |
| `perf_and_quality_presets.md` | 品質檔位與效能預算。預算數字尚未在這台 Mac 上量過。 |
| `3d_refs/shots/REVIEW.md` | 現有截圖對 HUD 清單的自評。 |
| `THIRD_PARTY_NOTICES.md` | 第三方授權。 |
| `How-To-Change_Project_Name.md`、`How-To-Change_Version_number.md` | 改名與改版。版本只走 `./configure.sh version`。 |

## 歷史，不要當成現況

`kool_hello_world_ref.md`、`kool_setup_ref.md`、`kool_rendering_mapping_ref.md`、`kool_study_migration_plan_ref.md`、`kool_study_trial_diff.patch`、`myants_migration_plan_ref.md`、`grok_build_plan_prompt.md`。

`3d_refs/feasibility_trial/` 是當初對 kool 0.19.0 編譯通過的試寫，不是現在的 `render3d` 程式。
