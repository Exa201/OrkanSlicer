# OrcaSlicer 2.5 Android-core integration

The Android app uses the SliceBeam UI and a platform-adapted OrcaSlicer engine in
`app/src/main/jni`. The Android port is not a desktop source drop-in. There is no
`orcaslicer-nightly` Git remote configured in this checkout; upstream revisions must be
fetched explicitly and recorded in `ORCA_NIGHTLY_LOCK.md`.

## Reviewed upstream (2026-10-30)

- Official repository: <https://github.com/OrcaSlicer/OrcaSlicer>
- Current `main`: `98b539c3e6b44e87bbde950e26d1b23c66691a0a`
- Latest 2.5 prerelease: `v2.5.0-beta`, published 2026-10-27
- `main` has 67 commits since the beta comparison point and diverged by 5 commits.
- Official release assets are desktop distributions; none is a drop-in Android
  `libslic3r.so`.
- The existing Android source baseline recorded in `ORCA_NIGHTLY_LOCK.md` is not
  sufficient proof of the exact code ancestry. The lock remains unchanged until that
  ancestry and fixed slicing fixtures are verified. The ARM64 build now passes, but
  build success alone does not prove slicing correctness.

## Safe update sequence

The selective ports on `nightly-2.5` include empty-layer guards, 64-bit filament masks,
AMS same-filament preset preservation, Qidi initial-tool filenames, and per-filament
retraction threshold/Z-hop enforcement. The latter is adapted to this Android fork's
filament-ID indexing; upstream's newer variant-column API is not present here. The
0.8.413 ARM64 native build succeeded and Debug/Slice JVM suites pass 686/686 each.
A dedicated multi-filament lift test was added and compiles into the instrumentation APK,
but it has not run because no ADB device is connected. Fixed slicing fixtures and printer
validation remain necessary, so this is not a completed engine migration.

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
   upstream commit. Until then the 2.5 branch is experimental, and its native engine
   baseline remains the previously released Android port with selective backports.

## Current source comparison

At the reviewed `main` commit, `src/libslic3r` has 609 paths versus 496 in the Android
port: 130 upstream-only paths and 17 Android-only paths. New source groups include
Belt printing, CAD/sketch solving, and G-code sender support. Android-only code includes
the mobile postprocessor/sender and geometry helpers; these files must be preserved.

The first two upstream core fixes are ported on `nightly-2.5`: return safely when a
thin model produces an empty layer series, and store/serialize filament-to-unprintable-
extruder masks as 64-bit values. The full 2.5 lock remains pending a native build, a
device slice, and G-code/preview checks.
