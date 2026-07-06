package com.villagershop.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/**
 * Slot fantôme de prix B : masqué (inactif) par défaut, rendu visible à la demande
 * par l'écran (bouton « + »). Quand inactif, il n'est ni affiché ni cliquable, mais
 * son contenu reste synchronisé.
 */
public class ToggleGhostSlot extends Slot {
    private boolean visible = false;

    public ToggleGhostSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isActive() {
        return visible;
    }
}
