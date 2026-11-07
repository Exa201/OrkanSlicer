const a=`---\r
title: Ajustes de imagem\r
slug: image-adjustments\r
order: 35\r
description: Todos os controlos de tom e cor, o efeito nas cores-alvo e quando incorporar a pré-visualização na imagem.\r
---\r
\r
# Ajustes de imagem\r
\r
Os ajustes mudam a imagem-alvo antes da redução de cores. Não calibram filamento, alteram HD nem garantem que uma cor mostrada seja fisicamente imprimível.\r
\r
Mova um controlo para pré-visualizar; a vista atualiza quando termina a interação. Todos começam a zero. Setas individuais repõem um controlo; a reposição do painel repõe todos.\r
\r
## Controlos de tom e cor\r
\r
![Exemplos ilustrativos negativos, neutros e positivos para os doze ajustes.](32_adjustment_controls.svg)\r
\r
_São tendências esquemáticas, não previsões calibradas. Os efeitos dependem da origem e dos outros ajustes ativos._\r
\r
### Tom\r
\r
| Controlo | Intervalo | Valores negativos | Valores positivos | Consequência para preparar a impressão |\r
| ---------- | ------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |\r
| Exposição | −3 a +3 pontos, passo 0,01 | Escurece valores RGB. | Clareia; +1 duplica canais até ao corte. | Desloca tons gerais. Este ajuste de imagem renderizada não recupera informação RAW cortada. |\r
| Contraste | −100% a +100% | Aproxima tons do cinzento médio. −100% fica cinzento médio antes de outros ajustes. | Afasta tons do cinzento, cortando em preto/branco. | Separa zonas principais, mas pode achatar sombras e luzes subtis. |\r
| Altas luzes | −100% a +100% | Escurece regiões claras. | Clareia regiões claras. | Muda tons claros que competem pela paleta; não recupera detalhe ausente. |\r
| Sombras | −100% a +100% | Escurece zonas de sombra. | Clareia zonas de sombra. | Pode revelar diferenças escuras existentes antes da redução. |\r
| Brancos | −100% a +100% | Escurece a gama mais clara. | Clareia a gama mais clara. | Separa ou combina alvos quase brancos. Não é equilíbrio de brancos. |\r
| Pretos | −100% a +100% | Escurece a gama mais escura. | Clareia a gama mais escura. | Muda alvos quase pretos; preto puro mantém-se porque os valores são multiplicados. |\r
\r
Altas luzes/Sombras cobrem gamas maiores do que Brancos/Pretos. Há sobreposição: um píxel muito escuro pode reagir a Pretos e Sombras. As gamas são avaliadas depois de Exposição, Contraste, Temperatura/Tonalidade e HSL, pelo que os controlos interagem.\r
\r
### Cor e detalhe local\r
\r
| Controlo | Intervalo | Valores negativos | Valores positivos | Consequência para preparar a impressão |\r
| ----------- | -------------- | -------------------------------------------------------- | ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |\r
| Saturação | −100% a +100% | Reduz intensidade; −100% dessatura. | Aumenta intensidade. | Muda diferenças de cor da imagem, não a gama física do filamento. |\r
| Vivacidade | −100% a +100% | Reduz mais a saturação em cores relativamente pouco saturadas. | Aumenta mais a saturação nessas cores. | Menos uniforme que Saturação, mas sem reconhecer tons de pele. Cinzento puro mantém-se. |\r
| Matiz | −180° a +180° | Roda matizes num sentido. | Roda no outro. | Recolore toda a imagem; edite uma amostra para uma só cor exata. |\r
| Temperatura | −100 a +100 | Mais frio: menos vermelho, mais azul. | Mais quente: mais vermelho, menos azul. | Ajuste aproximado de dominante, **não em kelvin**. |\r
| Tonalidade | −100 a +100 | Acrescenta verde. | Acrescenta magenta, aumentando vermelho/azul e reduzindo verde. | Corrige ou introduz dominante; não é um perfil medido de câmara. |\r
| Claridade | −100 a +100 | Suaviza contraste local. | Realça contraste e contornos locais. | Pode criar cores de bordo/halos que exigem quantização. Não recupera detalhe nem alarga linhas finas. |\r
\r
Exceto Exposição e Matiz, os controlos usam passos inteiros. O alfa não muda. A quantização tem outro comportamento: torna totalmente opacos os píxeis parcialmente transparentes.\r
\r
## Pré-visualizar e aplicar\r
\r
![A pré-visualização deriva da origem. Aplicar incorpora o aspeto e repõe os controlos; quantização, PNG e 3D usam depois esses píxeis.](33_adjustment_bake.svg)\r
\r
**Aplicar** incorpora o aspeto na imagem subjacente, repõe os controlos a zero e cria um passo no histórico. Não reduz cores, gera o modelo nem muda definições de impressão.\r
\r
A distinção importa:\r
\r
- **Quantização, Redimensionar imagem, Transferir imagem e geração 3D usam a imagem de trabalho subjacente**, não ajustes por aplicar.\r
- **Cores da imagem** descreve píxeis subjacentes, pelo que as amostras não seguem ajustes em direto.\r
- Os retoques editam a imagem subjacente; ajustes ativos reaplicam-se por cima.\r
- Remover pontilhamento lê a imagem ajustada. Incorpore primeiro para não deixar ajustes ativos sobre o resultado processado.\r
\r
A sequência fiável é **pré-visualizar ajustes → aplicar ajustes → quantizar → examinar/limpar → gerar 3D**. Recorte e redimensione antes, quando possível.\r
\r
## Repor e anular são diferentes\r
\r
Repor remove um ajuste em direto, não uma edição já incorporada. Após Aplicar, os controlos a zero são esperados porque o efeito já está na imagem. Use **Anular** para restaurar a imagem anterior.\r
\r
Aplicações repetidas atuam na imagem já editada, acumulando corte e perda de detalhe tonal. Anule primeiro ao comparar alternativas.\r
\r
Seguinte: [Redução de cores](reducing-colors).\r
`;export{a as default};
