# OrkanSlicer UI 2.0 execution state

## Latest continuation — 0.8.253 (2026-10-17)

- Code commit: `cecb2461283a07519cb56befdb282974d46cbd20` (`Improve Maker accessibility and touch targets`). Maker section headers now expose distinct localized TalkBack descriptions for expandable groups versus single-action launchers; action rows and headings have a 48 dp minimum target.
- Added `MakerComposeDeviceTest` exercising expand/collapse, actual action callbacks, semantics, and 48 dp bounds at 320 dp width and 150% Compose font scale. Targeted Honor run: 2/2 passed.
- Debug and Slice unit suites: 549/549 each, 0 failures/errors/skips. Full Honor `:app:connectedSliceTestAndroidTest`: 60 XML cases, 0 failures/errors, 3 private multi-color fixtures skipped (57 executed passed); emulator `emulator-5554` remains offline.
- `:app:assembleDebug` passed from code commit `cecb246`. APK `app/build/outputs/apk/debug/OrkanSlicer_cecb246128.apk`, 157,040,681 bytes; SHA-256 `28CB1051B6630442F41963A027D5AB2B1B0639DEB61CFD4E338E7CAB766EF8CE`.
- Installed and launched on Honor PTP-N49 / Android 16 using verified package `com.codemastercody3d.orcaslicermobile`; version `0.8.253` / code `288`; `ru.ytkab0bp.slicebeam.MainActivity` top-resumed. Fresh logcat sample had no crash/ANR/OOM/fatal-signal markers. SliceBeam was not used. No print job was started.
- UI 2.0 remains incomplete. Next: audit and test global settings/profile menu actions and remaining legacy entry points; extend responsive/accessibility coverage beyond the Maker test. Keep the BedFragment generic dialog host unchanged until IME-aware Back coverage exists. Full multi-plate persistence remains separate and incomplete; retain the active-plate export warning.
- Branch `codex/rinkhals-safe-start-audit`; PR #2 remains open and unmerged. Preserve `.codex-remote-attachments/`.

## Latest continuation — 0.8.252 (2026-10-17)

- Code commit: `319fdc041ca86a992cf9e6f529e5f8b4a611255d` (`Require Compose for all workspace menus`). The six `WorkspaceDestination` menu routes now render through a Compose-only `WorkspaceMenuHost`; its Android View fallback and BedFragment's menu-view factory were removed. Destination creation is exhaustive, and the old silent default-to-Project route was removed. The GL renderer adapter and tool-route compatibility boundary remain unchanged.
- Existing Honor `WorkspaceNavigationDeviceTest` passed on PTP-N49 / Android 16: six main destinations switch with the same GLView instance and direct Compose ownership; the test also exercises ten Compose tool routes and Android Back cleanup.
- Debug and Slice unit suites: 549/549 passed each (0 failures/errors/skips). Full Honor Slice instrumentation: 61 XML test cases, 0 failures/errors, 3 private multi-color fixture tests skipped (58 executed passed); emulator `emulator-5554` remains offline.
- `:app:assembleDebug` passed. APK: `app/build/outputs/apk/debug/OrkanSlicer_319fdc041c.apk`, 157,039,746 bytes; SHA-256 `249681A25965D46666E0A0BA3BE3559145D0C613A3F93C514E2097108CCE1E77`.
- Installed and launched on Honor PTP-N49 under the explicitly verified package `com.codemastercody3d.orcaslicermobile`, version `0.8.252` / code `287`; `MainActivity` top-resumed, with no crash/ANR/OOM matches in the fresh logcat sample. No print job was started.
- The primary workspace now has no Android-view menu fallback. Remaining UI 2.0 work includes action-by-action audit of Maker/global/profile settings and tool actions, BedFragment's generic dialog/overlay host (retain until Android Back/IME tests can protect its behavior), system-level 320/360/400 dp + font-scale + TalkBack matrix, and the known separate multi-plate project serializer. Preserve the active-plate export warning. Do not claim the migration complete.
- Next concrete task: continue auditing the actual Maker and global menu actions and strengthen the next independent accessibility/device interaction path without changing the dialog host before IME-aware Back coverage.

## Latest continuation — 0.8.251 (2026-10-17)

