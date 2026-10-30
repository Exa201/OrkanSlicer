# OrcaSlicer 2.5 Android-core integration

The Android app uses the SliceBeam UI and a platform-adapted OrcaSlicer engine in
`app/src/main/jni`. The Android port is not a desktop source drop-in. There is no
`orcaslicer-nightly` Git remote configured in this checkout; upstream revisions must be
fetched explicitly and recorded in `ORCA_NIGHTLY_LOCK.md`.

## Reviewed upstream (2026-10-30)

- Official repository: <https://github.com/OrcaSlicer/OrcaSlicer>
- Current `main`: `98b539c3e6b44e87bbde950e26d1b23c66691a0a`
- Latest 2.5 prerelease: `v2.5.0-beta`, published 2026-10-27
- Official release assets are desktop distributions; none is a drop-in Android
  `libslic3r.so`.
- The existing Android source baseline recorded in `ORCA_NIGHTLY_LOCK.md` is not
  sufficient proof of the exact code ancestry. The lock remains unchanged until that
  ancestry and an Android build are verified.

## Safe update sequence

1. Work in the separate `Exa201/OrkanSlicer-Nightly-2.5` repository; keep the stable
   app repository and its release branch independent.
2. Pin upstream source and profiles to reviewed immutable commits. Record each commit,
   license notices, submodule revisions, and generated dependency versions.
3. Compare `src/libslic3r`, profile schemas, CMake source lists, and native APIs against
   the Android port. Port compatible changes in small commits; do not replace JNI,
   Android CMake imports, or bundled ARM64 libraries wholesale.
4. Resolve each new upstream dependency for Android ARM64 and verify ABI, runtime
   licensing, and APK size before enabling its feature.
5. Build `libslic3r.so`, run native/unit tests, slice fixed fixtures, then verify
   single- and multi-material assignments, layer metadata, thumbnails, and
   printer-specific G-code in OrkanSlicer.
6. Advance the engine lock only after the above checks pass on the exact recorded
   upstream commit. Until then the 2.5 branch is experimental and the current app is
   still on its previously released engine baseline.

## Historical comparison

An earlier source comparison recorded 545 upstream `src/libslic3r` files versus 496 in
the Android port, and 116 added CMake lines at that time. Those counts predate the
reviewed 2.5 beta and are historical, not a current 2.5 diff. A fresh pinned source
comparison is required before claiming which 2.5 features are missing from the Android
app.
