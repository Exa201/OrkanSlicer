const e=`---\r
title: Auto-paint-Steuerung\r
slug: auto-paint\r
order: 62\r
description: Filamentdaten, druckbare Details, Farbabstimmung, Höhenbegrenzungen und Zuverlässigkeit.\r
---\r
\r
# Auto-paint-Steuerung\r
\r
Auto-paint sagt das Aussehen dünner, übereinanderliegender Filamentschichten voraus und wählt druckbare Höhen für das vorbereitete Bild. Mehrere sichtbare Farben können durch unterschiedliche Dicken desselben physischen Filaments entstehen. Zwanzig Bildfarben benötigen daher nicht zwangsläufig zwanzig Spulen.\r
\r
Lege [physische Größe, Schichthöhen und effektive Linienbreite](3d-mode#3d-print-settings) fest, füge tatsächlich verfügbare Filamente hinzu, warte auf die Berechnung und klicke auf **3D-Modell erstellen**. Eingaben lösen automatisch eine Neuberechnung aus; die angezeigte Geometrie aktualisiert sich erst beim Erstellen.\r
\r
## Filamenteingaben\r
\r
| Steuerelement | Eingabe oder Aktion | Wirkung |\r
| ---------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------ |\r
| **Filament hinzufügen** | Eine neue neutralgraue Zeile anlegen. | Fügt ein verfügbares Material hinzu, keinen physischen Druckerschacht. |\r
| **Farbfeld / Hex** | Die deckende Filamentfarbe auswählen oder einen Hex-Wert eingeben. | Ändert Mischfarben und exportierte Materialfarbe. Gib die tatsächliche Spulenfarbe an, keine gewünschte Ausgabefarbe. |\r
| **Name** | Die Spule sinnvoll benennen. | Identifiziert sie. Ein leerer Name wird durch eine automatische farbbasierte Bezeichnung ersetzt. |\r
| **HD** | Deckdistanz unter Auflicht, 0,01 bis 2 mm. | Kürzere HD verdeckt untere Farben schneller. Längere HD benötigt mehr Dicke und lässt bei gleicher Dicke mehr durch. |\r
| **Aus TD umrechnen** | Herkömmliche Transmission Distance für Lithophanien/Hinterleuchtung eingeben und **Umrechnen** drücken. | Rechnet ungefähr TD × 0,1 um, rundet auf 0,01 mm und begrenzt auf den HD-Bereich. Einen HD-Wert nicht erneut umrechnen. |\r
| **Zauberstab** | HD aus der Farbe schätzen. | Ein Ausgangswert, keine Messung. |\r
| **Statusanzeige** | **Schätzung** oder eine kalibrierte Zuverlässigkeitsangabe prüfen; per Mauszeiger RGB-Kanalwerte anzeigen. | Beschreibt die HD-Datenlage dieser Zeile, nicht die Genauigkeit des gesamten Bildes. |\r
| **Papierkorb** | Die Zeile entfernen. | Das Filament steht nicht mehr zur Verfügung. Gespeicherte Profile bleiben bis zum Speichern unverändert. |\r
| **Kalibrieren** | Deckdistanz, Palette Proof oder Stack Matrix öffnen. | Physische Daten gemäß [Kalibrierabläufe](calibration-workflows) erfassen. |\r
\r
Das Übernehmen eines HD-Werts, Umrechnen von TD oder Verwenden des Zauberstabs löscht die gespeicherte HD-Kalibrierung dieser Zeile. Wird die Farbe eines kalibrierten Filaments geändert, wird dessen Kalibrierung inaktiv und eine farbbasierte Schätzung verwendet. Die Rückkehr zur gemessenen Farbe kann erhaltene Kalibrierung wieder aktivieren, sofern sie zwischenzeitlich nicht gelöscht oder ersetzt wurde. Eine Namensänderung ist keine Messung.\r
\r
Gemessenes Kanalverhalten beeinflusst sowohl die vorhergesagte Farbe als auch die Übergangsdicke. Gehe bei verändertem Material oder Prozess nicht davon aus, dass die alte Vorschau weiterhin bestätigt ist.\r
\r
## Filamentprofile\r
\r
Das Laden eines gespeicherten Profils ersetzt das Arbeits-Filamentset. **Ungespeicherte Änderungen** bedeutet, dass das aktuelle Set vom ausgewählten Profil abweicht.\r
\r
| Aktion | Ergebnis |\r
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |\r
| **Änderungen im aktuellen Profil speichern** | Überschreibt das bearbeitbare Profil mit dem aktuellen Set. Deaktiviert ohne Änderungen, ohne ausgewähltes Profil oder bei einer Vorlage. |\r
| **Als neues Profil speichern** | Einen nichtleeren Namen eingeben, um separat zu speichern; gilt auch für geänderte Vorlagen. |\r
| **Ausgewähltes Profil umbenennen** | Ändert die Bezeichnung, nicht die Filamente. Bei Vorlagen oder ohne ausgewähltes Profil nicht verfügbar. |\r
| **Profil aus Datei importieren** | Natives \`.kfil\`, älteres \`.kapp\`, JSON oder unterstützte HueForge-Spulen-CSV/TSV einlesen. |\r
| **Aktuelle Filamente als .kfil-Datei exportieren** | Exportiert das aktuelle Set. Desktop öffnet Speichern unter, Browser nutzt seinen Downloadablauf. Abbrechen ändert keine Kalibrierung. |\r
| **Ausgewähltes Profil löschen** | Entfernt das gespeicherte Profil. Bei Bedarf zuerst eine Sicherung exportieren. Vorlagen können nicht gelöscht werden. |\r
\r
Erscheinungsdaten gehören zu einer exakten Filamentkonfiguration. Solange ein ausgewähltes Profil ungespeicherte Änderungen hat, werden seine gespeicherten Erscheinungsdaten nicht an Auto-paint übergeben. Der Export dieser Änderungen erstellt ein separat benanntes Profil ohne inkompatible Erscheinungsdaten des alten Sets. Aktive HD-Kalibrierung je Filament ist von Matrix- und Proof-Daten auf Profilebene getrennt. Siehe [Profilkompatibilität und Importverhalten](settings-and-controls#filament-profile-files).\r
\r
### Vorlagen\r
\r
Herstellervorlagen liefern beworbene Farben, Namen, Marken und geschätzte HD-Werte. Sie sind schreibgeschützt. Entferne Farben, die du nicht besitzt, kalibriere und wähle **Als neues Profil speichern**. Hersteller-Referenzfarben garantieren kein bestimmtes Verhalten einer Charge oder unter bestimmten Betrachtungsbedingungen. Vorlagen sind inoffiziell und keine Empfehlung durch den Hersteller.\r
\r
## Normale Farbabstimmung\r
\r
Ist **Verbesserte Farbabstimmung ausgeschaltet**, sortiert Kromacut die Filamente nach Helligkeit von dunkel nach hell und berechnet ihre Übergänge. Die Reihenfolge des Hinzufügens wird nicht übernommen. Auch die Zuordnung von Bild zu Höhe verwendet die Helligkeit: Sie normiert diese zwischen dem dunkelsten und hellsten nichttransparenten Bildpixel, legt diesen Bereich zwischen Grundlagenoberfläche und Stapeloberseite und rastet dann auf druckbare Schichten ein. Zwei unterschiedliche Farbtöne gleicher Helligkeit können daher unabhängig von ihrem Farbton dieselbe Höhe erhalten. Wiederholungen, Farbtrennung, Höhen-Dithering und Optimierungseinstellungen sind inaktiv.\r
\r
Nutze diese einfachere Ausgangsmethode für helligkeitsgeführte Reliefs. Die verbesserte Abstimmung berücksichtigt die Bildfarbe sowohl bei der Materialreihenfolge als auch bei der Zuordnung druckbarer Farben und ist deshalb geeignet, wenn Farbtonunterschiede wichtig sind.\r
\r
## Maximale Höhe\r
\r
Leere **Maximale Höhe** oder drücke **Automatisch**, um die berechnete, am Schichtraster ausgerichtete Stapelhöhe zu verwenden. Das Feld erlaubt 0,5 bis 20 mm. Die Grenze ist ein Höchstwert, kein Zielwert; das Ergebnis kann kürzer sein.\r
\r
Eine Grenze zwischen gültigen Schichtgrenzen wird abgerundet. Sind normale Übergänge zu hoch, staucht Kromacut sie und zeigt zum Vergleich die automatische Höhe.\r
\r
![Automatische und höhenbegrenzte Stapel behalten die deckende Grundlage, während obere Übergänge kürzer werden.](15_transition_height.svg)\r
\r
_Schematische Darstellung. Farbbänder kennzeichnen Materialabschnitte, nicht das vorhergesagte Mischfarbenaussehen._\r
\r
Die Grundlage muss weiterhin etwa 95 % Deckkraft in jedem modellierten RGB-Kanal erreichen. Eine zu niedrige Grenze verwirft den Stapel, statt einen durchscheinenden Untergrund als deckend zu behandeln. Erhöhe die Grenze oder verwende ein für die Grundlage geeignetes Filament mit kürzerer HD.\r
\r
Stauchung kann nützliche Zwischenfarben entfernen. Sie skaliert nicht einfach ein ansonsten identisches Bild gleichmäßig. Bei **Flat Paint** ist der transparente Träger zusätzliche Geometrie, und die Anordnung der ersten Schicht unterscheidet sich vom Relief. Prüfe die vollständigen **Modell**-Abmessungen und die Dicke im Slicer; Maximale Höhe ist nicht die Plattendicke einschließlich Träger.\r
\r
## Druckbare Details\r
\r
Stelle [**Effektive Linienbreite** unter **3D-Druckeinstellungen**](3d-mode#effective-line-width) auf die geplante Extrusionsbreite des Slicers, nicht auf Düsendurchmesser oder Pixelgröße. Auto-paints Breitenwarnungsvorschau und optionale Bereinigung isolierter Punkte nutzen denselben Wert; ihre Steuerelemente bleiben hier.\r
\r
Stell dir einen **zwei Pixel breiten violetten Streifen** auf blauem Hintergrund vor. Bei **0,10 mm/Pixel** ist er **0,20 mm breit**. Ist die geplante Extrusionslinie **0,40 mm breit**, ist der Streifen schmaler als die Linie. Kromacut kann ihn als **gefährdet** markieren, erhält ihn aber auch bei aktivierter Bereinigung. Ein schmaler sichtbarer Bildbereich kann Teil einer viel breiteren darunterliegenden Materialschicht sein; Slicer-Bahnen können Details erhalten, die diese reine Bildprüfung markiert.\r
\r
![Ein zwei Pixel breiter violetter Streifen ist schmaler als die geplante Extrusionsbreite. Gelborange ist eine Breitenwarnung, keine Filamentfarbe. Die Bereinigung isolierter Punkte erhält den Streifen und ersetzt nur einen winzigen eingeschlossenen rosa Punkt durch Blau.](13_printable_detail.svg)\r
\r
_Dies ist eine Breitenschätzung, keine exakte Slicer-Bahn. Die Farbbalken vergleichen Breiten; die Quadrate zeigen das Beispielbild._\r
\r
**Isolierte Farbpunkte auslassen** bietet zwei Möglichkeiten:\r
\r
- **Aus:** Alle Quellpixel bleiben in der Auto-paint-Eingabe erhalten, einschließlich jedes hervorgehobenen Bereichs.\r
- **Ein:** Nur Farben, die ausschließlich in winzigen, kompakten, eingeschlossenen Punkten vorkommen, werden vor Abstimmung und Modellerzeugung durch die umgebende breitere Farbe ersetzt. Die gesamte Ausdehnung eines Punkts muss kleiner als die effektive Linienbreite sein. Es muss genau eine eindeutige umgebende Farbe mit einem breiteren Bereich geben. Kommt dieselbe Quellfarbe irgendwo im Bild auch in einer Linie oder einem größeren Bereich vor, bleibt sie überall erhalten.\r
\r
Dünne Linien, diagonale Verbindungen, an größere Bereiche angeschlossene Verzweigungen, Details am Bildrand sowie Punkte neben Transparenz oder mehreren Farben bleiben erhalten. Die Bereinigung macht niemals einen Pixel zum Loch und bearbeitet dein ursprüngliches 2D-Bild nicht. Sie ist bewusst vorsichtig und kann unerwünschte Punkte übriglassen; für umfassendere Bildbearbeitung nutze die [2D-Bereinigung](dedithering-cleanup).\r
\r
Mit **Vorschau öffnen** kannst du Folgendes prüfen:\r
\r
- **Gefährdet:** Gelborange markiert schmale Quellfarbbereiche neben breiteren Farben; Rosa markiert schmale Bereiche ohne breiteren Nachbarn. Andere Pixel werden abgedunkelt. Dies sind Warnungen, keine Vorhersage, dass ein Detail nicht gedruckt werden kann.\r
- **Ergebnis:** Die Pixel, die Auto-paint nach der optionalen Bereinigung erhält. Keine Slicer-Vorschau und keine Druckbarkeitsgarantie.\r
- **Markiert / geeignet / ausgelassen:** Der Anteil mit Breitenwarnung, die Pixel, die die Regeln für isolierte Punkte erfüllen, und die tatsächlich ersetzte Anzahl. **Markierte Pixel erhalten** zählt ausdrücklich Warnungen, die das Bild nicht verändern.\r
\r
Werden geeignete Punktfarben ausgelassen, zählt Auto-paint die Zielfarben und plant den Stapel anhand des bereinigten Bildes. Das Entfernen einer ganzen Zielfarbe kann die Zuordnung anderswo verändern; prüfe daher das neu erstellte Ergebnis. Ein Warnungsanteil größer null bei **0 ausgelassenen Pixeln** bedeutet, dass die Bereinigung sämtliche Quelldetails erhalten hat.\r
\r
Lass Auto-paint nach dem Umschalten fertig rechnen und klicke erneut auf **3D-Modell erstellen**. Die Analyse misst zusammenhängende Quellfarbbereiche, keine physischen Materialschichten, Wände, Füllungen oder Extrusion mit variabler Breite. Prüfe stets die geslicten Bahnen. Gehen dort tatsächlich Details verloren, vergrößere das Modell in XY, verbreitere sie in 2D oder wähle eine feinere Extrusionsbreite, die Drucker und Slicer unterstützen.\r
\r
## Verbesserte Farbabstimmung\r
\r
Die verbesserte Abstimmung sucht Materialfolgen für die aktuelle 2D-Palette. Filamente ohne nützlichen Beitrag zur Abdeckung können entfallen. Acht verfügbare Spulen müssen nicht acht Abschnitte erzeugen.\r
\r
Die Optimierung reduziert die vorbereitete Palette nicht heimlich erneut. Mehr Quellfarben benötigen mehr Arbeit. Bereite das Bild unter [Farben reduzieren](reducing-colors) vor und beurteile sein erreichbares Aussehen anschließend in 3D.\r
\r
Das Ausschalten der verbesserten Abstimmung deaktiviert auch Farbtrennung und Höhen-Dithering. Neue Berechnungen brechen ältere ab; Fortschrittsangaben sind Näherungen. Eine fehlgeschlagene Berechnung kann nicht auf einen unzusammenhängenden manuellen Stapel zurückfallen. Ein älteres erstelltes Modell kann sichtbar bleiben, bis ein gültiges neues Ergebnis erstellt wird.\r
\r
### Gesamtlimit für Wiederholungen\r
\r
Wähle **Aus** oder bis zu **2, 4, 6, 8 oder 12 zusätzliche Vorkommen** für den gesamten Stapel. Dies ist weder eine Erlaubnis je Filament noch eine genaue Wechselanzahl. Schwarz → Gelb → Schwarz verwendet ein zusätzliches Vorkommen von Schwarz. Die Rückkehr zu einem Material über einem neuen Untergrund eröffnet einen weiteren möglichen Mischpfad.\r
\r
![Ein gemeinsames Wiederholungsbudget und eigenständige Zielzuordnungen im Vergleich zum Zusammenführen verworfener Farben.](14_repeats_separation.svg)\r
\r
_Schematische Folgen und Zuordnungen, keine Materialvorhersagen._\r
\r
Mehr Wiederholungen erlauben eine breitere Suche und möglicherweise mehr Materialwechsel beim Drucken. Das Budget ist eine Obergrenze. Unnötige Abschnitte können entfallen.\r
\r
### Farbtrennung erhalten\r
\r
Normale Abstimmung kann unterschiedliche Bildfarben derselben Ausgabe zuordnen. Aktiviere **Farbtrennung erhalten**, wenn diese Unterschiede wichtig sind, etwa bei Schrift vor ihrem Hintergrund oder benachbarten Gesichtstonwerten.\r
\r
**Grenze für eindeutige Zuordnung (ΔE)** ist der feste maximale Farbunterschied, innerhalb dessen eine Bildfarbe eine eigene druckbare Ausgabe erhalten darf. Bereich: 1 bis 100; Standard: 6. Kleinere Werte verlangen genauere Entsprechungen und sind schwerer einzuhalten. Höhere Werte erlauben mehr Fehler, erzeugen aber keine besseren physischen Filamente.\r
\r
**Eindeutige Zuordnung für jede Farbe verlangen** ist standardmäßig aktiviert. Unvollständige Zuordnungen schlagen fehl. Deaktiviere die Option für eine **Teilpalette**: Nicht zugeordnete Farben verlieren ihre eigene Ausgabe und werden mit erhaltenen Zuordnungen zusammengeführt. Das Bild bleibt gefüllt, verliert aber Unterschiede. Ist keine Farbe geeignet, schlägt auch der Teilmodus fehl, weil kein Ziel zum Zusammenführen übrigbleibt.\r
\r
Die Optimierung maximiert zuerst erhaltene Farben und die Abdeckung des Quellbilds. Danach bevorzugt sie weniger zusätzliche Vorkommen, weniger Materialabschnitte und weniger physische Schichten, erst anschließend geringere Fehler innerhalb der Grenze. Wiederholungsbudgets werden schrittweise untersucht; die Suche kann enden, sobald alle Farben erhalten sind. Eine abschließende Löschprüfung entfernt einzelne Abschnitte, die diese Prioritäten nicht verbessern.\r
\r
Bei einem Fehlschlag im strikten Modus erwäge weniger 2D-Farben, mehr Höhe oder Wiederholungen, ein weiteres geeignetes Filament, eine größere ΔE-Grenze oder teilweises Zusammenführen. Wähle den tatsächlich gewünschten Kompromiss, statt eine Grenze nur zum Ausblenden der Fehlermeldung zu erhöhen.\r
\r
Farbtrennung und **Höhen-Dithering** schließen sich gegenseitig aus. Das Aktivieren des einen schaltet das andere aus.\r
\r
## Höhen-Dithering\r
\r
Höhen-Dithering kann Rundungsfehler auf kleine Blöcke mit benachbarten druckbaren Höhen verteilen, wenn seine Eingabe Höhen zwischen den verfügbaren Schichtgrenzen enthält. Diese Höhenunterschiede können aus Betrachtungsabstand Zwischentöne andeuten. Die Funktion benötigt die verbesserte Abstimmung und wirkt auf die exportierte Höhenkarte, nicht auf das 2D-Quellbild.\r
\r
![Der Mechanismus des Höhen-Ditherings bei Zwischenhöhen: direktes Einrasten im Vergleich zur Verteilung des Rundungsfehlers auf benachbarte druckbare Höhen.](16_height_dithering.svg)\r
\r
_Schematischer Mechanismus, kein garantiertes Vorher-nachher-Ergebnis. Unterschiedliche Oberhöhen bedeuten keine zusätzlichen Spulenfarben._\r
\r
Bei aktiviertem Dithering sucht Auto-paint eine Zwischenhöhe zwischen der ausgewählten und einer direkt benachbarten druckbaren Schicht, wenn deren Mischung die vorhergesagte Farbübereinstimmung verbessert. Exakte Farbtreffer und kalibrierte Zielzuordnungen behalten ihre Höhe; Bereiche ohne sinnvolle benachbarte Mischung bleiben unverändert. Erstelle das Modell nach einer Änderung neu und vergleiche Vorschau und Slicer-Ergebnis. Ohne Dithering gilt wieder die normale diskrete Zuordnung.\r
\r
Die Punktgröße richtet sich nach **Effektive Linienbreite** in den **3D-Druckeinstellungen** im Verhältnis zur **Pixelgröße**, gerundet auf eine ganzzahlige Pixelblockgröße. Das ist eine Näherung, keine exakte Mindestbreitengarantie. Randbereiche werden anders behandelt, um Grenzartefakte zu verringern. Prüfe im Slicer auf winzige Inseln und zusätzliche Leerfahrten.\r
\r
Wo Zwischenhöhenfehler vorhanden sind, kann die Umverteilung großflächige Tonwerte verbessern, während kleine Grafiken unruhiger oder die Geometrie aufwendiger werden. Sie erweitert keinen fehlenden Farbumfang und bestätigt keine ungestützte Kalibrierung. Entstehen viele kleine Bereiche, kann die Kombination mit Flat Paint besonders teuer sein, da diese Bereiche jede vollflächige Schicht gemeinsam belegen.\r
\r
## Optimierungseinstellungen\r
\r
![Gleichmäßige, mittige und randbetonte Priorität am selben Bild; höheres Übergangsdetail zeigt mehr mögliche Höhenoptionen.](18_optimizer_choices.svg)\r
\r
_Schematische Gewichte und Optionen, keine gemessenen Farben oder exakten Schichtzahlen. Abgedunkelte Bereiche erhalten weniger Priorität, werden aber nicht aus dem Bild entfernt._\r
\r
| Steuerelement | Auswahl und Wirkung |\r
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |\r
| **Algorithmus** | **Schnell:** engere, schnellere Suche. **Ausgewogen:** allgemeiner Standard. **Gründlich:** tiefere Verfeinerung von mehreren Startpunkten. **Tief:** breitere, aufwendigere Suche. **Exakte Grundreihenfolge:** zutreffende Reihenfolgen ohne Wiederholung vollständig durchgehen. |\r
| **Bereichspriorität** | **Gleichmäßig:** gleiches Pixelgewicht. **Mitte stärker gewichten:** Farben nahe der Mitte bevorzugen. **Ränder stärker gewichten:** Farben nahe den Bildrändern bevorzugen. Ändert Zuordnungsprioritäten, nicht Zuschnitt oder Extrusion. |\r
| **Übergangsdetail** | **Kompakt (80 %)**, **Detailliert (90 %, Standard)** und **Maximum (95 %)** legen Deckkraft-Endpunkte der Übergänge fest. Höhere Werte erlauben höhere Übergänge und mehr druckbare Zwischenfarben, begrenzt durch frühe Konvergenz und Höhenlimit. |\r
| **Startwert (optional)** | **Automatisch** verwendet einen stabilen, aus den Eingaben abgeleiteten Startwert. Gib eine Ganzzahl ein, um eine andere deterministische Suche zu vergleichen; leere das Feld für Automatisch. Kein Qualitätsregler. |\r
\r
Übergangsdetail beeinflusst obere Materialübergänge, nicht die erforderliche deckende Grundlage. Es erhöht weder die Bildauflösung noch verringert es die Linienbreite. Zusätzliche Übergangsfarben müssen dem aktuellen Bild nicht helfen.\r
\r
Bei gleichem Startwert behalten höhere heuristische Stufen das beste Ergebnis der niedrigeren Stufe bei. Es bleibt dennoch eine Optimierung von Vorhersagen. Exakte Grundreihenfolge prüft bei acht Filamenten 109.600 nichtleere Reihenfolgen, bei neun 986.409. Wiederholungen verwenden eine separate Verfeinerung, keinen vollständigen Beweis über jeden wiederholten Stapel.\r
\r
## Übergangszonen und Zuverlässigkeit\r
\r
| Anzeige | Bedeutung |\r
| ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |\r
| **Übergangszonen** | Physische Materialabschnitte mit Beginn, Ende und Dicke. Stauchungsmarkierungen zeigen eine geringere als die ideale Dicke. |\r
| **Gesamthöhe / physische Schichten** | Berechnete Stapelabmessungen, nicht die Anzahl der Bildfarben oder Spulen. |\r
| **Farbtrennungsstatus** | Erhaltene und zusammengeführte Farben, druckbare Kapazität, schlechtester erhaltener ΔE-Wert und Wiederholungen. Zusammengeführte Ziele sind keine erfolgreichen Zuordnungen oberhalb der Grenze. |\r
| **Erscheinungsmodell** | Ob Simulation, angepasstes Verhalten, lokale Vergleiche oder Stack-Matrix-Daten die Vorhersage stützen. Eine Messungsanzahl bedeutet nicht, dass jede Ausgabe gemessen wurde. |\r
| **Vorhersagezuverlässigkeit: Durchschnitt / Minimum** | Datenstärke der tatsächlich zugeordneten Farben. Der gewichtete Mittelwert kann einen schwachen Bereich verdecken, den das Minimum sichtbar macht. Zähler unterscheiden Messungen, Interpolation, angepasste Vorhersagen und Simulation. |\r
| **Ergebniszuverlässigkeit** | Kombinierte Indikatoren für HD-Kalibrierung, Abdeckung und Stauchung. Kein gemessener Genauigkeitsprozentsatz und nicht mit Vorhersagezuverlässigkeit identisch. |\r
| **Kalibrierung / Abdeckung / Stauchung** | Qualität der HD-Daten, Filamentabdeckung der Quellfarben und Einfluss des Höhenlimits. Hohe Werte zertifizieren den Druck nicht. |\r
| **Qualitätswert** | Vergleichswert der Optimierung, keine Messung. Bei unvollständiger, nichtstrikter Farbtrennung lautet die Beschriftung **Teilpalette**. |\r
| **Iterationen / Aus Zwischenspeicher** | Suchaufwand und Wiederverwendung von Ergebnissen. Mehr Iterationen beweisen keine bessere Farbe. |\r
| **Exaktes Optimum / Bestes gefundenes Ergebnis** | Vollständiger zutreffender Vergleich ohne Wiederholungen gegenüber heuristischer oder wiederholter Stapelverfeinerung. Keines beweist physische Genauigkeit. |\r
| **Kein entfernbarer Abschnitt** | Kein einzelner Abschnitt kann unter Erhalt der gewählten Prioritäten gelöscht werden. Eine andere Umordnung mehrerer Abschnitte könnte trotzdem besser sein. |\r
\r
Die Datenstützung nimmt mit Abstand von Messungen ab, wenn nahe Beobachtungen einander widersprechen oder wenn zurückgehaltene Vorhersagen die gemessenen Farben verfehlen. Normale Abstimmung kann begrenzte Unsicherheitskosten berücksichtigen. Die Farbtrennung verwendet für die Eignung weiterhin den unveränderten ΔE-Wert. Unsicherheit kann deshalb keine Farbe außerhalb der Grenze gültig machen.\r
\r
Prüfe diese Datenzusammenfassung nach Änderungen an Prozess oder Schichthöhe. Ein hoher allgemeiner Kalibrierwert bedeutet nicht, dass jedes neue Rezept gestützt ist. Siehe [Kalibrierabläufe](calibration-workflows).\r
\r
## Nächstes Filament vorschlagen\r
\r
**Nächstes Filament vorschlagen** erscheint, sobald ein Ergebnis vorliegt. Die Funktion sucht eine hypothetische Farbe, die die Abdeckung dieses Bildes verbessern könnte. Sie ist weder ein Produktangebot noch eine bereits im Drucker geladene Spule.\r
\r
| Feld oder Aktion | Bedeutung |\r
| -------------------- | ----------------------------------------------------------------------------------------------------------------------- |\r
| **Hex-Farbfeld** | Vorgeschlagene deckende Filamentfarbe. |\r
| **Gesch. ΔE +…%** | Geschätzte Verringerung des mischungsbezogenen mittleren Bildfehlers durch Hinzufügen. Höher ist besser; keine Zuverlässigkeitsangabe. |\r
| **HD** | Anfangsschätzung, übernommen vom nach wahrnehmungsbezogenem Farbabstand nächsten vorhandenen Filament. Nicht für ein Produkt gemessen. |\r
| **Erfasst** | Anteil der Bildpixel mit verbessertem geschätztem Fehler. |\r
| **Abgrenzung** | Unterschied zu aktuellen Filamenten auf einer Skala von 0 bis 1. Höhere Werte zeigen eine eigenständigere Abdeckungslücke. |\r
| **Zu Filamenten hinzufügen** | Fügt eine Arbeitszeile namens \`Kromacut-Suggestion-…\` hinzu und berechnet damit neu. |\r
\r
Suche eine echte Spule, wenn der Vorschlag hilfreich ist, und gib anschließend ihre tatsächliche Farbe und Kalibrierung ein. Drucke nicht in der Annahme, dass die hypothetische Zeile bereits verfügbar sei. Vorschläge werden bei Änderungen der Bildfarben oder des Filamentsets zurückgesetzt. Wird kein Kandidat gefunden, deckt das aktuelle Set das Bild nach dieser Näherungsprüfung bereits gut ab.\r
\r
Weiter: [Flat Paint](flat-paint), [Kalibrierabläufe](calibration-workflows) oder [Ergebnisse erzeugen und exportieren](generating-exporting-output).\r
`;export{e as default};
