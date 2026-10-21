const e=`---\r
title: FAQ\r
slug: faq\r
order: 100\r
description: Réponses courtes aux questions fréquentes sur Kromacut.\r
---\r
\r
# FAQ\r
\r
## Qu’est-ce que la distance de masquage ?\r
\r
La distance de masquage, ou **HD**, est le paramètre d’opacité en éclairage frontal, exprimé en mm, utilisé par la peinture automatique pour estimer l’apparence des couches de filament empilées. À une épaisseur égale à la HD, le modèle de base conserve 10 % de l’influence de la couleur sous-jacente. Le point auquel la différence devient imperceptible dépend aussi du filament, de la couleur du support et des conditions d’observation.\r
\r
Une HD faible indique un filament plus opaque qui couvre en moins de couches. Une HD élevée indique un filament plus translucide qui nécessite davantage d’épaisseur.\r
\r
La HD remplace la distance de transmission (TD) affichée dans les versions précédentes. Vous pouvez saisir une TD conventionnelle pour rétroéclairage ou lithophanie à l’aide du bouton de conversion de la ligne d’un filament. Kromacut la multiplie par 0,1 pour obtenir une estimation initiale de HD ; cette conversion n’est pas une nouvelle mesure physique. Consultez [Méthodes d’étalonnage](calibration-workflows).\r
\r
## Faut-il utiliser Manuel ou Peinture automatique ?\r
\r
Utilisez **Manuel** pour contrôler directement et artistiquement l’ordre des couleurs et les hauteurs de couche.\r
\r
Utilisez **Peinture automatique** si vous disposez des couleurs réelles de vos filaments et de leurs distances de masquage, et souhaitez que Kromacut prépare automatiquement l’empilement.\r
\r
## Dois-je étalonner mes filaments ?\r
\r
Vous pouvez commencer avec des distances de masquage estimées. Des distances de transmission publiées pour le filament exact que vous possédez peuvent aussi servir de point de départ : saisissez-les avec le bouton de conversion, pas directement dans le champ HD.\r
\r
L’étalonnage améliore généralement la peinture automatique, surtout lorsqu’aucune valeur publiée n’est disponible ou que le résultat reste incorrect. Il est particulièrement utile lorsque :\r
\r
- Un filament est translucide.\r
- Deux filaments se ressemblent visuellement.\r
- Vous souhaitez des résultats reproductibles d’un projet à l’autre.\r
\r
## Quelle différence entre couleurs de palette et couleurs de filament ?\r
\r
Les couleurs de palette sont les couleurs de l’image utilisées en mode 2D et en mode Manuel.\r
\r
Les couleurs de filament correspondent aux matériaux physiques utilisés par la peinture automatique. Celle-ci peut générer des couleurs de couche virtuelles à partir de l’empilement physique, mais le plan d’impression exporté repose toujours sur des filaments réels.\r
\r
## Pourquoi l’aperçu 3D nécessite-t-il un bouton de génération ?\r
\r
La génération 3D peut être coûteuse. Kromacut attend que vous cliquiez sur **Générer le modèle 3D** afin qu’un changement de réglage ne lance et n’annule pas sans cesse de lourds calculs.\r
\r
## Peut-on exporter sans le mode 3D ?\r
\r
Utilisez le mode 2D pour télécharger l’image source actuelle, avec les modifications appliquées. Incorporez les réglages en direct avec **Appliquer** avant le téléchargement. Utilisez le mode 3D pour générer et exporter des modèles STL ou 3MF.\r
\r
## L’aperçu des couches modifie-t-il l’export ?\r
\r
Non. La plage **Aperçu des couches** modifie seulement ce qui est visible dans l’aperçu. Les exports STL et 3MF contiennent le modèle généré complet.\r
\r
## Quel fichier faut-il imprimer ?\r
\r
Choisissez **Télécharger STL** pour une large compatibilité avec les logiciels de découpe et des changements de filament manuels.\r
\r
Choisissez **Télécharger 3MF** si votre logiciel de découpe prend en charge les fichiers 3MF avec couleurs et si vous souhaitez préserver les objets de couches colorés.\r
\r
## Pourquoi les hauteurs sont-elles approximatives ?\r
\r
Les numéros de couche dépendent du logiciel de découpe, notamment de la hauteur de première couche. Utilisez les valeurs des **Instructions d’impression**, puis confirmez les couches de changement finales dans l’aperçu du logiciel de découpe.\r
\r
## Puis-je partager mes réglages ?\r
\r
Oui. Exportez les palettes 2D personnalisées au format \`.kpal\` et les profils de filaments de peinture automatique au format \`.kfil\`. Les anciens profils \`.kapp\` restent importables.\r
\r
## Une taille de pixel plus petite remplace-t-elle une buse plus fine ?\r
\r
Non. La taille du pixel définit les dimensions physiques des pixels de l’image. Elle peut rendre un trait plus fin que le cordon extrudé que votre buse peut produire. Réglez **Largeur de ligne effective** dans **Paramètres d’impression 3D**, utilisez l’aperçu des détails imprimables en peinture automatique et examinez le résultat découpé. Consultez [Mode 3D](3d-mode).\r
\r
## Mon étalonnage reste-t-il valable avec une autre hauteur de couche ?\r
\r
Ne le supposez pas. Les mesures HD et les observations d’apparence jouent des rôles différents. Les observations d’épreuve et de matrice sont vérifiées par rapport aux réglages et aux filaments avec lesquels elles ont été enregistrées. Après un changement de hauteur de couche, examinez les observations actives et imprimez un petit échantillon de validation. Consultez [Méthodes d’étalonnage](calibration-workflows).\r
\r
## Pourquoi y a-t-il plus de couleurs d’aperçu que de bobines ?\r
\r
Les couches fines laissent les filaments inférieurs influencer la couleur visible. Différentes épaisseurs du même ordre de bobines produisent différents mélanges prédits. La peinture automatique choisit parmi ces préfixes d’empilement accessibles, tandis que les pièces exportées utilisent toujours les filaments réels. Consultez [Peinture automatique](auto-paint).\r
`;export{e as default};