- Code commits: `bac880a905b7bd72dc9b6068af7fe4bf02827168` (`Improve build plate picker touch target`) and `dbafa3981cfc836b994312f26131572b29c7fa63` (`Add accessible plate selection stress coverage`).
- The build-plate picker Cancel action now has an explicit minimum 48 dp height; Material 3's default 40 dp height was measured in the device test and corrected. The Compose test opens the picker, checks width and height, dismisses it, and verifies one dismissal callback.
- The ACE row scroll affordance from `98fb4f9` remains included: accessible labeled 48 dp control scrolls to the final synthetic gate in a 24-slot row.
- Added distinct localized TalkBack descriptions for plate selection cards. Device stress test selects the last plate in synthetic projects of 1/3/6/10/20 plates; it scrolls the multi-plate grid, verifies selection, and verifies picker dismissal.
- Debug and Slice unit suites: 549/549 passed each (0 failures/errors/skips). Full Honor instrumentation: 58 XML cases, 0 failures/errors, 3 private multi-color fixtures skipped (55 executed passed). Targeted plate Cancel, plate stress selection, and ACE scroll tests each passed 1/1.
- Final `:app:assembleDebug` passed from code commit `dbafa39`. APK: `app/build/outputs/apk/debug/OrkanSlicer_dbafa3981c.apk`, 157,039,789 bytes; SHA-256 `2C7C923028A3837F5E8D1C048A0EC4C86681274C96450F98A6F1B02B99867E8E`.
- Installed on Honor PTP-N49 / Android 16. Confirmed package `com.codemastercody3d.orcaslicermobile`, version `0.8.251` / code `286`, resumed `MainActivity`, and 0 crash/ANR/OOM markers in a fresh 2,000-line logcat sample. Physical config is 1280×2800 px / 560 dpi, about 366×800 dp, font scale 1.0; the automated Compose tests set 1.5 font scale for picker bounds. Emulator remains offline.
- UI 2.0 remains incomplete. Open: global menu/action-by-action verification, remaining BedFragment overlay/dialog responsibility (prior shell-only dialog migration regressed Back/IME behavior), system-level small-width/font-scale/TalkBack coverage, and complete multi-plate project persistence. Preserve the active-plate export warning.
- Branch: `codex/rinkhals-safe-start-audit`; PR #2 is open and unmerged. Preserve `.codex-remote-attachments/`.

## Latest continuation — 0.8.250 (2026-10-17)

- Code commit: `98fb4f9d94f61230019ebd7ce12763a6b13de2ee` (`Add accessible ACE slot scroll control`).
- The ACE physical-slot row now keeps its horizontal scrolling and adds a localized, accessible 48 dp forward control when the row overflows. This makes the remaining hardware gates discoverable without changing slot order, hardware capacity, or T0/T1/T2/T3 mappings.
- Added Honor Compose UI coverage with a 24-entry synthetic ACE row at 320 dp / 130% font scale; the accessible control is shown, meets 48 dp bounds, and reaches the final synthetic gate. Targeted test passed 1/1.
- Unit suites: Debug 549/549 passed; Slice 549/549 passed. Full Honor `:app:connectedSliceTestAndroidTest`: 56 XML test cases, 0 failures, 0 errors, 3 private multi-color fixture tests skipped (53 executed passed). One earlier full run had an intermittent existing import-dialog worker timeout; its targeted rerun passed 1/1, and the subsequent full suite passed. Emulator `emulator-5554` remains offline.
- `:app:assembleDebug` passed from code commit `98fb4f9`. APK: `app/build/outputs/apk/debug/OrkanSlicer_98fb4f9d94.apk`, 157,039,120 bytes; SHA-256 `BACBFFFB5A5336BD726BE279030C059BFAC3E0C423FE43F90DCD26A39C542F1B`.
- Installed on Honor PTP-N49 / Android 16, explicitly verified package `com.codemastercody3d.orcaslicermobile`, version `0.8.250` / code `285`, and resumed `ru.ytkab0bp.slicebeam.MainActivity`. Fresh logcat sample after launch: 0 crash/ANR/OOM markers. Device configuration was 1280×2800 px / 560 dpi (about 366×800 dp), portrait, system font scale 1.0. The new test separately exercised a constrained 320 dp / 130% Compose density; this is not a complete system font-scale or screenshot matrix.
- Active migration remains incomplete. Next: continue migrating remaining BedFragment-owned dialog/overlay responsibilities only with IME/back regressions; audit global menu actions and remaining legacy hosts. Full TalkBack/system font-scale/accessibility coverage and multi-plate project persistence remain open; preserve the active-plate-only export warning.
- Branch: `codex/rinkhals-safe-start-audit`; PR #2 remains unmerged. Preserve unrelated untracked `.codex-remote-attachments/`.

