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
| 1 | Low-GPU preview quality control | Implemented: automatic/50/65/80/100% viewport choices; package identity and Nightly updater are separated. Policy/updater tests pass 10/10 and instrumentation source compiles; live-device check pending. | Setting persists, changes live GL surface, touch picking remains aligned, no effect on sliced geometry. |
| 2 | Compound 3MF paint ownership | Implemented: each model-part volume owns a stable heap-allocated mesh, selector and AABB raycaster; world-space hit testing returns the source volume. Paint, undo/redo, clear and commit address the selected part without overwriting other parts. | Emulator ARM64 `TwoColorProjectDeviceTest` 5/5 passed, including independent painting of two combined 3MF parts, live overlays, undo/redo, 3MF export/reload and generated G-code with the expected T1 tool change. Physical-device validation remains outstanding. |
| 3 | Qidi Box slot mapping | Implemented: active G-code tools can map to Qidi Box slots 1–16; repeated slots are allowed, unmapped tools retain the printer setting, and confirmed choices persist per printer/host in Nightly-private preferences. Orca 2.5 Qidi agent semantics use `SAVE_VARIABLE value_tN` before `/printer/print/start`; the guarded Moonraker sequence is implemented. | Policy tests 5/5, dialog selection/persistence/cancel emulator tests 2/2, source compilation. Actual Qidi firmware/network behavior remains unverified without a Qidi device. |
| 4 | AMS/ACE physical extruder mapping | Sync UI exists; Orca multi-extruder/physical mapping has no complete profile edit + validation path. | Add/edit/remove mapping, prevent duplicates, sync safely, and preserve per-object material choice through slice. |
| 5 | Per-object and per-part overrides | Native object/volume config exists, but no complete mobile edit/apply workflow is established by the audit. | Edit one selected object/part, persist 3MF, slice and compare only the selected object's effective settings. |
| 6 | General modifier volumes | Cut connectors/negative text volumes exist; no general add/place/edit UI. | Add modifier/blocker/negative volume, transform independently, undo/redo and 3MF round-trip. |
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

## Delivery cadence and gates

1. Keep a numbered implementation log for the ten backlog items above.
2. For each item, inspect pinned upstream source and Android dependencies, implement only a safe
   adaptation, and add focused regression tests.
3. After each group of ten completed feature items, run grouped JVM/native/device verification,
   create a Nightly APK, inspect package identity and APK metadata/hash, then publish a Nightly
   GitHub release and update the changelog. Do not claim unsupported device tests passed.
4. Task #1 has code, policy and settings-flow tests; measured live surface-size change and model
   picking at reduced resolution still need a device interaction test. Task #2 passes
   `TwoColorProjectDeviceTest` 5/5 on the Android 16 ARM64 emulator, including distinct-volume
   painting, undo/redo, 3MF reload and generated G-code with the expected T1 tool change. Task #3
   passes mapping policy tests 5/5 and Qidi dialog emulator tests 2/2 (selection, persistence, cancel);
   the upstream `SAVE_VARIABLE` command and fail-closed-before-print path compile, but a live Qidi
   firmware/network exchange is not verified. Two items (#2 and #3) have implementation and focused
   emulator coverage (2/10); task #1 interaction and physical-printer validation remain outstanding.
   Items #4–#10 are open. No APK or release is created from this partial batch.

## Source audit documents

- `docs/ORCA_FEATURE_AUDIT.md` — profile and model-tool inventory; update stale rows when the
  current source proves the implementation exists.
- `docs/ORCASLICER_2_5_DELTA_AUDIT.md` — pinned upstream fixes and Android-port constraints.
- `ORCA_NIGHTLY_LOCK.md` — do not advance until engine identity and native test evidence justify it.
