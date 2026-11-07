const e=`---\r
title: Überblick\r
slug: overview\r
order: 10\r
description: Was Kromacut leistet und wie die wichtigsten Abläufe zusammenhängen.\r
---\r
\r
# Überblick\r
\r
Kromacut verwandelt ein flaches Bild in einen gestapelten, mehrfarbigen 3D-Druck. Das Grundprinzip ist einfach: Die Farben des Bildes werden zu physischen Schichten. Ihre Reihenfolge und Höhe bestimmen den Druckplan.\r
\r
Nutze Kromacut für Drucke im Stil von HueForge, farbige lithophanieähnliche Reliefs oder geschichtete Ausstellungsstücke, bei denen Filamentwechsel das fertige Bild erzeugen.\r
\r
## Wichtigster Arbeitsablauf\r
\r
Die meisten Projekte folgen demselben Ablauf:\r
\r
1. [Ein Bild laden oder importieren](loading-images).\r
2. [Farben reduzieren](reducing-colors), bis die Vorschau eine druckbare Palette hat.\r
3. [Raster glätten oder bereinigen](dedithering-cleanup), wenn einzelne Pixel das Bild unruhig wirken lassen.\r
4. Zum [3D-Modus](3d-mode) wechseln und Manuell oder Auto-paint wählen.\r
5. [Eine STL- oder 3MF-Datei erstellen und exportieren](generating-exporting-output) und die Druckanweisungen befolgen.\r
\r
## Zwei Möglichkeiten der Farbzuweisung\r
\r
Im 3D-Modus bietet Kromacut zwei Druckabläufe.\r
\r
| Arbeitsablauf | Geeignet, wenn | Was du steuerst |\r
| ---------- | ------------------------------------------------------ | --------------------------------------------------------------------------- |\r
| Manuell | Du jede Bildfarbe direkt steuern möchtest. | Farbreihenfolge, Höhen je Farbe, Druckeinstellungen und Wechsel. |\r
| Auto-paint | Kromacut den physischen Filamentstapel planen soll. | Filamentfarben, Deckdistanzen, maximale Höhe und Optimierungsoptionen. |\r
\r
Der manuelle Modus geht von den Farben im Bereich **Bildfarben** aus. Auto-paint geht von deinen tatsächlichen Filamenten und ihren **Deckdistanzen (HD)** aus und erzeugt daraus druckbare Schichten für das Bild.\r
\r
## Was die App zeigt\r
\r
Bereite das Motiv in 2D vor und plane die physischen Schichten in 3D. Die folgenden Anleitungen orientieren sich an dieser Unterscheidung.\r
\r
## Bebilderte Anleitungen\r
\r
Beginne mit der gewünschten Aufgabe. Jede Anleitung erklärt die Steuerelemente, ihr Zusammenspiel und die Folgen für den physischen Druck. Diagramme sind schematische Beispiele, keine kalibrierten Farbvorhersagen. Klicke auf eine Abbildung oder aktiviere sie per Tastatur, um sie in voller Größe zu öffnen.\r
\r
| Aufgabe | Anleitung |\r
| ------------------------------------------------------------------ | -------------------------------------------------------------- |\r
| Abmessungen, Schichthöhen und manuelle Farbreihenfolge festlegen | [3D-Modus](3d-mode) |\r
| Filamente wählen, Mischfarben optimieren und druckbare Details prüfen | [Auto-paint](auto-paint) |\r
| Eine flache Mehrmaterialplatte mit Bildseite oben oder unten erstellen | [Flat Paint](flat-paint) |\r
| HD messen, Palette Proofs vergleichen oder eine Stack Matrix fotografieren | [Kalibrierabläufe](calibration-workflows) |\r
| Die Silhouette vorbereiten und Pixel retuschieren | [Bilder laden](loading-images) |\r
| Tonwerte und Farben abstimmen und das Ergebnis übernehmen | [Bildanpassungen](image-adjustments) |\r
| Farben reduzieren und Paletten verwalten | [Farben reduzieren](reducing-colors) |\r
| Farbpunkte entfernen, ohne 2D-Bereinigung mit Höhen-Dithering zu verwechseln | [Rasterglättung und Bereinigung](dedithering-cleanup) |\r
| Den fertigen Stapel prüfen und an einen Slicer übergeben | [Ergebnisse erzeugen und exportieren](generating-exporting-output) |\r
\r
## Aufbau des Arbeitsbereichs\r
\r
Der Arbeitsbereich besteht aus drei Hauptbereichen:\r
\r
- Die Kopfzeile enthält Dokumentation, Darstellungseinstellungen und Community-Links.\r
- Links befinden sich die Steuerelemente des aktuellen Modus.\r
    - In **2D** sind das Bildanpassungen, Rasterglättung, Farbquantisierung, eigene Paletten und erkannte Bildfarben.\r
    - In **3D** sind das Druckeinstellungen, manuelle Steuerung, Auto-paint-Steuerung und Druckanweisungen.\r
- Die Hauptvorschau zeigt die 2D-Bildfläche oder das 3D-Modell.\r
\r
> Tipp: Änderungen an 3D-Einstellungen erstellen das Modell nicht automatisch neu. Klicke nach Änderungen an Druckeinstellungen, manuellen Farbhöhen oder Auto-paint-Optionen auf **3D-Modell erstellen**.\r
\r
## Ein gutes erstes Projekt\r
\r
Beginne mit einem kontrastreichen Bild, einem klar erkennbaren Motiv und wenigen Hintergrunddetails. Reduziere es auf 4 bis 16 Farben. Nutze anschließend den manuellen Modus, wenn du die Schichtreihenfolge bereits kennst, oder Auto-paint, wenn die Deckdistanzen deiner Filamente kalibriert sind.\r
\r
---\r
\r
Weiter: [Schnellstart](quick-start).\r
`;export{e as default};
