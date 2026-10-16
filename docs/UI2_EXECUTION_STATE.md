# OrkanSlicer UI 2.0 execution state

Updated: 2026-10-16

## Current phase

Phase 4 — workspace overlay migration is continuing. The model context menu now renders in the existing `WorkspaceShell` instead of a separately attached `ComposeView`. `BedFragment` still retains renderer/domain integration, generic message/custom dialog hosts, Kromacut, and Android back/inset coordination. The full UI 2.0 migration is not complete.

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
- Extended the Honor navigation instrumentation to open and close all nine direct Compose routes and verify no route Android View is allocated, composition lifecycle is active, and Android Back clears the selected tool. This verifies route lifecycle/navigation, not every tool action or model mutation.
- Bumped the app to alpha `0.8.244` / `versionCode 279`; updated both the repository changelog and localized in-app release notes.
- Added a device Compose test for the workspace-hosted UnfoldMenu lifecycle and an assertion for panel-fraction state ownership.
- SliceBeam was explicitly uninstalled from the Honor device (`ru.ytkab0bp.slicebeam`). The OrkanSlicer package (`com.codemastercody3d.orcaslicermobile`) was separately reinstalled and launched; only that package was used for the final screenshot and launch verification.
- Moved model context-menu composition ownership into `WorkspaceController.activePanelContent` and `WorkspaceShell`; removed the separate context-menu `ComposeView` attach/detach path from `BedFragment`. Existing action callbacks remain unchanged and Back/exclusive overlay dismissal still clears the same panel state.
- Removed the unused MainActivity.showUnfoldMenu host and its duplicate unfoldMenu back-state. Repository call-site search found no callers; active legacy tool routes are hosted through BedFragment and its WorkspaceShell panel path.

## Remaining active routes and technical boundaries

- Concrete BedFragment UnfoldMenu routes identified before this pass: 10 (position, rotation, scale, cut, calibration, calibration-model picker, AI generator, slice information, slice layers, Kromacut). Nine now render directly in WorkspaceShell; Kromacut remains a native `WebView` editor with an Android toolbar, file chooser, and JavaScript download bridge.
- Migrated tool routes retain `onCreateView` ComposeView fallbacks for non-workspace compatibility. The active BedFragment workspace uses direct Compose for the nine routes listed above.
- `SliceLayersMenu` direct route was opened and dismissed on device without a populated G-code preview; its preview slider actions still need a sliced-project device test.
- `UnfoldMenu` retains the standalone Android overlay API for compatibility. `MainActivity.showUnfoldMenu` remains declared but has no in-repository call sites; Kromacut is reached through BedFragment.
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
- On Honor PTP-N49 / Android 16, `WorkspaceNavigationDeviceTest`: 1 test, 0 failures/skips; it verifies nine direct Compose routes attach and clean up through Back, plus File → Model → File transition.
- Latest change removes the unused MainActivity-level UnfoldMenu host. Verification on the change: debug Kotlin compilation and Debug unit suite passed (549/549); Slice unit suite passed (549/549); WorkspaceNavigationDeviceTest passed 1/1 on Honor. Debug build passed. APK: OrkanSlicer_ede9339ab5.apk, 157006074 bytes, SHA-256 48B995D226932B2D291B25D01D94F3590D9367E87D3EC7F2F7A5F8D022E8EB83. Installed and launched as com.codemastercody3d.orcaslicermobile; MainActivity was top-resumed and inspected logcat had no crash/ANR/OOM markers. Filename derives from pre-commit HEAD ede9339; the APK includes this local removal.
- After moving the model context menu to workspace overlay state, `MainActivityTouchDeviceTest`: 1 test passed on Honor PTP-N49 / Android 16 after reverting the unsuccessful generic-dialog host experiment. The full Honor instrumentation suite now reports 53 tests, 0 failures, 0 errors, 3 skips (three private multi-color fixtures); the stale rotation test was updated to verify the attached WorkspaceShell Compose root. Both full unit suites passed: Debug 549/549 and Slice 549/549 (0 failures, 0 errors, 0 skipped each). A test run during the dialog-host experiment failed at camera-control Back and duplicate-input Back; that experiment and its changes were reverted. Emulator `emulator-5554` remains offline and was skipped.
- Final `:app:assembleDebug`: passed. The final APK was installed under the exact OrkanSlicer application ID and launched with `MainActivity` top-resumed. Logcat after launch had no `FATAL EXCEPTION`, `ANR in`, `OutOfMemoryError`, or `Fatal signal` matches. `ru.ytkab0bp.slicebeam` is the source namespace/activity, not an installed standalone SliceBeam app; the device package check showed only `com.codemastercody3d.orcaslicermobile`.
- Emulator `emulator-5554` remains offline and was skipped by Gradle. No 320/360/400 dp or font-scale matrix was run; the Honor was used at its physical 1280x2800 / 560 dpi configuration.
- The prior broader device run on the previous code commit reported 51 tests, 0 failures, 0 errors, and 3 skips due to unavailable private fixtures; this execution did not rerun that full device suite. `emulator-5554` remains offline.
- The added device test exercises Compose-hosted legacy route attach, dismiss, and cleanup. It does not claim to validate every route's GL model-edit behavior.
- Final Debug APK from code commit `ba596dbea946f973626f1ef0399ad4594e0b3df4`: `app/build/outputs/apk/debug/OrkanSlicer_ba596dbea9.apk`, 157,572,357 bytes; SHA-256 `7A2CCD450D8E71D6F56A1ACD33C5239108C750922D6763797FDA9C98AB32531B` (versionCode 279, versionName 0.8.244).
- The final APK above was reinstalled on Honor PTP-N49 / Android 16 under the explicit OrkanSlicer application ID `com.codemastercody3d.orcaslicermobile`; `MainActivity` was top-resumed. Logcat captured after a clean and fresh launch had no `FATAL EXCEPTION`, `ANR in`, `OutOfMemoryError`, or `Fatal signal` matches. The standalone SliceBeam package was absent in the package check.
- No printer job was started and no firmware was changed.

## Git and continuation

- Branch: `codex/rinkhals-safe-start-audit`.
- New code commits: `00bbb63d8f8019a2e13d5aa0970ab1a2b8e833ca` (`Render tool routes directly in Compose workspace`), `c29695d408186970e124f582e9c5967820448a3e` (`Bump alpha version to 0.8.244`), `ba596dbea946f973626f1ef0399ad4594e0b3df4` (`Host model context menu in workspace Compose overlay`). The model context menu renders through exclusive WorkspaceShell overlay content; its separate ComposeView attachment/removal path is gone. Generic dialog hosts remain unchanged after the attempted migration failed the Honor Back/keyboard test and was reverted.
- Preserve the unrelated untracked `.codex-remote-attachments/` directory; never stage it.
- PR #2 remains open, unmerged, and targets `master`. Branch synchronization was verified after push; GitHub reports `MERGEABLE` / `CLEAN`. No merge or release was performed.
- Next implementation task: continue with the remaining GL-backed tool presentation and actual tool-action verification, then complete menu/layout/accessibility work. Keep generic dialog hosts unchanged until an IME-aware device test is in place. Multi-plate project persistence remains separate and incomplete. Do not claim the overall migration complete.
