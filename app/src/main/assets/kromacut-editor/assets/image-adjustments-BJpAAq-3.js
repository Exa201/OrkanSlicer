const i=`---\r
title: Regolazioni dell’immagine\r
slug: image-adjustments\r
order: 35\r
description: Ogni cursore di tono e colore, il suo effetto sui colori obiettivo e quando applicare definitivamente l’anteprima all’immagine.\r
---\r
\r
# Regolazioni dell’immagine\r
\r
Le regolazioni cambiano l’immagine obiettivo prima di ridurre colori. Non calibrano filamenti, cambiano HD né garantiscono stampabilità fisica del colore.\r
\r
Muovi un cursore per l’anteprima; si aggiorna quando termini l’interazione. Tutti partono da zero. Frecce individuali ripristinano un cursore; il pannello li ripristina tutti.\r
\r
## Controlli di tono e colore\r
\r
![Esempi illustrativi negativi, neutri e positivi per tutti i dodici controlli.](32_adjustment_controls.svg)\r
\r
_Direzioni schematiche, non previsioni calibrate. Gli effetti dipendono dalla sorgente e dalle altre regolazioni._\r
\r
### Tono\r
\r
| Cursore | Intervallo | Valori negativi | Valori positivi | Conseguenza per la stampa |\r
| ---------- | ------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |\r
| Esposizione | −3 a +3 stop, passo 0,01 | Scurisce RGB. | Schiarisce; +1 raddoppia i canali fino al clipping. | Sposta i toni generali. Una regolazione dell’immagine renderizzata non recupera dati RAW tagliati. |\r
| Contrasto | −100% a +100% | Avvicina al grigio medio. −100% diventa grigio prima degli altri effetti. | Allontana dal grigio, tagliando a nero/bianco. | Separa grandi regioni ma può appiattire ombre/luci sottili. |\r
| Luci | −100% a +100% | Scurisce zone chiare. | Schiarisce zone chiare. | Cambia i toni chiari in competizione per la tavolozza; non recupera dettaglio assente. |\r
| Ombre | −100% a +100% | Scurisce zone d’ombra. | Schiarisce zone d’ombra. | Può rivelare differenze scure esistenti prima della riduzione. |\r
| Bianchi | −100% a +100% | Scurisce la gamma più luminosa. | Schiarisce la gamma più luminosa. | Separa o unisce obiettivi quasi bianchi. Non è bilanciamento del bianco. |\r
| Neri | −100% a +100% | Scurisce la gamma più scura. | Schiarisce la gamma più scura. | Cambia quasi-neri; nero puro rimane perché i valori sono scalati. |\r
\r
Luci/Ombre coprono gamme più ampie di Bianchi/Neri. Si sovrappongono: un pixel molto scuro risponde a Neri e Ombre. Le gamme sono valutate dopo Esposizione, Contrasto, Temperatura/Tinta e HSL, quindi interagiscono.\r
\r
### Colore e dettaglio locale\r
\r
| Cursore | Intervallo | Valori negativi | Valori positivi | Conseguenza per la stampa |\r
| ----------- | -------------- | -------------------------------------------------------- | ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |\r
| Saturazione | −100% a +100% | Riduce intensità; −100% desatura. | Aumenta intensità. | Cambia differenze dell’immagine, non gamut fisico. |\r
| Vividezza | −100% a +100% | Riduce più nei colori poco saturi. | Aumenta più nei colori poco saturi. | Meno uniforme di Saturazione, senza riconoscere incarnati. Grigio puro rimane. |\r
| Tonalità | −180° a +180° | Ruota tonalità in un senso. | Ruota nell’altro. | Ricolora tutto; modifica un campione per un colore esatto. |\r
| Temperatura | −100 a +100 | Più freddo: meno rosso, più blu. | Più caldo: più rosso, meno blu. | Dominante approssimata, **non kelvin**. |\r
| Tinta | −100 a +100 | Aggiunge verde. | Aggiunge magenta aumentando rosso/blu e riducendo verde. | Corregge o introduce dominante; non è un profilo fotocamera misurato. |\r
| Chiarezza | −100 a +100 | Ammorbidisce contrasto locale. | Accentua contrasto e bordi locali. | Può creare colori di bordo/aloni da quantizzare. Non recupera dettaglio né allarga linee. |\r
\r
Tranne Esposizione e Tonalità, i passi sono interi. Alfa rimane invariato. La quantizzazione invece rende opachi i pixel parzialmente trasparenti.\r
\r
## Anteprima e applicazione\r
\r
![L’anteprima deriva dalla sorgente. Applica incorpora l’aspetto e azzera cursori; quantizzazione, PNG e 3D usano poi quei pixel.](33_adjustment_bake.svg)\r
\r
**Applica** incorpora l’aspetto nell’immagine sottostante, azzera i cursori e crea un passo nello storico. Non riduce colori, genera modelli o cambia stampa.\r
\r
La distinzione conta:\r
\r
- **Quantizzazione, Ridimensiona immagine, Scarica immagine e generazione 3D usano l’immagine sottostante**, non regolazioni live non applicate.\r
- **Colori dell’immagine** descrive pixel sottostanti: i campioni non seguono l’anteprima live.\r
- I ritocchi modificano la sorgente; regolazioni attive si riapplicano sopra.\r
- Rimuovi dithering legge l’immagine regolata. Incorpora prima per non lasciare effetti attivi sul risultato elaborato.\r
\r
La sequenza affidabile è **anteprima regolazioni → applica → quantizza → controlla/pulisci → genera 3D**. Ritaglia e ridimensiona prima quando possibile.\r
\r
## Ripristina e Annulla sono diversi\r
\r
Ripristina elimina la regolazione live, non una modifica già incorporata. Dopo Applica, cursori a zero sono normali perché l’effetto è nell’immagine. Usa **Annulla** per tornare alla precedente.\r
\r
Applicazioni ripetute lavorano sull’immagine già modificata, accumulando clipping e perdita tonale. Annulla prima di confrontare alternative.\r
\r
Successivo: [Riduzione dei colori](reducing-colors).\r
`;export{i as default};
