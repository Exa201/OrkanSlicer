const e=`---\r
title: Încărcarea imaginilor\r
slug: loading-images\r
order: 30\r
description: Importă, decupează, redimensionează și retușează pixelii imaginii înainte de a deveni regiuni imprimate.\r
---\r
\r
# Încărcarea imaginilor\r
\r
În modul **2D**, pregătești imaginea pe care o va folosi modelul 3D. Modificările de culoare afectează culorile țintă; editările pixelilor afectează formele și detaliile imprimării.\r
\r
## Alege o sursă\r
\r
Apasă **Alege fișierul** în bara de instrumente a previzualizării sau trage un fișier imagine în previzualizarea 2D. Se încarcă doar primul fișier dintr-o tragere. Folosește un format de imagine pe care browserul sau componenta web a aplicației desktop îl poate decoda; PNG este util când contează transparența. Developează mai întâi fișierele RAW ale camerei și exportă-le într-un format obișnuit de imagine.\r
\r
Kromacut pornește cu sigla sa ca exemplu. Încărcarea altei imagini înlocuiește imaginea de lucru curentă. Nu publică imaginea și nu descarcă o sursă dintr-un link web lipit.\r
\r
## Inspectează fără să schimbi imaginea\r
\r
| Comandă | Efect |\r
| ------------------- | ----------------------------------------------------------------------------------------- |\r
| Rotița mouse-ului | Mărește în jurul indicatorului. Schimbă doar vederea, nu rezoluția imaginii sau dimensiunea imprimării. |\r
| Tragere cu butonul stâng | Deplasează vederea când nu este activ niciun instrument de retușare. |\r
| Tragere cu butonul mijlociu | Deplasează vederea chiar și când este activ un instrument de retușare. |\r
| Comută tabla de șah | Afișează un model în spatele pixelilor transparenți. Modelul nu face parte din imagine sau imprimare. |\r
| Indicatorul dimensiunii imaginii | Afișează dimensiunile în pixeli. La decupare, arată și dimensiunile propuse ale decupajului. |\r
\r
Previzualizarea folosește margini clare ale pixelilor, fără netezire. Mărește pentru a găsi pixelii izolați și detaliile înguste.\r
\r
## Decupare, redimensionare sau schimbarea dimensiunii de imprimare?\r
\r
![Decuparea elimină o parte din imagine, redimensionarea reduce rezoluția în pixeli, iar Dimensiunea pixelului schimbă scara fizică a fiecărui pixel.](30_crop_resize_scale.svg)\r
\r
_Dimensiunile schematice ilustrează relația, nu o dimensiune recomandată de imprimare._\r
\r
### Decupare\r
\r
Apasă **Decupează**, trage selecția sau mânerele colțurilor și marginilor, apoi alege **Salvează decupajul**. **Anulează decuparea** lasă imaginea neschimbată. Salvarea păstrează dreptunghiul selectat la rezoluția pixelilor imaginii.\r
\r
Decupează marginile nedorite înainte de reducerea culorilor, ca să nu concureze cu subiectul pentru paletă. Decuparea este dreptunghiulară; pentru un fundal neregulat folosește transparența.\r
\r
### Redimensionarea imaginii\r
\r
**Scară** variază de la **1% la 100%**, implicit **50%**. **Curent** și **După redimensionare** arată dimensiunile înainte de confirmare. Apasă **Aplică** pentru micșorare. 100%, sau o valoare care se rotunjește la aceleași dimensiuni, nu face nimic. Săgeata de resetare resetează procentul, nu imaginea.\r
\r
Redimensionarea folosește netezire, deci poate introduce culori amestecate pe margini și transparență parțială. Redimensionează înainte de cuantizare sau redu din nou culorile după aceea. Aplicările repetate redimensionează imaginea deja redimensionată: două aplicări de 50% păstrează 25% din lățimea și înălțimea originale. Folosește Anulează pentru a recupera detaliile, nu încerca să mărești imaginea aici.\r
\r
### Dimensiunea fizică în 3D\r
\r
**Dimensiunea pixelului (XY)** reprezintă milimetri pe pixel al imaginii, nu rezoluția imaginii. O imagine complet opacă, lată de 1000 de pixeli, la 0,1 mm/pixel are lățimea de 100 mm. Redimensionarea la 500 de pixeli cu aceeași setare dă 50 mm. Schimbarea la 0,2 mm/pixel restaurează lățimea de 100 mm, dar nu detaliile eliminate. Marginile exterioare complet transparente nu intră în amprenta modelului.\r
\r
Reducerea dimensiunilor în pixeli reduce volumul de procesare și geometrie. Schimbarea exclusivă a dimensiunii pixelului nu elimină pixeli. Consultă [Modul 3D](3d-mode) pentru comenzile scării fizice.\r
\r
## Retușează pixelii\r
\r
**Pensulă**, **Radieră**, **Umplere**, **Text** și **Alege culoarea din imagine** folosesc pixeli cu margini dure, fără antialiasing. O culoare personalizată poate totuși adăuga o culoare în paletă; marginile dure împiedică apariția nedorită a culorilor amestecate pe contur.\r
\r
![Pensula adaugă pixeli de culoare exactă, radiera elimină pixeli prin transparență, umplerea schimbă o regiune conectată, iar textul devine pixeli ai imaginii cu margini dure.](31_pixel_tools.svg)\r
\r
| Instrument sau câmp | Cum funcționează | Efect asupra imprimării |\r
| --------------------- | ------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------- |\r
| Pensulă | Trage pentru a picta pixeli opaci. Dimensiunea este un diametru de 1–64 pixeli ai imaginii, implicit 4. | Repară contururi, unește regiuni sau îngroașă detalii. Cursorul îi arată amprenta. |\r
| Radieră | Folosește aceeași comandă de dimensiune, dar scrie pixeli complet transparenți. | Elimină material din formă, putând crea găuri sau separa piese. |\r
| Umplere | Înlocuiește regiunea apăsată care are exact același RGB și alfa. Conexiunile sunt prin muchii, nu diagonale. | Recolorează o regiune conectată, nu toate aparițiile culorii. Nu există o toleranță fotografică a culorii. |\r
| Alege culoarea din imagine | Preia un pixel sursă netransparent și trece la Pensulă. | Refolosește o culoare din imaginea de bază, nu un efect de ajustare neaplicat. |\r
| Culoarea instrumentului | Alege din Culorile imaginii, folosește selectorul sau introdu un cod hexazecimal de șase cifre. Închiderea ferestrei confirmă selecția. | Stabilește culoarea opacă pentru Pensulă, Umplere sau Text. |\r
| Dimensiunea textului | Mărimea fontului de la 6 la 128 de pixeli ai imaginii, implicit 24. | Textul mai mare produce elemente mai mari la aceeași scară fizică; valoarea nu este în milimetri. |\r
\r
### Plasează textul\r
\r
Selectează Text, apasă pe imagine și scrie. Enter adaugă un rând. Trage mânerul de deplasare de deasupra casetei pentru repoziționare sau mânerul din dreapta pentru ajustarea încadrării cuvintelor. Dimensiunea și culoarea actualizează ciorna.\r
\r
Apasă butonul de confirmare sau **Ctrl+Enter** (**Command+Enter** pe macOS) pentru aplicare. Apăsarea în alt loc al imaginii sau schimbarea instrumentului confirmă de asemenea textul. X sau **Escape** renunță la ciorna deschisă; încă un Escape iese din instrument. Textul aplicat devine pixeli, nu un obiect text editabil.\r
\r
Fiecare trasare, umplere sau plasare de text care schimbă imaginea este un pas în istoricul imaginii. O linie de 1 pixel la 0,1 mm/pixel are doar 0,1 mm lățime, oricât de mare pare la zoom. Inspectează literele înguste în slicer.\r
\r
## Elimină un fundal\r
\r
Nu există selecție automată a subiectului sau comandă AI de eliminare a fundalului. Pentru un fundal uniform, deschide mostra sa din Culorile imaginii, fă-o complet transparentă și aplică. Se elimină fiecare potrivire exactă, inclusiv pixelii similari ai subiectului. Folosește Radiera pentru eliminare locală și tabla de șah pentru inspectarea conturului.\r
\r
Nu folosi **Șterge** pe mostră în acest scop: remapează culorile, nu face pixelii transparenți. Consultă [Culorile imaginii](reducing-colors#image-colors).\r
\r
## Anulare, descărcare și golire\r
\r
**Anulează** și **Refă** parcurg modificările confirmate ale imaginii: încărcare, decupare, redimensionare, integrarea ajustărilor, cuantizare, eliminarea ditheringului, editarea mostrelor și retușuri. Nu reprezintă istoricul fiecărei setări sau mișcări de glisor. O editare nouă șterge calea de refacere. Istoricul aparține sesiunii curente a aplicației, deci salvează imaginea dacă ai nevoie de ea ulterior.\r
\r
**Descarcă imaginea** salvează imaginea de lucru de bază ca PNG, la rezoluția pixelilor imaginii. Nu include zoomul, tabla de șah, mânerele de decupare sau ciornele de text. Aplică textul și folosește **Aplică ajustările** înainte, pentru a le include. Aplicația desktop folosește un dialog de salvare; în browser, locația urmează setările sale de descărcare.\r
\r
**Elimină imaginea** golește spațiul de lucru, nu setările. Nu te baza pe ea ca editare reversibilă: nu adaugă imaginea eliminată ca pas nou de anulare. Descarcă întâi o copie dacă trebuie să o păstrezi.\r
\r
În continuare: [Ajustările imaginii](image-adjustments).\r
`;export{e as default};
