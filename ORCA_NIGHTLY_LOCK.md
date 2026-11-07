# OrcaSlicer Android engine baseline and update lock

- Source: `https://github.com/OrcaSlicer/OrcaSlicer.git`
- Branch: `main`
- Current app engine baseline: `f114ef7cc991a2069b00519ae45524a7ab0fdae2` (previously recorded; exact Android-port ancestry still needs verification)
- Reviewed upstream `main` head: `98b539c3e6b44e87bbde950e26d1b23c66691a0a` (2026-10-30)
- Reviewed upstream beta: `v2.5.0-beta`, published 2026-10-27
- Android engine lock: **not yet advanced**

This project keeps its Android JNI port in `app/src/main/jni`. The port is based on
SliceBeam and cannot safely consume the desktop source tree as a drop-in replacement:
the JNI bridge, CMake imports, and Android `arm64-v8a` dependency bundle must stay in
sync with the engine sources.

There is no configured `orcaslicer-nightly` Git remote in this checkout. Do not mark a
new engine commit as integrated until the Android C++ port compiles against its pinned
dependencies, slices fixed fixtures, preserves multi-material assignments, and its
G-code and preview are verified. The desktop 2.5.0-beta binary is not an Android engine
and cannot be copied into the APK.
