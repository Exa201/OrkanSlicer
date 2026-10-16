## UI 2.0 – erster Arbeitsstand (2026-10-16)

Die Arbeitsbereichsnavigation trennt jetzt **Projekt** (Modell importieren, Projekt exportieren)
von **Modell** (Objektliste, Kopien, Druckplattenwechsel, Zentrieren, Plattenfüllung, Anordnen,
Ausrichten und Löschen). Das Modellmenü verwendet wie Bearbeiten ein kompaktes Zwei-Reihen-Raster.
Die unteren Navigationsziele haben kurze sichtbare Beschriftungen und vollständige
Accessibility-Namen. „Kopien erstellen“ beschreibt den Mengen-Dialog; „Alle anordnen“ bezeichnet
die Aktion, die alle Modelle auf der aktuellen Druckplatte verteilt. Der Renderer und die
Slicer-Engine wurden nicht geändert. IDs, Titel und Symbole der Ziele liegen nun im
Navigationspaket (`WorkspaceDestination`), getrennt vom Fragment-Lifecycle.

Die vorhandene Oberfläche erfüllt bereits die drei vorgegebenen Gestaltungsrichtungen: freie,
große Druckplatte mit einklappbarer Werkzeugfläche, kleiner Filament-Farbbutton mit Bottom Sheet
auf dem Smartphone beziehungsweise Seitenpanel ab 600 dp und gemeinsame Compose-/Material-3-
Komponenten. Das Filamentpanel wurde auf dem Honor geöffnet; ACE-Fächer und T0–T3-Zuordnung waren
sichtbar. Diese Änderung ist ein UI-Migrationsschritt und noch nicht die vollständige Ablösung der
imperativen `BedFragment`-Workspace-Hülle. Die weitere Entkopplung von Navigation, Zustand,
Workspace-Layout und Renderer-Adapter ist in `ORKANSLICER_UI_2_ROADMAP.md` festgehalten.

Verifiziert: `:app:testDebugUnitTest` und `:app:testSliceTestUnitTest` je 523/523 bestanden,
`:app:assembleDebug` einschließlich ARM64-CMake erfolgreich; APK
`app/build/outputs/apk/debug/OrkanSlicer_f85550d8fb.apk` (157.129.650 Byte, SHA-256
`2E8553021218AE9AC782B9F8B5BF23CECF9BB9E23C66E38A3D195A5425FDEB03`). Auf dem Honor PTP-N49
installiert und gestartet. Projekt-/Modellnavigation, zweireihiges Modellmenü und Filamentpanel
visuell geprüft; keine Abstürze, ANRs oder OOMs im abgefragten Logcat. Der Android-CLI-
Accessibility-Dump scheiterte an der Instrumentierungsverbindung. Der Emulator-Installationsversuch
endete nach APK-Transfer mit I/O-Timeout; Emulatorprüfung daher nicht bestätigt. Kein Druck gestartet.
Der aktualisierte PR #2 ist offen gegen `master`; aktueller UI-Arbeitsstand `162c070` wurde gepusht.

## Entwicklungsstand 0.8.242 – Temperaturen für die aktive Druckoberfläche

Filamentprofile zeigen jetzt separate Werte für erste und weitere Schichten auf glatter kühler,
Engineering-, Hochtemperatur-, strukturierter PEI-, strukturierter kühler und SuperTack-Platte.
Die Auswahl `curr_bed_type` im Druckerprofil bestimmt die Vorschau im Druckdialog. Fehlende Werte
werden nur aus älteren Profilen ergänzt; bereits gespeicherte Oberflächenwerte bleiben unverändert.

Verifiziert: Profilroutenprüfung 488 Einstellungen (319/100/69), beide Unit-Suiten 520/520,
`:app:assembleDebug` einschließlich ARM64-CMake erfolgreich, nativer Slice-/G-Code-Gerätetest
auf Honor bestanden (Textured PEI: M140 S62; kein S81). Debug-App auf Honor und Emulator
installiert; Honor-Startbildschirm visuell kontrolliert. Die Gradle-Instrumentierung über beide
Geräte lief wegen Emulator-ADB-Timeout nicht durch; der gezielte Test wurde direkt auf dem Honor
ausgeführt. Keine native Abhängigkeit fehlte, kein Druck wurde gestartet.

Alpha 0.8.242: https://github.com/Exa201/OrkanSlicer/releases/tag/v0.8.242. Das APK-Asset `OrkanSlicer_0bde473343.apk` (156.867.235 Byte) hat den bestätigten GitHub-Digest `sha256:4c1a0c27b5f463fe9068d99fa74fff50d532824c9a4cc7c62c1312ca62183cee`, identisch mit der lokalen APK. Zum damaligen Release-Zeitpunkt lag PR #2 bei `0bde473343`; der PR ist weiterhin offen gegen `master`, kein Merge ausgeführt.

# Gebündelter Build- und Prüfablauf

## Entwicklungsstand 0.8.241 – ACE-Slots behalten ihre festen T-Indizes

Die vollständige ACE-Synchronisierung komprimiert belegte Fächer nicht mehr. T0 bleibt ACE 1,
T1 bleibt ACE 2 usw.; ein leeres Fach wird als leerer T-Slot gespeichert, statt spätere
Filamente umzuschreiben. Nicht identische Moonraker-Toolmaps werden beim vollständigen Sync
abgewiesen. Die Filamentpalette erhält leere Slots beim Speichern und zeigt nur belegte
Materialien als verfügbaren Bestand.

Verifiziert: `:app:testDebugUnitTest` und `:app:testSliceTestUnitTest` je 517/517 bestanden;
`:app:assembleDebug` einschließlich ARM64-CMake-Build erfolgreich; Honor PTP-N49: APK
installiert und gestartet, **Sync** zeigte T0→ACE 1, T1→ACE 2, T2→ACE 3, T3→ACE 4. Kein Druck
wurde gestartet. Vorhandene Nutzer mit einer früher komprimierten gespeicherten Palette müssen
einmal **Sync** ausführen. Der Emulator wurde für diese Änderung nicht erneut geprüft.

## Arbeitsstand: Objektliste

`:app:testDebugUnitTest` und `:app:testSliceTestUnitTest` bestanden mit je 516 Tests;
`:app:assembleDebug` war ebenfalls erfolgreich. Die Debug-APK wurde auf dem Honor PTP-N49 installiert und OrkanSlicer startete
bis zur Modellansicht. Der Screenshot bestätigt den App-Start; die neue Objektliste selbst
ließ sich nicht prüfen, weil in der Sitzung kein Modell geladen war. Honor und Emulator waren
per ADB sichtbar. Der ARM64-Nativkern wurde als unverändert/up-to-date übernommen. Kein Druck
wurde gestartet.

## Build 0.8.241 – 3MF-Farbmalerei-Rundlauf repariert

Der Honor-Gerätetest `TwoColorProjectDeviceTest.paintedFilamentAssignmentsSurviveThreeMfExportAndImport` reproduzierte den Verlust gemalter Filamente beim 3MF-Rundlauf. Ursache: Der Orca/Bambu-Importer las nur `paint_color`, obwohl der verwendete Prusa-3MF-Exporter `slic3rpe:mmu_segmentation` schreibt. Beide 3MF-Parserpfade des Orca/Bambu-Importers akzeptieren nun beide Attribute. Zusätzlich baut eine Paint-Session ihren Selektor auf Volume 0 auf, dessen Facettenannotation sie speichert, sodass Dreiecksindizes bei Mehrvolumenobjekten übereinstimmen.

Der gezielte Round-trip-Test und die gesamte `TwoColorProjectDeviceTest`-Klasse (2/2) bestanden auf dem HONOR PTP-N49. `:app:testDebugUnitTest`, `:app:testSliceTestUnitTest` und `:app:assembleDebug` einschließlich ARM64-Nativkern bestanden ebenfalls. Die APK wurde auf dem Honor installiert und gestartet. Einzelnes Undo für Pinselstriche, Zoom beim Malen und komplexe Mehrvolumenmodelle bleiben offen.

Alpha 0.8.241 wurde unter `https://github.com/Exa201/OrkanSlicer/releases/tag/v0.8.241` veröffentlicht. Das APK-Asset `OrkanSlicer_0.8.241_alpha_debug.apk` (156.849.377 Byte) hat den bestätigten GitHub-Digest `sha256:4b04e1a7392c2e2b87c0bd4ec859c4b7f02674204d93b6d2bfeb08cbbdaacc8e`, identisch mit der lokalen APK. PR #2 bleibt offen gegen `master`, Kopfcommit `bf1c3707fb8d181de8861e79ef4b1aa0461d682a`; kein Merge ausgeführt. Die bestehende Raycast-API gibt keine Volume-/Facet-ID zurück. Deshalb ist Malen direkt auf einzelnen Volumes komplexer Orca/Bambu-Baugruppen nicht verifiziert und darf nicht aus dem Round-trip-Test für die einfache Zweiteil-Fixture abgeleitet werden.

Zusatzprüfung der Brückenoptionen: `NativeSliceDeviceTest.bridgeSettingsReachNativeConfigAndGenerateBridgeGcode` bestand auf dem Honor. Er prüft vier Brückeneinstellungen in der generierten Native-Konfiguration, Bridge-Markierung im G-Code sowie den Klipper-Pressure-Advance-Befehl. Eine getrennte äußere/innere Rollenassertion ist aus dem G-Code-Parser hier nicht ableitbar: Orca gibt für beide `BridgeInfill` aus. Der Test startet keinen Druck; physische Qualitätsabnahme bleibt offen.

## Build 0.8.174 – SliceBeam-Menüaufbau auf dem Honor geprüft

Der Vergleich mit SliceBeam 1.1.0 auf dem HONOR PTP-N49 bestätigt die fünf Hauptbereiche oben und die Arbeitsbereichs-Werkzeuge unten. Die aktive Tab-Beschriftung verwendet jetzt die Akzentfarbe. Der deutsche Menüpunkt für Verschieben, Drehen, Skalieren und Schneiden heißt „Objekt“ und passt ohne Kürzung in die Leiste.

Der vollständige Sammellauf bestand 342/342 Android-Unit-Tests, 99/99 Headless-/Geometrietests und 9/9 Android-16-Instrumentierungstests, Release-Lint (0 Fehler/390 Warnungen), Kotlin-/UI-, Profilrouten- (473 Einstellungen/91 deutsche Gruppen), Connector- und 63/63 ARM64-Prebuilt-Audits, Java/Kotlin-Interop, Druck-/Upload-/Filament-/G-Code-/Lithophan-/Fastener-Regressionen sowie APK-Metadaten, ZIP-CRC und `git diff --check`. Die endgültige UI-Fassung wurde danach noch einmal gebaut, getestet, installiert und auf dem Honor visuell geprüft. Kein Drucker wurde angesprochen.

Debug-APK `outputs/OrkanSlicer_0.8.174_debug.apk` (156.170.719 Byte; SHA-256 `675AC5DB7F8AB4F1833A48A8AEAE133DA89D15A07B41D12468E632674690C0FC`); unsignierte Release-APK `outputs/OrkanSlicer_0.8.174_release_unsigned.apk` (146.670.583 Byte; SHA-256 `87B1B6BF546BC24351C154D02EC24357430B692BA7BAC121985DA7377B194D48`). Die Release-APK hat mangels Release-Keystore keine Signatur. Screenshots und Prüfprotokoll: `outputs/OrkanSlicer_0.8.174/` sowie die oben genannten Honor-Bilder.

## Build 0.8.173 – Einstellungsmenüs passen sich dem Honor-Querformat an

Die fünf SliceBeam-Hauptbereiche bleiben sichtbar. Die App-Einstellungen zeigen im Hochformat weiterhin eine gut lesbare Spalte und schalten in kurzen Querformatfenstern auf zwei Spalten um. Alle vier geschlossenen Bereiche, „Maker öffnen“ und „Zurücksetzen“ sind direkt erreichbar. Ein geöffneter Abschnitt blieb beim Wechsel der Ausrichtung erhalten.

Der vollständige Sammellauf bestand 341/341 Android-Unit-Tests, 99/99 Headless-/Geometrietests, 9/9 Android-16-Instrumentierungstests auf dem HONOR PTP-N49, Release-Lint (0 Fehler/390 Warnungen), Kotlin-/UI-, Profilrouten-, Cut-Connector- und 63/63 ARM64-Prebuilt-Audits, Java/Kotlin-Interop, Druck-/Upload-/Filament-/G-Code-/Lithophan-/Fastener-Regressionen, APK-Metadaten, ZIP-CRC und `git diff --check`. Der Quellfingerabdruck des ARM64-Caches wurde bestätigt; er enthält 54 Bibliotheken.

Die Debug-App wurde mit erhaltenen App-Daten auf dem Honor installiert und gestartet. Hoch-/Querformat wurden visuell kontrolliert; der Prozess blieb aktiv, und kein Drucker wurde angesprochen. Die ADB-Drehautomatik wurde wiederhergestellt. Debug-APK `outputs/OrkanSlicer_0.8.173_debug.apk` (154.774.667 Byte; SHA-256 `30A7ED41009117491720B1E55478253BEE8C8EB385712CF761BB09EE24CC7659`). Unsigned Release-APK `outputs/OrkanSlicer_0.8.173_release_unsigned.apk` (146.670.583 Byte; SHA-256 `0A94B5DA8E47A3CAFEBE581236C9C6511D688C5C9DD272497B8CA521E4E944AC`). Screenshots, Prüfprotokoll und Bericht liegen in `outputs/OrkanSlicer_0.8.173/` und `outputs/OrkanSlicer_0.8.173_Buildbericht.md`.

## Build 0.8.171 – Druckmenü an SliceBeam auf dem Honor angepasst

Der direkte Vergleich auf dem HONOR PTP-N49 bestätigt die fünf festen Hauptbereiche und die klare, geschlossene Profilnavigation von SliceBeam 1.1.0. In OrkanSlicer bleiben die Arbeitsbereichs-Menüs Datei, Kamera, Ändern, Optionen und Slicen sichtbar. Auf dem Druckprofil bleibt T0–T3 sichtbar; Turmposition, Spülvolumen und ACE-Synchronisierung sind in „Material und Spülung“ zusammengefasst und beim Öffnen standardmäßig geschlossen. Die deutsche Kurzbeschriftung „Ändern“ passt vollständig in die Telefonleiste.

Der Quellcode-Sammellauf bestand 332/332 Android-Unit-Tests, 99/99 Headless-/Geometrietests und 9/9 Android-16-Instrumentierungstests. Profilrouten (471 Einstellungen/91 deutsche Gruppen), Kotlin-/UI-, Connector- und ARM64-Prebuilt-Audits (63/63), Java/Kotlin-Interop, Druck-/Upload-/Filament-/G-Code-, Lithophan- und Fastener-Regressionen sowie APK-Metadaten, ZIP-CRC und `git diff --check` waren erfolgreich. ARM64 wurde aus Quellcode gebaut; der Cache enthält 54 geprüfte Bibliotheken. Release-Lint: 0 Fehler/386 Warnungen.

Nach der Honor-Sichtprüfung wurden zwei lange Texte gekürzt und Debug/Release mit dem verifizierten Cache neu verpackt. Die endgültige Debug-Version wurde installiert; Vorschau sowie geschlossener und geöffneter Druckwerkzeug-Abschnitt wurden visuell geprüft. Kein Crash/ANR/OOM; kein Drucker wurde angesprochen. Debug `outputs/OrkanSlicer_0.8.171_debug.apk` (156.301.655 Byte, SHA-256 `36BC6B5E46E587A83A320EC1EE81AA03A2319716C7E56BB5250752C25C32E83E`); unsignierte Release `outputs/OrkanSlicer_0.8.171_release_unsigned.apk` (146.670.727 Byte, SHA-256 `8B343FE0A249EE6FA07937994AFB4904B2F16AD2854EEC64AF678976690B915A`). Prüfbilder und vollständiger Bericht liegen in `outputs/OrkanSlicer_0.8.171/` und `outputs/OrkanSlicer_0.8.171_Buildbericht.md`.

## Build 0.8.169 – SliceBeam-Profilmenüs als geschlossene Akkordeons

