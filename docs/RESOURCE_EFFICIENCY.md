# Resource efficiency and device compatibility

## Changes in this development branch

- AI-generator photos are inspected for dimensions before decoding. `ImageDecodePolicy`
  selects a power-of-two sample size so the longest decoded side is at most 1024 px,
  matching the upload preparation limit. Content-provider streams are closed after
  each of the bounds and image passes. Invalid or unreadable photos now show the
  existing generation error instead of passing a null bitmap onward.
- `IOUtils.IO_POOL` now caps concurrent shared background work at two or three workers
  (depending on reported CPU count) and uses slightly lower-priority named threads.
  This avoids the unbounded thread creation of a cached pool when file imports,
  generators, slicing, and network work overlap.
- Application startup work now uses two workers rather than creating a cached worker
  for every eligible boot task.
- Existing low-RAM rendering controls remain in place: reduced default render scale,
  simpler inactive-plate previews, smaller layer blocks, and capped active preview
  triangles. The source model used for slicing is unchanged by the preview triangle cap.

## Compatibility boundaries

- The app still requires Android API 23 or newer.
- OpenGL ES 3 remains a required device feature because the current renderer and
  shaders directly use GLES 3. Marking it optional would let unsupported devices
  install the app without providing a working viewport; a GLES 2 renderer is a
  separate compatibility project.
- `largeHeap` remains enabled for now. Removing it without measuring native slicing
  and large-model imports across representative devices could increase out-of-memory
  failures. It is not a memory optimization by itself and should be reconsidered only
  with real-device memory data.
- The bounded worker count limits simultaneous work but does not impose a bounded task
  queue. A bounded queue needs caller-facing rejection/progress handling at the many
  existing submission sites before it can be introduced safely.

## Verification

- `:app:testDebugUnitTest`: passed, including image sample-size policy tests.
- `:app:testSliceTestUnitTest`: passed.
- `git diff --check`: passed.
- `:app:assembleDebug` passed. The resulting APK was installed and launched on the Android
  API 36 emulator; a local calibration slice reached the G-code preview.
- No on-device CPU, memory, battery, or low-end GPU benchmark has been performed.
