# OrkanSlicer UI 2.0 execution state

Updated: 2026-10-16

## Current phase

Phase 4 — the workspace navigation is now rendered directly by `WorkspaceShell` instead of a nested navigation `ComposeView`. `BedFragment` still owns menu-route construction and legacy transition animation, and route content remains a compatibility `View`/`ComposeView`; the entire UI 2.0 migration is not complete.

## Completed in this execution

- Paint, Measure, Variable Layer Height, and Emboss use the exclusive Compose workspace mode-panel host; renderer mutations remain on the GL thread.
- `BedFragment.showUnfoldMenu` now registers route content and a dismiss callback with `WorkspaceController`; panel dismissal removes the content and runs route cleanup through AndroidView attach/detach lifecycle callbacks.
- `WorkspaceController` carries the requested route panel fraction so the old `getRequestedSize` sizing behavior is retained within the shared Compose panel host.
- Moved adaptive bottom navigation and navigation rail rendering into `WorkspaceShell`; `BedFragment` now provides selected destination, expanded state, and action callback rather than constructing/owning a separate navigation `ComposeView`.
- Workspace navigation theme changes now invalidate the root shell composition. Responsive rail width and bottom-navigation height continue to use the shared policies.
- Added Honor Compose UI tests for destination accessibility/selection and large-text two-row navigation.
- Updated the in-app changelog asset to show version 0.8.243 and include the direct Compose navigation change; verified the displayed version/text in the installed OrkanSlicer app.
- The legacy `UnfoldMenu` Android overlay is still available to callers outside the BedFragment workspace, including MainActivity routes.
- Added a device Compose test for the workspace-hosted UnfoldMenu lifecycle and an assertion for panel-fraction state ownership.
- SliceBeam was explicitly uninstalled from the Honor device (`ru.ytkab0bp.slicebeam`). The OrkanSlicer package (`com.codemastercody3d.orcaslicermobile`) was separately reinstalled and launched; only that package was used for the final screenshot and launch verification.

## Remaining active routes and technical boundaries

- Workspace UnfoldMenu route contents still include legacy Android Views and ComposeViews inside AndroidView; route-specific controls have not all been extracted into native Compose components.
- MainActivity and other non-workspace callers still use the standalone UnfoldMenu overlay path.
- BedFragment retains technical GLView/renderer lifecycle, current menu-view integration, domain-controller entry points, and Android configuration handling; these require selective migration and testing.
- The GL renderer and native Slicer engine remain necessary technical integrations and were not rewritten.
- Multi-plate project persistence is incomplete. Keep the active-plate-only export warning.
- Full menu-by-menu functional audit, small-display/font-scale/accessibility matrix, and real multi-plate/filament interaction stress coverage remain open.

## Verification in this execution

- Current code: `:app:testDebugUnitTest` and `:app:testSliceTestUnitTest` each report 548 tests, 0 failures, 0 errors, 0 skipped.
- `:app:compileDebugKotlin`, `:app:compileSliceTestAndroidTestKotlin`, and `:app:assembleDebug`: passed.
- On Honor PTP-N49 / Android 16, the two targeted `AdaptiveWorkspaceNavigationDeviceTest` tests passed: all six destinations were visible/reachable at 320 dp test width, selection dispatched, and destination accessibility labels remained available at 150% text scale.
- The prior broader device run on the previous code commit reported 51 tests, 0 failures, 0 errors, and 3 skips due to unavailable private fixtures; this execution did not rerun that full device suite. `emulator-5554` remains offline.
- The added device test exercises Compose-hosted legacy route attach, dismiss, and cleanup. It does not claim to validate every route's GL model-edit behavior.
- Final debug APK: `app/build/outputs/apk/debug/OrkanSlicer_046689025e.apk`, 157,006,109 bytes; SHA-256 `15012C9CC46DE6B306AF26F6B419B36441F8ECAD8D952C4FE73AE5571AC6AFC2`. It was built from code commit `046689025e1c9acb89cff3ba4a38d142862c1d98`.
- APK was installed and launched on Honor PTP-N49 / Android 16 under `com.codemastercody3d.orcaslicermobile`; `MainActivity` was top-resumed. `pm list packages` showed the Orkan package and no standalone SliceBeam package (`ru.ytkab0bp.slicebeam`). The in-app changelog visibly showed 0.8.243; after dismissing it, the workspace screenshot `app/build/orkan_workspace_0466890.png` showed OrkanSlicer navigation and plate. The checked logcat contained no `FATAL EXCEPTION`, `ANR in`, `OutOfMemoryError`, or `Fatal signal` lines.
- No printer job was started and no firmware was changed.

## Git and continuation

- Branch: `codex/rinkhals-safe-start-audit`.
- Latest code commit: `046689025e1c9acb89cff3ba4a38d142862c1d98` (`Synchronize in-app changelog with 0.8.243`). The workspace navigation implementation is in preceding code commit `85badc2fd97084eb76445d99f7c76fe424c2a04c`.
- Preserve the unrelated untracked `.codex-remote-attachments/` directory; never stage it.
- PR #2 remains open, unmerged, and targets `master`. Branch synchronization was verified after push; GitHub reports `MERGEABLE` / `CLEAN`. No merge or release was performed.
- Next implementation task: remove remaining BedFragment menu-view/action ownership in a safe functional group. Preserve the existing route transitions and renderer lifecycle; route contents are still partly compatibility Views/ComposeViews. Continue with route-specific Compose extraction and the menu/accessibility audit after that.
