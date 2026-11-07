const a=`---\r
title: Início rápido\r
slug: quick-start\r
order: 20\r
description: Uma primeira utilização prática, da imagem à exportação.\r
---\r
\r
# Início rápido\r
\r
Este guia percorre um projeto habitual, do carregamento da imagem à exportação.\r
\r
## Carregar ou importar uma imagem\r
\r
Use o botão de carregamento na barra da pré-visualização ou arraste uma imagem para a vista 2D.\r
\r
Depois, use a roda do rato para ampliar e arraste a pré-visualização para a deslocar. Se houver transparência, o botão de xadrez ajuda a distinguir as zonas transparentes.\r
\r
## Ajustar a imagem\r
\r
Em **2D**, use **Ajustes** antes de reduzir cores. Exposição, contraste, altas luzes, sombras, brancos, pretos, saturação, vivacidade, matiz, temperatura, tonalidade e claridade podem alterar as cores encontradas pelas ferramentas de paleta.\r
\r
Clique em **Aplicar** no painel Ajustes para incorporar os ajustes atuais na imagem antes de reduzir cores, gerar geometria 3D ou transferir. Os ajustes em direto são apenas uma pré-visualização, não uma atualização da imagem de origem. Consulte [Ajustes de imagem](image-adjustments) para exemplos antes/depois e comportamento de reposição.\r
\r
## Redimensionar se necessário\r
\r
Se a imagem for muito maior do que o detalhe a imprimir, use **Redimensionar imagem** para a reduzir por percentagem antes de reduzir cores. Isto diminui as dimensões reais em píxeis, podendo acelerar a geração 3D e facilitar o dimensionamento físico.\r
\r
## Reduzir cores\r
\r
Em **Definições de quantização**:\r
\r
1. Deixe **Paleta** em **Auto**, salvo se já tiver uma paleta específica em mente.\r
2. Comece com **Número de cores** em **16**. Diminua para menos regiões de origem ou aumente se faltar detalhe. Na pintura automática, este número não corresponde a bobinas nem a trocas.\r
3. Deixe **Algoritmo** na opção predefinida **K-means**. É o ponto de partida recomendado para a maioria das imagens.\r
4. Clique em **Aplicar**.\r
\r
Examine o resultado em **Cores da imagem**. Clique numa amostra para editar ou eliminar da paleta. Eliminar remapeia os píxeis para cores restantes; use Borracha ou alfa zero (totalmente transparente) para retirar píxeis da silhueta.\r
\r
## Remover pontilhamento ou limpar\r
\r
Se a redução deixar pontos isolados, use **Remover pontilhamento** como passagem de limpeza. É particularmente útil porque píxeis soltos na imagem 2D podem tornar-se pequenos elementos geométricos individuais em 3D.\r
\r
Comece com **Peso** e **Passagens** predefinidos e clique em **Aplicar**. Aumente as passagens apenas se uma deixar demasiados píxeis isolados.\r
\r
## Ativar o modo 3D\r
\r
Clique em **3D**. Defina primeiro os parâmetros básicos:\r
\r
- **Tamanho do píxel (XY)** controla a largura e profundidade físicas de cada píxel.\r
- **Altura de camada** deve corresponder à que pretende usar no laminador.\r
- **Altura da primeira camada** deve corresponder à definição do laminador.\r
- **Malha suavizada** pode suavizar fronteiras ligadas de cor para uma geometria mais lisa.\r
\r
## Escolher Manual ou Pintura automática\r
\r
Use **Manual** para controlar diretamente as cores reduzidas. Este modo usa as amostras de **Cores da imagem**: arraste-as para a ordem de impressão desejada e use cada controlo deslizante para decidir a espessura contribuída. É uma boa primeira opção se já souber a ordem ou trabalhar com uma paleta pequena e simples.\r
\r
Use **Pintura automática** para o Kromacut planear o empilhamento físico. Parte dos filamentos reais em vez das amostras reduzidas e usa a cor e a **Distância de ocultação (HD)** de cada um — a profundidade a que oculta o que está por baixo — para estimar o aspeto das camadas empilhadas.\r
\r
Numa primeira utilização:\r
\r
1. Adicione os filamentos que realmente pretende usar.\r
2. Defina a cor de cada um com a maior exatidão possível.\r
3. Defina a **HD**. A estimativa da varinha serve para experimentar e pode converter uma TD convencional (≈10 vezes a HD); distâncias calibradas costumam dar melhores resultados.\r
4. Deixe inicialmente **Altura máxima** em **Auto**.\r
5. Ative **Correspondência de cor melhorada** se o primeiro resultado falhar cores importantes ou quiser procurar uma ordem melhor de filamentos.\r
\r
Após o cálculo, verifique zonas de transição e detalhes de confiança antes de exportar. Baixa confiança costuma indicar falta de uma cor útil, necessidade de calibrar HD ou uma altura máxima demasiado restritiva.\r
\r
## Gerar e exportar\r
\r
Clique em **Gerar modelo 3D**. Quando aparecer, use **Pré-visualização de camadas** para examinar a construção de baixo para cima.\r
\r
Abra o menu de transferência e escolha **Transferir STL** ou **Transferir 3MF**. Copie depois as **Instruções de impressão** para ter a cor inicial, camadas de troca e definições recomendadas do laminador.\r
\r
Seguinte: [Modo 3D](3d-mode), [Pintura automática](auto-paint) ou [Geração e exportação](generating-exporting-output#before-you-export).\r
`;export{a as default};
