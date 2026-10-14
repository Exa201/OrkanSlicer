# OrkanSlicer UI: Bestandsaufnahme und Zielstruktur

Stand: 14.10.2026. Diese Zuordnung dient als Prüfkarte für weitere UI-Arbeit; sie ersetzt keine Gerätetests.

## Technische Bausteine

- Kotlin ist die Android-Hauptsprache. Die Navigation und einzelne ältere Dialoge nutzen Android-Views; die Arbeitsbereichsmenüs, Profile, Druckplattenauswahl und viele weitere Screens nutzen Jetpack Compose. Der Slicer-Kern und die 3D-Geometrie bleiben nativ. OpenGL rendert die Platte in `GLView`/`GLRenderer`.
- `BedFragment` hält das aktuelle Modell, die Plattenliste und den Renderer zusammen. `PlateTabs` zeigt die kompakte Plattenzahl und öffnet bei Bedarf die Auswahl. Nicht aktive Platten werden erst beim Anwählen aus 3MF geladen. `TemporaryProjectSettings` hält importierte 3MF-Werte projektlokal.
- `BeamComposeTheme` übersetzt das bestehende Hell-/Dunkel- und Akzentschema in Material 3. `OrkanUiTokens` liefert gemeinsame Abstände, Mindest-Touchfläche, Icon- und Menüschriftgrößen. `BedMenuActionsContent` zeichnet die wiederverwendbaren Menüaktionen.
- `UnfoldMenu` ist weiterhin der gemeinsame Host für spezialisierte Werkzeuge. Ein vollständiger Wechsel des UI-Frameworks würde den GL-Arbeitsbereich und bestehende Werkzeuge unnötig gefährden.

## Sichtbare Navigation und Funktionsorte

| Bereich | Bestehender Funktionsort | Funktion |
| --- | --- | --- |
| Modell/Datei | Vorschau → Modelle | Mehrfachimport, 3MF-Projektexport; Objektliste über Bearbeiten |
| Kamera | Vorschau → Ansicht | Perspektive, Kameraansichten und Steuerart |
| Objektbearbeitung | Vorschau → Bearbeiten | Objekte, Farbslots, Duplizieren, Plattenwechsel, Löschen, Anordnen, Ausrichten, Transformation, Malen und weitere Werkzeuge |
| Weitere Werkzeuge | Vorschau → Werkzeuge | Generator, Kalibriermodelle, Profilimport und -export |
| Slicing | Vorschau → Slicen | Slicen, Schichtvorschau, Analyse, Export und Senden |
| Profile | Druck, Filament, Drucker | Prozess-, Material- und Maschinenprofile sowie deren Detailwerte |
| App | App | Darstellung, Bedienung, Leistung und allgemeine Einstellungen |
| Druckplatten | Kompakter Button neben dem aktiven Drucker | Auswahl, Hinzufügen und Löschen; vereinfachte XY-Miniaturen, Kreis bei passender Displaygröße, sonst Raster |

Kontextmenüs am Modell und die Objektliste bleiben als zusätzliche Zugänge erhalten. Im Edit-Menü sind die häufigsten Aktionen direkt sichtbar; Löschen verlangt dort eine Bestätigung. Es werden keine Platzhalteraktionen hinzugefügt.

## Bereits vorhandene Schutzmechanismen

- Das automatische Reduzieren eines importierten Modells mit mehr als 500.000 Dreiecken fragt einmal pro Import um Zustimmung. Ablehnung belässt die Originalgeometrie; die Quelldatei wird nicht überschrieben. Manuell gestartete Vereinfachung hat einen eigenen Dialog.
- Das Schließen einer Platte mit Modellen fragt nach. Beim Schließen der einzigen Platte entsteht wieder eine leere Platte. Plattenwechsel nutzt denselben Zustandsweg für geladene und nachgeladene Modelle.
- Vorschaubilder werden nur bei geöffneter Plattenauswahl aktualisiert, aus Modellgrenzen statt aus zusätzlichen 3D-Szenen. Für diese kleinen Bilder reichen ungefähre transformierte Grenzen; Slice-Geometrie bleibt unverändert.

## Weitere Abnahme

- Menüzuordnung und Lesbarkeit bei großer Systemschrift, kleinem Telefon und Querformat vollständig durchgehen.
- Plattenauswahl mit 3–6 und 10–20 Platten am Gerät prüfen, besonders Löschen, Lazy Load und Rückkehr zum richtigen aktiven Modell.
- Importvereinfachung mit einem großen echten Modell für Zustimmung, Ablehnung und Abbruch testen; Speicherverhalten auf einem schwachen Telefon messen.
- Alle Modellwerkzeuge und deren Undo-Verhalten mit mehrteiligen 3MF-Projekten prüfen. Einen realen Mehrfarbdruck erst nach den separaten Drucksicherheitsprüfungen abnehmen.
