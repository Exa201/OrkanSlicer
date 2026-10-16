# OrkanSlicer UI 2.0 execution state

Updated: 2026-10-16

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
- SliceBeam was explicitly uninstalled from the Honor device (`ru.ytkab0bp.slicebeam`). The OrkanSlicer package (`com.codemastercody3d.orcaslicermobile`) was separately reinstalled and launched; only that package was used for the final screenshot and launch verification.
- Moved model context-menu composition ownership into `WorkspaceController.activePanelContent` and `WorkspaceShell`; removed the separate context-menu `ComposeView` attach/detach path from `BedFragment`. Existing action callbacks remain unchanged and Back/exclusive overlay dismissal still clears the same panel state.
- Removed the unused MainActivity.showUnfoldMenu host and its duplicate unfoldMenu back-state. Repository call-site search found no callers; active legacy tool routes are hosted through BedFragment and its WorkspaceShell panel path.
- Added a loaded-model Honor integration check for paint cancellation: the bundled `models/two-color-project.3mf` is loaded into the test workspace, GL paint mode is entered and cancelled through Android Back, and the test verifies both renderer mode exit and unchanged paint facets. The standalone Compose panel test separately verifies its Cancel and Done callbacks.
- Hardened the workspace route device test to wait for the actual settled Compose transition (animation idle and one attached route) before asserting route cleanup; this avoids sampling transient outgoing content.

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
- The Honor `WorkspaceNavigationDeviceTest` now also exercises paint cancellation against a loaded 3MF: 1/1 passed after waiting for both the menu transition flag and actual attached route count to settle. An initial run exposed that the old test checked attached-route count before the Compose transition ended; the wait now checks the real settled state. A UIAutomation accessibility-tree approach was discarded after Android 16 rejected a duplicate automation connection; no screenshot or wrong application was used.
- Paint cancellation coverage is limited to GL paint-mode abort via Android Back and verifies the model remains unpainted; it does not test painted-facet undo/redo or every paint mode.
- Latest full verification after the continuation change: Debug unit 549/549 passed; Slice unit 549/549 passed; full Honor Slice instrumentation 54 tests, 0 failures/errors, 3 skipped private multi-color fixtures; final `:app:assembleDebug` passed. The connected device was Honor PTP-N49 / Android 16; `emulator-5554` was skipped as offline.
- Latest Debug APK: `app/build/outputs/apk/debug/OrkanSlicer_805ce67a72.apk`, 157022458 bytes, SHA-256 `2ABF93E6593F0FC7991F8DE8C98C40FB4A896E97FD72B88103A6A6CA39BD9BB6`. It was installed and launched under `com.codemastercody3d.orcaslicermobile`; the resumed activity was `MainActivity`, and the sampled logcat contained no crash/ANR/OOM markers. Package verification found only the OrkanSlicer app package; the separate SliceBeam package was not installed.
- Final Debug APK from code commit `ba596dbea946f973626f1ef0399ad4594e0b3df4`: `app/build/outputs/apk/debug/OrkanSlicer_ba596dbea9.apk`, 157,572,357 bytes; SHA-256 `7A2CCD450D8E71D6F56A1ACD33C5239108C750922D6763797FDA9C98AB32531B` (versionCode 279, versionName 0.8.244).
- The final APK above was reinstalled on Honor PTP-N49 / Android 16 under the explicit OrkanSlicer application ID `com.codemastercody3d.orcaslicermobile`; `MainActivity` was top-resumed. Logcat captured after a clean and fresh launch had no `FATAL EXCEPTION`, `ANR in`, `OutOfMemoryError`, or `Fatal signal` matches. The standalone SliceBeam package was absent in the package check.
- No printer job was started and no firmware was changed.

## Git and continuation

- Branch: `codex/rinkhals-safe-start-audit`.
- Latest code commit: `97f20402af059785bc719cb58039aa49b461564f` (`Verify paint cancellation against loaded project`); it adds the loaded-model paint cancellation test and records the Honor results. The APK was built from product code at `805ce67a72cabe531b0e466b687e95e9b9cf6f55`; the intervening change only touched Android instrumentation, the changelog, and this execution record.
- Preserve the unrelated untracked `.codex-remote-attachments/` directory; never stage it.
- PR #2 is open, unmerged, and targets `master`. GitHub currently reports `OPEN` and `CLEAN` for head `codex/rinkhals-safe-start-audit`. No merge or release was performed. Push the new commit and refresh the PR record.
- Next implementation task: continue the remaining active GL-tool and global menu route audit, then run responsive/accessibility configurations. Expand actual filament and plate UI interaction coverage where the existing Honor harness allows. Keep generic dialog hosts unchanged until an IME-aware device test is in place. Multi-plate project persistence remains separate and incomplete. Do not claim the overall migration complete.
