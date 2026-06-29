# Changelog

Toutes les modifications notables de VillagerShop sont consignées ici.
Format basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/).

## [1.1.0] - 2026-06-29
### Ajouté
- **Upgrade de communication** (paratonnerre) : notifications aux proprios/co-proprios — rupture de marchandise, stock plein, mort du marchand. Livrées en direct si connecté, sinon à la prochaine connexion (si toujours d'actualité). File persistée.
- **Upgrade d'accès** (bloc d'or) : la gestion des co-propriétaires devient un module débloquable. Sans le bloc, les co-proprios sont conservés mais inactifs.
- **Upgrade de mémoire** (bibliothèque) : module Sauvegarde — exporte la config (nom, offres, co-proprios) dans un livre & plume, réimportable dans un autre comptoir.
- **Bouton « Localiser le marchand »** (onglet Upgrade, nécessite la communication) : renvoie les coordonnées du villageois.
- Propriétaire affiché en tête de la liste des co-proprios ; impossible de se retirer soi-même ou de retirer le propriétaire.
- Onglet Stock : jusqu'à 3 lignes affichées + lien vers l'upgrade de stock.

### Corrigé
- Une vente ne peut plus se faire s'il n'y a pas la place de stocker le paiement (plus de perte d'items).
- Une offre dont le prix n'est mis que dans le 2e slot est désormais bien proposée.
- Crash de migration des sauvegardes lié à l'ajout de slots d'upgrade.
- Scrollbar de la liste co-proprios cliquable/draggable (plus seulement la molette).

## [1.0.0] - 2026-06-29
### Ajouté
- Comptoir de marchand : bloc-métier configurable par le propriétaire (et co-propriétaires).
- Un villageois se lie au comptoir et sert les clients via une interface de marchand classique.
- Échange de n'importe quel item (pas seulement des émeraudes), stock physique réel.
- Stockage extensible par coffres : 3 emplacements de base, +27 par coffre, jusqu'à 8 coffres (216).
- Interface de config à onglets : Boutique / Échanges / Stock / Améliorations.
- Améliorations de protection du vendeur : invincibilité (étoile du Nether), zone limitée à 3 blocs (éclat de prismarine), statique face au comptoir (éclat d'améthyste).
- Messages de mort envoyés aux propriétaires si le vendeur meurt.
- Guide intégré (Patchouli) en anglais et en français.
