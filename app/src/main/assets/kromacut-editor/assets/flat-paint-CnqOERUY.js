const e=`---\r
title: Flat Paint\r
slug: flat-paint\r
order: 64\r
description: Relief în trepte, suporturi transparente cu fața în jos și plăci expuse cu fața în sus.\r
---\r
\r
# Flat Paint\r
\r
**Flat Paint** este un aranjament Auto-paint disponibil cu potrivire standard sau îmbunătățită. Creează o placă de grosime constantă în locul unui relief în trepte. Fiecare strat imprimat acoperă amprenta modelului, iar materiale diferite pot ocupa același strat, unul lângă altul.\r
\r
Folosește un flux multimaterial, precum AMS, CFS sau un schimbător de scule, cu suport adecvat în slicer. O schimbare manuală de filament la o anumită înălțime nu poate furniza mai multe materiale alăturate pe acel strat.\r
\r
![Secțiunile compară relieful normal, Flat Paint cu suport transparent și fața în jos, respectiv Flat Paint fără suport și cu fața în sus.](17_flat_paint_orientation.svg)\r
\r
_Secțiuni conceptuale. Culorile identifică materialele; etichetele identifică fața de vizualizare._\r
\r
Progresul generării numără părțile plasei, nu straturile imprimate: mai multe părți colorate pot ocupa același strat. Indicatorul Model arată numărul straturilor fizice, inclusiv suportul transparent când este utilizat. Previzualizarea straturilor taie placa strat cu strat; ambele mânere taie și regiunile unite de fundație și culoare. Exportul include întotdeauna modelul complet. Butonul Generează modelul 3D regenerează întotdeauna modelul, chiar dacă setările nu s-au schimbat, și restabilește întregul interval de straturi și vederea inițială a camerei.\r
\r
Dacă o construire eșuează, Kromacut elimină modelul incomplet și afișează eroarea în previzualizare. Apasă Generează modelul 3D pentru a reîncerca; după o construire reușită, dimensiunile modelului și previzualizarea straturilor reapar.\r
\r
## Implicit: fața în jos, cu suport transparent\r
\r
Activează **Flat Paint** și lasă **Fața în sus, fără strat transparent** dezactivat.\r
\r
1. Suportul transparent se imprimă primul și devine fața netedă de vizualizare, lipită de placă. Atribuie obiectului său filament transparent.\r
2. Coloanele imaginii își inversează ordinea normală a materialelor pentru vizualizarea de dedesubt. Materialul fundației umple spațiul din spatele coloanelor mai scurte.\r
3. Exportă **3MF** și păstrează orientarea. Imaginea este deja oglindită; nu o oglindi din nou în slicer.\r
4. După imprimare, întoarce piesa pentru a o privi prin suport.\r
\r
Textul poate părea inversat când este privit din spate în slicer. Verifică în schimb fața destinată vizualizării. Rotește camera sub previzualizarea Kromacut pentru a inspecta acea față.\r
\r
Suportul este geometrie suplimentară care necesită filament transparent real. Transparența de pe ecran nu măsoară claritatea filamentului sau finisajul plăcii de imprimare. Verifică grosimea totală în indicatorul **Model** și în slicer, nu doar **Înălțimea maximă** din Auto-paint.\r
\r
## Fața în sus, fără strat transparent\r
\r
Activează acest comutator pentru a elimina suportul transparent:\r
\r
- Fiecare coloană își păstrează ordinea normală a materialelor de jos în sus.\r
- Materialul fundației umple spațiul de sub coloanele mai scurte, aliniind culorile vizibile la o suprafață superioară plană.\r
- Nu există obiect de suport și nu este necesar filament transparent.\r
- Imprimă cu fața în sus, conform exportului, fără oglindire, și privește suprafața superioară expusă fără a întoarce piesa.\r
\r
Aceste aranjamente au geometrii diferite. Folosește din nou **Generează modelul 3D** după schimbarea comutatorului. Întoarcerea unui export vechi nu îl transformă dintr-un aranjament în celălalt.\r
\r
## Compararea fluxurilor\r
\r
| Flux | Fața de vizualizare | Geometrie | Atribuire în slicer |\r
| ------------------- | ---------------------------- | --------------------------------------------------- | ---------------------------------- |\r
| Auto-paint normal | Suprafață superioară în trepte | Coloanele mai scurte se opresc mai devreme. | Secvențe de material fizic pe înălțime. |\r
| Flat Paint, implicit | Partea de jos, prin suportul transparent | Coloane oglindite și inversate, cu fundația în spate. | Obiecte pentru fiecare filament, plus suport. |\r
| Flat Paint, fața în sus | Suprafață superioară plană expusă | Ordine normală, cu fundație sub coloanele mai scurte. | Obiecte pentru fiecare filament, fără suport. |\r
\r
**Geometrie netezită** funcționează în ambele orientări Flat Paint. Alege o intensitate și reconstruiește modelul. Netezirea atenuează conturul exterior și granițele comune dintre culori, păstrând placa plană, înălțimile straturilor și stivele de materiale. Contactele diagonale folosesc îmbinări comune mici pentru a închide regiunile fără suprapuneri. Instrucțiunile de imprimare și 3MF înregistrează intensitatea folosită.\r
\r
## Exportă și verifică\r
\r
Este disponibil doar **3MF**. Un STL fără culori ar pierde aranjamentul semnificativ al materialelor și ar păstra doar o placă. Obiectele sunt grupate după filamentul real, nu după fiecare culoare amestecată prezisă.\r
\r
1. Potrivește înălțimea straturilor normale și a primului strat cu Kromacut.\r
2. Păstrează scara și orientarea exportată. Nu adăuga încă o oglindire.\r
3. Atribuie corect fiecare obiect, inclusiv filamentul transparent al suportului implicit.\r
4. Inspectează straturile individuale din slicer pentru regiuni alăturate și o placă completă.\r
5. Confirmă direcția textului dinspre fața de vizualizare, apoi verifică schimbările și timpul de imprimare.\r
\r
**Instrucțiunile de imprimare** înlocuiesc lista de schimbări manuale cu indicații multimaterial specifice aranjamentului. **Previzualizarea straturilor** are o bară simplă, deoarece fiecare strat poate conține mai multe materiale. Limitele sale afectează în continuare doar inspecția, niciodată exportul complet.\r
\r
## Compromisuri de cost și detaliu\r
\r
O față plană nu înseamnă neapărat o imprimare mai simplă. Umplerea amprentei pe fiecare strat adaugă material și geometrie mai complexă față de relief. Straturile subțiri, stivele înalte și **Ditheringul de înălțime** pot produce multe regiuni mici, mai multe deplasări și generare, export sau feliere mai lente.\r
\r
Kromacut avertizează înainte de generarea proiectelor Flat Paint mari. **Generează oricum** acceptă volumul de lucru; nu certifică adecvarea imprimantei. Testează o piesă mică dacă materialul sau finisajul nu este verificat. Flat Paint păstrează aranjamentul optic intenționat al coloanelor, dar depinde în continuare de materiale calibrate și de un proces de imprimare corespunzător.\r
\r
În continuare: [Generarea și exportul rezultatului](generating-exporting-output).\r
`;export{e as default};
