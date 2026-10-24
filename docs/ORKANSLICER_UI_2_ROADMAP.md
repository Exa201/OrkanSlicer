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
2. **Workspace boundaries — in progress.** `WorkspaceController` now joins the selected
   destination, tool-panel expansion, active plate index, active special tool, and exclusive
   transient-panel coordinator. `BedFragment` delegates its menu and plate-picker selection state
   to it. The controller deliberately does not duplicate GL-owned model selection or the existing
   profile, dialog, and slicing domain state. `BedFragment` continues to own domain actions,
   plate/model lifecycle, renderer modes, and native lifecycle-bound overlays.
3. **Compose workspace shell — implemented for the main workspace layout.** `WorkspaceShell`
   places the current `GLView` through `RendererAdapter`, shares the workspace controls, and
   replaces the imperative portrait/landscape menu and workspace layout construction. The GL
   view is retained across Compose recompositions. Native snackbar, WebView, and renderer-specific
   overlays remain hosted by `BedFragment` where their Android lifecycle is still required.
4. **Action/controller separation.** Move UI callbacks into focused workspace controllers and
   state holders. Keep profile, plate, filament, printer, and slicing domain operations in their
   existing owners until a stable interface is defined.
5. **Remove superseded UI paths.** Delete old menu/layout implementations only after each route has
   parity and device verification. Avoid duplicate navigation or action systems during final
   migration.
6. **Quality checks.** Add layout/navigation policy tests, verify small/large windows and large
   text, check modal/panel overlap, run the Android build, and exercise key paths on the Honor.

## Remaining UI risks

- `BedFragment` remains a large lifecycle/domain coordinator and still owns native snackbar,
  WebView, and renderer-specific overlays; it is no longer the builder for the main workspace
  layout.
- Existing dialogs and special-tool routes have not all been migrated to shared Compose patterns.
- The workspace has a Compose shell and an incremental controller/overlay coordinator, but the
  controller does not yet own every requested state domain and modal dialogs are not all routed
  through it.
- The model action grid scrolls horizontally after its first six actions; discoverability and
  accessibility should be reviewed when testing with loaded models and larger text.
- The Android CLI accessibility dump did not connect on the Honor. Screenshot inspection and
  instrumentation tests succeeded.
- The emulator I/O timeout prevented a confirmed UI check there; Gradle's connected test runner
  logged an emulator property-fetch timeout but completed the Honor suite successfully.

## Workspace shell verification – 2026-10-16

`WorkspaceShell` now owns the responsive main layout and embeds the existing GL renderer with a
measured viewport through `RendererAdapter`. The host no longer manually assembles the full
workspace menu hierarchy in `BedFragment`. The printer WebView error state is rendered in a
Material 3 Compose panel, while the WebView and progress indicator retain their Android host.
Three layout-policy tests cover normal, expanded, and invalid/small viewport measurements.

On the Honor PTP-N49 (Android 16), the debug app installed and launched. A fresh screenshot showed
the global navigation, profile/filament/plate controls, the full bed viewport, and workspace
navigation visible without the prior GL surface covering the header. The 39 connected tests
passed; three private-fixture multicolor 3MF tests were skipped. Local unit suites each passed
526 tests. The emulator could not provide properties to Gradle and is not counted as verified.

This milestone does not complete the whole UI 2.0 migration. Legacy dialogs, special renderer
overlays, native global-navigation host plumbing, and the remaining `BedFragment` controller/state
responsibilities still need staged migration and regression verification.

## Etappe 8: Exklusive Workspace-Panels und lange Profilwahl (2026-10-16)

- `WorkspaceOverlayCoordinator` stellt einen einzigen aktiven transienten Workspace-Bereich bereit.
  Plattenauswahl, Filamentpalette, Profilwahl und Drucker-Webansicht schließen beim Öffnen den
  vorherigen Bereich; Zurück schließt zuerst den aktiven Bereich. Verspätete Dismiss-Callbacks
  dürfen den neu geöffneten Bereich nicht löschen.
- Die Compose-Plattenauswahl meldet Auswahl, Hinzufügen, Abbrechen und System-Back an denselben
  Koordinator. Native Bottom-Sheet-Dialoge der Filament- und Profilwahl melden ihre Schließung
  zurück.
- `SliceChoiceField` rendert kurze Auswahlmenüs weiterhin als Dropdown. Ab 12 Einträgen wird ein
  begrenzter Compose-Dialog mit Suchfeld und `LazyColumn` geöffnet. Das verhindert unnötiges
  Erzeugen sämtlicher Zeilen und erhält Quellindizes für die vorhandene Profilübernahme.
- Ein Honor-Gerätedurchgang fand beim ersten Versuch einen Compose-Intrinsic-Measurement-Absturz
  im langen Dropdown. Die lange Auswahl wurde daraufhin aus dem Dropdown in einen eigenen Dialog
  verschoben. Der reparierte Build zeigte auf dem Honor die filterbare Druckprofilliste inklusive
  Tastatur und scrollbarer Treffer; danach schlossen Zurück und Druckplattenauswahl wie erwartet.
  Im aktuellen Lauf blieb die Activity aktiv und der geprüfte Logcat enthielt keinen neuen Crash.
- Drei Koordinator-Tests prüfen Exklusivität, Zurück und verspätete Dismiss-Callbacks; zwei Tests
  prüfen Filterung und Erhalt der ursprünglichen Profilindizes. Beide lokalen Suiten bestanden mit
  531/531 Tests. Die Honor-Instrumentierung bestand mit 39 Tests; drei private 3MF-Tests waren
  mangels Fixture übersprungen.

### Weiter offen

Die Panelsteuerung deckt die zentralen Workspace-Flächen ab; sie ersetzt noch nicht alle Plattform-
Dialoge in `MainActivity`, Setup und Spezialwerkzeugen. Der `UnfoldMenu`-Rendererübergang und
renderergebundene Mal-/Mess-/Layer-Ansichten benötigen weiterhin ihre GL- und View-Lifecycle-Hosts.
`BedFragment` behält Modell-/Platten-Domainzustand. Eine vollständige Menü-zu-Menü-Abnahme über
alle Spezialwerkzeuge, kleine Fenster und 10–20 Platten ist noch nicht erfolgt. Der inkrementelle
Controller, die 130%-Navigation und die vier globalen Bereiche sind auf dem Honor weiter geprüft;
Details und die aktuelle APK-Prüfsumme stehen in `BUILD_AND_VERIFICATION.md`.
