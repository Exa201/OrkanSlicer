const e=`---\r
title: Présentation\r
slug: overview\r
order: 10\r
description: Le rôle de Kromacut et l’articulation des principaux parcours de création.\r
---\r
\r
# Présentation\r
\r
Kromacut transforme une image plane en une impression 3D composée de couches de couleurs empilées. Le principe est simple : les couleurs de l’image deviennent des couches physiques, dont l’ordre et la hauteur constituent le plan d’impression.\r
\r
Utilisez Kromacut pour réaliser une impression de type HueForge, un relief coloré de type lithophanie ou une pièce décorative en couches dont les changements de filament composent l’image finale.\r
\r
## Parcours principal\r
\r
La plupart des projets suivent le même chemin :\r
\r
1. [Charger ou importer une image](loading-images).\r
2. [Réduire les couleurs](reducing-colors) jusqu’à obtenir une palette imprimable dans l’aperçu.\r
3. [Détramer ou nettoyer](dedithering-cleanup) les pixels isolés si l’image paraît bruitée.\r
4. Passer en [mode 3D](3d-mode) et choisir Manuel ou Peinture automatique.\r
5. [Générer et exporter](generating-exporting-output) un fichier STL ou 3MF, puis suivre les instructions d’impression.\r
\r
## Deux façons de peindre\r
\r
Kromacut propose deux méthodes d’impression en mode 3D.\r
\r
| Méthode | À utiliser lorsque | Ce que vous contrôlez |\r
| ---------- | ------------------------------------------------------ | --------------------------------------------------------------------------- |\r
| Manuel | Vous souhaitez contrôler directement chaque couleur de l’image. | Ordre des couleurs, épaisseurs par couleur, paramètres d’impression et changements de filament. |\r
| Peinture automatique | Vous souhaitez que Kromacut prépare l’empilement physique des filaments. | Couleurs des filaments, distances de masquage, hauteur maximale et options d’optimisation. |\r
\r
Le mode Manuel part des couleurs affichées dans le panneau **Couleurs de l’image**. La peinture automatique part de vos filaments réels et de leurs **distances de masquage (HD)**, puis génère des couches imprimables pour l’image.\r
\r
## Ce que l’application affiche\r
\r
Utilisez la 2D pour préparer l’illustration et la 3D pour prévoir les couches physiques. Les guides ci-dessous suivent cette distinction.\r
\r
## Guides illustrés\r
\r
Commencez par la tâche que vous souhaitez accomplir. Chaque guide explique les commandes, leurs interactions et leurs conséquences sur l’impression physique. Les schémas sont des exemples de principe, pas des prédictions de couleurs étalonnées. Cliquez sur une illustration ou activez-la au clavier pour l’ouvrir en taille réelle.\r
\r
| Tâche | Guide |\r
| ------------------------------------------------------------------ | -------------------------------------------------------------- |\r
| Définir les dimensions, les hauteurs de couche et l’ordre manuel des couleurs | [Mode 3D](3d-mode) |\r
| Choisir les filaments, optimiser les mélanges et examiner les détails imprimables | [Peinture automatique](auto-paint) |\r
| Créer une plaque plane multimatériau, face vers le haut ou vers le bas | [Peinture à plat](flat-paint) |\r
| Mesurer la HD, comparer des épreuves de palette ou photographier une matrice d’empilements | [Méthodes d’étalonnage](calibration-workflows) |\r
| Préparer la silhouette et retoucher les pixels | [Chargement des images](loading-images) |\r
| Régler les tons et les couleurs, puis appliquer le résultat | [Réglages d’image](image-adjustments) |\r
| Réduire les couleurs et gérer les palettes | [Réduction des couleurs](reducing-colors) |\r
| Retirer les points isolés sans confondre nettoyage 2D et tramage de hauteur | [Détramage et nettoyage](dedithering-cleanup) |\r
| Vérifier l’empilement terminé et le transférer vers un logiciel de découpe | [Génération et export](generating-exporting-output) |\r
\r
## Organisation de l’espace de travail\r
\r
L’espace de travail comprend trois zones principales :\r
\r
- L’en-tête contient la documentation, les commandes de thème et les liens communautaires.\r
- Le panneau de gauche contient les commandes du mode actif.\r
    - En **2D**, il présente les réglages, le détramage, la quantification, les palettes personnalisées et les couleurs détectées dans l’image.\r
    - En **3D**, il présente les paramètres d’impression, les commandes manuelles, celles de peinture automatique et les instructions d’impression.\r
- L’aperçu principal affiche le canevas de l’image 2D ou le modèle 3D.\r
\r
> Conseil : les paramètres 3D ne régénèrent pas automatiquement le modèle. Après avoir modifié les paramètres d’impression, les épaisseurs manuelles ou les options de peinture automatique, cliquez sur **Générer le modèle 3D**.\r
\r
## Un bon premier projet\r
\r
Commencez par une image contrastée dont le sujet se détache bien et dont l’arrière-plan contient peu de détails. Réduisez-la à 4 à 16 couleurs, puis utilisez le mode Manuel si vous connaissez déjà l’ordre des couches souhaité, ou la peinture automatique si vous disposez de distances de masquage étalonnées.\r
\r
---\r
\r
Suite : [Démarrage rapide](quick-start).\r
`;export{e as default};
