const e=`---\r
title: Farben reduzieren\r
slug: reducing-colors\r
order: 40\r
description: Zweistufige Farbreduktion, feste Paletten und den Unterschied zwischen Umfärbung und Transparenz verstehen.\r
---\r
\r
# Farben reduzieren\r
\r
Die Quantisierung ersetzt viele Quellfarben durch eine kleinere Auswahl und vereinfacht damit die Bereiche für die manuellen und Auto-paint-basierten 3D-Abläufe. Eine 2D-Palette enthält Zielbildfarben, kein Auto-paint-Filamentprofil und keine gemessenen Druckvorhersagen.\r
\r
Schneide das Bild zuerst zu und skaliere es. Hast du [Bildanpassungen](image-adjustments) geändert, klicke vor der Quantisierung in diesem Bereich auf Anwenden.\r
\r
## Die zweistufige Verarbeitung\r
\r
![Algorithmusstärke begrenzt die Zwischenpalette; Anzahl der Farben oder eine gewählte feste Palette steuert die zweite Stufe.](34_quantization_pipeline.svg)\r
\r
**Algorithmusstärke** und **Anzahl der Farben** haben unterschiedliche Aufgaben. K-Means mit Stärke 128 gruppiert die Quelle zunächst in bis zu 128 Farben. Automatisch mit Anzahl der Farben 16 führt dieses Ergebnis anschließend auf höchstens 16 zusammen. Stärke ist weder ein Prozentsatz noch Deckkraft oder Filamentanzahl.\r
\r
| Feld oder Aktion | Bedeutung |\r
| -------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |\r
| Palette: Automatisch | Findet bildabhängige Farben und begrenzt anschließend ihre Anzahl. |\r
| Palette: integriert, Hersteller oder eigene | Ordnet das Zwischenbild den aktivierten Farben der gewählten Palette zu. Nicht jede verfügbare Farbe muss vorkommen. |\r
| Anzahl der Farben | Endgültige Obergrenze für Automatisch: 2 bis 256, Standard 16. Bei fester Palette deaktiviert. |\r
| Algorithmusstärke | Budget der Zwischenpalette: 2 bis 256, Standard 128. Ein höherer Wert erhält meist mehr Zwischendetails, liefert aber nicht unbedingt eine bessere endgültige Zuordnung. |\r
| Algorithmus | Reduktionsmethode der ersten Stufe. Standard: K-Means. |\r
| Anwenden | Verarbeitet das zugrunde liegende Bild und erzeugt einen Rückgängig-Schritt. Das Ändern einer Einstellung allein färbt es nicht um. |\r
| Rücksetz-Pfeil | Stellt Automatisch, 16 Farben, Stärke 128 und K-Means wieder her. Ein früheres Bild wird damit nicht wiederhergestellt. |\r
\r
Das Ergebnis kann weniger Farben enthalten als angefordert. Eine höhere Zielanzahl nach der Reduktion bringt entfernte Farben nicht zurück: Nutze zuerst **Rückgängig**, um Alternativen aus derselben Quelle zu vergleichen.\r
\r
Die Quantisierung erhält vollständig transparente Pixel, macht jedoch **alle teiltransparenten Pixel vollständig undurchsichtig**. Eine weiche Alphakante ist keine teilweise gedruckte Kante.\r
\r
## Einen Algorithmus wählen\r
\r
Diese Methoden gruppieren Farben. Keine fügt ein räumliches Dither-Muster hinzu oder garantiert, dass ein kleines Detail die Linienbreite deiner Düse übersteht.\r
\r
| Algorithmus | Was sich ändert | Sinnvoller Vergleich |\r
| ----------------------- | --------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |\r
| Keiner (nur Nachbearbeitung) | Überspringt die erste Quantisierungsstufe; Stärke ist deaktiviert. Die abschließende Anzahlreduktion oder Zuordnung zur festen Palette läuft weiterhin. | Saubere Grafiken direkt einer bekannten Palette zuordnen. Kein allgemeiner Schalter für „Bild unverändert lassen“. |\r
| Tontrennung | Teilt RGB-Kanäle in diskrete Stufen und setzt danach das Stärkebudget durch. | Bewusst abgestufte Grafiken; verfügbare Kanalstufen ändern sich sprunghaft. |\r
| Median-Cut | Teilt die Farbverteilung in Gruppen, die durch Durchschnittsfarben repräsentiert werden. | Vergleichen, wenn eine andere Methode eine wichtige Tonwertgruppe verliert. |\r
| K-Means | Findet nach Pixelanzahl gewichtete Farbcluster mit zufälliger Initialisierung. | Ein Ausgangspunkt für Fotos und gemischte Motive. Durchläufe derselben Quelle können sich leicht unterscheiden. |\r
| Wu | Nutzt Statistiken der Farbverteilung für Aufteilungen mit geringerer Variation innerhalb der Gruppen. | Bei Verläufen und Fotografien vergleichen. |\r
| Octree | Gruppiert Farben durch RGB-Unterteilungen und führt Gruppen passend zum Budget zusammen. | Bei Motiven mit vielen eigenständigen Bereichen vergleichen. |\r
\r
Es gibt keinen allgemein besten Algorithmus. Prüfe Motiv, kleine Schrift und wichtige Akzente in der vorgesehenen physischen Größe.\r
\r
## Feste Paletten und Herstellerpaletten\r
\r
Eine feste Palette bietet nur ihre ausgewählten Farben. Nach einer eventuellen ersten Reduktionsstufe wird jeder nichttransparente Pixel der nächstgelegenen verfügbaren Farbe im Lab-Farbraum zugeordnet. Dies ist eine Bildfarbabstimmung, keine optische Filamentsimulation.\r
\r
**Herstellerpaletten** sind inoffizielle Referenzsets aus Filamentnamen und beworbenen Hex-Farben. Sie garantieren weder aktuelle Produktverfügbarkeit noch Genauigkeit der Druckfarben. Die Bambu-Referenzfarben verwenden beispielsweise [Bambu Labs Filament-Hex-Tabelle](https://store.bblcdn.com/s7/default/1084369ef84345bbaa5d704a492954e0/Bambu_PLA_Basic_Hex_Code.pdf). Kromacut ist weder mit Herstellern verbunden noch wird es von ihnen unterstützt oder empfohlen.\r
\r
Integrierte Paletten und Herstellerpaletten sind schreibgeschützt. Klone eine Palette, um sie anzupassen. Verwende die [Filamentkalibrierung](calibration-theory) getrennt davon für das tatsächliche Verhalten von Spulen und Schichten.\r
\r
## Eigene Paletten\r
\r
| Steuerelement | Vorgehen |\r
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |\r
| Neue Palette erstellen | Benenne sie und füge mindestens eine aktivierte gültige Farbe hinzu. Die Palette wird ausgewählt. |\r
| Ausgewählte Palette bearbeiten | Ändert eine eigene Palette, nicht die aktuellen Bildpixel. Wende danach die Quantisierung an. |\r
| Farbe hinzufügen | Fügt Farbwähler, Hex-Feld und optionalen Namen hinzu. Gültig sind \`#RGB\` oder \`#RRGGBB\`; ungültige Zeilen werden beim Speichern ausgelassen. |\r
| Optionaler Farbname | Beschriftet eine Farbe, etwa mit einem Spulennamen. Beeinflusst die Zuordnung nicht. |\r
| Augenschalter | Deaktiviert eine gespeicherte Farbe, ohne sie zu löschen. Mindestens eine gültige Farbe muss aktiviert bleiben. |\r
| Zeile entfernen | Löscht die Zeile; die letzte Zeile lässt sich nicht entfernen. |\r
| Klonen | Kopiert eine andere Palette als Automatisch in eine bearbeitbare eigene Palette und erhält Namen sowie Deaktivierungskennzeichen. |\r
| Importieren | Liest eine \`.kpal\`-Datei und meldet importierte, überschriebene, doppelte oder umbenannte Einträge. Wählt gegebenenfalls die erste importierte Palette. |\r
| Exportieren | Speichert die ausgewählte eigene Palette als \`.kpal\`, einschließlich Namen und deaktivierter Farben. Klone integrierte Paletten zuerst, um eine bearbeitbare Kopie zu exportieren. |\r
| Ausgewählte Palette löschen | Entfernt die gespeicherte Palette und setzt die Auswahl auf Automatisch zurück. Löscht keine Bildpixel. |\r
| Speichern / Abbrechen | Übernimmt oder verwirft den Bearbeitungsentwurf. |\r
\r
Eine Auswahlbeschriftung wie **Meine Spulen (5/8)** bedeutet fünf aktivierte Farben aus acht gespeicherten Einträgen. Nur aktivierte Farben nehmen teil. Paletten und Auswahl werden lokal gespeichert; exportiere Sicherungen, bevor du App- oder Browserspeicher löschst. Sie sind von [Filamentprofilen](settings-and-controls#filament-profile-files) getrennt.\r
\r
## Bildfarben\r
\r
Bildfarben beschreibt das zugrunde liegende Bild, nicht laufende, noch nicht übernommene Anpassungen. Die Anzahl schließt vollständig transparente Pixel aus. Tooltips zeigen Hex-Wert, Alpha und Pixelanzahl. Verschiedene Alphawerte können separate Einträge mit demselben RGB erzeugen. Bei sehr farbreichen Bildern wird nur eine begrenzte Liste angezeigt, nicht jede Fotofarbe.\r
\r
Klicke auf ein Farbfeld, um **Farbe bearbeiten** zu öffnen. Nutze den RGBA-Farbwähler oder das Hex-Feld und anschließend Anwenden. Sechsstellige Hex-Werte ändern RGB und erhalten den aktuellen Alphawert des Farbwählers; achtstellige Hex-Werte enthalten Alpha ausdrücklich. Verwende die Transparenzsteuerung oder ein explizites Alpha-Suffix, wenn die Deckkraft wichtig ist.\r
\r
![Ein undurchsichtiger Ersatz färbt jede exakte Übereinstimmung um, Alpha null entfernt diese Pixel, und Löschen ordnet Farben neu zu, statt Löcher auszuschneiden.](35_swatch_operations.svg)\r
\r
| Aktion | Was sich ändert |\r
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |\r
| Undurchsichtige Farbe anwenden | Ersetzt jede exakte RGB-und-Alpha-Übereinstimmung im gesamten Bild, einschließlich unverbundener Bereiche. |\r
| Vollständig transparentes Alpha anwenden | Entfernt exakt passende Pixel aus dem sichtbaren Bild und der druckbaren Silhouette. Passende Motivpixel verschwinden ebenfalls. |\r
| Auf das transparente Farbfeld anwenden | Ersetzt alle vollständig transparenten Pixel und kann damit einen Hintergrund hinzufügen oder Löcher füllen. |\r
| Löschen | Quantisiert mit der verbleibenden Zielpalette erneut. Es löscht **keine** Pixel. Der ausgewählte Algorithmus der ersten Stufe läuft weiter; auch andere Farben können sich ändern. |\r
| Schließen / Escape | Verwirft die noch nicht übernommene Bearbeitung. |\r
\r
Wähle vor Löschen **Keiner (nur Nachbearbeitung)** für eine direkte Zuordnung zur verbleibenden Palette. Verwende [Füllen oder Radierer](loading-images#touch-up-pixels) für lokale Änderungen. Mache die Aktion rückgängig, wenn mehr vom Bild verändert wird als gewünscht.\r
\r
Manuelle Wechselanweisungen sind oberhalb von 64 nichttransparenten Farben deaktiviert. Auch unterhalb dieser Grenze kann eine kleinere Palette den Stapel vereinfachen. Weniger Zielfarben bedeuten aber nicht automatisch weniger Filamentwechsel in Auto-paint.\r
\r
Weiter: [Rasterglättung und Bereinigung](dedithering-cleanup).\r
`;export{e as default};
