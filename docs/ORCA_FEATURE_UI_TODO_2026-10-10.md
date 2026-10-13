# OrkanSlicer: priorisierte Funktions- und UI-Liste (10.10.2026)

Die Priorität richtet sich nach Drucksicherheit, Nutzen auf dem Telefon und Umsetzungsaufwand. Ein vorhandener Orca-Konfigurationsschlüssel bedeutet noch nicht, dass die Bedienung oder der Druckpfad vollständig geprüft sind.

Stand 0.8.208 (13.10.2026): Der 3MF-Projektimport mit temporären Profilen, die Zuordnung eigener Filamenttemperaturen, Drucker-bezogene Prozesseinstellungen und die mobile Plattenleiste sind inzwischen umgesetzt. Der STL-Import bietet Vorwarnung, echten Lesefortschritt, Abbruch und einen Reparaturhinweis. Der Sendeweg weist ein inzwischen ungültiges Slice-Ergebnis zurück, bevor ein Upload beginnt. Auch HTTP-200-Antworten mit eingebettetem Druckerfehler werden vor einer Erfolgsmeldung geprüft; ein synchroner Startfehler beendet die Fortschrittsanzeige. Die 3MF-Metadatenverarbeitung begrenzt Anzahl und entpackte Größe der relevanten Profile sowie die Plattenmetadaten; fehlende Basisprofile werden benannt. Die Abnahmen mit realem Zweifarbdruck, mehreren 3MF-Varianten und einer vollständigen Geräte-/Querformatprüfung bleiben offen. Ein Telefon war zuletzt über ADB nicht erreichbar; ein bestandener Gerätetest für 0.8.208 wird daher nicht behauptet.

## Orca-Funktionen

| Priorität | Aufgabe | Stand und Abnahme |
| --- | --- | --- |
| P0 | Multimaterial-Druckpfad einschließlich `flush_into_infill` und Slot-Zuordnung Ende-zu-Ende prüfen | `flush_into_infill` ist im nativen Kern vorhanden und jetzt unter **Druck → Füllung** im Standardmodus erreichbar. Mit realem Zweifarbdruck prüfen: korrekte Werkzeugwechsel, Reinigungsturm, vollständige letzte Schicht und eindeutige Slot-Zuordnung. Bei transparenten Außenwänden vor sichtbarer Mischfarbe warnen. |
| P0 | 3MF-Projektimport mit temporären Profilen, Objektfarben und Platten robust machen | Vorhandene Importpfade mit Orca-3MF aus mehreren Versionen testen; Druckerwechsel und eigene gespeicherte Profile dürfen nicht überschrieben werden. Jede Farbe und jedes Objekt muss nach erneutem Öffnen erhalten sein. |
| P0 | Druckerprofile und Verbindung validieren | Orca 2.4.2 legt Schwerpunkt auf Profilabhängigkeiten und Verbindung. Profilreferenzen bei Import/Update prüfen, fehlende Basisprofile benennen und Upload-Fehler verständlich anzeigen. |
| P1 | Kalibrierung als geführter Ablauf | Kalibriermodelle sind teilweise vorhanden. Temperatur, Fluss, Pressure Advance und maximalen Volumenstrom je Filament als Ergebnis speichern; G-Code und Profilwert gegentesten. |
| P1 | Farb-, Stütz- und Nahtmalen auf Touch verbessern | Grundlegende Malwerkzeuge sind vorhanden. Größere Pinselsteuerung, Rückgängig, Zoom beim Malen, klare Farblegende und Speicher-/3MF-Rundlauf testen. |
| P1 | Neue Brückenoptionen aus Orca 2.4.x abgleichen | Native Optionen und mobile Profile auf separate Brückenlinienbreite, Brückenrichtung und Vorschau prüfen. Nur aufnehmen, wenn der verwendete native Kern die Option wirklich unterstützt. |
| P2 | Filament je Druckbereich zuweisen | Der Prozesskatalog enthält die Orca-Schlüssel. Eine verständliche Vorschau für Außenwand, Innenwand, Füllung und Oberfläche mit Materialverträglichkeitsprüfung ergänzen. |
| P2 | Vorschau für große Modelle effizienter machen | Schichtbereiche, reduzierte Geometrie und Speichergrenzen weiterentwickeln; realistische Effekte aus Orca nur optional, wenn sie auf Mobilgeräten flüssig laufen. |

Quellen: [Orca Flush-Optionen](https://github.com/OrcaSlicer/OrcaSlicer/wiki/multimaterial_settings_flush_options), [Orca 2.4.2](https://github.com/OrcaSlicer/OrcaSlicer/wiki/release_2_4_2), [Orca 2.4.0 Beta](https://github.com/OrcaSlicer/OrcaSlicer/wiki/release_2_4_0_beta), [Orca-Kalibrierung](https://github.com/OrcaSlicer/OrcaSlicer/wiki/Calibration).

## Oberfläche und Bedienung

| Priorität | Aufgabe | Abnahme |
| --- | --- | --- |
| P0 | Startbild und Credits lesbar machen | Das Launcher-Motiv wurde kompakter neu gesetzt, die Credits haben Kontrast, Scrollen und größere Schrift. Auf dem Honor begrenzt die System-Startanimation die Symbolgröße weiterhin; für eine größere Markenansicht wäre ein eigener, kurzer In-App-Startbildschirm nötig. |
| P0 | Druckablauf auf eine klare Reihenfolge bringen | Modell → Drucker → Prozess/Filament → Slots → Slicen → Prüfen → Senden. Je Schritt nur eine primäre Aktion; kein zweiter Upload-Dialog. |
| P0 | Zuordnung von Modellfarben zu Slots sichtbar machen | Bereits vor dem Slicen direkt beim Objekt; Option „alles mit einem Slot“; veraltete Farben nach Filamentlöschung entfernen. |
| P1 | Druckeinstellungen schneller auffindbar machen | Suchfeld und Favoriten sind vorhanden, aber hinter der Such-Schaltfläche verborgen. Suchfeld sichtbarer machen, wenige Hauptgruppen und keine doppelten Werte; „Reinigung in der Füllung“ über Füllung und Suche erreichbar. |
| P1 | Portrait, Querformat und große Schrift systematisch prüfen | Keine abgeschnittenen Buttons/Schriften; geöffnete Dialoge und Bearbeitungszustand bleiben beim Drehen erhalten. Honor Magic 7 Pro und Xiaomi 13 prüfen. |
| P1 | Druckplatten und Objektliste vereinfachen | Platten als kompakte vertikale Zahlenleiste; Objektname, Farbe/Slot und Aktionen ohne verschachtelte Menüs; leere Platten nicht unbeabsichtigt erzeugen. |
| P2 | Vorschau und Status vereinheitlichen | Klare Legende für Werkzeug/Farbe, Druckzeit und Filament; Upload-Fortschritt und Fehler direkt beim Auftrag anzeigen. |

Auf dem Honor Magic 7 Pro wurden Startansicht, Druckmenü, Suche und Credits geprüft. Dabei fallen ein abgeschnittener langer Druckername, sehr hohe Filament- und Aktionsreihen sowie zwei Navigationsebenen auf; im Druckmenü bleibt dadurch wenig Platz für Einstellungen. Die Credits sind nach der Änderung lesbar. Eine vollständige visuelle Prüfung aller Dialoge und des Xiaomi steht noch aus.
