# OrkanSlicer UI 2.0 execution state

Updated: 2026-10-16

## Current phase

Phase 4 — `WorkspaceShell` owns adaptive navigation and menu-route transitions. `BedFragment` no longer maintains a route `FrameLayout` or a manual SpringAnimation; it supplies route views through an Android interop factory. Route content still includes compatibility `View`/`ComposeView`, and the entire UI 2.0 migration is not complete.

## Completed in this execution

- Paint, Measure, Variable Layer Height, and Emboss use the exclusive Compose workspace mode-panel host; renderer mutations remain on the GL thread.
- `BedFragment.showUnfoldMenu` now registers route content and a dismiss callback with `WorkspaceController`; panel dismissal removes the content and runs route cleanup through AndroidView attach/detach lifecycle callbacks.
- `WorkspaceController` carries the requested route panel fraction so the old `getRequestedSize` sizing behavior is retained within the shared Compose panel host.
- Moved adaptive bottom navigation and navigation rail rendering into `WorkspaceShell`; `BedFragment` now provides selected destination, expanded state, and action callback rather than constructing/owning a separate navigation `ComposeView`.
- Workspace navigation theme changes now invalidate the root shell composition. Responsive rail width and bottom-navigation height continue to use the shared policies.
- Replaced the manual BedFragment menu `FrameLayout` and SpringAnimation transition with a Compose `AnimatedContent` host in portrait and landscape. BedFragment lazily creates destination views and preserves the interaction guard through the transition; `GLView` remains a stable sibling host.
- Updated the workspace navigation device regression to assert that the selected route view is attached and the prior route view is detached after transition.
- Added Honor Compose UI tests for destination accessibility/selection and large-text two-row navigation.
- Updated the in-app changelog asset to show version 0.8.243 and include the direct Compose navigation change; verified the displayed version/text in the installed OrkanSlicer app.
- Added an Unreleased changelog entry for the Compose-owned menu-route transition.
- The legacy `UnfoldMenu` Android overlay is still available to callers outside the BedFragment workspace, including MainActivity routes.
- Added a device Compose test for the workspace-hosted UnfoldMenu lifecycle and an assertion for panel-fraction state ownership.
- SliceBeam was explicitly uninstalled from the Honor device (`ru.ytkab0bp.slicebeam`). The OrkanSlicer package (`com.codemastercody3d.orcaslicermobile`) was separately reinstalled and launched; only that package was used for the final screenshot and launch verification.

## Remaining active routes and technical boundaries

- Workspace route contents still include legacy Android Views and ComposeViews inside AndroidView; route-specific controls have not all been extracted into native Compose components.
- MainActivity and other non-workspace callers still use the standalone UnfoldMenu overlay path.
- BedFragment retains technical GLView/renderer lifecycle, domain-controller entry points, and Android configuration handling; these require selective migration and testing.
- The GL renderer and native Slicer engine remain necessary technical integrations and were not rewritten.
- Multi-plate project persistence is incomplete. Keep the active-plate-only export warning.
- Full menu-by-menu functional audit, small-display/font-scale/accessibility matrix, and real multi-plate/filament interaction stress coverage remain open.

## Verification in this execution

- Current code: `:app:testDebugUnitTest` and `:app:testSliceTestUnitTest` each report 548 tests, 0 failures, 0 errors, 0 skipped.
- `:app:compileDebugKotlin`, `:app:compileSliceTestAndroidTestKotlin`, and `:app:assembleDebug`: passed.
- On Honor PTP-N49 / Android 16, `WorkspaceNavigationDeviceTest` passed (1 test, 0 failures/skips): File → Model → File completed, and only the selected route view remained attached after each transition.
- The prior broader device run on the previous code commit reported 51 tests, 0 failures, 0 errors, and 3 skips due to unavailable private fixtures; this execution did not rerun that full device suite. `emulator-5554` remains offline.
- The added device test exercises Compose-hosted legacy route attach, dismiss, and cleanup. It does not claim to validate every route's GL model-edit behavior.
- Final debug APK built from code commit `0be0bfd972fcfdf6ebaf8f52e7c52708d539d655`: `app/build/outputs/apk/debug/OrkanSlicer_0be0bfd972.apk`, 157,006,113 bytes; SHA-256 `4370C8BDADB29B01933ED7C8BBABF7ED239CA3464C2F8314FCB77079C8855DB8` (versionCode 278, versionName 0.8.243).
- The final APK was installed on Honor PTP-N49 / Android 16 under the explicit Orkan package `com.codemastercody3d.orcaslicermobile`; `MainActivity` was top-resumed. The screenshot `app/build/orkan_workspace_transition.png` shows the OrkanSlicer workspace navigation, printer/profile controls, and plate viewport. Recent logcat had no `FATAL EXCEPTION`, `ANR in`, `OutOfMemoryError`, or `Fatal signal` lines. The standalone SliceBeam package remained absent.
- No printer job was started and no firmware was changed.

## Git and continuation

- Branch: `codex/rinkhals-safe-start-audit`.
- Latest code commit: `0be0bfd972fcfdf6ebaf8f52e7c52708d539d655` (`Move workspace menu transitions into Compose`), pushed to the existing branch. A docs-only status update remains to be committed.
- Preserve the unrelated untracked `.codex-remote-attachments/` directory; never stage it.
- PR #2 remains open, unmerged, and targets `master`. Branch synchronization was verified after push; GitHub reports `MERGEABLE` / `CLEAN`. No merge or release was performed.
- Next implementation task: migrate the first remaining UnfoldMenu functional group (transform position/rotation/scale) to direct Compose routes, or document why each AndroidView compatibility boundary remains; then continue with global-menu interaction and accessibility audit.