## Latest continuation — 0.8.249 (2026-10-17)

- Code commit: `8f2315154399afdc18bc0775e1901a89503c50ef` (`Compose workspace controls directly in shell`).
- Follow-up test commit: `2992f42` (`Assert workspace controls use shell composition`).
- Workspace profile status, project settings bar, filament entry point, plate picker, and printer-web entry point now render as direct Compose content in `WorkspaceShell`; their former `ComposeView` wrappers and BedFragment references were removed. The GL renderer remains hosted through the stable AndroidView adapter.
- `WorkspaceNavigationDeviceTest` asserts the same GLView instance survives workspace route switches. Lazy plate-load failures and cancellations now complete `ModelLoadCallback`, and the integration test records a concrete load error instead of waiting only for a timeout.
- Version `0.8.249` alpha / versionCode `284`. Debug build passed. APK: `app/build/outputs/apk/debug/OrkanSlicer_8f23151543.apk`, 157,022,242 bytes; SHA-256 `262A9AB3C4C1417FEEED8033B4AFB8ED07ECAAD4B66B7207F45DEC3EE301382A`.
- Debug and Slice unit tasks passed (both up-to-date, 549 tests each from the current suite). Full Honor `:app:connectedSliceTestAndroidTest` passed: 55 tests reported, 0 failures/errors, 3 private multi-color fixture tests skipped (52 executed passed). The targeted `MultiPlateProjectDeviceTest` also passed 4/4. Emulator `emulator-5554` remains offline.
- Follow-up `WorkspaceNavigationDeviceTest` passed 1/1 on Honor and now asserts the shell contains only its root ComposeView rather than nested ComposeView wrappers for its direct controls. Manual visual check used the confirmed OrkanSlicer `MainActivity` at 1280×2800 px / 560 dpi (about 366×800 dp), portrait, system font scale 1.0: the plate remained central and large, and the Model menu actions appeared in a complete two-row grid without overlap. This is one device configuration, not the full responsive/accessibility matrix.
- Installed APK on Honor PTP-N49 / Android 16, package `com.codemastercody3d.orcaslicermobile`; verified version 0.8.249 / code 284 and resumed `MainActivity`. Fresh logcat sample had no crash, ANR, or OOM markers. No screenshots were captured.
- The new native export still handles the active plate/model only; preserve the multi-plate warning. Full project persistence remains separate and incomplete.
- Next implementation: continue the global action-by-action menu audit and inspect remaining BedFragment-owned overlay/dialog routes. Generic message/custom-dialog host requires an IME-aware regression before changing it. Full responsive/system accessibility matrix, all menu action verification, and complete multi-plate persistence remain open; do not call the UI migration complete.

Updated: 2026-10-16

## Current continuation (0.8.248)

- All six `WorkspaceDestination` menus (Project, Model, Camera, Edit, Tools, Slice) now render direct Compose content in `WorkspaceShell`. The active workspace no longer creates their `ThemeAwareComposeView` menu hosts. Compatibility `onCreateView` routes remain for non-workspace callers.
- `ListBedMenu` event-bus registration is idempotent and follows the selected Compose route's enter/exit lifecycle; existing menu action builders and renderer callbacks are retained.
- Honor PTP-N49 / Android 16: `WorkspaceNavigationDeviceTest` passed (1/1) with all six route transitions and checks that their legacy menu Views are absent. Full instrumentation reported 55 tests, 0 failures, 3 skipped private-fixture tests (52 ran and passed). Emulator remains offline.
- Debug and Slice unit suites each passed 549/549 (0 failures, errors, or skips). Source and instrumentation compilation passed.
- Version 0.8.248 alpha / versionCode 283. `:app:assembleDebug` passed at code commit `becee99` (`becee9948c`). APK `app/build/outputs/apk/debug/OrkanSlicer_becee9948c.apk`, 157,022,247 bytes; SHA-256 `06EE2703F330AAC9D50B8FA13DBC9CF2D41BC7A179B8557361FB2258489BB9E6`.
- Installed and cold-started on Honor PTP-N49 / Android 16. Exact package `com.codemastercody3d.orcaslicermobile`, version 0.8.248 / code 283; `MainActivity` top-resumed. Fresh startup logcat had no crash, ANR, or OOM markers. No screenshot was taken.
- Next: continue the global menu action audit and remaining BedFragment UI/overlay responsibilities. The 3MF multi-plate persistence, complete global menu action verification, and full system display/font/accessibility matrix remain open; this migration is not complete.

