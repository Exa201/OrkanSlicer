# OrcaSlicer Android engine baseline and update lock

- Source: `https://github.com/OrcaSlicer/OrcaSlicer.git`
- Branch: `main`
- Current app engine baseline: `f114ef7cc991a2069b00519ae45524a7ab0fdae2` (previously recorded; exact Android-port ancestry still needs verification)
- Reviewed upstream `main` head: `98b539c3e6b44e87bbde950e26d1b23c66691a0a` (2026-10-30)
- Reviewed upstream beta: `v2.5.0-beta`, published 2026-10-27
- First 2.5 fixes ported: `59f0cc3806ab6424d081978fde331088c55d9f0f` (empty-layer crash guards), `5ea64864ef5f409c3422237263894e8ba1ea165e` (64-bit filament/extruder masks)
- Additional selective ports: `23fe91393658b7908ea42db1d0a4f2a65d643eef` (preserve compatible AMS filament presets), `9f39b8347f7492b11fdb905b220323001db96e6a` (Qidi active-tool output filename), `98b539c3e6b44e87bbde950e26d1b23c66691a0a` (per-filament retraction threshold and Z-hop enforcement; adapted to this port's filament-ID indexing), and `7125a546b` (avoid invalidating G-code for unchanged derived automatic filament mapping).
- The 0.8.413 ARM64 build passes with native identity 0.8.409 / build ID 444; Debug and Slice JVM suites pass 686/686. A dual-filament Z-hop G-code instrumentation test compiles but remains unexecuted without a device. Exact upstream ancestry and end-to-end engine behavior are not yet verified; the full 2.5 engine lock therefore remains unchanged.
- ARM64 `assembleDebug`, `assembleSliceTestAndroidTest`, and both JVM test tasks succeeded through app commit `5ec59ef50fdbf0768525a3859062a32c5e7a2f81`; Debug and Slice JVM suites passed 686/686 each. No ADB device was connected; neither the thin-object nor multi-filament lift instrumentation test has run, and no fixed 3MF/STL fixture has been verified for 0.8.414. APK SHA-256: `55A8490808D4C7D8DD69015D3A6D4569653CF6160FDF17F3DF76455DDCD95ED4`. Android engine lock: **not yet advanced to 2.5**; port remains selective.

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
