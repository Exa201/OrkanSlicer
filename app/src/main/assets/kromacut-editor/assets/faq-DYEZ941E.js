const a=`---\r
title: Perguntas frequentes\r
slug: faq\r
order: 100\r
description: Respostas breves às perguntas comuns sobre o Kromacut.\r
---\r
\r
# Perguntas frequentes\r
\r
## O que é a distância de ocultação?\r
\r
A distância de ocultação, ou **HD**, é o parâmetro de opacidade frontal, em mm, usado pela pintura automática para estimar o aspeto das camadas empilhadas. Com espessura igual à HD, o modelo básico conserva 10% da influência da cor inferior. O ponto em que a diferença deixa de ser visível depende também do filamento, cor da base e condições de observação.\r
\r
Uma HD menor significa filamento mais opaco que cobre com menos camadas. Uma HD maior significa filamento mais translúcido que precisa de mais espessura.\r
\r
A HD substitui a distância de transmissão (TD) de versões anteriores. Pode introduzir uma TD convencional de retroiluminação/litofania pelo botão de conversão da linha do filamento. O Kromacut multiplica por 0,1 para obter uma estimativa inicial de HD; a conversão não é uma nova medição física. Consulte [Fluxos de calibração](calibration-workflows).\r
\r
## Devo usar Manual ou Pintura automática?\r
\r
Use **Manual** para controlo artístico direto da ordem das cores e alturas de camada.\r
\r
Use **Pintura automática** se tiver cores reais de filamentos e HD e quiser planeamento automático.\r
\r
## Preciso de calibrar filamentos?\r
\r
Pode começar com HD estimadas. Valores publicados de TD para o filamento exato também servem de ponto de partida: introduza-os pelo botão de conversão, não diretamente no campo HD.\r
\r
A calibração costuma melhorar os resultados, sobretudo quando não há valores publicados ou o resultado continua errado. É especialmente útil quando:\r
\r
- Um filamento é translúcido.\r
- Dois filamentos são visualmente semelhantes.\r
- Pretende resultados repetíveis entre projetos.\r
\r
## Qual é a diferença entre cores de paleta e de filamento?\r
\r
As cores de paleta são cores da imagem usadas em 2D e no modo Manual.\r
\r
As cores de filamento representam materiais físicos usados pela pintura automática. Esta pode gerar cores virtuais de camada a partir do empilhamento, mas o plano exportado continua baseado em filamentos reais.\r
\r
## Porque é necessário um botão para gerar a pré-visualização 3D?\r
\r
A geração 3D pode ser dispendiosa. O Kromacut aguarda por **Gerar modelo 3D** para que alterar uma definição não inicie e cancele trabalho pesado repetidamente.\r
\r
## Posso exportar sem usar 3D?\r
\r
Use 2D para transferir a imagem de origem atual com edições aplicadas. Incorpore ajustes em direto com **Aplicar** antes de transferir. Use 3D para gerar e exportar STL ou 3MF.\r
\r
## A pré-visualização de camadas altera a exportação?\r
\r
Não. O intervalo de **Pré-visualização de camadas** só muda o que está visível. STL e 3MF incluem o modelo gerado completo.\r
\r
## Que ficheiro devo imprimir?\r
\r
Escolha **Transferir STL** para ampla compatibilidade com laminadores e trocas manuais.\r
\r
Escolha **Transferir 3MF** se o laminador suportar 3MF com cores e pretender preservar objetos de camadas coloridos.\r
\r
## Porque são aproximadas as alturas?\r
\r
Os números de camada dependem do laminador, sobretudo da primeira camada. Use os valores das **Instruções de impressão** e confirme as trocas na pré-visualização do laminador.\r
\r
## Posso partilhar as definições?\r
\r
Sim. Exporte paletas 2D como \`.kpal\` e perfis de pintura automática como \`.kfil\`. Os antigos perfis \`.kapp\` continuam a poder ser importados.\r
\r
## Um píxel menor substitui um bico menor?\r
\r
Não. O tamanho do píxel define as dimensões físicas dos píxeis. Pode tornar um traço mais estreito do que a extrusão do bico. Defina **Largura de linha efetiva** em **Definições de impressão 3D**, use a vista de detalhes imprimíveis e examine o resultado laminado. Consulte [Modo 3D](3d-mode).\r
\r
## A calibração continua válida com outra altura de camada?\r
\r
Não o assuma. HD e evidências de aparência têm papéis diferentes. Provas e matrizes são verificadas face às definições e filamentos originais. Depois de mudar a altura, examine as evidências ativas e imprima uma pequena validação. Consulte [Fluxos de calibração](calibration-workflows).\r
\r
## Porque há mais cores na pré-visualização do que bobinas?\r
\r
Camadas finas deixam o filamento inferior influenciar a cor visível. Espessuras diferentes das mesmas bobinas ordenadas criam misturas previstas diferentes. A pintura automática escolhe entre esses prefixos acessíveis, enquanto as peças exportadas continuam a usar filamentos reais. Consulte [Pintura automática](auto-paint).\r
`;export{a as default};
