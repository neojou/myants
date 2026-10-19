# myants

Ant colony simulation for desktop (JVM) and the browser (Kotlin/JS), rendered with kool 0.19.0.

The world view is a real 3D scene (deferred PBR, instanced ants, glowing pheromones). Grafik → Renderer can switch to Classic 2D without restarting. The simulation behaviour and the English/German UI copy follow [AntSim_V2](https://github.com/nilsgollub/AntSim_V2) by nilsgollub, MIT license. See [docs/programming/THIRD_PARTY_NOTICES.md](docs/programming/THIRD_PARTY_NOTICES.md). Intentional differences are in [docs/programming/myants_antsim_deviations.md](docs/programming/myants_antsim_deviations.md).

```bash
./gradlew :koolApp:runDesktop
./gradlew :koolApp:jsBrowserDevelopmentRun
```

Desktop flags: `--seed=` `--colonies=` `--quality=` `--renderer=3d|2d` `--backend=vk|gl` `--no-bloom` `--shot=day|night|rain|ant` `--shot-out=path.png`. The default backend is Vulkan. `--backend=gl` is not the default on macOS.

Medium desktop stills are in [docs/programming/3d_refs/shots/](docs/programming/3d_refs/shots/) (`day`, `night`, `rain`, `ant`). Notes against the HUD checklist are in [REVIEW.md](docs/programming/3d_refs/shots/REVIEW.md).
