const e=`---\r
title: Flat Paint\r
slug: flat-paint\r
order: 64\r
description: Rilievo a gradini, supporti trasparenti a faccia in giù e lastre esposte a faccia in su.\r
---\r
\r
# Flat Paint\r
\r
**Flat Paint** è una disposizione Auto-paint disponibile con corrispondenza standard o avanzata. Crea una lastra a spessore costante anziché un rilievo a gradini. Ogni strato copre l’intera impronta, eventualmente con materiali affiancati.\r
\r
Usa un sistema multimateriale come AMS, CFS o cambio utensile con slicer adatto. Un cambio manuale a una certa altezza non può fornire diversi materiali affiancati nello stesso strato.\r
\r
![Sezioni di rilievo normale, Flat Paint a faccia in giù con supporto trasparente e a faccia in su senza supporto.](17_flat_paint_orientation.svg)\r
\r
_Sezioni concettuali. I colori identificano i materiali; le etichette indicano la faccia da osservare._\r
\r
L’avanzamento conta le parti della mesh, non gli strati stampati: più parti colorate possono condividere uno strato. Il badge Modello mostra il numero di strati fisici, incluso il supporto trasparente se utilizzato. L’anteprima degli strati taglia la lastra uno strato alla volta; entrambe le maniglie tagliano anche le regioni unite di fondazione e colore. L’esportazione include sempre il modello completo. Il pulsante Crea modello 3D rigenera sempre il modello, anche senza modificare le impostazioni, e ripristina l’intero intervallo di strati e la vista iniziale della fotocamera.\r
\r
Se la creazione non riesce, Kromacut rimuove il modello incompleto e mostra l’errore nell’anteprima. Premi Crea modello 3D per riprovare; al termine di una creazione riuscita, ricompaiono le dimensioni del modello e l’anteprima degli strati.\r
\r
## Predefinito: faccia in giù con supporto trasparente\r
\r
Attiva **Flat Paint** e lascia **Faccia in su, senza strato trasparente** disattivato.\r
\r
1. Il supporto trasparente stampa per primo e forma la faccia liscia contro il piano. Assegna il suo oggetto al filamento trasparente.\r
2. Le colonne invertono l’ordine normale per essere viste da sotto. La fondazione riempie dietro le colonne corte.\r
3. Esporta **3MF** e conserva l’orientamento. L’immagine è già specchiata; non specchiarla di nuovo nello slicer.\r
4. Dopo la stampa, capovolgi per guardare attraverso il supporto.\r
\r
Il testo può sembrare invertito dal retro nello slicer. Controlla invece la faccia prevista. Ruota sotto l’anteprima Kromacut per esaminarla.\r
\r
Il supporto è geometria extra che richiede vero filamento trasparente. La trasparenza sullo schermo non misura limpidezza del filamento o finitura del piano. Controlla lo spessore completo in **Modello** e nello slicer, non solo **Altezza massima**.\r
\r
## Faccia in su, senza strato trasparente\r
\r
Attiva per rimuovere il supporto:\r
\r
- Ogni colonna mantiene l’ordine normale dal basso verso l’alto.\r
- La fondazione riempie sotto colonne corte, allineando colori visibili su un piano superiore.\r
- Nessun oggetto di supporto né necessità di filamento trasparente.\r
- Stampa a faccia in su come esportato, senza specchiare, e osserva il lato esposto senza capovolgere.\r
\r
Le geometrie sono diverse. **Genera modello 3D** di nuovo dopo il cambio. Capovolgere una vecchia esportazione non la converte.\r
\r
## Confrontare i flussi\r
\r
| Flusso | Faccia osservata | Geometria | Assegnazione nello slicer |\r
| ------------------- | ---------------------------- | --------------------------------------------------- | ---------------------------------- |\r
| Auto-paint normale | Superficie a gradini | Colonne corte terminano prima. | Sequenze fisiche per altezza. |\r
| Flat Paint predefinito | Fondo attraverso supporto | Colonne specchiate e invertite, fondazione dietro. | Oggetti per filamento più supporto. |\r
| Flat Paint a faccia in su | Superficie piana esposta | Ordine normale, fondazione sotto colonne corte. | Oggetti per filamento; niente supporto. |\r
\r
**Mesh levigata** funziona con entrambi gli orientamenti di Flat Paint. Scegli un’intensità e ricrea il modello. La levigatura ammorbidisce la sagoma esterna e i confini condivisi tra colori, mantenendo la lastra piatta, le altezze dei livelli e le pile di materiali. Piccole giunzioni condivise chiudono i contatti diagonali senza sovrapposizioni. Le istruzioni di stampa e il 3MF registrano l’intensità usata.\r
\r
## Esportare e verificare\r
\r
È disponibile solo **3MF**. STL senza colore perderebbe la disposizione utile lasciando una lastra. Gli oggetti si raggruppano per filamento reale, non per ogni miscela prevista.\r
\r
1. Allinea altezza normale e primo strato a Kromacut.\r
2. Mantieni scala e orientamento esportati. Non aggiungere specchiature.\r
3. Assegna ogni oggetto, incluso trasparente per il supporto predefinito.\r
4. Esamina singoli strati per regioni affiancate e lastra completa.\r
5. Conferma il verso del testo dalla faccia prevista e verifica cambi e durata.\r
\r
Le **Istruzioni di stampa** sostituiscono i cambi manuali con indicazioni multimateriale specifiche. **Anteprima strati** ha una traccia uniforme perché uno strato può contenere più materiali. I limiti agiscono solo sull’ispezione, mai sull’export completo.\r
\r
## Compromessi di costo e dettaglio\r
\r
Una faccia piana non implica stampa più semplice. Riempire l’impronta a ogni strato aggiunge materiale e geometria. Strati sottili, pile alte e **Dithering dell’altezza** possono creare molte piccole regioni, più spostamenti e rallentare generazione, export o slicing.\r
\r
Kromacut avvisa prima dei lavori grandi. **Genera comunque** accetta il carico, non certifica l’idoneità della stampante. Prova un pezzo piccolo se materiale o finitura non sono verificati. Flat Paint preserva la disposizione ottica prevista, ma dipende da materiali calibrati e processo corrispondente.\r
\r
Successivo: [Generazione ed esportazione](generating-exporting-output).\r
`;export{e as default};
