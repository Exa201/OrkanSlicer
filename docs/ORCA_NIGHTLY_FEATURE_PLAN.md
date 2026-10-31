# OrkanSlicer Nightly 2.5 — Portierungsplan

Stand: 2026-10-31. Upstream-Vergleich: OrcaSlicer `main` at
`5cc5fb7f9c40c42ca47fc13969756c4e53119046` (latest fetched 2026-10-31). This is a selective Android
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
| 7 | SVG surface emboss/engrave | Implemented: SAF selection and bounded/secure validation, width/depth controls, GL surface-tap placement, raised printable part or native negative volume; SVG curve tessellation uses a 0.05 mm target error. | `suppliedSvgCanBeRaisedOrEngravedOnASurfaceAndStillSlicesAfterThreeMfReload` passed 1/1 on API-35 x86_64 emulator with Android's ARM64 native bridge. It checks both modes, 3MF reload and changed extrusion paths. Manual live placement preview/cancel-before-commit remains open; commit-on-tap is undoable. |
| 8 | 3MF transform and parser-size parity | Ported Orca fixes `67a976e0` (preserve valid mirrored matrices in Prusa/BBS importers) and `d35ea27e` (reject oversized XML ZIP entries before Expat allocation). | Mirrored Prusa/BBS round-trip passed 1/1 and oversized ZIP64 XML rejection passed 1/1 on API-35 x86_64 emulator with ARM64 native bridge. |
| 9 | Qidi profile parity | Q2/Q2C profiles use selected filament bed temperature for `PRINT_START`/`M140`, initialize Qidi Box mapping, select the initial logical tool and use material-aware filenames. | Qidi Q2 native slice/G-code test passed 1/1 on API-35 x86_64 emulator with ARM64 native bridge. It verified 67 °C in `PRINT_START`/`M140` and resolved the executable initial-tool command. Profile transmission to a physical printer remains unverified. |
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
| 11 | `7ac46c9a6` Lightning overhang samples outside the infill bounds can make tree generation loop indefinitely. | Ported in `Fill/Lightning/DistanceField`: expand the searchable box to all unsupported sample points. API-35 x86_64 with ARM64 bridge passed a Lightning slice on an overhang-rich Bunny fixture, with sparse-infill extrusion in G-code (1/1). The exact upstream unsupported-cell-outside-bounds construction is still not reproduced byte-for-byte. | Ported; representative overhang runtime regression passed; exact synthetic edge fixture remains open |
| 12 | `9b4f84a92` Preserve every extruder's values when a custom printer has more nozzles than its parent. | Native preset code extends inherited nozzle vectors in local/system, project, and cloud paths. Android currently loads flattened profile `.ini` data and has no active `PresetBundle` call site; the source port compiles, but no live Nightly app route exercises this behavior. | Core port present; not counted as Android feature until an app call path exists |
| 13 | `fcd96ad91` Apply each object's own extruder layer-height limits and compare actual layer heights for prime-tower compatibility. | Corrected zero-based object extruder to one-based nozzle lookup. API-35 ARM64-bridge test slices two objects on nozzles with different min/max limits and the same actual layer sequence through one prime tower; deliberately unequal adaptive profiles remain correctly incompatible. | Implemented and runtime-verified (1/1) |
| 14 | `a4a510087` Keep build-plate-only tree supports inside XY clearance. | Equivalent collision geometry is already used by Android tree-support generation. API-35 x86_64 with ARM64 bridge sliced the overhang-rich Bunny at 0.2 and 0.8 mm XY clearance. G-code analysis samples every 16th support extrusion segment and measures its nearest outer-wall segment within 0.5 mm Z: median XY gaps were 3.4165 mm (393 samples) and 3.4992 mm (1,646 samples), respectively. This verifies a measurable monotonic response, but it is not the exact minimum gap to the STL surface. | Existing port; runtime clearance sensitivity and sampled path-gap regression passed (1/1); exact mesh-distance minimum remains open |
| 15 | `98b539c3e` Use the printing filament's retraction-lift settings. | Android reads thresholds/Z-hop and enforcement from the active logical filament ID. Two-filament temporary-project G-code regression passed 1/1 on API-35 with ARM64 bridge; its parser excludes the normal end-of-print head-raise command. | Equivalent path implemented and runtime-verified (1/1) |
| 16 | `b7ecf1b29` Avoid false out-of-printable-range warnings for an extruder area inset on multiple sides. | Added a JNI diagnostic getter for the native G-code validation error bitmask and an API-35 ARM64-bridge slice regression using a real tool-changer profile. The test slices the model inside a small inset toolhead polygon without the toolhead-area warning, then moves that polygon away and confirms the same mapped toolhead is rejected. | Existing port; asymmetric area false-positive and positive-control runtime test passed (1/1) |
| 17 | `29e189957` Ensure sparse layers exist during a tool change. | Both Android wipe-tower generators restore model Z before the travel-capable macro. The compacted-tower multicolor G-code regression passed 1/1 on API-35 with ARM64 bridge, checking toolchange Z against the active model layer. | Implemented and runtime-verified (1/1) |
| 18 | `4c7928d8a` Decide prime-tower need from the current model's filament set rather than the preceding model. | `Print::apply` now retains pre-normalized settings and re-evaluates after current model regions/filaments are rebuilt, so normalization can re-enable the tower. ARM64 native build and instrumented Kotlin compilation pass; a reused-Print runtime regression is still needed. | Ported; runtime test open |
| 19 | `59f0cc3806ab6424d081978fde331088c55d9f0f` Avoid indexing empty layer lists for objects too thin to create any layer when a raft is enabled. | Both guards are present in brim grouping and precise-Z layer alignment. The raft 0/2 × precise-Z off/on regression returned the expected normal “No layers were detected” error instead of crashing; passed 1/1 on API-35 with ARM64 bridge. | Equivalent guard runtime-verified (1/1) |
| 20 | `d28c71c48` Preserve enum-list config keys in default maps. | JNI serializes normalized `coEnums` defaults through `ConfigOptionDef::create_default_option`; enum values and nullable `nozzle_type=undefine` survive the native catalog, and a basic native slice still emits extrusion. Passed 1/1 on API-35 with ARM64 bridge. | Ported and runtime-verified (1/1) |
| 21 | `27cafaa06` Read nozzle diameters correctly with a decimal-comma UI locale. | Desktop-only nozzle/device UI routes are absent from Android. In the shared Bambu 3MF parser, replace locale-sensitive `atof` with `string_to_double_decimal_point`. The Android importer already enters a C numeric-locale guard, so this is explicit format-parsing parity and defense in depth, not a demonstrated Android locale bug. ARM64 native build passes; no dedicated import fixture assertion yet. | Defensive parity port; not counted as a completed feature group |
| 22 | `7b6c12cf7` Match grouped filaments to those loaded in dual-nozzle printers. | Relevant only after loaded physical-tray identity reaches the Android `Print` object. Current `MachineFilamentInfo` has no filament ID; `build_full_machine_filaments` reads color/type/tray name only; `Print::set_extruder_filament_info` has no Android call site. The model-side `filament_ids` already exist, but that alone cannot match to loaded trays. Defer algorithm changes until this data path and a generated-tool regression are implemented. | Audited; plumbing prerequisite missing, no partial port |
| 23 | `8e37d7d4b` Match closer colours and the same filament subtype in Convenience mode. | Latest upstream delta since `7ac46c9a6`. This modifies the color-distance assignment solver to prefer matching `filament_id` subtypes only after color distance, without letting subtype outweigh a color-distance step. Android's solver has neither machine-slot IDs nor a populated printer-filament list in `Print`; make the data path and a deterministic multi-tray G-code regression prerequisite. | Audited; blocked on loaded-tray data plumbing |
| 24 | `aa66e41e6` Group a filament onto the nozzle that has it loaded in Filament-Saving mode. | Latest upstream delta since `7ac46c9a6`. Candidate generation uses loaded per-nozzle trays and flush-cost budget to prefer fewer nozzle changes. Android exposes a model-side logical-to-physical map, but that is not loaded-tray identity. Requires real per-nozzle filament configs, test fixtures, and generated tool-sequence assertions before porting. | Audited; blocked on loaded-tray data plumbing |

