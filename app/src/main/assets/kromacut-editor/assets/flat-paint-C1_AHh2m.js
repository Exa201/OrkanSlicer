const a=`---\r
title: Pintura plana\r
slug: flat-paint\r
order: 64\r
description: Relevo escalonado, suportes transparentes com a face para baixo e placas expostas com a face para cima.\r
---\r
\r
# Pintura plana\r
\r
**Pintura plana** é uma disposição de pintura automática disponível com correspondência normal ou melhorada. Produz uma placa de espessura constante em vez de relevo escalonado. Cada camada cobre a área do modelo, podendo vários materiais partilhar uma camada lado a lado.\r
\r
Use um fluxo multimaterial como AMS, CFS ou troca de ferramentas, com suporte adequado no laminador. Uma troca manual a uma altura não fornece vários materiais lado a lado nessa camada.\r
\r
![Cortes comparam relevo normal, pintura plana com a face para baixo e suporte transparente, e face para cima sem suporte.](17_flat_paint_orientation.svg)\r
\r
_Cortes conceptuais. As cores identificam materiais; as etiquetas identificam a face de observação._\r
\r
O progresso conta partes da malha, não camadas impressas: várias partes coloridas podem partilhar uma camada. O indicador Modelo mostra o número de camadas físicas, incluindo o suporte transparente quando utilizado. A pré-visualização de camadas corta a placa camada a camada; ambos os controlos cortam também as regiões unidas de fundação e cor. A exportação inclui sempre o modelo completo. O botão Gerar modelo 3D regenera sempre o modelo, mesmo sem alterar as definições, e repõe todas as camadas e a vista inicial da câmara.\r
\r
Se a criação falhar, o Kromacut remove o modelo incompleto e apresenta o erro na pré-visualização. Prima Gerar modelo 3D para tentar novamente; quando a criação terminar com êxito, as dimensões do modelo e a pré-visualização das camadas voltam a aparecer.\r
\r
## Predefinição: face para baixo com suporte transparente\r
\r
Ative **Pintura plana** e deixe **Face para cima, sem camada transparente** desligada.\r
\r
1. O suporte transparente imprime primeiro e torna-se a face lisa contra a placa. Atribua o seu objeto a filamento transparente.\r
2. As colunas invertem a ordem normal dos materiais para observação por baixo. A fundação preenche por trás das colunas curtas.\r
3. Exporte **3MF** e mantenha a orientação. A imagem já está espelhada; não volte a espelhá-la no laminador.\r
4. Depois de imprimir, vire a peça para observar através do suporte.\r
\r
O texto pode parecer invertido pelo verso no laminador. Verifique a face prevista de observação. Rode a vista por baixo do modelo no Kromacut para a examinar.\r
\r
O suporte é geometria adicional que exige filamento transparente real. A transparência no ecrã não mede a clareza do filamento nem o acabamento da placa. Verifique a espessura completa no indicador **Modelo** e no laminador, não apenas na **Altura máxima**.\r
\r
## Face para cima, sem camada transparente\r
\r
Ative esta opção para retirar o suporte transparente:\r
\r
- Cada coluna mantém a ordem normal de materiais de baixo para cima.\r
- A fundação preenche por baixo das colunas curtas, alinhando cores visíveis num topo plano.\r
- Não existe objeto de suporte nem necessidade de filamento transparente.\r
- Imprima com a face para cima como exportado, sem espelhar, e observe o topo sem virar.\r
\r
Estas disposições têm geometria diferente. Volte a **Gerar modelo 3D** depois de alterar a opção. Virar uma exportação antiga não a converte entre disposições.\r
\r
## Comparar os fluxos\r
\r
| Fluxo | Face de observação | Geometria | Atribuição no laminador |\r
| ------------------- | ---------------------------- | --------------------------------------------------- | ---------------------------------- |\r
| Pintura automática normal | Topo escalonado | Colunas mais curtas terminam antes. | Sequências físicas de material por altura. |\r
| Pintura plana predefinida | Fundo através do suporte | Colunas espelhadas e invertidas com fundação atrás. | Objetos por filamento e suporte. |\r
| Pintura plana, face para cima | Topo plano exposto | Ordem normal, fundação sob colunas curtas. | Objetos por filamento; sem suporte. |\r
\r
**Malha suavizada** funciona com ambas as orientações de Pintura plana. Escolha uma intensidade e gere novamente. A suavização amacia o contorno exterior e os limites partilhados entre cores, mantendo a placa plana, as alturas das camadas e as pilhas de materiais. Pequenas ligações partilhadas fecham os contactos diagonais sem sobreposição. As instruções de impressão e o 3MF registam a intensidade usada.\r
\r
## Exportar e verificar\r
\r
Só é oferecido **3MF**. Um STL sem cores perderia a disposição significativa dos materiais, deixando uma placa. Os objetos agrupam-se por filamento real, não por cada mistura prevista.\r
\r
1. Faça corresponder alturas normal e inicial às do Kromacut.\r
2. Mantenha escala e orientação exportadas. Não acrescente outro espelhamento.\r
3. Atribua corretamente cada objeto, incluindo o transparente no suporte predefinido.\r
4. Examine camadas individuais no laminador para confirmar regiões lado a lado e placa completa.\r
5. Confirme o sentido do texto pela face prevista e verifique trocas e duração.\r
\r
As **Instruções de impressão** substituem a lista manual por orientação multimaterial específica. A **Pré-visualização de camadas** tem uma faixa simples porque cada camada pode conter vários materiais. Os limites afetam apenas a inspeção, nunca a exportação completa.\r
\r
## Compromissos de custo e detalhe\r
\r
Uma face plana não é necessariamente mais simples de imprimir. Preencher toda a área em cada camada acrescenta material e geometria face ao relevo. Camadas finas, empilhamentos altos e **Pontilhamento de altura** podem criar muitas zonas pequenas, mais deslocações e geração, exportação ou laminação mais lentas.\r
\r
O Kromacut avisa antes de gerar trabalhos grandes. **Gerar mesmo assim** aceita essa carga; não certifica a adequação da impressora. Teste uma peça pequena se o material ou acabamento não estiver validado. A pintura plana preserva a disposição ótica pretendida, mas depende de materiais calibrados e de um processo correspondente.\r
\r
Seguinte: [Geração e exportação](generating-exporting-output).\r
`;export{a as default};
