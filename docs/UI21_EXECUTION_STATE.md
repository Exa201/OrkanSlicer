# UI 2.1 ColorFlow — Ausführungsstand (15.11.2026)

- **Branch:** `codex/orkanslicer-ui21-colorflow` im isolierten Worktree.
- **Build:** Debug-APK `OrkanSlicer_UI21_ColorFlow_0.8.415_450_debug.apk`, Version 0.8.415 (Code 450). APK-Signatur gültig; SHA-256 `F7EB2EDF344C083B9CBC021A125F4B76B1925D7C401871C3AEC23FF493AED9A1`.
- **Umgesetzt:** ColorFlow-Farbpalette, farbcodierte Navigation, gruppierte Profilseiten, Mehrfarb-Icons und adaptive/gesuchte Objektliste; Renderer, WorkspaceShell und vorhandene Profil-/Projektzustände bleiben erhalten.
- **Sichtprüfung:** echte Emulator-Screenshots der Workspace-, Druck-, Filament-, Drucker- und App-Seite sowie des Spülvolumen-Dialogs unter `docs/screenshots/ui21/`. Im Dialog wurde dunkle Eingabeschrift gefunden und behoben; Werte und Suchsymbol sind jetzt kontrastreich.
- **Unit-Tests:** `:app:testDebugUnitTest` — 705/705 bestanden, 0 fehlgeschlagen, 0 übersprungen.
- **Emulator-Instrumentierung:** Spülvolumen-Dialogtests 4/4 und Einstellungs-A11y-Test 1/1 bestanden auf Android 16 Emulator.
- **Physisches Gerät:** Xiaomi per ADB sichtbar, aber Gradle-Installation des Testpakets wurde auf dem Gerät abgebrochen (`INSTALL_FAILED_USER_RESTRICTED`); daher keine Hardwareabnahme behauptet. Die Debug-APK wurde auf dem Emulator installiert und gestartet; im anschließenden Logcat-Suchfenster kein App-Crash gefunden.
- **Build/Prüfung:** `:app:assembleDebug` erfolgreich; `git diff --check` sauber. SDK-XML-Versionshinweis und nicht inkrementeller Eventbus-Annotationprozessor bleiben Buildhinweise.
- **Noch offen:** manuelle Honor-PTP-N49-Abnahme bei 150 % Schrift, visuelle Geräteabnahme des Objektlisten-Dialogs sowie kompletter Druck-/Slice-Workflow. TalkBack wurde nicht physisch bedient.
- **Nächster konkreter Schritt:** Debug-APK auf Honor/Xiaomi installieren und UI 2.1 mit großen Schriften und einem Mehrteil-3MF prüfen; anschließend verbleibende UI-Lücken gezielt beheben.
- **Veröffentlichung:** kein offizieller Release und kein Merge. Die Debug-APK liegt lokal in `artifacts/`; GitHub-Upload ist in diesem Durchgang nicht erfolgt.
