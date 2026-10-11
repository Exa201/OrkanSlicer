# LitoGEN – Analyse und technische Leitlinie

Stand: 28.09.2026

## Ziel und Schutz des bestehenden Generators

LitoGEN wird als eigener Maker-Bereich mit eigener Oberfläche, Parametern, Geometrie-Erzeugung und Tests entwickelt. Die vorhandenen Lithophan-Aktionen bleiben unverändert und werden weder entfernt noch umgeleitet. Gemeinsame, erprobte Hilfsfunktionen dürfen nach Möglichkeit wiederverwendet werden; LitoGEN-spezifische Geometrie kommt in neue Klassen.

## Ist-Zustand in OrcaMobile

- `ImageToStlConverter` erledigt Bilderwahl, Bildeditor, Helligkeit/Kontrast, Graustufenmodus, Auflösung, STL-Ausgabe, Fortschritt, Speichern und optionales Laden in den Slicer.
- `LithophaneStlWriter` erzeugt bereits flache, gebogene und rotationssymmetrische Lithophane.
- `writeCubeKit` akzeptiert genau vier Bildwände und schreibt diese zusammen mit einem schlichten quadratischen Boden flach nebeneinander in eine STL. Das ist ein Klebe-/Montagesatz, kein bereits montierter Würfel. Es existieren dort weder eine Deckelgeometrie noch eine fünfte Bildfläche oder Passungs-/Verbindungsoptionen.
- Der Maker-Bereich hat eine bestehende, separate Lithophan-Kategorie. LitoGEN muss als zusätzliche Kategorie hinzukommen; die bestehende Kategorie bleibt erreichbar.
- Die heutige Höhenkarte erhält Helligkeits-/Kontrastkorrektur und min./max. Wandstärke. LitoGEN sollte diese Bildpipeline wiederverwenden, aber die Geometrie und die Parameter getrennt halten.

## Funktionsumfang als Vorbild

Die Produktbeschreibung von Lithophane Maker Desktop nennt unter anderem flache und gebogene Lithophane, Kugeln/Mondlampen, Nachtlichter, Leuchtkästen, Lampen, Silhouetten und farbige Lithophane/Chromaphane. Öffentlich beschriebene Lithophane-Generatoren zeigen darüber hinaus Würfel mit mehreren Bildseiten, Laternen mit wiederholten Bildflächen, Kugelsegmente sowie separate Deckel und Sockel. Die genaue Liste und die exakten Einstellungsnamen der installierten Desktop-Version sind öffentlich nicht vollständig dokumentiert; daher behandeln wir die Funktionsliste als Zielbild, nicht als Zusage pixelgenauer Kompatibilität.

### LitoGEN-Formen in sinnvollen Ausbaustufen

1. **Fotowürfel mit Deckel:** vier Bildwände; optional fünftes Bild als Bilddeckel; separat wählbarer geschlossener Deckel und Boden; Innenraum für LED und Kabelauslass.
2. **Würfel mit vier Bildern:** vier Seitenbilder, fünfte Fläche als lichtundurchlässiger oder transluzenter Deckel; Boden separat.
3. **Würfel mit fünf Bildern:** vier Seitenbilder plus fünftes Bild auf dem Deckel; Boden bleibt separat, damit Montage und Lichtquelle zugänglich bleiben.
4. **Mehrbild-Laterne:** 3–12 Seitenflächen, 1–6 Bilder, auf die Flächen verteilt oder wiederholt; separate Sockel-/Deckelteile.
5. **Kugel-/Globuslampe:** ein Panorama oder mehrere Längensegmente, untere Öffnung und Sockel.
6. **Konus/Zylinder/Lampenschirm:** Umfangsbild, Durchmesser oben/unten, Höhe und Öffnung.
7. **Form-Lithophane:** flach, gebogen, Herz-/Rahmenkontur sowie später Silhouette und Chromaphan.

