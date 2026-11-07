const e=`---\r
title: Depanare\r
slug: troubleshooting\r
order: 90\r
description: Probleme frecvente și ce să încerci mai întâi.\r
---\r
\r
# Depanare\r
\r
Începe aici când un rezultat arată greșit sau o comandă este dezactivată.\r
\r
## Un link afișează „Pagina nu a fost găsită”\r
\r
Adresa poate conține o greșeală sau poate indica o pagină care nu mai există. Folosește **Deschide Kromacut** pentru a ajunge la instrument, **Mergi la pagina principală** pentru pagina de prezentare sau **Răsfoiește documentația** pentru a găsi ghidul actual. O adresă necunoscută de documentație nu este înlocuită automat cu alt ghid.\r
\r
## Generarea modelului 3D nu actualizează previzualizarea\r
\r
Setările 3D nu se aplică până când nu apeși **Generează modelul 3D**. Modifică setările dorite, apoi generează din nou.\r
\r
## Instrucțiunile de schimbare sunt dezactivate\r
\r
În modul Manual, imaginile cu peste 64 de culori dezactivează instrucțiunile de schimbare. Revino la **2D**, folosește **Setări de cuantizare** și redu imaginea la cel mult 64 de culori. Flat Paint nu are intenționat o secvență de schimbări manuale, deoarece fiecare strat imprimat poate conține mai multe filamente.\r
\r
## Modelul este prea înalt\r
\r
Încearcă în această ordine:\r
\r
1. În Auto-paint, micșorează **Înălțimea maximă** și urmărește zonele de tranziție comprimate.\r
2. În modul Manual, verifică numărul culorilor imaginii. Multe culori creează multe secțiuni suprapuse, astfel că modelul devine în mod natural mai înalt.\r
3. Redu culorile în **2D** dacă nu ai nevoie de fiecare culoare ca strat imprimat separat.\r
4. În modul Manual, micșorează grosimea uneia sau mai multor secțiuni de culoare.\r
5. Confirmă că **Înălțimea stratului** și **Înălțimea primului strat** coincid cu setările reale din slicer.\r
\r
## Modelul este prea mare pe X sau Y\r
\r
Micșorează **Dimensiunea pixelului (XY)** pentru a micșora modelul. Decupează întâi imaginea dacă are margini sau fundal nefolosit.\r
\r
## Imaginea are puncte sau insule minuscule\r
\r
Folosește **Eliminare dithering** după reducerea culorilor. Dacă imaginea are încă prea mulți pixeli izolați, încearcă mai puține culori sau alt algoritm de cuantizare.\r
\r
## Auto-paint pare inexact\r
\r
Cauze frecvente:\r
\r
- Distanțele de acoperire ale filamentelor sunt estimări, nu valori calibrate.\r
- Setul de filamente nu acoperă bine culorile imaginii.\r
- **Înălțimea maximă** comprimă prea mult zonele de tranziție.\r
- Optimizatorul are nevoie de **Potrivire îmbunătățită a culorilor** activată.\r
- Subiectul important se află în centru sau la margini, dar **Prioritatea regiunii** este **Uniformă**.\r
\r
Calibrează filamentele și verifică **Încrederea rezultatului** pentru indicii.\r
\r
Verifică separat rândul **Model de aspect**. Un scor general ridicat nu garantează precizia fizică. **Doar estimare** sau zero rețete active din matrice înseamnă că rezultatul curent nu are măsurători de matrice aplicabile. Salvarea unui profil calibrat nu face datele sale compatibile cu orice înălțime de strat sau set modificat de filamente. Consultă [Fluxuri de calibrare](calibration-workflows).\r
\r
## Un comutator l-a dezactivat pe altul\r
\r
**Păstrează separarea culorilor** și **Dithering de înălțime** se exclud în continuare, deoarece atribuie diferit culorile sursă înălțimilor imprimabile. **Geometrie netezită** și **Flat Paint** pot fi folosite împreună; reconstruiește după schimbarea oricărei setări. Consultă [Flat Paint](flat-paint) și [Auto-paint](auto-paint).\r
\r
## Separarea culorilor nu găsește un rezultat\r
\r
**Limita potrivirii unice** este o limită strictă a erorii de culoare prezise. Cu **Cere o potrivire unică pentru fiecare culoare** activat, o singură culoare fără potrivire respinge rezultatul. Poți reduce paleta 2D, adăuga un filament util, permite mai multă înălțime de tranziție sau repetări ori relaxa limita. Dezactivează potrivirea strictă numai dacă accepți renunțarea la culori distincte și unirea regiunilor lor. După respingere, pe ecran poate rămâne o previzualizare generată anterior; nu este o generare reușită cu setările respinse.\r
\r
## Ajustările dispar în 3D sau în fișierul descărcat\r
\r
Apasă **Aplică** în Ajustări pentru a integra aspectul live în imaginea sursă înainte de cuantizare, generare sau descărcare. Previzualizarea ajustărilor și sursa sunt separate. Consultă [Ajustările imaginii](image-adjustments).\r
\r
## Ștergerea unei mostre nu i-a eliminat pixelii\r
\r
**Șterge** elimină o opțiune din paletă și remapează imaginea la culorile rămase. Nu este o radieră. Folosește instrumentul Radieră sau setează transparența acelei mostre la zero pentru un decupaj. Cuantizarea poate face opaci pixelii parțial transparenți, așa că inspectează din nou silueta după aceea.\r
\r
## Colectează o înregistrare de diagnostic Auto-paint\r
\r
Pentru un rezultat care necesită investigații mai amănunțite în aplicația desktop, activează **Înregistrează diagnostice Auto-paint** în **Setări**, rulează un calcul Auto-paint nou și folosește **Deschide dosarul** pentru a găsi înregistrarea \`.jsonl\` rezultată. Activează înregistrarea înainte să înceapă calculul. Generarea geometriei dintr-un rezultat deja calculat nu înregistrează acel calcul anterior. Consultă [Diagnostice Auto-paint pe desktop](settings-and-controls#desktop-auto-paint-diagnostics) înainte de a partaja o înregistrare.\r
\r
## Generarea 3D este lentă\r
\r
Imaginile mari, multe culori, multe straturi și geometria netezită măresc timpul de generare. Încearcă:\r
\r
- Decuparea imaginii.\r
- Reducerea numărului de culori.\r
- Dezactivarea **Geometriei netezite**.\r
- Reducerea rezoluției cu **Redimensionează imaginea**. Simpla micșorare a dimensiunii pixelului face aceeași geometrie mai mică, nu mai simplă.\r
- Simplificarea opțiunilor Auto-paint.\r
\r
## Fișierul exportat se deschide cu culori neașteptate\r
\r
Pentru 3MF, verifică atribuirile materialelor sau filamentelor în slicer. Kromacut păstrează informațiile de culoare când este posibil, dar slicerele pot mapa diferit culorile la extrudoare.\r
\r
Pentru STL, fișierul nu conține atribuiri de culori ale filamentelor. Folosește **Instrucțiunile de imprimare** pentru schimbările de filament.\r
\r
Vederea Simulate din Auto-paint arată amestecuri estimate; slicerele afișează de obicei culorile fizice ale filamentelor. Comută Kromacut pe **Fizice** pentru verificarea atribuirilor, apoi inspectează maparea reală a rolelor. Niciuna dintre vederi nu dovedește culoarea imprimată finală.\r
\r
## Decuparea sau editările imaginii au mers prea departe\r
\r
Folosește **Anulează**. Refacerea este disponibilă dacă anulezi prea mult.\r
\r
În continuare: [Întrebări frecvente](faq).\r
`;export{e as default};
