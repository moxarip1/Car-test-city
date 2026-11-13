# Hypercar Resort & Stunt City

A 60 FPS 3D open-world luxury car driving and stunt game built natively for Android using **Kotlin**, **Jetpack Compose**, **OpenGL ES 3.0**, and procedural **AudioTrack** sound synthesis.

## Features
- **3 Procedural Luxury Hypercars** (`EV Hyper-GT`, `V12 Apex Roadster`, `VIP Executive Coupe`) with live mid-drive switching, 7 paint colors, and `Glossy` / `Matte` / `Chrome` clearcoat finishes.
- **4-Mode Camera Switcher** (`Dynamic Chase`, `Side-Action Tracking`, `Hood`, `Cinematic Orbit`).
- **Open-World Coastal Resort City**: Glass skyscraper district with procedural lit windows, palm-lined boulevards, grand central drift roundabout, neon acceleration tunnel with boost pads, dual hydraulic stunt ramps over a marina canal, and an elevated rooftop helipad landing zone.
- **Landscape Mobile Ergonomics & Arabic RTL HUD**: Locked `sensorLandscape` orientation, hardware-accelerated rendering, multi-touch steering/pedal controls, and keyboard support (`WASD` / Arrows, `Space`, `C`, `F`, `R`, `M`).

## Repository Structure & Size Isolation
- `app/` — Native Android Gradle module (Kotlin, Compose, OpenGL ES 3.0 shaders & procedural meshes/textures).
- `debug/` — Repository-root output directory for the automated `debug/debug.apk` build (`debug/.gitkeep` placeholder initially). Kept strictly at root outside `app/src/main/` and excluded in `app/build.gradle.kts` (`packaging.resources.excludes`) so APKs are never recursively bundled inside future APKs.
- `.github/workflows/build-debug-apk.yml` — Automated CI pipeline.

## Automated `debug/debug.apk` GitHub Pipeline
Every push to `main` or `master` (or manual `workflow_dispatch`) triggers `.github/workflows/build-debug-apk.yml`:
1. Sets up **JDK 17** and **Gradle 9.3.1** (matching `gradle/wrapper/gradle-wrapper.properties`).
2. Prepares `debug.keystore` from `debug.keystore.base64` and verifies no `*.apk` or `debug/` assets exist under `app/src/main/`.
3. Compiles the debug APK via `gradle assembleDebug`.
4. Copies the output to `debug/debug.apk`, commits and pushes it back with `[skip ci]` to prevent infinite workflow loops, and uploads `hypercar-resort-debug-apk` as a downloadable GitHub Actions artifact.

> **Note on Web / Capacitor Files**: Because this repository is a native Android Gradle/Kotlin/OpenGL ES 3.0 project (with no `package.json`, React/Vite web build, or Capacitor `android/` wrapper), `vite.config.ts`, `capacitor.config.ts`, and a GitHub Pages web deployment workflow are not included—everything builds and runs directly via Gradle.