Bei „vier Bilder“ und „fünf Bilder“ ist die hier festgehaltene Ausgangsannahme: vier Seitenbilder plus optional ein fünftes Bild auf dem Deckel. Der Boden bleibt ein eigenes Teil. Der Generator soll alternativ auch einen einfachen Bilddeckel ohne fünftes Foto anbieten.

## Geometrie- und Druckqualität

- Wandstärke wird aus luminanzkorrigierten Bildwerten abgebildet: helle Bildbereiche dünner, dunkle dicker. Für gleichmäßige Helligkeitswahrnehmung braucht die Abbildung eine Gamma-/Kennlinienkorrektur, nicht nur eine lineare Helligkeitskurve.
- Das Bild soll vor dem Export beschnitten, gedreht, gespiegelt und pro Fläche separat angepasst werden können. Für Wiederholungsflächen gibt es eine Option, Einstellungen auf alle Seiten zu übertragen.
- Für Bildflächen und Kanten sind Mindestwandstärke, Auflösung, Spitzen-/Rauschglättung und Seitenverhältnis vor dem Export zu prüfen. Ein dimensionsabhängiges Dreiecksbudget schützt Mobilgeräte vor übergroßen Meshes.
- Innenflächen bleiben glatt; Relief nur außen. So lässt sich die Lichtfläche sauber abbilden und die Konstruktion bleibt überschaubar.
- Steckverbindungen, Nut/Feder oder Magnettaschen müssen einstellbares Spiel erhalten. Eine kleine Passungslehre gehört vor einen kompletten Satz; Drucker, Material und Maßkorrektur beeinflussen den Sitz.
- Für die Druckqualität wird ein Profilvorschlag angezeigt, aber nicht ungefragt in Druckerprofile geschrieben. Zu prüfen sind weiße/transluzente Filamente, niedrige Schichthöhe, ausreichende Wandlinien, langsame Außenwände und Haftung. Konkrete Werte sollen als druckerspezifische Startwerte beschriftet werden, nicht als universelle Garantie.
- STL kann mehrere voneinander unabhängige Körper enthalten; für bessere Teileauswahl und beschriftete Komponenten bevorzugen wir später eine 3MF-Projektdatei. Als erster robuster Export kann zusätzlich ein ZIP mit einzeln benannten STL-Teilen und einer Einstellungsdatei dienen.

## Vorschau und Smartphone-Bedienung

- LitoGEN bekommt eine eigene Formauswahl und einen Schritt-für-Schritt-Editor: Form → Bilder zuordnen → Maße/Passung → Bildabstimmung → Vorschau → Ausgabe.
- Alle Flächen werden als Kacheln mit Bild, Flächenname und Status angezeigt. Bei vier/fünf Flächen kann der Nutzer sie direkt antippen und austauschen; die Reihenfolge muss nicht über mehrere Dateiauswahlen erraten werden.
- 3D-Vorschau zeigt den montierten Zustand, eine Explosionsansicht und die Druckanordnung der Teile. Eine Hinterleuchtungssimulation visualisiert die Wirkung der Dickenkarte; sie ist ausdrücklich eine Annäherung und keine Materialmessung.
- Vorschau-Meshes verwenden geringere Auflösung als der finale STL-Export. Bilder werden nacheinander verarbeitet und große Bitmap-/Mesh-Puffer nach jedem Teil freigegeben.
- Entwurf und Parameter werden rotationsfest gespeichert; „Nur generieren“ und „Generieren & speichern“ bleiben getrennte Aktionen.

## Offene, lizenzierte Referenzen

