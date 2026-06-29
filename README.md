# VillagerShop — mod de boutiques multijoueur (Forge 1.20.1)

Pose un **Comptoir de marchand**, configure tes ventes et achats, et laisse un
**villageois** servir tes clients. Seul le propriétaire du comptoir (ou les joueurs
qu'il autorise) peut ouvrir l'interface de configuration.

## Principe de jeu

1. **Crafte le Comptoir de marchand** : 8 planches autour d'une émeraude.
2. **Pose-le** : tu en deviens automatiquement le propriétaire.
3. **Place-le près d'un villageois adulte sans emploi** : comme un pupitre crée un
   bibliothécaire, le villageois réclame le comptoir comme poste de travail et
   devient ta vitrine (profession « Marchand »).
4. **Clic droit sur le comptoir** (proprio ou joueur autorisé) : interface de config.
   - Slots fantômes à gauche : dépose un item **modèle** dans le slot *prix* puis dans
     le slot *marchandise* pour définir une offre. L'item n'est **pas consommé**
     (c'est juste un gabarit). La **quantité** du modèle = la quantité de l'offre :
     pour vendre contre 5 émeraudes, tiens une pile de 5 et clique gauche le slot prix.
     Clic droit sur un slot fantôme = effacer l'offre.
   - Grille « Stock » (27 cases) : dépose la **marchandise à vendre**. Les **paiements
     reçus** y sont aussi rangés. Si le stock est vide, l'offre devient indisponible.
   - Champ « Nom du joueur » + **Ajouter / Retirer** : gère les joueurs autorisés.
5. **Clic droit sur le villageois** (n'importe quel client) : interface d'achat.
   Le client clique **Acheter** ; le serveur vérifie stock + paiement, puis échange.

## Modèle de stock & monnaie (choix de design)

- **Stock physique réel** : une vente puise dans les 27 cases du comptoir, le paiement
  y est déposé. Pas de stock = pas de vente.
- **Troc libre** : le prix peut être **n'importe quel item** (pas seulement des
  émeraudes). Le modèle de données gère aussi un second item de prix (`priceB`),
  exposé côté code mais pas encore dans l'UI (voir « Pistes »).

## Prérequis

- **JDK 17** (obligatoire pour Forge 1.20.1).
- Forge `1.20.1-47.2.0` (téléchargé automatiquement par Gradle au premier build).

## Build

Ce dépôt ne contient pas `gradle/wrapper/gradle-wrapper.jar` (binaire). Deux options :

**Option A — générer le wrapper (Gradle installé) :**
```bash
gradle wrapper --gradle-version 8.8
./gradlew build
```

**Option B — ouvrir dans l'IDE :**
IntelliJ IDEA / Eclipse → *Import Gradle project* sur ce dossier. L'IDE télécharge
Forge et génère le wrapper. Puis lance la tâche `build`.

Le jar du mod sort dans `build/libs/villagershop-1.0.0.jar` → à glisser dans le
dossier `mods/` d'un client **et** d'un serveur Forge 1.20.1.

## Lancer en dev

```bash
./gradlew runClient   # client de test
./gradlew runServer   # serveur de test
```

## Architecture

```
ShopMod                  point d'entrée @Mod, branche les registres + handlers
registry/
  ModBlocks              bloc shop_counter
  ModItems               BlockItem
  ModBlockEntities       type de BlockEntity
  ModMenus               MenuType config + trade
  ModVillagers           POI "shopkeeper" + profession "shopkeeper"
  ModCreativeTabs        onglet créatif
block/
  ShopBlock              EntityBlock ; pose -> owner ; clic -> ouvre la config si autorisé
  ShopBlockEntity        cerveau : owner, whitelist, offres, stock 27 ; logique de trade
data/ShopOffer           une offre : priceA (+priceB) -> result
event/VillagerInteract…  intercepte le clic sur le villageois-vitrine -> ouvre l'achat
menu/
  ShopConfigMenu         slots fantômes (offres) + stock + inventaire ; ghost slots
  ShopTradeMenu          inventaire client + offres (snapshot à l'ouverture)
client/
  ClientSetup            enregistre les écrans
  ShopConfigScreen       UI proprio (offres, stock, whitelist)
  ShopTradeScreen        UI client (offres + boutons Acheter)
  ClientPacketHandler    réception S2C côté client
network/
  ModNetwork             SimpleChannel
  BuyOfferPacket         C2S : acheter une offre (validé serveur)
  ManageAllowedPacket    C2S : ajouter/retirer un joueur autorisé (proprio only)
  RequestAllowedPacket   C2S : demander la liste à l'ouverture
  SyncAllowedPacket      S2C : liste des autorisés -> affichage
```

**Sécurité** : toute action sensible est revérifiée côté serveur contre `ownerUUID`
+ `allowedPlayers`. Le client n'est jamais source de vérité (achats, offres, whitelist).

## Limites connues / pistes d'amélioration

- **Textures placeholder** : `textures/block/shop_counter.png` et les 2 GUI sont des
  gabarits gris générés automatiquement. À remplacer par de vrais visuels.
- **`priceB`** (2e item de prix, façon villageois vanilla) est géré en données mais
  pas dans l'UI de config — à ajouter (slot fantôme supplémentaire par offre).
- **Snapshot des offres** : l'écran d'achat fige les offres à l'ouverture ; si le
  proprio les modifie pendant qu'un client regarde, il faut rouvrir. (Le serveur, lui,
  valide toujours sur l'état réel — pas d'exploit.)
- **Protection du villageois** : pense à empêcher les clients de tuer/déplacer la
  vitrine (ex. invulnérabilité ou no-AI optionnel) — non implémenté.
- **Réclamation du POI** : le villageois doit être **adulte, sans emploi et proche**
  du comptoir, avec un chemin valide, pour le réclamer (contrainte vanilla). Sur un
  serveur, prévois un enclos avec un villageois libre à côté du comptoir.
- **Bypass admin (OP)** : non implémenté ; seul owner + whitelist ont accès.
- **Versions** : ciblé Forge 1.20.1 (47.x). Pour 1.21/NeoForge, plusieurs API diffèrent.
