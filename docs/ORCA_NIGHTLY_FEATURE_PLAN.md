# OrkanSlicer Nightly 2.5 — Portierungsplan

Stand: 2026-10-31. Upstream-Vergleich: OrcaSlicer `main` at
`7ac46c9a6186458633a8c52203179c684c23fadc` (2026-10-30). This is a selective Android
port plan; the app must not be described as containing the complete upstream engine.

## Verified preview-performance finding

Desktop Orca exposes OpenGL MSAA sample counts (off/2/4/8/16), optional FXAA, and an FPS
cap. The Android workspace instead uses a GLES3 `GLSurfaceView` whose EGL config and
renderer lifetime are established by the existing host. Directly changing the desktop MSAA
setting is not compatible without redesigning that surface setup. OrkanSlicer already scales
the GL surface and maps touch coordinates to it, so the safe Android equivalent is a user
selectable viewport render scale. It changes only display pixels: native model data, slicing,
G-code, and export are untouched. Automatic mode retains the low-RAM device policy.

## Product identity / data isolation

Nightly uses Android application ID `com.exa201.orkanslicer.nightly25` and display name
`OrkanSlicer Nightly 2.5`. Android scopes private preferences, files, cache, and app-specific
external storage to the application ID; therefore Nightly installs beside the stable app with
separate data. The Kotlin namespace remains `ru.ytkab0bp.slicebeam` to avoid a needless source
and JNI package migration. SAF files explicitly selected by the user remain shared documents.
The Nightly updater queries the Nightly repository and selects published prereleases rather
than the stable app's `/releases/latest` endpoint.

## Existing Android parity verified from source

Already present: Move, Rotate, Scale, Mirror, auto-orient, flatten/lay-on-face, arrange,
duplicate/fill/center/delete/select, plane cut and connectors, color/support/seam/fuzzy paint,
text emboss/engrave, measure, variable layer-height profile, split to objects/parts, mesh
simplification, and Boolean union/difference/intersection. Do not reimplement these as new
features; extend them only where a demonstrated parity gap remains. The older
`ORCA_FEATURE_AUDIT.md` Boolean row is stale relative to `ModelBooleanDialog`,
`GLRenderer`, and the current Boolean workspace path.

Profile parity is substantial: the current audit records 887 parsed native options and 492
visible routed profile rows (319 process, 101 filament, 72 printer). Those figures measure
source routing, not desktop completeness or runtime verification.

## Prioritized feature backlog