- [Lithofun / giladdarshan/lithophane](https://github.com/giladdarshan/lithophane) ist als MIT-lizenziertes, lokales Bild-zu-STL-Projekt beschrieben. Das README erklärt Bildskalierung/Glättung, Min-/Maxstärke, Kurvenparameter und Vorschau. Es deckt nicht den benötigten Mehrbildwürfel ab und dient nur als Algorithmus- und Testreferenz.
- [colbrydi/Lithophane](https://github.com/colbrydi/Lithophane) ist ebenfalls MIT-lizenziert und eignet sich als zweite Referenz für die Höhenkarten-/STL-Grundidee; es ist ein Python-Beispiel und keine Android-Bibliothek.
- [lithophane/lithophane.github.io](https://github.com/lithophane/lithophane.github.io) beschreibt Cube, Lantern, Globe und Lid/Pedestal, ist aber AGPL-3.0. Wegen der zusätzlichen Lizenzpflichten übernehmen wir daraus keinen Code in die App. Die beschriebenen Bedienfälle können als Produktanforderungen dienen.
- Die Desktop-Produktseite [Lithophane Maker Desktop](https://americanfilament.us/products/lithophane-maker-desktop) beschreibt Lizenzvarianten und die genannten Modellfamilien, veröffentlicht aber nicht alle internen Algorithmen oder jedes Detail der Oberfläche.

Wir schreiben den Generator selbst. Bei einer späteren Codeübernahme aus einem Open-Source-Projekt müssen Lizenz, Copyright-Vermerke, Abhängigkeiten und die Vereinbarkeit mit der OrcaMobile-Lizenz vor dem Einbau geprüft und dokumentiert werden.

## Empfohlene Umsetzung und Abnahmetests

### Phase 1 – LitoGEN-Menü und Würfelkonzept

- Neue Maker-Kategorie „LitoGEN“; vorhandene Kategorie „Lithophane“ bleibt unverändert.
- Eigene, testbare `LitoGen*`-Klassen für Modellparameter, Bildflächen, Geometrie und Vorschau.
- Würfel mit vier Seitenbildern, optionalem fünften Bilddeckel, separatem Boden/Deckel und einstellbarem Montageabstand.
- Bildzuordnung und verbundene/montierte sowie flach angeordnete Vorschau.

### Phase 2 – Belastbare Teile und STL-Ausgabe

- Jede Komponente einzeln benannt exportieren; Geometrie prüfen: geschlossene Hülle, keine degenerierten Dreiecke, korrekte Innen-/Außenorientierung, gültige Dimensionen und korrekte Bildseiten.
- Tests mit kleinen Referenzrastern (2×2), realistischen Rastern, weiß/schwarz/Gradient, sehr schmalen Abmessungen und ungültigen Werten.
- Prüfen, dass Exportteilanzahl und Dreiecksanzahl genau der gewählten Konfiguration entsprechen.

### Phase 3 – Bildabstimmung und Qualitätsprofile

- Pro Seite Zuschnitt, Drehung, Spiegelung, Helligkeit, Kontrast, Gamma und Rauschglättung mit Live-Vorschau.
- Profile „Schnell“, „Ausgewogen“ und „Fein“; Warnung bei Meshgrößen, die auf Smartphones oder Tablets unpraktikabel werden.
- Druckhinweise und Passungs-Teststück für die jeweilige Verbindung.

### Phase 4 – weitere Maker-Formen

- Laterne/Mehrbild-Prisma, Kugel, Zylinder/Konus, Sockel/Deckel, dann Rahmen-/Herzformen.
- Chromaphan erst nach einem separat validierten Mehrmaterial-Farbmodell.

### Abnahme pro Form

1. Vorschau zeigt dieselben Flächen und Maße wie die exportierte Geometrie.
2. STL-Prüfer kann jede Komponente laden; sie ist nicht leer und hat endliche Koordinaten und sinnvolle Bounding-Box.
3. Testbilder mit klaren links/rechts-Markierungen bestätigen Rotation, Spiegelung und Seitenzuordnung.
4. Qualitätsstufen liefern die erwartete Zunahme an Detail und Dreiecken, ohne UI-Blockade oder OOM.
5. Generieren, Speichern und Laden in den Slicer funktionieren separat; bestehende Lithophan-Workflows bleiben unverändert.
