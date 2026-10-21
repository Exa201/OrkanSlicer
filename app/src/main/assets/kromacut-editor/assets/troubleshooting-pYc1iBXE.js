const i=`---\r
title: Risoluzione dei problemi\r
slug: troubleshooting\r
order: 90\r
description: Problemi comuni e prime soluzioni da provare.\r
---\r
\r
# Risoluzione dei problemi\r
\r
Parti da qui se un risultato è errato o un controllo disattivato.\r
\r
## Un collegamento mostra pagina non trovata\r
\r
L’indirizzo può contenere un errore o riferirsi a pagina rimossa. Usa **Apri Kromacut**, **Vai alla pagina iniziale** o **Esplora documentazione** per raggiungere strumento, home o guida corrente. Un indirizzo sconosciuto non viene sostituito automaticamente con un’altra guida.\r
\r
## Genera modello 3D non aggiorna la vista\r
\r
Le impostazioni vengono applicate solo facendo clic su **Genera modello 3D**. Modificale e genera di nuovo.\r
\r
## Istruzioni cambi disattivate\r
\r
In Manuale, oltre 64 colori disattivano le istruzioni. Torna a **2D**, usa **Impostazioni di quantizzazione** e riduci a 64 o meno. Flat Paint non ha cambi manuali perché uno strato può contenere diversi filamenti.\r
\r
## Il modello è troppo alto\r
\r
Prova nell’ordine:\r
\r
1. In Auto-paint riduci **Altezza massima**, osservando compressione delle transizioni.\r
2. In Manuale verifica i colori. Molti colori creano molti spessori sovrapposti e quindi altezza.\r
3. Riduci in **2D** se non serve ogni colore come strato distinto.\r
4. In Manuale riduci uno o più spessori.\r
5. Conferma che **Altezza strato** e **Altezza primo strato** coincidano con lo slicer.\r
\r
## Il modello è troppo grande in X o Y\r
\r
Riduci **Dimensione pixel (XY)**. Ritaglia prima bordi o sfondo inutilizzati.\r
\r
## L’immagine ha puntini o isole minuscole\r
\r
Usa **Rimuovi dithering** dopo la riduzione. Se restano troppi pixel isolati, prova meno colori o un altro algoritmo.\r
\r
## Auto-paint sembra inaccurato\r
\r
Cause comuni:\r
\r
- HD stimate invece che calibrate.\r
- Filamenti che non coprono bene i colori.\r
- **Altezza massima** comprime troppo.\r
- Serve **Corrispondenza colori avanzata**.\r
- Il soggetto importante è al centro o ai bordi, ma **Priorità regione** è **Uniforme**.\r
\r
Calibra e controlla **Affidabilità del risultato** per indizi.\r
\r
Verifica separatamente **Modello di aspetto**. Un punteggio alto non garantisce accuratezza fisica. **Solo stime** o zero ricette Matrix attive significa assenza di misure applicabili. Salvare un profilo calibrato non rende evidenze valide con qualsiasi altezza o set modificato. Vedi [Flussi di calibrazione](calibration-workflows).\r
\r
## Un interruttore ne ha spento un altro\r
\r
**Mantieni separazione dei colori** e **Dithering dell’altezza** restano esclusivi perché assegnano i colori sorgente alle altezze stampabili in modi diversi. **Mesh levigata** e **Flat Paint** possono essere usati insieme; ricrea il modello dopo ogni modifica. Vedi [Flat Paint](flat-paint) e [Auto-paint](auto-paint).\r
\r
## La separazione colori non trova risultati\r
\r
Il **Limite corrispondenza unica** è un limite rigido di errore previsto. Con **Richiedi una corrispondenza unica per ogni colore**, un solo colore mancante fa fallire. Considera meno colori, altro filamento utile, più altezza o ripetizioni, oppure limite più permissivo. Disattiva la rigidità solo se accetti fusione e perdita di distinzioni. Una vecchia anteprima può restare dopo il rifiuto: non è successo delle impostazioni rifiutate.\r
\r
## Le regolazioni scompaiono in 3D o nel download\r
\r
Fai **Applica** in Regolazioni per incorporare l’aspetto prima di quantizzare, generare o scaricare. Anteprima e sorgente sono separate. Vedi [Regolazioni dell’immagine](image-adjustments).\r
\r
## Eliminare un campione non ha rimosso i pixel\r
\r
**Elimina** rimuove un’opzione e rimappa sui colori rimasti. Non è una gomma. Usa Gomma o alfa zero per ritagliare. La quantizzazione può rendere opachi pixel semitrasparenti: ricontrolla la sagoma.\r
\r
## Raccogliere una traccia diagnostica Auto-paint\r
\r
Per indagare nella versione desktop, attiva **Registra diagnostica Auto-paint** in **Impostazioni**, avvia un nuovo calcolo e usa **Apri cartella** per il \`.jsonl\`. Attiva prima dell’avvio. Generare una mesh da risultato già calcolato non registra quel calcolo. Vedi [Diagnostica Auto-paint desktop](settings-and-controls#desktop-auto-paint-diagnostics) prima di condividere.\r
\r
## La generazione 3D è lenta\r
\r
Immagini grandi, tanti colori, strati e levigatura aumentano i tempi. Prova:\r
\r
- Ritagliare l’immagine.\r
- Ridurre colori.\r
- Disattivare **Mesh levigata**.\r
- Ridurre risoluzione con **Ridimensiona immagine**. Abbassare solo Dimensione pixel rimpicciolisce la stessa mesh, non la semplifica.\r
- Semplificare Auto-paint.\r
\r
## Il file esportato ha colori inattesi\r
\r
Con 3MF verifica materiali o filamenti nello slicer. Kromacut conserva le informazioni quando possibile, ma gli slicer possono mappare estrusori diversamente.\r
\r
STL non contiene assegnazioni cromatiche. Usa le **Istruzioni di stampa** per i cambi.\r
\r
La vista Simulati mostra miscele stimate; gli slicer mostrano normalmente colori fisici. Passa a **Fisici** per confrontare e verifica bobine reali. Nessuna vista prova il colore finale.\r
\r
## Ritaglio o modifiche eccessive\r
\r
Usa **Annulla**. Ripeti è disponibile se annulli troppo.\r
\r
Successivo: [Domande frequenti](faq).\r
`;export{i as default};
