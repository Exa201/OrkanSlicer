# OrkanSlicer Nightly 2.5 — Portierungsplan

## Nightly Batch 8 — 1/10 verifiziert (2026-11-02)

### Punkt 1/10 — Druckbettrand an Elephant-Foot-Kompensation anpassen

Die Orca-Nativeinstellung `brim_use_efc_outline` war im mobilen Prozesskatalog nicht erreichbar, obwohl der Android-Native-Kern die Funktion bereits unterstützt. Sie ist jetzt in „Skirt and brim → Brim“ im Prozessprofil-Editor sichtbar und bleibt eine Experteneinstellung. Der Native-Kern nutzt die kompensierte Erstschichtkontur nur, wenn die Option aktiv ist, Elephant-Foot-Kompensation und deren Layerzahl größer null sind und kein Raft verwendet wird.

`OrcaProcessSettingsCatalogTest#compensatedOutlineBrimControlIsExposedInBrimSection`: Teil der Klasse mit 11/11 erfolgreichen Tests. `scripts/check_profile_setting_routes.py`: 497 sichtbare Settings auf native Definitionen und passende Profilparser aufgelöst, 107 Gruppennamen eindeutig. `NativeSliceDeviceTest#brimCanFollowElephantFootCompensatedFirstLayerOutline` bestand auf Android API 36 (1/1): derselbe 20-mm-Würfel wurde mit 0,6-mm Elephant-Foot-Kompensation jeweils mit deaktivierter und aktivierter Einstellung gesliced; beide G-Codes enthalten Brim-Extrusion und die XY-Brim-Pfade unterscheiden sich. SliceTest- und Instrumentierungs-APK wurden gebaut und auf dem Emulator installiert. Kein physischer Drucktest.

Batch 8 steht bei **1/10**. Keine Distribution-APK oder Nightly-Veröffentlichung bis zehn neue Produktänderungen unabhängig verifiziert sind.

## Nightly Batch 7 — 10/10 (2026-11-02)

