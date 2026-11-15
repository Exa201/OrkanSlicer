# UI 2.1 ColorFlow: Alt → Neu

This branch applies the attached design package as a native Compose visual layer. Existing navigation, profile controllers, project state, GL renderer, and action handlers remain the source of truth.

| Bereich | Vorhandener Einstieg | UI 2.1-Ziel | Umsetzung in diesem Stand |
|---|---|---|---|
| 3D-Arbeitsbereich | Compose `WorkspaceShell` around AndroidView/GLView | Cyan workspace chrome, compact profile/plate toolbar, clear workspace action groups | Shared dark palette, cyan selection and per-destination colored navigation are wired without replacing the renderer |
| Druck | Profile editor with existing print setting categories and save/reset | Orange accent, grouped categories, visible profile and high-value quick actions | Orange route accent, explicit high-contrast profile search control; existing categories and commit/reset flow are retained |
| Filament | Slot palette and filament profile editor | Violet slot identity, material status, readable profile settings | Violet route accent plus a live profile summary sourced from the active filament, selected slot color and pending-or-saved nozzle/bed temperatures. The summary no longer repeats the profile title already shown in the picker; the full name remains in its TalkBack description. Slot/controller behavior remains unchanged |
| Drucker | Printer profile editor and connection/profile actions | Green identity and readable technical sections | Printer route now receives the green Material accent |
| App | Compose settings sections and system/profile actions | Blue-violet settings identity, consistent cards, navigation and accessibility | Blue-violet route accent, ColorFlow cards and multi-tone local vector icon treatment |
| Objektliste | AlertDialog with object groups, assignment and part actions | Adaptive wide/full-height sheet, searchable virtual list, complete long names, preserved group actions | Replaced active dialog entry point with bounded responsive Compose Dialog; filter returns original model indices |
| Designsystem | ThemeRepo palette plus Compose theme | Consistent tokens, semantic surfaces, local offline icons, dark and light modes | Added shared ColorFlow colors, spacing/shape/touch tokens, action/card/row primitives and multi-tone local icon wrapper. Dark renderer palette now matches ColorFlow; light palette remains intact |

## Verification

- `:app:testDebugUnitTest`: 705/705 passed, no skips/failures.
- `PrintConfigToolsDialogsDeviceTest`: 4/4 passed on Android 16 emulator; `SettingsAccessibilityDeviceTest`: 1/1 passed on emulator.
- `:app:assembleDebug`: successful for 0.8.422 / code 457. APK installed/launched on Android 16 emulator; inspected logcat window had no fatal error. v1/v2 signature verification passed; SHA-256 `D65316A5F76D4B3D7E35527010565D81D9B305B3726E8149E035FE24A82795C0`.
- Screenshots of all five main routes, the flush-volume dialog, startup changelog, active filament summary and active printer summary are in `docs/screenshots/ui21/`.
- `FilamentProfileSummaryDeviceTest`: 1/1 passed on Android 16 emulator; verifies actual profile temperature/material text and immediate refresh after staging a new temperature.
- `PrinterProfileSummaryDeviceTest`: 1/1 passed on Android 16 emulator; verifies the printer profile's model, nozzle diameter, build height and host.
- `ModelObjectsDialogLayoutDeviceTest`: 1/1 passed on Android 16 emulator at 320 dp / 150% text; grouped object and part actions remain reachable with at least 48 dp touch targets.
- `ProfileFilterAccessibilityDeviceTest`: 2/2 passed on Android 16 emulator at 150% text, including the 48 dp profile-search action.
- `InitialProjectMenuDeviceTest#objectListPlacesGroupWithoutJoiningUntilExplicitlyCombined`: 1/1 passed on Android 16 emulator. Three loaded objects remain independent through placement, become a group only after explicit combine, and appear as three filament parts in the captured object-list screenshot `12-object-list-multipart-0419.png`.
- `InitialProjectMenuDeviceTest#importedMulticolorProjectShowsLivePaintBeforeConfirmWithoutDuplicatePaletteOrPlateLoss`: 1/1 passed on Android 16 emulator. After loading the bundled two-color 3MF, both model rows and their red/blue filament assignments appear in screenshot `13-object-list-imported-3mf-0419.png`; the test continues through live paint, cancel, and build-plate integrity checks.
- `SettingsAccessibilityDeviceTest`: 1/1 passed across 12 responsive configurations (320/360/400 dp portrait and 640×320 dp landscape, at 100%, 130%, and 150% font scale).
- `FilamentProfileSummaryDeviceTest`: 1/1 passed after removing the duplicate visible profile title; summary accessibility description still contains the full profile name and updates pending temperatures.
- `FilamentPaletteComposeDeviceTest#paletteRowsFromOneThroughTwentyFourEntriesScrollAndSelectTheLastItem`: 1/1 passed for 1/4/8/16/24 slots across 320/360/400 dp and 100/130/150% text, including selecting the last slot and 48 dp minimum targets.
- `SlicePrintPreparationComposeDeviceTest`: 9/9 passed on Android 16 emulator, covering print preparation warnings, shared ACE gates, two-color/same-slot assignment, 3MF remapping, cancellation/back, and menu actions. The shared-gate test now invokes the workspace host dismissal callback on confirmation (matching the production warning state dismissal).
- Plate counter regression cases: 3/3 passed on Android 16 emulator after updating assertions to use the current “plate X of N” accessibility label. The counter now has a 56 dp minimum height so its stacked label/count can breathe at 150% text scale.
- `SettingsChoiceDialogDeviceTest`: 6/6 passed on Android 16 emulator after adding explicit Compose BackHandler to the shared workspace dialog. Reset confirmation remains non-destructive when system Back dismisses it.
- `GeneratedOutputDialogDeviceTest#generatedModelUseCallsOnlyUseAction` and `#rasterPreviewSaveDismissesAndReleasesBitmap`: each passed 1/1 on the emulator when run separately.
- An unrestricted `connectedSliceTestAndroidTest` invocation attempted all 484 tests on the AVD and connected Xiaomi. It stopped after 24 AVD tests when Xiaomi installation was denied (`INSTALL_FAILED_USER_RESTRICTED`); three stale plate-label assertions and one ActivityScenario teardown timeout appeared in that partial run. The plate assertions were corrected and the three focused tests passed on a direct ADB run. This is not a full-suite pass.
- `PrintProfileDropdownIntegrationDeviceTest`: 17/18 successful in a full class run; the remaining failure occurred in `ActivityScenario` teardown (`PAUSED` activity), while the same process-profile clone/delete test passed 1/1 in isolation. The two filament tests previously blocked by duplicate visible title matches both pass individually (1/1 each).
- Device layout check found flush-matrix values rendered black on dark cells; text now explicitly uses `onSurface`. Search icon tint is also explicit. The recaptured dialog confirms the correction.
- Automated connected-test task also attempted to install the instrumentation APK on the attached Xiaomi; the install was cancelled by Android (`INSTALL_FAILED_USER_RESTRICTED`). Emulator test results are successful; Xiaomi hardware is not counted as tested.
- Remaining acceptance: full print/slice workflow, Honor PTP-N49 at 150% font scale, TalkBack hardware/manual interaction and a clean complete Slice test suite on a single device. The imported two-color 3MF list and the separate grouped-part display now have emulator screenshot evidence.
