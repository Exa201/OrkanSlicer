const i=`---\r
title: Prezentare generală\r
slug: overview\r
order: 10\r
description: Ce face Kromacut și cum se leagă fluxurile principale de lucru.\r
---\r
\r
# Prezentare generală\r
\r
Kromacut transformă o imagine plană într-un obiect imprimat 3D din straturi colorate suprapuse. Ideea centrală este simplă: culorile imaginii devin straturi fizice, iar ordinea și înălțimea lor devin planul de imprimare.\r
\r
Folosește Kromacut pentru o imprimare în stil HueForge, un relief colorat în stil litofanie sau o piesă decorativă stratificată în care schimbările de filament creează imaginea finală.\r
\r
## Fluxul principal\r
\r
Majoritatea proiectelor urmează aceiași pași:\r
\r
1. [Încarcă sau importă o imagine](loading-images).\r
2. [Redu culorile](reducing-colors) până când previzualizarea are o paletă imprimabilă.\r
3. [Elimină ditheringul sau curăță](dedithering-cleanup) pixelii izolați dacă imaginea pare zgomotoasă.\r
4. Treci în [modul 3D](3d-mode) și alege Manual sau Auto-paint.\r
5. [Generează și exportă](generating-exporting-output) un fișier STL sau 3MF și urmează instrucțiunile de imprimare.\r
\r
## Două moduri de colorare\r
\r
Kromacut oferă două fluxuri de imprimare în modul 3D.\r
\r
| Flux | Când îl folosești | Ce controlezi |\r
| ---------- | ------------------------------------------------------ | --------------------------------------------------------------------------- |\r
| Manual | Vrei control direct asupra fiecărei culori din imagine. | Ordinea culorilor, grosimea fiecărei secțiuni de culoare, setările de imprimare și schimbările. |\r
| Auto-paint | Vrei ca Kromacut să planifice stiva fizică de filamente. | Culorile filamentelor, distanțele de acoperire, înălțimea maximă și opțiunile optimizatorului. |\r
\r
Modul Manual pornește de la culorile imaginii afișate în panoul **Culorile imaginii**. Auto-paint pornește de la filamentele reale și valorile lor de **distanță de acoperire (HD)**, apoi generează straturi imprimabile pentru imagine.\r
\r
## Ce vezi în aplicație\r
\r
Folosește 2D pentru a pregăti imaginea și 3D pentru a planifica straturile fizice. Ghidurile de mai jos urmează această distincție.\r
\r
## Ghiduri ilustrate\r
\r
Începe cu sarcina pe care vrei să o realizezi. Fiecare ghid explică comenzile, interacțiunile lor și consecințele asupra imprimării fizice. Diagramele sunt exemple schematice, nu predicții calibrate de culoare. Dă clic pe o ilustrație sau activeaz-o de la tastatură pentru a o deschide la dimensiune completă.\r
\r
| Sarcină | Ghid |\r
| ------------------------------------------------------------------ | -------------------------------------------------------------- |\r
| Stabilește dimensiunile, înălțimile straturilor și ordinea manuală a culorilor | [Modul 3D](3d-mode) |\r
| Alege filamentele, optimizează amestecurile și inspectează detaliile imprimabile | [Auto-paint](auto-paint) |\r
| Creează o placă multimaterial plană, cu fața în sus sau în jos | [Flat Paint](flat-paint) |\r
| Măsoară HD, compară probe Palette Proof sau fotografiază o Stack Matrix | [Fluxuri de calibrare](calibration-workflows) |\r
| Pregătește silueta și retușează pixelii | [Încărcarea imaginilor](loading-images) |\r
| Ajustează tonurile și culorile, apoi aplică definitiv rezultatul | [Ajustările imaginii](image-adjustments) |\r
| Redu culorile și gestionează paletele | [Reducerea culorilor](reducing-colors) |\r
| Elimină punctele fără a confunda curățarea 2D cu ditheringul de înălțime | [Eliminarea ditheringului și curățarea](dedithering-cleanup) |\r
| Verifică stiva finală și transfer-o într-un slicer | [Generarea și exportul rezultatului](generating-exporting-output) |\r
\r
## Organizarea spațiului de lucru\r
\r
Spațiul de lucru are trei zone principale:\r
\r
- Antetul conține documentația, comenzile pentru temă și legăturile comunității.\r
- Panoul din stânga conține comenzile modului curent.\r
    - În **2D**, afișează ajustările, eliminarea ditheringului, cuantizarea, paletele personalizate și culorile detectate în imagine.\r
    - În **3D**, afișează setările de imprimare, comenzile Manual și Auto-paint și instrucțiunile de imprimare.\r
- Previzualizarea principală afișează pânza imaginii 2D sau modelul 3D.\r
\r
> Sfat: setările 3D nu regenerează automat modelul. După schimbarea setărilor de imprimare, a grosimilor manuale sau a opțiunilor Auto-paint, apasă **Generează modelul 3D**.\r
\r
## Un prim proiect potrivit\r
\r
Începe cu o imagine cu contrast ridicat, un subiect clar și puține detalii de fundal. Redu-o la 4–16 culori, apoi folosește modul Manual dacă știi deja ordinea straturilor sau Auto-paint dacă ai distanțe de acoperire calibrate pentru filamente.\r
\r
---\r
\r
În continuare: [Ghid rapid](quick-start).\r
`;export{i as default};