| # | Candidate | Current evidence / gap | Acceptance evidence |
|---:|---|---|---|
| 1 | Low-GPU preview quality control | Implemented: automatic mode uses Android low-RAM status plus a conservative `GL_RENDERER` classifier for known software/legacy GPU families (70% on low-RAM, 80% on recognized low-end GL, 100% for unknown/current hardware); explicit 50/65/80/100% viewport choices remain available. Renderer identity is runtime-only. Fixed the automatic choice label so it reports the device policy even while a manual override is active. | Policy JVM tests pass, including renderer families and override precedence; Android 16 emulator `SettingsMenuActionDeviceTest#viewportQualityCanBeChangedAndAutomaticModeUsesDevicePolicy` passed 1/1 and verified GLES renderer discovery plus returning to the true automatic policy. Existing live surface/picking checks at 50% and 100% remain; physical-device performance benchmarking is pending. This is a conservative renderer-name heuristic, not a measured GPU benchmark. |
| 2 | Compound 3MF paint ownership | Implemented: each model-part volume owns a stable heap-allocated mesh, selector and AABB raycaster; world-space hit testing returns the source volume. Paint, undo/redo, clear and commit address the selected part without overwriting other parts. | Emulator ARM64 `TwoColorProjectDeviceTest` 5/5 passed, including independent painting of two combined 3MF parts, live overlays, undo/redo, 3MF export/reload and generated G-code with the expected T1 tool change. Physical-device validation remains outstanding. |
| 3 | Qidi Box slot mapping | Implemented: active G-code tools can map to Qidi Box slots 1–16; repeated slots are allowed, unmapped tools retain the printer setting, and confirmed choices persist per printer/host in Nightly-private preferences. Orca 2.5 Qidi agent semantics use `SAVE_VARIABLE value_tN` before `/printer/print/start`; the guarded Moonraker sequence is implemented. | Policy tests 5/5, dialog selection/persistence/cancel emulator tests 2/2, source compilation. Actual Qidi firmware/network behavior remains unverified without a Qidi device. |
| 4 | Orca logical-to-physical extruder map | Implemented: the printer profile exposes Orca's `physical_extruder_map`; the vector editor supplies an identity default, validates one zero-based target per logical extruder, and permits repeated targets. Sparse custom multi-nozzle profiles now skip absent/empty variant vectors instead of dereferencing them during native slicing. | Policy/catalog JVM tests 8/8; Android 16 ARM64 native slice class 51/51; filament/MMU sync regression 15/15. A real MMU/AFC device workflow remains unverified. |
| 5 | Multipart per-part filament reassignment | Implemented: the expanded object list exposes a dedicated filament action for each printable part. The native GL-thread edit changes only that volume, clears only its facet-level material map, refreshes the live mesh, and preserves sibling volumes. | Compose action test 1/1; Android 16 ARM64 `TwoColorProjectDeviceTest` 6/6, including slot swap, 3MF save/reload and native slice. Generic arbitrary process/filament setting overrides remain a later gap. |
| 6 | General modifier volumes | Implemented: the Model menu can add, position, resize and remove Orca-native negative volumes, support blockers and support enforcers on the selected object. Their typed meshes render as translucent, color-coded overlays and remain separate from printable parts. Existing modifier volumes can be inspected, resized and removed. The Orca 3MF importer now accepts the type strings emitted by its own exporter; box geometry is centered consistently so coordinates survive round-trip. | Android 16 ARM64 tests passed: Compose type/creation input 1/1; native add/edit/delete plus undo/redo snapshots and 3MF position/size/type round-trip 1/1; native slice from the reloaded project 1/1, with changed extrusion toolpaths and printable output. Per-volume arbitrary parameter-setting editing is not included in this geometry feature. |
| 7 | SVG surface emboss/engrave | Implemented in source: SAF SVG selection with bounded/secure validation, user-controlled width/depth, GL surface-tap placement, raised printable part or native negative volume. SVG curve tessellation uses a 0.05 mm target error rather than a coarse fixed tolerance. SVG mode now permits an empty unused text field, enforces the exact native width/depth range in Compose, and reports an invalidated target instead of a false success. | ARM64 native build, Kotlin compilation, focused SVG/text JVM tests (including boundary and empty-text cases) and instrumentation-test compilation pass. A new end-to-end test compares sliced extrusion toolpaths before/after raised and engraved SVG, checks 3MF reload and printable output. Runtime test is pending: only x86_64 emulator is attached, while prebuilt OpenCascade/native dependencies are ARM64-only. Live placement preview and manual cancel-before-commit are still open; current operation commits on tap and remains undoable. |
| 8 | 3MF transform and parser-size parity | Ported Orca 2.5 upstream fixes `67a976e0` (preserve valid mirrored instance matrices in both Prusa/BBS importers) and `d35ea27e` (reject XML ZIP entries above `INT_MAX` before Expat buffer allocation; use the checked size consistently). Added native round-trip tests for mirrored transforms and a small handcrafted ZIP64 archive whose XML entry claims a size above `INT_MAX`, with expected rejection as a normal import error. | ARM64 native build and instrumentation-source compilation pass. Device execution is pending because only x86_64 emulator is attached and app dependencies are ARM64-only. The ZIP64 and transform tests are not counted as runtime verification until an ARM64 run succeeds. |
| 9 | Qidi profile parity | Compared local Q2/Q2C profile behavior with current Orca 2.5 profile changes. Q2/Q2C nozzle variants use the selected single-bed initial-layer temperature for `PRINT_START` and `M140`, start with `[initial_tool]`, and initialize Qidi Box mapping; material-aware filenames are present. Added profile checks for every Q2/Q2C nozzle section and an ARM64 native slice test asserting that generated Q2 G-code resolves a selected bed temperature and initial tool. | Qidi profile/Box/package JVM tests pass 14/14; native G-code test source compiles but cannot run on the currently attached x86_64-only emulator because the app/native dependencies are ARM64-only. Printer firmware/Moonraker behavior remains hardware-unverified. |
| 10 | Orca profile-option gap refresh | Re-ran the route audit against 887 native options and 492 visible setting rows. Found `physical_extruder_map` in the visible printer catalog but missing from `PRINTER_CONFIG_KEYS`, which controls import/export routing; added it to the printer key set and a persistence/import regression test. Updated the route checker to follow the current Compose render-scale, navigation, workspace sync, color-editor and profile-import APIs instead of stale pre-migration names. | `check_profile_setting_routes.py` passes: all 492 visible rows resolve to native definitions/profile parsers and 107 German group labels are unique. JVM key persistence test passes 1/1. |

