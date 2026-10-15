# Kobra 3 V2 Moonraker reliability

The upload flow reserves the normalized printer origin while a job is being prepared, uploaded, and optionally started. This prevents a second in-app request from racing the first request for that printer. Existing G-code integrity checks and ACE assignment verification still run before upload.

Moonraker file upload is accepted only after its response identifies the expected file under `gcodes`. A print start is never automatically retried. If its response is missing, malformed, or lost, the app issues one lightweight `print_stats` query and reports success only when that file is currently printing or paused. If the state cannot confirm that file, the UI asks the user to inspect the printer before another start.

The changes leave printer firmware and the existing Anycubic G-code transformation and ACE gate mapping untouched. Unit coverage is added for Moonraker print-state parsing and filename matching, plus concurrent origin reservations.

## Verification

- `:app:testDebugUnitTest` passed.
- `:app:assembleDebug` could not finish: the native CMake target requires `app/src/main/jniImports/oneTBB/lib/arm64-v8a/libtbb.a`, which is absent from this checkout.
- The prebuilt-native APK path is unavailable for this branch because its cache targets app version `0.8.197` (build 232), while this source targets `0.8.232` (build 267). The repository correctly rejects that stale bundle.
