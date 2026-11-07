const a=`---\r
title: Resolução de problemas\r
slug: troubleshooting\r
order: 90\r
description: Problemas comuns e o que experimentar primeiro.\r
---\r
\r
# Resolução de problemas\r
\r
Comece aqui quando um resultado parecer errado ou um controlo estiver desativado.\r
\r
## Uma ligação mostra «Página não encontrada»\r
\r
O endereço pode ter uma gralha ou apontar para uma página removida. Use **Abrir Kromacut**, **Ir para a página inicial** ou **Explorar documentação** para encontrar a ferramenta, início ou guia atual. Um endereço desconhecido de documentação não é substituído automaticamente por outro guia.\r
\r
## Gerar modelo 3D não atualiza a vista\r
\r
As definições só são aplicadas ao clicar em **Gerar modelo 3D**. Altere-as e gere novamente.\r
\r
## As instruções de troca estão desativadas\r
\r
Em Manual, imagens com mais de 64 cores desativam as instruções. Volte a **2D**, use **Definições de quantização** e reduza a 64 ou menos. A pintura plana não tem sequência manual de propósito, porque cada camada pode conter vários filamentos.\r
\r
## O modelo é demasiado alto\r
\r
Experimente por esta ordem:\r
\r
1. Na pintura automática, reduza **Altura máxima** e observe a compressão das transições.\r
2. Em Manual, verifique o número de cores. Muitas cores criam muitas fatias empilhadas e aumentam naturalmente a altura.\r
3. Reduza cores em **2D** se não precisar de cada uma como camada separada.\r
4. Em Manual, reduza uma ou mais espessuras de cor.\r
5. Confirme que **Altura de camada** e **Altura da primeira camada** correspondem ao laminador.\r
\r
## O modelo é demasiado grande em X ou Y\r
\r
Reduza **Tamanho do píxel (XY)**. Recorte primeiro margens ou fundo sem uso.\r
\r
## A imagem tem pontos ou ilhas pequenas\r
\r
Use **Remover pontilhamento** após reduzir cores. Se continuarem demasiados píxeis isolados, experimente menos cores ou outro algoritmo.\r
\r
## A pintura automática parece imprecisa\r
\r
Causas comuns:\r
\r
- HD estimadas em vez de calibradas.\r
- O conjunto de filamentos cobre mal as cores.\r
- **Altura máxima** comprime demasiado as transições.\r
- O otimizador precisa de **Correspondência de cor melhorada**.\r
- O motivo importante está no centro ou margens, mas **Prioridade regional** está em **Uniforme**.\r
\r
Calibre os filamentos e consulte **Confiança do resultado**.\r
\r
Verifique separadamente **Modelo de aparência**. Uma pontuação global alta não garante exatidão física. **Apenas estimativas** ou zero receitas de matriz ativas significa ausência de medidas aplicáveis. Guardar um perfil calibrado não torna evidências compatíveis com todas as alturas ou conjuntos alterados. Consulte [Fluxos de calibração](calibration-workflows).\r
\r
## Uma opção desligou outra\r
\r
**Preservar separação de cores** e **Pontilhamento de altura** continuam a ser exclusivos, pois atribuem as cores de origem às alturas imprimíveis de formas diferentes. **Malha suavizada** e **Pintura plana** podem ser usadas em conjunto; gere novamente depois de alterar qualquer definição. Consulte [Pintura plana](flat-paint) e [Pintura automática](auto-paint).\r
\r
## A separação de cores não encontra resultado\r
\r
O **Limite de correspondência única** é um limite estrito de erro previsto. Com **Exigir correspondência única para todas as cores**, basta uma sem correspondência para rejeitar. Considere menos cores 2D, outro filamento útil, mais altura ou repetições, ou relaxar o limite. Desative o modo estrito só se aceitar perder distinções e fundir regiões. Uma vista antiga pode permanecer após rejeição; não é geração bem-sucedida das definições rejeitadas.\r
\r
## Os ajustes desaparecem em 3D ou na transferência\r
\r
Clique em **Aplicar** nos Ajustes para incorporar o aspeto na origem antes de quantizar, gerar ou transferir. A pré-visualização e a origem são distintas. Consulte [Ajustes de imagem](image-adjustments).\r
\r
## Eliminar uma amostra não removeu os píxeis\r
\r
**Eliminar** retira uma opção de paleta e remapeia para as restantes. Não é uma borracha. Use Borracha ou alfa zero para um recorte. Quantizar pode tornar opacos píxeis parcialmente transparentes; examine novamente a silhueta.\r
\r
## Recolher um registo de diagnóstico de pintura automática\r
\r
Para investigar um resultado na aplicação de ambiente de trabalho, ative **Registar diagnósticos de pintura automática** em **Definições**, faça um novo cálculo e use **Abrir pasta** para encontrar o \`.jsonl\`. Ative antes do cálculo. Gerar uma malha de um resultado já calculado não regista esse cálculo anterior. Consulte [Diagnósticos de pintura automática no ambiente de trabalho](settings-and-controls#desktop-auto-paint-diagnostics) antes de partilhar.\r
\r
## A geração 3D é lenta\r
\r
Imagens grandes, muitas cores, muitas camadas e malha suavizada aumentam o tempo. Experimente:\r
\r
- Recortar a imagem.\r
- Reduzir cores.\r
- Desligar **Malha suavizada**.\r
- Reduzir resolução com **Redimensionar imagem**. Baixar só o tamanho do píxel torna a mesma malha menor, não mais simples.\r
- Simplificar opções de pintura automática.\r
\r
## O ficheiro exportado abre com cores inesperadas\r
\r
Em 3MF, reveja atribuições de materiais ou filamentos no laminador. O Kromacut preserva cores quando possível, mas os laminadores podem associá-las a extrusores de modo diferente.\r
\r
STL não transporta atribuições de cor. Use **Instruções de impressão** para trocas.\r
\r
A vista Simuladas mostra misturas estimadas; laminadores mostram normalmente cores físicas. Mude para **Físicas** para conferir atribuições e verifique as bobinas reais. Nenhuma vista prova a cor final impressa.\r
\r
## O recorte ou as edições foram longe de mais\r
\r
Use **Anular**. Refazer está disponível se anular demasiado.\r
\r
Seguinte: [Perguntas frequentes](faq).\r
`;export{a as default};
