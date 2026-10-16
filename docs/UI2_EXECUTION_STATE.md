# OrkanSlicer UI 2.0 execution state

Updated: 2026-10-16

## Current phase

Phase 3 — GL-bound mode host integration. The common `UnfoldMenu` presentation host now uses Compose, and the three GL-bound mode Views plus the existing Compose measure panel share the workspace's exclusive `SPECIAL_TOOL` state and Back dismissal. Their native control surfaces and renderer callbacks remain intact; replacing those controls themselves still requires route-specific functional work.

## Completed work in this execution

- Phase 1 inventory verified against the active code: ten `UnfoldMenu` routes and three GL-bound View mode hosts (`PaintModeView`, `VariableLayerHeightModeView`, `EmbossModeView`). `MeasureModePanel` is Compose but was also manually attached.
- Replaced the old `UnfoldMenu` mirror/spring/FrameLayout presentation implementation with a responsive Compose sheet host. Existing route content remains at an explicit `AndroidView` compatibility boundary to preserve GL, WebView, and lifecycle behavior.
- The ten existing route classes continue to enter through that common host. Route-specific Apply/Cancel/device acceptance remains outstanding; this is a host migration, not a claim that every route's inner controls have been rewritten.
- Added attach/dismiss lifecycle and outside-dismiss device coverage for the common tool host.
- Added a shared Compose boundary for the three renderer-bound View mode hosts. GL callbacks stay in their existing views and remain dispatched as before.
- Registered Paint, Measure, Variable Layer Height, and Emboss modes with the exclusive workspace tool state. Back dismisses the active mode through its existing exit path.
- Fixed `WorkspaceController.selectTool` so switching from one special tool to another dismisses the previous host rather than replacing its callback. Added a regression test.
- Corrected the navigation instrumentation fixture to preserve the `crash` preference using its actual String type.

## Remaining legacy routes and technical boundaries

Ten named `UnfoldMenu` routes remain as compatibility route/content classes, but all use the shared Compose presentation host. Their contents span Compose, native Android Views, and WebView. The WebView file picker and Activity result handling remain platform integrations.

The three GL mode control surfaces are still implemented by native custom Views inside a Compose-owned AndroidView boundary. Their controls have not yet been converted to Compose and need functional mode-specific testing, especially Paint finish/cancel and variable-layer/emboss actions. GLView, GLRenderer, renderer lifecycle, and GL-thread dispatch are intentionally retained.

`BedFragment` still coordinates technical/domain actions, current tool references, and integration with the renderer. Removing the remaining UI/action orchestration requires further phased work; it is not complete.

## Verification

- `:app:testDebugUnitTest`: 546 tests, 0 failures/errors/skips.
- `:app:testSliceTestUnitTest`: 546 tests, 0 failures/errors/skips.
- `:app:compileDebugKotlin`: passed.
- New common `UnfoldMenu` host device test and Compose sheet outside-dismiss test passed in the full Honor run.
- Complete post-fix Honor instrumentation rerun: 47 tests, 0 failures, 3 skipped for unavailable private fixtures. The filtered GL adapter host test also passed. The emulator (`emulator-5554`) is offline.
- `:app:assembleDebug`: passed. APK `app/build/outputs/apk/debug/OrkanSlicer_849234b244.apk`, 156,973,286 bytes, SHA-256 `EF39ABD92A1B4FC563DD49C677858CA4B55C510C26DA8928DCF4F83ED9D2F6BC`.
- Honor PTP-N49 / Android 16: APK installed and launched; app process remained active. The inspected recent logcat contained no `FATAL EXCEPTION`, `ANR in`, `OutOfMemoryError`, or `Fatal signal` lines.
- Multi-plate project persistence remains separate and incomplete. The active-plate-only 3MF export warning must remain.

## Git state and continuation

- Base HEAD at the start of this execution: `aafb08a311b5d712422e2ec7f924f9e89bd63b19`.
- Latest code commit: `2f4678ad7e131b4ef382b367fd27f7c87f6fd748` (`Migrate tool overlays to shared Compose workspace host`).
- Build was produced after documentation commit `849234b2442da106210f0d7736e0b8d481b13de5`; no public release was created.
- PR #2 is OPEN and reported MERGEABLE. No merge was performed.
- Preserve the unrelated untracked `.codex-remote-attachments/` directory; do not stage it.
- Next concrete task: continue mode-specific functional migration and the remaining workspace/menu audit. Do not claim the whole UI 2.0 migration is complete.
