# OrkanSlicer UI 2.0 migration

## Design decisions

- Keep the printable bed as the largest workspace region.
- Keep filaments behind a compact color-and-count control. Use a bottom sheet on narrow windows
  and a side panel on wide windows.
- Use one set of navigation, menu, typography, spacing, and touch-target components.
- Keep the existing Slicer engine, profiles, renderer, file formats, and print behavior behind
  explicit UI/controller boundaries.
- Do not migrate the UI by rewriting renderer or native Slicer code.

## Current workspace

The workspace already uses Jetpack Compose and Material 3 for navigation, action menus, filament
palette, plate picker, and profile sheets. `BedFragment` still owns the workspace lifecycle, GL view,
menu container, overlay margins, and several Android view transitions. Menu actions call the
existing controller methods and engine bridge.

The current primary destinations are Project, Model, Camera/View, Edit, Tools, and Slice. Project
contains model import and 3MF export. Model contains object selection and arrangement operations in
a compact two-row grid. Edit contains geometry and mesh operations. Camera/View, Tools, and Slice
retain their existing function controllers.

## Migration sequence

1. **Navigation clarity and model actions — implemented.** Split Project from Model, give each a
   distinct destination, move model operations to their own two-row menu, and use unambiguous labels.
2. **Workspace boundaries — in progress.** Destination IDs, labels, icons, selected destination,
   and portrait tool-panel expansion now live in `ui/navigation`. `BedFragment` still coordinates
   transitions and Android view layout. Next, define a workspace controller for the active plate
   and mutually exclusive modal panels.
3. **Compose workspace shell.** Replace the remaining `LinearLayout`/`FrameLayout` layout-building
   in the workspace with a Compose shell. Embed the current `GLView` through a narrow Android View
   adapter and retain its lifecycle and rendering thread.
4. **Action/controller separation.** Move UI callbacks into focused workspace controllers and
   state holders. Keep profile, plate, filament, printer, and slicing domain operations in their
   existing owners until a stable interface is defined.
5. **Remove superseded UI paths.** Delete old menu/layout implementations only after each route has
   parity and device verification. Avoid duplicate navigation or action systems during final
   migration.
6. **Quality checks.** Add layout/navigation policy tests, verify small/large windows and large
   text, check modal/panel overlap, run the Android build, and exercise key paths on the Honor.

## Remaining UI risks

- `BedFragment` remains a large lifecycle and layout coordinator.
- The model action grid scrolls horizontally after its first six actions; discoverability and
  accessibility should be reviewed when testing with loaded models and larger text.
- The Android CLI accessibility dump did not connect on the Honor. Screenshot inspection succeeded.
- The emulator I/O timeout prevented a confirmed UI check there.
