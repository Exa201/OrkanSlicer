# UI 2.1 ColorFlow — Ausführungsstand (15.11.2026)

- **Branch:** `codex/orkanslicer-ui21-colorflow` im isolierten Worktree.
- **Build:** Debug-APK `OrkanSlicer_UI21_ColorFlow_0.8.415_450_423263fb_debug.apk`, Version 0.8.415 (Code 450), Quellcommit `423263fb04`. APK-Signatur gültig; SHA-256 `38CDD6BF6336747098B2EFB7CF28F2090D29E19F288E41CABF062A377320B848`. Installiert und gestartet auf Android-16-Emulator.
- **Umgesetzt:** ColorFlow-Farbpalette, farbcodierte Navigation, gruppierte Profilseiten, Mehrfarb-Icons und adaptive/gesuchte Objektliste; Renderer, WorkspaceShell und vorhandene Profil-/Projektzustände bleiben erhalten.
- **Sichtprüfung:** echte Emulator-Screenshots der Workspace-, Druck-, Filament-, Drucker- und App-Seite, des Spülvolumen-Dialogs und des Start-Changelogs unter `docs/screenshots/ui21/`. Der Changelog zeigt jetzt die tatsächliche APK-Version `0.8.415`; veralteter Inhalt für `0.8.396` wurde aktualisiert.
- **Unit-Tests:** `:app:testDebugUnitTest` — 705/705 bestanden, 0 fehlgeschlagen, 0 übersprungen.
- **Emulator-Instrumentierung:** Spülvolumen-Dialogtests 4/4 und Einstellungs-A11y-Test 1/1 bestanden auf Android 16 Emulator.
- **Changelog-Instrumentierung:** `ChangeLogComposeDeviceTest` 1/1 auf Android-16-Emulator bestanden. Der Gradle-Gesamtlauf bleibt wegen eines zweiten, auf dem verbundenen Xiaomi abgebrochenen Installationsversuchs mit `INSTALL_FAILED_USER_RESTRICTED` fehlerhaft; der Emulator-Test selbst ist erfolgreich.
- **Physisches Gerät:** Xiaomi per ADB sichtbar, aber Gradle-Installation des Testpakets wurde auf dem Gerät abgebrochen (`INSTALL_FAILED_USER_RESTRICTED`); daher keine Hardwareabnahme behauptet. Die Debug-APK wurde auf dem Emulator installiert und gestartet; im anschließenden Logcat-Suchfenster kein App-Crash gefunden.
- **Build/Prüfung:** `:app:assembleDebug` erfolgreich; `git diff --check` sauber. SDK-XML-Versionshinweis und nicht inkrementeller Eventbus-Annotationprozessor bleiben Buildhinweise.
- **Noch offen:** manuelle Honor-PTP-N49-Abnahme bei 150 % Schrift, visuelle Geräteabnahme des Objektlisten-Dialogs sowie kompletter Druck-/Slice-Workflow. TalkBack wurde nicht physisch bedient.
- **Nächster konkreter Schritt:** UI-Profilseiten mit realen Drucker-/Filamentwerten und Druckaktionen weiter gegen die Designvorgaben abnehmen; danach Objektlisten-Sichtprüfung mit einem Mehrteil-3MF. Honor-PTP-N49-Abnahme bei 150 % Schrift und physische TalkBack-Prüfung bleiben geräteabhängig offen.
- **Veröffentlichung:** kein offizieller Release und kein Merge. Die Debug-APK liegt lokal in `artifacts/`; GitHub-Upload ist in diesem Durchgang nicht erfolgt.
