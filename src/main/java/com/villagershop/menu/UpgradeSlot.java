package com.villagershop.menu;

import com.villagershop.util.ItemStackHandler;
import com.villagershop.util.SlotItemHandler;

/** Slot d'amélioration (validation par le handler), masquable selon l'onglet. */
public class UpgradeSlot extends SlotItemHandler {
    private boolean visible = true;

    public UpgradeSlot(ItemStackHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    public void setVisible(boolean v) { this.visible = v; }

    @Override
    public boolean isActive() { return visible; }
}