Die Honor-Referenz SliceBeam 1.1.0 zeigt fünf klare Hauptbereiche. In OrkanSlicer bleiben diese Bereiche erhalten; Druck-, Filament- und Druckerprofile zeigen ihre Kategorien jetzt als große Akkordeon-Karten, die beim Start geschlossen sind. Geöffnete Kategorien und Untergruppen sind pro aktivem Profil getrennt gemerkt. Die Suche erhält passende Elternkategorien und Untergruppen, und Profilfilter, Filamentpalette sowie der feste Speichern-/Zurücksetzen-Bereich bleiben erreichbar.

Auf dem HONOR PTP-N49 wurden die fünf SliceBeam-Bereiche und OrkanSlicers neue Profilansichten verglichen. Im Gerätetest öffneten sich „Qualität“ und die Untergruppe „Schichthöhe“; ein Scrolltest zeigte den zugehörigen Wert. Die installierte App blieb aktiv, ohne Crash/ANR/OOM; ein Drucker wurde nicht angesprochen.

Der vollständige Sammellauf bestand 330/330 Android-Unit-Tests, 99/99 Headless-/Geometrietests, 9/9 Android-16-Instrumentierungstests, Kotlin-/UI-, Connector- und Native-Prebuilt-Audits, Profilrouten (471 Einstellungen/91 deutsche Gruppen), Java/Kotlin-Interop, Druck-/Upload-/Filament-/G-Code-/Lithophan-/Fastener-Regressionen sowie APK-Metadaten, ZIP-CRC und `git diff --check`. Der ARM64-Quellcache enthält 54 Bibliotheken. Release-Lint: 0 Fehler/385 Warnungen.

Debug-APK `outputs/OrkanSlicer_0.8.169_debug.apk` (154.183.196 Byte; SHA-256 `8F9319792060D6DE99972917397699E16DE004F0FFFF63B724709CE3AA20EA39`); unsignierte Release-APK `outputs/OrkanSlicer_0.8.169_release_unsigned.apk` (146.653.391 Byte; SHA-256 `E23BE400FC9300DBDB76A96A31303F6A716E6798196CAFB6316A5F3FA353FBFA`). Screenshots und Prüfprotokoll liegen in `outputs/OrkanSlicer_0.8.169/`.

## Vorheriger Build 0.8.168 – Orca-Einstellung für den Ruck der ersten Schicht

Die native Orca-Option `initial_layer_travel_jerk` ist nun im mobilen Prozessprofil unter „Ruck (XY)“ verfügbar. Sie bleibt an die native Definition und den vorhandenen Profil- und Slicingpfad gebunden.

Der vollständige Sammellauf bestand 328/328 Android-Unit-Tests, 99/99 Headless-/Geometrietests, 9/9 Android-16-Instrumentierungstests, Kotlin-/UI-, Connector- und Native-Prebuilt-Audits, Profilrouten (471 Einstellungen/91 deutsche Gruppen), Java/Kotlin-Interop, Druck-/Upload-/Filament-/G-Code-/Lithophan-/Fastener-Regressionen sowie APK-Metadaten, ZIP-CRC und `git diff --check`. Der aus Quellcode aktualisierte ARM64-Cache enthält 54 Bibliotheken. Release-Lint: 0 Fehler/385 Warnungen.

Die Debug-APK wurde auf dem HONOR PTP-N49 installiert und gestartet. Es wurde kein Drucker angesprochen. Debug-APK `outputs/OrkanSlicer_0.8.168_debug.apk` (154.183.209 Byte; SHA-256 `6BA06F15E822C39508586648FE7D6C6BAF4AE7C9566F869A6B0F0BE2BD170721`); unsignierte Release-APK `outputs/OrkanSlicer_0.8.168_release_unsigned.apk` (146.653.391 Byte; SHA-256 `131F99545450E5C02C4C2446C70696384862EC09516349882704E0E81346C595`). Vollständiger Bericht und Protokoll liegen unter `outputs/OrkanSlicer_0.8.168*`.

## Vorheriger Build 0.8.167 – SliceBeam-Arbeitsbereichsmenü auf dem Honor

Der Arbeitsbereich zeigt jetzt die fünf Ziele Datei, Ansicht, Ändern, Optionen und Slicen. Die bislang hinter „Mehr“ versteckten Dateiaktionen sind direkt im Optionen-Menü zugänglich. Kachelbreiten und kurze deutsche Kamera-/Ansichtsnamen wurden auf dem HONOR PTP-N49 kontrolliert.

Die vollständige Sammelprüfung bestand: 327/327 Android-Unit-Tests, 99/99 Headless-/Geometrietests und 9/9 Android-16-Instrumentierungstests auf dem HONOR PTP-N49. Ebenfalls bestanden: Kotlin-/UI-Migrationsaudit (302 App-Produktionsquellen/86 Kotlin-Unit-Tests), Profilrouten (470 Einstellungen/91 deutsche Gruppen), Connector- und ARM64-Prebuilt-Audits (63/63), Java/Kotlin-Interop, Upload-/Filament-/G-Code-, Lithophan- und Fastener-Regressionen, APK-Metadaten, ZIP-CRC und `git diff --check`. Der ARM64-Quellcache enthält 54 geprüfte Bibliotheken. Release-Lint: 0 Fehler/385 Warnungen.

Die Debug-App wurde auf dem Honor mit App-Daten-Erhalt installiert und gestartet. Der Prozess blieb aktiv; im Startprotokoll gab es keinen Crash, ANR oder OOM. Alle fünf Arbeitsbereichsziele wurden visuell geprüft. Der isolierte Slice-Test verwendet eingebettete Daten; kein Drucker oder Netzwerkdienst wurde angesprochen.

Debug-APK: `outputs/OrkanSlicer_0.8.167_debug.apk` (156.283.415 Byte; SHA-256 `9044938487D65D2077E8E28D38F6CEADD17F2963114CF00376421374649DF465`). Die unsignierte Release-APK ist `outputs/OrkanSlicer_0.8.167_release_unsigned.apk` (146.653.391 Byte; SHA-256 `B2CAC3A5F222CD6AB6B0408BB453CBD378F849A498ABF35EC1CB544121888117`). Bericht und Prüflaufprotokoll liegen unter `outputs/OrkanSlicer_0.8.167*`.

## Build 0.8.166 – SliceBeam-Menüstruktur auf dem Honor

Die Hauptnavigation zeigt jetzt die fünf Bereiche Vorschau, Druck, Filament, Drucker und App ohne zusätzlichen Kopfbereich. Maker ist direkt aus dem App-Bereich erreichbar. Der Gerätetest fand dabei einen Fehler in der Fragmentnavigation: Unteransichten und zurückkehrende Ansichten wurden nicht in den Inhalt eingefügt, und die Zurückanimation lief in die falsche Richtung. Beide Pfade sind korrigiert.

Der Build bestand 327/327 Android-Unit-Tests, 9/9 Android-16-Instrumentierungstests auf dem HONOR PTP-N49, Kotlin-/UI-Migrationsaudit, Connectoraudit, 63/63 ARM64-Prebuilt-Prüfungen, `lintVitalRelease`, APK-Metadaten, ZIP-CRC und `git diff --check`. Der ARM64-Slicer wurde aus Quellcode gebaut und der Cache mit 54 Bibliotheken aktualisiert. Der vollständige informative `lintRelease`-Scan sowie Headless-/Geometriesuite wurden in diesem gezielten UI-Lauf nicht ausgeführt.

Auf dem Honor wurden Vorschau, Druck, Filament, Drucker, App, Maker und die Rückkehr aus Maker in die Einstellungen geprüft. Die installierte App blieb aktiv; im App-Protokoll gab es keinen Crash, ANR oder Out-of-memory-Fehler. Kein Drucker wurde angesprochen.

Debug-APK `outputs/OrkanSlicer_0.8.166_debug.apk` (154.253.427 Byte; SHA-256 `58301E99177EBBC5BC5050CC2A342FC066702F23936EE7F08B529A1D4B5FC910`); unsignierte Release-APK `outputs/OrkanSlicer_0.8.166_release_unsigned.apk` (146.653.015 Byte; SHA-256 `79F6FCDA9DB21136F522BE282E5B7B580B1E38548D5D5A2B2EA792B6BDD8B831`). Prüfbericht und Honor-Aufnahmen liegen unter `outputs/OrkanSlicer_0.8.166*`.

## Build 0.8.165 – Honor-Layout und sofortige Profilaktualisierung

Die Profileinstellungs-Compose-Ansicht wird jetzt nach Druckertyp und Profilnamen neu aufgebaut. So bleiben bei einem Druckerwechsel keine Eingabewerte des vorherigen Profils in der Liste stehen.

Auf dem HONOR PTP-N49 mit Android 16 wurde Qidi Q2 0.6 → „Maschinengrenzen“ → Anycubic Kobra 3 V2 → zurück zu Qidi getestet. Anycubic zeigte 255 × 260 mm und Qidi 270 × 256 mm. Qidi blieb nach Kaltstart ausgewählt; die App startete mit leerer Platte. Kein Drucker wurde angesprochen.

Der ARM64-Slicer wurde aus dem Quellcode gebaut (54 native Bibliotheken). 325/325 Unit-Tests, 99/99 Headless-/Geometrietests und 9/9 Android-Instrumentierungstests bestanden. Profilrouten (470 Einstellungen/91 deutsche Gruppen), Kotlin-/UI-/Connector-/Native-Prebuilt-Audits (63/63 Importe), Java/Kotlin-Interop, Upload-/G-Code-/Filament-/Anycubic-/Lithophan-/Fastener-Regressionen, APK-Metadaten, ZIP-CRC und `git diff --check` bestanden. `lintVitalRelease` erfolgreich; der vollständige informative Lint-Scan wurde für diesen Build nicht ausgeführt, da der Windows-Quelldatei-Scan zuvor hing.

Debug-APK `outputs/OrkanSlicer_0.8.165_debug.apk`; unsignierte Release-APK `outputs/OrkanSlicer_0.8.165_release_unsigned.apk`. Vollständiger Prüfbericht: `outputs/OrkanSlicer_0.8.165_Buildbericht.md`.

## Vorheriger Stand — Build 0.8.160 – Honor-Menüs und Reinigungsturm

Die Menüschrift ist standardmäßig 110%, Hauptmenütexte sind größer und die über der Platte liegende mehrzeilige Profilübersicht ist durch einen kompakten, weiterhin bedienbaren Profilknopf ersetzt. Für den Reinigungsturm bleibt Ziehen auf der Druckplatte möglich; die Smartphone-Touchzone beträgt mindestens 12 mm und ein Tipp öffnet die X-/Y-Eingabe.

