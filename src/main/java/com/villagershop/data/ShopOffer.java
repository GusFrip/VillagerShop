package com.villagershop.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Une offre de boutique : le client paie priceA (+ priceB optionnel) et reçoit result.
 * Les ItemStack peuvent être n'importe quel item (troc libre) avec leur quantité.
 */
public class ShopOffer {
    private ItemStack priceA;
    private ItemStack priceB;
    private ItemStack result;

    public ShopOffer() {
        this(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
    }

    public ShopOffer(ItemStack priceA, ItemStack priceB, ItemStack result) {
        this.priceA = priceA == null ? ItemStack.EMPTY : priceA;
        this.priceB = priceB == null ? ItemStack.EMPTY : priceB;
        this.result = result == null ? ItemStack.EMPTY : result;
    }

    public ItemStack getPriceA() { return priceA; }
    public ItemStack getPriceB() { return priceB; }
    public ItemStack getResult() { return result; }

    public void setPriceA(ItemStack s) { this.priceA = s == null ? ItemStack.EMPTY : s; }
    public void setPriceB(ItemStack s) { this.priceB = s == null ? ItemStack.EMPTY : s; }
    public void setResult(ItemStack s) { this.result = s == null ? ItemStack.EMPTY : s; }

    /** Vide = aucun des trois items. */
    public boolean isEmpty() {
        return priceA.isEmpty() && priceB.isEmpty() && result.isEmpty();
    }

    public boolean isValid() {
        // Marchandise seule suffit : prix optionnel (offre gratuite, geree par ShopMerchantMenu).
        return !result.isEmpty();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("PriceA", priceA.save(new CompoundTag()));
        tag.put("PriceB", priceB.save(new CompoundTag()));
        tag.put("Result", result.save(new CompoundTag()));
        return tag;
    }

    public static ShopOffer load(CompoundTag tag) {
        ItemStack a = ItemStack.of(tag.getCompound("PriceA"));
        ItemStack b = ItemStack.of(tag.getCompound("PriceB"));
        ItemStack r = ItemStack.of(tag.getCompound("Result"));
        return new ShopOffer(a, b, r);
    }

    public ShopOffer copy() {
        return new ShopOffer(priceA.copy(), priceB.copy(), result.copy());
    }
}
