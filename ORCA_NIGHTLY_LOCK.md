# OrcaSlicer Nightly lock

- Source: `https://github.com/OrcaSlicer/OrcaSlicer.git`
- Branch: `main`
- Commit: `f114ef7cc991a2069b00519ae45524a7ab0fdae2`
- Retrieved: 2026-09-19

This project keeps its Android JNI port in `app/src/main/jni`. The port is based on
SliceBeam and cannot safely consume the desktop source tree as a drop-in replacement:
the JNI bridge, CMake imports, and Android `arm64-v8a` dependency bundle must stay in
sync with the engine sources.

The remote `orcaslicer-nightly` tracks the locked official upstream revision. Update
the lock only after the Android port compiles, slices a fixture model, and its G-code
is verified in the app preview.
