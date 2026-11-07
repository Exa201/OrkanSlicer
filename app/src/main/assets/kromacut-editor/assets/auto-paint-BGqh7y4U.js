const i=`---\r
title: Controlli Auto-paint\r
slug: auto-paint\r
order: 62\r
description: Dati dei filamenti, dettagli stampabili, corrispondenza, limiti di altezza e affidabilità.\r
---\r
\r
# Controlli Auto-paint\r
\r
Auto-paint prevede l’effetto di strati sottili di filamento sovrapposti e sceglie altezze stampabili per l’immagine preparata. Più colori visibili possono derivare da spessori diversi dello stesso filamento fisico. Venti colori nell’immagine, quindi, non richiedono necessariamente venti bobine.\r
\r
Imposta [dimensioni fisiche, altezze degli strati e larghezza effettiva della linea](3d-mode#3d-print-settings), aggiungi i filamenti che puoi effettivamente caricare, attendi il calcolo, quindi usa **Genera modello 3D**. Le modifiche avviano automaticamente un nuovo calcolo; la geometria visualizzata si aggiorna solo quando generi il modello.\r
\r
## Dati dei filamenti\r
\r
| Controllo | Cosa inserire o fare | Effetto |\r
| ---------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------ |\r
| **Aggiungi filamento** | Crea una nuova riga grigio neutro. | Aggiunge un materiale disponibile, non uno slot fisico della stampante. |\r
| **Campione colore / Esadecimale** | Scegli il colore opaco del filamento o inserisci il codice esadecimale. | Modifica le miscele e il colore del materiale esportato. Inserisci il colore reale della bobina, non un colore finale desiderato. |\r
| **Nome** | Assegna alla bobina un’etichetta utile. | La identifica. Un nome vuoto ripristina l’etichetta automatica basata sul colore. |\r
| **HD** | Distanza di copertura con luce frontale, da 0,01 a 2 mm. | Una HD più corta nasconde prima i colori sottostanti. Una HD più lunga richiede più spessore e trasmette più luce a parità di spessore. |\r
| **Converti da TD** | Inserisci una distanza di trasmissione convenzionale per litofanie/retroilluminazione e premi **Converti**. | Converte approssimativamente TD × 0,1, arrotonda a 0,01 mm e limita all’intervallo HD. Non convertire nuovamente un valore già espresso in HD. |\r
| **Bacchetta** | Stima la HD dal colore. | È un’ipotesi iniziale, non una misura. |\r
| **Indicatore di stato** | Esamina **Stima** o il livello di affidabilità calibrato; passa sopra per i valori dei canali RGB. | Descrive i dati HD di questa riga, non la precisione dell’intera immagine. |\r
| **Cestino** | Rimuovi la riga. | Quel filamento non è più disponibile. I profili salvati restano invariati fino al salvataggio. |\r
| **Calibra** | Apri Distanza di copertura, Palette Proof o Stack Matrix. | Registra dati fisici seguendo i [Flussi di calibrazione](calibration-workflows). |\r
\r
Confermare un valore HD, convertire TD o usare la bacchetta cancella la calibrazione HD memorizzata per quella riga. Cambiare il colore di un filamento calibrato rende inattiva la calibrazione e usa una stima ricavata dal colore. Tornare al colore misurato può riattivare la calibrazione conservata, purché nel frattempo non sia stata cancellata o sostituita. Cambiare il nome non costituisce una misura.\r
\r
Il comportamento misurato dei canali influenza sia il colore previsto sia lo spessore delle transizioni. Se cambiano materiale o processo, non dare per verificata la vecchia anteprima.\r
\r
## Profili filamenti\r
\r
Caricare un profilo salvato sostituisce il set di filamenti di lavoro. **Modifiche non salvate** significa che il set attuale differisce dal profilo selezionato.\r
\r
| Azione | Risultato |\r
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |\r
| **Salva le modifiche nel profilo attuale** | Sovrascrive il profilo modificabile con il set attuale. Disabilitato se non ci sono modifiche, non è selezionato un profilo o è selezionato un modello. |\r
| **Salva come nuovo profilo** | Inserisci un nome non vuoto per salvare separatamente, anche i modelli modificati. |\r
| **Rinomina il profilo selezionato** | Cambia l’etichetta senza modificare i filamenti. Non disponibile per i modelli o senza un profilo selezionato. |\r
| **Importa profilo da file** | Legge \`.kfil\` nativi, \`.kapp\` precedenti, JSON o CSV/TSV di bobine HueForge supportati. |\r
| **Esporta i filamenti attuali come file .kfil** | Esporta il set attuale. La versione desktop apre Salva con nome; il browser usa il proprio flusso di download. Annullare non modifica la calibrazione. |\r
| **Elimina il profilo selezionato** | Rimuove il profilo salvato. Esporta prima una copia di sicurezza, se necessaria. I modelli non possono essere eliminati. |\r
\r
I dati di aspetto appartengono a una configurazione esatta dei filamenti. Finché un profilo selezionato ha modifiche non salvate, i suoi dati di aspetto salvati non vengono passati ad Auto-paint. Esportare queste modifiche crea un profilo con un nome separato, senza i dati di aspetto incompatibili del vecchio set. La calibrazione HD attiva per singolo filamento è distinta dai dati di matrici e prove a livello di profilo. Vedi [compatibilità dei profili e gestione dell’importazione](settings-and-controls#filament-profile-files).\r
\r
### Modelli\r
\r
I modelli dei fornitori forniscono colori dichiarati, nomi, marchi e valori HD stimati. Sono in sola lettura. Rimuovi i colori che non possiedi, calibra, quindi usa **Salva come nuovo profilo**. I colori di riferimento del fornitore non garantiscono il risultato per un lotto o una condizione di osservazione. I modelli non sono ufficiali né approvati dai fornitori.\r
\r
## Corrispondenza standard\r
\r
Con **Corrispondenza colori avanzata disattivata**, Kromacut ordina i filamenti dal più scuro al più chiaro per luminanza e ne calcola le transizioni. Non segue l’ordine di aggiunta delle righe. Anche la mappatura dall’immagine all’altezza usa la luminanza: normalizza la luminosità tra i pixel non trasparenti più scuri e più chiari, colloca quell’intervallo tra la superficie della base e la sommità della pila, poi lo allinea agli strati stampabili. Due tinte diverse con la stessa luminosità possono quindi ricevere la stessa altezza, indipendentemente dalla tinta. Ripetizioni, separazione, dithering dell’altezza e controlli dell’ottimizzatore sono inattivi.\r
\r
Usa questa base più semplice per un rilievo guidato dalla luminosità. La corrispondenza avanzata considera il colore dell’immagine sia nella scelta della sequenza dei materiali sia nell’assegnazione dei colori stampabili, quindi è la modalità adatta quando le differenze di tinta sono importanti.\r
\r
## Altezza massima\r
\r
Svuota **Altezza massima** o premi **Automatico** per usare l’altezza della pila calcolata e allineata agli strati. Il campo accetta da 0,5 a 20 mm. Un limite è un tetto, non un obiettivo, quindi il risultato può essere più basso.\r
\r
Un limite tra due confini validi degli strati viene arrotondato per difetto. Se le transizioni normali sono troppo alte, Kromacut le comprime e mostra l’altezza automatica per il confronto.\r
\r
![Le pile automatiche e quelle con altezza limitata mantengono la base opaca mentre le transizioni superiori diventano più corte.](15_transition_height.svg)\r
\r
_Schema. Le bande colorate identificano sequenze di materiale, non l’aspetto previsto delle miscele._\r
\r
La base deve comunque raggiungere circa il 95% di opacità in ogni canale RGB modellato. Un limite insufficiente fa rifiutare la pila, anziché trattare un fondo traslucido come opaco. Aumenta il limite o usa per la base un filamento adatto con HD più corta.\r
\r
La compressione può eliminare colori intermedi utili. Non è un ridimensionamento uniforme di un’immagine altrimenti identica. In **Flat Paint**, il supporto trasparente è una geometria aggiuntiva e la disposizione del primo strato differisce dal rilievo. Controlla le dimensioni complete del **Modello** e lo spessore nello slicer; Altezza massima non indica lo spessore della lastra comprensivo del supporto.\r
\r
## Dettagli stampabili\r
\r
Imposta [**Larghezza effettiva della linea** nelle **Impostazioni di stampa 3D**](3d-mode#effective-line-width) sulla larghezza di estrusione prevista dallo slicer, non sul diametro dell’ugello o sulla dimensione pixel. L’anteprima degli avvisi di larghezza e la pulizia facoltativa dei puntini isolati di Auto-paint usano la stessa impostazione; i loro controlli restano qui.\r
\r
Immagina una **striscia viola larga due pixel** su uno sfondo blu. A **0,10 mm/pixel**, la striscia è **larga 0,20 mm**. Se la linea di estrusione prevista è **larga 0,40 mm**, la striscia è più stretta della linea. Kromacut può segnalarla come **a rischio**, ma la mantiene anche con la pulizia attiva. Una regione sottile visibile nell’immagine può far parte di uno strato sottostante di materiale molto più ampio, e i percorsi dello slicer possono conservare dettagli segnalati da questo controllo basato solo sull’immagine.\r
\r
![Una striscia viola di due pixel è più stretta della larghezza di estrusione prevista. L’ambra è un avviso di larghezza, non un colore di filamento. La pulizia dei puntini isolati mantiene la striscia e sostituisce soltanto un minuscolo puntino rosa circondato dal blu.](13_printable_detail.svg)\r
\r
_È una stima della larghezza, non un percorso esatto dello slicer. Le barre colorate confrontano le larghezze; i quadrati mostrano l’immagine di esempio._\r
\r
**Escludi puntini di colore isolati** offre due scelte:\r
\r
- **Disattivato:** conserva tutti i pixel sorgente nell’ingresso di Auto-paint, comprese tutte le regioni evidenziate.\r
- **Attivato:** prima della corrispondenza e della generazione del modello, sostituisce solo i colori usati esclusivamente in minuscoli puntini compatti e racchiusi con il colore più ampio che li circonda. L’intera estensione di un puntino deve essere minore della larghezza effettiva della linea e deve esserci un unico colore circostante inequivocabile con una regione più ampia. Se lo stesso colore sorgente compare anche in una linea o in una regione più grande in qualsiasi punto dell’immagine, viene mantenuto ovunque.\r
\r
Linee sottili, collegamenti diagonali, ramificazioni attaccate a regioni più ampie, dettagli sul bordo dell’immagine e puntini accanto alla trasparenza o a più colori vengono mantenuti. La pulizia non trasforma mai un pixel in un foro e non modifica l’immagine 2D originale. È deliberatamente prudente e può lasciare puntini indesiderati; usa la [pulizia 2D](dedithering-cleanup) per modifiche più estese all’immagine.\r
\r
Usa **Apri anteprima** per esaminare:\r
\r
- **A rischio:** l’ambra indica regioni sottili dei colori sorgente vicine a colori più ampi; il rosa indica regioni sottili senza un vicino più ampio. Gli altri pixel sono attenuati. Sono avvisi, non previsioni che il dettaglio non possa essere stampato.\r
- **Risultato:** i pixel ricevuti da Auto-paint dopo la pulizia facoltativa. Non è un’anteprima dello slicer né una garanzia di stampabilità.\r
- **Segnalati / idonei / esclusi:** la quota di avvisi di larghezza, i pixel che soddisfano le regole dei puntini isolati e il numero effettivamente sostituito. **Pixel segnalati mantenuti** conta esplicitamente gli avvisi che non modificano l’immagine.\r
\r
Quando vengono esclusi colori di puntini idonei, Auto-paint usa l’immagine ripulita per contare i colori obiettivo e pianificare la pila. Rimuovere un intero colore obiettivo può cambiare le scelte di corrispondenza altrove, quindi esamina il risultato rigenerato. Una percentuale di avvisi non nulla con **0 pixel esclusi** significa che la pulizia ha conservato tutti i dettagli sorgente.\r
\r
Dopo aver cambiato l’interruttore, lascia che Auto-paint completi il calcolo e fai nuovamente clic su **Genera modello 3D**. L’analisi misura regioni connesse dei colori sorgente, non strati fisici di materiale, pareti, riempimento o estrusione a larghezza variabile. Controlla sempre i percorsi generati dallo slicer. Se lì il dettaglio viene davvero perso, aumenta le dimensioni XY del modello, allargalo in 2D oppure scegli una larghezza di estrusione più fine supportata da stampante e slicer.\r
\r
## Corrispondenza colori avanzata\r
\r
La corrispondenza avanzata cerca sequenze di materiali per la tavolozza 2D attuale. Può escludere filamenti che non aggiungono copertura utile. Otto bobine disponibili non devono necessariamente produrre otto sequenze.\r
\r
L’ottimizzatore non riduce di nuovo la tavolozza preparata senza dirtelo. Più colori sorgente richiedono più lavoro. Prepara l’immagine in [Riduzione dei colori](reducing-colors), poi valuta in 3D l’aspetto ottenibile.\r
\r
Disattivare la corrispondenza avanzata disabilita anche separazione e dithering dell’altezza. I nuovi calcoli annullano quelli precedenti; l’avanzamento è approssimativo. Un calcolo fallito non può ripiegare su una pila manuale non correlata. Un vecchio modello generato può rimanere visibile finché non generi un nuovo risultato valido.\r
\r
### Limite totale di ripetizioni\r
\r
Scegli **Disattivato**, oppure fino a **2, 4, 6, 8 o 12 occorrenze aggiuntive** per l’intera pila. Non è un limite per singolo filamento né un numero esatto di cambi. Nero → giallo → nero usa un’occorrenza aggiuntiva del nero. Tornare a un materiale sopra un nuovo fondo crea un’altra possibile sequenza di miscelazione.\r
\r
![Un limite condiviso di ripetizioni e assegnazioni distinte degli obiettivi, confrontati con l’unione dei colori eliminati.](14_repeats_separation.svg)\r
\r
_Sequenze e assegnazioni concettuali, non previsioni dei materiali._\r
\r
Più ripetizioni consentono una ricerca più ampia e potenzialmente più cambi di materiale durante la stampa. Il limite è un tetto. Le sequenze non necessarie possono essere escluse.\r
\r
### Mantieni la separazione dei colori\r
\r
La corrispondenza ordinaria può associare colori diversi dell’immagine alla stessa uscita. Abilita **Mantieni la separazione dei colori** quando queste distinzioni contano, per esempio per scritte rispetto allo sfondo o tonalità adiacenti di un volto.\r
\r
**Limite di corrispondenza univoca (ΔE)** è una differenza cromatica massima rigida affinché un colore dell’immagine abbia una propria uscita stampabile distinta. Intervallo: da 1 a 100; valore predefinito: 6. Valori inferiori richiedono corrispondenze più vicine e sono più difficili da soddisfare. Valori superiori consentono più errore, non migliorano i filamenti fisici.\r
\r
**Richiedi una corrispondenza univoca per ogni colore** è attivo per impostazione predefinita. Le assegnazioni incomplete falliscono. Disattivalo per una **tavolozza parziale**: i colori senza corrispondenza perdono le proprie uscite distinte e si uniscono alle associazioni rimaste. L’immagine resta piena, ma perde distinzioni. Se nessun colore è idoneo, anche la modalità parziale fallisce, perché non resta nulla a cui unirli.\r
\r
L’ottimizzatore massimizza prima i colori mantenuti e la copertura dell’immagine sorgente. Poi preferisce meno occorrenze ripetute, meno sequenze di materiale e meno strati fisici, prima di ridurre l’errore entro il limite. I limiti di ripetizione vengono esplorati progressivamente e la ricerca può terminare quando tutti i colori sono mantenuti. Un controllo finale di eliminazione rimuove singole sequenze che non migliorano queste priorità.\r
\r
In caso di errore nella modalità rigorosa, valuta meno colori 2D, più altezza o ripetizioni, un altro filamento adatto, un limite ΔE maggiore o un’unione parziale. Scegli il compromesso che desideri davvero, anziché alzare un limite solo per far sparire l’errore.\r
\r
Separazione e **Dithering dell’altezza** si escludono a vicenda. Attivare uno disattiva l’altro.\r
\r
## Dithering dell’altezza\r
\r
Il dithering dell’altezza può distribuire l’errore di arrotondamento su piccoli blocchi ad altezze stampabili vicine quando i dati in ingresso contengono altezze intermedie tra i confini disponibili degli strati. Queste differenze di altezza possono suggerire toni intermedi osservando da una certa distanza. Richiede la corrispondenza avanzata e agisce sulla mappa delle altezze esportata, non sull’immagine 2D sorgente.\r
\r
![Meccanismo del dithering dell’altezza in presenza di altezze frazionarie: allineamento diretto confrontato con la distribuzione dell’errore di arrotondamento tra altezze stampabili vicine.](16_height_dithering.svg)\r
\r
_Meccanismo schematico, non un risultato prima/dopo garantito. Altezze superiori diverse non significano colori aggiuntivi delle bobine._\r
\r
Con il dithering attivo, Auto-paint cerca un’altezza intermedia tra lo strato selezionato e uno strato stampabile adiacente quando la loro miscela migliora la corrispondenza prevista. Le corrispondenze esatte e quelle dei target calibrati mantengono l’altezza selezionata; le regioni senza una miscela adiacente utile restano invariate. Rigenera il modello dopo aver cambiato l’opzione, quindi confronta anteprima e risultato nello slicer. Disattivando il dithering si ripristina la normale corrispondenza discreta.\r
\r
La dimensione dei puntini segue **Larghezza effettiva della linea** nelle **Impostazioni di stampa 3D** in rapporto a **Dimensione pixel**, arrotondata a blocchi di pixel interi. È un’approssimazione, non una garanzia esatta della larghezza minima. Le regioni di bordo evitano lo stesso trattamento di dithering per ridurre gli artefatti sui confini. Controlla nello slicer la presenza di minuscole isole e spostamenti aggiuntivi.\r
\r
Dove esiste un errore di altezza frazionaria, ridistribuirlo può aiutare i toni ampi, ma rendere rumorosa la grafica piccola o più pesante la geometria. Non può aggiungere colori fuori gamma né convalidare una calibrazione non supportata. Quando crea molte regioni piccole, combinarlo con Flat Paint può essere particolarmente costoso perché quelle regioni condividono ogni strato esteso sull’intera sagoma.\r
\r
## Impostazioni ottimizzatore\r
\r
![Priorità uniforme, centrale e ai bordi sulla stessa immagine, con l’aumento del dettaglio delle transizioni rappresentato da più possibili scelte di altezza.](18_optimizer_choices.svg)\r
\r
_Pesi e scelte schematici, non colori misurati o conteggi esatti degli strati. Le regioni attenuate ricevono meno priorità; non vengono rimosse dall’immagine._\r
\r
| Controllo | Scelte ed effetto |\r
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |\r
| **Algoritmo** | **Veloce:** ricerca più limitata e veloce. **Bilanciato:** predefinito per uso generale. **Accurato:** affinamento più profondo con più punti di partenza. **Approfondito:** ricerca più ampia e costosa. **Ordine base esatto:** enumera gli ordini applicabili senza ripetizioni. |\r
| **Priorità delle regioni** | **Uniforme:** uguale peso per tutti i pixel. **Ponderata al centro:** favorisce i colori vicino al centro. **Ponderata ai bordi:** favorisce i colori vicino ai bordi dell’immagine. Modifica le priorità della corrispondenza, non il ritaglio o l’estrusione. |\r
| **Dettaglio delle transizioni** | **Compatto (80%)**, **Dettagliato (90%, predefinito)**, **Massimo (95%)** impostano i punti finali di opacità delle transizioni. Valori superiori consentono transizioni più alte e più colori intermedi stampabili, salvo convergenza anticipata e limite di altezza. |\r
| **Seme (facoltativo)** | **Automatico** usa un seme stabile ricavato dai dati in ingresso. Inserisci un intero per confrontare una ricerca deterministica diversa; svuota il campo per ripristinare Automatico. Non è un cursore della qualità. |\r
\r
Il dettaglio delle transizioni influenza le transizioni superiori dei materiali, non il requisito della base opaca. Non aumenta la risoluzione dell’immagine e non restringe la larghezza della linea. I colori di transizione aggiunti potrebbero non aiutare l’immagine attuale.\r
\r
A parità di seme, i livelli euristici superiori conservano il risultato migliore del livello inferiore. Resta comunque un’ottimizzazione di previsioni. Ordine base esatto controlla 109.600 ordini non vuoti con otto filamenti e 986.409 con nove. Le ripetizioni usano un affinamento separato, non una verifica esaustiva di ogni pila con ripetizioni.\r
\r
## Zone di transizione e affidabilità\r
\r
| Indicatore | Interpretazione |\r
| ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |\r
| **Zone di transizione** | Sequenze fisiche di materiale con inizio, fine e spessore. Le etichette «compresso» indicano uno spessore inferiore a quello ideale. |\r
| **Altezza totale / strati fisici** | Dimensioni calcolate della pila, non numero di colori dell’immagine o di bobine. |\r
| **Stato della separazione dei colori** | Colori mantenuti e uniti, capacità stampabile, peggior ΔE mantenuto e ripetizioni. Gli obiettivi uniti non sono corrispondenze riuscite oltre il limite. |\r
| **Modello di aspetto** | Indica se la previsione è supportata da simulazione, comportamento adattato, confronti locali o dati Stack Matrix. Un conteggio di misure non significa che ogni uscita sia stata misurata. |\r
| **Affidabilità della previsione: media / minima** | Solidità dei dati per i colori effettivamente associati. La media ponderata può nascondere una regione debole rivelata dal valore minimo. I conteggi distinguono misure, interpolazione, previsioni adattate e simulazione. |\r
| **Affidabilità del risultato** | Indicatori combinati di calibrazione HD, copertura e compressione. Non è una percentuale di precisione misurata e non coincide con l’affidabilità della previsione. |\r
| **Calibrazione / Copertura / Compressione** | Qualità dei dati HD, copertura dei colori sorgente da parte dei filamenti e impatto del limite di altezza. Punteggi elevati non certificano la stampa. |\r
| **Punteggio di qualità** | Confronto dell’ottimizzatore, non misura. Con una separazione incompleta non rigorosa, l’etichetta diventa **Tavolozza parziale**. |\r
| **Iterazioni / Risultato dalla cache** | Lavoro di ricerca e riutilizzo dei risultati. Più iterazioni non dimostrano un colore migliore. |\r
| **Ottimo esatto / Migliore trovato** | Confronto esaustivo applicabile senza ripetizioni, rispetto ad affinamento euristico o di pile ripetute. Nessuno dei due dimostra precisione fisica. |\r
| **Nessuna sequenza eliminabile** | Nessuna eliminazione di una singola sequenza conserva le priorità selezionate. Una diversa riorganizzazione di più sequenze potrebbe comunque essere migliore. |\r
\r
Il supporto dei dati si indebolisce allontanandosi dalle misure, quando osservazioni vicine non concordano o quando previsioni su dati riservati non riproducono i colori misurati. La corrispondenza ordinaria può includere una penalità limitata per l’incertezza. La separazione usa comunque il ΔE grezzo per l’idoneità, quindi l’incertezza non può rendere valido un colore oltre il limite.\r
\r
Dopo aver cambiato processo o altezza dello strato, esamina questo riepilogo dei dati. Un punteggio generale di calibrazione elevato non significa che ogni nuova ricetta sia supportata. Vedi [Flussi di calibrazione](calibration-workflows).\r
\r
## Suggerisci il prossimo filamento\r
\r
**Suggerisci il prossimo filamento** appare quando esiste un risultato. Cerca un colore ipotetico che potrebbe migliorare la copertura di questa immagine. Non è una proposta commerciale né una bobina già caricata nella stampante.\r
\r
| Campo o azione | Significato |\r
| -------------------- | ----------------------------------------------------------------------------------------------------------------------- |\r
| **Campione esadecimale** | Colore opaco suggerito per il filamento. |\r
| **ΔE stimato +…%** | Riduzione stimata dell’errore medio dell’immagine, considerando le miscele, se viene aggiunto. Un valore maggiore è migliore; non è affidabilità. |\r
| **HD** | Stima iniziale ricavata dal filamento esistente più vicino per distanza cromatica percettiva. Non misurata per un prodotto. |\r
| **Copertura** | Percentuale dei pixel dell’immagine con un errore stimato migliorato. |\r
| **Isolamento** | Distinzione dai filamenti attuali su una scala da 0 a 1. Un valore superiore indica una lacuna di copertura più separata. |\r
| **Aggiungi ai filamenti** | Aggiunge una riga di lavoro denominata \`Kromacut-Suggestion-…\` e ricalcola includendola. |\r
\r
Se il suggerimento è utile, trova una bobina reale, poi inserisci il suo colore e la sua calibrazione effettivi. Non stampare presumendo che la riga ipotetica sia già disponibile. I suggerimenti si azzerano quando cambiano i colori dell’immagine o il set di filamenti. Un risultato senza candidati indica che il set attuale copre già bene l’immagine secondo questo test approssimativo.\r
\r
Prosegui con: [Flat Paint](flat-paint), [Flussi di calibrazione](calibration-workflows) o [Generazione ed esportazione](generating-exporting-output).\r
`;export{i as default};
