package com.villagershop.menu;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Slot d'amélioration (validation par le handler), masquable selon l'onglet. */
public class UpgradeSlot extends SlotItemHandler {
    private boolean visible = true;

    public UpgradeSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    public void setVisible(boolean v) { this.visible = v; }

    @Override
    public boolean isActive() { return visible; }
}