## Latest follow-up

- Fixed the loaded-plate fast path in `BedFragment.switchPlate`: its optional completion callback now fires on the GL thread after selecting the resident model and queuing plate renderer state, consistent with lazy-load completion. Extended `MultiPlateProjectDeviceTest.workspaceLoadsTheSecondPlateWithoutLosingTheFirst` to switch back to the already-loaded first plate and verify callback completion and tool assignment.
- The first full Honor instrumentation run exposed a 60-second timeout in that second-plate test. The targeted test passed after the new resident-plate callback assertion; a subsequent full run passed 55 reported tests, 0 failures, 3 private-fixture skips (52 executed and passed). Emulator remains offline.
- Debug unit suite: 549/549 passed, 0 failures/errors/skips. Slice unit suite: 549/549 passed, 0 failures/errors/skips.
- Version 0.8.246 alpha / versionCode 281. Final `:app:assembleDebug` passed from code commit `d9aa1c4`. APK: `app/build/outputs/apk/debug/OrkanSlicer_d9aa1c4a6c.apk`, 157,022,324 bytes; SHA-256 `5E40D7CA10CD2A992DB6D634B8920702AFBA72EF61EC5E651465E1B1F07E1337`.
- Installed and cold-started that APK on Honor PTP-N49 / Android 16 under `com.codemastercody3d.orcaslicermobile`; `MainActivity` top-resumed, package inventory contains only the OrkanSlicer app package, and fresh logcat had no crash/ANR/OOM markers. No screenshot was taken.
- Extended `FilamentPaletteComposeDeviceTest` to exercise 1/4/8/16/24 UI entries at 320 dp / 100%, 360 dp / 130%, and 400 dp / 150%. It scrolls to and selects the final item and asserts its accessible hit bounds are at least 48 dp in each dimension.
- Added a device Compose test for the two-row tool menu at 320 dp / 150%; it scrolls to the final action, checks visibility and 48 dp touch bounds, and invokes that action.
- Honor PTP-N49 / Android 16: both `FilamentPaletteComposeDeviceTest` cases passed (2/2) using `:app:connectedSliceTestAndroidTest`. Emulator remains offline. These are Compose UI tests with constrained dp/font-scale locals, not a full system font-scale or visual screenshot audit.
- Current phase remains the open workspace/menu migration and audit. Next: migrate a top-level menu fully off its `ThemeAwareComposeView` host through the WorkspaceShell Compose route, with an interaction/lifecycle regression test; continue the remaining route and accessibility audit.

## Current phase

Phase 4 — workspace overlay migration is continuing. The model context menu and all ten identified tool routes render through the existing `WorkspaceShell`; Kromacut keeps an embedded Android WebView. `BedFragment` still retains renderer/domain integration, generic message/custom dialog hosts, Kromacut lifecycle, and Android back/inset coordination. The full UI 2.0 migration is not complete.

## Completed in this execution

