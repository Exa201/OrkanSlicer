const i=`---\r
title: Panoramica\r
slug: overview\r
order: 10\r
description: Cosa fa Kromacut e come si collegano i principali flussi di lavoro.\r
---\r
\r
# Panoramica\r
\r
Kromacut trasforma un’immagine piatta in una stampa 3D composta da strati di colore sovrapposti. L’idea centrale è semplice: i colori diventano strati fisici, il cui ordine e spessore costituiscono il piano di stampa.\r
\r
Usa Kromacut per stampe in stile HueForge, rilievi simili a litofanie a colori o oggetti decorativi stratificati in cui i cambi filamento creano l’immagine finale.\r
\r
## Flusso principale\r
\r
La maggior parte dei progetti segue questo percorso:\r
\r
1. [Carica o importa un’immagine](loading-images).\r
2. [Riduci i colori](reducing-colors) fino a ottenere una tavolozza stampabile nell’anteprima.\r
3. [Rimuovi il dithering o pulisci](dedithering-cleanup) i pixel isolati se l’immagine è rumorosa.\r
4. Passa alla [modalità 3D](3d-mode) e scegli Manuale o Auto-paint.\r
5. [Genera ed esporta](generating-exporting-output) un file STL o 3MF e segui le istruzioni di stampa.\r
\r
## Due modi di colorare\r
\r
Kromacut offre due flussi di stampa in modalità 3D.\r
\r
| Flusso | Quando usarlo | Cosa controlli |\r
| ---------- | ------------------------------------------------------ | --------------------------------------------------------------------------- |\r
| Manuale | Vuoi controllare direttamente ogni colore dell’immagine. | Ordine dei colori, spessori per colore, impostazioni e cambi filamento. |\r
| Auto-paint | Vuoi che Kromacut pianifichi la pila fisica dei filamenti. | Colori dei filamenti, distanze di copertura, altezza massima e opzioni di ottimizzazione. |\r
\r
La modalità Manuale parte dai colori nel pannello **Colori dell’immagine**. Auto-paint parte dai filamenti reali e dalle loro **distanze di copertura (HD)**, poi genera strati stampabili per l’immagine.\r
\r
## Cosa vedi nell’app\r
\r
Usa 2D per preparare l’immagine e 3D per pianificare gli strati fisici. Le guide seguenti rispettano questa distinzione.\r
\r
## Guide illustrate\r
\r
Parti dall’attività che vuoi svolgere. Ogni guida spiega controlli, interazioni e conseguenze per la stampa fisica. I diagrammi sono esempi schematici, non previsioni cromatiche calibrate. Fai clic su un’illustrazione o attivala da tastiera per aprirla a dimensione intera.\r
\r
| Attività | Guida |\r
| ------------------------------------------------------------------ | -------------------------------------------------------------- |\r
| Impostare dimensioni, altezze degli strati e ordine manuale | [Modalità 3D](3d-mode) |\r
| Scegliere filamenti, ottimizzare miscele e verificare dettagli stampabili | [Auto-paint](auto-paint) |\r
| Creare una lastra multimateriale a faccia in su o in giù | [Flat Paint](flat-paint) |\r
| Misurare HD, confrontare Palette Proof o fotografare una Stack Matrix | [Flussi di calibrazione](calibration-workflows) |\r
| Preparare la sagoma e ritoccare i pixel | [Caricamento delle immagini](loading-images) |\r
| Regolare toni e colori, poi applicare definitivamente | [Regolazioni dell’immagine](image-adjustments) |\r
| Ridurre colori e gestire tavolozze | [Riduzione dei colori](reducing-colors) |\r
| Eliminare puntini senza confondere pulizia 2D e dithering dell’altezza | [Rimozione dithering e pulizia](dedithering-cleanup) |\r
| Verificare la pila finita e trasferirla allo slicer | [Generazione ed esportazione](generating-exporting-output) |\r
\r
## Organizzazione dell’area di lavoro\r
\r
L’area di lavoro ha tre zone principali:\r
\r
- L’intestazione contiene documentazione, temi e collegamenti alla comunità.\r
- Il pannello sinistro contiene i controlli della modalità corrente.\r
    - In **2D**, mostra regolazioni, rimozione dithering, quantizzazione, tavolozze personalizzate e colori rilevati.\r
    - In **3D**, mostra impostazioni di stampa, controlli Manuale e Auto-paint e istruzioni di stampa.\r
- L’anteprima principale mostra la tela 2D o il modello 3D.\r
\r
> Suggerimento: le impostazioni 3D non rigenerano automaticamente il modello. Dopo modifiche a stampa, spessori manuali o opzioni Auto-paint, fai clic su **Genera modello 3D**.\r
\r
## Un buon primo progetto\r
\r
Inizia con un’immagine contrastata, un soggetto chiaro e pochi dettagli di sfondo. Riducila a 4–16 colori e usa Manuale se conosci già l’ordine degli strati, oppure Auto-paint se hai distanze di copertura calibrate.\r
\r
---\r
\r
Successivo: [Guida rapida](quick-start).\r
`;export{i as default};
