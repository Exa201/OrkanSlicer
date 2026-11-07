const e=`---\r
title: Ajustările imaginii\r
slug: image-adjustments\r
order: 35\r
description: Fiecare glisor de ton și culoare, efectul asupra culorilor țintă și când să aplici definitiv previzualizarea imaginii.\r
---\r
\r
# Ajustările imaginii\r
\r
Ajustările schimbă imaginea țintă înainte de reducerea culorilor. Nu calibrează filamentul, nu schimbă distanța de acoperire și nu garantează că o culoare afișată poate fi imprimată fizic.\r
\r
Mișcă un glisor pentru a previzualiza efectul; previzualizarea se actualizează când închei interacțiunea. Toate pornesc de la zero. Săgețile individuale resetează un singur glisor; resetarea panoului le readuce pe toate la valorile inițiale.\r
\r
## Comenzi pentru ton și culoare\r
\r
![Exemple ilustrative negative, neutre și pozitive pentru toate cele douăsprezece comenzi de ajustare.](32_adjustment_controls.svg)\r
\r
_Sunt direcții schematice, nu predicții calibrate de imprimare. Efectele depind de sursă și de celelalte ajustări active._\r
\r
### Ton\r
\r
| Glisor | Interval | Valori negative | Valori pozitive | Consecință pentru pregătirea imprimării |\r
| ---------- | ------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |\r
| Expunere | −3 până la +3 trepte, pas 0,01 | Întunecă valorile RGB. | Le luminează; +1 dublează valorile canalelor până la limitare. | Deplasează tonurile țintă generale. Această ajustare a imaginii randate nu poate recupera informații RAW pierdute prin limitare. |\r
| Contrast | −100% până la +100% | Apropie tonurile de griul mediu. −100% devine gri mediu înaintea celorlalte ajustări. | Îndepărtează tonurile de griul mediu, cu limitare la negru/alb. | Separă regiunile majore, dar poate uniformiza umbrele și zonele luminoase subtile. |\r
| Zone luminoase | −100% până la +100% | Întunecă regiunile luminoase. | Luminează regiunile luminoase. | Schimbă tonurile luminoase care concurează pentru paletă; nu poate recupera detalii absente. |\r
| Umbre | −100% până la +100% | Întunecă regiunile de umbră. | Luminează regiunile de umbră. | Poate dezvălui diferențe întunecate existente înaintea reducerii. |\r
| Alburi | −100% până la +100% | Întunecă regiunea cea mai luminoasă. | Luminează regiunea cea mai luminoasă. | Separă sau combină ținte aproape albe. Nu este o setare a balansului de alb. |\r
| Negruri | −100% până la +100% | Întunecă regiunea cea mai întunecată. | Luminează regiunea cea mai întunecată. | Schimbă țintele aproape negre; negrul pur rămâne negru, deoarece valorile sunt scalate. |\r
\r
Zone luminoase/Umbre acoperă intervale mai largi decât Alburi/Negruri. Intervalele se suprapun: un pixel foarte întunecat poate răspunde atât la Negruri, cât și la Umbre. Intervalele tonale sunt evaluate după Expunere, Contrast, Temperatură/Tentă și ajustările HSL, deci comenzile pot interacționa.\r
\r
### Culoare și detaliu local\r
\r
| Glisor | Interval | Valori negative | Valori pozitive | Consecință pentru pregătirea imprimării |\r
| ----------- | -------------- | -------------------------------------------------------- | ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |\r
| Saturație | −100% până la +100% | Reduce intensitatea; −100% desaturează. | Mărește intensitatea. | Schimbă diferențele dintre culorile imaginii, nu gama fizică a filamentelor. |\r
| Vibranță | −100% până la +100% | Reduce mai mult saturația culorilor relativ nesaturate. | Mărește mai mult saturația culorilor relativ nesaturate. | Mai puțin uniformă decât Saturația, dar nu recunoaște tonurile pielii. Griul pur rămâne gri. |\r
| Nuanță | −180° până la +180° | Rotește nuanțele într-un sens. | Le rotește în celălalt sens. | Recolorează întreaga imagine; pentru o singură culoare exactă, editează o mostră. |\r
| Temperatură | −100 până la +100 | Mai rece: mai puțin roșu, mai mult albastru. | Mai cald: mai mult roșu, mai puțin albastru. | O ajustare aproximativă a dominantei de culoare, **nu în Kelvin**. |\r
| Tentă | −100 până la +100 | Adaugă verde. | Adaugă magenta, mărind roșul/albastrul și reducând verdele. | Corectează sau introduce o dominantă; nu este un profil măsurat al camerei. |\r
| Claritate | −100 până la +100 | Atenuează contrastul local. | Accentuează contrastul local și marginile. | Poate crea culori de margine/halouri care necesită cuantizare. Nu recuperează detalii și nu lățește liniile subțiri. |\r
\r
În afară de Expunere și Nuanță, glisoarele folosesc pași întregi. Alfa rămâne neschimbat. Cuantizarea are alt comportament pentru alfa: face complet opaci pixelii parțial transparenți.\r
\r
## Previzualizare față de aplicare\r
\r
![Previzualizarea live pornește separat de la sursă. Aplică integrează acel aspect și resetează glisoarele; cuantizarea, exportul PNG și 3D folosesc apoi pixelii modificați.](33_adjustment_bake.svg)\r
\r
**Aplică** integrează aspectul curent în imaginea de bază, resetează glisoarele la zero și creează un pas în istoricul imaginii. Nu reduce culorile, nu generează modelul și nu schimbă setările de imprimare.\r
\r
Distincția este importantă:\r
\r
- **Cuantizarea, Redimensionează imaginea, Descarcă imaginea și generarea 3D folosesc imaginea de lucru de bază**, nu ajustările de previzualizare neaplicate.\r
- **Culorile imaginii** descrie pixelii de bază, deci mostrele sale nu urmăresc ajustările live.\r
- Instrumentele de retușare editează imaginea de bază; ajustările active se reaplică peste ea.\r
- Eliminarea ditheringului citește imaginea ajustată. Aplică definitiv ajustările mai întâi pentru a nu le lăsa active peste rezultatul procesat.\r
\r
Secvența sigură este **previzualizează ajustările → Aplică ajustările → cuantizează → inspectează/curăță → generează 3D**. Când este posibil, decupează și redimensionează înaintea acestor pași.\r
\r
## Resetarea și anularea sunt diferite\r
\r
Resetarea elimină o ajustare live, nu o editare deja integrată în imagine. După Aplică, valorile zero ale glisoarelor sunt normale, deoarece efectul lor anterior se află acum în imagine. Folosește **Anulează** pentru a restaura imaginea anterioară.\r
\r
Operațiile Aplică repetate lucrează pe imaginea deja editată, astfel că limitarea valorilor și pierderea detaliilor tonale se pot acumula. Anulează întâi când compari alternative.\r
\r
În continuare: [Reducerea culorilor](reducing-colors).\r
`;export{e as default};
