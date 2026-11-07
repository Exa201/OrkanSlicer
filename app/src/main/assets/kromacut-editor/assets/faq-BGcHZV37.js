const e=`---\r
title: Häufige Fragen\r
slug: faq\r
order: 100\r
description: Kurze Antworten auf häufige Fragen zu Kromacut.\r
---\r
\r
# Häufige Fragen\r
\r
## Was ist die Deckdistanz?\r
\r
Die Deckdistanz, kurz **HD** (Hiding Distance), ist der Deckkraftparameter in Millimetern für die Betrachtung unter Auflicht. Auto-paint schätzt damit das Aussehen gestapelter Filamentschichten. Bei einer Dicke von einer HD lässt das Grundmodell noch 10 % des Einflusses der darunterliegenden Farbe übrig. Ab wann du keinen Unterschied mehr erkennst, hängt zusätzlich vom Filament, der Grundfarbe und den Betrachtungsbedingungen ab.\r
\r
Eine geringere HD bedeutet ein deckenderes Filament, das mit weniger Schichten abdeckt. Eine höhere HD bedeutet ein durchscheinenderes Filament, das mehr Dicke benötigt.\r
\r
HD ersetzt die in früheren Versionen angezeigte Transmission Distance (TD). Einen herkömmlichen TD-Wert für hinterleuchtete Lithophanien kannst du über die Umrechnen-Schaltfläche einer Filamentzeile eingeben. Kromacut multipliziert ihn mit 0,1, um eine anfängliche HD-Schätzung zu erhalten. Diese Umrechnung ist keine neue physische Messung. Siehe [Kalibrierabläufe](calibration-workflows).\r
\r
## Sollte ich Manuell oder Auto-paint verwenden?\r
\r
Nutze **Manuell**, wenn du Farbreihenfolge und Schichthöhen direkt gestalterisch steuern möchtest.\r
\r
Nutze **Auto-paint**, wenn dir tatsächliche Filamentfarben und Deckdistanzen vorliegen und Kromacut den Stapel automatisch planen soll.\r
\r
## Muss ich Filamente kalibrieren?\r
\r
Du kannst mit geschätzten Deckdistanzen beginnen. Veröffentlichte Transmission-Distance-Werte genau deines Filaments können ebenfalls einen Ausgangspunkt liefern. Gib sie über die Umrechnen-Schaltfläche ein, nicht direkt ins HD-Feld.\r
\r
Eine Kalibrierung verbessert Auto-paint-Ergebnisse meist, insbesondere wenn keine veröffentlichten Werte vorliegen oder das Ergebnis weiterhin falsch aussieht. Sie ist besonders hilfreich, wenn:\r
\r
- Ein Filament durchscheinend ist.\r
- Zwei Filamente ähnlich aussehen.\r
- Du projektübergreifend wiederholbare Ergebnisse möchtest.\r
\r
## Was unterscheidet Palettenfarben von Filamentfarben?\r
\r
Palettenfarben sind die Bildfarben im 2D-Modus und im manuellen Modus.\r
\r
Filamentfarben stehen für die physischen Materialien von Auto-paint. Auto-paint kann aus dem physischen Filamentstapel virtuelle Schichtfarben berechnen. Der exportierte Druckplan basiert aber weiterhin auf echten Filamenten.\r
\r
## Warum braucht die 3D-Vorschau eine Erstellen-Schaltfläche?\r
\r
Die 3D-Erzeugung kann aufwendig sein. Kromacut wartet auf **3D-Modell erstellen**, damit das Ändern einer Einstellung nicht immer wieder rechenintensive Arbeit startet und abbricht.\r
\r
## Kann ich ohne 3D-Modus exportieren?\r
\r
Im 2D-Modus kannst du das aktuelle Quellbild einschließlich übernommener Änderungen herunterladen. Übernimm laufende Bildanpassungen vorher mit **Anwenden**. Zum Erstellen und Exportieren von STL- oder 3MF-Modellen dient der 3D-Modus.\r
\r
## Ändert die Schichtvorschau den Export?\r
\r
Nein. Der Bereich **Schichtvorschau** beeinflusst nur, was in der Vorschau sichtbar ist. STL- und 3MF-Exporte enthalten das vollständig erzeugte Modell.\r
\r
## Welche Datei sollte ich drucken?\r
\r
Wähle **STL herunterladen** für breite Slicer-Kompatibilität und manuelle Filamentwechsel.\r
\r
Wähle **3MF herunterladen**, wenn dein Slicer farbfähige 3MF-Dateien unterstützt und du die farbigen Schichtobjekte erhalten möchtest.\r
\r
## Warum sind Höhen Näherungswerte?\r
\r
Schichtnummern hängen vom Verhalten des Slicers ab, insbesondere von der Höhe der ersten Schicht. Verwende die Angaben unter **Druckanweisungen** und bestätige anschließend die endgültigen Wechselschichten in der Slicer-Vorschau.\r
\r
## Kann ich meine Einstellungen teilen?\r
\r
Ja. Exportiere eigene 2D-Paletten als \`.kpal\` und Auto-paint-Filamentprofile als \`.kfil\`. Ältere Filamentprofile im Format \`.kapp\` lassen sich weiterhin importieren.\r
\r
## Ersetzt eine kleinere Pixelgröße eine kleinere Düse?\r
\r
Nein. Die Pixelgröße bestimmt die physische Größe der Bildpixel. Dadurch kann ein Strich schmaler werden als die Extrusionsbahn, die deine Düse erzeugen kann. Stelle **Effektive Linienbreite** unter **3D-Druckeinstellungen** ein, nutze die Vorschau druckbarer Details in Auto-paint und prüfe das geslicte Ergebnis. Siehe [3D-Modus](3d-mode).\r
\r
## Gilt meine Kalibrierung auch bei einer anderen Schichthöhe?\r
\r
Gehe nicht automatisch davon aus. HD-Messungen und Erscheinungsdaten haben unterschiedliche Aufgaben. Proof- und Matrix-Daten werden auf die Einstellungen und Filamente geprüft, mit denen sie erfasst wurden. Prüfe nach einem Wechsel der Schichthöhe die aktiven Daten und drucke eine kleine Validierungsprobe. Siehe [Kalibrierabläufe](calibration-workflows).\r
\r
## Warum gibt es mehr Vorschaufarben als Spulen?\r
\r
Dünne Schichten lassen darunterliegendes Filament auf die sichtbare Farbe einwirken. Unterschiedliche Dicken derselben Spulenreihenfolge erzeugen unterschiedliche vorhergesagte Mischfarben. Auto-paint wählt aus diesen erreichbaren Stapelpräfixen, während die exportierten Teile weiterhin die tatsächlichen Filamente verwenden. Siehe [Auto-paint](auto-paint).\r
`;export{e as default};
