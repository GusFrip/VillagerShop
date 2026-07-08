# Changelog

All notable changes to VillagerShop are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.4.1] - 2026-07-05
### Fixed
- The last payment of a trading session could land in a freshly emptied stock slot instead of topping up the existing pile (e.g. 63+1 instead of a single stack of 64). Deposits now fill existing piles first, then empty slots — same order as the trade simulation.
## [1.4.0] - 2026-07-04
### Added
- **Trades upgrade** (Emerald Block): counters now start with **1 unlocked trade**; each Emerald Block placed in the Upgrades tab unlocks one more, up to 7 blocks = 8 trades. Locked trades are greyed out; their settings are kept when blocks are removed.
- **Ownership transfer**: the owner can Shift+click a co-owner's ★ in the list to hand over the shop (old owner becomes co-owner).
- Locked stock slots are now greyed out with a tooltip (Chest required).
- **Blast protection upgrade** (Obsidian): the counter resists explosions (creepers, TNT).

### Changed
- **Existing counters are affected by the trades upgrade**: after updating, only the first trade is unlocked until Emerald Blocks are added. Configured trades are NOT lost — they re-activate as you add blocks.
- **Hoppers and pipes can no longer interact with the counter** (ownership/security concerns). May come back later as a dedicated upgrade.
- Co-owner list: removal now requires clicking precisely on the ✕ (no more accidental removals when clicking a row).
- **Owned counters are now protected**: only the owner and co-owners can break the counter (creative players bypass, e.g. for admins). Unowned counters remain breakable by anyone.

### Fixed
- Setting an item in a trade slot sometimes required several clicks (micro-drag was swallowed).
- The last payment of a full-stock trading session could drop on the ground instead of entering the stock.
- Save book: loading a config while the counter GUI was open didn't refresh (and could be overwritten by stale data).
## [1.3.0] - 2026-06-30
### Added
- **Free distribution**: leave an offer's price empty to give the item away for free (still limited by physical stock). Handled directly by the shop's merchant menu (no extra dependency), capped at one stack per shift-click, no duplication.

## [1.2.0] - 2026-06-30
### Fixed
- **Critical: shift-clicking a trade duplicated the goods** (item received without consuming the payment or the counter's stock). Caused by a vanilla crash when playing the trade sound on a non-entity merchant; the merchant menu now handles it safely. Shift-click is also capped at one stack per click.
- Offers now correctly go **out of stock** as the physical stock is depleted during a trading session, preventing buying/receiving beyond the available stock.

## [1.1.0] - 2026-06-29
### Added
- **Communication upgrade** (Lightning Rod): notifies owners/co-owners — out of goods, storage full, merchant death. Delivered instantly if online, otherwise on next login (only if still relevant). Persistent queue.
- **Access upgrade** (Block of Gold): co-owner management becomes an unlockable module. Without the block, co-owners are kept but inactive.
- **Memory upgrade** (Bookshelf): Save module — exports the config (name, offers, co-owners) into a book, re-importable into another counter.
- **"Locate merchant" button** (Upgrade tab, requires Communication): reports the villager's coordinates.
- Owner shown at the top of the co-owner list; you can't remove yourself or the owner.
- Stock tab: up to 3 rows displayed + link to the storage upgrade.

### Fixed
- A sale can no longer happen if there's no room to store the payment (no more lost items).
- An offer whose price is only set in the 2nd slot is now properly offered.
- Save migration crash caused by adding upgrade slots.
- Co-owner list scrollbar is now clickable/draggable (not just the mouse wheel).

## [1.0.0] - 2026-06-29
### Added
- Merchant Counter: a job-site block configurable by the owner (and co-owners).
- A villager binds to the counter and serves customers through a vanilla-style trading screen.
- Trade any item (not just emeralds), with real physical stock.
- Chest-based expandable storage: 3 base slots, +27 per chest, up to 8 chests (216).
- Tabbed config interface: Shop /