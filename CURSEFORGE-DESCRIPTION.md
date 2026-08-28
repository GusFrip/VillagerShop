# VillagerShop

**Turn villagers into real merchants: create fully configurable multiplayer shops.**

VillagerShop adds a **Merchant Counter**: a job-site block you place and configure, which a villager claims to serve your customers - exactly like a villager binding to a lectern or a cartography table.

You define what you sell and what you buy, and the goods come from (and payments go into) the counter's own storage. No more trust chests: the shop manages stock and payments on its own.

## ✨ Features

- **Configurable Merchant Counter** - a tabbed interface (Shop / Trade / Stock / Upgrades) reserved for the owner.
- **A villager as the seller** - it binds to the counter like a real job-site block and opens a vanilla-style trading screen for customers.
- **Any item as currency** - not just emeralds: trade anything for anything.
- **Real physical stock** - goods sold leave the counter, payments are deposited into it. No stock = the offer is unavailable.
- **Modular upgrades** (Upgrades tab):
  - **Immortality** (Nether Star) - the seller becomes invincible.
  - **Movement** (Prismarine Shard = stays within 3 blocks; Amethyst Shard = stands still in front of the counter).
  - **Storage** (Chests) - +27 slots per chest, up to 216.
  - **More offers** (Emerald Blocks) - each block unlocks an extra trade, up to 8 offers.
  - **Communication** (Lightning Rod) - notifies owners when a sale is blocked (out of goods, or storage full) and when the merchant dies - instantly if online, otherwise on their next login. Adds a "Locate merchant" button.
  - **Access** (Block of Gold) - unlocks co-owner management.
  - **Memory** (Bookshelf) - save module: export the shop config into a signed book (copyable in a crafting table) and import it into another counter.
  - **Blast protection** (Obsidian) - the counter resists explosions.
- **Co-owners** - share configuration with other players (via the Access upgrade).
- **Built-in guide** - a Patchouli book explains everything, available in English and French.

Each upgrade unlocks a feature; remove the item and the feature turns off - but your settings are kept.

## 🏹 PillagerControl integration

With **PillagerControl** installed (1.21.1 NeoForge), a **pacified pillager** can run your counter instead of a villager - shopkeeper outfit included. The Upgrades tab then shows an exclusive **Guard Kit slot**: your vendor defends the shop with his crossbow when attacked, and even the Movement upgrade lets him leave for his daily meal at the Mess.

## 📦 Requirements

- **Minecraft 1.21.1** - NeoForge or Fabric
- **Minecraft 1.20.1** - Forge 47.2.0+
- **Patchouli** (required dependency, matching your version and loader)

## 🚀 Installation

1. Install NeoForge/Fabric (1.21.1) or Forge (1.20.1).
2. Put VillagerShop AND Patchouli in your mods/ folder.
3. On a server, install both mods server-side (and client-side for players).

## 📜 License

Released under the GNU LGPL-3.0. You're free to use, study, modify and redistribute the mod; modifications to the mod's code must stay under the same license with their source available. See the LICENSE and COPYING files.

## 🙏 Credits

- Development & original assets: GusFrip
- Dependency: Patchouli by Vazkii
