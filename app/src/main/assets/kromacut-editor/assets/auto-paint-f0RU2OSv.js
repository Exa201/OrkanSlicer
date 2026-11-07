const e=`---\r
title: Comenzi Auto-paint\r
slug: auto-paint\r
order: 62\r
description: Datele filamentelor, detalii imprimabile, potrivire, limite de înălțime și încredere.\r
---\r
\r
# Comenzi Auto-paint\r
\r
Auto-paint prezice aspectul straturilor subțiri de filament suprapuse și alege înălțimi imprimabile pentru imaginea pregătită. Mai multe culori vizibile pot proveni din grosimi diferite ale aceluiași filament fizic. Douăzeci de culori ale imaginii nu necesită deci neapărat douăzeci de role.\r
\r
Setează [dimensiunea fizică, înălțimile straturilor și lățimea efectivă a liniei](3d-mode#3d-print-settings), adaugă filamente pe care le poți încărca efectiv, așteaptă calculul, apoi **Generează modelul 3D**. Datele de intrare se recalculează automat; geometria afișată se actualizează doar la generare.\r
\r
## Datele filamentelor\r
\r
| Comandă | Ce introduci sau faci | Efect |\r
| ---------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------ |\r
| **Adaugă filament** | Creează un rând nou gri neutru. | Adaugă un material disponibil, nu un locaș fizic al imprimantei. |\r
| **Mostră de culoare / Hex** | Alege culoarea opacă a filamentului sau introdu codul hexazecimal. | Schimbă amestecurile și culoarea materialului exportat. Introdu culoarea rolei reale, nu culoarea rezultatului dorit. |\r
| **Nume** | Dă rolei o etichetă utilă. | O identifică. Un nume gol revine la o etichetă automată bazată pe culoare. |\r
| **HD** | Distanță de acoperire la iluminare frontală, 0,01–2 mm. | HD mai mic acoperă mai repede culorile inferioare. HD mai mare necesită mai multă grosime și transmite mai mult la aceeași grosime. |\r
| **Convertește din TD** | Introdu Distanța de transmisie convențională pentru litofanie/retroiluminare și apasă **Convertește**. | Convertește aproximativ TD × 0,1, rotunjit la 0,01 mm și limitat la intervalul HD. Nu converti din nou o valoare HD. |\r
| **Baghetă** | Estimează HD din culoare. | O presupunere inițială, nu o măsurătoare. |\r
| **Indicator de stare** | Inspectează **Estimare** sau eticheta de încredere calibrată; treci cursorul pentru valorile canalelor RGB. | Descrie datele HD ale rândului, nu precizia întregii imagini. |\r
| **Coș** | Elimină rândul. | Filamentul nu mai este disponibil. Profilurile salvate rămân neschimbate până la salvare. |\r
| **Calibrează** | Deschide Distanță de acoperire, Palette Proof sau Stack Matrix. | Înregistrează date fizice folosind [Fluxuri de calibrare](calibration-workflows). |\r
\r
Confirmarea unei valori HD, conversia TD sau folosirea baghetei șterge calibrarea HD stocată a acelui rând. Schimbarea culorii unui filament calibrat face calibrarea inactivă și folosește o estimare derivată din culoare. Revenirea la culoarea măsurată poate reactiva calibrarea păstrată, dacă nu a fost între timp ștearsă sau înlocuită. Schimbarea numelui nu este o măsurătoare.\r
\r
Comportamentul măsurat al canalelor influențează atât culoarea prezisă, cât și grosimea tranziției. Dacă materialul sau procesul se schimbă, nu presupune că vechea previzualizare rămâne verificată.\r
\r
## Profiluri de filamente\r
\r
Încărcarea unui profil salvat înlocuiește setul de filamente de lucru. **Modificări nesalvate** înseamnă că setul curent diferă de profilul selectat.\r
\r
| Acțiune | Rezultat |\r
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |\r
| **Salvează modificările în profilul curent** | Suprascrie profilul editabil cu setul curent. Dezactivat dacă nu există modificări, nu este selectat un profil sau este selectat un șablon. |\r
| **Salvează ca profil nou** | Introdu un nume nevid pentru salvare separată, inclusiv pentru șabloane modificate. |\r
| **Redenumește profilul selectat** | Schimbă eticheta fără a schimba filamentele. Indisponibil pentru șabloane sau fără profil selectat. |\r
| **Importă profil din fișier** | Citește \`.kfil\` nativ, \`.kapp\` vechi, JSON sau CSV/TSV de role HueForge acceptat. |\r
| **Exportă filamentele curente ca fișier .kfil** | Exportă setul curent. Desktop deschide Salvează ca; browserul folosește fluxul său de descărcare. Anularea nu schimbă calibrarea. |\r
| **Șterge profilul selectat** | Elimină profilul salvat. Exportă mai întâi o copie de siguranță dacă este necesar. Șabloanele nu pot fi șterse. |\r
\r
Datele de aspect aparțin unei configurații exacte de filamente. Cât timp profilul selectat are editări nesalvate, datele sale de aspect nu sunt transmise către Auto-paint. Exportarea editărilor creează un profil denumit separat, fără date de aspect incompatibile din vechiul set. Calibrarea HD activă pentru fiecare filament este separată de datele matricelor și probelor la nivel de profil. Consultă [compatibilitatea profilurilor și gestionarea importului](settings-and-controls#filament-profile-files).\r
\r
### Șabloane\r
\r
Șabloanele furnizorilor oferă culori publicate, nume, mărci și valori HD estimate. Sunt doar pentru citire. Elimină culorile pe care nu le ai, calibrează, apoi **Salvează ca profil nou**. Culorile de referință ale furnizorilor nu garantează aspectul unui lot sau într-o anumită lumină. Șabloanele sunt neoficiale, nu recomandări din partea furnizorilor.\r
\r
## Potrivirea standard\r
\r
Cu **Potrivire îmbunătățită a culorilor dezactivată**, Kromacut sortează filamentele de la întunecat la luminos după luminanță și calculează tranzițiile. Nu urmează ordinea adăugării rândurilor. Maparea imaginii la înălțime folosește tot luminanța: normalizează luminozitatea între cei mai întunecați și cei mai luminoși pixeli netransparenți, plasează acel interval între suprafața fundației și vârful stivei, apoi aliniază la straturi imprimabile. Două nuanțe diferite cu aceeași luminozitate pot deci primi aceeași înălțime, indiferent de nuanță. Repetările, separarea, ditheringul de înălțime și comenzile optimizatorului sunt inactive.\r
\r
Folosește această variantă simplă de referință pentru relief bazat pe luminozitate. Potrivirea îmbunătățită ține cont de culoarea imaginii când alege atât secvența materialelor, cât și atribuirile de culori imprimabile, deci este modul potrivit când contează distincțiile de nuanță.\r
\r
## Înălțimea maximă\r
\r
Golește **Înălțime maximă** sau apasă **Auto** pentru înălțimea calculată a stivei, aliniată la straturi. Câmpul acceptă 0,5–20 mm. Limita este un plafon, nu o țintă, deci rezultatul poate fi mai scurt.\r
\r
O limită între două granițe valide de strat se rotunjește în jos. Dacă tranzițiile normale sunt prea înalte, Kromacut le comprimă și afișează înălțimea automată pentru comparație.\r
\r
![Stivele cu înălțime automată și limitată păstrează fundația opacă, în timp ce tranzițiile superioare devin mai scurte.](15_transition_height.svg)\r
\r
_Schemă. Benzile colorate identifică secvențele materialelor, nu aspectul amestecat prezis._\r
\r
Fundația trebuie să atingă în continuare aproximativ 95% opacitate în fiecare canal RGB modelat. O limită insuficientă respinge stiva, nu tratează un suport translucid drept opac. Mărește limita sau folosește un filament potrivit pentru fundație, cu HD mai mic.\r
\r
Comprimarea poate elimina culori intermediare utile. Nu este o scalare uniformă a unei imagini altfel identice. În **Flat Paint**, suportul transparent este geometrie suplimentară, iar aranjamentul primului strat diferă de relief. Verifică dimensiunile complete din **Model** și grosimea din slicer; Înălțimea maximă nu reprezintă grosimea plăcii inclusiv suportul.\r
\r
## Detalii imprimabile\r
\r
Setează [**Lățimea efectivă a liniei** în **Setări de imprimare 3D**](3d-mode#effective-line-width) la lățimea de extrudare dorită în slicer, nu la diametrul duzei sau dimensiunea pixelului. Previzualizarea avertismentelor de lățime și curățarea opțională a punctelor izolate din Auto-paint folosesc aceeași setare; comenzile lor rămân aici.\r
\r
Imaginează-ți o **dungă violet de doi pixeli** pe fundal albastru. La **0,10 mm/pixel**, dunga are **0,20 mm lățime**. Dacă linia de extrudare planificată are **0,40 mm lățime**, dunga este mai îngustă decât linia. Kromacut o poate marca **la risc**, dar păstrează dunga chiar cu curățarea activă. O regiune vizibilă îngustă poate face parte dintr-un strat de material mult mai lat de dedesubt, iar traseele slicerului pot păstra detalii semnalate de această verificare exclusiv a imaginii.\r
\r
![O dungă violet de doi pixeli este mai îngustă decât lățimea de extrudare planificată. Chihlimbariul este avertisment de lățime, nu culoare de filament. Curățarea punctelor izolate păstrează dunga și înlocuiește doar un punct roz minuscul închis cu albastru.](13_printable_detail.svg)\r
\r
_Este o estimare de lățime, nu un traseu exact de slicer. Barele colorate compară lățimile; pătratele arată imaginea exemplului._\r
\r
**Omite punctele de culoare izolate** oferă două variante:\r
\r
- **Oprit:** păstrează toți pixelii sursă la intrarea Auto-paint, inclusiv fiecare regiune evidențiată.\r
- **Pornit:** înainte de potrivire și generare, înlocuiește numai culorile folosite exclusiv în puncte minuscule, compacte și închise cu culoarea mai lată din jur. Întreaga întindere a punctului trebuie să fie mai mică decât lățimea efectivă a liniei și trebuie să aibă o singură culoare înconjurătoare neambiguă, cu o regiune mai lată. Dacă acea culoare sursă este folosită și într-o linie sau regiune mai mare oriunde în imagine, este păstrată peste tot.\r
\r
Liniile subțiri, conexiunile diagonale, ramurile atașate unor regiuni mai late, detaliile de la marginea imaginii și punctele lângă transparență sau mai multe culori sunt păstrate. Curățarea nu transformă niciodată un pixel într-o gaură și nu editează imaginea 2D originală. Este intenționat prudentă și poate lăsa puncte nedorite; folosește [curățarea 2D](dedithering-cleanup) pentru editare mai amplă.\r
\r
Folosește **Deschide previzualizarea** pentru a inspecta:\r
\r
- **La risc:** chihlimbariul marchează regiuni înguste de culoare sursă lângă culori mai late; rozul marchează regiuni înguste fără vecin mai lat. Ceilalți pixeli sunt estompați. Sunt avertismente, nu predicții că detaliul nu se poate imprima.\r
- **Rezultat:** pixelii primiți de Auto-paint după curățarea opțională. Nu este o previzualizare de slicer sau o garanție a imprimabilității.\r
- **Semnalați / eligibili / omiși:** proporția cu avertisment de lățime, pixelii care respectă regulile punctelor izolate și numărul efectiv înlocuit. **Pixeli semnalați păstrați** numără explicit avertismentele care nu schimbă imaginea.\r
\r
Când sunt omise culori de puncte eligibile, Auto-paint folosește imaginea curățată pentru a număra culorile țintă și a planifica stiva. Eliminarea unei culori țintă întregi poate schimba alegerile de potrivire în alte părți, deci inspectează rezultatul regenerat. Un procent nenul de avertismente cu **0 pixeli omiși** înseamnă că curățarea a păstrat toate detaliile sursă.\r
\r
După schimbarea comutatorului, lasă Auto-paint să termine calculul și apasă din nou **Generează modelul 3D**. Analiza măsoară regiuni conectate de culoare sursă, nu straturi fizice de material, pereți, umplere sau extrudare cu lățime variabilă. Verifică întotdeauna traseele feliate. Dacă detaliile se pierd într-adevăr acolo, mărește dimensiunea XY a modelului, lățește-le în 2D sau alege o lățime de extrudare mai fină, acceptată de imprimantă și slicer.\r
\r
## Potrivire îmbunătățită a culorilor\r
\r
Potrivirea îmbunătățită caută secvențe de materiale pentru paleta 2D curentă. Poate omite filamente care nu adaugă acoperire utilă. Opt role disponibile nu trebuie să producă opt secvențe.\r
\r
Optimizatorul nu reduce din nou paleta pregătită pe ascuns. Mai multe culori sursă necesită mai multă muncă. Pregătește imaginea în [Reducerea culorilor](reducing-colors), apoi evaluează aspectul realizabil în 3D.\r
\r
Dezactivarea potrivirii îmbunătățite dezactivează și separarea, și ditheringul de înălțime. Calculele noi le anulează pe cele vechi; progresul este aproximativ. Un calcul eșuat nu poate reveni la o stivă manuală fără legătură. Un model mai vechi poate rămâne vizibil până când generezi un rezultat nou valid.\r
\r
### Limita totală de repetări\r
\r
Alege **Oprit** sau până la **2, 4, 6, 8 ori 12 apariții suplimentare** pentru întreaga stivă. Nu este nici o limită per filament, nici un număr exact de schimbări. Negru → galben → negru folosește o apariție suplimentară a negrului. Revenirea la un material peste un substrat nou creează încă o cale posibilă de amestec.\r
\r
![Un buget comun de repetări și atribuiri distincte ale țintelor, comparate cu unirea culorilor abandonate.](14_repeats_separation.svg)\r
\r
_Secvențe și atribuiri conceptuale, nu predicții de materiale._\r
\r
Mai multe repetări permit o căutare mai largă și, posibil, mai multe schimbări de material în timpul imprimării. Bugetul este un plafon. Secvențele inutile pot fi omise.\r
\r
### Păstrează separarea culorilor\r
\r
Potrivirea obișnuită poate mapa culori diferite ale imaginii la același rezultat. Activează **Păstrează separarea culorilor** când contează aceste distincții, de exemplu pentru litere față de fundal sau tonuri vecine ale feței.\r
\r
**Limita potrivirii unice (ΔE)** este diferența maximă strictă de culoare pentru ca o culoare a imaginii să aibă un rezultat imprimabil distinct. Interval: 1–100; implicit: 6. Valorile mai mici cer potriviri mai apropiate și sunt mai greu de satisfăcut. Valorile mai mari permit mai multă eroare, nu filamente fizice mai bune.\r
\r
**Cere o potrivire unică pentru fiecare culoare** este activat implicit. Atribuirile incomplete eșuează. Dezactivează-l pentru o **paletă parțială**: culorile fără potrivire își pierd rezultatele distincte și se unesc cu mapările rămase. Imaginea rămâne umplută, dar pierde distincții. Dacă nicio culoare nu este eligibilă, chiar și modul parțial eșuează, deoarece nu rămâne nimic cu care să se unească.\r
\r
Optimizatorul maximizează mai întâi culorile păstrate și acoperirea imaginii sursă. Apoi preferă mai puține apariții repetate, mai puține secvențe de material și mai puține straturi fizice, înaintea unei erori mai mici în limite. Bugetele de repetare sunt explorate progresiv, iar căutarea se poate opri când fiecare culoare este păstrată. O verificare finală de eliminare scoate secvențele individuale care nu îmbunătățesc aceste priorități.\r
\r
La eșecul modului strict, ia în considerare mai puține culori 2D, mai multă înălțime sau repetări, alt filament potrivit, o limită ΔE mai mare ori unirea parțială. Alege compromisul pe care îl dorești cu adevărat, nu mări o limită doar ca să închizi eroarea.\r
\r
Separarea și **Ditheringul de înălțime** se exclud reciproc. Activarea uneia o dezactivează pe cealaltă.\r
\r
## Dithering de înălțime\r
\r
Ditheringul de înălțime poate distribui eroarea de rotunjire între blocuri mici la înălțimi imprimabile vecine când intrarea conține înălțimi între limitele de strat disponibile. Aceste diferențe de înălțime pot sugera tonuri intermediare de la distanță. Necesită potrivire îmbunătățită și acționează asupra hărții de înălțime exportate, nu asupra imaginii sursă 2D.\r
\r
![Mecanismul ditheringului de înălțime când există înălțimi fracționare: aliniere directă comparată cu distribuirea erorii de rotunjire între înălțimi imprimabile vecine.](16_height_dithering.svg)\r
\r
_Mecanism schematic, nu un rezultat înainte/după garantat. Înălțimi superioare diferite nu înseamnă culori suplimentare de role._\r
\r
Cu ditheringul activat, Auto-paint caută o înălțime intermediară între stratul ales și un strat imprimabil vecin atunci când amestecul lor îmbunătățește potrivirea estimată. Potrivirile exacte de culoare și cele ale țintelor calibrate păstrează înălțimea aleasă; regiunile fără un amestec vecin util rămân neschimbate. Reconstruiește modelul după modificarea setării, apoi compară previzualizarea și rezultatul din slicer. Dezactivarea ditheringului restabilește potrivirea discretă obișnuită.\r
\r
Dimensiunea punctelor urmează **Lățimea efectivă a liniei** din **Setări de imprimare 3D** în raport cu **Dimensiunea pixelului**, rotunjită la un bloc de pixeli întregi. Este o aproximație, nu o garanție exactă a lățimii minime. Regiunile de margine evită același tratament de dithering pentru a reduce artefactele de contur. Verifică în slicer insulele minuscule și deplasările suplimentare.\r
\r
Unde există eroare de înălțime fracționară, redistribuirea poate ajuta tonurile întinse, dar poate face grafica mică zgomotoasă sau geometria mai complexă. Nu poate adăuga o gamă cromatică lipsă ori valida o calibrare neacoperită de date. Când creează multe regiuni mici, combinarea cu Flat Paint poate fi deosebit de costisitoare, deoarece acele regiuni împart fiecare strat cu amprentă completă.\r
\r
## Setările optimizatorului\r
\r
![Prioritate uniformă, centrală și de margine pe aceeași imagine, cu detalierea crescândă a tranzițiilor reprezentată prin mai multe înălțimi potențiale.](18_optimizer_choices.svg)\r
\r
_Ponderi și alegeri schematice, nu culori măsurate sau numere exacte de straturi. Regiunile estompate primesc prioritate mai mică; nu sunt eliminate din imagine._\r
\r
| Comandă | Opțiuni și efect |\r
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |\r
| **Algoritm** | **Rapid:** căutare mai restrânsă și mai rapidă. **Echilibrat:** opțiunea implicită generală. **Minuțios:** rafinare mai profundă cu porniri multiple. **Profund:** căutare mai largă și mai costisitoare. **Ordine de bază exactă:** enumeră ordinele aplicabile fără repetări. |\r
| **Prioritatea regiunii** | **Uniformă:** pondere egală a pixelilor. **Accent pe centru:** favorizează culorile din apropierea centrului. **Accent pe margini:** favorizează culorile de lângă marginile imaginii. Schimbă prioritățile potrivirii, nu decupajul sau extrudarea. |\r
| **Detalierea tranzițiilor** | **Compact (80%)**, **Detaliat (90%, implicit)**, **Maxim (95%)** stabilesc punctele finale de opacitate ale tranziției. Valorile mai mari permit tranziții mai înalte și mai multe culori intermediare imprimabile, în limitele convergenței timpurii și ale înălțimii maxime. |\r
| **Sămânță aleatorie (opțional)** | **Automat** folosește o sămânță stabilă derivată din intrări. Introdu un întreg pentru a compara o altă căutare deterministă; golește pentru revenire la automat. Nu este un glisor de calitate. |\r
\r
Detalierea tranzițiilor afectează tranzițiile materialelor superioare, nu cerința fundației opace. Nu mărește rezoluția imaginii și nu îngustează lățimea liniei. Culorile de tranziție suplimentare pot să nu ajute imaginea curentă.\r
\r
Pentru aceeași sămânță, nivelurile euristice mai înalte păstrează cel mai bun rezultat al nivelului inferior. Rămâne tot o optimizare a predicțiilor. Ordinea de bază exactă verifică 109.600 de ordine nevide cu opt filamente și 986.409 cu nouă. Repetările folosesc o rafinare separată, nu o demonstrație exhaustivă pentru fiecare stivă repetată.\r
\r
## Zonele de tranziție și încrederea\r
\r
| Indicator | Interpretare |\r
| ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |\r
| **Zone de tranziție** | Secvențe fizice de material cu început, sfârșit și grosime. Etichetele de comprimare indică grosime sub cea ideală. |\r
| **Înălțime totală / straturi fizice** | Dimensiunile calculate ale stivei, nu numărul culorilor imaginii sau al rolelor. |\r
| **Starea separării culorilor** | Culori păstrate și unite, capacitate imprimabilă, cel mai mare ΔE păstrat și repetări. Țintele unite nu sunt potriviri reușite peste limită. |\r
| **Model de aspect** | Dacă predicția este susținută de simulare, comportament ajustat, comparații locale sau date Stack Matrix. Un număr de măsurători nu înseamnă că fiecare rezultat a fost măsurat. |\r
| **Încrederea predicției: medie / minimă** | Puterea datelor pentru culorile efectiv mapate. Media ponderată poate ascunde o regiune slabă dezvăluită de valoarea minimă. Numerele disting măsurători, interpolare, predicții ajustate și simulare. |\r
| **Încrederea rezultatului** | Indicatori combinați de calibrare HD, acoperire și comprimare. Nu este un procent măsurat al preciziei și nu este identic cu încrederea predicției. |\r
| **Calibrare / Acoperire / Comprimare** | Calitatea datelor HD, acoperirea culorilor sursă de filamente și impactul limitei de înălțime. Scorurile mari nu certifică imprimarea. |\r
| **Scor de calitate** | Comparație a optimizatorului, nu măsurătoare. La separare incompletă nestrictă, eticheta devine **Paletă parțială**. |\r
| **Iterații / Rezultat din cache** | Volumul căutării și reutilizarea rezultatului. Mai multe iterații nu dovedesc culori mai bune. |\r
| **Optim exact / Cel mai bun găsit** | Comparație exhaustivă aplicabilă fără repetări, față de rafinare euristică sau cu stive repetate. Niciuna nu dovedește precizia fizică. |\r
| **Nicio secvență eliminabilă** | Nicio eliminare a unei singure secvențe nu păstrează prioritățile selectate. O altă rearanjare cu mai multe secvențe ar putea fi totuși mai bună. |\r
\r
Datele sunt mai puțin convingătoare departe de măsurători, când observațiile apropiate nu concordă sau când predicțiile pentru datele de validare ratează culorile măsurate. Potrivirea obișnuită poate include un cost limitat de incertitudine. Separarea folosește în continuare ΔE brut pentru eligibilitate, deci incertitudinea nu poate valida o culoare din afara limitei.\r
\r
După schimbarea procesului sau a înălțimii stratului, inspectează acest rezumat al datelor. Un scor general de calibrare ridicat nu înseamnă că fiecare rețetă nouă este susținută. Consultă [Fluxuri de calibrare](calibration-workflows).\r
\r
## Sugerează următorul filament\r
\r
**Sugerează următorul filament** apare după ce există un rezultat. Caută o culoare ipotetică ce ar putea îmbunătăți acoperirea acestei imagini. Nu este o listă de produse sau o rolă deja încărcată în imprimantă.\r
\r
| Câmp sau acțiune | Semnificație |\r
| -------------------- | ----------------------------------------------------------------------------------------------------------------------- |\r
| **Mostră hexazecimală** | Culoarea opacă sugerată pentru filament. |\r
| **ΔE estimat +…%** | Reducerea estimată a erorii medii a imaginii, ținând cont de amestecuri, dacă este adăugat. Mai mare este mai bine; nu reprezintă încredere. |\r
| **HD** | Estimare inițială preluată de la cel mai apropiat filament existent după distanța perceptuală de culoare. Nu este măsurată pentru un produs. |\r
| **Acoperă** | Procentul pixelilor imaginii cu eroare estimată îmbunătățită. |\r
| **Izolare** | Distincția față de filamentele curente pe o scară de la 0 la 1. Mai mare indică un gol de acoperire mai separat. |\r
| **Adaugă la filamente** | Adaugă un rând de lucru numit \`Kromacut-Suggestion-…\` și recalculează cu el. |\r
\r
Găsește o rolă reală dacă sugestia este utilă, apoi introdu culoarea și calibrarea sa reale. Nu imprima presupunând că rândul ipotetic este deja disponibil. Sugestiile se resetează când se schimbă culorile imaginii sau setul de filamente. Un rezultat fără candidat arată că setul curent acoperă deja bine imaginea în cadrul acestui test aproximativ.\r
\r
În continuare: [Flat Paint](flat-paint), [Fluxuri de calibrare](calibration-workflows) sau [Generarea și exportul rezultatului](generating-exporting-output).\r
`;export{e as default};
