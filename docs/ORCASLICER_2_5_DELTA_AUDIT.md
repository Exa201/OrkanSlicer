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
| `59f0cc3806ab6424d081978fde331088c55d9f0f`: avoid indexing an empty layer list for objects too thin to create a layer | Guard the brim grouping path and precise-Z layer alignment; add a device regression test expecting a normal “No layers were detected” error | ARM64 native build and instrumentation APK compilation succeeded; no ADB device is connected, so the instrumentation test is not executed |
| `5ea64864ef5f409c3422237263894e8ba1ea165e`: extruder bit masks overflow at 32-bit shifts | Store and serialize `limit_filament_maps` as `uint64_t` and use 64-bit shifts | ARM64 native build succeeded; no Android execution or 32/63-nozzle regression test has been run |
| `23fe91393658b7908ea42db1d0a4f2a65d643eef`: retain custom filament preset overrides when printer sync reports the same filament | In `PresetBundle::sync_ams_list`, keep a compatible non-system preset only when its `filament_id` matches the synced preset; apply the check to mapped, placeholder, and append/deduplication paths | ARM64 native build and 685/685 Debug + Slice JVM suites passed; no device-level AMS-sync scenario was available |
| `9f39b8347f7492b11fdb905b220323001db96e6a`: use the material for the initial Qidi tool in output filenames | Change the 14 Qidi profile patterns that still selected `filament_type[0]`; add a regression test ensuring every Qidi material-aware filename pattern uses `initial_tool` | Debug and Slice JVM suites passed 686/686; ARM64 app and instrumentation builds succeeded. No physical Qidi was available |
| `98b539c3e6b44e87bbde950e26d1b23c66691a0a`: use the active filament's retraction threshold and Z-hop policy | Use filament-ID-indexed values in G-code travel/retract and in writer Z-hop boundaries; import and expose the per-filament lift-enforcement setting | ARM64 native and instrumentation APK builds succeeded; Debug/Slice JVM suites 686/686 each. A two-filament native G-code test was added but not run: no ADB device |
| `7125a546b`: don't invalidate G-code on unchanged auto-mapping state after slicing | Remove engine-derived `filament_map` from the full-config diff when the normalized map is carried forward | Full ARM64 build and instrumentation APK compilation succeeded for 0.8.414; Debug/Slice JVM suites 686/686 each. The upstream Catch2 repeat-apply regression isn't part of the Android test harness, and no device was connected for an end-to-end slice |

These are selective backports. The app must not be labeled as running the complete
OrcaSlicer 2.5 engine yet; `ORCA_NIGHTLY_LOCK.md` remains unadvanced.

The Android Qidi Q2 package path already embeds a validated `; thumbnail begin ...`
Base64 image block at the beginning of `Metadata/plate_1.gcode` and includes the PNG in
the package's plate/top/pick preview entries. Host tests compare the extracted image to
the captured preview and validate the G-code MD5; physical Qidi acceptance is still
unverified.

## High-priority 2.5 changes still to assess/port

1. Qidi Box mapping (`b6ca378b`): Android already uploads the packaged Q2 G-code 3MF
   through Moonraker and starts via `/printer/print/start`. The desktop-agent `SAVE_VARIABLE`
   Box-slot persistence is not ported; the Android UI does not yet provide a Qidi Box slot
   selection to safely populate `value_t<n>`. Printer-agent UI behavior is desktop-only.
2. Multi-material preset slot synchronization fixes (`5897d95d`, `23fe9139`,
   `db35f3d0`): map the desktop state changes to OrkanSlicer's filament profile and
   ACE/MMU sync flows without overwriting user selections.
3. Qidi filename and Q2/Q2C profile updates (`9f39b834`): the initial-tool filename fix
   is ported to the Qidi Android profile and covered by a host regression test. Broader
   printer profile changes still need a field-by-field compatibility comparison.
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
