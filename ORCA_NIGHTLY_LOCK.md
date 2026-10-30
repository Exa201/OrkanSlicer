# OrcaSlicer Android engine baseline and update lock

- Source: `https://github.com/OrcaSlicer/OrcaSlicer.git`
- Branch: `main`
- Current app engine baseline: `f114ef7cc991a2069b00519ae45524a7ab0fdae2` (previously recorded; exact Android-port ancestry still needs verification)
- Reviewed upstream `main` head: `98b539c3e6b44e87bbde950e26d1b23c66691a0a` (2026-10-30)
- Reviewed upstream beta: `v2.5.0-beta`, published 2026-10-27
- First 2.5 fixes ported: `59f0cc3806ab6424d081978fde331088c55d9f0f` (empty-layer crash guards), `5ea64864ef5f409c3422237263894e8ba1ea165e` (64-bit filament/extruder masks)
- ARM64 `assembleDebug` and `assembleSliceTestAndroidTest` succeeded on 2026-10-30 after these source changes. The thin-object test source compiles; it has not run on a device, and no fixed 3MF/STL slice fixtures have been verified. Android engine lock: **not yet advanced to 2.5**; port remains partial.

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
