# OrcaSlicer Android engine baseline and update lock

- Source: `https://github.com/OrcaSlicer/OrcaSlicer.git`
- Branch: `main`
- Current app engine baseline: `f114ef7cc991a2069b00519ae45524a7ab0fdae2` (previously recorded; exact Android-port ancestry still needs verification)
- Reviewed upstream `main` head: `a4a51008798934b55ae766aa7cc41d61c40d11e6` (2026-10-30)
- Reviewed upstream beta: `v2.5.0-beta`, published 2026-10-27
- First 2.5 fixes ported: `59f0cc3806ab6424d081978fde331088c55d9f0f` (empty-layer crash guards), `5ea64864ef5f409c3422237263894e8ba1ea165e` (64-bit filament/extruder masks)
- Additional selective ports: `23fe91393658b7908ea42db1d0a4f2a65d643eef` (preserve compatible AMS filament presets), `9f39b8347f7492b11fdb905b220323001db96e6a` (Qidi active-tool output filename), `98b539c3e6b44e87bbde950e26d1b23c66691a0a` (per-filament retraction threshold and Z-hop enforcement; adapted to this port's filament-ID indexing), `7125a546b` (avoid invalidating G-code for unchanged derived automatic filament mapping), and `a4a510087` (tree supports use the XY collision-clearance region for build-plate-only continuation).
- The 0.8.413 ARM64 build passes with native identity 0.8.409 / build ID 444; Debug and Slice JVM suites pass 686/686. A dual-filament Z-hop G-code instrumentation test compiles but remains unexecuted without a device. Exact upstream ancestry and end-to-end engine behavior are not yet verified; the full 2.5 engine lock therefore remains unchanged.
- 0.8.415 ARM64 native/app/instrumentation builds and both JVM suites succeeded; Debug/Slice each 686/686. `TreeSupport.cpp` compiled with the new clearance behavior. APK SHA-256: `A4A479E891731E544203822843F2D6670A782D15199B476E88DCEEFAAE064DB7`. No ADB device was connected, so device slicing and printer behavior remain unverified. Android engine lock: **not yet advanced to 2.5**; port remains selective.

This project keeps its Android JNI port in `app/src/main/jni`. The port is based on
SliceBeam and cannot safely consume the desktop source tree as a drop-in replacement:
the JNI bridge, CMake imports, and Android `arm64-v8a` dependency bundle must stay in
sync with the engine sources.

The `orcaslicer-upstream` source reference is documented in `docs/NIGHTLY_INTEGRATION.md`;
the working repository is `Exa201/OrkanSlicer-Nightly-2.5`, branch `nightly-2.5`.
Do not mark the full engine lock as advanced until the Android C++ port compiles against
its pinned dependencies, slices fixed fixtures, preserves multi-material assignments,
and its G-code and preview are verified. The desktop 2.5.0-beta binary is not an Android
engine and cannot be copied into the APK.