### Current continuation — 2026-10-31

- The low-GPU viewport option is already implemented: automatic device policy uses low-RAM and known renderer hints; users can override it in App settings. It changes only GL surface resolution and preview mesh budgets, not slice geometry. Policy JVM tests and live emulator touch-coordinate checks are recorded in `UI2_EXECUTION_STATE.md`; physical GPU benchmarking remains open.
- Orca 2.5 candidates 11, 13, 14, 15, 17, 19, and 20 now have focused API-35 runtime evidence using the emulator's ARM64 native bridge. Candidate 11 passed Lightning slicing on an overhang-rich Bunny fixture. Candidate 14 passed actual tree-auto, build-plate-only support generation at two XY clearances, and the support extrusion paths changed between 0.2 and 0.8 mm. Exact synthetic sample/path-distance assertions remain open for 11 and 14. Candidate 15's initial failure was a test false positive: the end-of-print head raise was being counted as an extrusion Z-hop. Candidate 13's deliberately different adaptive profiles were correctly rejected; its final regression checks unequal nozzle limits with the same actual layer sequence.
- Candidate 16's asymmetric inset-area false-positive and excluded-area positive control now pass through the real Android native slice path (1/1). The test-only evidence uses a native validation error bitmask getter exposed to Kotlin as a general slice diagnostic. Candidate 18's `Print::apply` re-normalization port compiles, but Android's JNI creates a new `Print` for each `model_slice` call; a reused-Print runtime scenario does not currently occur in the app. Candidate 12's preset inheritance similarly has no active Android `PresetBundle` call path.
- Candidate 19 is the thin-object/raft guard, not mixed-filament validation. Orca's separate mixed-filament component-expansion fix is outside current scope because this Android fork has no mixed-filament composition model or `FilamentMixer`; it must not be partially ported as a bed-check-only change.
- ARM64 native builds, focused instrumentation, and the complete Debug/Slice JVM suites pass. On 2026-10-31, both JVM suites passed 704/704 with 0 failures, 0 errors, and 0 skips; the focused #16 device regression passed 1/1 on API 35 x86_64 with ARM64 translation. Physical GPU benchmarking and direct Qidi firmware/Moonraker communication remain hardware-only follow-ups.
- Orca upstream advanced to `27cafaa06` (2026-10-31). Candidate #21's locale-safe nozzle parser is applied to the shared Bambu 3MF reader; its existing outer `CNumericLocalesSetter` already protects Android import, so the change is defensive parity and does not count as a newly verified feature. ARM64 native build passed; direct metadata round-trip coverage remains open.
- Next: refine #14 from a sampled G-code wall-path median to the exact minimum clearance against the adjacent model cross-section. #11's exact unsupported-sample-outside-bounds stress construction also remains more specific than its passing overhang-rich smoke test. Do not count #12 or #18 as app-visible feature completions without an Android call path; replace unsuitable candidates from the pinned upstream backlog before the ten-item APK/release gate.
- Additional focused API-35 ARM64-bridge runtime evidence: `NativeSliceDeviceTest#lightningInfillCompletesOnOverhangRichBunnyFixture` passed 1/1 after explicitly placing the imported Bunny on the bed; generated G-code contains sparse-infill extrusion. `#buildPlateOnlyTreeSupportsSliceWithConfiguredObjectClearance` passed 1/1 and verified that changing build-plate-only tree-support clearance from 0.2 to 0.8 mm changes generated support extrusion paths. A first AVD attempt ended when the emulator went offline; the repeat passed after restarting the Android-15 ARM64-translation AVD. One initial Lightning attempt exposed an improperly placed test fixture and was corrected with `ensureOnBed(0)`. These failed attempts are not counted as passes.
- These tests exercise real native slicing but do not assert the precise out-of-bounds Lightning sample or measure minimum support-to-model path distance. Keep those narrower geometry checks open; do not claim the smoke tests prove them.

