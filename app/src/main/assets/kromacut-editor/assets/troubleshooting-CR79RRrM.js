const a=`---\r
title: Solución de problemas\r
slug: troubleshooting\r
order: 90\r
description: Problemas habituales y qué probar primero.\r
---\r
\r
# Solución de problemas\r
\r
Empieza aquí cuando un resultado parezca incorrecto o un control esté desactivado.\r
\r
## Un enlace muestra «Página no encontrada»\r
\r
La dirección puede tener una errata o apuntar a una página que ya no existe. Usa **Abrir Kromacut** para ir a la herramienta, **Ir al inicio** para visitar la página principal o **Explorar documentación** para encontrar la guía actual. Una dirección desconocida de documentación no se sustituye automáticamente por otra guía.\r
\r
## Generar modelo 3D no actualiza la vista\r
\r
Los ajustes 3D no se aplican hasta pulsar **Generar modelo 3D**. Cambia los ajustes deseados y vuelve a generar.\r
\r
## Las instrucciones de cambios están desactivadas\r
\r
En Manual, las imágenes de más de 64 colores desactivan estas instrucciones. Vuelve a **2D**, usa **Ajustes de cuantización** y reduce a 64 colores o menos. Pintura plana no tiene secuencia manual deliberadamente, porque cada capa puede contener varios filamentos.\r
\r
## El modelo es demasiado alto\r
\r
Prueba en este orden:\r
\r
1. En pintura automática, reduce **Altura máxima** y observa la compresión de las transiciones.\r
2. En Manual, revisa el número de colores. Muchos colores crean muchos grosores apilados, de modo que el modelo crece naturalmente.\r
3. Reduce colores en **2D** si no necesitas cada uno como capa separada.\r
4. En Manual, reduce uno o varios grosores de color.\r
5. Confirma que **Altura de capa** y **Altura de primera capa** coinciden con el laminador.\r
\r
## El modelo es demasiado grande en X o Y\r
\r
Reduce **Tamaño de píxel (XY)** para hacerlo menor. Recorta antes si hay bordes o fondo sin usar.\r
\r
## La imagen tiene motas o islas diminutas\r
\r
Usa **Eliminar tramado** después de reducir colores. Si quedan demasiados píxeles aislados, prueba menos colores u otro algoritmo de cuantización.\r
\r
## La pintura automática parece inexacta\r
\r
Causas habituales:\r
\r
- Las distancias de ocultación son estimadas, no calibradas.\r
- El conjunto de filamentos no cubre bien los colores de la imagen.\r
- **Altura máxima** comprime demasiado las transiciones.\r
- El optimizador necesita **Correspondencia de color mejorada**.\r
- El sujeto importante está en el centro o los bordes, pero **Prioridad regional** está en **Uniforme**.\r
\r
Calibra los filamentos y consulta **Confianza del resultado** para obtener pistas.\r
\r
Comprueba por separado **Modelo de apariencia**. Una puntuación global alta no garantiza exactitud física. **Solo estimaciones** o cero recetas de matriz activas significa que no hay mediciones de matriz aplicables. Guardar un perfil calibrado no hace compatible su evidencia con cualquier altura de capa o conjunto modificado. Consulta [Flujos de calibración](calibration-workflows).\r
\r
## Una opción ha desactivado otra\r
\r
**Conservar separación de colores** y **Tramado de altura** siguen siendo excluyentes porque asignan los colores originales a las alturas imprimibles de forma distinta. **Malla suavizada** y **Pintura plana** pueden usarse juntas; reconstruye después de cambiar cualquiera de los ajustes. Consulta [Pintura plana](flat-paint) y [Pintura automática](auto-paint).\r
\r
## La separación de colores no encuentra resultado\r
\r
El **Límite de correspondencia única** es un máximo estricto de error de color previsto. Con **Exigir una correspondencia única para cada color**, un solo color sin correspondencia rechaza el resultado. Considera reducir la paleta 2D, añadir un filamento útil, permitir más altura de transición o repeticiones o relajar el límite. Desactiva la exigencia estricta solo si aceptas perder colores distintos y fusionar sus regiones. Un modelo anterior puede seguir visible tras el rechazo; no es una generación correcta de los ajustes rechazados.\r
\r
## Mis ajustes desaparecen en 3D o al descargar\r
\r
Pulsa **Aplicar** en Ajustes para incorporar el aspecto en directo a la imagen original antes de cuantizar, generar o descargar. La vista ajustada y el original son distintos. Consulta [Ajustes de imagen](image-adjustments).\r
\r
## Eliminar una muestra no ha quitado sus píxeles\r
\r
**Eliminar** retira una opción de paleta y reasigna la imagen a los colores restantes. No es una goma. Usa Goma o pon el alfa de la muestra a cero para un recorte. Cuantizar puede volver opacos los píxeles parcialmente transparentes, así que revisa la silueta después.\r
\r
## Recopilar una traza de diagnóstico de pintura automática\r
\r
Para investigar a fondo un resultado en escritorio, activa **Registrar diagnósticos de pintura automática** en **Ajustes**, ejecuta un nuevo cálculo y usa **Abrir carpeta** para encontrar la traza \`.jsonl\`. Activa el registro antes de empezar el cálculo. Generar una malla de un resultado ya calculado no registra ese cálculo anterior. Consulta [Diagnósticos de pintura automática en escritorio](settings-and-controls#desktop-auto-paint-diagnostics) antes de compartir una traza.\r
\r
## La generación 3D es lenta\r
\r
Imágenes grandes, muchos colores, muchas capas y malla suavizada aumentan el tiempo. Prueba:\r
\r
- Recortar la imagen.\r
- Reducir el número de colores.\r
- Desactivar **Malla suavizada**.\r
- Reducir la resolución con **Redimensionar imagen**. Bajar solo Tamaño de píxel hace la misma malla más pequeña, no más sencilla.\r
- Simplificar las opciones de pintura automática.\r
\r
## El archivo exportado se abre con colores inesperados\r
\r
En 3MF, revisa las asignaciones de materiales o filamentos del laminador. Kromacut conserva el color cuando puede, pero los laminadores pueden asignar colores a extrusores de otra forma.\r
\r
STL no incluye asignaciones de color de filamento. Usa las **Instrucciones de impresión** para los cambios.\r
\r
La vista Simulados muestra mezclas estimadas; los laminadores suelen mostrar colores físicos. Cambia Kromacut a **Físicos** para revisar las asignaciones y después comprueba las bobinas reales. Ninguna vista demuestra el color final impreso.\r
\r
## El recorte o las ediciones han ido demasiado lejos\r
\r
Usa **Deshacer**. Rehacer está disponible si deshaces demasiado.\r
\r
Siguiente: [Preguntas frecuentes](faq).\r
`;export{a as default};
