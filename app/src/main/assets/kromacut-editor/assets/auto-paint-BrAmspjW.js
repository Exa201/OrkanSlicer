const a=`---\r
title: Controlos de pintura automática\r
slug: auto-paint\r
order: 62\r
description: Filamentos, detalhes imprimíveis, correspondência de cores, limites de altura e confiança.\r
---\r
\r
# Controlos de pintura automática\r
\r
A pintura automática prevê camadas finas sobrepostas e escolhe alturas imprimíveis para a imagem preparada. Várias cores visíveis podem resultar de diferentes espessuras de um só filamento. Vinte cores de imagem não exigem necessariamente vinte bobinas.\r
\r
Defina [tamanho físico, alturas e largura efetiva](3d-mode#3d-print-settings), adicione filamentos realmente disponíveis, aguarde o cálculo e **Gere o modelo 3D**. As entradas recalculam automaticamente; a geometria só atualiza ao gerar.\r
\r
## Entradas de filamento\r
\r
| Controlo | O que introduzir ou fazer | Efeito |\r
| ---------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------ |\r
| **Adicionar filamento** | Cria uma linha cinzenta neutra. | Adiciona material disponível, não uma posição física na impressora. |\r
| **Amostra / Hex** | Escolha cor opaca ou hexadecimal. | Altera misturas e cor exportada. Introduza a cor real da bobina, não a desejada no resultado. |\r
| **Nome** | Dê um nome útil. | Identifica a bobina. Vazio regressa a nome automático pela cor. |\r
| **HD** | Distância frontal de ocultação, 0,01–2 mm. | HD menor oculta mais depressa. Maior precisa de espessura e transmite mais à mesma espessura. |\r
| **Converter de TD** | Introduza TD convencional de litofania/retroiluminação e **Converta**. | Aproximadamente TD × 0,1, arredondado a 0,01 mm e limitado à gama HD. Não reconverta uma HD. |\r
| **Varinha** | Estima HD pela cor. | Suposição inicial, não medição. |\r
| **Indicador de estado** | Examine **Estimativa** ou confiança calibrada; passe o rato para canais RGB. | Descreve evidência HD da linha, não exatidão global. |\r
| **Lixo** | Remove a linha. | O filamento deixa de estar disponível. Perfis só mudam quando guardados. |\r
| **Calibrar** | Abre HD, Prova de paleta ou Matriz. | Registe evidências físicas com [Fluxos de calibração](calibration-workflows). |\r
\r
Confirmar HD, converter TD ou usar a varinha apaga a calibração HD dessa linha. Mudar a cor calibrada desativa a calibração e usa uma estimativa pela cor. Regressar à cor medida pode reativar a calibração preservada, se não tiver sido apagada ou substituída. Mudar o nome não é medir.\r
\r
O comportamento medido dos canais afeta cor e espessura de transição. Se mudar material ou processo, não assuma que a vista antiga continua validada.\r
\r
## Perfis de filamentos\r
\r
Carregar um perfil substitui o conjunto de trabalho. **Alterações por guardar** significa que difere do selecionado.\r
\r
| Ação | Resultado |\r
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |\r
| **Guardar alterações no perfil atual** | Substitui o perfil editável. Desativado sem alterações, sem seleção ou com modelo de perfil. |\r
| **Guardar como novo perfil** | Introduza nome não vazio para guardar separadamente, incluindo modelos modificados. |\r
| **Renomear perfil selecionado** | Muda o nome, não filamentos. Indisponível para modelos ou sem seleção. |\r
| **Importar perfil de ficheiro** | Lê \`.kfil\`, antigo \`.kapp\`, JSON ou CSV/TSV HueForge suportado. |\r
| **Exportar filamentos atuais como .kfil** | Exporta o conjunto. Ambiente de trabalho abre Guardar como; navegador segue transferência. Cancelar não muda calibração. |\r
| **Eliminar perfil selecionado** | Remove o guardado. Exporte cópia antes se necessário. Modelos não se eliminam. |\r
\r
As evidências de aparência pertencem a uma configuração exata. Enquanto houver alterações por guardar, as evidências do perfil não passam à pintura automática. Exportar cria outro perfil sem evidências incompatíveis antigas. A calibração HD por filamento é separada das matrizes e provas do perfil. Consulte [Compatibilidade e importação](settings-and-controls#filament-profile-files).\r
\r
### Modelos de perfil\r
\r
Fornecem cores anunciadas, nomes, marcas e HD estimadas. São só de leitura. Retire cores que não possui, calibre e **Guarde como novo perfil**. As referências não garantem um lote ou condição de observação. São não oficiais, não aprovações de fornecedores.\r
\r
## Correspondência normal\r
\r
Com **Correspondência de cor melhorada desligada**, os filamentos ordenam-se do escuro ao claro por luminância e calculam-se transições, não pela ordem de adição. O mapeamento de alturas usa também luminância: normaliza brilho entre píxeis não transparentes mais escuro e mais claro, coloca esse intervalo entre fundação e topo e ajusta a camadas imprimíveis. Matizes diferentes com igual brilho podem receber a mesma altura independentemente da cor. Repetições, separação, pontilhamento e otimizador ficam inativos.\r
\r
Use esta base simples para relevo guiado por brilho. A correspondência melhorada considera cor na sequência e atribuição de alturas, sendo adequada quando importam diferenças de matiz.\r
\r
## Altura máxima\r
\r
Limpe **Altura máxima** ou prima **Auto** para a altura calculada alinhada às camadas. Aceita 0,5–20 mm. É um teto, não um alvo; o resultado pode ser menor.\r
\r
Um limite entre fronteiras válidas arredonda para baixo. Se as transições normais forem altas demais, são comprimidas e aparece a altura automática para comparação.\r
\r
![Empilhamentos automáticos e limitados preservam a fundação opaca enquanto encurtam transições superiores.](15_transition_height.svg)\r
\r
_Esquema. As faixas representam sequências de materiais, não misturas previstas._\r
\r
A fundação tem de atingir cerca de 95% de opacidade em cada canal RGB modelado. Um limite insuficiente rejeita em vez de tratar suporte translúcido como opaco. Aumente o limite ou use um filamento adequado à fundação com HD menor.\r
\r
A compressão pode eliminar cores intermédias úteis. Não é uma escala uniforme da mesma imagem. Em **Pintura plana**, o suporte é geometria adicional e a primeira camada difere do relevo. Verifique dimensões completas em **Modelo** e no laminador; Altura máxima não inclui a espessura do suporte.\r
\r
## Detalhe imprimível\r
\r
Defina [**Largura de linha efetiva** em **Definições de impressão 3D**](3d-mode#effective-line-width) para a extrusão prevista, não diâmetro do bico nem tamanho do píxel. Avisos e limpeza opcional usam essa definição; os controlos continuam aqui.\r
\r
Imagine uma **faixa roxa de dois píxeis** sobre azul. A **0,10 mm/píxel**, mede **0,20 mm**. Se a extrusão prevista for **0,40 mm**, é mais estreita. O Kromacut pode assinalar **em risco**, mas mantém a faixa mesmo com limpeza. Uma região fina visível pode fazer parte de uma camada inferior muito mais larga, e trajetórias do laminador podem preservar detalhe assinalado por esta análise só da imagem.\r
\r
![A faixa de dois píxeis é mais estreita que a extrusão. Âmbar é aviso, não filamento. A limpeza mantém a faixa e só substitui um pequeno ponto rosa fechado por azul.](13_printable_detail.svg)\r
\r
_Estimativa de largura, não trajetória exata. As barras comparam larguras; os quadrados mostram a imagem de exemplo._\r
\r
**Omitir pontos isolados de cor** oferece:\r
\r
- **Desligado:** mantém todos os píxeis de origem, incluindo regiões assinaladas.\r
- **Ligado:** substitui apenas cores usadas exclusivamente em pontos pequenos, compactos e fechados pela cor mais larga circundante antes de corresponder e gerar. A extensão total do ponto deve ser menor que a largura efetiva, com uma só cor circundante inequívoca numa região maior. Se a cor também existir numa linha ou zona grande em qualquer parte, mantém-se em toda a imagem.\r
\r
Linhas finas, ligações diagonais, ramos presos a zonas largas, detalhes da margem e pontos junto a transparência ou várias cores mantêm-se. Nunca se cria um buraco nem se edita a imagem 2D original. A limpeza é cautelosa e pode deixar pontos indesejados; use [limpeza 2D](dedithering-cleanup) para alterações amplas.\r
\r
Use **Abrir pré-visualização** para examinar:\r
\r
- **Em risco:** âmbar marca regiões finas junto a cores largas; rosa marca finas sem vizinho largo. Outros píxeis escurecem. São avisos, não previsão de impossibilidade.\r
- **Resultado:** píxeis recebidos após limpeza opcional. Não é vista do laminador nem garantia.\r
- **Assinalados / elegíveis / omitidos:** proporção de avisos, píxeis que cumprem regras e número realmente substituído. **Píxeis assinalados mantidos** conta avisos que não mudam a imagem.\r
\r
Quando se omitem cores elegíveis, a imagem limpa é usada para contar alvos e planear. Retirar uma cor inteira pode mudar correspondências noutros locais; examine a regeneração. Avisos não nulos com **0 píxeis omitidos** significam que todo o detalhe foi mantido.\r
\r
Após mudar a opção, aguarde e volte a **Gerar modelo 3D**. A análise mede regiões ligadas de cores de origem, não camadas físicas, paredes, enchimento nem extrusão variável. Verifique sempre trajetórias. Se aí perder detalhe, aumente XY, engrosse em 2D ou escolha extrusão mais fina suportada.\r
\r
## Correspondência de cor melhorada\r
\r
Procura sequências para a paleta 2D atual. Pode omitir filamentos sem cobertura útil. Oito bobinas não exigem oito sequências.\r
\r
O otimizador não volta a reduzir secretamente a paleta. Mais cores exigem mais trabalho. Prepare em [Redução de cores](reducing-colors) e avalie a aparência possível em 3D.\r
\r
Desligar a melhoria desliga separação e pontilhamento. Novos cálculos cancelam anteriores; progresso é aproximado. Um cálculo falhado não recorre a empilhamento manual sem relação. O modelo antigo pode permanecer até gerar um novo válido.\r
\r
### Limite total de repetições\r
\r
Escolha **Desligado** ou até **2, 4, 6, 8 ou 12 aparições extra** no empilhamento inteiro. Não é por filamento nem número exato de trocas. Preto → amarelo → preto usa uma aparição extra de preto. Voltar ao material sobre novo substrato cria outro percurso de mistura.\r
\r
![Orçamento partilhado de repetições e atribuições distintas comparados com fusão de cores abandonadas.](14_repeats_separation.svg)\r
\r
_Sequências e atribuições conceptuais, não previsões de materiais._\r
\r
Mais repetições permitem pesquisa mais ampla e possíveis trocas adicionais na impressão. O orçamento é um teto. Sequências desnecessárias podem ser omitidas.\r
\r
### Preservar separação de cores\r
\r
A correspondência normal pode mapear cores diferentes para o mesmo resultado. Ative **Preservar separação de cores** quando as distinções importam, como letras no fundo ou tons de rosto adjacentes.\r
\r
**Limite de correspondência única (ΔE)** é a diferença máxima estrita para uma cor ter um resultado distinto. Intervalo 1–100; predefinição 6. Menor exige proximidade mais difícil. Maior permite erro, não filamentos fisicamente melhores.\r
\r
**Exigir correspondência única para todas as cores** está ativo por predefinição. Atribuições incompletas falham. Desligue para **paleta parcial**: cores sem correspondência perdem resultados distintos e fundem-se nos restantes. A imagem fica preenchida mas perde distinções. Se nenhuma cor for elegível, mesmo o modo parcial falha porque nada resta para fundir.\r
\r
O otimizador maximiza primeiro cores preservadas e cobertura. Depois prefere menos aparições repetidas, sequências e camadas físicas, antes de menor erro dentro do limite. Explora repetições progressivamente e pode parar quando preserva todas. Uma verificação final remove sequências individuais que não melhoram prioridades.\r
\r
Perante falha estrita, considere menos cores, mais altura/repetições, outro filamento, limite ΔE maior ou fusão parcial. Escolha o compromisso pretendido em vez de subir limites só para ocultar o erro.\r
\r
Separação e **Pontilhamento de altura** são exclusivos. Ativar um desliga o outro.\r
\r
## Pontilhamento de altura\r
\r
Pode distribuir erro de arredondamento por blocos pequenos de alturas vizinhas quando a entrada contém alturas entre fronteiras de camada. Estas diferenças podem sugerir tons intermédios à distância. Exige correspondência melhorada e atua no mapa exportado, não na origem 2D.\r
\r
![Com alturas fracionárias, compara-se ajuste direto com distribuição do erro por alturas imprimíveis próximas.](16_height_dithering.svg)\r
\r
_Mecanismo esquemático, não antes/depois garantido. Topos diferentes não significam cores de bobina extra._\r
\r
Com o dithering ativado, a pintura automática procura uma altura intermédia entre a camada selecionada e uma camada imprimível adjacente quando a mistura melhora a correspondência prevista. As correspondências exatas e as dos alvos calibrados mantêm a altura selecionada; as regiões sem uma mistura vizinha útil permanecem inalteradas. Regenere o modelo depois de alterar a opção e compare a pré-visualização com o resultado no laminador. Desativar o dithering repõe a correspondência discreta habitual.\r
\r
O tamanho do ponto segue **Largura de linha efetiva** nas **Definições de impressão 3D** em relação ao **Tamanho do píxel**, arredondado a blocos inteiros. É aproximado, não garantia exata de largura mínima. Regiões de bordo evitam o mesmo tratamento para reduzir artefactos. Verifique ilhas pequenas e deslocações extra.\r
\r
Com erro fracionário, a redistribuição pode ajudar tons amplos mas tornar gráficos pequenos ruidosos e geometria pesada. Não acrescenta gama ausente nem valida calibração sem apoio. Muitas pequenas regiões tornam a combinação com Pintura plana especialmente dispendiosa porque partilham cada camada completa.\r
\r
## Definições do otimizador\r
\r
![Prioridades uniforme, central e periférica, com detalhe crescente representado por mais alturas possíveis.](18_optimizer_choices.svg)\r
\r
_Pesos e escolhas esquemáticos, não cores medidas nem contagens exatas. Zonas escuras recebem menos prioridade, não são removidas._\r
\r
| Controlo | Escolhas e efeito |\r
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |\r
| **Algoritmo** | **Rápido:** pesquisa menor e rápida. **Equilibrado:** predefinição geral. **Minucioso:** refinamento mais fundo com vários inícios. **Profundo:** pesquisa ampla e cara. **Ordem base exata:** enumera ordens aplicáveis sem repetição. |\r
| **Prioridade regional** | **Uniforme:** mesmo peso por píxel. **Centro:** favorece cores centrais. **Margens:** favorece cores exteriores. Altera prioridades, não recorte nem extrusão. |\r
| **Detalhe de transição** | **Compacto (80%)**, **Detalhado (90%, predefinição)** e **Máximo (95%)** definem opacidades finais. Mais permite transições altas e cores intermédias, sujeitas a convergência antecipada e limite. |\r
| **Semente (opcional)** | **Automática** deriva uma semente estável das entradas. Introduza inteiro para outra pesquisa determinística; limpe para automática. Não é um controlo de qualidade. |\r
\r
O detalhe afeta transições superiores, não a fundação opaca. Não aumenta resolução nem estreita extrusão. Cores adicionais podem não ajudar esta imagem.\r
\r
Com a mesma semente, níveis heurísticos superiores conservam o melhor inferior. Continua a ser otimização de previsões. A ordem exata verifica 109 600 ordens não vazias com oito filamentos e 986 409 com nove. Repetições usam refinamento separado, não prova exaustiva de todos os empilhamentos repetidos.\r
\r
## Zonas de transição e confiança\r
\r
| Leitura | Interpretação |\r
| ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |\r
| **Zonas de transição** | Sequências físicas com início, fim e espessura. Indicadores comprimidos significam menos espessura que a ideal. |\r
| **Altura total / camadas físicas** | Dimensões calculadas, não contagem de cores ou bobinas. |\r
| **Estado de separação** | Cores preservadas/fundidas, capacidade, pior ΔE preservado e repetições. Alvos fundidos não são correspondências bem-sucedidas acima do limite. |\r
| **Modelo de aparência** | Indica se há simulação, ajuste, comparação local ou matriz. Contagem de medições não significa que todos os resultados foram medidos. |\r
| **Confiança de previsão: média / mínima** | Força de apoio das cores mapeadas. Média ponderada pode ocultar zona fraca revelada pelo mínimo. Contagens distinguem medição, interpolação, ajuste e simulação. |\r
| **Confiança do resultado** | Combina calibração HD, cobertura e compressão. Não é percentagem medida de exatidão nem igual à confiança de previsão. |\r
| **Calibração / Cobertura / Compressão** | Qualidade HD, cobertura das cores e impacto do limite. Valores altos não certificam impressão. |\r
| **Pontuação de qualidade** | Comparação do otimizador, não medição. Em separação não estrita incompleta passa a **Paleta parcial**. |\r
| **Iterações / Acerto de cache** | Trabalho e reutilização. Mais iterações não provam melhor cor. |\r
| **Ótimo exato / Melhor encontrado** | Comparação exaustiva sem repetição versus refinamento heurístico/repetido. Nenhum prova exatidão física. |\r
| **Nenhuma sequência removível** | Eliminar uma só sequência não preserva prioridades. Outra reorganização múltipla pode ser melhor. |\r
\r
A evidência enfraquece longe de medições, quando vizinhas discordam ou previsões de validação falham. A correspondência normal pode incluir custo limitado de incerteza. Separação usa ΔE bruto para elegibilidade; incerteza não torna válida uma cor fora do limite.\r
\r
Depois de mudar processo ou altura, examine o resumo. Alta calibração geral não significa que cada nova receita tenha apoio. Consulte [Fluxos de calibração](calibration-workflows).\r
\r
## Sugerir o próximo filamento\r
\r
**Sugerir o próximo filamento** aparece após existir resultado. Procura uma cor hipotética que melhore cobertura, não um produto anunciado nem bobina carregada.\r
\r
| Campo ou ação | Significado |\r
| -------------------- | ----------------------------------------------------------------------------------------------------------------------- |\r
| **Amostra hexadecimal** | Cor opaca sugerida. |\r
| **ΔE estimado +…%** | Redução estimada de erro médio considerando misturas se adicionada. Mais é melhor; não é confiança. |\r
| **HD** | Estimativa emprestada do filamento existente mais próximo em cor percetual. Não medida para um produto. |\r
| **Abrange** | Percentagem de píxeis com erro estimado melhorado. |\r
| **Isolamento** | Distinção dos atuais entre 0 e 1. Maior indica lacuna mais separada. |\r
| **Adicionar aos filamentos** | Adiciona linha \`Kromacut-Suggestion-…\` e recalcula. |\r
\r
Encontre uma bobina real se útil e introduza cor/calibração reais. Não imprima assumindo que a linha hipotética existe. Sugestões reiniciam quando mudam cores ou filamentos. Sem candidato significa que o conjunto já cobre bem segundo este teste aproximado.\r
\r
Seguinte: [Pintura plana](flat-paint), [Fluxos de calibração](calibration-workflows) ou [Geração e exportação](generating-exporting-output).\r
`;export{a as default};
