const e=`---\r
title: Fehlerbehebung\r
slug: troubleshooting\r
order: 90\r
description: Häufige Probleme und erste Lösungsversuche.\r
---\r
\r
# Fehlerbehebung\r
\r
Beginne hier, wenn ein Ergebnis falsch aussieht oder eine Funktion deaktiviert ist.\r
\r
## Ein Link zeigt „Seite nicht gefunden“\r
\r
Die Adresse kann einen Tippfehler enthalten oder auf eine nicht mehr vorhandene Seite zeigen. Mit **Kromacut öffnen** erreichst du das Werkzeug, mit **Zur Startseite** die Einstiegsseite und über **Dokumentation durchsuchen** die aktuelle Anleitung. Eine unbekannte Dokumentationsadresse wird nicht automatisch durch eine andere Anleitung ersetzt.\r
\r
## 3D-Modell erstellen aktualisiert die Vorschau nicht\r
\r
3D-Einstellungen werden erst beim Klicken auf **3D-Modell erstellen** übernommen. Ändere die gewünschten Einstellungen und erstelle das Modell anschließend erneut.\r
\r
## Wechselanweisungen sind deaktiviert\r
\r
Im manuellen Modus deaktivieren Bilder mit mehr als 64 Farben die Wechselanweisungen. Kehre zu **2D** zurück und reduziere das Bild unter **Farbquantisierung** auf höchstens 64 Farben. Flat Paint hat absichtlich keine manuelle Wechselfolge, weil jede Druckschicht mehrere Filamente enthalten kann.\r
\r
## Das Modell ist zu hoch\r
\r
Probiere in dieser Reihenfolge:\r
\r
1. Setze in Auto-paint **Maximale Höhe** niedriger und achte auf gestauchte Übergangszonen.\r
2. Prüfe im manuellen Modus die Anzahl der Bildfarben. Viele Farben erzeugen viele gestapelte Farbbereiche, wodurch das Modell von selbst höher wird.\r
3. Reduziere Farben in **2D**, wenn nicht jede Farbe als eigener gedruckter Bereich benötigt wird.\r
4. Verringere im manuellen Modus eine oder mehrere Farbhöhen.\r
5. Prüfe, ob **Schichthöhe** und **Höhe der ersten Schicht** den tatsächlichen Slicer-Einstellungen entsprechen.\r
\r
## Das Modell ist in X oder Y zu groß\r
\r
Verringere **Pixelgröße (XY)**, um das Modell zu verkleinern. Schneide das Bild zuerst zu, wenn es ungenutzte Ränder oder Hintergrund enthält.\r
\r
## Das Bild hat Farbpunkte oder winzige Inseln\r
\r
Nutze nach der Farbreduktion **Raster glätten**. Bleiben zu viele einzelne Pixel übrig, versuche weniger Farben oder einen anderen Quantisierungsalgorithmus.\r
\r
## Auto-paint wirkt ungenau\r
\r
Häufige Ursachen:\r
\r
- Die Deckdistanzen der Filamente sind geschätzt statt kalibriert.\r
- Das Filamentset deckt die Bildfarben nicht gut ab.\r
- **Maximale Höhe** staucht die Übergangszonen zu stark.\r
- Die Optimierung benötigt aktivierte **Verbesserte Farbabstimmung**.\r
- Das wichtige Motiv liegt in der Mitte oder an den Rändern, aber **Bereichspriorität** steht auf **Gleichmäßig**.\r
\r
Kalibriere deine Filamente und prüfe **Ergebniszuverlässigkeit** auf Hinweise.\r
\r
Prüfe die Zeile **Erscheinungsmodell** gesondert. Ein hoher Gesamtwert garantiert keine physische Genauigkeit. **Nur geschätzt** oder null aktive Matrixrezepte bedeutet, dass das aktuelle Ergebnis keine anwendbaren Matrixmessungen nutzt. Das Speichern eines kalibrierten Profils macht dessen Daten nicht mit jeder Schichthöhe oder jedem geänderten Filamentset kompatibel. Siehe [Kalibrierabläufe](calibration-workflows).\r
\r
## Ein Schalter hat einen anderen ausgeschaltet\r
\r
**Farbtrennung erhalten** und **Höhen-Dithering** schließen sich weiterhin aus, da sie Quellfarben unterschiedlich auf druckbare Höhen abbilden. **Glatte Vernetzung** und **Flat Paint** lassen sich gemeinsam verwenden; baue nach Änderungen neu. Siehe [Flat Paint](flat-paint) und [Auto-paint](auto-paint).\r
\r
## Die Farbtrennung findet kein Ergebnis\r
\r
Die **Grenze für eindeutige Zuordnung** ist eine feste Grenze des vorhergesagten Farbfehlers. Ist **Eindeutige Zuordnung für jede Farbe verlangen** aktiviert, verwirft bereits eine nicht zugeordnete Farbe das Ergebnis. Erwäge eine kleinere 2D-Palette, ein zusätzliches hilfreiches Filament, mehr Übergangshöhe oder Wiederholungen oder eine lockerere Grenze. Deaktiviere die strikte Zuordnung nur, wenn das Weglassen eigenständiger Farben und Zusammenführen ihrer Bereiche akzeptabel ist. Eine zuvor erstellte Vorschau kann nach einer Verwerfung sichtbar bleiben; sie ist kein erfolgreiches Modell der verworfenen Einstellungen.\r
\r
## Meine Anpassungen verschwinden in 3D oder im Download\r
\r
Klicke bei den Bildanpassungen auf **Anwenden**, um das aktuelle Aussehen vor Quantisierung, Modellerstellung oder Download ins Quellbild zu übernehmen. Anpassungsvorschau und Quelle sind getrennt. Siehe [Bildanpassungen](image-adjustments).\r
\r
## Das Löschen eines Farbfelds hat dessen Pixel nicht entfernt\r
\r
**Löschen** entfernt eine Palettenoption und ordnet das Bild den verbleibenden Farben zu. Es ist kein Radierer. Verwende für eine Aussparung den Radierer oder setze den Alphawert des Farbfelds auf null. Quantisierung kann teiltransparente Pixel undurchsichtig machen; prüfe die Silhouette danach erneut.\r
\r
## Eine Auto-paint-Diagnoseaufzeichnung erstellen\r
\r
Aktiviere für eine genauere Untersuchung in der Desktop-App unter **Einstellungen** die Option **Auto-paint-Diagnosedaten aufzeichnen**, führe eine neue Auto-paint-Berechnung aus und suche mit **Ordner öffnen** die entstandene \`.jsonl\`-Aufzeichnung. Aktiviere die Aufzeichnung vor Beginn der Berechnung. Das Erstellen eines Netzes aus einem bereits berechneten Ergebnis zeichnet die frühere Berechnung nicht nachträglich auf. Lies vor dem Teilen einer Aufzeichnung [Auto-paint-Diagnose auf dem Desktop](settings-and-controls#desktop-auto-paint-diagnostics).\r
\r
## Die 3D-Erstellung ist langsam\r
\r
Große Bilder, viele Farben, viele Schichten und glatte Vernetzung verlängern die Erstellung. Versuche:\r
\r
- Das Bild zuzuschneiden.\r
- Die Anzahl der Farben zu verringern.\r
- **Glatte Vernetzung** auszuschalten.\r
- Die Bildauflösung mit **Bild skalieren** zu verringern. Eine kleinere Pixelgröße macht dasselbe Netz nur kleiner, nicht einfacher.\r
- Die Auto-paint-Optionen zu vereinfachen.\r
\r
## Die exportierte Datei öffnet sich mit unerwarteten Farben\r
\r
Prüfe bei 3MF die Material- oder Filamentzuweisungen im Slicer. Kromacut erhält Farbinformationen nach Möglichkeit, Slicer können Farben aber unterschiedlich Extrudern zuordnen.\r
\r
STL enthält keine Filamentfarbzuweisungen. Verwende die **Druckanweisungen** für Filamentwechsel.\r
\r
Die Auto-paint-Ansicht Simuliert zeigt geschätzte Mischfarben; Slicer zeigen normalerweise physische Filamentfarben. Schalte Kromacut zur Zuweisungsprüfung auf **Physisch** und kontrolliere dann die tatsächliche Spulenzuordnung. Keine der Ansichten beweist die endgültige gedruckte Farbe.\r
\r
## Zuschnitt oder Bildbearbeitung gingen zu weit\r
\r
Nutze **Rückgängig**. Mit Wiederholen kannst du zu weit zurückgenommene Schritte wiederherstellen.\r
\r
Weiter: [Häufige Fragen](faq).\r
`;export{e as default};
