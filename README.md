# VillagerShop

**Transforme tes villageois en marchands : des boutiques multijoueur entièrement configurables pour Minecraft Fabric 1.21.1.**

> Version 1.21.1 (Fabric) du mod. Les versions 1.20.1 (Forge) et 1.21.1 (NeoForge) vivent dans les dossiers `villagershop-mod` et `villagershop-mod-1.21` du même dépôt.

Pose un **Comptoir de marchand**, configure tes ventes et tes achats, et un **villageois** se lie au comptoir pour servir tes clients — exactement comme un villageois s'attache à un établi ou à une table de cartographie. Seul le propriétaire (ou les joueurs qu'il autorise) peut ouvrir la configuration.

## Principe de jeu

1. **Crafte et pose le Comptoir de marchand** : tu en deviens automatiquement le propriétaire.
2. **Place-le près d'un villageois adulte sans emploi** : le villageois réclame le comptoir comme poste de travail et devient ta vitrine (profession « Marchand »).
3. **Clic droit sur le comptoir** (propriétaire ou joueur autorisé) → interface de configuration à onglets.
4. **Clic droit sur le villageois** (n'importe quel client) → interface de marchand classique pour acheter.

## Fonctionnalités

- **Comptoir de marchand configurable** via une interface à 4 onglets : **Boutique**, **Échanges**, **Stock**, **Améliorations**.
- **Vendeur villageois** lié au comptoir comme un vrai bloc-métier, qui ouvre une interface de marchand vanilla pour les clients (sans barre de progression / XP).
- **Troc libre** : le prix peut être **n'importe quel item**, pas seulement des émeraudes.
- **Stock physique réel** : les marchandises vendues sortent du comptoir, les paiements y sont déposés. Stock vide = offre indisponible.
- **Stockage extensible par coffres** : 3 emplacements de base, +27 par coffre inséré, jusqu'à 8 coffres (216 emplacements).
- **Co-propriétaires** : autorise d'autres joueurs à configurer la boutique.
- **Améliorations de protection du vendeur** (onglet Améliorations) :
  - **Étoile du Nether** → le vendeur devient invincible.
  - **Éclat de prismarine** → le vendeur ne s'éloigne jamais à plus de 3 blocs du comptoir.
  - **Éclat d'améthyste** → le vendeur se place face au comptoir et reste statique.
- **Messages de mort** envoyés aux propriétaires si le vendeur meurt.
- **Guide intégré** (Patchouli) disponible en **anglais** et en **français**.
- **Bloc orienté** (modèle type lectern), avec hitbox ajustée, qui **se casse comme une bûche** (plus vite à la hache).

## Prérequis

- **Minecraft 1.21.1**
- **Fabric Loader 0.16+**
- **[Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)** — dépendance obligatoire.
- **[Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli)** (version FABRIC) — dépendance obligatoire.
- **JDK 21** pour compiler.

## Installation

1. Installe **Fabric 1.21.1**.
2. Place **VillagerShop**, **Fabric API** ET **Patchouli** dans le dossier `mods/`.
3. Sur un serveur, installe les deux mods côté serveur (et côté client pour les joueurs).

## Compiler depuis les sources

```bash
./gradlew build      # jar dans build/libs/villagershop-1.0.0.jar
./gradlew runClient  # client de test
./gradlew runServer  # serveur de test
```

## Sécurité

Toute action sensible (achat, modification d'offre, gestion des autorisés) est **revérifiée côté serveur** contre le propriétaire et la liste des joueurs autorisés. Le client n'est jamais source de vérité.

## Licence

Distribué sous **GNU LGPL-3.0** (voir `LICENSE` et `COPYING`). Tu es libre d'utiliser, étudier, modifier et redistribuer le mod ; les modifications de son code doivent rester sous la même licence avec leur source disponible.

Dépendance : **Patchouli** par *Vazkii*, non incluse et sous sa propre licence.
