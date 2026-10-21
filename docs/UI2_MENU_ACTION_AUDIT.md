# UI 2.0 — source-to-action audit

## 2026-10-21 continuation

- The orphan `components/bed_menu/ConverterMenu.kt` was removed after confirming there are no production references. The supported KromaCut/Maker route is untouched.
- `scripts/check_kotlin_ui_migration.py` now passes against the live source architecture. Its prior failures were stale static contracts (removed `UnfoldMenu` wrappers, consolidated Compose dialogs, renamed KromaCut asset entry, and required GL/WebView bridges), not reproduced app defects.
- Fresh local JVM run after removal: Debug 588/588 and Slice 588/588, no failures/errors/skips. Focused Honor Maker generator run after removal: 11 tests finished, none skipped or failed. No new full-suite or APK build was performed.
- Honor `WorkspaceNavigationDeviceTest` passed 1/1, including route lifecycle checks for KromaCut WebGL and GL renderer restoration. The test suite ran on PTP-N49 / Android 16; emulator unavailable.
- Added and verified the real global navigation path into KromaCut (Settings → Maker → KromaCut): `SettingsMenuActionDeviceTest` 8/8, with a final focused re-run 1/1. The test checks the WebView attaches and Back restores the same renderer/model instance. This closes that specific Maker navigation gap; generator success/save coverage and full six-menu reconciliation remain open.
- The six-menu map below remains a source-to-test audit with uncovered action-level gaps; passing the migration checker does not close Phase 5.

Audit basis: current branch HEAD `40f9eac6` (application runtime unchanged since `adf62115`, 0.8.337 / versionCode 372). This matrix maps the active route definitions to the strongest existing action-level test evidence; a test of a dialog or callback is not represented as a full real-printer or release workflow.

## Six workspace destinations

| Destination | Active source and actions | Action-level evidence | Status / boundary |
|---|---|---|---|
| Project / files | `FileMenu.onCreateItems`: add/import models and export the active plate as 3MF; `createOptionsItems`: calibration, profile import/export, conditional cloud generator. | `InitialProjectMenuDeviceTest`: first-open route, single/multiple STL import, single-plate export/reload, multi-plate export warning/cancel, profile import/export. `CalibrationWorkspaceDeviceTest`; `MakerGeneratorWorkspaceDeviceTest`. | Core file/profile paths have interaction tests. Android document picker behavior is covered with test result handlers; cloud generation requires account/feature fixtures and is conditional. Multi-plate export intentionally warns and exports the active plate; full project persistence is separate. |
| Model | `ModelMenuItems.createItems`: object list, duplicate, move plate, center, fill, arrange, orient all/selected, flatten, delete selected/all, select all. | `InitialProjectMenuDeviceTest`: functional tests for duplicate, center, flatten, auto-orient, arrange, fill, move plate, select/delete and cancellation; `FilamentPaletteComposeDeviceTest` checks model/filament assignment UI. | The listed safe model operations have action tests with native model state assertions. Destructive tests use fixtures and exercise cancel/back. |
| View / camera | `CameraMenu`: orthographic projection toggle and camera-control-mode choice; view presets/cube are part of the workspace camera controls. | `InitialProjectMenuDeviceTest`: projection/control mode, all camera presets, rapid changes, view-cube face tap and drag. | Renderer pose/projection is asserted. Physical TalkBack traversal is still unavailable. |
| Edit | `TransformMenuItems`: reset rotation/scale, undo/redo, move/rotate/scale, simplify, cut, split, variable layer height, support/seam/fuzzy/color paint, emboss, measure. | `InitialProjectMenuDeviceTest`: confirm/cancel transforms, undo/redo, simplify confirm/undo, cut preview/apply on GL thread, split, variable-layer cancel, paint modes/back, canvas paint/undo, emboss, measure. `GlModePanelsComposeDeviceTest` supplements panel checks. | High-risk GL actions use test meshes; preview cancellation and model-history behavior are asserted. No physical print is performed. |
| Tools | `OptionsMenu` delegates to `FileMenu.createOptionsItems`: optional AI generator, calibration, profile import/export. Maker is a separate reachable `MakerFragment` route from App settings. | `InitialProjectMenuDeviceTest` profile import/export; calibration and maker/generator workspace tests. Current Maker actions are defined by `MakerFragment.buildSections`; generator-form tests cover open/cancel/back and selected validation/output flows. | The old `ConverterMenu` class had no production instantiation and was removed in this pass. The actual Maker route remains. Individual generation of every maker output is not covered by the form cancel/back sweep. |
| Slicing | `SliceMenuItems`: quick/smart/current-settings slice; after slicing, info, layers, G-code export/share, performance toggle, conditional upload and upload-and-print. `SliceMenu` routes upload through preparation and `SliceUploadController`. | `InitialProjectMenuDeviceTest`: slice result, info/layers/share, performance toggle and G-code document export; `SliceResultWorkspaceDeviceTest`; `SlicePrintPreparationComposeDeviceTest`; `SliceUploadOnlyLoopbackDeviceTest` (local fake server). | Slice/result/export UI is tested. Upload-only was exercised against localhost fake HTTP; no physical printer was contacted. Upload-and-print is explicitly excluded from physical execution. |

