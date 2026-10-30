const e=`---\r
title: Bildanpassungen\r
slug: image-adjustments\r
order: 35\r
description: Alle Ton- und Farbregler, ihr Einfluss auf Zielfarben und wann die Vorschau ins Bild übernommen werden sollte.\r
---\r
\r
# Bildanpassungen\r
\r
Bildanpassungen verändern das Zielbild vor der Farbreduktion. Sie kalibrieren kein Filament, ändern keine Deckdistanz und garantieren nicht, dass eine angezeigte Farbe physisch druckbar ist.\r
\r
Bewege einen Regler, um seine Wirkung zu sehen; die Vorschau aktualisiert sich nach dem Beenden der Bedienung. Alle Regler beginnen bei null. Einzelne Rücksetz-Pfeile setzen einen Regler zurück, die Rücksetzfunktion des Bereichs alle Regler.\r
\r
## Ton- und Farbregler\r
\r
![Schematische negative, neutrale und positive Beispiele für alle zwölf Bildanpassungen.](32_adjustment_controls.svg)\r
\r
_Gezeigt werden schematische Wirkungsrichtungen, keine kalibrierten Druckvorhersagen. Die Effekte hängen von der Quelle und anderen aktiven Anpassungen ab._\r
\r
### Tonwerte\r
\r
| Regler | Bereich | Negative Werte | Positive Werte | Bedeutung für die Druckvorbereitung |\r
| ---------- | ------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |\r
| Belichtung | −3 bis +3 Blendenstufen, Schritt 0,01 | RGB-Werte abdunkeln. | Aufhellen; +1 verdoppelt die Kanalwerte bis zur Begrenzung. | Verschiebt großflächige Zieltonwerte. Diese Anpassung des gerenderten Bildes kann abgeschnittene RAW-Informationen nicht wiederherstellen. |\r
| Kontrast | −100 % bis +100 % | Tonwerte zu Mittelgrau ziehen. −100 % ergibt vor weiteren Anpassungen Mittelgrau. | Tonwerte von Mittelgrau wegschieben, mit Begrenzung bei Schwarz und Weiß. | Trennt große Bereiche, kann aber feine Schatten- und Lichtabstufungen einebnen. |\r
| Lichter | −100 % bis +100 % | Helle Bereiche abdunkeln. | Helle Bereiche aufhellen. | Ändert helle Tonwerte, die um Palettenplätze konkurrieren; fehlende Details lassen sich nicht zurückholen. |\r
| Tiefen | −100 % bis +100 % | Schattenbereiche abdunkeln. | Schattenbereiche aufhellen. | Kann vorhandene dunkle Unterschiede vor der Reduktion sichtbar machen. |\r
| Weiß | −100 % bis +100 % | Den hellsten Bereich abdunkeln. | Den hellsten Bereich aufhellen. | Trennt oder verbindet nahezu weiße Zielfarben. Keine Weißabgleichseinstellung. |\r
| Schwarz | −100 % bis +100 % | Den dunkelsten Bereich abdunkeln. | Den dunkelsten Bereich aufhellen. | Ändert nahezu schwarze Zielfarben; reines Schwarz bleibt wegen der Skalierung der Werte schwarz. |\r
\r
Lichter und Tiefen wirken auf breitere Bereiche als Weiß und Schwarz. Die Bereiche überlappen sich: Ein sehr dunkler Pixel kann sowohl auf Schwarz als auch auf Tiefen reagieren. Die Tonwertbereiche werden nach Belichtung, Kontrast, Temperatur/Tönung und den HSL-Anpassungen ausgewertet; die Regler können sich daher gegenseitig beeinflussen.\r
\r
### Farbe und lokale Details\r
\r
| Regler | Bereich | Negative Werte | Positive Werte | Bedeutung für die Druckvorbereitung |\r
| ----------- | -------------- | -------------------------------------------------------- | ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |\r
| Sättigung | −100 % bis +100 % | Intensität verringern; −100 % entsättigt. | Intensität erhöhen. | Ändert Unterschiede zwischen Bildfarben, nicht den physischen Farbumfang der Filamente. |\r
| Dynamik | −100 % bis +100 % | Die Sättigung vergleichsweise ungesättigter Farben stärker verringern. | Die Sättigung vergleichsweise ungesättigter Farben stärker erhöhen. | Weniger gleichmäßig als Sättigung, berücksichtigt aber keine Hauttöne. Reines Grau bleibt grau. |\r
| Farbton | −180° bis +180° | Farbtöne in eine Richtung drehen. | In die andere Richtung drehen. | Färbt das gesamte Bild um; zum Ändern einer exakten Farbe ein Farbfeld bearbeiten. |\r
| Temperatur | −100 bis +100 | Kühler: weniger Rot, mehr Blau. | Wärmer: mehr Rot, weniger Blau. | Eine angenäherte Farbstichkorrektur, **keine Kelvin-Angabe**. |\r
| Tönung | −100 bis +100 | Grün hinzufügen. | Magenta hinzufügen, Rot/Blau erhöhen und Grün verringern. | Korrigiert oder erzeugt einen Farbstich; kein gemessenes Kameraprofil. |\r
| Klarheit | −100 bis +100 | Lokalen Kontrast abschwächen. | Lokalen Kontrast und Kanten betonen. | Kann Kantenfarben oder Halos erzeugen, die quantisiert werden müssen. Stellt keine Details wieder her und verbreitert keine dünnen Linien. |\r
\r
Außer Belichtung und Farbton verwenden die Regler ganzzahlige Schritte. Alpha bleibt unverändert. Bei der Quantisierung ist das Alpha-Verhalten anders: Teiltransparente Pixel werden vollständig undurchsichtig.\r
\r
## Vorschau und Anwenden\r
\r
![Eine laufende Vorschau zweigt vom Quellbild ab. Anwenden übernimmt dieses Aussehen und setzt die Regler zurück; Quantisierung, PNG-Export und 3D verwenden danach die übernommenen Pixel.](33_adjustment_bake.svg)\r
\r
**Anwenden** übernimmt das aktuelle Aussehen fest in das zugrunde liegende Bild, setzt die Regler auf null und erzeugt einen Schritt im Bildverlauf. Dabei werden keine Farben reduziert, kein Modell erstellt und keine Druckeinstellungen verändert.\r
\r
Die Unterscheidung ist wichtig:\r
\r
- **Farbquantisierung, Bild skalieren, Bild herunterladen und 3D-Erzeugung verwenden das zugrunde liegende Arbeitsbild**, nicht die noch nicht übernommenen Vorschauanpassungen.\r
- **Bildfarben** beschreibt die zugrunde liegenden Pixel; die Farbfelder folgen daher nicht den laufenden Anpassungen.\r
- Retuschierwerkzeuge bearbeiten das zugrunde liegende Bild; aktive Anpassungen wirken anschließend erneut darauf.\r
- Die Rasterglättung liest das angepasste Bild. Übernimm die Anpassungen vorher, damit sie nicht zusätzlich auf das verarbeitete Ergebnis wirken.\r
\r
Der verlässliche Ablauf lautet **Anpassungen vorprüfen → Anpassungen anwenden → quantisieren → prüfen/bereinigen → 3D erstellen**. Schneide das Bild nach Möglichkeit vorher zu und skaliere es vorher.\r
\r
## Zurücksetzen und Rückgängig sind verschieden\r
\r
Zurücksetzen entfernt eine laufende Anpassung, keine bereits ins Bild übernommene Änderung. Nach Anwenden stehen die Regler erwartungsgemäß auf null, weil ihre vorherige Wirkung jetzt im Bild steckt. Verwende **Rückgängig**, um das frühere Bild wiederherzustellen.\r
\r
Wiederholtes Anwenden wirkt auf das bereits bearbeitete Bild. Dadurch können sich abgeschnittene Werte und verlorene Tonwertdetails summieren. Mache die Änderung zuerst rückgängig, wenn du Alternativen vergleichen möchtest.\r
\r
Weiter: [Farben reduzieren](reducing-colors).\r
`;export{e as default};
