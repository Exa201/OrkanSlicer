# OrcaSlicer Nightly mobile function audit

Compared against OrcaSlicer `main` at `f114ef7cc991a2069b00519ae45524a7ab0fdae2`.

## Available in the Android UI and JNI bridge

- STL/OBJ/3MF project import and multi-plate display
- Model duplication, deletion, placement, rotation, scaling and automatic orientation
- Cutting, connectors, text embossing and adaptive layer height
- Multi-selection and fill-bed operations
- Multi-color facet painting and filament assignment
- Orca print, filament and printer settings including scarf seams, tree supports,
  support ironing, prime tower and flushing controls
- Calibration modes and G-code preview
- Local Orca profile bundles and the Anycubic Kobra 3 V2 stock 0.4 mm profile
- German OrcaSlicer parameter database from the locked Nightly revision
- Native model snapshot undo/redo for mobile editing
- OctoPrint and Moonraker/Klipper upload and print start

## Partially available

- Multi-plate projects can be viewed and switched, but desktop plate management and
  per-plate arrangement controls are simplified for touch screens.
- Multi-material slicing and painting are available; desktop device/cloud workflows
  remain dependent on the printer host.
- CAD formats supported through OCCT are available where the Android importer exposes
  them; the desktop file-dialog integrations do not apply to Android.

## Nightly modules not yet portable

- Other Clipper2 geometry paths from the locked Nightly source set; multiline infill now uses Clipper2 1.5.2 on Android
- Texture-to-color processing
- New multi-nozzle utilities
- Assimp-based additional mesh importers
- SolveSpace/CAD editing integration

The remaining modules require Android-compatible native dependencies and JNI/UI
contracts; copying the desktop files alone would not produce working mobile features.

## Mobile UI decisions

- Keep OpenGL ES 3 because the current renderer, shaders and touch interaction are
  implemented and tested on it.
- Render on demand and preserve the EGL context across pauses to reduce battery use
  and avoid rebuilding model buffers after app switching.
- Use icons plus short labels in the main navigation. Settings tabs no longer repeat
  a small gear glyph in their text labels.
- Localize the plate indicator and primary workflow labels through Android resources.
