# Kobra 3 V2 Moonraker reliability

The upload flow reserves the normalized printer origin while a job is being prepared, uploaded, and optionally started. This prevents a second in-app request from racing the first request for that printer. Existing G-code integrity checks and ACE assignment verification still run before upload.

Moonraker file upload is accepted only after its response identifies the expected file under `gcodes`. A print start is never automatically retried. If its response is missing, malformed, or lost, the app issues one lightweight `print_stats` query and reports success only when that file is currently printing or paused. If the state cannot confirm that file, the UI asks the user to inspect the printer before another start.

The changes leave printer firmware untouched and retain the existing Anycubic G-code transformation. Unit coverage is added for Moonraker print-state parsing and filename matching, plus concurrent origin reservations.

For Anycubic Kobra uploads, slicer tool `Tn` is now fixed to ACE slot `n + 1`. Empty slots keep their position: the uploader validates the exact used slot and printer `ttg_map`, then refuses the upload if that slot is empty or redirected. It no longer substitutes the first loaded gate or rewrites a tool command to another ACE slot.

The preparation dialog shows this fixed mapping and checks material against the matching physical slot. Empty-slot gap warnings now use tool indices directly. Unit coverage verifies sparse G-code tools preserve their commands and palette positions and that an empty used slot or redirected firmware map is rejected.

## Verification

- `:app:testDebugUnitTest` passed.
- `:app:assembleDebug` could not finish: the native CMake target requires `app/src/main/jniImports/oneTBB/lib/arm64-v8a/libtbb.a`, which is absent from this checkout.
- The prebuilt-native APK path is unavailable for this branch because its cache targets app version `0.8.197` (build 232), while this source targets `0.8.232` (build 267). The repository correctly rejects that stale bundle.
- After the ACE routing update, Gradle verification could not be rerun in this execution because the wrapper could not open its distribution lock under the user's `.gradle` directory (`Access denied`). The earlier test run on the routing changes passed before the final dialog gap-warning helper was simplified; the latest helper call sites and tests were reviewed, but remain unverified by Gradle.
