const e=`---\r
title: Întrebări frecvente\r
slug: faq\r
order: 100\r
description: Răspunsuri scurte la întrebările frecvente despre Kromacut.\r
---\r
\r
# Întrebări frecvente\r
\r
## Ce este distanța de acoperire?\r
\r
Distanța de acoperire, sau **HD**, este parametrul de opacitate la iluminare frontală, în mm, pe care Auto-paint îl folosește pentru a estima aspectul straturilor suprapuse de filament. La o grosime egală cu HD, modelul de bază lasă 10% din influența culorii de dedesubt. Punctul în care nu mai vezi diferența depinde și de filament, culoarea bazei și condițiile de vizualizare.\r
\r
Un HD mai mic înseamnă un filament mai opac, care acoperă în mai puține straturi. Un HD mai mare înseamnă un filament mai translucid, care are nevoie de mai multă grosime.\r
\r
HD înlocuiește Distanța de transmisie (TD) afișată în versiunile anterioare. Poți introduce un TD convențional pentru retroiluminare/litofanie prin butonul de conversie de pe rândul filamentului. Kromacut îl înmulțește cu 0,1 pentru o estimare inițială a HD; conversia nu reprezintă o nouă măsurătoare fizică. Consultă [Fluxuri de calibrare](calibration-workflows).\r
\r
## Să folosesc Manual sau Auto-paint?\r
\r
Folosește **Manual** când vrei control artistic direct asupra ordinii culorilor și înălțimilor straturilor.\r
\r
Folosește **Auto-paint** când ai culorile filamentelor reale și valorile distanței de acoperire și vrei ca Kromacut să planifice automat stiva.\r
\r
## Trebuie să calibrez filamentele?\r
\r
Poți începe cu distanțe de acoperire estimate. Valorile publicate ale Distanței de transmisie pentru exact filamentul pe care îl ai pot oferi și ele un punct de pornire: introdu-le prin butonul de conversie, nu direct în câmpul HD.\r
\r
Calibrarea îmbunătățește de obicei rezultatele Auto-paint, mai ales când nu există valori publicate sau rezultatul arată încă greșit. Calibrarea este deosebit de utilă când:\r
\r
- Un filament este translucid.\r
- Două filamente sunt similare vizual.\r
- Vrei rezultate repetabile între proiecte.\r
\r
## Care este diferența dintre culorile paletei și culorile filamentelor?\r
\r
Culorile paletei sunt culorile imaginii folosite în modul 2D și în modul Manual.\r
\r
Culorile filamentelor sunt materialele fizice folosite de Auto-paint. Auto-paint poate genera culori virtuale de strat din stiva fizică de filamente, dar planul de imprimare exportat se bazează în continuare pe filamente reale.\r
\r
## De ce are previzualizarea 3D nevoie de un buton de generare?\r
\r
Generarea 3D poate consuma multe resurse. Kromacut așteaptă comanda **Generează modelul 3D**, astfel încât schimbarea unei setări să nu pornească și să anuleze repetat o operație costisitoare.\r
\r
## Pot exporta fără modul 3D?\r
\r
Folosește modul 2D pentru a descărca imaginea sursă curentă, inclusiv editările aplicate. Integrează ajustările live cu **Aplică** înainte de descărcare. Folosește modul 3D pentru a genera și exporta modele STL sau 3MF.\r
\r
## Previzualizarea straturilor schimbă exportul?\r
\r
Nu. Intervalul **Previzualizarea straturilor** schimbă doar ce este vizibil în previzualizare. Exporturile STL și 3MF includ întregul model generat.\r
\r
## Ce fișier să imprim?\r
\r
Alege **Descarcă STL** pentru compatibilitate largă cu slicerele și schimbări manuale de filament.\r
\r
Alege **Descarcă 3MF** când slicerul acceptă fișiere 3MF cu informații de culoare și vrei să păstrezi obiectele colorate ale straturilor.\r
\r
## De ce sunt înălțimile aproximative?\r
\r
Numerele straturilor depind de comportamentul slicerului, în special de înălțimea primului strat. Folosește valorile din **Instrucțiunile de imprimare**, apoi confirmă straturile finale de schimbare în previzualizarea slicerului.\r
\r
## Pot partaja setările?\r
\r
Da. Exportă paletele 2D personalizate ca fișiere \`.kpal\` și profilurile de filamente Auto-paint ca fișiere \`.kfil\`. Fișierele mai vechi de profil \`.kapp\` pot fi importate în continuare.\r
\r
## O dimensiune mai mică a pixelului înlocuiește o duză mai mică?\r
\r
Nu. Dimensiunea pixelului stabilește dimensiunea fizică a pixelilor imaginii. Poate face o linie mai îngustă decât traseul de extrudare pe care îl poate produce duza. Setează **Lățimea efectivă a liniei** în **Setări de imprimare 3D**, folosește previzualizarea detaliilor imprimabile din Auto-paint și inspectează rezultatul feliat. Consultă [Modul 3D](3d-mode).\r
\r
## Calibrarea mea se aplică și la altă înălțime de strat?\r
\r
Nu presupune acest lucru. Măsurătorile HD și datele de aspect au roluri diferite. Datele probelor și matricelor sunt verificate față de setările și filamentele cu care au fost înregistrate. După schimbarea înălțimii stratului, inspectează datele active și imprimă o mostră mică de validare. Consultă [Fluxuri de calibrare](calibration-workflows).\r
\r
## De ce sunt mai multe culori de previzualizare decât role?\r
\r
Straturile subțiri permit filamentului de dedesubt să influențeze culoarea vizibilă. Grosimi diferite ale acelorași role în aceeași ordine creează amestecuri prezise diferite. Auto-paint alege dintre aceste prefixe de stivă realizabile, în timp ce piesele exportate folosesc în continuare filamentele reale. Consultă [Auto-paint](auto-paint).\r
`;export{e as default};
