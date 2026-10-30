const e=`---\r
title: Paramètres et commandes\r
slug: settings-and-controls\r
order: 80\r
description: Actions de l’en-tête, thèmes, sauvegarde, palettes, profils et commandes de l’espace de travail.\r
---\r
\r
# Paramètres et commandes\r
\r
Cette page rassemble les commandes qui affectent toute l’application ou passent facilement inaperçues.\r
\r
## Commandes de l’en-tête\r
\r
| Commande | Fonction |\r
| ------------- | ------------------------------------------------------------------------------ |\r
| Logo Kromacut | Revient à l’application lorsque la documentation est ouverte ; sinon, ouvre la page d’accueil. |\r
| Paramètres | Ouvre la boîte des paramètres, avec les commandes de langue, thème, ressources et mises à jour. |\r
\r
Le sélecteur de thème propose **Système**, **Sombre** et **Clair**. **Système** suit la préférence de couleurs du système d’exploitation ou du navigateur et se met à jour lorsqu’elle change. Le thème choisi est conservé pour les sessions suivantes.\r
\r
Le sélecteur **Langue** modifie l’interface, la documentation, les schémas et les pages publiques. Choisissez **Langue du système** pour suivre une langue prise en charge du navigateur ou du système d’exploitation, ou sélectionnez anglais, français, allemand, italien, roumain, espagnol, japonais, chinois simplifié, hindi, portugais européen, ukrainien ou bengali. Votre choix est enregistré localement et ne change ni vos illustrations, ni vos profils de filaments, ni vos réglages d’impression, ni la géométrie générée. Les traductions et polices sont incluses dans l’application de bureau ; aucun service de traduction en ligne ne reçoit votre travail.\r
\r
Les pages publiques proposent aussi un sélecteur de langue. La documentation traduite utilise des liens partageables préfixés par la langue, comme \`/ro/docs/overview\`. Les noms de pages et les ancres de section restent stables d’une langue à l’autre. Les extensions de fichiers, données numériques des modèles, noms saisis par l’utilisateur et titres originaux des œuvres communautaires ne sont pas traduits.\r
\r
La boîte des paramètres contient des liens vers la documentation, Discord, Reddit, GitHub et Patreon, et affiche la version actuelle de Kromacut. **Confidentialité et données locales** et **Conditions d’utilisation** sont également accessibles ici et s’ouvrent dans votre navigateur dans la langue sélectionnée.\r
\r
## Modes de l’espace de travail\r
\r
Utilisez les boutons **2D** et **3D** pour passer de la préparation d’image à la génération du modèle.\r
\r
La séparation verticale entre le panneau de commandes et l’aperçu peut être déplacée. Élargissez le panneau de gauche pour travailler sur des réglages détaillés, ou l’aperçu pour examiner l’image ou le modèle.\r
\r
Les pages de documentation utilisent des liens \`/docs/...\` partageables. Ouvrir un de ces liens vous amène directement au guide correspondant.\r
\r
Sur les petits écrans, développez **Sommaire** pour choisir un guide ou **Sur cette page** pour atteindre une section. Les deux se referment après sélection pour laisser de la place à la lecture. Les illustrations s’ouvrent en taille réelle en cliquant dessus ou en ciblant leur lien puis en appuyant sur Entrée.\r
\r
La plupart des sections latérales peuvent être repliées par leur titre. Le repli masque les commandes, pas leurs effets : les réglages actifs, les paramètres d’impression et les options d’optimisation restent appliqués. Les résumés repliés et les indicateurs d’état aident à repérer les changements actifs. L’état ouvert/fermé est mémorisé. Développer une section ne la réinitialise pas.\r
\r
**Annuler / Rétablir** partage l’historique de retouche d’image entre la 2D et la 3D. Ce n’est pas un historique des modifications de filaments, de l’étalonnage, des hauteurs de couche ou des paramètres d’optimisation. Utilisez le bouton de réinitialisation propre à chaque panneau lorsqu’il existe, et régénérez après avoir restauré un état de l’image.\r
\r
## Mode multiplateau expérimental\r
\r
L’option **Mode multiplateau** des paramètres est une fonctionnalité inachevée. Elle mémorise la préférence et peut jouer une animation d’aperçu, mais ne découpe pas encore l’image, ne crée pas de tuiles, ne répartit pas les objets sur des plateaux et ne change pas la géométrie exportée. Laissez-la désactivée pour l’impression normale. Ne l’utilisez pas pour faire tenir un modèle trop grand sur votre plateau.\r
\r
## Paramètres d’impression enregistrés\r
\r
Kromacut mémorise dans le navigateur des paramètres comme **Taille du pixel (XY)**, **Hauteur de couche**, **Hauteur de première couche**, **Largeur de ligne effective** et **Maillage lissé**.\r
\r
Utilisez le bouton de réinitialisation de **Paramètres d’impression 3D** pour revenir aux valeurs par défaut de la section, dont 0,42 mm pour la largeur de ligne effective.\r
\r
Les paramètres mémorisés sont locaux au navigateur/site actuel ou à l’application de bureau. Ils ne constituent ni une sauvegarde de l’illustration ni un projet complet enregistré, et les navigateurs distincts ou l’application de bureau ne les partagent pas nécessairement. Exportez les palettes et profils importants avant d’effacer les données. Charger un profil restaure ses filaments et ses observations, pas une image ni un maillage déjà généré.\r
\r
## État de peinture automatique enregistré\r
\r
Les paramètres de peinture automatique sont conservés entre sessions, notamment :\r
\r
- Les filaments.\r
- Le mode de peinture.\r
- La hauteur maximale et la hauteur de couche de l’éprouvette d’étalonnage.\r
- La correspondance des couleurs améliorée.\r
- La préservation de la séparation des couleurs, sa limite ΔE de correspondance unique et l’exigence éventuelle d’une correspondance unique pour chaque couleur.\r
- La limite totale de répétitions, partagée entre les apparitions supplémentaires de filaments dans l’empilement.\r
- Le détail des transitions et le tramage de hauteur.\r
- La largeur de ligne effective, modifiée dans **Paramètres d’impression 3D**, pour les avertissements de largeur, le nettoyage des points isolés et le tramage de hauteur, ainsi que la préférence **Omettre les points de couleur isolés**. Cette préférence de nettoyage ne supprime pas tous les avertissements de largeur et ne contrôle pas le tramage de hauteur.\r
- La peinture à plat et sa préférence face vers le haut sans couche transparente.\r
- L’algorithme d’optimisation et sa graine.\r
- La priorité régionale.\r
\r
Les profils sont distincts de cet état mémorisé. Utilisez-les pour disposer de jeux de filaments nommés que vous pouvez charger, importer ou exporter.\r
\r
## Fichiers de palette\r
\r
Les palettes personnalisées servent à la réduction des couleurs 2D. Leurs fichiers portent l’extension \`.kpal\`.\r
\r
La version 2 du format ajoute deux champs facultatifs : \`disabledColors\` (couleurs conservées mais exclues de la quantification) et \`colorNames\` (noms d’affichage facultatifs par couleur). Les deux sont conservés à l’export et à l’import. Les fichiers de version 1 se chargent sans modification avec toutes les couleurs activées et sans nom ; un fichier v2 ouvert dans un ancien Kromacut est simplement traité comme si toutes les couleurs étaient activées.\r
\r
Utilisez des palettes personnalisées pour faire correspondre l’image réduite à un jeu de filaments connu ou à une collection fixe de couleurs.\r
\r
## Fichiers de profils de filaments\r
\r
Les profils de filaments de peinture automatique sont des jeux nommés que vous pouvez enregistrer, charger, importer et exporter. Ils utilisent \`.kfil\` et conservent les couleurs, noms, distances de masquage, données d’étalonnage, épreuves de palette et évaluations enregistrées, ainsi que des plans de matrice d’empilements et couleurs mesurées de taille limitée lorsqu’ils sont disponibles. Les anciens profils \`.kapp\` restent importables. Les profils d’anciennes versions stockaient les valeurs non étalonnées sur l’échelle TD conventionnelle ; elles sont converties automatiquement (×0,1) au chargement ou à l’import.\r
\r
Utilisez l’**icône d’importation** de la barre de profils pour importer un fichier. Un ancien fichier de même identifiant sans données d’apparence est importé comme copie distincte renommée au lieu d’effacer les observations d’étalonnage plus récentes ; en cas d’échec de stockage, la liste existante reste inchangée et un message d’erreur apparaît. Utilisez l’**icône de téléchargement** pour exporter le jeu de filaments actuel. Les fichiers exportés utilisent \`.kfil\` par défaut. Si le profil chargé contient des modifications de filaments non enregistrées, l’export crée un nouveau profil « modifications non enregistrées », sans les observations d’apparence liées aux anciennes identités de filaments.\r
\r
### Formats d’import pris en charge\r
\r
| Format | Extension | Remarques |\r
| ----------------------- | -------------- | ------------------------------------------------------------------------------ |\r
| Profil Kromacut | \`.kfil\` | Format natif. Accepte un profil unique ou un tableau de profils dans un même fichier. |\r
| Ancien profil Kromacut | \`.kapp\` | Ancien format natif, toujours entièrement pris en charge à l’import. |\r
| JSON brut | \`.json\` | Accepté si le fichier contient un objet de profil ou un tableau d’objets de profil. |\r
| CSV/TSV de bobines HueForge | \`.csv\`, \`.tsv\` | Voir ci-dessous. |\r
\r
### Gestion des doublons\r
\r
À l’import, Kromacut compare chaque profil entrant aux profils existants :\r
\r
- **Même identifiant :** remplace normalement le profil existant. Si cela remplacerait des observations d’apparence enregistrées par un fichier sans observations utilisables, une copie distincte est importée à la place.\r
- **Même contenu, identifiant différent :** ignoré uniquement si les données de filaments et les observations d’apparence correspondent à un profil existant.\r
- **Même nom, contenu différent :** importé avec un suffixe numérique ajouté au nom (par exemple \`Mes bobines (2)\`).\r
\r
Un bref bilan du nombre de profils importés, remplacés, ignorés ou renommés apparaît après chaque import.\r
\r
### Importer depuis HueForge\r
\r
Les exports de bibliothèque de bobines HueForge (\`.csv\` ou \`.tsv\`) sont directement importables. Utilisez **Export Spools** dans HueForge pour enregistrer un CSV, puis cliquez sur l’icône d’importation de la barre de profils de peinture automatique et sélectionnez le fichier. Le séparateur (virgule ou tabulation) est détecté automatiquement dans l’en-tête. Chaque bobine devient une entrée de filament nommée \`<Brand>-<Color Name>-<Hex>\`, par exemple \`Inland Basic-Light Brown-#BF9C81\`. Les UUID HueForge sont conservés comme identifiants de filament afin que réimporter la même bibliothèque ne crée pas de doublons. Les valeurs TD HueForge sont considérées comme des TD conventionnelles pour rétroéclairage/lithophanie et converties en distances de masquage frontales à l’import.\r
\r
## Avis de mise à jour sur ordinateur\r
\r
Dans l’application de bureau, Kromacut peut afficher un avis lorsqu’une version plus récente est disponible. Cet avis permet d’ouvrir la page de téléchargement ou de fermer le rappel.\r
\r
Ouvrez **Paramètres** pour rechercher manuellement des mises à jour. Les paramètres de bureau comprennent aussi **Vérifier au démarrage**, qui détermine si Kromacut recherche des mises à jour à l’ouverture. L’option est activée par défaut et les vérifications manuelles fonctionnent même lorsqu’elle est désactivée.\r
\r
Sous Linux, les AppImages contenant des informations de mise à jour peuvent être actualisées avec des outils compatibles comme AppImageUpdate. Ces outils utilisent le fichier \`.AppImage.zsync\` de la version pour télécharger les parties modifiées. Les anciennes AppImages sans ces informations nécessitent un premier téléchargement manuel d’une version compatible. Le fichier \`.zsync\` n’est pas un programme d’installation, et l’avis de mise à jour de Kromacut n’installe pas automatiquement les mises à jour.\r
\r
## Diagnostics de peinture automatique sur ordinateur\r
\r
L’application de bureau peut enregistrer des informations structurées sur les nouveaux calculs de peinture automatique. Ouvrez **Paramètres** et activez **Enregistrer les diagnostics de peinture automatique** avant de lancer un calcul. Le réglage ne relance pas et n’enregistre pas un résultat déjà calculé.\r
\r
Chaque calcul crée un fichier \`.jsonl\` distinct dans le dossier de diagnostics de peinture automatique de Kromacut. Utilisez **Ouvrir le dossier** à côté du réglage pour trouver les fichiers. Chaque ligne constitue un événement JSON complet : progression, erreurs et annulations restent donc lisibles même si le calcul n’aboutit pas.\r
\r
Une trace complète contient les informations de base d’exécution, l’instantané des filaments et de l’étalonnage actifs, les paramètres de génération et d’optimisation, des échantillons limités de progression, l’état d’ajustement de l’apparence, les décisions progressives de niveaux de répétition, les couches physiques finales, chaque couleur imprimable candidate finale, les comparaisons Delta E entre cibles et candidats, la confiance de prédiction et les mesures ayant contribué aux couleurs interpolées ou ajustées localement. Elle enregistre les couleurs de palette traitées et leurs poids, pas l’image source chargée. Les données d’étalonnage et de profil peuvent néanmoins être sensibles : examinez la trace avant de la partager publiquement.\r
\r
L’enregistrement sert aux investigations et peut créer de gros fichiers. Laissez-le désactivé pour les impressions ordinaires lorsque vous n’avez pas besoin d’une trace.\r
\r
## Ouvrir des fichiers depuis le bureau\r
\r
Les installations de bureau associent les fichiers \`.kfil\` et les anciens \`.kapp\` aux profils de filaments, et les fichiers \`.kpal\` aux palettes. Double-cliquez sur un fichier pour l’importer et le sélectionner dans Kromacut. Si l’application est déjà ouverte, le fichier est traité dans sa fenêtre existante. Les règles habituelles de validation, migration, doublons et préservation du calibrage s’appliquent.\r
\r
Les fichiers ouverts depuis le bureau attendent tant que l’éditeur de palettes, la boîte de dialogue d’étalonnage ou un formulaire pour renommer ou enregistrer un nouveau profil est ouvert. Terminez ou annulez cette session pour poursuivre les importations en attente ; les noms saisis et le profil en cours de modification restent inchangés jusque-là.\r
\r
Les importations attendent également pendant la modification du nom ou du champ HD d’un filament, ou tant que son sélecteur de couleur ou le panneau **Convertir depuis TD** est ouvert. Quittez le champ ou fermez le sélecteur ou le panneau pour reprendre les importations en attente ; les modifications de filaments non enregistrées nécessitent toujours votre choix avant d’être remplacées par un autre profil.\r
\r
Avant de remplacer des modifications de filaments non enregistrées, Kromacut propose **Conserver les modifications** ou **Ouvrir le profil**. Conservez-les pour les enregistrer d’abord ; ouvrir le profil les abandonne. Les fichiers ouverts ainsi doivent faire moins de 32 Mio. Les fichiers JSON génériques, images et modèles conservent leurs procédures d’importation habituelles.\r
\r
Sous Linux, les AppImages portables nécessitent une intégration au bureau pour les associations de fichiers. Si Kromacut n’est pas l’application par défaut, utilisez **Ouvrir avec** dans votre gestionnaire de fichiers.\r
\r
Suite : [Dépannage](troubleshooting).\r
`;export{e as default};