## Global destinations and current focused evidence

| Destination / group | Active routes | Focused action tests | Status / boundary |
|---|---|---|---|
| Print profiles | `PrintConfigFragment` and profile editors | `PrintProfileDropdownIntegrationDeviceTest` 18; `ProfileNumericOptionDeviceTest` 3; `ProfileSettingsImeDeviceTest` 12; profile choice/help tests. | Selection, edits, keyboard and dialogs were exercised on Honor with test configuration. |
| Filament | `FilamentConfigFragment`, palette and MMU assignment/sync | `FilamentPaletteComposeDeviceTest` 20; `MmuFilamentSyncDeviceTest` 15; `SlicePrintPreparationComposeDeviceTest` includes repeated physical gate assignment. | MMU sync uses test doubles, not live ACE/printer state. |
| Printer | `PrinterConfigFragment`, printer web panel | `PrinterWebWorkspacePanelDeviceTest` 3 and `PrinterWebWorkspaceRouteDeviceTest`. | Test routes/fakes only; no printer mutation or firmware update. |
| App / settings / Maker | `SettingsFragment`; Maker is opened from its Maker setting action. | `SettingsMenuActionDeviceTest` 7; accessibility settings tests; `MobileNavigationDeviceTest`; maker/generator/calibration tests. | App actions and navigation tested. Manual device TalkBack remains unverified because no accessibility service was enabled. |

## Current-pass verification and remaining work

- Fresh Honor `MakerGeneratorWorkspaceDeviceTest`: Gradle reported 11 tests finished, 0 skipped, 0 failed; app code was rebuilt after removing the unused host. No real printer was contacted.
- The latest previously recorded full Honor sliceTest suite was 234 passed, 3 skipped for unavailable private multicolor KromaCut fixtures, 0 failures/errors; these were against unchanged runtime code. JVM Debug and Slice suites were each 588/588 at that same runtime revision.
- Current-pass `SliceUploadOnlyLoopbackDeviceTest` was previously run against that same unchanged runtime; it uses a local fake server. Its “print” request is a mock endpoint test, not a real print.
- This is a source-to-test map, not a claim that every conditional Maker generator has been run end-to-end. Remaining action gaps: exercise the active Maker screen's KromaCut entry end-to-end through its actual navigation path; cover each Maker generator's generate/save success path where safe fixtures exist; verify printer upload on a controlled printer only if such an environment is intentionally provisioned; execute physical TalkBack traversal when available.
- No actual print start, live printer sync, firmware change, or user-profile overwrite was performed.

## Maker generator success-path follow-up — 2026-10-21

- Added a real Text Tag Maker submit test to `MakerGeneratorWorkspaceDeviceTest`. On Honor PTP-N49 / Android 16 it generated a binary STL in the app cache, passed the production STL validator (facet count, exact binary length, finite coordinates, and non-degenerate bounds), displayed the generated-output choices, and removed the artifact after the user-facing discard action.
- Focused test: 1/1 passed. Full `MakerGeneratorWorkspaceDeviceTest`: 12/12 passed, no skips or failures. The test does not select save, open Android's document picker, or write outside app cache.
- This closes one representative generator success/discard path; it does not claim success-path coverage for all Maker generators or external-save workflow. Those remain explicit gaps in Phase 5.
- A second Honor success/discard path now covers Cable Grommet defaults as well. The full `MakerGeneratorWorkspaceDeviceTest` reports 13/13; its output is validated and discarded from app cache. No external save behavior was exercised.
- A third success/discard path covers Phone Stand defaults, including the larger generated mesh. The Maker device class reports 14/14 on Honor; the generated file passed production binary STL validation and was discarded from cache. The external document picker remains unverified.
