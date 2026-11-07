const a=`---\r
title: Controles de pintura automática\r
slug: auto-paint\r
order: 62\r
description: Filamentos, detalles imprimibles, correspondencia de colores, límites de altura y confianza.\r
---\r
\r
# Controles de pintura automática\r
\r
La pintura automática predice capas finas de filamento superpuestas y elige alturas imprimibles para la imagen preparada. Varios colores visibles pueden proceder de distintos grosores de un mismo filamento físico. Veinte colores de imagen no necesitan necesariamente veinte bobinas.\r
\r
Define [tamaño físico, alturas de capa y ancho efectivo](3d-mode#3d-print-settings), añade filamentos que realmente puedas cargar, espera al cálculo y pulsa **Generar modelo 3D**. Las entradas recalculan automáticamente; la geometría mostrada solo cambia al generar.\r
\r
## Datos de filamentos\r
\r
| Control | Qué introducir o hacer | Efecto |\r
| ---------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------ |\r
| **Añadir filamento** | Crea una fila nueva gris neutro. | Añade un material disponible, no una ranura física de impresora. |\r
| **Muestra / Hex** | Elige el color opaco o escribe hexadecimal. | Cambia mezclas y color exportado. Introduce el color real de la bobina, no el resultado deseado. |\r
| **Nombre** | Da una etiqueta útil a la bobina. | La identifica. Vacío recupera una etiqueta automática basada en el color. |\r
| **HD** | Distancia de ocultación frontal, de 0,01 a 2 mm. | Una HD menor oculta antes los colores inferiores. Una mayor requiere grosor y transmite más a igual espesor. |\r
| **Convertir desde TD** | Introduce una distancia convencional de litofanía/retroiluminación y pulsa **Convertir**. | Convierte aproximadamente TD × 0,1, redondeada a 0,01 mm y limitada al intervalo HD. No conviertas otra vez una HD. |\r
| **Varita** | Estima la HD a partir del color. | Suposición inicial, no medición. |\r
| **Insignia de estado** | Revisa **Estimación** o la confianza calibrada; pasa el puntero para ver canales RGB. | Describe evidencia HD de esa fila, no precisión de toda la imagen. |\r
| **Papelera** | Quita la fila. | El filamento deja de estar disponible. Los perfiles no cambian hasta guardarlos. |\r
| **Calibrar** | Abre Distancia de ocultación, Prueba de paleta o Matriz de apilamientos. | Registra evidencia física con [Flujos de calibración](calibration-workflows). |\r
\r
Confirmar una HD, convertir TD o usar la varita borra la calibración HD de esa fila. Cambiar el color de un filamento calibrado desactiva su calibración y usa una estimación derivada del color. Volver al color medido puede reactivar la calibración conservada si no se borró o sustituyó entretanto. Cambiar un nombre no es medir.\r
\r
El comportamiento medido por canal afecta tanto al color previsto como al grosor de transición. Si cambia material o proceso, no supongas que la vista anterior sigue verificada.\r
\r
## Perfiles de filamentos\r
\r
Cargar un perfil sustituye el conjunto de trabajo. **Cambios sin guardar** significa que el conjunto actual difiere del perfil elegido.\r
\r
| Acción | Resultado |\r
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |\r
| **Guardar cambios en el perfil actual** | Sobrescribe el perfil editable con el conjunto actual. Desactivado si no hay cambios, perfil seleccionado o si es una plantilla. |\r
| **Guardar como nuevo perfil** | Escribe un nombre no vacío para guardar por separado, incluidas plantillas modificadas. |\r
| **Renombrar perfil seleccionado** | Cambia su etiqueta sin cambiar filamentos. No disponible para plantillas ni sin selección. |\r
| **Importar perfil desde archivo** | Lee \`.kfil\`, \`.kapp\` antiguo, JSON o CSV/TSV de bobinas HueForge compatibles. |\r
| **Exportar filamentos actuales como .kfil** | Exporta el conjunto actual. Escritorio abre Guardar como; navegador usa su descarga. Cancelar no cambia calibración. |\r
| **Eliminar perfil seleccionado** | Quita el perfil guardado. Exporta antes una copia si la necesitas. Las plantillas no se eliminan. |\r
\r
La evidencia de apariencia pertenece a una configuración exacta. Mientras el perfil seleccionado tenga cambios sin guardar, su evidencia guardada no se pasa a pintura automática. Exportarlos crea otro perfil nombrado sin evidencia incompatible del conjunto antiguo. La calibración HD activa por filamento es distinta de las pruebas y matrices del perfil. Consulta [Compatibilidad de perfiles e importación](settings-and-controls#filament-profile-files).\r
\r
### Plantillas\r
\r
Las plantillas de proveedores incluyen colores anunciados, nombres, marcas y HD estimadas. Son de solo lectura. Quita colores que no tengas, calibra y **Guarda como nuevo perfil**. Las referencias no garantizan un lote o condición de observación. Son plantillas no oficiales, no avales del proveedor.\r
\r
## Correspondencia estándar\r
\r
Con **Correspondencia de color mejorada desactivada**, Kromacut ordena los filamentos de oscuro a claro por luminancia y calcula transiciones. No sigue el orden de adición. El mapeo imagen-altura también usa luminancia: normaliza el brillo entre los píxeles no transparentes más oscuro y más claro, coloca ese intervalo entre la superficie de fundación y el tope y lo ajusta a capas imprimibles. Dos matices distintos de igual brillo pueden recibir la misma altura, independientemente de su matiz. Repeticiones, separación, tramado de altura y optimizador están inactivos.\r
\r
Usa esta base sencilla para relieves guiados por brillo. La correspondencia mejorada considera el color al elegir secuencia de materiales y asignaciones imprimibles, por lo que es adecuada cuando importan las diferencias de matiz.\r
\r
## Altura máxima\r
\r
Vacía **Altura máxima** o pulsa **Auto** para usar la altura calculada alineada a capas. Admite de 0,5 a 20 mm. Es un techo, no un objetivo; el resultado puede ser más corto.\r
\r
Un límite entre fronteras válidas se redondea hacia abajo. Si las transiciones normales son demasiado altas, Kromacut las comprime y muestra la altura automática para comparar.\r
\r
![Los apilamientos automáticos y limitados conservan la fundación opaca mientras las transiciones superiores se acortan.](15_transition_height.svg)\r
\r
_Esquema. Las bandas identifican secuencias de material, no apariencia mezclada prevista._\r
\r
La fundación debe alcanzar aproximadamente un 95 % de opacidad en cada canal RGB modelado. Un límite insuficiente rechaza el apilamiento en vez de tratar un respaldo translúcido como opaco. Aumenta el límite o usa un filamento apto para fundación con HD menor.\r
\r
Comprimir puede quitar colores intermedios útiles. No es escalar uniformemente una imagen idéntica. En **Pintura plana**, el soporte transparente es geometría adicional y su primera capa difiere del relieve. Comprueba dimensiones completas de **Modelo** y grosor del laminador; Altura máxima no incluye el soporte de la placa.\r
\r
## Detalles imprimibles\r
\r
Define [**Ancho de línea efectivo** en **Ajustes de impresión 3D**](3d-mode#effective-line-width) al ancho previsto por el laminador, no diámetro de boquilla ni Tamaño de píxel. Los avisos y la limpieza opcional usan el mismo ajuste; sus controles siguen aquí.\r
\r
Piensa en una **franja morada de dos píxeles** sobre fondo azul. A **0,10 mm/píxel**, mide **0,20 mm**. Si la extrusión prevista mide **0,40 mm**, la franja es más estrecha. Kromacut puede marcarla **en riesgo**, pero la conserva incluso con limpieza. Una región visible fina puede pertenecer a una capa inferior mucho más ancha, y las trayectorias del laminador pueden conservar detalles señalados por este análisis solo de imagen.\r
\r
![La franja morada de dos píxeles es más estrecha que la extrusión prevista. Ámbar es un aviso, no filamento. La limpieza conserva la franja y solo sustituye una mota rosa encerrada por azul.](13_printable_detail.svg)\r
\r
_Es una estimación de anchura, no una trayectoria exacta. Las barras comparan anchuras; los cuadrados muestran la imagen de ejemplo._\r
\r
**Omitir motas de color aisladas** ofrece dos opciones:\r
\r
- **Desactivado:** conserva todos los píxeles originales en la entrada, incluidas todas las regiones señaladas.\r
- **Activado:** antes de la correspondencia y generación, sustituye solo colores usados exclusivamente en motas diminutas, compactas y encerradas por el color más ancho circundante. Toda la mota debe ser menor que el ancho efectivo y tener un único color circundante inequívoco con una región más amplia. Si ese color aparece también en una línea o región grande en cualquier parte, se conserva en todas partes.\r
\r
Se conservan líneas finas, conexiones diagonales, ramas unidas a regiones mayores, detalles del borde y motas junto a transparencia o varios colores. La limpieza nunca convierte un píxel en agujero ni edita la imagen 2D. Es deliberadamente prudente y puede dejar motas indeseadas; usa la [limpieza 2D](dedithering-cleanup) para ediciones más amplias.\r
\r
Usa **Abrir vista previa** para revisar:\r
\r
- **En riesgo:** ámbar señala regiones finas junto a colores más amplios; rosa, regiones finas sin vecino mayor. Los demás píxeles se atenúan. Son avisos, no predicciones de imposibilidad de impresión.\r
- **Resultado:** los píxeles que recibe pintura automática tras la limpieza opcional. No es vista del laminador ni garantía de imprimibilidad.\r
- **Señalados / aptos / omitidos:** fracción de avisos, píxeles que cumplen las reglas y número realmente reemplazado. **Píxeles señalados conservados** cuenta explícitamente los avisos que no cambian la imagen.\r
\r
Al omitir colores aptos, la imagen limpia se usa para contar objetivos y planificar el apilamiento. Quitar un color completo puede cambiar elecciones en otros lugares, así que revisa el resultado regenerado. Un porcentaje de aviso no nulo con **0 píxeles omitidos** significa que se ha conservado todo el detalle.\r
\r
Tras cambiar la opción, deja terminar el cálculo y pulsa otra vez **Generar modelo 3D**. El análisis mide regiones conectadas de colores originales, no capas físicas, paredes, relleno ni extrusión de ancho variable. Revisa siempre las trayectorias. Si realmente pierden detalle, aumenta el tamaño XY, engrosa el detalle en 2D o elige un ancho menor compatible con impresora y laminador.\r
\r
## Correspondencia de color mejorada\r
\r
Busca secuencias de materiales para la paleta 2D actual. Puede omitir filamentos sin cobertura útil. Ocho bobinas disponibles no tienen por qué producir ocho secuencias.\r
\r
El optimizador no reduce otra vez en secreto la paleta preparada. Más colores originales requieren más trabajo. Prepara la imagen en [Reducir colores](reducing-colors) y valora después la apariencia alcanzable en 3D.\r
\r
Desactivar la mejora también desactiva separación y tramado de altura. Los cálculos nuevos cancelan los anteriores; el progreso es aproximado. Un fallo no recurre a un apilamiento manual sin relación. Un modelo anterior puede seguir visible hasta generar un resultado nuevo válido.\r
\r
### Límite total de repeticiones\r
\r
Elige **Desactivado**, o hasta **2, 4, 6, 8 o 12 apariciones adicionales** para todo el apilamiento. No es un límite por filamento ni un número exacto de cambios. Negro → amarillo → negro usa una aparición adicional de negro. Volver a un material sobre otro sustrato crea otra posible ruta de mezcla.\r
\r
![Presupuesto compartido de repeticiones y asignaciones distintas comparados con la fusión de colores descartados.](14_repeats_separation.svg)\r
\r
_Secuencias y asignaciones conceptuales, no predicciones de materiales._\r
\r
Más repeticiones permiten una búsqueda más amplia y posiblemente más cambios al imprimir. El presupuesto es un techo. Las secuencias innecesarias pueden omitirse.\r
\r
### Conservar separación de colores\r
\r
La correspondencia normal puede asignar colores distintos al mismo resultado. Activa **Conservar separación de colores** cuando importen esas distinciones, como letras contra fondo o tonos contiguos de una cara.\r
\r
El **Límite de correspondencia única (ΔE)** es la diferencia máxima estricta para que un color posea un resultado imprimible distinto. Intervalo: 1 a 100; defecto: 6. Valores menores exigen coincidencias más cercanas y difíciles. Valores mayores permiten más error, no mejores filamentos físicos.\r
\r
**Exigir una correspondencia única para cada color** está activado por defecto. Las asignaciones incompletas fallan. Desactívalo para una **paleta parcial**: los colores sin coincidencia pierden su salida propia y se fusionan con las asignaciones supervivientes. La imagen sigue llena, pero pierde diferencias. Si ningún color es apto, incluso el modo parcial falla porque no queda nada con lo que fusionar.\r
\r
El optimizador maximiza primero colores conservados y cobertura de la imagen. Después prefiere menos repeticiones, menos secuencias y menos capas físicas, antes que menor error dentro del límite. Las repeticiones se exploran progresivamente y la búsqueda puede detenerse cuando todos los colores se conservan. Una comprobación final elimina secuencias individuales que no mejoran esas prioridades.\r
\r
Ante un fallo estricto, considera menos colores 2D, más altura o repeticiones, otro filamento adecuado, un límite ΔE mayor o fusión parcial. Elige el compromiso deseado en vez de subir un límite solo para descartar el error.\r
\r
La separación y el **Tramado de altura** son excluyentes. Activar uno desactiva el otro.\r
\r
## Tramado de altura\r
\r
Puede distribuir el error de redondeo en bloques pequeños a alturas imprimibles vecinas cuando la entrada contiene alturas entre fronteras disponibles. Esas diferencias pueden sugerir tonos intermedios a distancia. Requiere correspondencia mejorada y actúa sobre el mapa de alturas exportado, no la imagen 2D.\r
\r
![Mecanismo con alturas fraccionarias: ajuste directo frente a distribución del error entre alturas imprimibles cercanas.](16_height_dithering.svg)\r
\r
_Mecanismo esquemático, no un antes/después garantizado. Distintos topes no significan más colores de bobina._\r
\r
Con el tramado activado, Auto-paint busca una altura intermedia entre la capa seleccionada y una capa imprimible adyacente cuando su mezcla mejora la coincidencia prevista. Las coincidencias exactas y las de objetivos calibrados conservan la altura seleccionada; las regiones sin una mezcla vecina útil no cambian. Regenera el modelo después de modificar la opción y compara la vista previa con el resultado del laminador. Desactivar el tramado restaura la asignación discreta habitual.\r
\r
El tamaño del punto sigue el **Ancho de línea efectivo** de **Ajustes de impresión 3D** respecto al **Tamaño de píxel**, redondeado a un bloque entero. Es aproximado, no una garantía exacta de anchura mínima. Los bordes evitan el mismo tratamiento para reducir artefactos. Revisa islas diminutas y desplazamientos extra en el laminador.\r
\r
Cuando hay error fraccionario, redistribuirlo puede ayudar a tonos amplios y a la vez dar ruido a gráficos pequeños o hacer la geometría más pesada. No añade gama ausente ni valida calibración sin respaldo. Si crea muchas zonas pequeñas, combinarlo con Pintura plana puede ser especialmente costoso porque comparten cada capa de superficie completa.\r
\r
## Ajustes del optimizador\r
\r
![Prioridad uniforme, central y de bordes en la misma imagen, con detalle de transición creciente como más alturas posibles.](18_optimizer_choices.svg)\r
\r
_Pesos y opciones esquemáticos, no colores medidos ni cantidades exactas de capas. Las zonas atenuadas reciben menos prioridad; no se eliminan._\r
\r
| Control | Opciones y efecto |\r
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |\r
| **Algoritmo** | **Rápido:** búsqueda menor y más rápida. **Equilibrado:** predeterminado general. **Exhaustivo:** refinamiento más profundo con varios inicios. **Profundo:** búsqueda más amplia y costosa. **Orden base exacto:** enumera órdenes aplicables sin repetición. |\r
| **Prioridad regional** | **Uniforme:** igual peso por píxel. **Centro:** favorece colores centrales. **Bordes:** favorece colores periféricos. Cambia prioridades, no recorte ni extrusión. |\r
| **Detalle de transición** | **Compacto (80 %)**, **Detallado (90 %, defecto)** y **Máximo (95 %)** fijan opacidades finales. Valores mayores permiten transiciones más altas y más colores intermedios, sujetos a convergencia anticipada y límite de altura. |\r
| **Semilla (opcional)** | **Automática** usa una semilla estable derivada de las entradas. Introduce un entero para comparar otra búsqueda determinista; vacía para recuperar automática. No es un deslizador de calidad. |\r
\r
El detalle afecta a las transiciones superiores, no a la exigencia de fundación opaca. No aumenta resolución ni reduce ancho de línea. Los colores añadidos pueden no ayudar a esta imagen.\r
\r
Con la misma semilla, los niveles heurísticos superiores conservan el mejor resultado del inferior. Sigue siendo optimización de predicciones. El orden base exacto comprueba 109 600 órdenes no vacíos con ocho filamentos y 986 409 con nueve. Las repeticiones usan refinamiento separado, no una demostración exhaustiva de todos los apilamientos repetidos.\r
\r
## Zonas de transición y confianza\r
\r
| Indicador | Interpretación |\r
| ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |\r
| **Zonas de transición** | Secuencias físicas con inicio, final y grosor. Las insignias de compresión indican menos grosor del ideal. |\r
| **Altura total / capas físicas** | Dimensiones calculadas, no número de colores de imagen o bobinas. |\r
| **Estado de separación** | Colores conservados y fusionados, capacidad, peor ΔE conservado y repeticiones. Los objetivos fusionados no son coincidencias correctas fuera del límite. |\r
| **Modelo de apariencia** | Indica si la predicción se apoya en simulación, comportamiento ajustado, comparaciones locales o matriz. Un número de medidas no significa que cada resultado se haya medido. |\r
| **Confianza de predicción: media / mínima** | Respaldo de colores realmente asignados. La media ponderada puede ocultar una región débil que revela el mínimo. Los recuentos distinguen mediciones, interpolación, ajuste y simulación. |\r
| **Confianza del resultado** | Combina calibración HD, cobertura y compresión. No es un porcentaje medido de exactitud ni equivale a confianza de predicción. |\r
| **Calibración / Cobertura / Compresión** | Calidad de evidencia HD, cobertura de colores originales e impacto del límite de altura. Valores altos no certifican la impresión. |\r
| **Puntuación de calidad** | Comparación del optimizador, no medición. En separación no estricta incompleta pasa a **Paleta parcial**. |\r
| **Iteraciones / Acierto de caché** | Trabajo de búsqueda y reutilización. Más iteraciones no prueban mejor color. |\r
| **Óptimo exacto / Mejor encontrado** | Comparación exhaustiva aplicable sin repetición frente a refinamiento heurístico o repetido. Ninguno demuestra exactitud física. |\r
| **Ninguna secuencia eliminable** | No se puede quitar una sola secuencia conservando las prioridades. Otro reordenamiento de varias podría ser mejor. |\r
\r
La evidencia se debilita lejos de mediciones, si observaciones cercanas discrepan o si las predicciones de validación fallan. La correspondencia normal puede incluir un coste acotado de incertidumbre. La separación usa ΔE bruto para admisibilidad, así que la incertidumbre no valida un color fuera del límite.\r
\r
Tras cambiar proceso o altura de capa, revisa este resumen. Una puntuación general alta no significa que toda receta nueva esté respaldada. Consulta [Flujos de calibración](calibration-workflows).\r
\r
## Sugerir el siguiente filamento\r
\r
**Sugerir el siguiente filamento** aparece cuando hay un resultado. Busca un color hipotético que mejore la cobertura. No es un anuncio de producto ni una bobina ya cargada.\r
\r
| Campo o acción | Significado |\r
| -------------------- | ----------------------------------------------------------------------------------------------------------------------- |\r
| **Muestra hexadecimal** | Color opaco sugerido. |\r
| **ΔE estimado +… %** | Reducción estimada del error medio de imagen considerando mezclas si se añade. Mayor es mejor; no es confianza. |\r
| **HD** | Estimación inicial tomada del filamento existente perceptualmente más cercano. No está medida para un producto. |\r
| **Captura** | Porcentaje de píxeles cuyo error estimado mejora. |\r
| **Aislamiento** | Distinción respecto a filamentos actuales de 0 a 1. Mayor indica un hueco de cobertura más separado. |\r
| **Añadir a filamentos** | Añade una fila de trabajo llamada \`Kromacut-Suggestion-…\` y recalcula. |\r
\r
Busca una bobina real si la sugerencia sirve y después introduce su color y calibración. No imprimas suponiendo que la fila hipotética ya está disponible. Las sugerencias se restablecen si cambian colores o filamentos. Si no hay candidato, el conjunto actual ya cubre bien la imagen según esta prueba aproximada.\r
\r
Siguiente: [Pintura plana](flat-paint), [Flujos de calibración](calibration-workflows) o [Generación y exportación](generating-exporting-output).\r
`;export{a as default};
