# OrcaSlicer 2.5 Android port delta audit

Reviewed 2026-10-30 against official OrcaSlicer `main` commit
`98b539c3e6b44e87bbde950e26d1b23c66691a0a` and beta tag `v2.5.0-beta`
(`345efec64c7b95a05ce5cf96df0dde66a158835c`).

## What the comparison proves

- `src/libslic3r`: 609 upstream paths, 496 Android paths, 130 upstream-only paths,
  17 Android-only paths. This is a path-count comparison; it does not imply that all
  609 files differ or can be copied directly.
- Upstream-only families include Belt printing, CAD/sketch/constraint solving, and
  additional G-code sender code.
- Android-only code includes `GCode/PostProcessor`, `GCodeSender`, Clipper2 helpers,
  JumpPointSearch, and Shiny headers. Preserve these mobile integrations.
- Since the 2.5 beta comparison point, `main` has 67 commits and is 5 commits diverged.
  The complete commit delta is not equivalent to Android-ready features.

## Core fixes already ported on `nightly-2.5`

| Upstream change | Android port | Verification |
| --- | --- | --- |
| `59f0cc3806ab6424d081978fde331088c55d9f0f`: avoid indexing an empty layer list for objects too thin to create a layer | Guard the brim grouping path and precise-Z layer alignment; add a device regression test expecting a normal “No layers were detected” error | Android test source added; no ADB device is currently connected, so the instrumentation test is not yet executed |
| `5ea64864ef5f409c3422237263894e8ba1ea165e`: extruder bit masks overflow at 32-bit shifts | Store and serialize `limit_filament_maps` as `uint64_t` and use 64-bit shifts | Native compile pending in this branch; the upstream test covers nozzle indices 1, 31, 32 and 63 |

These are selective backports. The app must not be labeled as running the complete
OrcaSlicer 2.5 engine yet; `ORCA_NIGHTLY_LOCK.md` remains unadvanced.

## High-priority 2.5 changes still to assess/port

1. Qidi Box mapping and printer-agent start behavior (`b6ca378b`): compare per-filament
   Box persistence and print-start semantics with Android's Qidi Moonraker upload path.
2. Multi-material preset slot synchronization fixes (`5897d95d`, `23fe9139`,
   `db35f3d0`): map the desktop state changes to OrkanSlicer's filament profile and
   ACE/MMU sync flows without overwriting user selections.
3. Qidi filename and Q2/Q2C profile updates (`9f39b834`): update only after comparing
   the Android profile format and verifying output names and slot metadata.
4. 3MF variant round-trip and import/export fixes (`c0efd44e`, `45a29d39`): review the
   Android 3MF serializer before porting.
5. Belt printing and CAD/design functionality: blocked on Android UI, geometry,
   dependency, memory, and interaction design; these features are not automatically
   available merely because their desktop source exists.

## Port rules

- Pin upstream and profile commits; do not chase moving `main` during a build.
- Import source with its CMake references and dependencies together.
- Keep JNI entry points, Android CMake imports, bundled ARM64 libraries, and all
  Android-only source paths intact.
- Run ARM64 native build, native/unit tests where available, and fixed STL/3MF slices.
- Verify model colors, assigned tools, extrusion G-code, layer metadata, previews, and
  printer-specific upload packaging before moving the engine lock.
