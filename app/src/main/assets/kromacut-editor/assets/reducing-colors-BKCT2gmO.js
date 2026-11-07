const i=`---\r
title: Riduzione dei colori\r
slug: reducing-colors\r
order: 40\r
description: Comprendi la riduzione in due fasi, le tavolozze fisse e la differenza tra ricolorazione e trasparenza.\r
---\r
\r
# Riduzione dei colori\r
\r
La quantizzazione sostituisce molti colori sorgente con un insieme minore, semplificando le regioni usate dai flussi 3D Manuale e Auto-paint. Una tavolozza 2D contiene colori obiettivo, non un profilo filamenti né previsioni misurate di stampa.\r
\r
Ritaglia e ridimensiona prima. Se hai cambiato le [regolazioni dell’immagine](image-adjustments), fai Applica nel pannello prima di quantizzare.\r
\r
## Il processo in due fasi\r
\r
![Peso algoritmo limita la tavolozza intermedia; Numero di colori o una tavolozza fissa controlla la seconda fase.](34_quantization_pipeline.svg)\r
\r
**Peso algoritmo** e **Numero di colori** hanno ruoli diversi. K-means con peso 128 raggruppa prima in un massimo di 128 colori. Automatico con numero 16 fonde poi fino a 16. Peso non è percentuale, opacità né numero di filamenti.\r
\r
| Campo o azione | Significato |\r
| -------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |\r
| Tavolozza: Automatico | Trova colori dipendenti dall’immagine, poi ne limita il numero. |\r
| Tavolozza: integrata, fornitore o personalizzata | Mappa l’intermedio sui colori abilitati. Non tutti quelli disponibili devono apparire. |\r
| Numero di colori | Limite finale Automatico: 2–256, predefinito 16. Disattivato per tavolozza fissa. |\r
| Peso algoritmo | Budget intermedio: 2–256, predefinito 128. Più alto conserva in genere più dettaglio intermedio, non necessariamente migliore corrispondenza finale. |\r
| Algoritmo | Metodo della prima fase. Predefinito: K-means. |\r
| Applica | Elabora l’immagine sottostante e crea un passo Annulla. Cambiare un’impostazione da solo non ricolora. |\r
| Freccia di ripristino | Ripristina Automatico, 16 colori, peso 128 e K-means. Non recupera un’immagine precedente. |\r
\r
Il risultato può avere meno colori del richiesto. Aumentare il numero dopo la riduzione non recupera quelli scartati: **Annulla** prima di confrontare alternative dalla stessa sorgente.\r
\r
La quantizzazione preserva trasparenza completa ma rende **completamente opachi tutti i pixel parzialmente trasparenti**. Un bordo alfa morbido non è un bordo stampato parzialmente.\r
\r
## Scegliere un algoritmo\r
\r
Questi metodi raggruppano colori. Nessuno aggiunge dithering spaziale né garantisce che un dettaglio sopravviva alla larghezza dell’ugello.\r
\r
| Algoritmo | Cosa cambia | Confronto utile |\r
| ----------------------- | --------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |\r
| Nessuno (solo post-elaborazione) | Salta il quantizzatore iniziale; Peso disattivato. Riduzione finale o mappatura fissa avvengono comunque. | Mappare direttamente grafica pulita su tavolozza nota. Non è un’opzione universale per lasciare tutto invariato. |\r
| Posterizza | Divide i canali RGB in livelli discreti, poi applica Peso. | Grafica volutamente a gradini; i livelli disponibili cambiano per salti. |\r
| Median-cut | Divide la distribuzione in gruppi rappresentati da medie. | Confrontare quando un altro metodo perde un gruppo tonale importante. |\r
| K-means | Trova gruppi pesati per numero di pixel con inizializzazione casuale. | Partenza per foto e grafica mista. Esecuzioni uguali possono differire leggermente. |\r
| Wu | Usa statistiche cromatiche per divisioni con minore variazione interna. | Confrontare su gradienti e fotografie. |\r
| Octree | Raggruppa tramite suddivisioni RGB e unisce per rispettare il budget. | Confrontare su grafica con molte regioni distinte. |\r
\r
Non esiste un algoritmo universalmente migliore. Controlla soggetto, scritte piccole e accenti importanti alla dimensione fisica prevista.\r
\r
## Tavolozze fisse e dei fornitori\r
\r
Una tavolozza fissa offre solo i colori scelti. Dopo l’eventuale prima riduzione, ogni pixel non trasparente va al colore disponibile più vicino in Lab. È corrispondenza d’immagine, non simulazione ottica dei filamenti.\r
\r
Le **Tavolozze dei fornitori** sono riferimenti non ufficiali di nomi e colori esadecimali dichiarati. Non garantiscono disponibilità attuale né colore stampato. Per esempio Bambu usa la [tabella esadecimale dei filamenti Bambu Lab](https://store.bblcdn.com/s7/default/1084369ef84345bbaa5d704a492954e0/Bambu_PLA_Basic_Hex_Code.pdf). Kromacut non è affiliato né approvato dai produttori.\r
\r
Le tavolozze integrate e dei fornitori sono di sola lettura. Clonale per personalizzare. Usa separatamente la [calibrazione dei filamenti](calibration-theory) per il comportamento reale di bobine e strati.\r
\r
## Tavolozze personalizzate\r
\r
| Controllo | Cosa fare |\r
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |\r
| Crea tavolozza | Assegna nome e almeno un colore valido abilitato. Viene selezionata. |\r
| Modifica tavolozza selezionata | Cambia la tavolozza, non i pixel correnti. Applica poi quantizzazione. |\r
| Aggiungi colore | Aggiunge selettore, esadecimale e nome facoltativo. Validi \`#RGB\` o \`#RRGGBB\`; righe non valide escluse al salvataggio. |\r
| Nome facoltativo | Etichetta il colore, per esempio con la bobina. Non cambia la corrispondenza. |\r
| Occhio | Disabilita senza cancellare. Deve rimanere almeno un colore valido attivo. |\r
| Rimuovi riga | Cancella la riga; non puoi rimuovere l’ultima. |\r
| Clona | Copia una tavolozza diversa da Automatico in una modificabile, preservando nomi e disabilitazioni. |\r
| Importa | Legge \`.kpal\` e segnala importati, sovrascritti, duplicati o rinominati. Seleziona la prima importata quando applicabile. |\r
| Esporta | Salva la personalizzata in \`.kpal\`, con nomi e disabilitati. Clona prima le integrate per una copia modificabile. |\r
| Elimina tavolozza selezionata | Rimuove il salvataggio e torna a Automatico. Non cancella pixel. |\r
| Salva / Annulla | Conferma o scarta la bozza. |\r
\r
**Le mie bobine (5/8)** significa cinque colori abilitati su otto. Partecipano solo gli abilitati. Tavolozze e selezione si salvano localmente: esporta copie prima di cancellare dati. Sono separate dai [profili filamenti](settings-and-controls#filament-profile-files).\r
\r
## Colori dell’immagine\r
\r
Descrive la sorgente, non regolazioni live non incorporate. Il contatore esclude trasparenza completa. Le descrizioni mostrano esadecimale, alfa e pixel. Alfa diversi possono creare voci separate con stesso RGB. Immagini molto colorate hanno una lista limitata, non ogni colore fotografico.\r
\r
Fai clic su un campione per **Modifica colore**. Usa RGBA o esadecimale, poi Applica. Sei cifre cambiano RGB mantenendo l’alfa corrente; otto includono alfa esplicito. Usa il controllo trasparenza o un suffisso alfa quando l’opacità conta.\r
\r
![Sostituzione opaca ricolora ogni corrispondenza esatta, alfa zero rimuove pixel ed Elimina rimappa anziché tagliare fori.](35_swatch_operations.svg)\r
\r
| Azione | Cosa cambia |\r
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |\r
| Applica colore opaco | Sostituisce tutte le corrispondenze RGB/alfa esatte, incluse regioni scollegate. |\r
| Applica alfa trasparente | Rimuove corrispondenze dall’immagine e sagoma. Scompaiono anche pixel uguali nel soggetto. |\r
| Applica al campione trasparente | Sostituisce tutti i pixel trasparenti, potendo creare sfondo o riempire fori. |\r
| Elimina | Riquantizza con tavolozza restante. **Non** cancella pixel. L’algoritmo iniziale continua, quindi altri colori possono cambiare. |\r
| Chiudi / Esc | Scarta la modifica non confermata. |\r
\r
Scegli **Nessuno (solo post-elaborazione)** prima di Elimina per mappatura diretta. Usa [Riempimento o Gomma](loading-images#touch-up-pixels) per modifiche locali. Annulla se cambia più del previsto.\r
\r
Le istruzioni manuali sono disattivate oltre 64 colori non trasparenti. Una tavolozza minore semplifica anche sotto tale limite, ma meno obiettivi non significa automaticamente meno cambi Auto-paint.\r
\r
Successivo: [Rimozione dithering e pulizia](dedithering-cleanup).\r
`;export{i as default};
