# OrkanSlicer UI 2.0 execution state

Updated: 2026-10-16

## Current phase

Phase 4 — continue migrating remaining workspace UI responsibilities out of `BedFragment`. Paint, Measure, Variable Layer Height, and Emboss now share the exclusive Compose workspace mode-panel host. The full UI 2.0 migration is not complete.

## Completed in this execution

- Replaced the Paint, Variable Layer Height, and Emboss custom View mode surfaces with Compose panels hosted by the shared workspace mode-panel host.
- Kept renderer mutations and model operations on the GL thread. Emboss event registration is owned by the Compose lifecycle through a small `EmbossModeController`.
- Paint now has explicit commit and cancel paths. Cancellation rolls back the active paint session; atomic finish protection prevents duplicate completion and stale tool cleanup cannot dismiss a replacement tool.
- Removed the three old View mode classes, their legacy mode host, and obsolete View-hosted device tests.
- Added Compose UI instrumentation for paint controls, Variable Layer Height, and Emboss; added a controller regression test for delayed cleanup from a replaced special tool.
- Moved the already-Compose Measure panel from its own manually attached bottom overlay into the same exclusive workspace mode-panel host; its nested surface was removed so the shared host supplies the panel surface consistently.
- Added a Measure panel render/readout instrumentation test. Back and tool switching for all four modes continue to use the same `WorkspaceController` exclusive tool lifecycle.
- The shared `UnfoldMenu` routes remain compatibility content under their existing Compose host. Their route-specific inner contents and action verification are still incomplete.

## Remaining active routes and technical boundaries

- The ten `UnfoldMenu` compatibility routes still use their existing Android/Compose/WebView inner content; migrating the visible host did not replace all route-specific controls.
- `BedFragment` still coordinates renderer lifecycle, menu/action integration, dialogs, and domain-controller entry points. Its remaining responsibilities must be separated selectively without disturbing GL or native Slicer lifecycle.
- The GL renderer and `GLView` remain necessary technical Android integrations.
- Multi-plate project persistence remains incomplete. The existing active-plate-only export warning must remain.
- Full menu-by-menu functional audit, responsive/accessibility matrix, and real multi-plate/filament interaction stress coverage remain open.

## Verification in this execution

- `:app:testDebugUnitTest`: 547 tests, 0 failures, 0 errors, 0 skipped.
- `:app:testSliceTestUnitTest`: 547 tests, 0 failures, 0 errors, 0 skipped.
- `:app:compileSliceTestAndroidTestKotlin`: passed.
- `:app:connectedAndroidTest`: Honor PTP-N49 / Android 16 — 49 tests, 0 failures, 0 errors, 3 skipped due to unavailable private fixtures. The offline `emulator-5554` was skipped.
- Compose instrumentation exercises paint controls and cancel, Variable Layer Height status/exit, Emboss settings/exit, and Measure readout rendering. It does not claim to validate GL model edits on a loaded project.
- `:app:assembleDebug` and install/launch must be rerun after committing the additional Measure-host consolidation below.
- Earlier APK from code commit `6ca56ca82fc9a0998cd89addbda9213fd19f4824`: `app/build/outputs/apk/debug/OrkanSlicer_6ca56ca82f.apk`, 157,006,045 bytes; SHA-256 `4FF5735B09CDB36A07A197678BE9AA06D6B3FBF132BD0BC9F618A11B1D8334A5`. This artifact does not include the Measure-host consolidation.
- The emulator (`emulator-5554`) is offline. No true printer job was started and no firmware was changed.

## Git and continuation

- Branch: `codex/rinkhals-safe-start-audit`.
- Base HEAD for this execution: `4aafd3dddcf0e9fc2340102a6c8cdbacf5a6a3b7`.
- Code commit: `6ca56ca82fc9a0998cd89addbda9213fd19f4824` (`Migrate GL mode panels to Compose`); pushed to the existing branch. The Measure-host consolidation is currently under verification and not yet committed.
- Build the updated APK from the next code commit; do not present the earlier APK as containing the Measure change.
- Preserve the unrelated untracked `.codex-remote-attachments/` directory; do not stage it.
- PR #2 remains open; latest GitHub report marked it mergeable. No merge or release was performed.
- Next concrete task: finish the Measure-host verification/build, then continue Phase 4 by moving an independently separable UI responsibility from `BedFragment` into the existing workspace Compose/controller structure, while preserving `GLView` identity and lifecycle.
