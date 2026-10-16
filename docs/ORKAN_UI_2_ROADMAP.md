# OrkanSlicer UI 2.0

Stand: 16.10.2026. Die UI wird in überprüfbaren Etappen erneuert. Der native Slicer, der OpenGL-Plattenrenderer, Modellzustände und bewährte Druckfunktionen bleiben erhalten. Die Umstellung geschieht pro Bereich, nachdem dessen Funktionen in der neuen Oberfläche verfügbar und geprüft sind.

## Leitlinien

- Die Druckplatte bekommt auf dem Telefon möglichst viel zusammenhängende Fläche.
- Werkzeugkategorien bleiben an festen Positionen. Im Hochformat klappt ein zweiter Druck auf die aktive Kategorie das Aktionspanel ein oder aus.
- Zweireihige Modell- und Bearbeitungsaktionen bleiben zweireihig, wenn das Panel geöffnet ist.
- Auf schmalen Telefonen und bei großer Systemschrift werden weniger, breitere Aktionskacheln angezeigt. Beschreibungen und TalkBack-Zustände bleiben verfügbar.
- Filamentfarben stehen als kompakter Knopf mit Farbindikatoren bereit. Er öffnet die bereits verwendete Filamentzuordnung für das Modell.
- Der Knopf zeigt zusätzlich die Gesamtzahl der Filamente; bei großen Paletten bleiben vier Farbindikatoren sichtbar und TalkBack nennt die Gesamtzahl.
- Gemeinsame Compose-/Material-3-Komponenten und `OrkanUiTokens` bilden die Grundlage für neue Oberflächen. Neue Funktionen erhalten keine parallelen View- und Compose-Implementierungen.
- Vor dem Entfernen einer alten Ansicht müssen ihre Aktionen und Zustände migriert und getestet sein.

Der Master-Prompt setzt als Ziel einen vollständigen Austausch der App-Oberfläche und des UI-Frameworks. Die Umsetzung erfolgt deshalb als geplanter Ersatz der alten Routen, nicht als zweites dauerhaftes UI-System. Der bestehende OpenGL-Renderer und die Slicer-/Drucklogik werden über klare Zustands- und Aktionsschnittstellen weiterverwendet. Die globalen Bereiche und noch nicht migrierten Dialoge sind weiterhin offene Arbeit.

## Etappe 1: Arbeitsbereich

Umgesetzt:

- Werkzeugpanel im Hochformat standardmäßig eingeklappt; die Druckplatte nutzt den frei werdenden Platz.
- Tap auf eine andere Kategorie wechselt die Werkzeuge und öffnet das Panel. Tap auf die ausgewählte Kategorie klappt es ein oder aus.
- Ein kleiner Pfeil am aktiven Navigationssymbol zeigt, ob das Werkzeugpanel offen ist und erneut angetippt werden kann.
- Snackbars reservieren die Höhe der sichtbaren Bedienelemente.
- Zweireihige Menüs wählen auf schmalen Telefonen oder bei großer Systemschrift drei statt vier sichtbarer Spalten und bekommen bei großer Schrift zusätzliche Höhe.
- Filamentfarbknopf mit bis zu vier Farbindikatoren nutzt `showModelColorSlots()` und damit denselben vorhandenen Zuweisungsdialog wie bisher.
- Der separate Eintrag „Farbslots“ im Modellmenü entfällt, damit die Zuweisung an einer Stelle bleibt.
- Menüstatus wird für TalkBack angesagt.

Prüfung dieser Etappe am 16.10.2026: `:app:testDebugUnitTest` bestand mit 515 Tests und ohne Fehler; `:app:assembleDebug` baute einschließlich `buildCMakeRelease[arm64-v8a]`. Die APK wurde auf `emulator-5554` und HONOR PTP-N49 installiert. Die eingeklappte Platte, der Wechsel zwischen Modell und Bearbeiten, die zweireihigen Aktionen sowie die schmalere Drei-Spalten-Anordnung wurden visuell geprüft. Auf dem Emulator wurde zusätzlich Systemschrift 130 % geprüft und anschließend auf 100 % zurückgesetzt. In den erfassten Gerätelogs gab es keinen App-Crash und keinen ANR. Das Modell war leer; echtes Modell- und Mehrfarbverhalten wird daher weiterhin mit einem Projekt geprüft.

## Etappe 2: Gemeinsame UI-Bausteine

