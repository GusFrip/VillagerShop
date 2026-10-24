package com.villagershop.menu;

/** Implémenté par les menus qui affichent le stock défilant (capacité + offset). */
public interface ScrollableStorageMenu {
    /** Capacité où l'on peut DÉPOSER (coffres × slots par coffre, selon la config). */
    int getActiveCapacity();
    /** Capacité AFFICHÉE : la capacité active, étendue aux slots encore occupés au-delà
     *  (ex. après une baisse de la config) pour que rien ne soit perdu : retrait seulement. */
    int getDisplayCapacity();
    int getScrollOffset();

    /** Les cases vides « coffre requis » complètent l'affichage jusqu'à ce slot, jamais au-delà. */
    int PLACEHOLDER_END = 27;

    /**
     * Capacité affichée : jusqu'au dernier item conservé hors capacité (au moins {@link #PLACEHOLDER_END}
     * pour laisser la place aux cases « coffre requis »), sinon la capacité active.
     */
    static int displayCapacity(int active, int chests, com.villagershop.util.ItemStackHandler storage) {
        int last = -1;
        for (int i = storage.getSlots() - 1; i >= active; i--) {
            if (!storage.getStackInSlot(i).isEmpty()) { last = i; break; }
        }
        if (last < 0) return active;
        return Math.min(storage.getSlots(), Math.max(last + 1, PLACEHOLDER_END));
    }
}
