# UI 2.1 ColorFlow: Alt → Neu

This branch applies the attached design package as a native Compose visual layer. Existing navigation, profile controllers, project state, GL renderer, and action handlers remain the source of truth.

| Bereich | Vorhandener Einstieg | UI 2.1-Ziel | Umsetzung in diesem Stand |
|---|---|---|---|
| 3D-Arbeitsbereich | Compose `WorkspaceShell` around AndroidView/GLView | Cyan workspace chrome, compact profile/plate toolbar, clear workspace action groups | Shared dark palette, cyan selection and per-destination colored navigation are wired without replacing the renderer |
| Druck | Profile editor with existing print setting categories and save/reset | Orange accent, grouped categories, visible profile and high-value quick actions | Print route now receives the ColorFlow orange Material accent; existing categories and commit/reset flow are retained |
| Filament | Slot palette and filament profile editor | Violet slot identity, material status, readable profile settings | Filament route now receives the violet Material accent; slot/controller behavior remains unchanged |
| Drucker | Printer profile editor and connection/profile actions | Green identity and readable technical sections | Printer route now receives the green Material accent |
| App | Compose settings sections and system/profile actions | Blue-violet settings identity, consistent cards, navigation and accessibility | Blue-violet route accent, ColorFlow cards and multi-tone local vector icon treatment |
| Objektliste | AlertDialog with object groups, assignment and part actions | Adaptive wide/full-height sheet, searchable virtual list, complete long names, preserved group actions | Replaced active dialog entry point with bounded responsive Compose Dialog; filter returns original model indices |
| Designsystem | ThemeRepo palette plus Compose theme | Consistent tokens, semantic surfaces, local offline icons, dark and light modes | Added shared ColorFlow colors, spacing/shape/touch tokens, action/card/row primitives and multi-tone local icon wrapper. Dark renderer palette now matches ColorFlow; light palette remains intact |

## Verification

- `:app:testDebugUnitTest`: 705/705 passed, no skips/failures.
- `PrintConfigToolsDialogsDeviceTest`: 4/4 passed on Android 16 emulator; `SettingsAccessibilityDeviceTest`: 1/1 passed on emulator.
- `:app:assembleDebug`: successful. The APK was installed and launched on the emulator; logcat showed no app crash in the inspected window. APK signing verification passed.
- Screenshots of all five main routes and the flush-volume dialog are in `docs/screenshots/ui21/`.
- Device layout check found flush-matrix values rendered black on dark cells; text now explicitly uses `onSurface`. Search icon tint is also explicit. The recaptured dialog confirms the correction.
- Automated connected-test task also attempted to install the instrumentation APK on the attached Xiaomi; the install was cancelled by Android (`INSTALL_FAILED_USER_RESTRICTED`). Emulator test results are successful; Xiaomi hardware is not counted as tested.
- Remaining acceptance: Honor PTP-N49 at 150% font scale, TalkBack hardware/manual interaction, and a visual screenshot of the object list with a multi-part 3MF fixture. Full print/slice workflow remains a separate functional test.
