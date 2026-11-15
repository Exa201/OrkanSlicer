# UI 2.1 ColorFlow — Funktions-Mapping

Stand: Nightly 0.8.466, Branch `codex/ui-2.1-redesign`. Diese Tabelle erfasst den realen Migrationsstand; „offen“ heißt, dass eine vollständige visuelle und funktionale Abnahme noch fehlt.

| Bereich | Vorhandene Route / Quelle | Zielkomponente | Funktionsbindung | Status |
|---|---|---|---|---|
| 3D-Arbeitsplatz | `BedFragment` → `WorkspaceShell`, `RendererAdapter`, `PlateTabsCompose` | ColorFlow-Workspace mit unverändertem GL-Renderer | Plattenwahl, Menüs, Renderer-Gesten und Werkzeugaktionen bleiben in `WorkspaceController`/`BedFragment` | Teilmigration; Stil- und Responsivabnahme offen |
| Druckprofile | `PrintConfigFragment` → `ProfileSettingsComposeList` / `ComposeProfileCategoryRow` | Farblich gruppierte Profilbereiche mit kontrastreichen Titeln | Bestehender Profil-Controller, Filter, Werteditoren und Speichern | Gruppenoberfläche aktualisiert; Detaileditoren noch nicht vollständig vereinheitlicht |
| Filamentprofile | `FilamentConfigFragment` → `ProfileSettingsComposeList`, `OwnFilamentSlotBar` | Bereichsakzent Violett, Slot-/Materialchips und Profilbereiche | Bestehende Filamentauswahl, Slotzuordnung, Editor- und Speicherrouten | Teilmigration; Slot-Matrix 1/4/8/16/24 und 3MF-Fälle noch offen |
| Druckerprofile | `PrinterConfigFragment` → `ProfileSettingsComposeList` | Bereichsakzent Grün, Profil- und Verbindungsstatus klar trennen | Profilwerte und bestehender Verbindungscontroller | Profilgruppenoberfläche aktualisiert; Status-/Detailabnahme offen |
| App-Einstellungen | `SettingsFragment` → `SettingsContent` | `OrkanPageHeader`, `OrkanAccordion`, `OrkanSettingRow` | Theme, Sprache, Akzent, Textgröße, Renderer, Kamera, Profile, Speicher, Maker, Info und Reset behalten vorhandene Callbacks | Komponenten integriert; zentrale Accessibility- und Handlerregression bestanden |
| Arbeitsbereichnavigation | `WorkspaceShell` → `AdaptiveWorkspaceNavigation` | Einzeilige adaptive Labels und ColorFlow-Akzent | Selectable-Destination führt zum bisherigen Controller-Menüslot | 150-%-Labelgrenzen getestet; systemweite Varianten noch offen |
| Objektliste | `BedFragment` / `ModelObjectsDialog` | Adaptive Vollbildansicht, Suche in Objektnamen/Teilen/Materialien, virtuell gerenderte Einzelzeilen | Renderer-/Model-Controller und vorhandene Callbacks | Suche, Lazy-Rendering und semantische Checkbox-Gruppenauswahl ergänzt; gezielte Tests 3/3 auf API-36-Emulator; Honor wegen gesperrtem Gerät nicht bedienbar |
| Dialoge und Spezialansichten | Compose- und Android-Dialoge an den jeweiligen Features | Einheitliche Flächen, Kontrast, Inset-/IME-Verhalten | Vorhandene Handler und Lifecycle-Hosts | Noch nicht vollständig inventarisiert und migriert |

## Verifizierte Regressionen dieser Stufe

- Debug-Unit-Suite: 785/785.
- `SettingsAccessibilityDeviceTest`: 1/1 bei 320 dp und 150 % Text.
- `ProfileSettingsImeDeviceTest`: 12/12 Profil-/IME-Interaktionen.
- `AdaptiveWorkspaceNavigationDeviceTest`: 4/4; kompakte Navigationslabels bleiben bei 150 % innerhalb der 56-dp-Zeile.
- `ModelObjectsAdaptiveDialogDeviceTest`: 3/3 auf API-36-Emulator **und** Honor PTP-N49 Android 16; langer Name/Aktionsbedienung bei 150 %, Teilmaterial-Suche mit Scrollen/Leerzustand sowie Checkbox-Zustand und bestätigte Objekt-IDs.
- Echter Emulator-Screenshot der Arbeitsfläche bei 150 %: `docs/images/ui21/emulator-workspace-0465-150.png`; dabei waren Navigationslabels und Profilelemente sichtbar, das Profil wird bei der engen Breite erwartungsgemäß gekürzt.

## Nächste UI-Arbeit

1. Mit dem aktuellen Commit APK und echte Screenshots der fünf Hauptseiten erstellen.
2. Profileditoren anhand von 100/150 % Text und schmalen Hochformaten ausrichten; Slot- und Verbindungszustände dabei funktional testen.
3. Objektliste und Dialoginventar gegen `Codex_Prompt.md` abnehmen und fehlende Compose-Komponenten integrieren.
