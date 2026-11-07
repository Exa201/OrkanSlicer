const a=`---\r
title: Inicio rápido\r
slug: quick-start\r
order: 20\r
description: Un primer recorrido práctico de la imagen a la exportación.\r
---\r
\r
# Inicio rápido\r
\r
Esta guía recorre un proyecto habitual desde la carga de la imagen hasta su exportación.\r
\r
## Cargar o importar una imagen\r
\r
Usa el botón de carga de la barra de herramientas de la vista previa o arrastra una imagen a la vista 2D.\r
\r
Después de cargarla, usa la rueda del ratón para ampliar y arrastra la vista para desplazarla. Si la imagen tiene transparencia, el botón del cuadriculado ayuda a distinguir las zonas transparentes.\r
\r
## Ajustar la imagen\r
\r
En **2D**, usa **Ajustes** antes de reducir colores. Exposición, contraste, altas luces, sombras, blancos, negros, saturación, intensidad, matiz, temperatura, tinte y claridad pueden cambiar los colores que encuentran las herramientas de paleta.\r
\r
Haz clic en **Aplicar** en el panel Ajustes para incorporar los ajustes actuales a la imagen antes de reducir colores, generar geometría 3D o descargarla. Los ajustes en directo son solo una vista previa, no una imagen original actualizada. Consulta [Ajustes de imagen](image-adjustments) para ver ejemplos antes/después y el funcionamiento del restablecimiento.\r
\r
## Redimensionar si hace falta\r
\r
Si la imagen es mucho mayor que el detalle que quieres imprimir, usa **Redimensionar imagen** para reducirla por porcentaje antes de reducir colores. Esto disminuye las dimensiones reales en píxeles, lo que puede acelerar la generación 3D y facilitar la elección del tamaño físico.\r
\r
## Reducir colores\r
\r
En **Ajustes de cuantización**:\r
\r
1. Deja **Paleta** en **Auto**, salvo que ya tengas una paleta concreta en mente.\r
2. Empieza con **Número de colores** en **16**. Bájalo para tener menos regiones de color originales o súbelo si falta detalle. En pintura automática, este número no es el de bobinas ni el de cambios.\r
3. Deja **Algoritmo** en la opción predeterminada **K-means**. Es el punto de partida recomendado para la mayoría de las imágenes.\r
4. Haz clic en **Aplicar**.\r
\r
Usa el panel **Colores de la imagen** para revisar el resultado. Haz clic en una muestra para editarla o eliminarla de la paleta. Eliminar reasigna los píxeles a los colores restantes; usa la goma o pon el alfa a cero (totalmente transparente) para quitar píxeles de la silueta.\r
\r
## Eliminar tramado o limpiar\r
\r
Si la reducción deja motas aisladas, usa **Eliminar tramado** como pasada de limpieza. Es especialmente útil porque los píxeles aislados de la imagen 2D pueden convertirse en pequeños elementos geométricos individuales del modelo 3D.\r
\r
Empieza con el **Peso** y las **Pasadas** predeterminados y haz clic en **Aplicar**. Aumenta las pasadas solo si una deja todavía demasiados píxeles aislados.\r
\r
## Activar el modo 3D\r
\r
Haz clic en **3D**. Ajusta primero los parámetros básicos:\r
\r
- **Tamaño de píxel (XY)** controla la anchura y profundidad físicas de cada píxel.\r
- **Altura de capa** debe coincidir con la que vayas a usar en el laminador.\r
- **Altura de primera capa** debe coincidir con ese ajuste del laminador.\r
- **Malla suavizada** puede suavizar los límites conectados entre colores para obtener una geometría más lisa.\r
\r
## Elegir Manual o Pintura automática\r
\r
Usa **Manual** para controlar directamente los colores de la imagen reducida. El modo Manual usa las muestras de **Colores de la imagen**: arrástralas al orden de impresión deseado y usa el deslizador de cada fila para decidir cuánto grosor aporta ese color. Es una buena primera opción si ya conoces el orden de capas o trabajas con una paleta pequeña y sencilla.\r
\r
Usa **Pintura automática** para que Kromacut planifique el apilamiento físico. Parte de tus filamentos reales en vez de las muestras de la imagen reducida y utiliza el color y la **Distancia de ocultación (HD)** de cada filamento —la profundidad a la que oculta lo que hay debajo— para estimar cómo se verán las capas apiladas.\r
\r
Para una primera prueba de pintura automática:\r
\r
1. Añade los filamentos que realmente vayas a usar.\r
2. Define cada color de filamento con la mayor precisión posible.\r
3. Define su **HD**. La estimación de la varita sirve para experimentar, y puedes convertir un valor TD convencional (≈10 veces la HD); las distancias calibradas suelen dar los mejores resultados.\r
4. Deja **Altura máxima** en **Auto** al principio.\r
5. Activa **Correspondencia de color mejorada** si el primer resultado no reproduce colores importantes o quieres que el optimizador busque un orden mejor.\r
\r
Tras calcular el apilamiento, revisa las zonas de transición y los detalles de confianza antes de exportar. Una confianza baja suele indicar que falta un color útil en el conjunto de filamentos, que las distancias necesitan calibración o que la altura máxima es demasiado restrictiva.\r
\r
## Generar y exportar\r
\r
Haz clic en **Generar modelo 3D**. Cuando aparezca el modelo, usa el deslizador **Vista previa de capas** para inspeccionar cómo se construye de abajo arriba.\r
\r
Abre el menú de descarga y elige **Descargar STL** o **Descargar 3MF**. Después copia las **Instrucciones de impresión** para conservar el color inicial, las capas de cambio y los ajustes recomendados del laminador.\r
\r
Siguiente: [Modo 3D](3d-mode), [Pintura automática](auto-paint) o [Generación y exportación](generating-exporting-output#before-you-export).\r
`;export{a as default};
