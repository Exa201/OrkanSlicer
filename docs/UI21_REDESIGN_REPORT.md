# OrkanSlicer UI 2.1 – Arbeitsstand

## 0.8.462 Debug-APK

- Datei: `artifacts/OrkanSlicerNightly25_0.8.462_UI21_debug.apk`
- Version: 0.8.462, versionCode 497
- SHA-256: `0BC48D46C83C12156AFCA6CB9AFE300AF2601FE8327898A006BB82BB20E83033`
- Installiert und gestartet auf Android-16-Emulator (1080 × 2400, 420 dpi).
- Honor-Abnahme offen: das Honor-Gerät war gesperrt; die APK ließ sich daher nicht dort installieren/bedienen.

- Branch-Commit: `f390db86ea081480bcc420eb8bade6f5149bf232`.

## Umgesetzt

- Eigene Akzentfarben für die Bereiche 3D, Druck, Filament, Drucker und App in Navigation, Profilüberschriften, Einstellungen und Werkzeugkacheln.
- Adaptive Ansicht für „Geladene Objekte“ mit Aktionsflächen und lesbaren langen Namen.
- ACE-/Filamentauswahl in einem Raster mit bis zu vier Spalten.
- Kompakte lokalisierte Aktionsbeschriftung für „Fit volume“.
- Changelog/Startmeldung für 0.8.462 auf Deutsch, Englisch und Russisch.

## Verifikation

- Debug-Build: erfolgreich.
- Debug-Unit-Suite: erfolgreich.
- SliceTest-Unit-Suite: erfolgreich.
- Compose-Emulator-Regressionen: 4/4 bestanden (Objektdialog, Filamentraster, Einstellungen, Aktionskacheln).
- `git diff --check`: erfolgreich.
- Emulator-Screenshots: `docs/images/ui21/`.

## Weiter offen

Das ist die erste funktionsfähige UI-2.1-Iteration, noch keine vollständige Neugestaltung aller Unterseiten. Es fehlen insbesondere die vollständige visuelle Überarbeitung sämtlicher Einstellungseditoren und eine Honor-Abnahme bei 150 % Schriftgröße. Kein Merge und kein offizielles Release durchgeführt.


