const a=`---\r
title: Ajustes y controles\r
slug: settings-and-controls\r
order: 80\r
description: Acciones de la cabecera, temas, persistencia, paletas, perfiles y controles del espacio de trabajo.\r
---\r
\r
# Ajustes y controles\r
\r
Esta página reúne controles que afectan a toda la aplicación o que es fácil pasar por alto.\r
\r
## Controles de la cabecera\r
\r
| Control | Función |\r
| ------------- | ------------------------------------------------------------------------------ |\r
| Logotipo de Kromacut | Vuelve a la aplicación si la documentación está abierta; si no, abre la página principal. |\r
| Ajustes | Abre el diálogo con controles de idioma, tema, recursos y actualizaciones. |\r
\r
El selector de tema ofrece **Sistema**, **Oscuro** y **Claro**. **Sistema** sigue la preferencia de colores del sistema operativo o navegador y se actualiza si cambia. La elección se guarda para futuras sesiones.\r
\r
El selector **Idioma** cambia interfaz, documentación, diagramas y páginas públicas. Elige **Idioma del sistema** para seguir un idioma compatible del navegador o sistema, o selecciona inglés, francés, alemán, italiano, rumano, español, japonés, chino simplificado, hindi, portugués europeo, ucraniano o bengalí. La elección se guarda localmente y no cambia ilustraciones, perfiles de filamentos, ajustes de impresión ni geometría generada. Las traducciones y fuentes se incluyen en la aplicación de escritorio; ningún servicio de traducción en línea recibe tu trabajo.\r
\r
Las páginas públicas también ofrecen selector de idioma. La documentación traducida usa enlaces compartibles con prefijo de idioma, como \`/ro/docs/overview\`. Los nombres de página y anclas de sección permanecen estables entre idiomas. No se traducen extensiones, datos numéricos del modelo, nombres introducidos por el usuario ni títulos originales de obras comunitarias.\r
\r
El diálogo incluye enlaces a documentación, Discord, Reddit, GitHub y Patreon, y muestra la versión actual de Kromacut. **Privacidad y datos locales** y **Términos y condiciones** también están disponibles aquí y se abren en el navegador en el idioma seleccionado.\r
\r
## Modos del espacio de trabajo\r
\r
Usa **2D** y **3D** para alternar entre preparación de imagen y generación del modelo.\r
\r
Puedes arrastrar el separador vertical entre controles y vista previa. Amplía el panel izquierdo para trabajar con ajustes detallados o la vista para inspeccionar imagen o modelo.\r
\r
Las páginas de documentación usan enlaces compartibles \`/docs/...\`. Abrir uno lleva directamente a la guía correspondiente.\r
\r
En pantallas pequeñas, despliega **Contenido** para elegir guía o **En esta página** para saltar a una sección. Ambos se cierran tras seleccionar para dejar espacio de lectura. Las ilustraciones se abren a tamaño completo al pulsarlas o enfocar su enlace y pulsar Intro.\r
\r
La mayoría de las secciones laterales se pliegan desde sus títulos. Plegar oculta controles, no efectos: los ajustes activos, parámetros de impresión y opciones del optimizador siguen aplicándose. Los resúmenes y puntos de estado ayudan a detectar cambios activos. El estado abierto/cerrado se recuerda. Desplegar una sección no la restablece.\r
\r
**Deshacer / Rehacer** comparte el historial de imagen entre 2D y 3D. No es un historial de cambios de filamentos, calibración, alturas u opciones del optimizador. Usa el restablecimiento propio de cada panel cuando exista y regenera después de restaurar una imagen.\r
\r
## Modo multiplaca experimental\r
\r
La opción **Modo multiplaca** de Ajustes es un flujo sin terminar. Recuerda la preferencia y puede reproducir una animación, pero aún no divide la imagen, crea mosaicos, distribuye objetos entre placas ni cambia la geometría exportada. Déjala desactivada para impresión normal. No sirve para encajar un modelo demasiado grande en la cama.\r
\r
## Ajustes de impresión guardados\r
\r
Kromacut recuerda en el navegador ajustes como **Tamaño de píxel (XY)**, **Altura de capa**, **Altura de primera capa**, **Ancho de línea efectivo** y **Malla suavizada**.\r
\r
Usa el restablecimiento de **Ajustes de impresión 3D** para volver a los valores predeterminados, incluidos 0,42 mm de ancho de línea efectivo.\r
\r
Los ajustes recordados son locales al navegador/sitio o aplicación de escritorio actual. No son una copia de la ilustración ni un proyecto completo, y otros navegadores o el escritorio no tienen por qué compartirlos. Exporta paletas y perfiles importantes antes de borrar datos. Cargar un perfil restaura filamentos y evidencia, no una imagen ni una malla ya generada.\r
\r
## Estado guardado de pintura automática\r
\r
Se conservan entre sesiones los siguientes ajustes:\r
\r
- Filamentos.\r
- Modo de pintura.\r
- Altura máxima y altura de capa de la cuña de calibración.\r
- Correspondencia de color mejorada.\r
- Conservación de separación, su límite ΔE único y la exigencia de correspondencia única para cada color.\r
- Límite total de repeticiones, compartido entre apariciones adicionales en todo el apilamiento.\r
- Detalle de transición y tramado de altura.\r
- Ancho de línea efectivo, editado en **Ajustes de impresión 3D**, para avisos, limpieza de motas y tramado de altura, junto con la preferencia **Omitir motas de color aisladas**. La limpieza no quita todos los avisos ni controla el tramado de altura.\r
- Pintura plana y la preferencia boca arriba sin capa transparente.\r
- Algoritmo y semilla del optimizador.\r
- Prioridad regional.\r
\r
Los perfiles son independientes de este estado. Úsalos para conjuntos nombrados que puedas cargar, importar o exportar.\r
\r
## Archivos de paleta\r
\r
Las paletas personalizadas sirven para reducir colores 2D. Sus archivos usan \`.kpal\`.\r
\r
La versión 2 añade dos campos opcionales: \`disabledColors\` (colores conservados pero excluidos de cuantización) y \`colorNames\` (nombres opcionales por color). Ambos se conservan al exportar e importar. Los archivos v1 se cargan sin cambios, con todos los colores activados y sin nombre; un v2 abierto en un Kromacut antiguo simplemente considera todos los colores activados.\r
\r
Usa paletas personalizadas para hacer coincidir la imagen reducida con filamentos conocidos o una colección fija de colores.\r
\r
## Archivos de perfiles de filamentos\r
\r
Los perfiles de pintura automática son conjuntos nombrados que puedes guardar, cargar, importar y exportar. Usan \`.kfil\` y almacenan colores, nombres, HD, calibración, registros y valoraciones de pruebas de paleta y, cuando existen, planes limitados de matrices y colores medidos. Los antiguos \`.kapp\` siguen siendo importables. Los perfiles de versiones anteriores guardaban valores no calibrados en escala TD convencional; se convierten automáticamente (×0,1) al cargar o importar.\r
\r
Usa el **icono de carga** en la barra de perfiles para importar. Un archivo antiguo con el mismo ID y sin apariencia se importa como copia renombrada separada en vez de borrar evidencia reciente; un error de almacenamiento deja intacta la lista existente y muestra un mensaje. Usa el **icono de descarga** para exportar el conjunto actual. Los archivos usan \`.kfil\` por defecto. Si el perfil cargado tiene ediciones sin guardar, la exportación crea un perfil nuevo de «ediciones sin guardar» sin evidencia de apariencia vinculada a las identidades anteriores.\r
\r
### Formatos de importación compatibles\r
\r
| Formato | Extensión | Notas |\r
| ----------------------- | -------------- | ------------------------------------------------------------------------------ |\r
| Perfil Kromacut | \`.kfil\` | Formato nativo. Admite un perfil o una matriz de perfiles en un archivo. |\r
| Perfil antiguo Kromacut | \`.kapp\` | Formato nativo anterior, aún totalmente compatible al importar. |\r
| JSON sin procesar | \`.json\` | Se acepta si contiene un objeto de perfil o una matriz de objetos. |\r
| CSV/TSV de bobinas HueForge | \`.csv\`, \`.tsv\` | Véase abajo. |\r
\r
### Gestión de duplicados\r
\r
Al importar, Kromacut compara cada perfil con los que ya tienes:\r
\r
- **Mismo ID:** normalmente sobrescribe el existente. Si sustituiría evidencia guardada por un archivo sin evidencia útil, importa una copia aparte.\r
- **Mismo contenido, distinto ID:** se omite solo si coinciden tanto datos de filamentos como evidencia de apariencia.\r
- **Mismo nombre, distinto contenido:** se importa añadiendo un sufijo numérico (por ejemplo, \`Mis bobinas (2)\`).\r
\r
Después de cada importación aparece un resumen de perfiles importados, sobrescritos, omitidos o renombrados.\r
\r
### Importar desde HueForge\r
\r
Las bibliotecas de bobinas HueForge (\`.csv\` o \`.tsv\`) se importan directamente. Usa **Export Spools** en HueForge para guardar un CSV y después el icono de carga de la barra de perfiles para elegirlo. El separador (coma o tabulación) se detecta en la cabecera. Cada bobina se convierte en una entrada llamada \`<Brand>-<Color Name>-<Hex>\`, por ejemplo \`Inland Basic-Light Brown-#BF9C81\`. Los UUID de HueForge se conservan como ID de filamentos para que reimportar no duplique entradas. Las TD de HueForge se tratan como entradas convencionales de retroiluminación/litofanía y se convierten a HD frontal durante la importación.\r
\r
## Avisos de actualización en escritorio\r
\r
La aplicación de escritorio puede avisar cuando hay una versión nueva. El aviso permite abrir la página de descarga o descartar el recordatorio.\r
\r
Abre **Ajustes** para buscar actualizaciones manualmente. En escritorio también aparece **Comprobar al iniciar**, que controla la búsqueda al abrir la aplicación. Está activada por defecto y las búsquedas manuales funcionan aunque se desactive.\r
\r
En Linux, las AppImage con información de actualización integrada pueden actualizarse con herramientas compatibles como AppImageUpdate. Estas utilizan el archivo \`.AppImage.zsync\` de la versión para descargar las partes modificadas. Las AppImage antiguas sin esa información requieren una primera descarga manual de una versión compatible. El archivo \`.zsync\` no es un instalador y el aviso de actualización de Kromacut no instala actualizaciones automáticamente.\r
\r
## Diagnósticos de pintura automática en escritorio\r
\r
La aplicación de escritorio puede registrar información estructurada de nuevos cálculos. Abre **Ajustes** y activa **Registrar diagnósticos de pintura automática** antes de iniciar uno. El ajuste no reinicia ni registra un resultado ya calculado.\r
\r
Cada cálculo crea un \`.jsonl\` separado en la carpeta de diagnósticos. Usa **Abrir carpeta** junto al ajuste para encontrarlos. Cada línea es un evento JSON completo, así que progreso, errores y cancelaciones siguen siendo legibles aunque el cálculo no termine.\r
\r
Una traza completa incluye información básica de ejecución, instantánea de filamentos y calibración activos, ajustes de generación y optimizador, muestras limitadas de progreso, estado de ajuste de apariencia, decisiones progresivas de niveles de repetición, capas físicas finales, todos los candidatos imprimibles finales, comparaciones Delta E entre objetivos y candidatos, confianza y las mediciones que contribuyeron a colores interpolados o ajustados localmente. Registra colores de paleta procesados y pesos, no la imagen original cargada. Los datos de calibración y perfil pueden seguir siendo sensibles, así que revisa la traza antes de compartirla públicamente.\r
\r
El registro está pensado para investigar y puede crear archivos grandes. Déjalo desactivado en impresión normal cuando no necesites una traza.\r
\r
## Abrir archivos desde el escritorio\r
\r
Las instalaciones de escritorio asocian los archivos \`.kfil\` y los antiguos \`.kapp\` con perfiles de filamentos, y los archivos \`.kpal\` con paletas. Haz doble clic en un archivo para importarlo y seleccionarlo en Kromacut. Si la aplicación ya está abierta, se utiliza su ventana existente. Se aplican las reglas habituales de validación, migración, duplicados y conservación de la calibración.\r
\r
Los archivos abiertos desde el escritorio quedan en espera mientras esté abierto el editor de paletas, el cuadro de calibración o el formulario para renombrar o guardar un perfil nuevo. Termina o cancela esa sesión para continuar con las importaciones pendientes; los nombres escritos y el perfil que estás editando permanecen sin cambios hasta entonces.\r
\r
Las importaciones también esperan mientras editas el nombre o el campo HD de un filamento, o mientras su selector de color o el panel **Convertir desde TD** está abierto. Sal del campo o cierra el selector o el panel para reanudar las importaciones pendientes; los cambios de filamentos sin guardar siguen requiriendo tu decisión antes de que otro perfil los sustituya.\r
\r
Antes de reemplazar cambios de filamentos sin guardar, Kromacut ofrece **Conservar cambios** o **Abrir perfil**. Conserva los cambios para guardarlos primero; abrir el perfil los descarta. Los archivos abiertos así deben tener menos de 32 MiB. Los archivos JSON genéricos, imágenes y modelos mantienen sus flujos habituales de importación.\r
\r
En Linux, las AppImage portátiles requieren integración con el escritorio para asociar archivos. Si Kromacut no es la aplicación predeterminada, utiliza **Abrir con** en el gestor de archivos.\r
\r
Siguiente: [Solución de problemas](troubleshooting).\r
`;export{a as default};
