# Nightly integration

The Android app uses the SliceBeam UI and a platform-adapted OrcaSlicer engine in
`app/src/main/jni`. The official OrcaSlicer `main` branch is tracked as the Git remote
`orcaslicer-nightly`; the reviewed revision is recorded in `ORCA_NIGHTLY_LOCK.md`.

The Nightly import is staged deliberately:

1. Refresh the remote and compare `src/libslic3r` with the Android port.
2. Import build-system changes together with every source file they introduce.
3. Preserve Android JNI methods and `arm64-v8a` CMake imports.
4. Build the native library against the matching Boost, oneTBB, OCCT, GMP, and MPFR
   prebuilt bundle.
5. Slice a fixed STL fixture, inspect the generated G-code in the preview, then move
   the lock to the tested upstream commit.

The current comparison found 545 files in Nightly `src/libslic3r` and 496 files in
the Android port. The Nightly CMake list introduces 116 added lines, including new
source groups such as Clipper2 utilities, texture-to-color processing, multi-nozzle
utilities, Assimp import, and optional CAD/SolveSpace integration. These changes need
their corresponding Android dependencies and JNI exposure before they are enabled.
