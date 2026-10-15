# Review: OrkanSlicer Rinkhals Safe Start patch

Reviewed 2026-10-15 against repository `master` (`e48f93a`) and the open Safe Start development PR. The attached ZIP is review input only; its installer, shell script, and firmware patch were not executed or applied.

## Decision

The main Kotlin changes in the ZIP duplicate work already present in the open development PR. Do not apply its whole-method replacement or add a second Moonraker gate. Keep the existing controller flow and retain only the correction in this PR: reconcile a print start against the exact relative G-code path, not only the basename. That prevents `jobs/part.gcode` from being reported as started when Moonraker says `archive/part.gcode` is active.

The `Rinkhals` patch is a separate change to printer-side G-code metadata processing. It does not belong in OrkanSlicer and is not included here.

## Comparison and code findings

- `master` already has URL construction/validation, upload acknowledgement validation, a dedicated print-start client, and explicit response inspection. It does not have the process-local upload reservation or the one-shot `print_stats` reconciliation used by the open Safe Start PR.
- The ZIP adds these same reservation/reconciliation concepts, but its installer replaces the complete `uploadMoonraker` method and introduces another gate. This is redundant with the current PR's broader reservation, which also covers preparation and upload and applies across printer upload types.
- The ZIP replacement refers to `statusClient`, but its installer does not declare or inject that client. It would not compile against the reviewed controller without additional edits. Its stub compilation does not validate the full installed project.
- A single read-only status query after an uncertain `print/start` response is appropriate. The request is not retried; only a matching active file in `printing` or `paused` is treated as confirmation. An idle/error response or lost status query remains inconclusive and requires the user to inspect the printer.
- The previous comparison accepted any path with the same basename. The added regression test now requires the same relative path, including directories.
- The in-memory reservation prevents races only within one app process and normalized URL origin. It cannot recognize that an IP address and a `.local` alias are the same printer, and it is lost on app restart. No automatic retry occurs after restart, but a user-initiated retry after an interrupted ambiguous start still needs a manual printer-status check.
- If an upload fails before Moonraker acknowledges it, the app does not issue a print-start request. A partial or replaced remote file may still need manual cleanup; automatic delete/re-upload is intentionally avoided.
- Status polling is conditional, not periodic. The current implementation creates its status HTTP client only for ambiguous starts; this is low-frequency overhead, but no on-device resource profiling was available. The existing synchronous wait is bounded by the status client's timeouts.

## ACE Pro, slicers, and other printer types

The ZIP replacement is confined to the Moonraker upload function. It does not itself change ACE mapping or G-code rewriting, and it leaves QIDI packaging and OctoPrint paths outside that function. The open PR already retains the ACE gate validation and fixed `T0` → slot 1 mapping; its unit tests cover sparse tools and rejected empty/redirected gates. The exact-path fix affects only Moonraker print-start reconciliation.

## Optional Rinkhals patch

The patch targets `mmu_ace_metadata.py` in Rinkhals `20260901_01`, not an OrkanSlicer source file. The upstream implementation performs an initial metadata/strip scan and, only for supported slicers, a second tool-discovery scan. The proposed refactor removes that second file read for supported slicers and preserves the current tool list by clearing it for unsupported slicers. However, it also runs the tool regex over every line of unsupported or unidentified slicer files, where upstream previously skipped tool discovery. The claimed I/O saving therefore needs timing tests on representative supported and unsupported files. Since this parser's output feeds ACE/MMU G-code preprocessing, this review does not apply or package the change; it should be proposed and tested in the Rinkhals project separately.

## Verification and limits

- `:app:testDebugUnitTest` passed after the exact-path regression test: 501 tests, 0 failures/errors.
- `:app:testSliceTestUnitTest` passed: 501 tests, 0 failures/errors.
- `:app:assembleDebug` passed after the correction, including the native ARM64 component and locally available native dependencies. This verifies compilation, not printer behavior.
- No printer was connected, no physical print was started, and no Rinkhals SWU/firmware was built or installed.
- CPU, memory, and Wi-Fi/reconnect behavior were not profiled on a Kobra 3 V2.

## Hardware-test recommendation

Suitable only for a supervised first hardware test after the updated unit tests and build pass. Start with upload-only, confirm the resulting file in Moonraker, then use a small attended print while keeping the printer's stop control available. Do not treat an ambiguous status result as permission to resend `print/start`. Do not install the optional Rinkhals patch as part of this app test.
