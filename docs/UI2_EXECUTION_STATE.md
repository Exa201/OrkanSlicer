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

- `:app:testDebugUnitTest`: 548 tests, 0 failures, 0 errors, 0 skipped.
- `:app:testSliceTestUnitTest`: 548 tests, 0 failures, 0 errors, 0 skipped.
- `:app:connectedSliceTestAndroidTest`: Honor PTP-N49 / Android 16 — 51 tests, 0 failures, 0 errors, 3 skipped due to unavailable private fixtures. `emulator-5554` was offline and skipped.
- `:app:compileDebugKotlin`, `:app:compileSliceTestAndroidTestKotlin`, and `:app:assembleDebug`: passed.
- After the direct navigation migration, `:app:compileDebugKotlin` and `:app:testDebugUnitTest` passed; `:app:compileSliceTestAndroidTestKotlin` passed; the two targeted `AdaptiveWorkspaceNavigationDeviceTest` tests passed on Honor PTP-N49 / Android 16.
- The added device test exercises Compose-hosted legacy route attach, dismiss, and cleanup. It does not claim to validate every route's GL model-edit behavior.
- Final debug APK: `app/build/outputs/apk/debug/OrkanSlicer_d7bc3e59da.apk`, 157,006,048 bytes; SHA-256 `51DDE60BAFB0C1D51A459C9AC6364130B7751D1F757D612C0D04B92B8346FCE8`.
- APK was installed on Honor PTP-N49 / Android 16 under `com.codemastercody3d.orcaslicermobile`; `MainActivity` was top-resumed. The final workspace screenshot is `app/build/orkan_workspace_final.png` (ignored build output). The recent checked logcat contained no `FATAL EXCEPTION`, `ANR in`, `OutOfMemoryError`, or `Fatal signal` lines.
- No printer job was started and no firmware was changed.

## Git and continuation

- Branch: `codex/rinkhals-safe-start-audit`.
- Latest code commit at the start of the current change: `d7bc3e59da28565d52f7bd89b37a1d654f441209` (`Host legacy workspace routes in Compose panels`). Direct navigation work is being verified and will be recorded with its commit and a new debug APK before this execution ends.
- Preserve the unrelated untracked `.codex-remote-attachments/` directory; never stage it.
- PR #2 remains open, unmerged, and targets `master`. Branch synchronization was verified after push; GitHub reports `MERGEABLE` / `CLEAN`. No merge or release was performed.
- Next implementation task: migrate remaining BedFragment workspace menu/action ownership and convert remaining reachable route contents in coherent groups, starting with the simplest ComposeView-backed route. Then continue the functional menu audit and responsive/accessibility coverage.
