# Orca calibration integration in OrkanSlicer

## Scope

The calibration screen exposes only modes that have a G-code generation path in the bundled native `libslic3r` core. The mode IDs are kept next to the UI presets and covered by a Kotlin unit test so they can be compared with `CalibMode` in `app/src/main/jni/libslic3r/calib.hpp`.

Currently exposed:

- Pressure Advance line and tower.
- Temperature tower, initialized from the selected filament's nozzle temperature.
- Speed and VFA towers.
- Retraction tower.

The user must have a loaded model for tower modes. The existing PA line test keeps its built-in small test model. Numeric input is validated before it is staged for slicing; calibration parameters remain one-shot in-memory slice parameters and do not silently overwrite a printer or filament profile.

The calibration menu uses a taller portrait sheet than the old 35% layout so that more choices are visible at once while the build plate remains partially visible.

## Deliberately not exposed

- Flow Rate: the enum exists in the native core, but no corresponding calibration G-code generation branch was found.
- Auto PA line: there is no complete UI/core path verified for this Android bridge.
- Input-shaping and cornering calibration: the core emits Klipper commands that change motion settings. These need a dedicated compatibility and user-confirmation review for Rinkhals before exposing them.
- Native calibration values are verified by code inspection and Kotlin validation tests. Physical calibration quality and generated G-code still require a printer-side hardware check before release.

## Resource impact

The UI creates a small preset list and a short-lived parameter dialog. It adds no polling, background service, persistent cache, or native dependency. The test model continues to load on demand.

## Modifier volume status

The bundled 3MF import/export and native slicing code already recognize negative volumes,
parameter modifiers, support enforcers, and support blockers. Avoid adding a parallel file
representation or rewriting that pipeline. Creating and editing modifier geometry from the
Android UI is not exposed: that requires native volume lifecycle APIs plus renderer,
transformation, and model-history integration. Treat that as a separate feature with targeted
native and UI tests rather than adding an incomplete menu action.