- Paint, Measure, Variable Layer Height, and Emboss use the exclusive Compose workspace mode-panel host; renderer mutations remain on the GL thread.
- `BedFragment.showUnfoldMenu` now registers route content and a dismiss callback with `WorkspaceController`; panel dismissal removes the content and runs route cleanup through AndroidView attach/detach lifecycle callbacks.
- `WorkspaceController` carries the requested route panel fraction so the old `getRequestedSize` sizing behavior is retained within the shared Compose panel host.
- Moved adaptive bottom navigation and navigation rail rendering into `WorkspaceShell`; `BedFragment` now provides selected destination, expanded state, and action callback rather than constructing/owning a separate navigation `ComposeView`.
- Workspace navigation theme changes now invalidate the root shell composition. Responsive rail width and bottom-navigation height continue to use the shared policies.
- Replaced the manual BedFragment menu `FrameLayout` and SpringAnimation transition with a Compose `AnimatedContent` host in portrait and landscape. BedFragment lazily creates destination views and preserves the interaction guard through the transition; `GLView` remains a stable sibling host.
- Updated the workspace navigation device regression to assert that the selected route view is attached and the prior route view is detached after transition.
- Added Honor Compose UI tests for destination accessibility/selection and large-text two-row navigation.
- Earlier in the execution, updated the in-app changelog for 0.8.243; this pass supersedes it with localized 0.8.244 release notes.
- Recorded the Compose-owned menu-route transition in the repository changelog.
- The legacy `UnfoldMenu` Android overlay is still available to callers outside the BedFragment workspace, including MainActivity routes.
- Added direct Compose workspace content for the position, rotation, scale, cut, calibration, calibration-model, slice-information, slice-layer, and AI-generator routes. Their existing `onCreate`/`onDestroy`, callbacks, renderer operations, and system activity-result flows are retained; the former `ThemeAwareComposeView` implementations remain compatibility fallbacks for non-workspace callers.
- Extended the Honor navigation instrumentation to open and close all ten direct Compose routes and verify no route Android View is allocated, composition lifecycle is active, and Android Back clears the selected tool. Kromacut additionally verifies its attached WebView and restricted file-access setting. This verifies route lifecycle/navigation, not every tool action or model mutation.
- Bumped the app to alpha `0.8.244` / `versionCode 279`; updated both the repository changelog and localized in-app release notes.
- Added a device Compose test for the workspace-hosted UnfoldMenu lifecycle and an assertion for panel-fraction state ownership.
- SliceBeam was explicitly uninstalled from the Honor device. The latest package inventory shows `com.codemastercody3d.orcaslicermobile` installed; no screenshot was captured during this execution; device checks targeted the OrkanSlicer package ID.
- Moved model context-menu composition ownership into `WorkspaceController.activePanelContent` and `WorkspaceShell`; removed the separate context-menu `ComposeView` attach/detach path from `BedFragment`. Existing action callbacks remain unchanged and Back/exclusive overlay dismissal still clears the same panel state.
- Removed the unused MainActivity.showUnfoldMenu host and its duplicate unfoldMenu back-state. Repository call-site search found no callers; active legacy tool routes are hosted through BedFragment and its WorkspaceShell panel path.
- Added a loaded-model Honor integration check for paint cancellation: the bundled `models/two-color-project.3mf` is loaded into the test workspace, GL paint mode is entered and cancelled through Android Back, and the test verifies both renderer mode exit and unchanged paint facets. The standalone Compose panel test separately verifies its Cancel and Done callbacks.
- Hardened the workspace route device test to wait for the actual settled Compose transition (animation idle and one attached route) before asserting route cleanup; this avoids sampling transient outgoing content.
- Fixed the GL measurement lifecycle: switching away from the Measure panel now clears renderer measurement mode and points on the GL thread; the panel Done and Android Back paths share the same BedFragment exit cleanup.
- Updated the localized in-app release notes in `app/src/main/assets/update.json` for 0.8.245 (German, English, and Russian).
- Extended the loaded-model Honor integration path to enter Measure, switch to Variable Layer Height, and assert both Compose exclusive tool state and the renderer cleared measurement flag.

## Remaining active routes and technical boundaries

- Concrete BedFragment UnfoldMenu routes identified before this pass: 10 (position, rotation, scale, cut, calibration, calibration-model picker, AI generator, slice information, slice layers, Kromacut). All ten now render directly in WorkspaceShell. Kromacut retains its necessary WebView, file chooser, and JavaScript download bridge, while its visible close toolbar is Compose.
- Migrated tool routes retain Compose-based `onCreateView` compatibility entry points. The active BedFragment workspace uses direct Compose for all ten routes, including Kromacut; AndroidView is limited to its WebView editor surface.
- `SliceLayersMenu` direct route was opened and dismissed on device without a populated G-code preview; its preview slider actions still need a sliced-project device test.
- `UnfoldMenu` retains its compatibility route API and non-workspace `onCreateView` implementations. The unused duplicate `MainActivity.showUnfoldMenu` host and back-state were removed; Kromacut is reached through BedFragment.
- `BedFragment` still owns the GLView/renderer and domain integration, menu-view lifecycle, and Android back/inset coordination.
- Generic message/custom dialogs remain in a manually attached ComposeView on the existing overlay host. A WorkspaceShell-only host attempt failed the Honor Back sequence for camera-control and numeric duplicate dialogs and was fully reverted.
- Global menu actions, the full menu-by-menu audit, large-font/small-display matrix, and full specialty-tool edit/undo/abort behavior remain unverified.
- BedFragment retains technical GLView/renderer lifecycle, domain-controller entry points, and Android configuration handling; these require selective migration and testing.
- The GL renderer and native Slicer engine remain necessary technical integrations and were not rewritten.
- Multi-plate project persistence is incomplete. Keep the active-plate-only export warning.
- Full menu-by-menu functional audit, small-display/font-scale/accessibility matrix, and real multi-plate/filament interaction stress coverage remain open.

