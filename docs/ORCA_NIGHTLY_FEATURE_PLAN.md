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

Profile parity is substantial: the current audit records 887 parsed native options and 490
visible routed profile rows (319 process, 100 filament, 71 printer). Those figures measure
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
| 7 | SVG surface emboss/engrave | SVG-to-mesh conversion exists; surface-following SVG placement/depth flow is missing. | Import SVG, preview placement/depth, commit/cancel, preserve printable mesh and undo/redo. |
| 8 | 3MF variant round-trip | Upstream 2.5 contains variant fixes; Android serializer requires field-by-field verification. | Fixtures preserve object/volume/material/config/transform metadata across load-save-load and slice. |
| 9 | Qidi profile parity | Initial-tool filename fix is ported; broader Q2/Q2C profile changes need a field-by-field diff. | Reviewed upstream profile diff and representative G-code/package acceptance for Q2/Q2C. |
| 10 | Orca profile-option gap refresh | Existing audited catalog covers 490 rows; avoid exposing internal, experimental, or unsafe options. | Generate a current missing-key list and port only options with correct model, persistence, and visible controls. |

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
