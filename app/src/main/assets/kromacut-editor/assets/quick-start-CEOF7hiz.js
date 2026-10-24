const e=`---\r
title: Ghid rapid\r
slug: quick-start\r
order: 20\r
description: O primă utilizare practică, de la imagine la export.\r
---\r
\r
# Ghid rapid\r
\r
Acest ghid parcurge un proiect obișnuit, de la încărcarea imaginii până la export.\r
\r
## Încarcă sau importă o imagine\r
\r
Folosește butonul de încărcare din bara de instrumente a previzualizării sau trage o imagine în previzualizarea 2D.\r
\r
După încărcare, folosește rotița mouse-ului pentru zoom și trage previzualizarea pentru deplasare. Dacă imaginea are transparență, butonul cu tablă de șah poate face zonele transparente mai ușor de văzut.\r
\r
## Ajustează imaginea\r
\r
În **2D**, folosește **Ajustări** înainte de reducerea culorilor. Expunerea, contrastul, zonele luminoase, umbrele, alburile, negrurile, saturația, vibranța, nuanța, temperatura, tenta și claritatea pot schimba culorile identificate de instrumentele pentru paletă.\r
\r
Apasă **Aplică** în panoul Ajustări pentru a integra ajustările curente în imagine înainte de reducerea culorilor, generarea geometriei 3D sau descărcare. Ajustările live sunt doar o previzualizare, nu o imagine sursă actualizată. Consultă [Ajustările imaginii](image-adjustments) pentru exemple înainte/după și comportamentul resetării.\r
\r
## Redimensionează dacă este necesar\r
\r
Dacă imaginea este mult mai mare decât detaliile pe care vrei să le imprimi, folosește **Redimensionează imaginea** pentru a o micșora procentual înainte de reducerea culorilor. Astfel scad dimensiunile reale în pixeli, ceea ce poate accelera generarea ulterioară a modelului 3D și simplifica stabilirea dimensiunilor fizice.\r
\r
## Redu culorile\r
\r
În **Setări de cuantizare**:\r
\r
1. Lasă **Paletă** pe **Auto**, dacă nu ai deja în vedere o anumită paletă.\r
2. Începe cu **Număr de culori** la **16**. Micșorează-l pentru mai puține regiuni de culoare sursă sau mărește-l dacă previzualizarea are nevoie de mai multe detalii. În Auto-paint, acest număr nu reprezintă numărul de role sau de schimbări.\r
3. Lasă **Algoritm** pe opțiunea implicită **K-means**. Este algoritmul recomandat pentru început la majoritatea imaginilor.\r
4. Apasă **Aplică**.\r
\r
Folosește panoul **Culorile imaginii** pentru a inspecta rezultatul. Apasă o mostră pentru a o edita sau a o șterge din paletă. Ștergerea remapează pixelii la culorile rămase; folosește Radiera sau setează alfa la zero (complet transparent) pentru a elimina pixeli din siluetă.\r
\r
## Elimină ditheringul sau curăță\r
\r
Dacă reducerea culorilor lasă puncte izolate, folosește **Eliminare dithering** ca trecere de curățare a zgomotului. Este deosebit de utilă deoarece pixelii rătăciți individuali din imaginea 2D pot deveni bucăți individuale de geometrie în modelul 3D.\r
\r
Începe cu valorile implicite pentru **Intensitate** și **Treceri**, apoi apasă **Aplică**. Mărește numărul de treceri doar dacă imaginea are încă prea mulți pixeli izolați după prima trecere.\r
\r
## Activează modul 3D\r
\r
Apasă **3D**. Stabilește întâi parametrii de bază ai imprimării:\r
\r
- **Dimensiunea pixelului (XY)** controlează lățimea și adâncimea fizică a fiecărui pixel al imaginii.\r
- **Înălțimea stratului** trebuie să coincidă cu înălțimea pe care intenționezi să o folosești în slicer.\r
- **Înălțimea primului strat** trebuie să coincidă cu setarea primului strat din slicer.\r
- **Geometrie netezită** poate netezi limitele de culoare conectate pentru o geometrie mai lină.\r
\r
## Alege Manual sau Auto-paint\r
\r
Folosește **Manual** când vrei control direct asupra culorilor imaginii reduse. Modul Manual folosește mostrele din **Culorile imaginii**: trage culorile în ordinea de imprimare dorită, apoi folosește glisorul fiecărui rând pentru a stabili contribuția acelei culori la înălțime. Este o primă alegere bună dacă știi deja ordinea straturilor sau lucrezi cu o paletă mică și simplă.\r
\r
Folosește **Auto-paint** când vrei ca Kromacut să planifice stiva fizică de filamente. Auto-paint pornește de la filamentele tale reale, nu de la mostrele imaginii reduse, apoi folosește culoarea și **Distanța de acoperire (HD)** a fiecărui filament — adâncimea la care acoperă ce se află dedesubt — pentru a estima aspectul straturilor suprapuse.\r
\r
Pentru prima utilizare Auto-paint:\r
\r
1. Adaugă filamentele cu care intenționezi efectiv să imprimi.\r
2. Setează cât mai precis culoarea fiecărui filament.\r
3. Setează **HD** pentru fiecare filament. Estimarea cu bagheta este potrivită pentru experimente și poți converti o valoare TD convențională (≈10× HD); distanțele de acoperire calibrate dau de obicei cele mai bune rezultate.\r
4. Lasă inițial **Înălțime maximă** pe **Auto**.\r
5. Activează **Potrivire îmbunătățită a culorilor** dacă primul rezultat ratează culori importante sau dacă vrei ca optimizatorul să caute o ordine mai bună a filamentelor.\r
\r
După ce Auto-paint calculează o stivă, verifică zonele de tranziție și detaliile de încredere înainte de export. Încrederea redusă înseamnă de obicei că setului de filamente îi lipsește o culoare utilă, distanțele de acoperire necesită calibrare sau înălțimea maximă este prea restrictivă.\r
\r
## Generează și exportă\r
\r
Apasă **Generează modelul 3D**. Când apare modelul, folosește glisorul **Previzualizarea straturilor** pentru a inspecta construirea imprimării de jos în sus.\r
\r
Deschide meniul de descărcare și alege **Descarcă STL** sau **Descarcă 3MF**. Apoi copiază **Instrucțiunile de imprimare**, ca să ai culoarea de început, straturile de schimbare și setările recomandate pentru slicer.\r
\r
În continuare: [Modul 3D](3d-mode), [Auto-paint](auto-paint) sau [Generarea și exportul rezultatului](generating-exporting-output#before-you-export).\r
`;export{e as default};