Begonnen am 16.10.2026:

- `OrkanUiTokens` ist die zentrale Quelle für Touchflächen, Ecken, Abstände und Sheet-Maße.
- Profilzugriff und Filamentknopf verwenden dieselbe 48-dp-Touchfläche, gemeinsame Ecken und Abstände.
- Das Profil-Sheet nutzt zentrale Höhen- und Formwerte; seine bestehende Orca-Profil-Logik bleibt unverändert.
- Die adaptive Navigation und Material-3-Farbpalette bleiben die gemeinsame Navigation und Theme-Grundlage.

Diese Etappe migriert gemeinsame Gestaltungswerte, ersetzt noch nicht sämtliche alten Ansichten oder deren Navigation.

## Etappe 3: Gemeinsame Komponenten im Arbeitsbereich

In Arbeit:

- Der kompakte Filamentzugang zeigt Farbindikatoren und die Anzahl der verfügbaren Filamente, ohne dauerhaft eine Filamentliste einzublenden.
- Werkzeugfelder bleiben an ihrem festen unteren beziehungsweise seitlichen Bereich verankert. Die Druckplatte bleibt im normalen Arbeitsmodus sichtbar und wird nur bei geöffneter Werkzeuggruppe verkleinert.
- Die Modellaktionen bleiben in zwei Reihen; ähnliche Aktionen behalten eindeutige Bezeichnungen wie **Kopieren**, **Platte füllen**, **Modelle anordnen** und **Auf Platte legen**.

Die nächste Migration betrifft die vier globalen Bereiche **Druck**, **Filament**, **Drucker** und **App**. Dabei werden Funktionszustände zuerst an zuständige Controller/Modelle gebunden und danach die jeweiligen Ansichten ausgetauscht. Eine Funktion wird erst aus der alten Route entfernt, wenn ihre neue Route erreichbar ist und denselben Zustand bearbeitet.

Prüfung am 16.10.2026: Debug- und Slice-Unit-Suites bestanden mit jeweils 515 Tests; `assembleDebug` einschließlich des nativen ARM64-Kerns war erfolgreich. Pinselregler-, temporäre Projektprofil- und Touch-/Raycast-Gerätetests bestanden auf dem Honor; Pinselregler und temporäre Projektprofile bestanden zusätzlich auf dem Emulator. Die vollständige Android-Instrumentierung auf dem Honor meldete 35 bestandene Tests; die drei privaten 3MF-Fixture-Tests wurden mangels bereitgestellter Datei übersprungen. Der vollständige Emulator-Lauf blieb bei 2 von 35 Tests hängen und ADB-Shell-Befehle antworteten nicht mehr. APK 0.8.237 / Code 272 wurde auf Honor und Emulator installiert; die abschließende Installation nach der deutschen Beschriftungskorrektur und der vollständige Emulator-Lauf bleiben wegen des nicht antwortenden Emulators offen. Auf dem Honor zeigt die Oberflächenprüfung die große freie Platte, den Farbknopf samt Anzahl und das zweireihige Modell-Menü. Die Modellaktionen sind als **Duplizieren**, **Platte füllen** und **Anordnen** klar unterscheidbar. Weitere globale Routen bleiben Teil der nächsten Etappen.

## Nächste Etappen

1. Die globalen Bereiche Druck, Filament, Drucker und App entlang derselben Komponenten, Abstände und Zustandsmuster vereinheitlichen.
2. Dialoge und Sonderwerkzeuge aus dem Arbeitsbereich einzeln inventarisieren und in die gemeinsame UI-Struktur migrieren.
3. Platten-, Filament- und Profilzustände bei Rotation, Tastatur, großen Schriftstufen und großen Listen prüfen.
4. Layout-Tests mit messbaren Sichtbarkeits- und Überlappungsbedingungen ergänzen, wo die vorhandene UI-Testinfrastruktur das erlaubt.
5. Alte Oberflächen und ungenutzte Abhängigkeiten erst entfernen, wenn die jeweilige Funktion in der neuen Route nachweislich erhalten ist.

## Grenzen

Etappe 1 ist noch keine vollständige UI-2.0-Umstellung. Insbesondere die globalen Bereiche und viele vorhandene Dialoge behalten ihre bisherigen Routen, bis ihre Funktionen schrittweise migriert und geprüft wurden. Es wurden keine Änderungen an der Slicer-Engine oder Druckerfirmware vorgenommen.