After each ten-item group reaches its explicit code and runtime gates, build and inspect the isolated
Nightly APK and publish the corresponding Nightly GitHub prerelease. Items 1–10 now meet their
defined test gates; hardware follow-ups are not represented as tested.

## Latest upstream delta reviewed — 2026-10-31

`orcaslicer-upstream/main` now points to `5cc5fb7f9c40c42ca47fc13969756c4e53119046`. Its three
new commits were checked individually:

- `5cc5fb7f9` stops Bambu LAN SSDP discovery before destroying the desktop network agent. Nightly has
  no Bambu desktop plugin or equivalent agent lifecycle; its Moonraker path has no matching API.
- `913c62257` remembers printers per desktop printer-agent and keeps AMS system filaments visible.
  The desktop `DeviceManager`/`PresetBundle` path is absent on Android, so no direct port is warranted.
  The related Android MMU state-isolation gap is handled by the adaptation below.
- `7af399554` links libjpeg for the Linux Arch build and does not apply to Android.

The upstream reference was refreshed; the native engine lock was not advanced. Each Android port
still needs an app route and its own runtime evidence.

## Android adaptation — per-printer MMU synchronization state (2026-10-31)

| # | Upstream basis | Android change and evidence | State |
|---|---|---|---|
| 25 | `913c62257` printer-specific persistence concept | MMU synced markers, selected tool-to-gate choices, palette-to-gate order, and Moonraker snapshots are stored under SHA-256 preference keys derived from the printer profile title. Existing single-printer keys remain as migration fallbacks and are copied to scoped keys on read. The global filament palette remains the user's active palette; only printer-specific MMU metadata is scoped. `MmuFilamentSyncDeviceTest` passed 16/16 on each of two API-35 emulators, including A→B→A switching with distinct gate choices and snapshots. `FilamentPaletteSyncPolicyTest` passed 10/10; Android test Kotlin compilation and `git diff --check` passed. | Implemented and emulator-verified; physical-printer validation remains open |
