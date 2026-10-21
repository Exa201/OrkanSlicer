const e=`---\r
title: Eliminarea ditheringului și curățarea\r
slug: dedithering-cleanup\r
order: 50\r
description: Curățare în vecinătate după culoarea exactă, cu efecte asupra punctelor, marginilor, transparenței și insulelor imprimabile.\r
---\r
\r
# Eliminarea ditheringului și curățarea\r
\r
**Eliminare dithering** înlocuiește pixelii nesusținuți local cu culori vecine. Este o trecere separată de curățare a imaginii, nu un algoritm de cuantizare sau o simulare a ceea ce poate imprima duza.\r
\r
Folosește-o pe modele de dithering cu culori repetate sau pe o imagine redusă cu puncte izolate. Poate funcționa înaintea cuantizării dacă sursa conține deja asemenea modele sau după cuantizare, pe regiunile reduse. Nu este un filtru de reducere a zgomotului fotografic: pixelii vecini din fotografii au adesea culori exacte ușor diferite.\r
\r
## Cum este ales un pixel\r
\r
![Cei opt pixeli din jur votează după culoarea exactă. Intensitatea stabilește câți vecini identici sunt necesari pentru păstrarea centrului; trecerile repetate folosesc fiecare rezultatul anterior.](36_dedither_neighbors.svg)\r
\r
Fiecare pixel verifică cei opt vecini imediați, inclusiv diagonalele. Dacă suficienți au **exact același RGB și alfa**, rămâne neschimbat. Altfel, preia cea mai frecventă culoare diferită din vecinătate. Egalitățile sunt rezolvate aleatoriu, deci aceleași setări nu produc neapărat rezultate identice la rulări diferite.\r
\r
Se folosesc numai culori vecine existente, inclusiv pixeli transparenți. Acestea nu sunt mediate într-o nuanță nouă.\r
\r
## Intensitate\r
\r
**Intensitate** reprezintă numărul de vecini identici necesar pentru a păstra pixelul original. Interval **1–9**; valoare implicită **4**.\r
\r
| Exemplu | Rezultat |\r
| ------------------------------------ | ------------------------------------------------------------ |\r
| Niciun vecin identic | Se schimbă dacă există un vecin de altă culoare, chiar și la intensitatea 1. |\r
| Trei vecini identici | Rămâne la intensitatea 3; poate fi înlocuit la intensitatea 4. |\r
| Centrul și toți cei opt vecini sunt identici | Rămâne chiar și la intensitatea 9, deoarece nu există un vecin diferit. |\r
\r
Valorile mici tind să păstreze detaliile; cele mari fac mai mulți pixeli eligibili pentru înlocuire. Cu doar opt vecini, intensitatea 9 nu poate atinge pragul de păstrare. Este o setare agresivă pentru margini, nu o rază mai mare. Pixelii de la marginea imaginii au și mai puțini vecini disponibili.\r
\r
## Treceri\r
\r
**Treceri** repetă curățarea de **1–10** ori, implicit **1**. Fiecare trecere citește întregul rezultat anterior. Trecerile suplimentare pot elimina puncte persistente, dar pot și deplasa marginile, rupe conexiuni înguste sau șterge text mic.\r
\r
Săgețile de resetare ale câmpurilor readuc intensitatea la 4 sau trecerile la 1. Resetarea panoului le restabilește pe ambele. Niciuna nu restaurează imaginea anterioară; pentru aceasta folosește Anulează.\r
\r
## Aplică și inspectează\r
\r
1. Folosește întâi **Aplică** pentru toate ajustările active. Eliminarea ditheringului citește vederea ajustată, iar integrarea lor prealabilă evită să lași ajustări live active peste rezultatul curățat.\r
2. Începe cu o trecere. Intensitatea implicită este 4; încearcă valori mai mici dacă detaliile fine sunt importante.\r
3. Apasă **Aplică**, apoi inspectează contururile, literele și marginile transparente la zoom mare.\r
4. Anulează înainte de a compara altă setare pe aceeași imagine inițială.\r
\r
Apăsările repetate pe Aplică continuă curățarea imaginii deja curățate. Nu sunt comparații independente cu originalul.\r
\r
## Efectul asupra imprimării\r
\r
Eliminarea pixelilor izolați de altă culoare poate înlătura insule minuscule de culoare, dar și detalii dorite. Alfa participă la vot, așa că eliminarea ditheringului poate extinde sau micșora silueta și deschide sau închide găuri.\r
\r
Operația lucrează în **pixelii imaginii**, nu în milimetri. Un detaliu de trei pixeli la 0,1 mm/pixel ocupă 0,3 mm înainte de deciziile ulterioare ale geometriei sau slicerului. Eliminarea ditheringului nu primește diametrul duzei. Folosește [comenzile pentru detalii imprimabile 3D](3d-mode) și previzualizarea slicerului pentru a verifica elementele fizice.\r
\r
## Eliminarea ditheringului față de ditheringul de înălțime\r
\r
| Instrument | Unde | Ce se schimbă |\r
| ---------------- | ---------------- | -------------------------------------------------------------------------------- |\r
| Eliminare dithering | 2D | Regiunile cu culoare exactă și, posibil, conturul transparent al imaginii sursă. |\r
| Dithering de înălțime | Auto-paint în 3D | Modelul generat al înălțimii suprafeței, folosit pentru a aproxima culorile țintă. |\r
\r
Folosirea unuia nu îl activează pe celălalt. Nu aplica eliminarea ditheringului dacă vrei să păstrezi pixel art-ul intenționat sau punctarea artistică.\r
\r
În continuare: [Modul 3D](3d-mode).\r
`;export{e as default};
