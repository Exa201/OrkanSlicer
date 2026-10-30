const a=`---\r
title: Visão geral\r
slug: overview\r
order: 10\r
description: O que faz o Kromacut e como se articulam os principais fluxos de trabalho.\r
---\r
\r
# Visão geral\r
\r
O Kromacut transforma uma imagem plana numa impressão 3D com camadas de cor empilhadas. A ideia central é simples: as cores da imagem tornam-se camadas físicas, e a ordem e altura dessas camadas definem o plano de impressão.\r
\r
Use o Kromacut para uma impressão ao estilo HueForge, um relevo de litofania a cores ou uma peça decorativa em camadas cuja imagem final é criada pelas trocas de filamento.\r
\r
## Fluxo principal\r
\r
A maioria dos projetos segue o mesmo percurso:\r
\r
1. [Carregue ou importe uma imagem](loading-images).\r
2. [Reduza as cores](reducing-colors) até obter uma paleta imprimível na pré-visualização.\r
3. [Remova o pontilhamento ou limpe](dedithering-cleanup) píxeis isolados se a imagem tiver ruído.\r
4. Passe ao [modo 3D](3d-mode) e escolha Manual ou Pintura automática.\r
5. [Gere e exporte](generating-exporting-output) um ficheiro STL ou 3MF e siga as instruções de impressão.\r
\r
## Duas formas de pintar\r
\r
O Kromacut tem dois fluxos de impressão no modo 3D.\r
\r
| Fluxo | Quando utilizar | O que controla |\r
| ---------- | ------------------------------------------------------ | --------------------------------------------------------------------------- |\r
| Manual | Pretende controlar diretamente cada cor da imagem. | Ordem das cores, espessuras por cor, definições de impressão e trocas. |\r
| Pintura automática | Pretende que o Kromacut planeie o empilhamento físico. | Cores dos filamentos, distâncias de ocultação, altura máxima e opções do otimizador. |\r
\r
O modo Manual parte das cores no painel **Cores da imagem**. A pintura automática parte dos filamentos reais e das suas **distâncias de ocultação (HD)**, gerando depois camadas imprimíveis para a imagem.\r
\r
## O que vê na aplicação\r
\r
Use 2D para preparar a imagem e 3D para planear as camadas físicas. Os guias seguintes seguem essa distinção.\r
\r
## Guias ilustrados\r
\r
Comece pela tarefa que pretende realizar. Cada guia explica os controlos, as interações e as consequências para a impressão física. Os diagramas são exemplos esquemáticos, não previsões de cor calibradas. Clique numa ilustração ou ative-a pelo teclado para a abrir em tamanho completo.\r
\r
| Tarefa | Guia |\r
| ------------------------------------------------------------------ | -------------------------------------------------------------- |\r
| Definir dimensões, alturas de camada e ordem manual das cores | [Modo 3D](3d-mode) |\r
| Escolher filamentos, otimizar misturas e examinar detalhes imprimíveis | [Pintura automática](auto-paint) |\r
| Criar uma placa plana multimaterial com a face para cima ou para baixo | [Pintura plana](flat-paint) |\r
| Medir HD, comparar provas de paleta ou fotografar uma matriz | [Fluxos de calibração](calibration-workflows) |\r
| Preparar a silhueta e retocar píxeis | [Carregar imagens](loading-images) |\r
| Ajustar tons e cores e incorporar o resultado | [Ajustes de imagem](image-adjustments) |\r
| Reduzir cores e gerir paletas | [Redução de cores](reducing-colors) |\r
| Remover pontos sem confundir limpeza 2D com pontilhamento de altura | [Remoção de pontilhamento e limpeza](dedithering-cleanup) |\r
| Verificar o empilhamento final e transferi-lo para um laminador | [Geração e exportação](generating-exporting-output) |\r
\r
## Disposição da área de trabalho\r
\r
A área de trabalho tem três zonas principais:\r
\r
- O cabeçalho contém documentação, controlos de tema e ligações da comunidade.\r
- O painel esquerdo contém os controlos do modo atual.\r
    - Em **2D**, mostra ajustes, remoção de pontilhamento, quantização, paletas personalizadas e cores detetadas.\r
    - Em **3D**, mostra definições de impressão, controlos manuais, de pintura automática e instruções de impressão.\r
- A pré-visualização principal mostra a imagem 2D ou o modelo 3D.\r
\r
> Sugestão: as definições 3D não regeneram o modelo automaticamente. Depois de alterar definições de impressão, espessuras manuais ou opções de pintura automática, clique em **Gerar modelo 3D**.\r
\r
## Um bom primeiro projeto\r
\r
Comece com uma imagem de alto contraste, um motivo claro e pouco detalhe no fundo. Reduza-a a 4–16 cores e use Manual se já conhecer a ordem das camadas, ou Pintura automática se tiver distâncias de ocultação calibradas.\r
\r
---\r
\r
Seguinte: [Início rápido](quick-start).\r
`;export{a as default};