### Later candidates

- Review upstream 2.5 fixes in bounded, pinned groups, each with a concrete regression and native
  or device evidence; do not bulk-copy commits or advance the engine lock prematurely.
- Validate frame pacing/FPS cap against Android battery/latency behavior after render-scale
  control is measured. Desktop preference semantics are not presumed to transfer to Android.
- Reassess other Orca desktop functions only after inspecting their native dependencies, Android
  UI feasibility, mobile memory cost, and license/build requirements.

### OrcaSlicer 2.5 Alpha capability review (2026-10-31)

The official 2.5.0 Alpha notes describe a large desktop release, not a drop-in Android package.
The following candidates are ranked by whether they can safely use the existing native engine and
mobile interaction model. Do not count a candidate as implemented until its Android action and
regression evidence exist.

| Upstream capability | Android evidence / feasibility | Port decision |
|---|---|---|
| Texture Displacement | Native mesh displacement and texture sampling could run on Android, but it can greatly increase triangle count and RAM. | High-value later candidate; prototype bounded resolution, cancellation, memory guard, undo/redo, and slice parity before exposing. |
| Parametric Design tab | Requires a sketch/CAD constraint solver, feature tree, replay, and 3MF persistence; it is a separate CAD subsystem. | Do not imitate with static mesh generators. First scope solver dependencies and an independently versioned parametric project format. |
| Python plugins | Arbitrary code, filesystem, network, and process access create a large security and lifecycle surface on Android. | Not a direct port. Consider only a sandboxed, permissioned extension API after threat modeling; no arbitrary plugin execution in this plan. |
| Color mixing | Depends on compatible multi-input hardware and firmware semantics, unlike ordinary single-nozzle MMU tool changes. | Defer until a supported mixer hardware profile and firmware-safe G-code contract are identified. |
| IDEX/IQEX parallel printing | Requires independent carriage collision planning and printer-specific G-code/start sequencing. | Defer pending a complete, testable dual-carriage printer profile; never infer support from generic extruder count. |
| Belt printers | Coordinate transforms, bed geometry, and print sequencing differ from planar beds. | Defer until belt printer machine limits and G-code are ported as a coherent profile family. |
| Speed Dial / action launcher | Mobile already has searchable menus and configurable bottom actions; parity should not duplicate those entry points. | Revisit only if the menu audit finds a measured discoverability gap. |
| Faster startup, slicing, and G-code export | Upstream contains many independent native changes and fixes. | Port individually against pinned commits with Android ARM64 regression tests; do not mass-update the engine lock. |
| Precise Seam modifiers and new infill options | Some native engine support may already exist; UI coverage and serialization need verification. | Audit current keys/enum paths first, then expose only safe, tested gaps in backlog item 10. |
| Mixed nozzle sizes / new printer support | Depends on per-tool nozzle geometry, profile definitions, and G-code validation. | Treat as profile-family work with fixture-based export tests, not a generic UI toggle. |