## Verification in this execution

- Previous baseline at the version-0.8.244 code commit: Debug 548 tests, 0 failures, 0 errors, 0 skipped.
- Previous baseline at the version-0.8.244 code commit: Slice 548 tests, 0 failures, 0 errors, 0 skipped.
- On Honor PTP-N49 / Android 16, `WorkspaceNavigationDeviceTest`: 1 test, 0 failures/skips; it verifies ten direct Compose routes attach and clean up through Back, checks Kromacut embeds a WebView with file access disabled, and checks File → Model → File transition.
- On Honor PTP-N49 / Android 16, `PaintModeComposeDeviceTest`: 2/2 passed. It exercises brush toggle, bucket parameter visibility, cancel callback, and separate Done/commit callback with a GLView host; it does not assert a loaded model's GL paint mutation.
- Latest combined verification at code commit 49b92ad9c7d0fcaccf6914d600217887cafe9237: debug unit suite 549/549, Slice unit suite 549/549, and WorkspaceNavigationDeviceTest 1/1 on Honor (Kromacut WebView and file-access checks included); `:app:assembleDebug` passed. Current APK: OrkanSlicer_49b92ad9c7.apk, 157022466 bytes, SHA-256 8DEE75EC93B5838B7B97BDA8CD5C4BE46646A285A44BFDCC97D317506F025E81. Installed and launched under com.codemastercody3d.orcaslicermobile; MainActivity top-resumed, no crash/ANR/OOM matches in fresh logcat sample.
- After moving the model context menu to workspace overlay state, `MainActivityTouchDeviceTest`: 1 test passed on Honor PTP-N49 / Android 16 after reverting the unsuccessful generic-dialog host experiment. The full Honor instrumentation suite now reports 53 tests, 0 failures, 0 errors, 3 skips (three private multi-color fixtures); the stale rotation test was updated to verify the attached WorkspaceShell Compose root. Both full unit suites passed: Debug 549/549 and Slice 549/549 (0 failures, 0 errors, 0 skipped each). A test run during the dialog-host experiment failed at camera-control Back and duplicate-input Back; that experiment and its changes were reverted. Emulator `emulator-5554` remains offline and was skipped.
- Final `:app:assembleDebug`: passed. The final APK was installed under the exact OrkanSlicer application ID and launched with `MainActivity` top-resumed. Logcat after launch had no `FATAL EXCEPTION`, `ANR in`, `OutOfMemoryError`, or `Fatal signal` matches. `ru.ytkab0bp.slicebeam` is the source namespace/activity, not an installed standalone SliceBeam app; the device package check showed only `com.codemastercody3d.orcaslicermobile`.
- Emulator `emulator-5554` remains offline and was skipped by Gradle. No 320/360/400 dp or font-scale matrix was run; the Honor was used at its physical 1280x2800 / 560 dpi configuration.
- The prior broader device run on the previous code commit reported 51 tests, 0 failures, 0 errors, and 3 skips due to unavailable private fixtures; this execution did not rerun that full device suite. `emulator-5554` remains offline.
- The Honor `WorkspaceNavigationDeviceTest` now also exercises paint cancellation against a loaded 3MF: 1/1 passed after waiting for both the menu transition flag and actual attached route count to settle. An initial run exposed that the old test checked attached-route count before the Compose transition ended; the wait now checks the real settled state. A UIAutomation accessibility-tree approach was discarded after Android 16 rejected a duplicate automation connection. No screenshot was captured during that test.
- Paint cancellation coverage is limited to GL paint-mode abort via Android Back and verifies the model remains unpainted; it does not test painted-facet undo/redo or every paint mode.
- Latest full verification after the continuation change: Debug unit 549/549 passed; Slice unit 549/549 passed; full Honor Slice instrumentation 54 tests, 0 failures/errors, 3 skipped private multi-color fixtures; final `:app:assembleDebug` passed. The connected device was Honor PTP-N49 / Android 16; `emulator-5554` was skipped as offline.
- After adding measurement-mode cleanup coverage, targeted Honor `WorkspaceNavigationDeviceTest`: 1/1 passed on PTP-N49 / Android 16; it loads the bundled 3MF, cancels paint without model mutation, enters Measure, switches to Variable Layer Height, verifies GL measurement state is cleared, and verifies Back closes the panel. `emulator-5554` remained offline.
- Full Honor `:app:connectedSliceTestAndroidTest` rerun after the GL cleanup: 54 test cases, 0 failures/errors, 3 skipped private fixtures (51 executed and passed). Freshly relaunched the 0.8.245 OrkanSlicer APK afterward; MainActivity resumed, package inventory showed only `com.codemastercody3d.orcaslicermobile`, and cleared logcat had no crash/ANR/OOM markers. Emulator remained offline.
- Latest Debug APK: `app/build/outputs/apk/debug/OrkanSlicer_805ce67a72.apk`, 157022458 bytes, SHA-256 `2ABF93E6593F0FC7991F8DE8C98C40FB4A896E97FD72B88103A6A6CA39BD9BB6`. It was installed and launched under `com.codemastercody3d.orcaslicermobile`; the resumed activity was `MainActivity`, and the sampled logcat contained no crash/ANR/OOM markers. Package verification found only the OrkanSlicer app package; the separate SliceBeam package was not installed.
- Measurement lifecycle fix verification: Debug 549/549 and Slice 549/549 unit tests passed; targeted Honor WorkspaceNavigationDeviceTest 1/1 passed; full Honor instrumentation 54 cases, 0 failures/errors, 3 skipped private fixtures (51 passed); full :app:assembleDebug passed. Version 0.8.245 alpha / versionCode 280. Final Debug APK app/build/outputs/apk/debug/OrkanSlicer_7155e8f613.apk, 157022269 bytes, SHA-256 2021BA4A0550C1C3E10AA2F5325788BFB5314D43E77B7AC196F0A6EEB15F367A. Installed and launched on Honor PTP-N49 under package com.codemastercody3d.orcaslicermobile; versionName/versionCode verified, MainActivity top-resumed, and sampled logcat had no fatal crash/ANR/OOM markers. Package inventory showed only OrkanSlicer; no screenshot was taken.
- Final Debug APK from code commit `ba596dbea946f973626f1ef0399ad4594e0b3df4`: `app/build/outputs/apk/debug/OrkanSlicer_ba596dbea9.apk`, 157,572,357 bytes; SHA-256 `7A2CCD450D8E71D6F56A1ACD33C5239108C750922D6763797FDA9C98AB32531B` (versionCode 279, versionName 0.8.244).
- The final APK above was reinstalled on Honor PTP-N49 / Android 16 under the explicit OrkanSlicer application ID `com.codemastercody3d.orcaslicermobile`; `MainActivity` was top-resumed. Logcat captured after a clean and fresh launch had no `FATAL EXCEPTION`, `ANR in`, `OutOfMemoryError`, or `Fatal signal` matches. The standalone SliceBeam package was absent in the package check.
- No printer job was started and no firmware was changed.

## Git and continuation

- Branch: `codex/rinkhals-safe-start-audit`.
- Latest code commit: `7155e8f613d6c9c9a52b2d447a81e064fb466546` (`Update localized 0.8.245 release notes`); APK built from this exact commit. The prior GL fix is in code commit `df0e6bd83f16f3a483e3abbb8cb99455351855df`.
- Preserve the unrelated untracked `.codex-remote-attachments/` directory; never stage it.
- PR #2 is open, unmerged, and targets `master`. Code and verification documentation have been pushed to `codex/rinkhals-safe-start-audit`; the latest GitHub check reported `OPEN` and `CLEAN`. No merge or release was performed.
- Next implementation task: continue the remaining active GL-tool and global menu route audit, then run responsive/accessibility configurations. Expand actual filament and plate UI interaction coverage where the existing Honor harness allows. Keep generic dialog hosts unchanged until an IME-aware device test is in place. Multi-plate project persistence remains separate and incomplete. Do not claim the overall migration complete.