Der Batch hat das 10/10-Gate erreicht. `:app:testDebugUnitTest` und `:app:testSliceTestUnitTest` bestanden mit je 737/737 Tests. Vier gezielte API-36-Instrumentierungstests (Changelog/Back, Profilfiltersuche/Back+Escape, Z-Schrumpfung und Spiralvase) bestanden 4/4. Der 553-Test-Vollauf wurde nach 8 Tests kontrolliert abgebrochen: der alte Changelog-Text zeigte 0.8.420 statt der Build-Version; Text und Back-Behandlung wurden behoben und der Test danach gezielt bestanden. Nightly 0.8.426 wurde gebaut und auf dem AVD gestartet. Prerelease: [v0.8.426-nightly.1](https://github.com/Exa201/OrkanSlicer-Nightly-2.5/releases/tag/v0.8.426-nightly.1). Der Vollauf und ein physischer Xiaomi-Test bleiben offen.

### Punkt 8 — Android-Back/Escape schließt die Profilfiltersuche

- Orca `06bccd5570` schließt Suchoberflächen beim Escape-Tastendruck. In OrkanSlicer schließen Android-Zurück und Escape die geöffnete Profilfiltersuche, ohne den Profil-Host oder die Einstellungen zu verlassen.
- `ProfileFilterAccessibilityDeviceTest#backClosesProfileSearchWithoutClosingItsHost` wurde auf API 36 (`emulator-5554`) ausgeführt: 1/1 bestanden. Der Test öffnet die Suche, prüft Android-Zurück, öffnet sie erneut und prüft Escape.
- Der Back/Escape-Punkt ist als Punkt 8 verifiziert. Frühere Zwischenstände zählten 7/10; die hierunter dokumentierten Punkte 9 und 10 vervollständigen den Batch.
- Der frühere Fixture-Vorbehalt zu Vase/Höhenbereich wurde durch einen gezielten nativen Fixture-Test abgelöst; siehe Punkt 10. Der Test prüft die Höhenbereichskonfiguration und erzeugten G-Code im selben Slice.

### Punkt 9/10 — Z-Schrumpfung skaliert Höhenbereiche konsistent

- Portiert Orca `5fd0edf5cc`: `LayerRanges` berücksichtigt den Z-Schrumpfungsfaktor; nach Änderung der skalierten Grenzen werden abhängige PrintObjectRegions neu aufgebaut. Beim Clipping wird die unkompensierte Objekthöhe verwendet.
- `NativeSliceDeviceTest#zShrinkageCompensationKeepsHeightRangeAlignedAfterPrintConfigChanges` bestand auf API 36 (1/1): dieselbe native Print-Instanz wird mit 100 % und danach 95 % Z-Schrumpfung verarbeitet; der konfigurierte dünne/dichte Höhenbereich bleibt mit der skalierten Geometrie ausgerichtet.
- Der erste Testlauf scheiterte an einer vom Orca-Fixture abweichenden Testkonfiguration; nach Anpassung an Orcas tatsächliche Schichthöhen- und Kontureinstellungen bestand der Test. Kein Hardwaredrucktest.

### Punkt 10/10 — Spiralvase akzeptiert gleichfarbige Höhenbereich-Overrides

- Orca `2a8abac43d`: Materialprüfung im Spiralvasenmodus darf einen Geschwindigkeits-/Höhenbereich-Override derselben Filamentzuordnung nicht fälschlich als Mehrmaterialkonflikt einstufen. Die Android-Produktlogik entsprach dem bereits; neu ist der explizite native Regressionstest.
- `NativeSliceDeviceTest#spiralVaseAcceptsSingleFilamentHeightRangeOverrideAndProducesGcode` bestand auf API 36 (1/1) und erzeugte realen G-Code für Spiralvase mit einem Filament und aktivem Höhenbereich-Override.
- Voriger Fixture-Vorbehalt ist damit ersetzt. Kein physischer Drucktest.

### Zusatzprüfung — Bridge-Speed bei Curling-Verlangsamung (bereits Batch 6, nicht erneut gezählt)

- Orca `533d4c3e9e` war bereits Punkt 8/10 in Batch 6; seine Android-Portierung und der erste echte G-Code-Test sind im Batch-6-Protokoll unten dokumentiert.
- `NativeSliceDeviceTest#bridgeInfillKeepsBridgeSpeedWithCurledPerimeterSlowdown` wurde erneut auf API 36 / `emulator-5554` ausgeführt: 1/1 bestanden; die inneren Bridge-Infill-Bahnen bei Z=5,2 mm verwenden weiterhin 45 mm/s.
- Diese Wiederholungsprüfung ändert den Batch-7-Zähler nicht. Batch 7 bleibt bei **10/10**; die gezielte Release-Verifikation steht noch aus.

### Punkt 7 — Ausgewählte Objektgruppe ins Druckvolumen einpassen

- Orca `b3819e7c9e` dokumentiert die Verfügbarkeitsregel für „Scale to build volume“: nur bei unterstütztem rechteckigem oder rundem Bett, nicht für eine Volumenauswahl. OrkanSlicer hat keine separate Volumenauswahl; die Aktion ist für ein oder mehrere ausgewählte Modellobjekte verfügbar.
- Die mobile Implementierung berechnet die gemeinsame Begrenzungsbox, skaliert alle ausgewählten Objekte mit demselben Faktor und erhält relative Abstände. Die Gruppe wird auf den Bettmittelpunkt ausgerichtet und auf Z=Minimum gesetzt; pro Objekt werden GL-Mesh und Vorschau erneuert. Die vorhandenen Modellhistoriepfade erhalten Undo/Redo.
- `InitialProjectMenuDeviceTest#scaleToBuildVolumeFitsSelectedGroupAndPreservesRelativeLayout` bestand auf API 36 / `emulator-5554` (1/1): zwei getrennte, absichtlich zu große Objekte werden gemeinsam eingepasst; relative Distanz und Objektzahl bleiben erhalten, Undo/Redo funktionieren. `:app:connectedSliceTestAndroidTest` war erfolgreich.
- Kein physischer Drucktest. Batch 7 steht bei **10/10**; APK-/Release-Gate folgt nach gezielter Instrumentierungsprüfung.

### Punkt 6 — Scarf-Rampenendpunkt stabilisieren

- Orca `6c96e41162`: auf fast geraden Segmenten wird ein knapp hinter der exakten Rampenlänge liegender Knotenpunkt übersprungen. Dadurch bleibt der Seam-Endpunkt über Layer konsistent und ein kurzer Planer-Stummel entfällt; echte Ecken dürfen den Endpunkt auf den Eckknoten setzen.
- Android-Port in `ExtrusionEntity.cpp`. `scarfRampRemainsStableAcrossLayerVertexShiftsAndCorners` prüft den Produktionspfad auf API 36 mit zwei verschobenen Layerknoten und einer Ecke: 1/1 bestanden. ARM64 Native- und Instrumentierungsbuild erfolgreich.
- Kein echter Druck-/Firmwaretest. Batch 7 steht bei **6/10**; APK-/Release-Gate folgt bei 10/10.

### Punkt 5 — Filament-für-Funktion überschreibt keine anderen Feature-Zuordnungen

- Orca `304924a69a` trennt die Filamentwahl für Sparse Infill, interne massive Füllung sowie obere/untere Oberfläche. Ein Feature mit Wert `Default` verwendet weiter das Objektfilament; eine andere explizite Feature-Auswahl darf es nicht verändern.
- Diese Upstream-Semantik ist im aktuellen Android-Port bereits vorhanden. `NativeSliceDeviceTest#oneFeatureFilamentOverrideDoesNotCascadeToOtherInfillOrSurfaceFeatures` wurde frisch auf API 36 ausgeführt und bestand 1/1. Der Test erstellt vier echte G-Code-Slices und kontrolliert die erzeugten Config-Werte und Feature-Werkzeugnummern.
- Keine Produktionsänderung erforderlich. Batch 7 steht bei **5/10**; APK-/Release-Gate erst bei 10/10.

## Nightly Batch 6 — 10/10 (2026-11-01)

- Orca-Upstream `27cafaa06c` korrigiert Düsengrößen, die unter einer UI-Locale mit Dezimalkomma ankommen. Die Android-Profilfilterung hatte denselben Fehler: `0,6` wurde vor dem Zahlenvergleich am Komma getrennt und als `0` interpretiert.
- Android liest jetzt ein einzelnes Dezimalkomma als Dezimalpunkt, erhält aber die Auswahl des ersten Werts aus Punkt-/Komma-getrennten Düsengrößenlisten und unterstützt Semikolonlisten mit lokal formatiertem Einzelwert. Profilnamen, Layerhöhen- und Linienbreitenprüfungen normalisieren ebenfalls Dezimalkomma.
- `Slic3rUtilsNozzleLocaleTest`: 4/4 bestanden (Einzeldüse, Mehrdüsenlisten, Profil-Kompatibilität, Layerhöhe/Linienbreite). `:app:testDebugUnitTest --tests ru.ytkab0bp.slicebeam.slic3r.Slic3rUtilsNozzleLocaleTest` erfolgreich; `git diff --check` erfolgreich. Das ist ein gezielter Android-Profilpfad-Test, kein vollständiger Build oder Geräteslice.
- Batch 6 Punkt 1/10: Düsengrößen mit Dezimalkomma, siehe Nachweis oben.

### Punkt 2/10 — Experimentelle hochpräzise G-Code-Ausgabe

- Portiert Orca `887a8f1806`: optionale Prozessprofileinstellung `high_precision_gcode` (standardmäßig aus, Expertenmodus). Bei Aktivierung werden XYZ/IJ mit vier statt drei und E mit sechs statt fünf Dezimalstellen geschrieben. Relative E-Rundungsreste werden in Folgebewegungen übernommen. Supports behalten Standardpräzision; Support-Ironing bleibt präzise. FanMover, PressureEqualizer, Spiral Vase und G-Code-Vorschau lesen bzw. bewahren die gewählte Präzision.
- Einstellung ist in der mobilen Prozessprofilgruppe „Precision“ sichtbar und wird in Profilen gespeichert; die Änderung invalidiert die betroffenen Slicing-Schritte.
- `NativeSliceDeviceTest#experimentalHighPrecisionSettingChangesRealSliceCoordinatesAndExtrusionDigits`: 1/1 auf beiden verfügbaren API-16-Emulatoren bestanden. Der echte ARM64-Slice bestätigt die serialisierte Option und die tatsächlichen Ausgabeformate (XYZ 3→4, relatives E 5→6 Stellen). `scripts/check_profile_setting_routes.py` bestanden: 496 sichtbare Einstellungen mit nativer Definition und Parser verknüpft. ARM64-Native- und Instrumentierungsbuild erfolgreich; `git diff --check` sauber.
- Physischer Drucker-/Firmwaretest steht aus. Die Option bleibt experimentell und standardmäßig deaktiviert; eine erhöhte G-Code-Dateigröße ist zu erwarten.

### Punkt 3/10 — Transparente Filamente in der G-Code-Vorschau

- Android-Port von Orca `7319a2c54c26a7683e6d14ae7694753936f9a27f`: Die G-Code-Werkzeugpfadvorschau besitzt jetzt die gespeicherte Einstellung `preview_translucent_filaments` (standardmäßig an). Sie übernimmt den Alpha-Kanal der Slice-Farbpalette im GLES-Viewer und aktualisiert die Darstellung direkt beim Umschalten. Native Slice-Palette und G-Code bleiben unverändert. Der Port umfasst nur die mobile G-Code-Werkzeugpfadvorschau; Orkas separates Desktop-Modell-Viewport-/Glanzverhalten ist nicht enthalten.
- `SliceFilamentColorResolverTest`: 10/10 bestanden, darunter unveränderte native RGB-Palette, Alpha-Erhalt, RGB-Extraktion im opaken Modus und ungültiger Alpha-Fallback. Die API-36-Gerätetests `GCodePauseLayerPreviewDeviceTest#translucentToolPaletteRendersThroughGlesWithoutErrors` und `SettingsMenuActionDeviceTest#translucentFilamentPreviewCanBeToggledAndPersists` bestanden auf beiden verfügbaren Emulatoren (4 Ausführungen, 0 Fehler/Skips). Kein physischer Gerätetest behauptet.
- Ein erster Test im opaken Modus entdeckte eine fehlerhafte RGBA-zu-RGB-Extraktion. Diese wurde korrigiert; anschließend bestand die gezielte Testsuite. Noch keine APK oder Release für Batch 6.

### Punkt 4/10 — G-Code-Platzhalter nach Filament- und Druckkopfindex auflösen

- Android-Port von Orca `e52c735d1a`: unindizierte Vektor-Platzhalter verwenden den Filamentindex für Filament-Overrides, aber den nullbasierten physischen Druckkopfindex für andere Vektoren. `filament_map` selbst ist 1-basiert; ungültige/fehlende Zuordnungen fallen auf den aktuellen Filamentindex zurück. Float-Or-Percent-Vektoren verwenden dieselbe Auswahl und lösen Prozent-Abhängigkeiten über denselben Vektorindex auf.
- Native Probe `placeholder_filament_map_regression` wird über `NativeSliceDeviceTest#placeholderVectorsUseFilamentOrMappedExtruderIndexAsAppropriate` ausgeführt. Der Test bestätigt mit Filamenten 0/1 auf gemeinsamem Druckkopf 0: individuelle Rückzugswerte `0.4`/`3`, Düsenwert `0.4` für beide, Float-Or-Percent-Druckkopfdaten `0.2` und den Fallbackwert `0.6` bei ungültiger Map.
- API-36-Instrumentierung: 1/1 auf beiden verfügbaren Emulatoren bestanden. ARM64-Native- und Slice-Instrumentierungsbuild erfolgreich. Zwei Zwischenläufe deckten zunächst einen falschen Test-Fixture-Namen und danach eine falsche Erwartung für Druckkopf 0 auf; Fixture/Erwartung wurden korrigiert und der finale Lauf bestand. Es wurde kein tatsächlicher Druck gestartet.

### Punkt 5/10 — Prime-Tower-Normalisierung bei wiederverwendetem Print verifiziert

- Orca-Upstream `4c7928d8aa` korrigiert, dass `Print::apply()` den Prime-Tower-Zustand zunächst anhand des vorherigen Modells normalisieren konnte. Der Android-Port war bereits im Quellcode vorhanden; der fehlende Nachweis für ein wiederverwendetes `Print` wurde jetzt ergänzt.
- Der Test wendet dieselbe native `Print`-Instanz nacheinander auf ein Ein- und ein Zweifilamentmodell an und ruft nach jeder Anwendung `Print::process()` auf. Er prüft, dass beim ersten Modell kein Tower erzeugt wird und der Tower für das zweite Modell mit zwei Werkzeugen wieder aktiviert und tatsächlich generiert wird.
- `:app:assembleSliceTest :app:assembleSliceTestAndroidTest` erfolgreich. `NativeSliceDeviceTest#reusedPrintReenablesPrimeTowerWhenNextModelUsesAnotherFilament` bestand direkt per ADB auf beiden API-36-Emulatoren (je 1/1). Das belegt den nativen Print-/Slice-Zustand, aber keinen G-Code-Export oder physischen Druck.

### Punkt 6/10 — Adaptive-PA-Messungen nur für geänderte Varianten validieren

- Android-Anpassung von Orca `2cb2f0d2e7`: `adaptive_pressure_advance_model` prüft beim Bearbeiten nur Varianten, deren Text tatsächlich geändert wurde. Unveränderte `0,0,0`-Platzhalter für andere Filamente blockieren das Speichern nicht; Leerzeichen um Kommawerte und mehrzeilige Messreihen sind zulässig. Fehler werden mit Filament- und Zeilennummer gezeigt und sperren Speichern bis zur Korrektur.
- `AdaptivePAModelValidatorTest`: 4/4 bestanden (Whitespace/mehrzeilige Reihen, geänderte Variante, unveränderter Platzhalter und Grenz-/Formatfehler). `AdaptivePAModelEditorDeviceTest`: 2/2 auf `solidcraft_port_test_clean_20261101` / API 36; echter Dialogtest bestätigt zeilenbezogene Fehlermeldung plus deaktiviertes Speichern und danach erfolgreiches Speichern des Whitespace-Modells unter Erhalt des unveränderten Platzhalters.
- `:app:assembleSliceTestAndroidTest` erfolgreich. Der parallele Lauf auf `solidcraft_touch_isolated_api36` scheiterte in beiden Tests vor Semantikzugriff mit „No compose hierarchies found“; dessen Systemlog zeigt mehrsekündige Graphics-Composer-/Surface-Sync-Verzögerungen. Die gezielte Wiederholung auf dem sauberen Port-AVD bestand 2/2. Kein physischer Drucker-, Xiaomi- oder echter Drucktest.

### Punkt 7/10 — Boolesche Mesh-Konvertierung entfernt isolierte CGAL-Vertices

- Portiert Orca `b3f8d0af15`: die CGAL-Surface-Mesh-Konvertierung entfernt unbenutzte Vertices und kompaktifiziert die Indizes, bevor CGAL Volumenprädikate auswertet. Das verhindert den Upstream-Crash bei geschlossenen Meshes mit zusätzlichen isolierten Vertices.
- `NativeSliceDeviceTest#cgalBooleanVolumeChecksIgnoreUnreferencedVertices` verwendet den produktiven Konvertierungspfad mit drei isolierten Vertices (vor, zwischen und nach benutzten Indizes). Der API-36-Test bestätigte geschlossene Volumenhülle, keine Selbstüberschneidung, Volumen 1000, gültige kompaktierte Indizes und alle 12 Dreiecke.
- `:app:assembleSliceTest :app:assembleSliceTestAndroidTest` erfolgreich; gezielte API-36-Instrumentierung 1/1 erfolgreich. Das ist ein Native-/CGAL-Regressionsnachweis, kein Boolean-UI-Test, vollständiger Slice-Suite-Lauf oder physischer Druckertest.

### Punkt 8/10 — Bridge-Speed bei Curling-Verlangsamung

- Portiert Orca `533d4c3e9e`: bei `slowdown_for_curled_perimeters` behält nicht unterstütztes Bridge Infill die konfigurierte `bridge_speed`, statt den Wert für vollständig überhängende Perimeter zu übernehmen.
- `NativeSliceDeviceTest#bridgeInfillKeepsBridgeSpeedWithCurledPerimeterSlowdown` erzeugt einen echten nativen Slice und kontrolliert die G-Code-Geschwindigkeit der Bridge-Infill-Linien. Der API-36-Emulatorlauf bestand 1/1 (0 Fehler/Skips; 10,943 s Testlaufzeit).
- Batch 6: **8/10**. Noch keine APK oder Release; vor dem 10/10-Gate folgen zwei neue Android-relevante Orca-Punkte mit reproduzierbarem Slice- oder UI-Effekt.

### Punkt 9/10 — 3D-Fuel-PCTG-ESD-Profil mit Herstellername

- Orca `bb4c092ae4` führt den 3D-Fuel-Herstellernamen im ESD-PCTG-Profilnamen und verwendet die aktualisierte Profil-ID. Das Android-Profilasset zeigte bisher `3D-Fuel Pro PCTG-ESD` mit abweichender Schreibweise; es heißt jetzt wie im Orca-Profilkatalog `3D-Fuel Pro PCTG ESD`.
- `NativeSliceDeviceTest#orca3DFuelMaterialLibraryLoadsIndependentlyAndSlicesRepresentativeFilaments` prüft, dass der neue Markenname vorhanden und der alte Name nicht mehr gelistet ist. Es lädt das ESD-Profil, verifiziert 270 °C und erzeugt einen echten Slice mit ausführbarer Filamenttemperatur im G-Code. Der gezielte API-36-Lauf bestand 1/1; ARM64-Native- und Test-APK-Builds waren erfolgreich.
- Batch 6: **9/10**. Kein APK-/Release-Gate bis zum Abschluss eines weiteren geeigneten Orca-Ports und dessen fokussierter Runtime-Verifikation.

### Punkt 10/10 — Lightning-Infill-Stützproben außerhalb der Infill-Grenze

- Portiert Orca `4b3e4194ff`: geöffnete Überhangproben werden in die Such-Bounding-Box des Lightning-Distanzfelds aufgenommen. `update()` kann dadurch jede erzeugte Probe erreichen und löschen; zuvor konnte eine Probe knapp außerhalb der Infill-Kontur endlos erneut in den Support-Baum aufgenommen werden.
- `NativeSliceDeviceTest#lightningDistanceFieldExpandsBoundsToContainOpenedOverhangSamples` bestand 1/1 und reproduziert Proben außerhalb der ursprünglichen Kontur ohne doppelte Ausgabe. `NativeSliceDeviceTest#lightningInfillCompletesWhenOpenedOverhangExtendsPastInfillBounds` bestand 1/1 mit einem vollständigen nativen Slice samt Sparse-Infill-G-Code.
- ARM64-Slice- und Instrumentierungsbuild erfolgreich. Batch 6 ist damit 10/10 funktional verifiziert; APK-/Release-Gate folgt nach vollständigen Debug-/Slice-Unit-Tests und Release-Build.

## Workspace-Import-Fortsetzung — 2026-11-02

- Import-Arrange ist jetzt atomar: bei Überlauf bleiben vorhandene Modelle an ihren Positionen. Aktivierte Auto-Ausrichtung wird vor dem Arrange angewendet. Nicht passende importierte Objekte werden auf automatisch erzeugten und ausgewählten Druckplatten platziert; einzeln zu große Objekte werden zentriert und gewarnt.
- Die Arrange-Parameter erlauben jetzt zusätzlich XY-Rotationen. API-36-Test mit zwei rechtwinkligen Modellen auf dem 255×255-mm-Bett bewies Platzgewinn, gemeinsame Plattenbelegung und überlappungsfreie Bounds. Import-Overflow und atomarer Arrange-Rollback bestanden ebenfalls direkt auf API 36.
- Mehrmodell-Importe versuchen zuerst ein gemeinsames Arrange; nur bei Platzmangel folgt der atomare pro-Objekt-Overflow-Fallback.
- Android-16-Regressionsprüfung: automatische Plattenerzeugung bei voller Platte, XY-Drehung, Nutzung der konkaven L-Freifläche und Mehrfachauswahl großer STL-Dateien mit Erhalt der vorhandenen Platte bestanden. Der Mehrfachimporttest bestätigt drei belegte Platten, aktive Auswahl der letzten Platte sowie innerhalb der Bettgrenzen platzierte, auf Z=0 liegende Modelle.
- Erneute API-36-Ausführung im aktuellen Arbeitsbaum: `detailedConcaveModelKeepsItsOpenFootprintAboveLegacyProjectionLimit`, `importedObjectsMoveToAutomaticallyCreatedPlateWhenCurrentPlateIsFull`, `importedObjectsRotateInPlaneToUseAvailableBedSpace` und `additionalProjectModelOverflowCreatesPlateWithoutMovingExistingModel` jeweils 1/1 bestanden. `git diff --check` ebenfalls erfolgreich. Xiaomi-Hardware wurde nicht geprüft; diese Gegenprüfung erstellt weder APK noch Release.
- Diese Workspace-Änderung ist kein Orca-Upstream-Port und kein Batch-Zähler. `ModelInstance::get_arrange_polygon()` verwendet für geeignete Meshes bis 50.000 Facetten die vereinigte konkave Projektion; bei ungültigen, zu großen oder nicht projizierbaren Meshes bleibt die konvexe Hülle als sichere Rückfallebene. Die Bounds belegen gedrehte Instanzen; `Model.getRotation()` liest dagegen den ModelObject-Transform, nicht die ModelInstance-Rotation, und ist daher für diesen Test nicht aussagekräftig.

## Fortsetzung nach Batch 5 — Stand 2026-11-01

- Der Stable- und Nightly-Fix für das Kobra-3-V2-Pressure-Advance-Problem ist verifiziert: die erneuerte v2-Migration entfernt gespeichertes `M900 K0.051`, erhält den Filamentwert und wurde durch einen echten API-36-Slice für PA `0.075` sowie deaktiviertes PA geprüft.
- Die vom Benutzer gewünschte Ausrichtung entlang der gedrehten Achsen des Hauptobjekts wurde als Nightly-Ergänzung umgesetzt (`75c87032fa`); UI-Auswahlgruppe, JNI-Projektion, Undo/Redo, 3MF-Rundlauf und Slice sind getestet. Diese zusätzliche Workspace-Funktion zählt nicht als Orca-Upstream-Port und nicht als Batch-6-Item.
- Upstream-Referenz aktualisiert auf `b5517b693e898261a6e28f940b110cd62683fede` (2026-11-01). Die relevanten neueren `libslic3r`-Fixes wurden gegengeprüft: Lightning-Grenzfall `7ac46c9a6` ist bereits in `DistanceField.cpp`; Extruderindex-/Layervergleich aus `fcd96ad91c` ist bereits portiert; Subtypkosten aus `8e37d7d4b` sind im Android-Matcher vorhanden, während geladene Drucker-Tray-Daten weiterhin den vollständigen App-Pfad blockieren. Desktop-Agent-/PresetBundle-Änderung `913c622574` hat auf Android keinen passenden Hostpfad; per-Drucker-MMU-Zustand ist separat bereits implementiert.
- Der Xiaomi-ADB-Installationsversuch der Stable-APK wurde mit `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user` beendet; nicht umgangen. Der Nightly-Objektachsen-Gerätetest bestand 1/1 auf API 36 und `PrimaryObjectAxesTest` bestand.
- Nächster konkreter Nightly-Schritt: die konvexe Arrange-Hülle gegen ein konkaves/mehrteiliges Testmodell messen, danach den präzisen Runtime-Nachweis für Orca #14 weiterentwickeln und einen weiteren Android-relevanten Upstream-Punkt auswählen. Keine Batch-6-APK oder Release, bevor zehn neue, tatsächlich Android-relevante Upstream-Punkte jeweils umgesetzt und verifiziert sind.

### Orca #11 — direkter Bounds-Invariant-Nachweis (2026-11-01)

- `lightning_distance_field_bounds_probe()` baut das produktive `DistanceField` mit einer 10-mm-Infill-Begrenzung und einem 20-mm-Überhang auf und zählt dessen tatsächlich erzeugte Samples. Die Regression belegt zugleich, dass mindestens ein Sample außerhalb der ursprünglichen Grenzen liegt und nach dem produktiven Bounds-Merge kein Sample außerhalb der aktualisierten Grenzen bleibt.
- `NativeSliceDeviceTest#lightningDistanceFieldExpandsBoundsToContainOpenedOverhangSamples`: 1/1 auf API 36; ergänzend `#lightningInfillCompletesWhenOpenedOverhangExtendsPastInfillBounds`: echter Lightning-G-Code-Slice 1/1 auf API 36. Produktcode des Orca-Fixes war bereits portiert; diese Nachweisverbesserung ist kein neuer Batch-6-Featurepunkt.
- Codecommit `6e411be2d6` ist auf `nightly-2.5` gepusht. Der direkte #11-Proben-/Bounds-Blocker ist geschlossen. #14s exakter Abstand zwischen Supportpfad und Mesh bleibt offen.

## Nightly Batch 5 — abgeschlossen: 10/10 Änderungen (2026-11-01)

### Änderung 10/10: Hilbert-Curve-Sparse-Infill glätten

Port Orca `fb36d5e73b`'s configurable Hilbert corner smoothing. `sparse_infill_smooth_factor` (0–100%) is a native process-profile option, appears in the mobile Infill settings, participates in preset save/load and invalidates sparse-infill slicing state. At 0% the original Hilbert path is preserved; higher values round only sparse Hilbert infill corners. Wall, top and bottom toolpaths are unaffected.

`NativeSliceDeviceTest#hilbertSparseInfillSmoothFactorChangesOnlySparseInfillToolpaths` passed 1/1 on an API-36 emulator using real native slices and comparing G-code moves at 0% and 100%. The test confirms the setting reaches the serialized native config, sparse-infill moves change and add flattened segments, and outer/inner walls plus top/bottom surfaces remain byte-for-byte equal. ARM64 native and instrumentation APK builds passed. Full Debug and Slice JVM suites both passed 712/712, zero failures/errors/skips. The complete batch has 10/10 verified changes and its isolated Nightly APK/release are published below.

Batch gate: code commit `4867891309e46c796a70b8bd710131792ce29889`; APK `OrkanSlicerNightly25_4867891309.apk`, 239,951,714 bytes, SHA-256 `96DB4552AEADCBFAEC0FD3715E8460D0E0C76A235AF7361678A43C8F70C6F536`; version 0.8.424 / code 459; package `com.exa201.orkanslicer.nightly25`. It installed and launched on the API-36 emulator. GitHub prerelease: [v0.8.424-nightly.1](https://github.com/Exa201/OrkanSlicer-Nightly-2.5/releases/tag/v0.8.424-nightly.1). Xiaomi `2211133G` installation remains user-restricted by MIUI (`INSTALL_FAILED_USER_RESTRICTED`); no physical test is claimed.

### Änderung 9/10: lange Extrusionsketten lokal verfeinern (2026-11-01)

Port Orca `69193d5681`'s bounded travel refinement into the active extrusion-entity ordering path. Chains longer than eight paths now refine the existing greedy chain with reversible 2-opt and short/long or-opt moves, using a uniform endpoint grid and a capped work queue. A nearest-next chain is also refined and selected only when its measured travel is shorter. Chains without a fixed nozzle start keep the existing behavior; paths that cannot reverse are never flipped. This changes path order only, not geometry or extrusion values.

`NativeSliceDeviceTest#longExtrusionChainsAreRefinedWithoutIncreasingTravel` passed 1/1 on API 36. Its deterministic 12-segment adversarial route verifies that each segment occurs once and that the refined travel is shorter. ARM64 native build and Slice instrumentation APK compilation passed. Full Debug and Slice JVM suites passed 712/712 each, with no failed or skipped tests. Xiaomi physical installation remains unavailable due to `INSTALL_FAILED_USER_RESTRICTED`.

Change 9 verification details follow; Batch 5 completion is recorded above.

## Nightly Batch 5 — Änderung 8/10: Kobra-3-V2-Pressure-Advance-Startwert entfernt (2026-11-01)

Das eingebaute Stockprofil setzte fest `M900 K0.051` im Drucker-Start-G-Code. Damit blieb Pressure Advance aktiv und wurde zusätzlich zum Filamentwert ausgegeben, selbst wenn der Benutzer den Filament-PA-Wert anders einstellte oder ausschaltete. Der feste Befehl wurde aus dem Profil entfernt. Beim ersten Start nach dem Update entfernt eine einmalige Migration exakt diese alte Zeile aus dem gespeicherten Stockprofil `Anycubic Kobra 3 v2 0.4 nozzle - Stock`; andere Profilwerte und manuelle Änderungen bleiben erhalten.

`Kobra3V2ProfileMigrationTest` besteht 2/2. `SlicePrintPreparationComposeDeviceTest#kobra3V2StockStartGcodeDoesNotForcePressureAdvance` prüft das ausgelieferte Profil und bestand 1/1 auf API 36. Debug-Unit- und Slice-Unit-Gesamtsuiten laufen am Batch-Gate; Xiaomi-Installationen bleiben wegen `INSTALL_FAILED_USER_RESTRICTED` ungeprüft. Stable-Ausgabe wurde separat als 0.8.409 (`e28e071`) gebaut. Nightly 5 steht bei 8/10; noch keine APK aus diesem Teilbatch.

Stand: 2026-11-01. Upstream-Vergleich: OrcaSlicer `main` at
`3aedd50bcb94221db79ccbf8d6a7985e795fe25d`; the official OrcaSlicer 2.5.0 Beta notes were also
reviewed. Nightly Batch 4 is at **10/10 implemented**: five native slicer ports, Qidi selected-bed start
temperatures, safe By-Object ordering for tall objects, and Orca's sparse-infill-only arc fitting.
Changes 1–8 were already on `nightly-2.5`; changes 9–10 are now focused-test verified locally.
The batch APK gate follows full Debug/Slice JVM regression and commit/push. This is a selective
Android port, not the complete upstream engine.

Batch-4 build verification: Debug and Slice JVM suites both passed 708/708 (0 failures, errors,
or skips). The two new native slice instrumentations passed 1/1 each on API 36. Nightly APK
`OrkanSlicerNightly25_a717018b7e.apk` (version 0.8.423 / code 458, application ID
`com.exa201.orkanslicer.nightly25`) built from code commit `a717018b7e31f871f3903916bc0a391ac786d10e`,
239,853,168 bytes, SHA-256 `9C8E1340613FDB6C3CCC0C77E631524AC7B28315379BE4FC9CD67ED764C26042`. It installed and
started on the API-36 emulator without FATAL EXCEPTION, native fatal signal, ANR, or OOM in the
captured logcat window. Xiaomi 2211133G installation remains blocked by Android's
`INSTALL_FAILED_USER_RESTRICTED`; this is not a Xiaomi test pass.

## Verified preview-performance finding

Desktop Orca exposes OpenGL MSAA sample counts (off/2/4/8/16), optional FXAA, and an FPS
cap. The Android workspace instead uses a GLES3 `GLSurfaceView` whose EGL config and
renderer lifetime are established by the existing host. Directly changing the desktop MSAA
setting is not compatible without redesigning that surface setup. OrkanSlicer already scales
the GL surface and maps touch coordinates to it, so the safe Android equivalent is a user
selectable viewport render scale. It changes only display pixels: native model data, slicing,
G-code, and export are untouched. Automatic mode retains the low-RAM device policy.

The Android adaptation now uses 70% for low-RAM devices and 65% for recognized legacy/software GL renderers, with 100% for unknown/current renderers. Users can override it at 50/65/80/100%. Recognized low-performance devices also use a 35-layer initial G-code preview block, simplified inactive plate previews, and a 180,000-triangle active-preview cap; low-RAM keeps stricter caps. These limits affect display previews only, never model geometry or slicing. Policy tests passed; GL surface/picking passed at 50/65/100% on API 15/16 emulators. In this turn the focused policy JVM test and 1/1 GL/touch test passed again on an API-36 emulator. Xiaomi 2211133G rejected instrumentation installation (`INSTALL_FAILED_USER_RESTRICTED`), so physical-device verification remains open. The renderer-name classifier is a conservative heuristic, not a benchmark.

The official Orca 2.5.0 Beta notes do not describe a dedicated weak-GPU preview-quality switch; they highlight slicing, travel, infill, toolhead-clearance, and translucent-preview changes. This Android render-scale/preview-budget policy is the compatible adaptation, not a direct port of a desktop graphics preference.

## Product identity / data isolation

Nightly uses Android application ID `com.exa201.orkanslicer.nightly25` and display name
`OrkanSlicer Nightly 2.5`. Android scopes private preferences, files, cache, and app-specific
external storage to the application ID; therefore Nightly installs beside the stable app with
separate data. The Kotlin namespace remains `ru.ytkab0bp.slicebeam` to avoid a needless source
and JNI package migration. SAF files explicitly selected by the user remain shared documents.
The Nightly updater queries the Nightly repository and selects published prereleases rather
than the stable app's `/releases/latest` endpoint.

## Existing Android parity verified from source

Already present: Move, Rotate, Scale, Mirror, auto-orient, flatten/lay-on-face, arrange,
duplicate/fill/center/delete/select, plane cut and connectors, color/support/seam/fuzzy paint,
text emboss/engrave, measure, variable layer-height profile, split to objects/parts, mesh
simplification, and Boolean union/difference/intersection. Do not reimplement these as new
features; extend them only where a demonstrated parity gap remains. The older
`ORCA_FEATURE_AUDIT.md` Boolean row is stale relative to `ModelBooleanDialog`,
`GLRenderer`, and the current Boolean workspace path.

Profile parity is substantial: the current audit records 887 parsed native options and 492
visible routed profile rows (319 process, 101 filament, 72 printer). Those figures measure
source routing, not desktop completeness or runtime verification.

## Prioritized feature backlog

| # | Candidate | Current evidence / gap | Acceptance evidence |
|---:|---|---|---|
| 1 | Low-GPU preview quality control | Implemented: automatic mode uses Android low-RAM status plus a conservative `GL_RENDERER` classifier for known software/legacy GPU families (70% on low-RAM, 65% on recognized low-end GL, 100% for unknown/current hardware); explicit 50/65/80/100% viewport choices remain available. Recognized low-performance devices use a 35-layer initial G-code preview block, simplified inactive plate previews, and 180,000-triangle active-preview cap; low-RAM remains stricter. Renderer identity is runtime-only. | Policy JVM tests passed; settings interaction passed on API 15 and API 16 emulators on rerun (initial API 15 attempt failed while locating Settings before exercising the target action); GL surface/picking passed at 50%, 65%, and 100% on both emulators. Physical-device performance benchmarking remains pending. This is a conservative renderer-name heuristic, not a measured GPU benchmark. |
| 2 | Compound 3MF paint ownership | Implemented: each model-part volume owns a stable heap-allocated mesh, selector and AABB raycaster; world-space hit testing returns the source volume. Paint, undo/redo, clear and commit address the selected part without overwriting other parts. | Emulator ARM64 `TwoColorProjectDeviceTest` 5/5 passed, including independent painting of two combined 3MF parts, live overlays, undo/redo, 3MF export/reload and generated G-code with the expected T1 tool change. Physical-device validation remains outstanding. |
| 3 | Qidi Box slot mapping | Implemented: active G-code tools can map to Qidi Box slots 1–16; repeated slots are allowed, unmapped tools retain the printer setting, and confirmed choices persist per printer/host in Nightly-private preferences. Orca 2.5 Qidi agent semantics use `SAVE_VARIABLE value_tN` before `/printer/print/start`; the guarded Moonraker sequence is implemented. | Policy tests 5/5, dialog selection/persistence/cancel emulator tests 2/2, source compilation. Actual Qidi firmware/network behavior remains unverified without a Qidi device. |
| 4 | Orca logical-to-physical extruder map | Implemented: the printer profile exposes Orca's `physical_extruder_map`; the vector editor supplies an identity default, validates one zero-based target per logical extruder, and permits repeated targets. Sparse custom multi-nozzle profiles now skip absent/empty variant vectors instead of dereferencing them during native slicing. | Policy/catalog JVM tests 8/8; Android 16 ARM64 native slice class 51/51; filament/MMU sync regression 15/15. A real MMU/AFC device workflow remains unverified. |
| 5 | Multipart per-part filament reassignment | Implemented: the expanded object list exposes a dedicated filament action for each printable part. The native GL-thread edit changes only that volume, clears only its facet-level material map, refreshes the live mesh, and preserves sibling volumes. | Compose action test 1/1; Android 16 ARM64 `TwoColorProjectDeviceTest` 6/6, including slot swap, 3MF save/reload and native slice. Generic arbitrary process/filament setting overrides remain a later gap. |
| 6 | General modifier volumes | Implemented: the Model menu can add, position, resize and remove Orca-native negative volumes, support blockers and support enforcers on the selected object. Their typed meshes render as translucent, color-coded overlays and remain separate from printable parts. Existing modifier volumes can be inspected, resized and removed. The Orca 3MF importer now accepts the type strings emitted by its own exporter; box geometry is centered consistently so coordinates survive round-trip. | Android 16 ARM64 tests passed: Compose type/creation input 1/1; native add/edit/delete plus undo/redo snapshots and 3MF position/size/type round-trip 1/1; native slice from the reloaded project 1/1, with changed extrusion toolpaths and printable output. Per-volume arbitrary parameter-setting editing is not included in this geometry feature. |
| 7 | SVG surface emboss/engrave | Implemented: SAF selection and bounded/secure validation, width/depth controls, GL surface-tap placement, raised printable part or native negative volume; SVG curve tessellation uses a 0.05 mm target error. | `suppliedSvgCanBeRaisedOrEngravedOnASurfaceAndStillSlicesAfterThreeMfReload` passed 1/1 on API-35 x86_64 emulator with Android's ARM64 native bridge. It checks both modes, 3MF reload and changed extrusion paths. Manual live placement preview/cancel-before-commit remains open; commit-on-tap is undoable. |
| 8 | 3MF transform and parser-size parity | Ported Orca fixes `67a976e0` (preserve valid mirrored matrices in Prusa/BBS importers) and `d35ea27e` (reject oversized XML ZIP entries before Expat allocation). | Mirrored Prusa/BBS round-trip passed 1/1 and oversized ZIP64 XML rejection passed 1/1 on API-35 x86_64 emulator with ARM64 native bridge. |
| 9 | Qidi profile parity | Q2/Q2C profiles use selected filament bed temperature for `PRINT_START`/`M140`, initialize Qidi Box mapping, select the initial logical tool and use material-aware filenames. | Qidi Q2 native slice/G-code test passed 1/1 on API-35 x86_64 emulator with ARM64 native bridge. It verified 67 °C in `PRINT_START`/`M140` and resolved the executable initial-tool command. Profile transmission to a physical printer remains unverified. |
| 10 | Orca profile-option gap refresh | Re-ran the route audit against 887 native options and 492 visible setting rows. Found `physical_extruder_map` in the visible printer catalog but missing from `PRINTER_CONFIG_KEYS`, which controls import/export routing; added it to the printer key set and a persistence/import regression test. Updated the route checker to follow the current Compose render-scale, navigation, workspace sync, color-editor and profile-import APIs instead of stale pre-migration names. | `check_profile_setting_routes.py` passes: all 492 visible rows resolve to native definitions/profile parsers and 107 German group labels are unique. JVM key persistence test passes 1/1. |

### Later candidates

- Review upstream 2.5 fixes in bounded, pinned groups, each with a concrete regression and native
  or device evidence; do not bulk-copy commits or advance the engine lock prematurely.
- Validate frame pacing/FPS cap against Android battery/latency behavior after render-scale
  control is measured. Desktop preference semantics are not presumed to transfer to Android.
- Reassess other Orca desktop functions only after inspecting their native dependencies, Android
  UI feasibility, mobile memory cost, and license/build requirements.

### Latest-upstream feature triage (2026-10-31)

The Orca 2.5.0 Beta notes emphasize toolhead collision volumes, shorter travel moves, rounded
infill corners, translucent-filament preview and slicing performance on detailed/multipart models.
The alpha feature set additionally includes Texture Displacement, experimental parametric Design,
Python plugins, color mixing and IDEX/IQEX parallel printing. These are not all suitable Android
ports: collision geometry needs machine-profile/runtime validation; translucency is a render-only
candidate; rounded infill and travel optimization need native G-code comparisons; Texture
Displacement requires mesh/3MF ownership and memory/cancellation work; the Design tab, arbitrary
Python execution, color mixing and IDEX require substantial independent subsystems or hardware
contracts and stay deferred until those prerequisites exist.

The fetched `main` delta after `5cc5fb7f` is profile/localization work rather than new engine code.
New vendor profiles are useful only if the Android setup wizard can consume their profile index and
inheritance chain; do not count copied JSON assets as a working printer feature without testing
selection and slicing. Keep the current implementation order: complete the open native regression
gaps in batch 2, then prototype a bounded, cancelable Texture Displacement path; investigate
translucent preview and collision-volume support as separately testable follow-up candidates.

### OrcaSlicer 2.5 Alpha capability review (2026-10-31)

The official 2.5.0 Alpha notes describe a large desktop release, not a drop-in Android package.
The following candidates are ranked by whether they can safely use the existing native engine and
mobile interaction model. Do not count a candidate as implemented until its Android action and
regression evidence exist.

| Upstream capability | Android evidence / feasibility | Port decision |
|---|---|---|
| Texture Displacement | Native mesh displacement and texture sampling could run on Android, but it can greatly increase triangle count and RAM. | High-value later candidate; prototype bounded resolution, cancellation, memory guard, undo/redo, and slice parity before exposing. |
| Parametric Design tab | Requires a sketch/CAD constraint solver, feature tree, replay, and 3MF persistence; it is a separate CAD subsystem. | Do not imitate with static mesh generators. First scope solver dependencies and an independently versioned parametric project format. |
| Python plugins | Arbitrary code, filesystem, network, and process access create a large security and lifecycle surface on Android. | Not a direct port. Consider only a sandboxed, permissioned extension API after threat modeling; no arbitrary plugin execution in this plan. |
| Color mixing | Depends on compatible multi-input hardware and firmware semantics, unlike ordinary single-nozzle MMU tool changes. | Defer until a supported mixer hardware profile and firmware-safe G-code contract are identified. |
| IDEX/IQEX parallel printing | Requires independent carriage collision planning and printer-specific G-code/start sequencing. | Defer pending a complete, testable dual-carriage printer profile; never infer support from generic extruder count. |
| Belt printers | Coordinate transforms, bed geometry, and print sequencing differ from planar beds. | Defer until belt printer machine limits and G-code are ported as a coherent profile family. |
| Speed Dial / action launcher | Mobile already has searchable menus and configurable bottom actions; parity should not duplicate those entry points. | Revisit only if the menu audit finds a measured discoverability gap. |
| Faster startup, slicing, and G-code export | Upstream contains many independent native changes and fixes. | Port individually against pinned commits with Android ARM64 regression tests; do not mass-update the engine lock. |
| Precise Seam modifiers and new infill options | Some native engine support may already exist; UI coverage and serialization need verification. | Audit current keys/enum paths first, then expose only safe, tested gaps in backlog item 10. |
| Mixed nozzle sizes / new printer support | Depends on per-tool nozzle geometry, profile definitions, and G-code validation. | Treat as profile-family work with fixture-based export tests, not a generic UI toggle. |

Primary upstream source: [OrcaSlicer V2.5.0 Alpha release notes](https://github.com/OrcaSlicer/OrcaSlicer/releases).
The existing backlog items 7–10 remain the active implementation sequence; this review adds candidates
for prioritization after those items and is not itself counted as a shipped feature.

## Delivery cadence and gates

1. Keep a numbered implementation log for the ten backlog items above.
2. For each item, inspect pinned upstream source and Android dependencies, implement only a safe
   adaptation, and add focused regression tests.
3. After each group of ten completed feature items, run grouped JVM/native/device verification,
   create a Nightly APK, inspect package identity and APK metadata/hash, then publish a Nightly
   GitHub release and update the changelog. Do not claim unsupported device tests passed.
4. Task #1 has code, policy/settings-flow tests and a live Android 16 emulator interaction: GL surface
   size and M12 ray picking passed at 50% and 100% render scales; physical-device check is pending. Task #2 passes
   `TwoColorProjectDeviceTest` 5/5 on the Android 16 ARM64 emulator, including distinct-volume
   painting, undo/redo, 3MF reload and generated G-code with the expected T1 tool change. Task #3
   passes mapping policy tests 5/5 and Qidi dialog emulator tests 2/2 (selection, persistence, cancel);
   the upstream `SAVE_VARIABLE` command and fail-closed-before-print path compile, but a live Qidi
   firmware/network exchange is not verified. Three items (#1–#3) have implementation and focused emulator coverage (3/10); physical-device
   and physical-printer validation remain outstanding.
   Items #4 and #5 are implemented and covered by focused tests: 5/10 feature items. The physical extruder-map work passed policy/catalog JVM 8/8, `NativeSliceDeviceTest` 51/51, and filament/MMU sync 15/15; the part-level assignment passed its Compose action test 1/1 and `TwoColorProjectDeviceTest` 6/6 on Android 16 ARM64, including 3MF round-trip and native slicing. Generic arbitrary process/filament overrides and real MMU/AFC device validation remain open. No APK or release is created from this partial batch.

## Source audit documents

- `docs/ORCA_FEATURE_AUDIT.md` — profile and model-tool inventory; update stale rows when the
  current source proves the implementation exists.
- `docs/ORCASLICER_2_5_DELTA_AUDIT.md` — pinned upstream fixes and Android-port constraints.
- `ORCA_NIGHTLY_LOCK.md` — do not advance until engine identity and native test evidence justify it.

## Next Orca 2.5 parity batch (items 11–20)

Candidate selection is based on upstream commits present in `orcaslicer-upstream/main` as of
2026-10-31. These are scoped targets, not completion claims. Each requires Android-specific
regression evidence before it can count toward an APK/release batch.

| # | Upstream change | Android adaptation / acceptance evidence | State |
|---|---|---|---|
| 11 | `7ac46c9a6` Lightning overhang samples outside the infill bounds can make tree generation loop indefinitely. | Ported in `Fill/Lightning/DistanceField`: expand the searchable box to all unsupported sample points. In addition to the Bunny regression, an API-35 instrumented test now slices a stepped prism with a 10 mm stem and a 20 mm cap, deliberately placing the opened overhang beyond the stem's infill bounds; slicing must finish within the 90 s test limit and emit Lightning extrusion. Passed 1/1 on each of two API-35 emulators. | Ported; targeted outside-infill-bounds geometry runtime regression passed on two emulators |
| 12 | `9b4f84a92` Preserve every extruder's values when a custom printer has more nozzles than its parent. | Native preset code extends inherited nozzle vectors in local/system, project, and cloud paths. Android currently loads flattened profile `.ini` data and has no active `PresetBundle` call site; the source port compiles, but no live Nightly app route exercises this behavior. | Core port present; not counted as Android feature until an app call path exists |
| 13 | `fcd96ad91` Apply each object's own extruder layer-height limits and compare actual layer heights for prime-tower compatibility. | Corrected zero-based object extruder to one-based nozzle lookup. API-35 ARM64-bridge test slices two objects on nozzles with different min/max limits and the same actual layer sequence through one prime tower; deliberately unequal adaptive profiles remain correctly incompatible. | Implemented and runtime-verified (1/1) |
| 14 | `a4a510087` Keep build-plate-only tree supports inside XY clearance. | API-36 ARM64 slice of a synthetic 10 mm square stem / 20 mm cap. The test excludes stationary E-only prime commands, measures every positive-length support extrusion between Z+0.5 and Z+9.5 against the exact analytic stem cross-section, and asserts no path crosses the solid. Minimum path-centerline gaps at configured 0.2 and 0.8 mm were 0.4845 and 0.6370 mm (+0.1525 mm); 1/1 passed. This proves cross-section non-intersection and a monotonic response, not that the deposited bead surface maintains the full configured clearance. | Existing port; exact synthetic model cross-section path check passed 1/1 on API 36; deposited-bead clearance remains open |
| 15 | `98b539c3e` Use the printing filament's retraction-lift settings. | Android reads thresholds/Z-hop and enforcement from the active logical filament ID. Two-filament temporary-project G-code regression passed 1/1 on API-35 with ARM64 bridge; its parser excludes the normal end-of-print head-raise command. | Equivalent path implemented and runtime-verified (1/1) |
| 16 | `b7ecf1b29` Avoid false out-of-printable-range warnings for an extruder area inset on multiple sides. | Added a JNI diagnostic getter for the native G-code validation error bitmask and an API-35 ARM64-bridge slice regression using a real tool-changer profile. The test slices the model inside a small inset toolhead polygon without the toolhead-area warning, then moves that polygon away and confirms the same mapped toolhead is rejected. | Existing port; asymmetric area false-positive and positive-control runtime test passed (1/1) |
| 17 | `29e189957` Ensure sparse layers exist during a tool change. | Both Android wipe-tower generators restore model Z before the travel-capable macro. The compacted-tower multicolor G-code regression passed 1/1 on API-35 with ARM64 bridge, checking toolchange Z against the active model layer. | Implemented and runtime-verified (1/1) |
| 18 | `4c7928d8a` Decide prime-tower need from the current model's filament set rather than the preceding model. | `Print::apply` now retains pre-normalized settings and re-evaluates after current model regions/filaments are rebuilt, so normalization can re-enable the tower. ARM64 native build and instrumented Kotlin compilation pass; a reused-Print runtime regression is still needed. | Ported; runtime test open |
| 19 | `59f0cc3806ab6424d081978fde331088c55d9f0f` Avoid indexing empty layer lists for objects too thin to create any layer when a raft is enabled. | Both guards are present in brim grouping and precise-Z layer alignment. The raft 0/2 × precise-Z off/on regression returned the expected normal “No layers were detected” error instead of crashing; passed 1/1 on API-35 with ARM64 bridge. | Equivalent guard runtime-verified (1/1) |
| 20 | `d28c71c48` Preserve enum-list config keys in default maps. | JNI serializes normalized `coEnums` defaults through `ConfigOptionDef::create_default_option`; enum values and nullable `nozzle_type=undefine` survive the native catalog, and a basic native slice still emits extrusion. Passed 1/1 on API-35 with ARM64 bridge. | Ported and runtime-verified (1/1) |
| 21 | `27cafaa06` Read nozzle diameters correctly with a decimal-comma UI locale. | Desktop-only nozzle/device UI routes are absent from Android. In the shared Bambu 3MF parser, replace locale-sensitive `atof` with `string_to_double_decimal_point`. The Android importer already enters a C numeric-locale guard, so this is explicit format-parsing parity and defense in depth, not a demonstrated Android locale bug. ARM64 native build passes; no dedicated import fixture assertion yet. | Defensive parity port; not counted as a completed feature group |
| 22 | `7b6c12cf7` Match grouped filaments to those loaded in dual-nozzle printers. | Relevant only after loaded physical-tray identity reaches the Android `Print` object. Current `MachineFilamentInfo` has no filament ID; `build_full_machine_filaments` reads color/type/tray name only; `Print::set_extruder_filament_info` has no Android call site. The model-side `filament_ids` already exist, but that alone cannot match to loaded trays. Defer algorithm changes until this data path and a generated-tool regression are implemented. | Audited; plumbing prerequisite missing, no partial port |
| 23 | `8e37d7d4b` Match closer colours and the same filament subtype in Convenience mode. | Latest upstream delta since `7ac46c9a6`. This modifies the color-distance assignment solver to prefer matching `filament_id` subtypes only after color distance, without letting subtype outweigh a color-distance step. Android's solver has neither machine-slot IDs nor a populated printer-filament list in `Print`; make the data path and a deterministic multi-tray G-code regression prerequisite. | Audited; blocked on loaded-tray data plumbing |
| 24 | `aa66e41e6` Group a filament onto the nozzle that has it loaded in Filament-Saving mode. | Latest upstream delta since `7ac46c9a6`. Candidate generation uses loaded per-nozzle trays and flush-cost budget to prefer fewer nozzle changes. Android exposes a model-side logical-to-physical map, but that is not loaded-tray identity. Requires real per-nozzle filament configs, test fixtures, and generated tool-sequence assertions before porting. | Audited; blocked on loaded-tray data plumbing |

### Current continuation — 2026-10-31

- The low-GPU viewport option is already implemented: automatic device policy uses low-RAM and known renderer hints; users can override it in App settings. It changes only GL surface resolution and preview mesh budgets, not slice geometry. Policy JVM tests and live emulator touch-coordinate checks are recorded in `UI2_EXECUTION_STATE.md`; physical GPU benchmarking remains open.
- Orca 2.5 candidates 11, 13, 14, 15, 17, 19, and 20 have focused runtime evidence using the emulator's ARM64 native bridge. Candidate 11's direct probe/bounds assertion is additionally covered by API-36 tests. Candidate 14 now checks the exact analytic cross-section of its synthetic solid and verifies that no positive-length tree-support extrusion enters it; the deposited-bead surface gap remains unverified. Candidate 15's initial failure was a test false positive: the end-of-print head raise was being counted as an extrusion Z-hop. Candidate 13's deliberately different adaptive profiles were correctly rejected; its final regression checks unequal nozzle limits with the same actual layer sequence.
- Candidate 16's asymmetric inset-area false-positive and excluded-area positive control now pass through the real Android native slice path (1/1). The test-only evidence uses a native validation error bitmask getter exposed to Kotlin as a general slice diagnostic. Candidate 18's `Print::apply` re-normalization port compiles, but Android's JNI creates a new `Print` for each `model_slice` call; a reused-Print runtime scenario does not currently occur in the app. Candidate 12's preset inheritance similarly has no active Android `PresetBundle` call path.
- Candidate 19 is the thin-object/raft guard, not mixed-filament validation. Orca's separate mixed-filament component-expansion fix is outside current scope because this Android fork has no mixed-filament composition model or `FilamentMixer`; it must not be partially ported as a bed-check-only change.
- ARM64 native builds, focused instrumentation, and the complete Debug/Slice JVM suites pass. On 2026-10-31, both JVM suites passed 704/704 with 0 failures, 0 errors, and 0 skips; the focused #16 device regression passed 1/1 on API 35 x86_64 with ARM64 translation. Physical GPU benchmarking and direct Qidi firmware/Moonraker communication remain hardware-only follow-ups.
- Orca upstream advanced to `27cafaa06` (2026-10-31). Candidate #21's locale-safe nozzle parser is applied to the shared Bambu 3MF reader; its existing outer `CNumericLocalesSetter` already protects Android import, so the change is defensive parity and does not count as a newly verified feature. ARM64 native build passed; direct metadata round-trip coverage remains open.
- Next: refine #14 from a sampled G-code wall-path median to the exact minimum clearance against the adjacent model cross-section. #11's exact sample-outside-original-bounds and inclusion-in-expanded-bounds invariant now has a direct native probe as well as the production Lightning slice regression. Do not count #12 or #18 as app-visible feature completions without an Android call path; replace unsuitable candidates from the pinned upstream backlog before the ten-item APK/release gate.
- Additional focused API-35 ARM64-bridge runtime evidence: `NativeSliceDeviceTest#lightningInfillCompletesOnOverhangRichBunnyFixture` passed 1/1 after explicitly placing the imported Bunny on the bed; generated G-code contains sparse-infill extrusion. `#buildPlateOnlyTreeSupportsSliceWithConfiguredObjectClearance` passed 1/1 and verified that changing build-plate-only tree-support clearance from 0.2 to 0.8 mm changes generated support extrusion paths. A first AVD attempt ended when the emulator went offline; the repeat passed after restarting the Android-15 ARM64-translation AVD. One initial Lightning attempt exposed an improperly placed test fixture and was corrected with `ensureOnBed(0)`. These failed attempts are not counted as passes.
- The Lightning test now directly asserts that the expanded bounds include samples outside the original infill bounds, in addition to exercising actual native slicing. The tree-clearance test still does not measure minimum support-to-model mesh distance; keep that narrower diagnostic open.

After each ten-item group reaches its explicit code and runtime gates, build and inspect the isolated
Nightly APK and publish the corresponding Nightly GitHub prerelease. Items 1–10 now meet their
defined test gates; hardware follow-ups are not represented as tested.

## Latest upstream delta reviewed — 2026-10-31

`orcaslicer-upstream/main` now points to `5cc5fb7f9c40c42ca47fc13969756c4e53119046`. Its three
new commits were checked individually:

- `5cc5fb7f9` stops Bambu LAN SSDP discovery before destroying the desktop network agent. Nightly has
  no Bambu desktop plugin or equivalent agent lifecycle; its Moonraker path has no matching API.
- `913c62257` remembers printers per desktop printer-agent and keeps AMS system filaments visible.
  The desktop `DeviceManager`/`PresetBundle` path is absent on Android, so no direct port is warranted.
  The related Android MMU state-isolation gap is handled by the adaptation below.
- `7af399554` links libjpeg for the Linux Arch build and does not apply to Android.

The upstream reference was refreshed; the native engine lock was not advanced. Each Android port
still needs an app route and its own runtime evidence.

## Android adaptation — per-printer MMU synchronization state (2026-10-31)

| # | Upstream basis | Android change and evidence | State |
|---|---|---|---|
| 25 | `913c62257` printer-specific persistence concept | MMU synced markers, selected tool-to-gate choices, palette-to-gate order, and Moonraker snapshots are stored under SHA-256 preference keys derived from the printer profile title. Existing single-printer keys remain as migration fallbacks and are copied to scoped keys on read. The global filament palette remains the user's active palette; only printer-specific MMU metadata is scoped. `MmuFilamentSyncDeviceTest` passed 16/16 on each of two API-35 emulators, including A→B→A switching with distinct gate choices and snapshots. `FilamentPaletteSyncPolicyTest` passed 10/10; Android test Kotlin compilation and `git diff --check` passed. | Implemented and emulator-verified; physical-printer validation remains open |

## Orca `304924a69` — Default-Zuweisung für Feature-Filamente (2026-10-31)

Der upstream-Fix entfernt die automatische Kaskade, durch die das Auswählen eines Infill-/Oberflächen-Filaments auch andere Features vom Default-Objektfilament wegzog. Der Nightly-Kern enthält die entsprechende Korrektur bereits in `6d8f78d9d`, und dieser Commit liegt vor dem Release-Commit `0db4f11d3` für `v0.8.420-nightly.1`; sie ist daher keine neue Implementierung für den folgenden Zehner-Batch. Der vorhandene Android-Regressionstest `oneFeatureFilamentOverrideDoesNotCascadeToOtherInfillOrSurfaceFeatures` wurde am 2026-10-31 erneut auf Android 16 ausgeführt: 1/1 bestanden, einschließlich nativer Slice-Ausgabe und Prüfung der vier Feature-T-Werkzeuge. Diese Prüfung bestätigt die G-Code-Zuweisung für die getesteten Standard-/Feature-Fälle; sie ersetzt keine Prüfung einer beliebigen privaten farbigen 3MF-Datei oder physischer MMU-Slotzuordnung.

## Orca `ffdb5da10` — BenFab 450 Druckerprofile (2026-10-31)

Übernommen wurden Orcas BenFab-450-Maschinenfamilie mit 426×426×480-mm-Druckbereich, Klipper-Start-/End-G-Code, vier Düsenvarianten (0,4/0,6/0,8/1,0 mm) und 18 dazugehörigen Prozessprofilen. Die Profile werden wie die anderen Vendor-INIs vom Setup-Assistenten automatisch eingelesen. Der Instrumentierungstest prüft alle vier Varianten und führt für jede einen nativen Slice aus; der erzeugte G-Code muss Extrusion und die konfigurierte `G28 ;home`-Startsequenz enthalten. Der gezielte Lauf bestand auf API 35 und API 36 (je 1/1 Test; vier Varianten pro Testlauf). Das ist Profil-/Slice-Parität, kein Test mit einem physischen BenFab-Drucker. Orcas zugehöriges Bed-Mesh/Cover ist derzeit nicht Teil der App-Assets; die Android-Workspace-Druckplatte nutzt die native `printable_area`-Geometrie.

## Nächster APK-Batch nach `v0.8.420-nightly.1`

Der zweite Nightly-Batch steht nach dem BenFab-Port bei **4/10** eigenständigen Produktänderungen:

1. `24ee77b1f` — Orca-Enumlisten-Defaults in den Android-Profilkatalog übernehmen.
2. `a8dd87ba4` — MMU-/Druckersynchronisierungszustand je Druckerprofil isolieren.
3. `8e5c25773` — Low-GPU-/Low-RAM-Vorschau anpassen, ohne Modell oder Slice zu verändern.
4. `6899a7fed` — BenFab-450-Profile einschließlich vier echter Slice-Varianten ergänzen.

## Orca `eac3ba441` — BLOCKS RF50 0,6-mm-Start-G-Code (2026-10-31)

Der in Orca reparierte Profilfehler war auch in `orca_profiles/Blocks.ini` vorhanden: `SET_PRESSURE_ADVANCE` stand hinter einem Semikolon in der `PRINT_START`-Zeile und war damit ein G-Code-Kommentar. Die Android-Profilzeile trennt die Befehle jetzt durch einen Zeilenumbruch. `NativeSliceDeviceTest#orcaBlocksRf50SixTenthNozzleKeepsPressureAdvanceAsExecutableGcode` lädt das echte RF50-0,6-mm-Profil, slice't einen Testwürfel und prüft, dass `SET_PRESSURE_ADVANCE ADVANCE=0.046` als eigene ausführbare G-Code-Zeile und nicht innerhalb des Kommentars erzeugt wird. API 35 und API 36: je 1/1 bestanden. Kein physischer RF50-Drucker wurde getestet.

Der Batchstand nach diesem Fix beträgt **5/10**. Änderung 5: `6dacbf757` macht den Pressure-Advance-Befehl im RF50-0,6-mm-G-Code ausführbar. Noch fünf unabhängig verifizierte Änderungen bis zur APK-/Release-Schwelle. Es wurde keine APK aus dem Teilbatch gebaut oder veröffentlicht.

## Orca `3ca96a089` — unabhängige 3D-Fuel-Filamentbibliothek (2026-10-31)

Orca hat 13 3D-Fuel-Materialprofile ergänzt. Nightly übernimmt sie als `app/src/main/assets/orca_profiles/3D-Fuel.ini`, ohne einen nicht unterstützten 3D-Fuel-Drucker vorzutäuschen. Das Vendor-Metadatum `filament_library = 1` lässt den Setup-Assistenten die Profile auch dann anbieten, wenn ein anderer Drucker ausgewählt ist; normale druckerspezifische Filamentvendors bleiben weiterhin an die Druckerauswahl gebunden.

`SetupProfileSelectionPolicyTest#independentMaterialLibrariesAreShownWithoutSelectingTheirPrinterVendor` prüft beide Auswahlregeln. `NativeSliceDeviceTest#orca3DFuelMaterialLibraryLoadsIndependentlyAndSlicesRepresentativeFilaments` lädt das echte Bibliotheksasset, prüft alle 13 Profile und slicet Standard-PLA, Pro-PETG und PET-CF mit einem isolierten Anycubic-Testprofil. Der G-Code muss Extrusion und die jeweilige Temperatur im ausführbaren `G9111`-Startmakro enthalten. API 35 und API 36 bestanden je 1/1; der gemeinsame Lauf mit dem Policy-Unit-Test endete `BUILD SUCCESSFUL`. Keine physischen 3D-Fuel-Drucke.

Damit beträgt der zweite Nightly-Batch **6/10**. Die nächste APK wird erst nach vier weiteren unabhängigen, fokussiert verifizierten Produktänderungen gebaut. Die Nightly-App-ID `com.exa201.orkanslicer.nightly25` und der Anzeigename `OrkanSlicer Nightly 2.5` sind bereits gegenüber der Stable-App getrennt; ihre privaten App-Daten/Einstellungen werden nicht übernommen.

## Orca `efd65b733` — OpenEYE Peacock V2 Moonraker-Host (2026-10-31)

Orca änderte den OpenEYE-Peacock-V2-Host von OctoPrint auf Moonraker. Im Android-Profil fehlte `host_type` bisher; OrkanSlicer fiel dadurch auf den OctoPrint-Standard zurück, obwohl die Maschine Klipper nutzt. Alle vier Düsenvarianten (0,2/0,4/0,6/0,8 mm) geben nun explizit `host_type = moonraker` vor. Das ist der von Androids Upload-Auswahl verwendete Wert; das desktopbezogene `printer_agent` wird nicht als Android-Verhalten portiert.

`NativeSliceDeviceTest#orcaOpenEyePeacockV2ProfilesUseMoonrakerAndSlice` lädt das echte Profilasset, prüft alle vier Host-Zuordnungen gegen die Upload-Policy und slicet mit 0,4-mm-Düse und Generic PLA. Extrusions-G-Code und `START_PRINT` müssen erhalten bleiben. API 35 und API 36 bestanden je 1/1. Ein erster Testentwurf prüfte zusätzlich das hier nicht verwendete `printer_agent`-Metadatum und scheiterte an dieser Testannahme; nach Ausrichtung auf den tatsächlichen App-Datenpfad bestanden beide Geräte.

Der zweite Nightly-Batch steht damit bei **7/10**. Drei weitere unabhängig verifizierte Produktänderungen sind erforderlich, bevor die APK gebaut und als Nightly-Prerelease veröffentlicht wird.

## Orca `efd65b733` — OpenEYE Peacock V2 Prozessprofil-Parität (2026-10-31)

Die bestehenden 13 OpenEYE-Prozessprofile übernehmen nun die Werte aus Orcas aktualisiertem gemeinsamen bzw. OpenEYE-spezifischen Prozessprofil: deaktivierte Fuzzy Skin als expliziter Default, Fuzzy-Abstands-/Dickenwerte, Erstschicht-Reisebeschleunigung 6000, deaktivierte bedingte/Innenwand-Seam-Slope mit 10 mm Mindestlänge und 10% Start-Höhe sowie Support-Ironing-Abstand 0,15 mm. Die Einstellungen sind an allen 13 Profilen vorhanden, statt nur an einem einzelnen Düsentyp.

Der bestehende `orcaOpenEyePeacockV2ProfilesUseMoonrakerAndSlice`-Ende-zu-Ende-Test prüft diese Werte für alle 13 Prozessprofile, kontrolliert die generierte native Konfiguration und slicet anschließend auf dem 0,4-mm-Profil. Extrusions-G-Code und `START_PRINT` bleiben vorhanden. API 35 und API 36 bestanden je 1/1.

Der zweite Nightly-Batch steht jetzt bei **8/10**. Zwei weitere geprüfte Produktänderungen sind erforderlich, bevor die APK gebaut und als Nightly-Prerelease veröffentlicht wird.

## Orca `efd65b733` — OpenEYE modellbezogene G-Code-Dateinamen (2026-10-31)

Alle 13 OpenEYE-Prozessprofile verwenden jetzt Orcas Dateinamentemplate mit Modellname (oder STL-Basisname als Fallback), optionalem Plattennamen, Materialtyp und Druckzeit. OrkanSlicer übernimmt den resultierenden nativen empfohlenen Dateinamen für Senden/Export bereits über `GCodeProcessorResult.getRecommendedName()`; daher wirkt die Profiländerung unmittelbar auf den Nutzerpfad.

Der instrumentierte OpenEYE-Slice prüft den tatsächlichen empfohlenen Namen: Modellbasis vorhanden, Material `PLA`, nichtleerer Druckzeitanteil, `.gcode`-Endung und keine ungelösten Platzhalter. Zusätzlich müssen Extrusionswege und `START_PRINT` bestehen bleiben. API 35 und API 36 bestanden je 1/1.

Der zweite Nightly-Batch steht damit bei **9/10**. Eine weitere abgeschlossene Änderung vor dem nächsten APK-/Nightly-Prerelease-Build.

## Orca `f273fe8ac` — tatsächlich verwendete Materialtypen im Dateinamen (2026-10-31)

Der Orca-Platzhalter `{printing_filament_types}` wird jetzt im nativen Slice-Export befüllt: verwendete Werkzeug-/Filamenttypen werden in Tool-Reihenfolge gesammelt, doppelte Typen entfernt und mit Unterstrichen verbunden. Er steht sowohl im Placeholder-Kontext für G-Code-Templates als auch in den fertigen Print-Statistiken bereit. Alle 13 OpenEYE-Prozessprofile verwenden ihn nun statt nur den Typ des Startwerkzeugs; Mehrfarben-Dateinamen können dadurch beispielsweise `PLA_PETG` enthalten.

`NativeSliceDeviceTest#recommendedFilenameListsDistinctFilamentTypesActuallyUsedByThePlate` baut ein zusammengesetztes Zweifilamentmodell, setzt für den Slice kontrolliert `PLA` und `PETG`, prüft echte Extrusion und den nativen empfohlenen Dateinamen und wiederholt den Slice mit zweimal `PLA`, um die Deduplizierung nachzuweisen. API 35 und API 36 bestanden je 1/1. ARM64-Native-Build und Instrumentierungs-Kotlin-Kompilierung bestanden ebenfalls.

Damit ist Batch 2 bei **10/10 fokussiert verifizierten Änderungen**. Vor APK und Nightly-Prerelease laufen jetzt noch die gebündelten Debug-/Slice-JVM-Regressionen; danach folgen APK-Identitäts-/Hashprüfung, Installation und Start auf dem verfügbaren Testemulator sowie Veröffentlichung.

## Nightly Batch 4 — 10/10 implementation complete (2026-11-01)

1. Orca `2e64ab3db5` — Beschleunigung der Brim-Gruppierung für zusammengesetzte Objekte mit vielen Volumenteilen. Die Android-Portierung erstellt nach dem Offset pro Layer Begrenzungsboxen und führt Polygonüberschneidungen nur bei überlappenden Boxen aus. Die redundante TBB-Parallelisierung für jedes einzelne Volumenpaar entfällt; der serielle Paar-Scan beendet sich beim ersten Treffer. Das 12-Teile-Instrumentierungsfixture slice't auf API 35 erfolgreich und erhält beide Materialzuweisungen sowie Schicht-G-Code (1/1). Der bestehende Zwei-Teil-3MF-Roundtrip-/Slice-Test und der auf den Slot-Konfigurationsparser verstärkte Auto-for-Match-Test bestanden ebenfalls (je 1/1). ARM64-Native-Build, Android-Instrumentierungs-Kompilierung und `git diff --check` bestanden. Der Android-Lauf ist eine Funktions-/Regressionsprüfung, kein kontrollierter Vorher-Nachher-Leistungstest; ein absoluter Speedup wird nicht behauptet. APK/Release erst nach 10/10.

Der strengere Auto-for-Match-Test baut jetzt synthetische Drucker-Slot-`DynamicPrintConfig`s und ruft `build_machine_filaments()` auf, statt `MachineFilamentInfo` direkt auszufüllen. Das prüft den realen nativen Slot-Parser für `filament_colour`, `filament_type`, `tray_name` und `filament_id`. Es ist Testverstärkung, keine neue Produktfunktion. Der Android-App-Pfad übergibt weiterhin keine geladenen Maschinenfilamente an das native `Print`; deshalb gilt die Orca-Dual-Nozzle-Ladezuordnung nicht als appweit integriert. Orcas Kandidaten 22–24 bleiben bis zu einer passenden, tatsächlich genutzten Druckerroute zurückgestellt.

2. Orca `b1846e3408` — einstellbare obere und untere Farbdurchdringung für gemalte Modellflächen. Die neuen Prozessprofiloptionen `top_color_penetration_layers` und `bottom_color_penetration_layers` reichen bis in die native `PrintRegionConfig` und Mehrmaterial-Segmentierung. Der Standardwert `0` erhält bisheriges Verhalten und folgt der jeweiligen Schalenstärke. Änderungen invalidieren den Slice; beide Optionen sind im vorhandenen Abschnitt „Obere / untere Schichten“ editierbar.

`OrcaProcessSettingsCatalogTest#paintPenetrationControlsAreExposedWithTopAndBottomShellSettings` bestand zusammen mit dem Katalog 10/10. `NativeSliceDeviceTest#paintedTopAndBottomColorPenetrationChangesTheActualSlicedToolpathDepth` bemalt gezielt nur obere bzw. untere Flächen und vergleicht echte G-Code-Werkzeuglayer bei 2 gegen 8 Penetrationslagen; API 35 bestand 1/1. Initial zeigte ein ungenauer Höhenmal-Fixture auch angrenzende Seitenfacetten an; der Test wurde auf Raycast plus coplanaren Flächen-Bucket umgestellt und besteht nun für Ober- und Unterseite. Kein Druckstart.

3. Orca `0ad175c42b` — dichte interne Brücken werden über `diff_expanded()` beschleunigt; bei nicht vollständig dichten unteren Bereichen bleibt die genaue Geometrieberechnung erhalten. API-35-Dichtefixture und API-16-Rückfallfixture bestanden.

4. Orca `e8d35fadd4` — interne Brücken von Hilbert-Curve- und Octagram-Spiral-Infill folgen der tatsächlichen Rotation der tragenden Schicht. API-16-Test maß die resultierenden G-Code-Brückenrichtungen mit 0°/45° Rotation und bestand.

5. Orca `25fd549d17` — mehrliniges Infill verwendet Clipper2-Miter-Verbindungen. Der API-16-Vergleich ergab 11.427 runde gegenüber 7.461 Miter-Sparse-Extrusionsbewegungen; der gezielte Regressionstest bestand 1/1. Commit `dabcf408c7` ist gepusht.

6. Orca `546b9c621d` — 17 Qidi-Q1-/X-CF-Pro-/X-Max-3-/X-Plus-3-/X-Smart-3-Varianten heizen nach dem ausgewählten Plattentyp statt fest nach der High-Temp-Platte. `QidiProfileFilenameTest` bestand 3/3 und prüfte alle betroffenen Startmakros. Der native Q1-Slice auf API 16 verwendete bei gewähltem Textured PEI exakt 71 °C (`PRINT_START BED=71`, `M140 S71`) und nicht die Cool-Plate-Temperatur 31 °C (1/1). Q2/Q2C waren bereits korrekt und blieben unverändert.

7. Orca `a30ca5e54a` — By-Object-Druck ordnet hohe Modelle anhand tatsächlicher Y-Überlappung und Querträger-/Deckelhöhenbedingungen um. Sichere ursprüngliche Reihenfolgen bleiben erhalten; für notwendige Änderungen wird eine Warnung erzeugt, bei unlösbaren Bedingungen bleibt die bestehende Kollisionsvalidierung maßgeblich. Der native G-Code-Test mit absichtlich zuerst gelistetem hohen Modell prüft, dass ein kurzes überlappendes Modell zuerst und das hohe Modell danach ausgegeben wird. API 36 Emulator 1/1 bestanden. Xiaomi 2211133G: Testinstallation durch Android blockiert (`INSTALL_FAILED_USER_RESTRICTED`).

8. Orca `373ffaa13f` — Arc-Fitting-Modus erweitert: aus, überall oder nur Sparse-Infill. Alte boolesche Profilwerte werden vor dem Android-Enum-Fallback migriert, damit vorhandene Profile ihre bisherige Ein/Aus-Wirkung behalten.

Verifikation Änderung 8: ARM64-Native-Build erfolgreich; `NativeSliceDeviceTest#sparseInfillArcFittingDoesNotFitWallsAndLegacyBooleanProfilesRemainCompatible` auf API 36 bestanden (1/1), `NativeEnumValueSanitizerTest` 5/5. Der Test liest den echten G-Code und bestätigt, dass im Sparse-only-Modus ausschließlich `Sparse infill` G2/G3-Bögen ausgibt; alte Werte 1/0 erhalten die vormalige volle Ein/Aus-Wirkung. Der erste Testdurchlauf fand einen Android-Sanitizer-Compatibilitätsfehler, der vor dem erfolgreichen Lauf behoben wurde. Xiaomi-Installation bleibt wegen `INSTALL_FAILED_USER_RESTRICTED` ungeprüft.

9. Orca `01ae2dd49a` — bei monotoner Oberflächenfüllung werden Gap-Fill-Pfade anhand der kürzesten Anschlusswege zwischen Oberflächenlinien einsortiert; Pfadorientierung wird bei Bedarf umgedreht. Der Android-Port bleibt auf `no_sort`-Ausgaben beschränkt und behält die vorherige Reihenfolge als Rückfall, falls greedy Interleaving keinen kürzeren Weg erzeugt. Ein echter API-36-Slice mit Orcas A-Form-Fixture bestätigt unveränderte Oberflächenlinien-Endpunkte und Gap-Fill zwischen benachbarten Oberflächenlinien (1/1).

10. Flow-Ratio-Validierung — verwendete Filamente mit Flow-Ratio `<= 0` oder ungültigem NaN-Wert werden vor G-Code-Erzeugung mit einer verständlichen filamentbezogenen Validierung abgewiesen. Die E-pro-mm-Rechnung verwendet nun direkt den Querschnitt, sodass eine Nullrate nicht mehr den bisherigen `0/0`-Pfad erzeugt. API-36-Instrumentierung bestätigt die verständliche Ablehnung und dass kein NaN-/Overflow-G-Code geschrieben wird (1/1).

Verifikation der Änderungen 9 und 10: ARM64-Native-Build, Slice-Instrumentierungskompilierung und Instrumentierungs-APK erfolgreich; beide gezielten Tests auf dem API-36-Emulator bestanden. Xiaomi 2211133G war erreichbar, Android verweigerte jedoch die Test-APK-Installation erneut mit `INSTALL_FAILED_USER_RESTRICTED`; keine physische Xiaomi-Verifikation behauptet. `git diff --check` sauber. Die vollständigen Debug-/Slice-JVM-Suiten und anschließendes Nightly-APK-/Prerelease-Gate sind noch offen.

## OrcaSlicer 2.5.0 Beta — Portierungsprioritäten

Die offizielle Beta nennt Werkzeugkopf-Kollisionsvolumen, kürzere Travel-Wege, abgerundetes
Sparse-Infill, Sparse-only Arc-Fitting, hochpräzisen G-Code, transparente Filamentvorschau und
schnelleres Slicing. Die Alpha ergänzt Texture Displacement, parametrisches Design, Python-Plugins,
Farbmischung, Belt- und IDEX/IQEX-Druck.

| Priorität | Kandidat | Android-Bewertung | Nächster Nachweis |
|---|---|---|---|
| P1 | Kürzere Travel-Wege | Native Android-Slice-Strecke vorhanden; G-Code ändert sich bewusst und braucht Bewegungs-/Kollisionsregressionen. | Portumfang aus Beta-Commits abgrenzen, dann Vorher-/Nachher-G-Code auf repräsentativen Ein- und Mehrmodellplatten vergleichen. |
| P1 | Werkzeugkopf-Kollisionsvolumen | Hoher Nutzen, benötigt Profildefinition, 3D-Vorschau, Arrange-, Support- und Travel-Integration; nur als vollständige Funktion portieren. | Native Android-Aufrufkette und Datenmodell erfassen; keine Teilportierung nur in Arrange zählen. |
| P2 | Abgerundete Sparse-Infill-Ecken / Sparse-only Arc-Fitting | Native Slice-Funktionen sind portierbar, falls nicht bereits enthalten; beide brauchen G-Code- und Extrusionstests. | Beta-Diff gegen Android-Quelle prüfen und Gyroid/Hilbert/Octagram instrumentiert vergleichen. |
| P2 | Hochpräziser G-Code | Benötigt konsistente XYZ/IJ/E-Formatierung, Rundungsfehlerfortschreibung und Ausnahmen für Support/Turm. | Vollständige Writer-Abhängigkeiten und Firmwareverhalten prüfen, dann Standard-/Präzisions-G-Code vergleichen. |
| P2 | Transparente Filamentvorschau | Android hat einen getrennten GLES/libvgcode-Renderer; möglicher Preview-only-Port. | Blend-/Sortierpfad prototypisch prüfen und Slice/G-Code-Unverändertheit absichern. |
| Zurückgestellt | Texture Displacement | Mesh-Bake, RAM, Abbruch und 3MF-Rundlauf erfordern einen eigenen begrenzten Arbeitsstrang. | Nach den unmittelbar portierbaren Beta-Funktionen. |
| Nicht direkt portieren | Python-Plugins, parametrisches Design, Farbmischung, Belt, IDEX/IQEX | Erfordern neue Ausführungs-, CAD- oder Hardwareverträge; Plugins führen ausführbaren Fremdcode ein. | Als eigenständige Produktziele mit Sicherheits-, Hardware- und UX-Konzept neu bewerten. |

Bereits im Android-Fork vorhandene Beta-Parität (u. a. Paint-Penetration, Infill-Verbindungen,
Qidi-Profile und GL-Preview-Reduktion) wird nicht erneut als neuer Port gezählt.

## Orca-Upstream-Funktionsprüfung nach `v0.8.421-nightly.1` — 2026-10-31

Der Upstream-Zeiger wurde bis `1093ff0714e26ca8d81f3d7fe15d782cc047346f` aktualisiert. Die drei neuen Orca-Änderungen wurden gegen aktive Android-Aufrufpfade geprüft:

- `041df4031` verbessert die Anordnung kurzer Modelle bei „Drucken nach Objekt“. Der native Arrange-Kern enthält bereits die nötigen sequenziellen Druckparameter, aber Androids `Native.bed_arrange` erhält aktuell nur Bett und Modell; die aktive Druckkonfiguration wird nicht übergeben. Kein isolierter Kernpatch wird gezählt. Nächster Schritt für diesen Kandidaten: Konfigurationsübergabe über Anordnen, Duplizieren, Bettfüllen und Import auf dem GL-Thread entwerfen und mit echter By-Object-Platzierungs-/Clearance-Regression testen.
- `c77020e65` behandelt bei Orca unbekannte, aber farblich bekannte Creality-CFS-Spulen. Android besitzt keinen Creality-CFS-Agenten, liest aber ein kompatibles generisches Moonraker-MMU-Schema. Die Anpassung hält ein belegtes Fach mit gültiger Farbe unter seinem physischen Index verfügbar, verwendet sichtbar `Generic PLA` als Orca-Fallback und lässt unbekannte/ungültige Farbe weiterhin durch die bestehende Material-/Uploadwarnung beziehungsweise den Slotfehler absichern. Sie verändert nicht die Zuordnung der übrigen Fächer.
- `1093ff071` repariert Retry-Zustände der Desktop-`PostProcessor`-/Plugin-Pipeline nach Fehlern. Android registriert keine Desktop-Skripte/Plugins und hat keinen entsprechenden Ausführungspfad; der Fix ist aktuell nicht portierbar.

### Nightly Batch 3 — 1/10

1. Unbekannte geladene MMU-Spule sichtbar und indexstabil halten; Farbformen RGB, RGB+Alpha und Creality `0RRGGBB` parsen. Leere und ungültige Slots bleiben nicht auswählbar; Materialabweichungen bleiben im Zuordnungsdialog erkennbar.

`MmuFilamentSyncDeviceTest` bestand 18/18 auf Android API 35 und 18/18 auf API 36. Abgedeckt sind Slotindex, Farbe, Verfügbarkeit, `Generic PLA`-Fallback, Materialabweichung, leere Fächer und ungültige Farben. Debug-Unit und Slice-Unit bestanden jeweils 705/705 (0 failures, errors oder skips). `git diff --check` bestanden. Keine physische Creality-/Moonraker-Hardware wurde für dieses Paket getestet. Der APK-/Release-Gate wird erst bei 10/10 erreicht.

## Orca `041df4031` — By-Object-Clearance in Android-Anordnung (2026-10-31)

Die Android-Anordnung übergibt jetzt die aktuelle Druckkonfiguration über `Bed3D` und JNI an den nativen Arrange-Kern, während die Build-Volume-Geometrie weiter aus dem aktiven Druckbett stammt. Für By-Object werden Instanzpolygone mit Orcas konfigurationsbewusster `get_instance_arrange_poly`-Routine aufgebaut. Das ist wichtig, weil der generische Android-Helfer keine Instanzhöhe lieferte und Orcas Arrange-Logik damit alle Objekte fälschlich als kurz einstufte. Nun können aktive Düsen-/Gantry-Clearance, Objekt-/Skirt-Abstand und Sequenzmodus sicher in die Platzierung einfließen. By-Layer behält den bisherigen 6-mm-Abstand.

`byObjectArrangeUsesActiveClearanceAndKeepsObjectsApart` setzt ein isoliertes 20-mm-Testmodell, By-Object, 0,1-mm-Düsenhöhe und 30-mm-Clearance und prüft, dass die zwei Modelle nach echter nativer Anordnung mindestens 29,5 mm Abstand haben. Der bestehende `modelMenuArrangeBringsOffBedObjectBackIntoWorkspace` deckt den By-Layer-Weg einschließlich Undo/Redo und Workspace-Wiederherstellung ab. Beide Tests bestanden auf Android API 35 und API 36 (je 1/1). Debug- und Slice-Unit-Suiten jeweils 705/705 ohne Fehler oder Skips; ARM64-Native-Build und `git diff --check` bestanden.

Batch 3 steht jetzt bei **2/10**. Die Änderung ist noch lokal, nicht committet oder gepusht. APK/Prerelease wird erst nach zehn fokussiert verifizierten Änderungen erstellt.

## Orca `43c76f1f5` — Layer-Pausen bleiben in der korrekten Vorschau-Schicht (2026-10-31)

Beim Verarbeiten einer Pause flusht der G-Code-Prozessor den Zeitplan und fügt gegebenenfalls Actual-Speed-Moves in die Move-Liste ein. Dadurch konnte der gespeicherte Index des Pause-/Farbmarkers veralten; die Vorschau ordnete danach Moves der falschen Layer-ID zu und konnte die Layerreihenfolge verletzen. Androids native `GCodeProcessor`-Port führt den Markerindex nun nach Move-Einfügungen nach. Beim Entfernen des Markers werden noch wartende Zeitblöcke ebenfalls angepasst. Der erzeugte G-Code und die Druckparameter werden nicht verändert.

`GCodePauseLayerPreviewDeviceTest#pauseAfterTimeFlushRemainsOnItsOwnLayerAndPreviewLayersStayOrdered` verarbeitet einen dreischichtigen G-Code mit Rückzug, Pause und Extrusion auf der Folgeschicht, lädt die Moves in libvgcode unter einem GLES3-Kontext und prüft, dass die Vorschau bei drei fortlaufenden Schichten bleibt. API-35- und API-36-Emulatoren bestanden je 1/1; ARM64-Debug-Build erfolgreich. Die vollständigen JVM-Suiten werden am Batch-Gate erneut ausgeführt.

Nightly-Batch 3: **3/10**. Kein APK/Release vor 10/10 fokussiert verifizierten Änderungen.

## Nightly Batch 5 — Änderung 6/10: exakte kurze Verfahrwegketten (2026-11-01)

Orca `69193d5681` enthält exakte dynamische Programmierung für kurze Extrusionspfadketten. Android minimiert jetzt die Verfahrdistanz für Gruppen mit bis zu acht Segmenten, wenn ein fester Startpunkt existiert. Das gilt für die aktive Kettenbildung der Extrusionspfade und für die zweite Greedy-Kettenvariante der Infill-Polylinien. Bei fehlendem Startpunkt bleibt das bisherige Greedy-Verhalten erhalten. Lange Kettenverfeinerung, geschlossene Objekt-/Insel-Touren und verbundene Infill-Linien aus demselben Orca-Commit sind **nicht** übernommen.

`NativeSliceDeviceTest#shortExtrusionChainsUseTheMinimumTravelOrder` prüft den produktiven nativen Pfad mit einer Vier-Segment-Geometrie. Das Ergebnis stimmt mit einer vollständigen Enumeration aller Reihenfolgen und erlaubten Orientierungen überein und ist kürzer als eine einfache Nearest-Neighbor-Kette. ARM64-SliceTest-Native-Build und APK-Paketierung bestanden; gezielter Test 1/1 auf API-36-Emulator. Xiaomi 2211133G blieb wach und per ADB online, doch MIUI verweigerte die Testinstallation mit `INSTALL_FAILED_USER_RESTRICTED`; kein physischer Xiaomi-Testpass. Ein vollständiger Instrumentierungslauf wurde wegen eines bestehenden Release-Notes-Testfehlers und der Geräteinstallationssperre nicht als bestanden gewertet.

Batch 5 steht bei **6/10 fokussiert verifizierten Produktänderungen**. Es wurde kein APK/Release aus diesem Teilbatch erstellt.

## Nightly Batch 5 — Änderung 7/10: exakte Default-Objekttour (2026-11-01)

Für `PrintOrder::Default` wird bei bis zu zehn Objektinstanzen die Reihenfolge nun als exakt minimale geschlossene Tour optimiert. Das nutzt Orcas TSP-Dynamic-Programming aus `69193d5681`; bei mehr als zehn Instanzen bleibt die vorhandene Kettenheuristik aktiv. Der By-Object-Pfad mit übergebenem Düsen-/Wipe-Tower-Startpunkt bleibt unverändert.

`NativeSliceDeviceTest#defaultObjectOrderUsesTheMinimumClosedTourForSmallProjects` ruft den Tour-DP mit sechs Objektmittelpunkten auf und vergleicht die geschlossene Distanz gegen vollständige Enumeration aller 120 Touren sowie eine Nearest-Neighbor-Tour. ARM64-SliceTest-Native-Build, Test-APK-Paketierung und direkter API-36-Emulatorlauf bestanden (1/1). Der Xiaomi 2211133G war per ADB online und auf `stay_on_while_plugged_in=15`, aber MIUI verweigerte erneut die Testinstallation (`INSTALL_FAILED_USER_RESTRICTED`); kein physischer Gerätetest.

Batch 5 steht bei **7/10 fokussiert verifizierten Produktänderungen**. Kein APK/Release aus diesem Teilbatch.
