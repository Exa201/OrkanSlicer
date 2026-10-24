const e=`---\r
title: Ajustes de imagen\r
slug: image-adjustments\r
order: 35\r
description: Todos los controles de tono y color, su efecto en los colores objetivo y cuándo aplicar la vista previa a la imagen.\r
---\r
\r
# Ajustes de imagen\r
\r
Los ajustes cambian la imagen objetivo antes de reducir colores. No calibran el filamento, no cambian la distancia de ocultación ni garantizan que un color mostrado sea físicamente imprimible.\r
\r
Mueve un deslizador para previsualizar su efecto; la vista se actualiza al terminar la interacción. Todos empiezan en cero. Las flechas individuales restablecen un deslizador; el restablecimiento del panel recupera todos.\r
\r
## Controles de tono y color\r
\r
![Ejemplos ilustrativos negativos, neutros y positivos para los doce ajustes.](32_adjustment_controls.svg)\r
\r
_Son tendencias esquemáticas, no predicciones de impresión calibradas. Los efectos dependen del original y de otros ajustes activos._\r
\r
### Tono\r
\r
| Deslizador | Intervalo | Valores negativos | Valores positivos | Consecuencia para preparar la impresión |\r
| ---------- | ------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |\r
| Exposición | −3 a +3 pasos, incremento 0,01 | Oscurece los valores RGB. | Los aclara; +1 duplica los canales hasta recortarlos. | Desplaza los tonos objetivo generales. Este ajuste de imagen renderizada no recupera información RAW recortada. |\r
| Contraste | −100 % a +100 % | Acerca los tonos al gris medio. −100 % produce gris medio antes de otros ajustes. | Aleja los tonos del gris medio, recortando en negro/blanco. | Separa regiones principales, pero puede aplanar sombras y luces sutiles. |\r
| Altas luces | −100 % a +100 % | Oscurece las zonas claras. | Aclara las zonas claras. | Cambia los tonos claros que compiten por la paleta; no recupera detalle ausente. |\r
| Sombras | −100 % a +100 % | Oscurece las zonas de sombra. | Aclara las zonas de sombra. | Puede revelar diferencias oscuras existentes antes de reducir. |\r
| Blancos | −100 % a +100 % | Oscurece el intervalo más claro. | Aclara el intervalo más claro. | Separa o combina objetivos cercanos al blanco. No es un ajuste de balance de blancos. |\r
| Negros | −100 % a +100 % | Oscurece el intervalo más oscuro. | Aclara el intervalo más oscuro. | Cambia objetivos cercanos al negro; el negro puro sigue negro porque los valores se multiplican. |\r
\r
Altas luces/Sombras abarcan intervalos mayores que Blancos/Negros. Se solapan: un píxel muy oscuro puede responder a Negros y Sombras. Los intervalos tonales se evalúan después de Exposición, Contraste, Temperatura/Tinte y ajustes HSL, así que los controles pueden interactuar.\r
\r
### Color y detalle local\r
\r
| Deslizador | Intervalo | Valores negativos | Valores positivos | Consecuencia para preparar la impresión |\r
| ----------- | -------------- | -------------------------------------------------------- | ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |\r
| Saturación | −100 % a +100 % | Reduce la intensidad; −100 % desatura. | Aumenta la intensidad. | Cambia diferencias de color de la imagen, no la gama física del filamento. |\r
| Intensidad | −100 % a +100 % | Reduce más la saturación en colores relativamente poco saturados. | Aumenta más la saturación en colores relativamente poco saturados. | Menos uniforme que Saturación, pero sin reconocer tonos de piel. El gris puro sigue gris. |\r
| Matiz | −180° a +180° | Gira los matices en un sentido. | Los gira en el contrario. | Recolorea toda la imagen; edita una muestra para cambiar un color exacto. |\r
| Temperatura | −100 a +100 | Más frío: menos rojo y más azul. | Más cálido: más rojo y menos azul. | Ajuste aproximado de dominante, **no en kelvin**. |\r
| Tinte | −100 a +100 | Añade verde. | Añade magenta aumentando rojo/azul y reduciendo verde. | Corrige o introduce una dominante; no es un perfil medido de cámara. |\r
| Claridad | −100 a +100 | Suaviza el contraste local. | Resalta contraste local y bordes. | Puede crear colores de borde o halos que requieren cuantización. No recupera detalle ni ensancha líneas finas. |\r
\r
Excepto Exposición y Matiz, los deslizadores usan pasos enteros. El alfa no cambia. La cuantización tiene otro comportamiento: vuelve totalmente opacos los píxeles parcialmente transparentes.\r
\r
## Vista previa frente a Aplicar\r
\r
![La vista previa parte de la imagen original. Aplicar incorpora ese aspecto y restablece controles; cuantización, exportación PNG y 3D usan después los píxeles aplicados.](33_adjustment_bake.svg)\r
\r
**Aplicar** incorpora el aspecto actual a la imagen subyacente, devuelve los deslizadores a cero y crea un paso en el historial. No reduce colores, genera el modelo ni cambia ajustes de impresión.\r
\r
La distinción importa:\r
\r
- **Cuantización, Redimensionar imagen, Descargar imagen y generación 3D usan la imagen de trabajo subyacente**, no los ajustes de vista previa sin aplicar.\r
- **Colores de la imagen** describe los píxeles subyacentes, así que sus muestras no siguen los ajustes en directo.\r
- Los retoques editan la imagen subyacente; los ajustes activos se reaplican encima.\r
- Eliminar tramado lee la imagen ajustada. Aplica primero para evitar dejar ajustes activos sobre el resultado procesado.\r
\r
La secuencia fiable es **previsualizar ajustes → aplicar ajustes → cuantizar → inspeccionar/limpiar → generar 3D**. Recorta y redimensiona antes cuando sea posible.\r
\r
## Restablecer y Deshacer son distintos\r
\r
Restablecer retira un ajuste en directo, no una edición ya aplicada. Después de Aplicar, es normal que los controles estén a cero porque su efecto ya forma parte de la imagen. Usa **Deshacer** para restaurar la imagen anterior.\r
\r
Aplicar repetidamente actúa sobre la imagen ya editada, de modo que el recorte de valores y la pérdida de detalle tonal pueden acumularse. Deshaz primero al comparar alternativas.\r
\r
Siguiente: [Reducir colores](reducing-colors).\r
`;export{e as default};