Primary upstream source: [OrcaSlicer V2.5.0 Alpha release notes](https://github.com/OrcaSlicer/OrcaSlicer/releases).
The existing backlog items 7–10 remain the active implementation sequence; this review adds candidates
for prioritization after those items and is not itself counted as a shipped feature.

## Delivery cadence and gates

1. Keep a numbered implementation log for the ten backlog items above.
2. For each item, inspect pinned upstream source and Android dependencies, implement only a safe
   adaptation, and add focused regression tests.
3. After each group of ten completed feature items, run grouped JVM/native/device verification,
   create a Nightly APK, inspect package identity and APK metadata/hash, then publish a Nightly
   GitHub release and update the changelog. Do not claim unsupported device tests passed.
4. Task #1 has code, policy/settings-flow tests and a live Android 16 emulator interaction: GL surface
   size and M12 ray picking passed at 50% and 100% render scales; physical-device check is pending. Task #2 passes
   `TwoColorProjectDeviceTest` 5/5 on the Android 16 ARM64 emulator, including distinct-volume
   painting, undo/redo, 3MF reload and generated G-code with the expected T1 tool change. Task #3
   passes mapping policy tests 5/5 and Qidi dialog emulator tests 2/2 (selection, persistence, cancel);
   the upstream `SAVE_VARIABLE` command and fail-closed-before-print path compile, but a live Qidi
   firmware/network exchange is not verified. Three items (#1–#3) have implementation and focused emulator coverage (3/10); physical-device
   and physical-printer validation remain outstanding.
   Items #4 and #5 are implemented and covered by focused tests: 5/10 feature items. The physical extruder-map work passed policy/catalog JVM 8/8, `NativeSliceDeviceTest` 51/51, and filament/MMU sync 15/15; the part-level assignment passed its Compose action test 1/1 and `TwoColorProjectDeviceTest` 6/6 on Android 16 ARM64, including 3MF round-trip and native slicing. Generic arbitrary process/filament overrides and real MMU/AFC device validation remain open. No APK or release is created from this partial batch.

## Source audit documents

- `docs/ORCA_FEATURE_AUDIT.md` — profile and model-tool inventory; update stale rows when the
  current source proves the implementation exists.
- `docs/ORCASLICER_2_5_DELTA_AUDIT.md` — pinned upstream fixes and Android-port constraints.
- `ORCA_NIGHTLY_LOCK.md` — do not advance until engine identity and native test evidence justify it.

## Next Orca 2.5 parity batch (items 11–20)

Candidate selection is based on upstream commits present in `orcaslicer-upstream/main` as of
2026-10-31. These are scoped targets, not completion claims. Each requires Android-specific
regression evidence before it can count toward an APK/release batch.

| # | Upstream change | Android adaptation / acceptance evidence | State |
|---|---|---|---|
| 11 | `7ac46c9a6` Lightning overhang samples outside the infill bounds can make tree generation loop indefinitely. | Ported in `Fill/Lightning/DistanceField`: expand the searchable box to all unsupported sample points. ARM64 native build and Lightning slice-test compilation pass; runtime and exact overhang regression remain open. | Implemented, unverified on device |
| 12 | `9b4f84a92` Preserve every extruder's values when a custom printer has more nozzles than its parent. | Audit all Android preset inheritance paths; preserve extra nozzle vectors for local, project-embedded, and cloud presets. Add a parent-with-2 / child-with-4 regression and verify all four values survive load/save. | Ported in all three inheritance paths; ARM64 build passed, runtime round-trip still open |
| 13 | `fcd96ad91` Apply each object's own extruder layer-height limits and compare actual layer heights for prime-tower compatibility. | Verify `Slicing.cpp` against the local per-object extruder assignment model; test two objects assigned to nozzles with different min/max layer heights and tower generation. | 0-based nozzle lookup and premature min/max tower rejection ported; ARM64 build passed, runtime scenario open |
| 14 | `a4a510087` Keep build-plate-only tree supports inside XY clearance. | Equivalent collision geometry is already used by Android tree-support generation; native build evidence is recorded in `ORCASLICER_2_5_DELTA_AUDIT.md`. Boundary runtime scenario remains open. | Port already present; runtime test open |
| 15 | `98b539c3e` Use the printing filament's retraction-lift settings. | Android already reads lift thresholds/Z-hop from the active logical filament ID, and uses the same ID for filament-scoped retraction policy. This is the fork's equivalent to upstream `config_index()`; a two-filament G-code regression exists but still needs ARM64 execution. | Port already present; runtime test open |
| 16 | `b7ecf1b29` Avoid false out-of-printable-range warnings for an extruder area inset on multiple sides. | Android already intersects the actual unprintable polygons with the path bounds; asymmetric multi-side inset boundary runtime scenario remains open. | Port already present; runtime test open |
| 17 | `29e189957` Ensure sparse layers exist during a tool change. | Both Android wipe-tower generators emit `restore_layer_z_before_toolchange`; `NativeSliceDeviceTest#compactedTowerRestoresModelLayerBeforeCustomMulticolorToolchange` compiles. Run the regression on ARM64 to verify the toolchange Z and tower descent. | Ported; runtime test open |
| 18 | `4c7928d8a` Decide prime-tower need from the current model's filament set rather than the preceding model. | `Print::apply` now retains pre-normalized settings and re-evaluates after current model regions/filaments are rebuilt, so normalization can re-enable the tower. ARM64 native build and instrumented Kotlin compilation pass; a reused-Print runtime regression is still needed. | Ported; runtime test open |
| 19 | `59f0cc3806ab6424d081978fde331088c55d9f0f` Avoid indexing empty layer lists for objects too thin to create any layer when a raft is enabled. | Both guards are already present in brim grouping and precise-Z layer alignment. `NativeSliceDeviceTest` covers raft layers 0/1 and precise-Z on/off, expecting the normal “No layers were detected” error instead of a crash. ARM64 native build and instrumentation compilation pass; runtime execution remains open. | Port already present; runtime test open |
| 20 | `d28c71c48` Preserve enum-list config keys in default maps. | Static config initialization now builds `coEnums` defaults through `ConfigOptionDef::create_default_option`; `ConfigOptionEnumsGenericTempl::set` adopts the keys map when its static option has none. ARM64 native build passes; a focused serialization regression remains to be added/run. | Ported; regression test open |
| 21 | `27cafaa06` Read nozzle diameters correctly with a decimal-comma UI locale. | Desktop-only nozzle/device UI routes are absent from Android. In the shared Bambu 3MF parser, replace locale-sensitive `atof` with `string_to_double_decimal_point`. The Android importer already enters a C numeric-locale guard, so this is explicit format-parsing parity and defense in depth, not a demonstrated Android locale bug. ARM64 native build passes; no dedicated import fixture assertion yet. | Defensive parity port; not counted as a completed feature group |
| 22 | `7b6c12cf7` Match grouped filaments to those loaded in dual-nozzle printers. | Relevant only after loaded physical-tray identity reaches the Android `Print` object. Current `MachineFilamentInfo` has no filament ID; `build_full_machine_filaments` reads color/type/tray name only; `Print::set_extruder_filament_info` has no Android call site. The model-side `filament_ids` already exist, but that alone cannot match to loaded trays. Defer algorithm changes until this data path and a generated-tool regression are implemented. | Audited; plumbing prerequisite missing, no partial port |

### Current continuation — 2026-10-31

- The low-GPU viewport option is already implemented: automatic device policy uses low-RAM and known renderer hints; users can override it in App settings. It changes only GL surface resolution and preview mesh budgets, not slice geometry. Policy JVM tests and live emulator touch-coordinate checks are recorded in `UI2_EXECUTION_STATE.md`; physical GPU benchmarking remains open.
- Orca 2.5 candidates 14 and 16 were already equivalent in the Android core. Candidate 15 also already uses active logical filament IDs for retraction lift; that is the available Android equivalent to Orca's newer variant `config_index()` abstraction. Candidate 17 is ported in both wipe-tower generators with a dedicated compacted-tower G-code regression; ARM64 execution is outstanding.
- Candidate 18 is ported in `Print::apply`: final normalization now starts from a pre-normalized snapshot after current regions are rebuilt, so a prime tower can be re-enabled when the used-filament set changes. Candidate 20's enum-list static-default key-map fix is ported to `PrintConfig.hpp` and `Config.hpp`.
- Candidate 19's upstream identity is the thin-object/raft guard (not mixed-filament validation); the plan's prior hash/description was incorrect. Its two guards and instrumented regression already exist in Nightly. Orca's mixed-filament component-expansion fix is outside current scope because this Android fork has no mixed-filament composition model or `FilamentMixer` implementation; it must not be partially ported as a bed-check-only change.
- Full grouped verification on this worktree: ARM64 `:app:externalNativeBuildDebug`, instrumentation Kotlin compilation, Debug JVM 704/704, Slice JVM 704/704; zero test failures/skips. These unit tests do not execute native slicing. `adb devices` still shows only `sdk_gphone64_x86_64`; native/OpenCascade runtime tests require ARM64. No APK or release was created because the ten-feature verification gate is not met.
- Orca upstream advanced to `27cafaa06` (2026-10-31). Candidate #21's locale-safe nozzle parser is applied to the shared Bambu 3MF reader; its existing outer `CNumericLocalesSetter` already protects Android import, so the change is defensive parity and does not count as a newly verified feature. ARM64 native build passed; direct metadata round-trip coverage remains open.
- Next: add focused tests for candidates 18 and 20 and continue the pinned Orca 2.5 audit with a new compatible item; separately execute pending ARM64 runtime cases for candidates 11–19 and first-batch items 7–9 before counting them toward a release batch.

After each ten-item group reaches its explicit code and runtime gates, build and inspect the isolated
Nightly APK and publish the corresponding Nightly GitHub release. The current first group still has
items 7–9 awaiting ARM64 runtime execution; later-batch work does not close that gate.