Der vollständige ARM64-Quellbuild erneuerte den nativen Cache mit 54 Bibliotheken für `versionCode 195`. 322/322 Android-Unit-Tests, 99/99 Headless-/Geometrietests und 9/9 Android-16-Gerätetests auf dem HONOR PTP-N49 bestanden. Dazu kamen Kotlin-/UI-/Connector-/63/63 Native-Prebuilt-Audits, Profilrouten (470 Einstellungen/91 deutsche Gruppen), Java/Kotlin-Interop, Druck-/Upload-/G-Code-/Filament-/Anycubic-/Lithophan-/Fastener-Regressionen, beide Demo-Builds, Release-Lint (0 Fehler/379 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die Debug-App wurde mit App-Daten-Erhalt installiert, geöffnet und visuell auf Profilknopf, Druckeinstellungsseite und X/Y-Dialog geprüft; kein Crash/ANR. Ein Reinigungsturm-Modell und ein Druckerauftrag waren nicht Teil des Gerätetests.

Debug-APK: `outputs/OrkanSlicer_0.8.160_debug.apk` (154.163.002 Byte; SHA-256 `FBDF8D4AF2D5603C7E55C1F3F2FFFB24017D89870A82304647ADEBB9349ABA4D`). Release-APK: `outputs/OrkanSlicer_0.8.160_release_unsigned.apk` (146.649.567 Byte; SHA-256 `DA7905A895DFF09EDB9B0B71D5418E1270A2FA41DC86A46E76AA2479209439C3`); ohne Release-Keystore unsigniert. Bericht: `outputs/OrkanSlicer_0.8.160_Buildbericht.md`.

## Build 0.8.139 – Setup-Pager vollständig in Compose

Der Setup-Assistent verwendet Compose `HorizontalPager` und `rememberPagerState` anstelle von ViewPager2. Die vorhandenen Setup-Seiten behalten Navigation und Gesten; Profilseiten werden weiter dynamisch geladen. Die aktive Seite und die Setup-Begrenzung werden nach Activity-Neuerstellung wiederhergestellt. Vollständiger ARM64-Quellbuild in 29:07 Minuten. 285/285 Unit-Tests, 96/96 Headless-E2E-Tests und 9/9 Android-16-Instrumentierungstests bestanden. Kotlin-/UI-Audit (290 Kotlin-Appquellen), Profilrouten (470 Einstellungen/91 deutsche Gruppen), Java/Kotlin-Interop, Druck-/Upload-/G-Code-/Generator-/Geometrieregressionen, Connector-/63/63 ARM64-Prebuilt-Audits, Release-Lint (0 Fehler/370 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check` waren erfolgreich. Der Quellbuild aktualisierte den Native-Cache mit 54 ARM64-Bibliotheken. Die Debug-App wurde auf `emulator-5554` installiert und ohne Crash gestartet. ADB erkannte kein physisches Xiaomi; ein Drucker wurde nicht angesprochen.

Debug-APK: `outputs/OrkanSlicer_0.8.139_debug.apk` (154.089.964 Byte; SHA-256 `DD477AC199623FA81559FF99963D66CAD630960FAB3404F5729B9DF1C746290C`). Release-APK: `outputs/OrkanSlicer_0.8.139_release_unsigned.apk` (146.592.911 Byte; SHA-256 `4E48F3AA17484174F49EF975F9C9E861A6CB25122075096C17BE442A877BAF28`); ohne Release-Keystore unsigniert. Bericht und Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.139*`.

## Build 0.8.138 – Setup-Seite und Repository-Zeile in Compose

Die Repository-Zeile des Setup-Assistenten verwendet jetzt eine zugängliche Compose-Schalterzeile mit gut lesbarer Beschreibung. Der Seitenhintergrund und die äußere Setup-Ansicht sind ebenfalls Compose; der vorhandene ViewPager2 bleibt als Android-View eingebettet, damit Seitenwechsel, Wischgesten, Insets und Titelanimation weiterlaufen. Der Sammellauf mit geprüftem ARM64-Cache bestand 285/285 Android-Unit-Tests, 96/96 Headless-E2E-Tests und 8/8 Android-16-Instrumentierungstests. Kotlin-/UI-Audit (290 Kotlin-Appquellen), Profilrouten (470 Einstellungen/91 deutsche Gruppen), Java/Kotlin-Interop, Druck-/Upload-/G-Code-/Generator-/Geometrieregressionen, Connector-/63/63 ARM64-Prebuilt-Audits, Release-Lint (0 Fehler/370 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check` bestanden. Die Debug-App wurde auf dem Android-16-Emulator installiert und gestartet; ADB erkannte kein physisches Xiaomi.

Debug-APK: `outputs/OrkanSlicer_0.8.138_debug.apk` (154.073.475 Byte; SHA-256 `3A8E1C3CD3194B86636E9AE51D804FB74EA35408C7DFF2A7A86471C005F6C81E`). Release-APK: `outputs/OrkanSlicer_0.8.138_release_unsigned.apk` (146.592.803 Byte; SHA-256 `1E29C8032CDEA07CBDA7B6384B2C659151ED5027C2C62045184877B4868F03FC`); ohne Release-Keystore unsigniert. Vollständiger Bericht und Protokoll liegen in `outputs/OrkanSlicer_0.8.138*`.

## Build 0.8.137 – Setup-Startbild und Titel in Compose

Das Hintergrundbild und der animierte Titel des Setup-Assistenten werden jetzt in Compose gezeichnet. Theme-abhängige Darstellung, Seitenübergang, Insets und Titelbewegung bleiben erhalten; der vorhandene ViewPager2-Pager behält seine Gesten und Navigation. Der ARM64-Quellbuild dauerte 27:18 Minuten. 285/285 Unit-Tests, 96/96 Headless-E2E-Tests und 8/8 Android-16-Instrumentierungstests bestanden. Das Release-Lint meldete 0 Fehler und 370 Warnungen. Kotlin-/UI-Audit (288 Kotlin-Appquellen), Profilrouten (470 Einstellungen/91 deutsche Gruppen), Java/Kotlin-Interop, Druck-, Upload-, G-Code-, Generator- und Geometrie-Regressionen, Connector-/63/63 ARM64-Prebuilt-Audits, APK-Metadaten, ZIP-CRC und `git diff --check` waren erfolgreich. Die Debug-App wurde auf dem Emulator installiert und ohne Crash gestartet; ein physisches Xiaomi war nicht in ADB sichtbar.

Debug-APK: `outputs/OrkanSlicer_0.8.137_debug.apk` (154.073.410 Byte; SHA-256 `6208326104CCBE46ECEB7BEEA34867996F9923C749C059FA052ED59F7AFB9325`). Release-APK: `outputs/OrkanSlicer_0.8.137_release_unsigned.apk` (146.592.723 Byte; SHA-256 `D8532E396529D0F40040F981359AB93A1CBB087122EF88B8D87EAE2A49814B54`); ohne Release-Keystore unsigniert. Vollständiger Bericht und Protokoll liegen in `outputs/OrkanSlicer_0.8.137/`.

## Build 0.8.136 – Setup-Unterstützerseite in Compose

Die Unterstützerseite des Einrichtungsassistenten nutzt jetzt Compose. Namen stehen in einer scrollbar begrenzten Liste; geänderte Serverdaten aktualisieren die Ansicht auch auf der separat geöffneten Seite. Die Liste bleibt bei bloßem Wiederanzeigen stabil. Der externe Boosty-Link sowie die Weiter-/Schließen-Aktionen bleiben erhalten. Der ARM64-Quellbuild und erste Sammellauf dauerten 25:33 Minuten; nach der abschließenden Aktualisierung der Serverdatenlogik bestand der endgültige Stand eine 14:27-Minuten-Prüfung mit dem versionstreuen Quellbuild-Native-Cache. 285/285 Unit-Tests, 96/96 Headless-E2E-Tests und 7/7 Android-16-Instrumentierungstests bestanden. Profilrouten, Kotlin-/UI-, Connector- und 63/63 ARM64-Prebuilt-Audits, Java/Kotlin-Interop, Druck-/Upload-/G-Code-/Generator-/Geometrieregressionen, Release-Lint (0 Fehler/370 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check` waren erfolgreich. Die Debug-App wurde auf dem Emulator installiert und startete ohne Crash; ADB erkannte kein Xiaomi.

Debug-APK: `outputs/OrkanSlicer_0.8.136_debug.apk` (154.568.731 Byte; SHA-256 `C9D5A22218A54D3DF5692F413938261F357BF20C8207B9620F4265F5B93E67AA`). Release-APK: `outputs/OrkanSlicer_0.8.136_release_unsigned.apk` (146.576.351 Byte; SHA-256 `77812404AF5F70FCAA0A7707601597E3242606C7411B5FBA53B4998BCDB0F959`); ohne Release-Keystore unsigniert. Vollständiger Bericht und Protokoll liegen in `outputs/OrkanSlicer_0.8.136/`.

## Build 0.8.135 – Absturzbildschirm in Compose

Der Wiederherstellungsbildschirm verwendet jetzt Compose und `ComponentActivity`, damit ein gespeicherter Fehlerbericht auch im Notfall einen Lifecycle-Owner erhält. Der Bericht ist scrollbar; Teilen und Neustart bleiben erreichbar. Die Darstellung berücksichtigt den System-Farbmodus und umgeht Druckerprofile sowie die normale Theme-Initialisierung. Der ARM64-Kern wurde für 0.8.135 aus dem Quellcode gebaut; die finale Gruppenprüfung dauerte nach Behebung des Lifecycle-Fehlers 14:27 Minuten. 285/285 Unit-Tests, 96/96 Headless-E2E-Tests und 6/6 Android-16-Instrumentierungstests bestanden. Profil-/Kotlin-UI-/Connector-/63/63 Native-Prebuilt-Audits, Java/Kotlin-Interop, Druck-/Upload-/G-Code-/Generator-/Geometrieregressionen, Release-Lint (0 Fehler/370 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check` waren erfolgreich. Die App wurde auf dem Emulator installiert und startete ohne Crash; ADB erkannte kein Xiaomi.

Debug-APK: `outputs/OrkanSlicer_0.8.135_debug.apk` (154.568.932 Byte; SHA-256 `50ECF6BCCD85AD6527176F0812AEB32C100EA552B03F5D656292AE5EF1079324`). Release-APK: `outputs/OrkanSlicer_0.8.135_release_unsigned.apk` (146.576.443 Byte; SHA-256 `57CC617076FC1EFA3D32FC484CAFA559B1E976E62CAF135469E29D364160998E`); ohne Release-Keystore unsigniert. Emulator-Screenshot und vollständiges Protokoll liegen unter `outputs/OrkanSlicer_0.8.135/`.

## Build 0.8.134 – Changelog und Profilfarben in Compose

Das Änderungsprotokoll und die Unterstützerliste sind auf Compose umgestellt; Profilfarben öffnen dieselbe zentrale Farbauswahl wie Filament-Slots. Der vollständige ARM64-Quellbuild dauerte 26:12 Minuten. Die endgültige APK nach einer deutschen Textkorrektur bestand eine inkrementelle Prüfung in 2:26 Minuten. 285/285 Unit-Tests, 96/96 Headless-E2E-Tests, 5/5 Android-16-Instrumentierungstests, Java/Kotlin-Interop, Kotlin-UI-/Profilrouten-/Connector-/63/63 ARM64-Prebuilt-Audits, Geometrie-/Druck-/Upload-Regressions, Release-Lint (0 Fehler/371 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check` bestanden. Der Android-16-x86_64-Emulator installierte und startete die Debug-App ohne Absturz. Ein Xiaomi war nicht per ADB sichtbar.

## Build 0.8.132 – Compose-Bilder im Setup

Die Drucker- und Filamentbilder des Setup-Assistenten verwenden keine eingebetteten `ImageView`s mehr. Ein gemeinsamer Glide-Loader liefert Größenlimits, Fallback-Symbole und das Beenden von Bildanfragen beim Verlassen des Composables; der Cloud-Profilavatar nutzt dieselbe Komponente. Der vollständige ARM64-Quellbuild dauerte 25:06 Minuten. 279/279 Unit-Tests, 96/96 Headless-E2E-Tests, 3/3 Android-16-Instrumentierungstests, Java/Kotlin-Interop, Kotlin-UI-/Profilrouten-/Connector-/63/63 ARM64-Prebuilt-Audits, Druck-/Upload-/G-Code-/Geometrie-Regressions, Release-Lint (0 Fehler/374 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check` bestanden. Der Debug-Build wurde auf dem Android-16-Emulator installiert und startete ohne Crash. Das Xiaomi war nicht per ADB sichtbar.

## Build 0.8.131 – Compose-Profilstatus und Android-16-Rotation

Die Profilstatuskarte zeigt die vollständigen Drucker-, Düsen-, Filament- und Prozessangaben auf bis zu vier Zeilen. Die Leiste mit Druckplatten und Webzugriff wird unter der gemessenen Statuskartenhöhe platziert. Der vollständige ARM64-Quellbuild dauerte 24:22 Minuten. 279/279 Unit-Tests, 96/96 Headless-E2E-Tests, 3/3 Android-16-Instrumentierungstests, Java/Kotlin-Interop, Profilrouten, Kotlin-UI-/Connector-/54/54 ARM64-Prebuilt-Audits, Druck-/Upload-/G-Code-/Geometrie-Regressions, Release-Lint (0 Fehler/372 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check` bestanden. Die Debug-App wurde auf dem Emulator installiert und gestartet. ADB listete kein Xiaomi.

## Build 0.8.130 – übersichtliches Slice-Menülabel

Das Aktionslabel zum manuellen Slicen wurde gekürzt und im Emulator auf Deutsch und Englisch geprüft. Der ARM64-Quellbuild dauerte 24:41 Minuten. 279/279 Unit-Tests, 96/96 Headless-E2E-Tests, 3/3 Android-16-Instrumentierungstests und der vollständige Druck-/Profil-/G-Code-/Generator-/Paket-Prüflauf bestanden; Release-Lint: 0 Fehler/372 Warnungen. Das Xiaomi wurde von ADB nicht erkannt.

## Build 0.8.128 – Arbeitsmenü-Navigation in Compose

Die vier Kategorien Datei, Bearbeiten, Ansicht und Slicen im Modell-Arbeitsbereich verwenden nun die adaptive Compose-Navigation für Hoch- und Querformat. Die vorhandenen Menüaktionen, die Zurück-Navigation, die Slice-Menü-Aktualisierung und die G-Code-Freigabe bleiben erhalten. Der Kotlin-UI-Audit deckt beide Layoutvarianten und den Compose-Auswahlzustand ab. Der Android-Instrumentierungstest prüft den angehängten Compose-Menübereich nach einer Bildschirmdrehung; er wurde kompiliert, aber wegen der offline gebliebenen Testumgebung nicht ausgeführt.

Build 0.8.128 (`versionCode 163`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 279/279 Android-Unit-Tests, 96/96 Headless-E2E-Tests, Java/Kotlin-Interop, Profilrouten (470 Einstellungen/91 deutsche Gruppen), Kotlin-UI- und Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Datei-/Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, Release-Lint (0 Fehler/372 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die 54 ARM64-Nativbibliotheken wurden im Quellbuild erstellt und der native Cache passend zur Version aktualisiert. Der Sammellauf dauerte 20:16 Minuten. ADB listete weder ein Xiaomi noch einen online verfügbaren Android-16-Emulator; die lokale Slice-/Rotations-Instrumentierung wurde kompiliert, aber nicht ausgeführt.

Debug-APK: `outputs/OrkanSlicer_0.8.128_debug.apk` (154.007.256 Byte; SHA-256 `B4F4613AF557E8EBC91708D1D7FB157D301097C458696108D4AB0D8B2996F7DD`). Release-APK: `outputs/OrkanSlicer_0.8.128_release_unsigned.apk` (146.559.339 Byte; SHA-256 `FC5C32DC4B8CBD64E75D8ED07DA4F25E02EAB45DFB813ABAE1690A2454CE275B`); ohne Release-Keystore unsigniert. Bericht und Protokoll: `outputs/OrkanSlicer_0.8.128_Buildbericht.md` und `outputs/OrkanSlicer_0.8.128/verification-transcript.txt`.

## Build 0.8.127 – Compose-Profilrotation unter Android 16

Beim Wechsel zwischen Hoch- und Querformat wurde die horizontal scrollbare Filamentpalette während der adaptiven Navigation zeitweise mit unendlicher Maximalbreite gemessen. `FilamentPaletteBar` begrenzt ihre Breite jetzt auf 1200 dp, bevor das horizontale Scrollen angewendet wird. Der Android-16-Instrumentierungstest prüft die erhaltene Arbeitsfläche samt Modell, die geöffneten App-Einstellungen und die Maker-, Druck-, Filament- und Druckerbereiche über Orientierungswechsel hinweg. Wiederholte Druckplatten-Tipps bleiben ebenfalls durch den Touch-Coalescing-Test abgedeckt.

Build 0.8.127 (`versionCode 162`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand den vollständigen ARM64-Quellbuild in 31:51 Minuten, 279/279 Android-Unit-Tests, 96/96 Headless-E2E-Tests und 3/3 Android-16-Instrumentierungstests. Java/Kotlin-Interop, Profilrouten (470 Einstellungen/91 deutsche Gruppen), Kotlin-UI-/Connector-/63/63 ARM64-Prebuilt-Audits, Geometrie-/Druckregressionen, Release-Lint (0 Fehler/372 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check` bestanden. Der native Cache wurde aus den 54 Quellbibliotheken erneuert. Die Debug-App wurde auf `emulator-5554` installiert und ohne Absturz gestartet; ADB erkannte kein Xiaomi.

Debug-APK: `outputs/OrkanSlicer_0.8.127_debug.apk` (154.007.268 Byte; SHA-256 `5CFEA2BD4E6FE56162ACD3287559DCF8C9DD89E4D15F740E23FAF8CA9925AB63`). Release-APK: `outputs/OrkanSlicer_0.8.127_release_unsigned.apk` (146.559.339 Byte; SHA-256 `43608A40969AC044F9C895223529CC3779F39E82678C4488497213F586B1F992`); ohne Release-Keystore unsigniert. Bericht und Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.127*`.

## Build 0.8.126 – Renderer-Viewport und Modellberührung

Der GL-Renderer speichert nun die tatsächliche Breite und Höhe seiner Oberfläche, bevor er die Projektion berechnet. Zuvor blieben die Werte null, sodass die Modell-Trefferprüfung auf dem Android-16-Emulator keinen Treffer lieferte. Der Gerätetest lädt einen echten STL-Würfel, bestätigt Renderer-/Oberflächenmaße, tippt zweimal und hält gedrückt. Auswahl und eingeklappte Aktionsleiste bleiben erhalten, ohne dass ein modaler Dialog erscheint.

Build 0.8.126 (`versionCode 161`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand den vollständigen ARM64-Quellbuild in 28:33 Minuten, 279/279 Android-Unit-Tests, 96/96 Headless-E2E-Tests, 3/3 Android-16-Instrumentierungstests, Java/Kotlin-Interop, Profilrouten (470 Einstellungen/91 deutsche Gruppen), Kotlin-UI-/Connector-/63/63 ARM64-Prebuilt-Audits, Release-Lint (0 Fehler/372 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Der native Cache wurde aus den 54 Quellbibliotheken erneuert. Die Debug-App wurde auf `emulator-5554` installiert und ohne Absturz gestartet; ADB sah kein Xiaomi.

Die Folgeprüfung ergänzt einen echten Compose-Rotationsfall für die App-Einstellungen: Der Test öffnet den Einstellungsbereich, klappt „Oberfläche“ auf und prüft, dass dieselbe Ansicht samt geöffnetem Bereich Hochformat → Querformat → Hochformat und den Wechsel zwischen unterer Navigation und Seitenleiste übersteht. Der verifizierte ARM64-Quellcache wurde wiederverwendet. Erneut bestanden 279/279 Unit-Tests, 96/96 Headless-E2E-Tests, 3/3 Android-16-Instrumentierungstests und alle UI-, Profil-, Connector-, G-Code- und APK-Audits; Sammellauf 1:57 Minuten. Die Debug-App wurde aktualisiert auf dem Emulator installiert. ADB listete weiterhin nur den Emulator.

Debug-APK: `outputs/OrkanSlicer_0.8.126_debug.apk` (154.525.830 Byte; SHA-256 `61DC679076057C3264F9B0206C73B198541A9A7AE2A2395A3179D5E141CE0EDB`). Release-APK: `outputs/OrkanSlicer_0.8.126_release_unsigned.apk` (146.559.339 Byte; SHA-256 `8E7B770130B12D5FF303C2F4D4F0C8F9EBA8CB7F96EBB31D9D05B8773DAD409A`); ohne Release-Keystore unsigniert. Bericht und Prüflog liegen unter `outputs/OrkanSlicer_0.8.126*`.

## Build 0.8.112 – Druckplattenberührung abgesichert

Die Berührungsauswertung im 3D-Arbeitsbereich akzeptiert nur noch einen vollständigen Ein-Finger-Tipp. Abgebrochene, gezogene und Mehrfinger-Gesten sowie ein Loslassen ohne vorangegangenes Antippen lösen keine Objektauswahl mehr aus. Beim langen Drücken zum Aufnehmen und Verschieben bleibt die Auswahl erhalten, die Aktionsleiste wird dabei jedoch nicht geöffnet. Die drei neuen Touch-Gate-Tests prüfen Einmaligkeit, Abbruch, Ziehen und Mehrfinger-Eingaben. Die Drucker-/Filamentstatuskarte wurde außerdem nach Compose migriert und zeigt lange Bezeichnungen lesbar gekürzt.

Build 0.8.112 (`versionCode 147`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 261/261 Android-Unit-Tests, 95/95 Headless-E2E-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, Profilrouten (470 sichtbare Einstellungen/91 deutsche Gruppen), Kotlin-UI-, Connector- und 63/63 ARM64-Prebuilt-Audits, Release-Lint (0 Fehler/383 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Der ARM64-Cache wurde aus der source-built Debug-APK dieses Stands erneuert und als 54 Bibliotheken geprüft. Das angeschlossene Xiaomi war in ADB nicht sichtbar; Installations- und manueller Touchtest auf dem Gerät bleiben offen. Slice-/Rotations-Instrumentierung wurde kompiliert.

Debug-APK: `outputs/OrkanSlicer_0.8.112_debug.apk` (154.434.776 Byte; SHA-256 `9C8B53B408284153ABAAA294D4B342E6ABDA4D622610EF59E053DFB1ECC852AD`). Release-APK: `outputs/OrkanSlicer_0.8.112_release_unsigned.apk` (146.518.403 Byte; SHA-256 `7F6320CAEB0F4E6536EAE0D4F552A0A38BF8D68998241342F69D50D0BB443992`); ohne Release-Keystore unsigniert. Bericht und Prüfprotokoll: `outputs/OrkanSlicer_0.8.112_Buildbericht.md` und `outputs/OrkanSlicer_0.8.112/verification-transcript.txt`.

## Build 0.8.111 – Druckplattenleiste in Compose

Die Leiste für mehrere Druckplatten verwendet jetzt Compose mit klar getrennten Schaltflächen für Plattenauswahl, Schließen und Hinzufügen. Die aktive Platte wird in der horizontalen Liste sichtbar gehalten; Auswahl und Schließen sind auch für Screenreader getrennt benannt. Schließen der letzten Platte setzt sie weiterhin auf eine neue leere Platte zurück. Sechs neue Policy-Tests prüfen aktives Schließen, Indexverschiebungen und ungültige Indizes.

Build 0.8.111 (`versionCode 146`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 258/258 Android-Unit-Tests, 95/95 Headless-E2E-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, Profilrouten (470 sichtbare Einstellungen/91 deutsche Gruppen), Kotlin-UI-, Connector- und 63/63 ARM64-Prebuilt-Audits, Release-Lint (0 Fehler/383 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die ARM64-Bibliotheken wurden aus den Quellen neu gebaut. ADB zeigte kein angeschlossenes Gerät; daher wurde 0.8.111 nicht auf dem Xiaomi installiert oder interaktiv geprüft. Slice-/Rotations-Instrumentierung wurde kompiliert.

Debug-APK: `outputs/OrkanSlicer_0.8.111_debug.apk` (153.900.658 Byte; SHA-256 `2F4A05060F05C2A64D4FA182348F9CFE98A0785EADEB191CB5C293C2C8958E82`). Release-APK: `outputs/OrkanSlicer_0.8.111_release_unsigned.apk` (146.518.403 Byte; SHA-256 `B5CBB849BD1517AB1CB24F81127000B80C42691825F420A6A29B488DD94B41B7`); ohne Release-Keystore unsigniert. Bericht und Prüfprotokoll: `outputs/OrkanSlicer_0.8.111_Buildbericht.md` und `outputs/OrkanSlicer_0.8.111/verification-transcript.txt`.

## Build 0.8.110 – Modell-Aktionsleiste in Compose

Die Aktionsleiste für ein ausgewähltes Modell verwendet jetzt das gemeinsame Compose-Theme. Modellname und Verschieben, Drehen, Skalieren, Duplizieren und Weitere bleiben erhalten; die Aktionschips sind mindestens 48 dp hoch, barrierefrei als Schaltflächen markiert und bei schmalen Displays horizontal scrollbar. Der lange Modellname kann separat horizontal gelesen werden. Theme-Wechsel werden in der eingebetteten Compose-Ansicht aktualisiert. Zwei neue Unit-Tests sichern Aktionsreihenfolge und Beschriftungen; der Kotlin-UI-Audit prüft Theme, Auswahlzustand und alle Aktionen.

Build 0.8.110 (`versionCode 145`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 252/252 Android-Unit-Tests, 95/95 Headless-E2E-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, Profilrouten (470 sichtbare Einstellungen/91 deutsche Gruppen), Kotlin-UI-, Connector- und 63/63 ARM64-Prebuilt-Audits, Release-Lint (0 Fehler/383 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die ARM64-Bibliotheken wurden erfolgreich aus den Quellen gebaut. Das Xiaomi 13 verschwand vor der Installation aus ADB; Build 0.8.110 wurde daher noch nicht auf dem Gerät geprüft. Instrumentierung für Slicing und Rotation wurde kompiliert.

Debug-APK: `outputs/OrkanSlicer_0.8.110_debug.apk` (153.900.654 Byte; SHA-256 `DC3A21463210110BB1976763EA7B41796D2C685A34E3630620D78AC59764AC48`). Release-APK: `outputs/OrkanSlicer_0.8.110_release_unsigned.apk` (146.502.019 Byte; SHA-256 `306BA734B73B4A801E09C024C3B6A7652BA0D3863B8D68287A3D2AEF44058F38`); ohne Release-Keystore unsigniert. Bericht und Protokoll: `outputs/OrkanSlicer_0.8.110_Buildbericht.md` und `outputs/OrkanSlicer_0.8.110/verification-transcript.txt`.

## Build 0.8.109 – Plattenberührung ohne automatisches Aktionsmenü

Ein langes Berühren der Platte oder eines Modells öffnet kein Aktionsmenü mehr; das Auswählen eines Modells in „Objekte“ löst ebenfalls keine Aktionen automatisch aus. Die Modellaktionen bleiben über „Mehr“ erreichbar. Die Änderung wurde auf dem Xiaomi 13 mit Android 16 manuell geprüft; nach dem Kaltstart blieb die Platte leer und das Kobra-Profil erhalten.

## Build 0.8.108 – Clipper2-Mehrzeilenfüllung

Die bisher fehlende native Mehrzeilenfüllung verwendet jetzt OrcaSlicers Clipper2-Implementierung aus dem festgeschriebenen Orca-Quellstand. Clipper2 1.5.2 ist mit Lizenz- und Herkunftshinweis eingebunden; die Füllpfad-Umrechnung ist im Android-Native-Build enthalten.

Build 0.8.108 (`versionCode 143`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 250/250 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, Profilrouten (470 sichtbare Einstellungen/91 deutsche Gruppen), Kotlin-UI-, Connector- und 63/63 ARM64-Prebuilt-Audits, Release-Lint (0 Fehler/384 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die ARM64-Bibliotheken wurden aus den Quellen gebaut. Auf dem Xiaomi 13 mit Android 16 wurde die Debug-App installiert und kalt gestartet. Derselbe 10-mm-Testwürfel wurde mit `fill_multiline=2` und anschließend `fill_multiline=1` tatsächlich gesliced; beide G-Codes enthalten 50 Schichten und 42 Abschnitte mit sparsamer Füllung. Der Wert 2 erzeugte 1.629 Füllbewegungen, der Wert 1 2.058. Beide Durchläufe liefen ohne Absturz. Danach wurden die Testwerte in den gespeicherten Profilen auf 1 zurückgesetzt und kontrolliert; ein Kaltstart zeigte die leere Druckplatte, das gespeicherte Profil `0.20mm High Quality` und einen aktiven App-Prozess.

Debug-APK: `outputs/OrkanSlicer_0.8.108_debug.apk` (153.884.272 Byte; SHA-256 `808563FF814DF2589DD45C20167899B7AB2A2CA49B5F6A7EA324B5D13FB88AE9`). Release-APK: `outputs/OrkanSlicer_0.8.108_release_unsigned.apk` (146.502.019 Byte; SHA-256 `33A0EDDFDFBA0B96495CD965EAA0F225F729D3168F0A5A8A9CD8F1368469FD8C`); ohne Release-Keystore unsigniert. G-Code, Profilrücksetzung, Screenshots und Build-Protokoll liegen in `outputs/OrkanSlicer_0.8.108/`.

## Build 0.8.91 – Filament-Kühlung und Temperaturregelung ergänzt

Sechs Orca-Filamentregler sind in passende mobile Gruppen aufgenommen: Aktivierung der Kammertemperaturregelung, Lüfter für Stützflächen und Bügeln, Überhang-/Brückenlüfter, volle Drehzahl des Zusatzlüfters ab einer wählbaren Schicht sowie „Außenwand nicht verlangsamen“. Die Werte werden als Filamentoptionen klassifiziert und bleiben beim Profilimport und Speichern erhalten. Die zentrale allgemeine Heizbett-Temperatur bleibt bestehen; plattenabhängige Temperaturduplikate und experimentelle Volumenstromoptionen bleiben verborgen.

Build 0.8.91 (`versionCode 126`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 191/191 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Profilrouten (466 sichtbare Einstellungen/91 deutsche Gruppen), Kotlin-UI-, Connector- und 63/63 ARM64-Prebuilt-Audits, Upload-/Filament-/G-Code/Anycubic-, Lithophan- und Fastener-Regressionen, Release-Lint (0 Fehler/379 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Slice-/Rotations-Instrumentierungstests wurden kompiliert. ADB und Windows-PnP erkannten das als angeschlossen gemeldete Xiaomi nicht; Installation und Starttest waren deshalb nicht möglich. Der Gradle-Sammellauf dauerte 25:55 Minuten.

Debug-APK: `outputs/OrkanSlicer_0.8.91_debug.apk` (153.524.790 Byte; SHA-256 `F084EA59F03264C2A550544A5D56B59486CDD5C30190722CA86612143525FB30`). Release-APK: `outputs/OrkanSlicer_0.8.91_release_unsigned.apk` (146.224.459 Byte; SHA-256 `D3D107094420011B4F214CE14F73D2774E2928E8E1CA5E4E14D672EE8AC98154`); ohne Release-Keystore unsigniert. Bericht und Protokoll: `outputs/OrkanSlicer_0.8.91_Buildbericht.md` und `outputs/OrkanSlicer_0.8.91/verification-transcript.txt`.

## Build 0.8.90 – Support- und Überhangoptionen ergänzt

Fünf zuvor nicht dargestellte Orca-Optionen sind in passenden Prozessgruppen ergänzt: Stützfluss, Stützflächen-Flussfaktor, Überlappung am Schwellwert, Abstand der unteren Stützfläche und Überhangsfluss. Die bestehende native Orca-Metadaten-, Profil- und Speicherroute versorgt die Eingaben.

Build 0.8.90 (`versionCode 125`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 188/188 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Profilrouten (460 sichtbare Einstellungen/91 deutsche Gruppen), Kotlin-UI-, Connector- und 63/63 ARM64-Prebuilt-Audits, Upload-/Filament-/G-Code/Anycubic-, Lithophan- und Fastener-Regressionen, Release-Lint (0 Fehler/379 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Slice-/Rotations-Instrumentierungstests wurden kompiliert. Das Xiaomi wurde trotz Anschlussmeldung weder von ADB noch von Windows-PnP erkannt; Installation und Starttest waren deshalb nicht möglich. Der Gradle-Sammellauf dauerte 20:24 Minuten.

Debug-APK: `outputs/OrkanSlicer_0.8.90_debug.apk` (153.524.774 Byte; SHA-256 `45A588842B5446886AF11D00CB1DE90FD5F81CFE799132CE6ACF2749B2A26C17`). Release-APK: `outputs/OrkanSlicer_0.8.90_release_unsigned.apk` (146.224.459 Byte; SHA-256 `7CF145F78D12C92929A1A75F0AE2BBBCBF831BB73780076B2B3F9B0BE4C5C7F3`); ohne Release-Keystore unsigniert. Bericht und Protokoll: `outputs/OrkanSlicer_0.8.90_Buildbericht.md` und `outputs/OrkanSlicer_0.8.90/verification-transcript.txt`.

## Build 0.8.89 – Mehrmaterial-Verzahnung in der mobilen Profileinstellung

Die mobile Prozessübersicht stellt nun sechs native Orca-Optionen für Beam-Interlocking in einer deutschen Gruppe bereit. Vorhandene Orca-Definitionen, Profilwerte und Slice-Pfade bleiben die Quelle für Verhalten und Standardwerte.

Build 0.8.89 (`versionCode 124`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 187/187 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Profilrouten (455 sichtbare Einstellungen/90 deutsche Gruppen), Kotlin-UI-, Connector- und 63/63 ARM64-Prebuilt-Audits, Upload-/Filament-/G-Code/Anycubic-, Lithophan- und Fastener-Regressionen, Release-Lint (0 Fehler/379 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Slice-/Rotations-Instrumentierungstests wurden kompiliert. Das Xiaomi wurde trotz Anschlussmeldung weder von ADB noch von Windows-PnP erkannt; Installation und Starttest waren deshalb nicht möglich. Der Gradle-Sammellauf dauerte 19:58 Minuten.

Debug-APK: `outputs/OrkanSlicer_0.8.89_debug.apk` (153.524.682 Byte; SHA-256 `1970280FF146E2ED58D13F8A183788F718EB679098D1CB0116F247606B712D92`). Release-APK: `outputs/OrkanSlicer_0.8.89_release_unsigned.apk` (146.224.407 Byte; SHA-256 `47047EC0A7B57F29BAAD10A12208B8EBC964BDC673A217AEC32E599DAEB341C9`); ohne Release-Keystore unsigniert. Bericht und Protokoll: `outputs/OrkanSlicer_0.8.89_Buildbericht.md` und `outputs/OrkanSlicer_0.8.89/verification-transcript.txt`.

OrkanSlicer wird in kleinen, zusammengehörigen Änderungspaketen weiterentwickelt. Nach jeder einzelnen Datei startet kein kompletter Android-/ARM64-Build mehr. Stattdessen wird ein Paket fertiggestellt und am gemeinsamen Prüfpunkt mit einem einzigen Gradle-Aufruf, allen Regressionen und bei Bedarf einem Gerätestart geprüft. Gradle-Ausgaben erscheinen dabei fortlaufend im Terminal und werden parallel im Buildprotokoll gespeichert; lange native Schritte bleiben dadurch sichtbar.

Der Arbeitsrhythmus ist paketbasiert: Mehrere Änderungen derselben Oberfläche oder Logikschicht werden gemeinsam fertiggestellt und erst am Paketende gezielt kompiliert. Druckablauf, Dateiimport und App-Lebenszyklus bleiben getrennte Pakete. Die vollständige APK-, Native-, Lint- und Regressionmatrix läuft einmal pro abgeschlossenem Paket. Wird dabei nur ein Prüfskript fehlerhaft, wird die Korrektur gegen dieselben erzeugten Klassen und APKs erneut geprüft (`-SkipGradleBuild`); native Bibliotheken werden nicht unnötig neu kompiliert. Bei Änderungen am App-Code startet der nächste Paketabschluss wieder den normalen Sammellauf.

Für die Kotlin-Migration bedeutet das: mehrere einfache Klassen derselben Schicht werden erst gemeinsam umgestellt und dann zusammen geprüft. Dateien mit unterschiedlicher Laufzeitwirkung oder erhöhtem Risiko (z. B. Druckstart, Netzwerk-Upload, Modellimport oder App-Lebenszyklus) bleiben eigene Änderungspakete. So werden häufige Vollbuilds vermieden, ohne Fehler über mehrere unabhängige Funktionen zu vermischen. Nach einem Paket wird erst bei einem konkreten Fehler gezielt nachgebessert und anschließend derselbe Sammellauf erneut gestartet.

## Ein Sammellauf

Aus dem Projektordner:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\verify-and-package.ps1 -InstallSerial e7dbd22e
```

`-InstallSerial` ist optional. Ohne diese Angabe baut und prüft der Lauf lokal und kompiliert den isolierten nativen Slice-Instrumentierungstest, führt ihn aber nicht aus. Mit Gerätekennung führt der Lauf zuerst den ARM64-Slice-Test im separaten `sliceTest`-Build auf dem ausgewählten Gerät aus. Anschließend installiert er die normale Debug-APK per `adb install -r`, erhält damit vorhandene App-Daten, startet die App und kontrolliert Paketversion sowie laufenden Prozess. Bei Testfehlern oder Absturz-/ANR-/OOM-Meldungen bricht er ab. Der native Test nutzt ein eingebettetes Modell und ruft keinen Drucker oder Netzwerkdienst auf.

Der Sammellauf bündelt:

1. Android-Unit-Tests, Debug-/Release-Paketierung, vollständiges Release-Lint, Ermittlung des Test-Classpaths und Kompilierung des Slice-Instrumentierungstests in einem Gradle-Aufruf. Mit `-InstallSerial` ersetzt der Aufruf die reine Kompilierung durch `connectedSliceTestAndroidTest` und führt den nativen Test auf dem angegebenen Gerät aus.
2. Kotlin-/UI-, Schnittverbinder- und ARM64-Vorprüfungen.
3. Headless-E2E-, Upload-/Filament-/G-Code-, Lithophane- und Fastener-Regressionen.
4. Java/Kotlin-Interoperabilitätsprüfungen für migrierte öffentliche Konstruktoren, Felder und statische Methoden sowie `git diff --check`.
5. Versionierte APKs, Lintausgabe, Buildbericht und vollständiges Konsolenprotokoll unter `outputs/`.

Die Regressionstests verwenden den Classpath, den der Hauptlauf schon beim Gradle-Build ermittelt hat. Die Java-Testharness lädt sowohl Java- als auch Kotlin-Klassen; die Uploadtests starten deshalb keinen zweiten Gradle-Prozess. `scripts/tests/run_e2e_tests.ps1` bleibt separat ausführbar und löst dann bei Bedarf seine eigene Classpath-Ermittlung aus.

`-SkipGradleBuild` ist nur für bereits gebaute Ausgaben gedacht, etwa für Regressionen nach einer reinen Testskriptänderung. Nach Änderungen an App-Quellen ist der Standardlauf ohne diesen Schalter zu verwenden.

## Gebündelte Kotlin-Schritte

Build 0.8.35 migriert fünf Bootstrap-Aufgaben gemeinsam nach Kotlin: `PrefsTask`, `PrintConfigWarmupTask`, `VibrationUtilsTask`, `ClearModelCacheTask` und `CheckUpdateJsonTask`. Der Java-Interop-Test kontrolliert ihre Konstruktoren und Scheduling-Eigenschaften; der statische Kotlin-Audit kontrolliert zusätzlich ihre Java-Aufrufverträge. Die komplette Prüfmatrix lief einmal nach Abschluss des Pakets: 106 Unit-Tests, 94 Headless-E2E-Tests, Upload-/Filament-/G-Code-, Lithophane- und Fastener-Regressions, ARM64-Prebuilt- und UI-Audits, Release-Lint sowie ein Installations-/Starttest auf dem Xiaomi 13. Alles bestanden. Das Paket änderte den Quellbestand von 100/143 auf 95 Java- und 148 Kotlin-Dateien.

Build 0.8.36 migriert fünf Druckvorbereitungs-Helfer gemeinsam: `FilamentTypes`, `FilamentPaletteSyncPolicy`, `GCodeToolMapper`, `GCodeIntegrityValidator` und `PrinterWebOriginPolicy`. Der bestehende Regressionstestläufer verwendet nun ausschließlich die gebauten App-Klassen für diese Helfer und legt Testklassen in einem separaten Verzeichnis ab, damit ältere Java-Kompilate keine migrierten Kotlin-Klassen überdecken. Die einmalige Gesamtprüfung bestand mit 106 Unit-Tests, 94 Headless-E2E-Tests, allen Druckdaten-, Lithophane- und Fastener-Regressions, Release-Lint und statischen Audits. Die Debug-App wurde auf dem Xiaomi 13 gestartet. Der Quellbestand beträgt jetzt 90 Java- und 153 Kotlin-Dateien.

Build 0.8.37 bündelt fünf weitere Helfer aus dem Bild-/Maker- und Geometriebereich: Lithophane-Eingabe und Tonzuordnung, Befestigermaße, CSS-Farbpalette und `Vec3d`. Der Regressionstestläufer schreibt seine Testklassen in einen eigenen, vor dem Lauf geleerten Ordner; Produktionsklassen und Tests können so nicht mehr durch alte `.class`-Dateien verwechselt werden. Die gesamte Prüfung lief nach Fertigstellung des Pakets: 106 Unit-Tests, 94 Headless-E2E-Tests, Java/Kotlin-Interop, Druckdaten-/Upload-, Lithophane- und Befestigergeometrie, Release-Lint, ARM64-Prebuilt- und Kotlin-UI-Audits. Der erste Testlauf fand den veralteten Klassenpfad-Eintrag und wurde nach der Korrektur vollständig wiederholt. Die Debug-App wurde auf dem Xiaomi 13 gestartet. Es sind jetzt 85 Java- und 158 Kotlin-Dateien im App-Modul.

Build 0.8.38 migriert zehn kleine EventBus-Signale gemeinsam nach Kotlin. Öffentliche parameterlose Konstruktoren und Erweiterbarkeit bleiben erhalten; der Java/Kotlin-Interoptest prüft beide Verträge. Die gebündelte Prüfung bestand mit 106 Unit-Tests, 94 Headless-E2E-Tests, Interop-, Upload-/G-Code-, Lithophane- und Fastener-Regressions, Release-Lint sowie den statischen Profil-, Kotlin-UI-, Schnittverbinder- und ARM64-Audits. Die Debug-App wurde auf dem Xiaomi 13 installiert und gestartet. Der Bestand liegt jetzt bei 75 Java- und 168 Kotlin-Dateien.

Build 0.8.39 bündelt sechs Start- und Initialisierungstasks in einer Änderung: `BootTask`, `CloudCachedInitTask`, `CloudInitTask`, `EventBusTask`, `BeamServerDataTask` und `LoadSlic3rConfigTask`. Task-Reihenfolge, Java-Aufrufverträge und Profilinitialisierung werden gemeinsam geprüft. Statt nach jedem Task einen Vollbuild auszuführen, erfolgten ein Kotlin-/Java-Paketcompile als früher Sprachgrenzencheck und danach genau ein vollständiger Sammellauf. Er bestand mit 106 Unit-Tests, 94 Headless-E2E-Tests, Interop-, Druckdaten-/Upload-, Lithophane- und Fastener-Regressionen, ARM64-Prebuilt- und Kotlin-UI-Audits, Profilrouten-Audit und Release-Lint. Die Debug-App wurde auf dem Xiaomi 13 installiert und gestartet. Der Bestand liegt jetzt bei 69 Java- und 174 Kotlin-Dateien.

Build 0.8.40 schließt das Startpaket mit `AppBoot` und `TrueTimeTask` ab. Ein Headless-Interoptest prüft die tatsächliche Worker-zu-Hauptthread-Abhängigkeit des Schedulers. Nach dem gemeinsamen Paketcompile wurde die Prüfsuite ausgeführt; danach folgte genau ein vollständiger ARM64-/Android-Sammelbuild. Alle 106 Unit-Tests, 94 Headless-E2E-Tests, Upload-/Druckdaten-, Lithophane- und Fastener-Regressionen, statischen Audits und Release-Lint bestanden. Die Debug-App wurde auf dem Xiaomi 13 gestartet, bevor das Gerät abgesteckt wurde. Der Bestand liegt jetzt bei 67 Java- und 176 Kotlin-Dateien.

Build 0.8.41 bündelt sechs eng verwandte Java-Datenklassen: vier EventBus-Nutzdaten und beide Smart-Slice-DTOs. Nach dem schnellen Kotlin-/Java-Paketcompile gab es einen vollständigen Gradle-/ARM64-Build. Eine neue Interop-Assertion erwartete zunächst eine gekürzte Dezimalzahl; nach Korrektur auf die tatsächliche Zweistellenformatierung bestanden Java/Kotlin-Interop und alle Regressionen in einem erneuten Prüflauf, der die vorhandenen Builds nutzte. So musste der native Orca-Kern nicht nochmals kompiliert werden. Insgesamt bestanden 106 Unit-Tests, 94 Headless-E2E-Tests, Druckdaten-/Upload-, Lithophane- und Fastener-Regressionen, Profil-/Kotlin-UI-/Schnittverbinder-/ARM64-Audits und Release-Lint. Die Geräteschritte blieben ausgelassen; der Nutzer hatte das Handy abgesteckt und gebeten, ohne Gerät weiterzuarbeiten. Der Bestand beträgt jetzt 61 Java- und 182 Kotlin-Dateien.

Build 0.8.42 migriert drei kleine, eng gekoppelte Helfer: Java-statische Debug-Assertions, Vibrationszugriff und den Fortschrittscallback des nativen Slicers. Ein schneller gemeinsamer Compilecheck und ein Java-Interoptest prüfen die API und die JNI-Methodensignatur, bevor der einzelne vollständige Gradle-/ARM64-Sammellauf beginnt. Danach bestanden 106 Unit-Tests, 94 Headless-E2E-Tests, Interop, Upload-/Filament-/G-Code-, Lithophane- und Fastener-Regressionen, Profil-/Kotlin-UI-/Schnittverbinder-/ARM64-Audits, Release-Lint und Patchprüfung. Das Handy blieb unbenutzt. Das Modul umfasst 58 Java- und 185 Kotlin-Dateien.

Build 0.8.43 migriert `NativeEnumValueSanitizer` und `Slic3rUtils` als zusammengehöriges Profil-/Slicer-Paket. Nach dem schnellen Kotlin-/Java-Interopcheck lief genau ein vollständiger Gradle-/ARM64-Sammellauf. 106 Unit-Tests, 94 Headless-E2E-Tests, Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophane- und Fastener-Regressionen sowie Kotlin-UI-, Profilrouten-, Schnittverbinder- und ARM64-Audits bestanden. Release-Lint: 0 Fehler, 254 Warnungen; Patchprüfung bestanden. Kein Gerätetest, da das Handy abgesteckt war. Der Bestand beträgt 56 Java- und 187 Kotlin-Dateien. Debug- und unsignierte Release-APK, Bericht und vollständiges Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.43*`.

Build 0.8.44 migriert `GCodeProcessorResult` und `PaintSession` als gebündelte native Slicer-Ressourcenwrapper. Ein schneller Kotlin-/Java-Paketcompile wurde durch genau einen vollständigen Gradle-/ARM64-Sammellauf ergänzt. 106 Unit-Tests, 94 Headless-E2E-Tests, Interop mit JNI-Argument- und Freigabekontrollen, Upload-/Filament-/G-Code-/Anycubic-, Lithophane- und Fastener-Regressionen sowie Kotlin-UI-, Profilrouten-, Schnittverbinder- und ARM64-Audits bestanden. Release-Lint: 0 Fehler, 254 Warnungen; Patchprüfung bestanden. Der Sammelrunner zeigt Gradle-Fortschritt jetzt während des Builds an. Das Handy blieb unbenutzt. Der Bestand beträgt 54 Java- und 189 Kotlin-Dateien. Debug-/unsignierte Release-APK, Bericht und Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.44*`.

Build 0.8.45 bündelt die Geometrieanalyse und die Smart-Slice-Regeln in `GeometryAnalysis.kt` und `SmartSliceEngine.kt`. Der Java/Kotlin-Interoptest prüft Cache-/Geometrieverträge und die Smart-Slice-Konfiguration. Er fand und verhinderte außerdem einen Vasenmodusfehler, bei dem allgemeine Wandberechnungen die Zahl der oberen Deckschichten wieder überschrieben. Nach dem schnellen Vertragstest lief genau ein vollständiger Android-/ARM64-Sammelbuild. Alle 106 Unit-Tests, 94 Headless-E2E-Tests, Druckdaten-/Upload-, Lithophan- und Befestigerregressionen, statischen Audits, 63 ARM64-Prebuilt-Prüfungen und `git diff --check` bestanden. Release-Lint: 0 Fehler, 254 Warnungen. Das Handy war abgesteckt, daher erfolgte kein Installations- oder Gerätetest. Version 0.8.45 (`versionCode 80`) umfasst 52 Java- und 191 Kotlin-Dateien. Debug- und unsignierte Release-APK sowie vollständiger Bericht liegen unter `outputs/OrkanSlicer_0.8.45*`.

Build 0.8.46 migriert die 3D-Kamerasteuerung `Camera` und den Koordinatenachsen-Renderer `CoordAxes` gemeinsam nach Kotlin. Kamera- und Render-APIs bleiben für Java-Aufrufer erhalten; neue Interoptests decken Gestenzoom, Grenzen, Richtungsvektoren, View-Matrix-Cache, Verschieben und Orbit ab. Nach dem schnellen Paketcompile und der Headless-Regression lief ein vollständiger ARM64-/Android-Sammelbuild. Alle 106 Unit-Tests, 94 Headless-E2E-Tests, Druckdaten-/Upload-, Lithophan- und Befestigerregressionen, statischen Audits, 63 ARM64-Prebuilt-Prüfungen und `git diff --check` bestanden. Release-Lint: 0 Fehler und 254 Warnungen. ADB meldete kein angeschlossenes Gerät, daher ohne Installationstest. Version 0.8.46 (`versionCode 81`) umfasst 50 Java- und 193 Kotlin-Dateien. Die Debug-APK (153.087.519 Bytes, SHA-256 `3478BF147C7BBCE73F72F83DD5EF1703C9A4C1D24A356F5DE6ED99903A522652`) und die unsignierte Release-APK (145.983.827 Bytes, SHA-256 `199FC94CDA2793EC82C0E33F1835CF2552468F0D5BC003C01D03D21AAFC8235D`) sowie Bericht und Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.46*`.

Build 0.8.47 migriert `Slic3rLocalization` in Kotlin und prüft den Java-Aufrufvertrag, PO-Einträge, deutsche UTF-8-Texte, Fortsetzungszeilen, Escape-Sequenzen, leere/fallback Übersetzungen und Dateiende ohne Leerzeile. Der schnelle Parse-/Interop- und Regressionstest bestand; anschließend lief genau ein kompletter ARM64-/Android-Sammelbuild. Alle 108 Unit-Tests, 94 Headless-E2E-Tests, Druckdaten-/Upload-/G-Code-, Lithophan- und Befestigerregressionen, statischen Audits, 63 ARM64-Prebuilt-Prüfungen und `git diff --check` bestanden. Release-Lint: 0 Fehler und 254 Warnungen. ADB meldete kein Gerät, daher kein Installationstest. Version 0.8.47 (`versionCode 82`) umfasst 49 Java- und 194 Kotlin-Dateien. Die Debug-APK (153.087.523 Bytes, SHA-256 `DF082906022C5A6ECA420BF328446A6CFF92DE188D10334FE67A8039F03855AA`) und die unsignierte Release-APK (145.983.827 Bytes, SHA-256 `943D58AF23B8B7B7240EEC8C5E27289E7195CF0D6B7BB2AF1EC249C74EC9CAD2`) sowie Bericht und Protokoll liegen unter `outputs/OrkanSlicer_0.8.47*`.

Build 0.8.48 bündelt den Renderblock `GLShaderProgram`, `GLShadersManager`, `GLModel` und `Bed3D` in Kotlin. Shader-IDs bleiben Java-Compile-time-Konstanten; Matrix- und Vektor-Uniform-Überladungen, Thread-lokale Puffer, Cache-/Freigabeablauf und der private, statische `@Keep`-JNI-Einstieg bleiben erhalten. Öffentliche Klassen und überschreibbare Methoden bleiben für Java-Unterklassen offen; Mesh-Raycaster, Trefferwerte, Druckbettgrenzen, Render-Aufrufe und Freigabe werden im Java-Interoptest geprüft. Shader-Asset-Streams werden geschlossen, der renderkritische Shader-Lookup bleibt eine direkte Schleife ohne zusätzliche Iteratoren, und das native Druckbett behält die Eigentümerschaft über seine drei Meshes. Frühe Compile- und Interopchecks fanden Kotlin/Java-Grenzabweichungen, die vor dem Sammellauf behoben wurden.

Alle 108 Unit-Tests, 94 Headless-E2E-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, Kotlin-UI-, Profilrouten-, Connector- und ARM64-Prebuilt-Audits (63/63), Release-Lint sowie `git diff --check` bestanden. Release-Lint: 0 Fehler und 254 Warnungen. Das Handy war abgesteckt; es gab keinen Gerätetest. Version 0.8.48 (`versionCode 83`) umfasst 45 Java- und 198 Kotlin-Dateien. Die Änderungen wurden unter derselben laufenden Entwicklungsnummer gebündelt, damit ein reiner Zwischenschritt nicht die große native Slicer-Kompilierung erneut auslöst. Die Debug-APK (153.481.444 Bytes, SHA-256 `94E4886395AD717A129D22195DA2BE37F7499B20FB3226B906635FC886196CED`) und die unsignierte Release-APK (145.983.827 Bytes, SHA-256 `3929D52E1A3D56B1CF09D36B101031214B046FDE787C4C706628BA9D8443CF86`) sowie Bericht und Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.48*`.

Build 0.8.58 migriert die App-Einstellungsseite vollständig auf Compose. Eine schnelle Kotlin-Compile-/Unit-Prüfung fing zuerst einen fehlenden Iconverweis ab. Der vollständige Gradle-/ARM64-Lauf baute danach Debug und Release sowie das native Slicer-Instrumentierungspaket; alle 119 Unit-Tests bestanden und Release-Lint meldete 0 Fehler. Der erste nachgelagerte Lauf fand veraltete Tests des alten Settings-Recycler-Hosts. Nach Umstellung der Prüferwartungen auf die Compose-Aktionen liefen 94/94 Headless-E2E-Tests, Interop, Druckdaten- und Generatorregressionen sowie die Profilschema-, UI-, Connector- und ARM64-Audits erfolgreich. Ein fehlerhafter eingebetteter Python-Aufruf zur ZIP-CRC-Prüfung wurde in ein separates, getestetes Skript verschoben; der Abschlusslauf gegen dieselben APKs bestand. Gesamt: 449 native Orca-Einstellungen/89 Gruppenübersetzungen, 63/63 native Prebuilts, Release-Lint 0/312 und korrekte Version-/SDK-Metadaten; kein Gerätetest, da das Handy abgesteckt war. APKs, Prüfsummen, Bericht und Protokoll stehen in `outputs/OrkanSlicer_0.8.58*`.

Build 0.8.59 migriert `slic3r/Model` nach Kotlin und prüft seine Java/JNI-Kompatibilität samt Modellimport, Bounds-Cache, Extruderbedarf, Merge und Freigabe. Der vollständige Android-/ARM64-Sammelbuild dauerte 20:35 Minuten; 119 Unit-Tests, 94/94 Headless-E2E-Tests, Interop, Druckdaten-/Upload-/G-Code-, Lithophane- und Fastener-Regressionen, UI-/Profil-/Connector-Audits, 63/63 ARM64-Prebuilts, Release-Lint und APK-Metadaten/ZIP-CRC bestanden. Zwei statische Prüfer verwiesen noch auf die entfernte `Model.java`; nach Korrektur liefen alle Abschlussprüfungen erfolgreich, ohne den nativen Build zu wiederholen. Release-Lint: 0 Fehler, 312 Warnungen. Das Handy war abgesteckt; die Slice-Instrumentierung wurde lokal kompiliert. Version 0.8.59 (`versionCode 94`) umfasst 12 Java- und 230 Kotlin-Dateien. APKs, Prüfsummen, Bericht und Prüfprotokoll stehen unter `outputs/OrkanSlicer_0.8.59*`.

## Build 0.8.66 – Rotationstest für Android 16

Der Rotationstest für Hauptaktivität, Navigation und aktive View wurde ergänzt und in Android-Test kompiliert. 129 Android-Unit-Tests, 94 Headless-Tests, Upload-/Druckdaten-, Lithophane- und Fastener-Regressionen, Profil-/Kotlin-UI-/Connector-/ARM64-Audits sowie Release-Lint (0 Fehler/363 Warnungen) bestanden. Kein Gerätelauf in diesem Build.

## Build 0.8.67 – zwölf Java-Dateien nach Kotlin migriert

EventBus-API und Runtime, Annotation-Processor, EventBus-/SAPIL-Demos und der native Slice-Gerätetest wurden gebündelt migriert; die beiden Demo-Apps wurden im Gradle-Sammellauf gebaut. 129/129 Unit-Tests, 94/94 Headless-Tests und die statischen, G-Code-, Upload-, Generator-, Profil- und ARM64-Prüfungen bestanden; Release-Lint: 0 Fehler/363 Warnungen. APK-Version 0.8.67 / versionCode 102 bestand Metadaten- und ZIP-CRC-Prüfungen. Android 16 erkannte das Xiaomi 13, verweigerte aber die separate Test-APK mit `INSTALL_FAILED_USER_RESTRICTED`, sodass 0 Instrumentierungstests ausgeführt wurden. Die normale Debug-App ließ sich anschließend installieren und starten; der Prozess blieb aktiv und das Crash-/ANR-Protokoll sauber. Es wurde kein Drucker angesprochen. Die Debug- und unsignierte Release-APK, Prüfsummen und Protokoll liegen unter `outputs/OrkanSlicer_0.8.67*`.

## Build 0.8.68 – Profileinstellungs-Gruppen in Compose

Gruppenüberschriften und erklärende Hinweise in den Druck-, Filament- und Druckerprofilen verwenden jetzt Compose mit dem gemeinsamen Hell-/Dunkelthema. Überschriften zeigen einen gut antippbaren Auf-/Zuklapp-Pfeil; TalkBack erhält Aktion und Zustand. Die Gruppenbezeichnungen reagieren auf den Sprachwechsel und die Aktions-/Statusmeldungen sind auf Deutsch, Englisch und Russisch vorhanden. Filter, Profilentwurf, Gruppenstatus und Speichern laufen über die bisherigen Controller weiter.

Build 0.8.68 (`versionCode 103`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand den Debug-/Release- und Demo-Build, 129/129 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophane- und Fastener-Regressionen, Profilrouten-/Kotlin-UI-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/365 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Das Slice-/Rotation-Testpaket kompilierte. Beim Installationsversuch meldete ADB kein angeschlossenes Gerät; daher gab es für 0.8.68 keinen Geräte-Start- oder Rotationstest.

Die Debug-APK (`153.381.107` Bytes; SHA-256 `F360AA553B12FD8F908B86B05F893C4C8DE969425009CE5D5FF7F1588FDFDF55`) und die mangels Release-Keystore unsignierte Release-APK (`146.065.335` Bytes; SHA-256 `475CF64E50AC3BC79B19163AD5C02385AACA5C595E81D877F23668C3C7A016EF`) liegen unter `outputs/OrkanSlicer_0.8.68_*.apk`. Buildbericht und vollständiges Protokoll liegen unter `outputs/OrkanSlicer_0.8.68*`.

## Compose-Migration – Profilkategorie-Zeilen, Build 0.8.69

Die auswählbaren Oberkategorien in den Druck-, Filament- und Druckerprofilen verwenden jetzt Compose. Sichtbarer Titel, Symbolfärbung, Klick zum Wechsel, geöffneter Zustand und Pfeilanzeige entsprechen dem bisherigen Profilverhalten. Theme-Farben, aktive Gerätesprache, Screenreader-Zustand und Aktionsbeschreibung werden berücksichtigt. RecyclerView-, Filter-, Profilentwurf- und Orca-Einstellungslogik bleiben erhalten.

Build 0.8.69 (`versionCode 104`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 129/129 Unit-Tests, 94/94 Headless-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophane- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Compose-/Kotlin-UI- und Schnittverbinder-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/365 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Der ARM64-/Android-Sammellauf dauerte 20:28 Minuten. Das Xiaomi erschien auch nach Neustart des ADB-Dienstes nicht in `adb devices -l`; darum waren Installation, Start und visuelle Prüfung nicht möglich. Die Slice-/Rotation-Instrumentierung wurde lokal kompiliert, aber nicht auf dem Gerät ausgeführt.

Die Debug-APK (`153.250.951` Bytes; SHA-256 `5509406DFB56DBC4B026D2CD3AAF0DE6319D43BF97236E50CE7DB58BB0DD7542`) und die mangels Release-Keystore unsignierte Release-APK (`146.065.335` Bytes; SHA-256 `65883D8FF6C98A696A401A7A18BA937CC6E455CAA951840EBA5F1BA33B43A4CA`) liegen unter `outputs/OrkanSlicer_0.8.69_*.apk`. Bericht und vollständiges Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.69*`.

## Compose-Migration – Cloud-Kopf in Profilen, Build 0.8.70

Der Cloud-Kontokopf in den Drucker-, Druck- und Filamentprofilen ist jetzt eine Compose-Zeile. Anmelde-, Lade- und Abmeldezustände, Name, Untertitel und die bisherige Öffnungsaktion bleiben erhalten. Glide lädt weiterhin Profilbilder; die in Compose eingebettete Android-ImageView wird bei Zeilenfreigabe bereinigt. Ellipsen, Theme-Farben und eine zusammengeführte Screenreader-Beschreibung unterstützen schmale Displays und Bedienungshilfen. Der Profilaudit prüft die Cloud-Zustände, die Avatarfreigabe, Aktionssemantik sowie das Entfernen der alten Profilzeile.

Build 0.8.70 (`versionCode 105`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 129/129 Android-Unit-Tests, 94/94 Headless-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophane- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Kotlin-UI-/Profil-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/365 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Der vollständige Android-/ARM64-Sammellauf dauerte 20:46 Minuten. ADB und die Windows-Gerätesuche fanden kein angeschlossenes Xiaomi; daher wurden Installation, Start und visuelle Prüfung nicht ausgeführt. Das Slice-/Rotation-Instrumentierungspaket kompilierte lokal.

Die Debug-APK (`153.250.960` Bytes; SHA-256 `0B268354FC30C73537FD98BFBD0D1D82A060BC4FCD9993F5CA054DE84FA65D65`) und die mangels Release-Keystore unsignierte Release-APK (`146.081.719` Bytes; SHA-256 `45D0869C211DCD6EE25B44BA393F56859966064B0A6EC511208AF3656A491363`) liegen unter `outputs/OrkanSlicer_0.8.70_*.apk`. Bericht und vollständiges Protokoll liegen unter `outputs/OrkanSlicer_0.8.70*`.

## Build 0.8.71 – Profilaktionen mit Compose

Die Fußleiste im Profilbereich wurde auf Compose umgestellt. Speichern und Zurücksetzen bleiben erhalten; Dialoge zeigen verständliche, lokalisierte Bestätigungen, eine scrollbare Zusammenfassung ausstehender Profiländerungen sowie Eingabeprüfung für Profilnamen. Die vorhandenen Profileinstellungen und Speicherroutinen bleiben über den bisherigen Controller verbunden.

Build 0.8.71 (`versionCode 106`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 131/131 Android-Unit-Tests, 94/94 Headless-Tests, Java/Kotlin-Interop, Druckdaten-/Upload-/Filament-/G-Code-/Anycubic-, Lithophane- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Compose-/Kotlin-UI-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/365 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die Slice-/Rotation-Instrumentierung kompilierte. Nach ADB-Neustart listete Windows weiterhin kein Android-Gerät; Installation und Gerätestart waren deshalb nicht möglich. Der Gradle-Sammellauf dauerte 20:23 Minuten.

Die Debug-APK (`153.269.044` Bytes; SHA-256 `2EA4176A1DB5205EDF3B75D77CA63AB7B2AEF65296E42310FD20B88F7CEE82B4`) und die mangels Release-Keystore unsignierte Release-APK (`146.083.443` Bytes; SHA-256 `872E69B8113BF7CC1EE2B8B28D20689BB6F1DE3FAE7ED02DDE8D394E707E4E0B`) liegen unter `outputs/OrkanSlicer_0.8.71_*.apk`. Bericht und vollständiges Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.71*`.

## Build 0.8.72 – letzter Profiloption-Rückfallpfad in Compose

Der verbliebene klassische Dialog-Editor für Orca-Profilwerte wurde durch einen Compose-Rückfall-Editor ersetzt. Profiloptionen verwenden jetzt durchgängig Compose, einschließlich ungewöhnlicher Enum- und Listeneinträge sowie mehrzeiliger Werte. Wertgrenzen für Zahlen und Vektoren werden vor dem Speichern validiert; Enum-Auswahl, Abbrechen, Bestätigen und Long-Press-Hilfe bleiben erhalten.

Build 0.8.72 (`versionCode 107`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 134/134 Android-Unit-Tests, 94/94 Headless-Tests, Java/Kotlin-Interop, Druckdaten-/Upload-/Filament-/G-Code-/Anycubic-, Lithophane- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Compose-/Kotlin-UI-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/365 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die Slice-/Rotation-Instrumentierung kompilierte. ADB listete kein angeschlossenes Gerät; Installation und Gerätestart blieben deshalb offen. Der Gradle-Sammellauf dauerte 20:34 Minuten.

Die Debug-APK (`153.285.444` Bytes; SHA-256 `E24C0440A334C0411EA6FA3911A2CD9A281774AD828BA345CE579AF4A761CAD8`) und die mangels Release-Keystore unsignierte Release-APK (`146.099.827` Bytes; SHA-256 `2D6E554517BE7BDC3CCCEE2465D2E055DCBD01A8877E7D849BBDC69A1B91E96C`) liegen unter `outputs/OrkanSlicer_0.8.72_*.apk`. Bericht und vollständiges Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.72*`.

## Build 0.8.73 – gemeinsamer Profil-Listenhost in Compose

Die gemeinsame Liste für Drucker-, Druck-, Filament- und App-Einstellungen verwendet jetzt Compose `LazyColumn` statt RecyclerView. Kategorieabschnitte, Farbkarten, Filter-Leerzustände, Cloud-Kopf, Zeilenaktualisierung und Pager-Kantenüberblendung bleiben erhalten. Bestehende Orca-Einstellungszeilen werden innerhalb der Compose-Liste weiterverwendet, damit ihre Editier- und Speicherlogik unangetastet bleibt.

Build 0.8.73 (`versionCode 108`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 136/136 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Kotlin-UI-/Profil-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/363 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Das Slice-/Rotation-Instrumentierungspaket kompilierte. Obwohl das Xiaomi als angeschlossen gemeldet wurde, listeten ADB und Windows kein Gerät; Installation, Start und visuelle Prüfung waren daher nicht möglich. Der erfolgreiche Gradle-Sammellauf dauerte 2:07 Minuten bei bereits gebauten ARM64-Nativdateien.

Die Debug-APK (`153.376.528` Bytes; SHA-256 `0AEA5F0276CCFDAF5C767316141A9238EE736E939AAC444A3F37F10168ACC766`) und die mangels Release-Keystore unsignierte Release-APK (`146.099.827` Bytes; SHA-256 `8A41ABF58D77B025636E472AFD4F2FADDF2B8F694DF46D0BC8D308846BC088C7`) liegen unter `outputs/OrkanSlicer_0.8.73_*.apk`. Bericht und vollständiges Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.73*`.

## Build 0.8.74 – Slicing-Fortschritt in Compose

Der Slicing-Fortschrittsdialog zeigt Status, Prozentwert, Fortschrittsbalken und Schrittprotokoll nun mit Compose. Ein unveränderlicher Zustandsreduzierer begrenzt den Prozentwert, erhält den Status bei leeren Ereignismeldungen, übersetzt und dedupliziert aufeinanderfolgende Schritte und schließt den Dialog ab 100 Prozent. EventBus-Anmeldung, automatische Scrollposition zum neuesten Schritt und das erweiterte Bottom-Sheet-Verhalten bleiben erhalten.

Build 0.8.74 (`versionCode 109`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 140/140 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Kotlin-UI-/Profil-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/365 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Das Slice-/Rotation-Instrumentierungspaket kompilierte. ADB listete trotz gemeldetem Anschluss kein Gerät; Installation, Start und visuelle Prüfung waren nicht möglich. Der erfolgreiche Android-/ARM64-Sammellauf dauerte 20:44 Minuten.

Die Debug-APK (`153.301.835` Bytes; SHA-256 `84ED83C28A16C0B3D2B0022016F59697E3E4D91D1966AC137BE3D6C5137B8187`) und die mangels Release-Keystore unsignierte Release-APK (`146.099.827` Bytes; SHA-256 `367EE2FFEF24E237E778C24B9EED25D3D2ED7A9255DC226655E9C0F60C1739C4`) liegen unter `outputs/OrkanSlicer_0.8.74_*.apk`. Bericht und vollständiges Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.74*`.

## Build 0.8.75 – Cloud-Verwaltung in Compose

Der Cloud-Verwaltungsdialog verwendet jetzt Compose. Er zeigt Lade- und Kontostatus, die höchste gültige Abo-Stufe, den passenden Abo-Verwaltungslink, die nur bei berechtigtem Abo verfügbare Profilsynchronisierung sowie Abmelden. Ungültige oder nicht erreichbare Weblinks werden abgefangen und verständlich gemeldet. Drei Zustands-Unit-Tests decken Abo-Auswahl, Synchronisierungsberechtigung und unvollständige Cloud-Daten ab.

Build 0.8.75 (`versionCode 110`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 143/143 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Kotlin-UI-/Profil-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/366 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Das Slice-/Rotation-Instrumentierungspaket kompilierte. ADB listete trotz gemeldetem Anschluss kein Gerät; daher waren Installation, Start und visuelle Prüfung nicht möglich. Der vollständige Android-/ARM64-Sammellauf dauerte 22:13 Minuten.

Die Debug-APK (`153.318.849` Bytes; SHA-256 `4C495AAA6E76DEDBAF286CDBD8025C6B0FE1BBEDB7251A74FA39AED59DB93AAF`) und die mangels Release-Keystore unsignierte Release-APK (`146.116.843` Bytes; SHA-256 `96918A7B6D5637E90E59A64842AC772B14BC2F03769F0DEBF85088554C3F8F06`) liegen unter `outputs/OrkanSlicer_0.8.75_*.apk`. Bericht und vollständiges Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.75*`.

## Build 0.8.76 – adaptive Farbauswahl und Filament-Slot-Editor

Die CSS- und häufige Farbauswahl verwendet jetzt eine adaptive Compose-Palette mit gut antippbaren, für Screenreader beschrifteten Farben. Der Farbkreis bleibt für freie Farben erhalten. Der Filament-Slot-Editor zeigt in Compose die Farbvorschau, Materialauswahl und optionale Löschaktion. Die Tests prüfen Common-Farben, CSS-Palette, Deckkraft und responsive Spaltenzahl.

Build 0.8.76 (`versionCode 111`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 146/146 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Kotlin-UI-/Profil-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/369 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Das Slice-/Rotation-Instrumentierungspaket wurde kompiliert. ADB listete kein Gerät; daher konnten Installation und visueller Gerätetest nicht ausgeführt werden. Der vollständige Android-/ARM64-Lauf dauerte 20:59 Minuten.

Die Debug-APK (`153.351.598` Bytes; SHA-256 `18CCFC94CE702913A4A2D69897ADC0353C317731C8EA20496DD7482D971F81A8`) und die mangels Release-Keystore unsignierte Release-APK (`146.116.843` Bytes; SHA-256 `323D275612AA869621B8A859E30448A69C1FC3BDBCFBA8B6B259AB83F5FF06F5`) liegen unter `outputs/OrkanSlicer_0.8.76_*.apk`. Bericht und vollständiges Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.76*`.
## Build 0.8.77 – Filament- und ACE-Zuordnung in Compose

Die Zuordnung vor dem Slicen und die ACE-Fachzuordnung verwenden jetzt Compose. Der Ein-Slot-Modus kann alle Objekte gemeinsam zuordnen; die Objekt-Einzelzuordnung bleibt erhalten. Drei neue Unit-Tests sichern T0/Slot 1, Slot-Grenzen und Ein-Slot-Zuweisung.

Build 0.8.77 (`versionCode 112`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 149/149 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Kotlin-UI-/Profil-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/373 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die Slice-/Rotation-Instrumentierung kompilierte. ADB listete kein Gerät, daher blieben Installation und Gerätetest offen. Der Android-/ARM64-Sammellauf dauerte 20:08 Minuten.

Die Debug-APK (`153.367.982` Bytes; SHA-256 `6FA97D66183685097906CDD1465023980626A91150FD1BAD40CD34430C6FC423`) und die mangels Release-Keystore unsignierte Release-APK (`146.133.227` Bytes; SHA-256 `80199398C2A8D49ACE4824DBF783F8B7A668C207907B4ABC6AA3DA94C13F92F9`) liegen unter `outputs/OrkanSlicer_0.8.77_*.apk`. Bericht und vollständiges Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.77*`.
## Build 0.8.78 – Druckvorbereitungsdialog in Compose

Der Druckvorbereitungsdialog ist auf Compose umgestellt. Drucker-/Druckplatteninfo, automatische Leveling-Erkennung, ACE-Zuordnung, Wechsel des Druckerprofils und erneutes Slicen vor dem Senden bleiben erhalten. Neue Regressionstests prüfen G29, unterstützte Upload-Ziele und den Profilwechsel.

Build 0.8.78 (`versionCode 113`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 152/152 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Kotlin-UI-/Profil-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/375 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die Slice-/Rotation-Instrumentierung kompilierte. ADB erkannte weiterhin kein Gerät; ein Xiaomi-Gerätetest ist offen. Der Android-/ARM64-Sammellauf dauerte 21:37 Minuten.

Die Debug-APK (`153.384.368` Bytes; SHA-256 `97B928562429D61A651EC45773C7EBDA99DB66B3EDF31962A613B597029F69D0`) und die mangels Release-Keystore unsignierte Release-APK (`146.149.611` Bytes; SHA-256 `1845F4D170EDC7EC18931C562A056092324E5907E8272B0D64A688A373666B03`) liegen unter `outputs/OrkanSlicer_0.8.78_*.apk`. Bericht und vollständiges Prüfprotokoll liegen unter `outputs/OrkanSlicer_0.8.78*`.
## Build 0.8.79 – Compose-Formulare für Quick Slice und Smart Expert

Build 0.8.79 (`versionCode 114`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 161/161 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Datei-/Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Kotlin-UI-/Profil-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/375 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die Slice-/Rotation-Instrumentierung kompilierte. ADB und die Windows-Geräteerkennung listeten kein angeschlossenes Xiaomi, daher waren Installation, Start und visueller Gerätetest nicht möglich.

Die neuen Compose-Formulare prüfen die Quick-Slice-Schichthöhe und Füllung vor dem Schreiben in das Orca-Druckprofil. Smart Expert behält seine Druckerauswahl, asynchrone Geometrieanalyse, Abbruch-/Modelländerungssicherung und Ergebnisaktionen bei. Beide Formulare nehmen Dezimalkommas an und zeigen ungültige Maße direkt am Eingabefeld.

Debug-APK: `outputs/OrkanSlicer_0.8.79_debug.apk` (155.457.072 Byte; SHA-256 `7A1AD7B180EE2B86579008CEEC256B0E6000BA6C6D31D9E04D6C52164DE07320`). Release-APK: `outputs/OrkanSlicer_0.8.79_release_unsigned.apk` (146.168.783 Byte; SHA-256 `E18E33FD7E6300F534110EDACA8CB3EB756AEA122FDB17A71018215737915833`); ohne Release-Keystore unsigniert. Vollständiger Bericht: `outputs/OrkanSlicer_0.8.79_Buildbericht.md`.

## Build 0.8.80 – Workspace-Profilauswahl in Compose

Die Auswahl von Drucker-, Filament- und Druckprofilen im Arbeitsbereich wurde als Compose-Bottom-Sheet umgesetzt. Auswahlzustand, Kompatibilitätsprüfung, Speichern und Aktualisierung des Profilbildschirms bleiben erhalten. Drei Unit-Tests prüfen Indexauflösung, Profiländerungen und nicht vorhandene Einträge.

Build 0.8.80 (`versionCode 115`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 164/164 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Datei-/Upload-/Filament-/G-Code/Anycubic-, Lithophan- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Kotlin-UI-/Profil-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/376 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die Slice-/Rotation-Instrumentierung kompilierte. ADB und Windows listeten kein Gerät; Installation und visuelle Prüfung waren deshalb offen. Der vollständige Sammellauf dauerte 21:41 Minuten.

Debug-APK: `outputs/OrkanSlicer_0.8.80_debug.apk` (153.436.315 Byte; SHA-256 `42BAB8095F2BDBAE6CC80BA1E2110DB8D5AB153E239E11BA3737F398F636AD2D`). Release-APK: `outputs/OrkanSlicer_0.8.80_release_unsigned.apk` (146.168.779 Byte; SHA-256 `23A3BFFC942F7B4B81EAFF5023828387D19D28B61EF987EE5D888B981044A565`); ohne Release-Keystore unsigniert. Vollständiger Bericht: `outputs/OrkanSlicer_0.8.80_Buildbericht.md`.

## Build 0.8.81 – Befestigergenerator in Compose

Schrauben-, Muttern- und Scheibenparameter verwenden nun ein scrollbares Compose-Formular mit metrischer Größenauswahl, Passungsvorgaben, Dezimalkomma-Eingabe und direkter Validierung. Die STL-Erzeugung, Passprobe, Fortschrittsanzeige und der vorhandene Speichern-/Laden-Ablauf bleiben erhalten.

Build 0.8.81 (`versionCode 116`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 169/169 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Datei-/Upload-/Filament-/G-Code/Anycubic-, Lithophan- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Kotlin-UI-/Profil-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/377 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die Slice-/Rotation-Instrumentierung kompilierte. ADB und Windows listeten kein Gerät; Installation und visuelle Prüfung waren deshalb offen. Der vollständige Sammellauf dauerte 24:27 Minuten.

Debug-APK: `outputs/OrkanSlicer_0.8.81_debug.apk` (153.437.180 Byte; SHA-256 `BF8773B8AF24DDE5B4C53F6FDF59FB3662306E51D7344DFA4F6742793CC0261A`). Release-APK: `outputs/OrkanSlicer_0.8.81_release_unsigned.apk` (146.186.035 Byte; SHA-256 `4A0833A441C12C03EB0BEB665004A5FAADA68820D39112AF3B1901C0482140F6`); ohne Release-Keystore unsigniert. Vollständiger Bericht: `outputs/OrkanSlicer_0.8.81_Buildbericht.md`.

## Build 0.8.82 – Profilwerkzeuge und Sonderdialoge in Compose

Die Druckprofil-Schnellaktionen sowie die Editoren für Spülmatrix und Reinigungsturmposition verwenden nun Compose. Die Matrix bleibt horizontal und vertikal scrollbar, validiert Dezimalkommas und zeigt Zellwerte verständlich an. Positionen können je Druckplatte bearbeitet werden. Filamentsynchronisierung zeigt ihren Ladezustand; Orca-Profil- und Speicherpfade bleiben erhalten. Fünf neue Unit-Tests prüfen Matrix- und Mehrplattenlogik.

Build 0.8.82 (`versionCode 117`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 174/174 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Kotlin-UI-/Profil-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/379 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die Slice-/Rotation-Instrumentierung kompilierte. ADB listete trotz angeschlossen gemeldetem Xiaomi kein Gerät; Installation und Gerätetest waren deshalb nicht möglich. Der Gradle-Sammellauf dauerte 22:37 Minuten.

Debug-APK: `outputs/OrkanSlicer_0.8.82_debug.apk` (153.474.579 Byte; SHA-256 `9112E6241AEC5816776040F34F36B109B24D48F845EBB47DA86125BBCA8BAF48`). Release-APK: `outputs/OrkanSlicer_0.8.82_release_unsigned.apk` (146.190.683 Byte; SHA-256 `0E2547B99470D5F80B6CA7E1CEF55B8ADD7854EDDAD0ABA3DBB00BD1760B01C0`); ohne Release-Keystore unsigniert. Vollständiger Prüfbericht: `outputs/OrkanSlicer_0.8.82_Buildbericht.md`.

## Build 0.8.83 – Profilkopf und Filamentpalette in Compose

Der Profilkopf verwendet ein barrierefreies Compose-Auswahlmenü mit aktueller Markierung sowie den bestehenden Aktionen zum Duplizieren und Löschen. Die Filamentpalette T0–T15 ist horizontal scrollbar und bietet weiterhin Farb-/Materialbearbeitung, Speichern und Mindest-/Maximalgrenzen. Größere Texte, lokalisierte Screenreader-Beschreibungen und Theme-Aktualisierung sind erhalten.

Build 0.8.83 (`versionCode 118`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 181/181 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Upload-/Filament-/G-Code-/Anycubic-, Lithophan- und Fastener-Regressionen, 449 Profilrouten/89 deutsche Gruppen, Kotlin-UI-/Profil-/Connector-Audits, 63/63 ARM64-Prebuilt-Prüfungen, Release-Lint (0 Fehler/379 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die Slice-/Rotation-Instrumentierung kompilierte. ADB und Windows listeten trotz angeschlossen gemeldetem Xiaomi kein Gerät; Installation und Starttest waren deshalb nicht möglich. Der Android-/ARM64-Gradle-Lauf dauerte 22:25 Minuten.

Debug-APK: `outputs/OrkanSlicer_0.8.83_debug.apk` (153.508.286 Byte; SHA-256 `110CD76B013C80C2483A293A864E8801B7319A3ABEB1F401A7426908F60517BE`). Release-APK: `outputs/OrkanSlicer_0.8.83_release_unsigned.apk` (146.208.023 Byte; SHA-256 `06CF24E8688F136AE3C79CA6F7BF05238DAF26A57EC72DF3C1305FDA25E6D4A2`); ohne Release-Keystore unsigniert. Vollständiger Prüfbericht: `outputs/OrkanSlicer_0.8.83_Buildbericht.md`.

## Build 0.8.84 – Direkte Compose-Zeilen für Orca-Profile

Boolesche Schalter, skalare Zahlen und einzelne Enum-Auswahlen sowie Gruppenüberschriften und Hinweise werden direkt in der gemeinsamen Compose-Liste gerendert. Damit entfällt für die häufigsten Profileinstellungen die zusätzliche Compose-in-Compose-Ansicht, ohne Orca-Wertquelle, Validierung, Profiländerung oder Hilfeaktionen zu ersetzen. Drei Tests sichern die Renderpolicy. Die vollständige Prüfung bestand mit 184 Unit- und 94 Headless-E2E-Tests, Kotlin-/Profilrouten-/Connector-Audits, Upload-, Druckdaten-, Lithophan-, Fastener- und ARM64-Prüfungen sowie Release-Lint (0 Fehler, 379 Warnungen). Debug- und unsignierte Release-APK wurden auf Metadaten und ZIP-CRC geprüft. Der 22:19-Minuten-Build kompilierte auch Slice-/Rotationsinstrumentierung; ADB listete kein Gerät, deshalb blieb der Xiaomi-Gerätetest offen.

Debug-APK: `outputs/OrkanSlicer_0.8.84_debug.apk` (153.508.288 Byte; SHA-256 `3DD421BD6C3A4417246B09B9E8B12D67590175ABA3B734A2D5E9E6B6695CEDB2`). Release-APK: `outputs/OrkanSlicer_0.8.84_release_unsigned.apk` (146.224.407 Byte; SHA-256 `3F5E525075B6FDEAE46A450BC6FF06DA186DF21C4B99DDFDBBEE29BEB279D964`); ohne Release-Keystore unsigniert. Vollständiger Prüfbericht: `outputs/OrkanSlicer_0.8.84_Buildbericht.md`.

## Build 0.8.85 – Vollständige direkte Compose-Darstellung der Profileinstellungen

Alle unterstützten Profiloptionen besitzen nun typspezifische Compose-Inhalte, darunter Enum-Menüs, Farben, skalare und zusammengesetzte Zahlen, Punkte, Texte und Textlisten. Unbekannte beziehungsweise externe Zeilen behalten einen sicheren Fallback. Orca-Profilquellen, Validierung und Speicheraktionen bleiben erhalten. Drei neue Policy-Tests prüfen die Typabdeckung.

Build 0.8.85 (`versionCode 120`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand mit 184/184 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Profilrouten-, Kotlin-UI-, Connector- und 63/63 ARM64-Prebuilt-Audits, Upload-/Filament-/G-Code/Anycubic-, Lithophan- und Fastener-Regressionen, Release-Lint (0 Fehler/379 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Der Slice-/Rotation-Instrumentierungstest kompilierte. ADB und Windows zeigten trotz angeschlossen gemeldetem Xiaomi kein Gerät, daher gab es keinen Installationstest. Der vollständige Sammellauf dauerte 19:42 Minuten.

Debug-APK: `outputs/OrkanSlicer_0.8.85_debug.apk` (153.524.683 Byte; SHA-256 `13138F566C1E3AAED40FDE205586EAAC72A3AFE81636C0CEEB4A3A2F1188CCFE`). Release-APK: `outputs/OrkanSlicer_0.8.85_release_unsigned.apk` (146.224.407 Byte; SHA-256 `1ABF70681F8AF6CBD91E9C369AD5E067815EB76CD9A7F2857DEE49C29B1014EB`); ohne Release-Keystore unsigniert. Vollständiger Bericht und Protokoll: `outputs/OrkanSlicer_0.8.85_Buildbericht.md` und `outputs/OrkanSlicer_0.8.85/verification-transcript.txt`.

## Build 0.8.88 – Cloud-Profilbild ohne eingebettete Android-View

Das Avatarbild im Cloud-Profilkopf wird jetzt als Compose-`Image` angezeigt. Glide lädt weiterhin das Bild; sein Request wird mit dem Composable-Lebenszyklus gestartet und beim Wechsel der URL oder Verlassen der Ansicht abgebrochen. Der getönte Platzhalter, Kreiszuschnitt und die zugängliche Beschriftung bleiben erhalten. Die UI- und Profilrouten-Audits prüfen, dass dieser Bereich kein `AndroidView`/`ImageView` mehr verwendet.

Build 0.8.88 (`versionCode 123`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 186/186 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Profilrouten-, Kotlin-UI-, Connector- und 63/63 ARM64-Prebuilt-Audits, Upload-/Filament-/G-Code/Anycubic-, Lithophan- und Fastener-Regressionen, Release-Lint (0 Fehler/379 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die Slice-/Rotation-Instrumentierungstests wurden kompiliert. Beim anschließenden Geräteschritt war das Xiaomi nicht mehr in ADB oder der Windows-Geräteliste sichtbar; der Installationsversuch endete vor der APK-Übertragung, daher gibt es für 0.8.88 keinen Starttest. Der Android-/ARM64-Gradle-Lauf dauerte 23:04 Minuten.

Debug-APK: `outputs/OrkanSlicer_0.8.88_debug.apk` (153.524.673 Byte; SHA-256 `21364B3B326A20F99389E37585B3D9074FB7B91C139290F93CCC9F28A62AAEEC`). Release-APK: `outputs/OrkanSlicer_0.8.88_release_unsigned.apk` (146.224.407 Byte; SHA-256 `EC8EE9E5B2A369678B7E9A8D9451B9D54AD8D50FC16A53006709D1C8DD9FDF52`); ohne Release-Keystore unsigniert. Bericht und Protokoll: `outputs/OrkanSlicer_0.8.88_Buildbericht.md` und `outputs/OrkanSlicer_0.8.88/verification-transcript.txt`.

## Build 0.8.87 – Profilaktionen und Abstände nativ in Compose

Die integrierten Druckeraktionen „Weboberfläche“ und „Filament synchronisieren“ werden jetzt als barrierefreie Compose-Zeilen dargestellt; Abstandszeilen laufen ebenfalls direkt über Compose. Für unbekannte externe Zeilentypen bleibt der bestehende Kompatibilitätsrenderer erhalten. Render-Policy-Unit-Tests und der Kotlin-UI-Audit decken die direkte Darstellung und den Fallback ab.

Build 0.8.87 (`versionCode 122`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand 186/186 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Profilrouten-, Kotlin-UI-, Connector- und 63/63 ARM64-Prebuilt-Audits, Upload-/Filament-/G-Code/Anycubic-, Lithophan- und Fastener-Regressionen, Release-Lint (0 Fehler/379 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Die Debug-App wurde anschließend auf dem Xiaomi 13 (Android 16) installiert und gestartet; Paketversion 0.8.87, aktiver Prozess und sauberes Crash-/ANR-/OOM-Protokoll wurden bestätigt. Slice-/Rotation-Instrumentierungstests wurden kompiliert, aber nicht auf dem Gerät ausgeführt. Der Android-/ARM64-Gradle-Lauf dauerte 23:17 Minuten.

Debug-APK: `outputs/OrkanSlicer_0.8.87_debug.apk` (153.524.684 Byte; SHA-256 `FDD21754A4489FA0D38DCFEAB2A21A6199240CD9A8A8D74526E1BEF0C8973FF6`). Release-APK: `outputs/OrkanSlicer_0.8.87_release_unsigned.apk` (146.224.407 Byte; SHA-256 `1EAB6A9142D3EEFE37D96C1E04653A28E8686945756D907B24B17D58FA4B1C62`); ohne Release-Keystore unsigniert. Bericht und Protokoll: `outputs/OrkanSlicer_0.8.87_Buildbericht.md` und `outputs/OrkanSlicer_0.8.87/verification-transcript.txt`.

## Build 0.8.86 – Crash-Sicherheit bei frühem Activity-Ende

`MainActivity` verwendete in Lifecycle- und Eventpfaden einen nicht-null Getter als Nullcheck. Wenn Android die Activity vor der Navigationseinrichtung beendete (unter anderem während der Crash-Wiederherstellung), warf der Getter `Navigation is not initialized`. Die betroffenen Aufrufe verwenden nun den nullable Delegate; der Kotlin-UI-Audit prüft diese Lifecycle-Verträge.

Build 0.8.86 (`versionCode 121`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`) bestand mit 184/184 Android-Unit-Tests, 94/94 Headless-E2E-Tests, Java/Kotlin-Interop, Profilrouten-, Kotlin-UI-, Connector- und 63/63 ARM64-Prebuilt-Audits, Upload-/Filament-/G-Code/Anycubic-, Lithophan- und Fastener-Regressionen, Release-Lint (0 Fehler/379 Warnungen), APK-Metadaten, ZIP-CRC und `git diff --check`. Auf dem Xiaomi 13 mit Android 16 wurde die Debug-Version installiert und bis zur Hauptansicht gestartet. Die bestehende Crash-Wiederherstellung wurde genutzt; anschließend blieb `MainActivity` aktiv und das Crash-/ANR-Protokoll leer. Modelle und Profile blieben erhalten. Die separate Installation des Slice-/Rotation-Testpakets wurde durch Android mit `INSTALL_FAILED_USER_RESTRICTED` abgelehnt; der Instrumentierungstest ist kompiliert, aber nicht auf dem Gerät ausgeführt. Der vollständige Sammellauf dauerte 22:30 Minuten.

Debug-APK: `outputs/OrkanSlicer_0.8.86_debug.apk` (153.524.689 Byte; SHA-256 `F4EDE461E6513FBEB1BA3078315AD8055C94FA3744FB1F754A2E93DFE758833F`). Release-APK: `outputs/OrkanSlicer_0.8.86_release_unsigned.apk` (146.224.407 Byte; SHA-256 `66A74795BE88E4B423B14F647914A9893270ED4C9C80E913869F4DBBFAFC0527`); ohne Release-Keystore unsigniert. Bericht und Verifikationsprotokoll: `outputs/OrkanSlicer_0.8.86_Buildbericht.md` und `outputs/OrkanSlicer_0.8.86/verification-transcript.txt`.


## Workspace-UI 2.0 – integrierter Debug-Build (2026-10-16)

Die Compose-basierte `WorkspaceShell` ersetzt den manuellen Aufbau der Haupt-Workspace-Menüs in
`BedFragment`. Der bestehende `GLView` wird über `RendererAdapter` innerhalb seines gemessenen
Viewport weiterverwendet. Das Printer-Web-Fehlerpanel ist Compose/Material 3; WebView und
Fortschrittsanzeige bleiben in ihrem Lifecycle-Host. ACE-Gerätetests wurden auf die fest
positionsgebundene Zuordnung (T0→Slot 1 bis T3→Slot 4, Lücken bleiben erhalten) korrigiert und
prüfen explizit, dass ein abweichendes Tool-Mapping nicht die Slotbefehle umschreibt.

Verifikation des Quellstands vor dem finalen Commit:

- `:app:testDebugUnitTest`: 526 Tests, 0 Fehler.
- `:app:testSliceTestUnitTest`: 526 Tests, 0 Fehler.
- `:app:connectedSliceTestAndroidTest` auf Honor PTP-N49/Android 16: 39 bestanden, 0 Fehler,
  3 übersprungen (private multicolor 3MF-Projekt-Fixture fehlt).
- `:app:assembleDebug`: erfolgreich, ARM64-Nativkern eingeschlossen.
- Debug-APK (`versionName 0.8.242`, `versionCode 277`) wurde auf dem Honor installiert und
  gestartet. Screenshot-Prüfung bestätigt sichtbare globale Navigation, Profil-/Filament-/
  Plattensteuerung, Druckplatte und Workspace-Navigation ohne Überdeckung durch die GL-Oberfläche.
  Logcat-Auszug nach Start enthielt keinen FATAL EXCEPTION-, ANR- oder OOM-Treffer.
- Emulator: Gradle konnte `emulator-5554` nicht abfragen (ddmlib property-fetch timeout); kein
  Emulator-Gerätetest wird als bestanden gewertet.
- Kein Druckstart wurde ausgelöst. Der UI-2.0-Migrationsstand ist noch nicht vollständig:
  Dialoge/Sonderwerkzeuge, zentrale Overlaysteuerung und weitere `BedFragment`-Zuständigkeiten
  verbleiben. Die Roadmap führt diese Punkte weiter.

Der abschließende APK-Pfad und SHA-256 werden nach dem Commit in PR #2 ergänzt. Der Build ist eine
lokale Debug-APK und kein öffentliches Release.

Finaler APK-Artefakt nach Commit `6b9b3c0423b9eb1d075521a2bd97eaeec330e10d`:

- `app/build/outputs/apk/debug/OrkanSlicer_6b9b3c0423.apk`
- 156.885.758 Byte; SHA-256 `E27CBF94377B3D694F4B4D9F8BC7EF8D6EB972DF15878BC9CDAA2F2C73AB6AD6`.
- Diese Debug-APK wurde nach dem Build erneut auf dem Honor installiert und gestartet. Das Paket
  blieb als `MainActivity` aktiv; der geprüfte Logcat-Auszug zeigte keinen FATAL-, ANR- oder
  OOM-Eintrag.

## Build-Verifikation: Workspace Panels und Profilfilter (2026-10-16)

- `:app:testDebugUnitTest`: 531/531 bestanden.
- `:app:testSliceTestUnitTest`: 531/531 bestanden.
- `:app:connectedSliceTestAndroidTest` auf Honor PTP-N49 / Android 16: 39 bestanden, 0 Fehler,
  3 übersprungen (private Mehrfarb-3MF-Fixtures nicht vorhanden).
- `:app:assembleDebug`: erfolgreich, ARM64-Kern eingeschlossen.
- Manuelle Honor-Prüfung: Profilblatt geöffnet, lange Druckprofilliste mit Suchfeld und Tastatur
  geprüft, Suchtext `0.16mm` filterte die Treffer, System-Back schloss die Ebenen nacheinander,
  danach öffnete die Plattenauswahl. Kein Druckstart.
- Beim ersten Öffnen der langen Auswahl trat ein Compose-Intrinsic-Measurement-Absturz auf; die
  Liste wurde aus dem Dropdown in einen begrenzten Dialog verschoben. Der reparierte Stand wurde
  erneut installiert und der lange Suchpfad lief ohne neuen Crash. Der alte Crash-Bericht blieb
  zunächst als SafeStart-Wiederherstellungsseite sichtbar und wurde mit „App neu starten“ beendet.
- Gradle meldete weiterhin einen Property-Fetch-Timeout für `emulator-5554`; der Emulator ist nicht
  als geprüft gewertet.
- Kein CPU-/RAM-Benchmark wurde durchgeführt. Der Lazy-Render-Pfad vermeidet das gleichzeitige
  Komponieren aller Optionen; quantitative Performance-Messungen bleiben offen.

Finaler UI-Panel-Build nach Code-Commit `49cb9fcb30cf5c76f0371ed34420c4708643cd1f`:

- APK: `app/build/outputs/apk/debug/OrkanSlicer_49cb9fcb30.apk`
- 156.903.080 Byte; SHA-256 `17FF2BCBE4EB5236A48CF2E6A37B487CB743750ACDE3B4D98AD819352ACC2ADB`.
- `:app:assembleDebug` nach dem Code-Commit erfolgreich. Die beiden Unit-Suiten mit je 531 Tests und die Honor-Instrumentierung (39 bestanden, 3 Fixture-bedingt übersprungen) waren mit demselben Quellstand bereits erfolgreich.
- Genau diese APK wurde auf dem Honor PTP-N49 installiert und gestartet; `MainActivity` blieb im Vordergrund. Ein frischer Logcat-Ausschnitt hatte keine FATAL EXCEPTION-, ANR-, OOM- oder Intrinsic-Measurement-Fehler. Ein Screenshot zeigt die große Druckplatte und Workspace-Navigation. Das Profilfilter-/Panelverhalten wurde im unmittelbar vorhergehenden Gerätedurchgang mit demselben Quellstand geprüft.
- Ein zusätzlicher Emulator-Installationsversuch hing im ADB-Transfer und wurde abgebrochen; Emulatorprüfung nicht bestanden/nicht bestätigt.
- Diese APK entspricht Code-Commit `49cb9fc`. Spätere reine Dokumentationsänderungen ändern den APK-Inhalt nicht. Debug-Build, kein Release.

## Workspace state and large-text navigation follow-up (2026-10-16)

- `WorkspaceController` now combines the active workspace destination, its expanded menu, selected plate index, active special tool, and the existing exclusive overlay coordinator. `BedFragment` delegates its menu and plate-picker selection state to it. The controller does not duplicate GL-owned model selection, domain/profile state, dialog state, or slicing state; those remain in their existing owners and are part of the remaining migration.
- Added controller tests for navigation/panel ownership, plate state, and special-tool exclusivity. Adaptive navigation tests cover the two-row policy and its height. At large font scales, the main navigation uses two rows (3+2) and workspace navigation uses 3+3, preserving full labels instead of compressing them into one row. Activity font-scale handling updates this UI without intentionally rebuilding the GL renderer.
- Honor PTP-N49 / Android 16 at 130% system text: opened and visually checked the workspace, Druck, Filament, Drucker, and App settings screens. Main navigation remained visible in two rows. The Druck and Filament areas use scrollable content for the larger settings/material lists; printer and app settings showed readable section controls. No print action was invoked. These screenshots do not establish coverage of landscape, the smallest display size, larger accessibility font settings, or every nested menu.
- Full verification on code commit `0e4e7f002cec8393cd5505a2000f0700e9c7853b`: `:app:testDebugUnitTest` 536 tests; `:app:testSliceTestUnitTest` 536 tests; `:app:connectedSliceTestAndroidTest` on Honor, 39 passed and 3 skipped because private multicolor fixtures are absent; `:app:assembleDebug` passed. The fresh Honor logcat scan had no `FATAL EXCEPTION`, `ANR in`, or `OutOfMemoryError` matches. The emulator is offline after its previously observed ADB property/transfer hang; no emulator test is claimed.
- Debug APK for that code commit: `app/build/outputs/apk/debug/OrkanSlicer_0e4e7f002c.apk` (156,904,735 bytes; SHA-256 `508D87138D3BE5B7F252F8E0410C2D8B534BEA9415BB972BAD68116BACF0C13C`). This is a local debug build, not a public release.
- UI 2.0 is not complete. `BedFragment` still owns renderer/lifecycle and domain integration plus some legacy presentation; workspace state does not yet centrally own all requested UI domains. Remaining work includes systematic nested-menu/dialog/special-tool migration and verification, small/landscape and accessibility-size checks, 1–20 plate stress coverage, long filament-list persistence cases, and broader functional coverage. Do not treat this milestone as full UI acceptance.
- The APK corresponds to code commit `0e4e7f0`; this verification entry is a later documentation-only commit.

## Workspace UI follow-up: model actions and adaptive navigation (2026-10-16)

- Migrated the selected-model context menu from the legacy Android item dialog to a scrollable Material 3 Compose dialog. All existing model actions and handlers remain connected; renderer-bound actions still use the existing GL-thread dispatch. The dialog is coordinated as an exclusive workspace overlay, and Back/dismiss clears it safely.
- On narrow phones the global navigation uses compact visible labels while retaining descriptive accessibility labels. At larger text scales the workspace navigation reserves height for both rows; the camera action shortens visually to “Iso” while preserving its semantic label.
- Honor PTP-N49 / Android 16 was checked at 150% font scale. Screenshots show the open camera panel, both workspace navigation rows, and readable “Iso” action; no print was started. App was installed/launched; the inspected logcat had no fatal, ANR, or OOM entries.
- Verification on code commit `5e91004574fe74be9e0c4c24fc4a12123f210bc9`: `:app:testDebugUnitTest` 538/538; `:app:testSliceTestUnitTest` 538/538; Honor `:app:connectedSliceTestAndroidTest` 39 passed, 0 failed, 3 skipped because private multicolor fixtures are unavailable; `:app:assembleDebug` passed. `git diff --check` passed.
- Debug APK: `app/build/outputs/apk/debug/OrkanSlicer_5e91004574.apk` (156,905,221 bytes; SHA-256 `D8264E19D85B88CB0D8FE1E5FEA96F17BA52B6029EC0EAF2B526FB919F6C95DC`). This is a local debug build, not a release.
- This does not complete UI 2.0. The wider migration remains open: legacy `BedFragment` presentation and technical/domain responsibilities, remaining dialogs and specialist routes, full menu-by-menu interaction audit, small/landscape/accessibility coverage, and 1–20 plate / long filament stress tests. Emulator checks remain unconfirmed after the prior ADB transfer hang. No print or release was initiated and PR #2 remains unmerged.

This verification entry is documentation for code commit `5e91004574fe74be9e0c4c24fc4a12123f210bc9`; later documentation changes do not alter the APK.
