package com.villagershop.menu;

/** Implémenté par les menus qui affichent le stock défilant (capacité + offset). */
public interface ScrollableStorageMenu {
    int getActiveCapacity();
    int getScrollOffset();
}
