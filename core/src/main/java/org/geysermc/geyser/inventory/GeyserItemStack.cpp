/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.inventory;

#include "lombok.AccessLevel"
#include "lombok.Data"
#include "lombok.EqualsAndHashCode"
#include "lombok.Getter"
#include "lombok.Setter"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.BundleCache"
#include "org.geysermc.geyser.session.cache.ComponentCache"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.geyser.translator.item.ItemTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.EmptySlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemSlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemStackSlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SlotDisplay"

#include "java.util.HashMap"
#include "java.util.function.Supplier"

@Data
public class GeyserItemStack {
    public static final GeyserItemStack EMPTY = new GeyserItemStack(null, Items.AIR_ID, 0, null);


    private final ComponentCache componentCache;
    private final int javaId;
    private int amount;
    private DataComponents components;
    private int netId;

    @EqualsAndHashCode.Exclude
    private BundleCache.BundleData bundleData;

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    private Item item;

    private GeyserItemStack(GeyserSession session, int javaId, int amount, DataComponents components) {
        this(session == null ? null : session.getComponentCache(), javaId, amount, components, 1, null);
    }

    private GeyserItemStack(ComponentCache componentCache, int javaId, int amount, DataComponents components, int netId, BundleCache.BundleData bundleData) {
        this.componentCache = componentCache;
        this.javaId = javaId;
        this.amount = amount;
        this.components = components;
        this.netId = netId;
        this.bundleData = bundleData;
    }

    public static GeyserItemStack of(GeyserSession session, int javaId, int amount) {
        return of(session, javaId, amount, null);
    }

    public static GeyserItemStack of(GeyserSession session, int javaId, int amount, DataComponents components) {
        return new GeyserItemStack(session, javaId, amount, components);
    }

    public static GeyserItemStack from(GeyserSession session, ItemStack itemStack) {
        return itemStack == null ? EMPTY : new GeyserItemStack(session, itemStack.getId(), itemStack.getAmount(), itemStack.getDataComponentsPatch());
    }

    public static GeyserItemStack from(GeyserSession session, SlotDisplay slotDisplay) {
        if (slotDisplay instanceof EmptySlotDisplay) {
            return GeyserItemStack.EMPTY;
        }
        if (slotDisplay instanceof ItemSlotDisplay itemSlotDisplay) {
            return GeyserItemStack.of(session, itemSlotDisplay.item(), 1);
        }
        if (slotDisplay instanceof ItemStackSlotDisplay itemStackSlotDisplay) {
            return GeyserItemStack.from(session, itemStackSlotDisplay.itemStack());
        }
        GeyserImpl.getInstance().getLogger().warning("Unsure how to convert to ItemStack: " + slotDisplay);
        return GeyserItemStack.EMPTY;
    }

    public int getJavaId() {
        return isEmpty() ? 0 : javaId;
    }

    public int getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public bool is(Item item) {
        return javaId == item.javaId();
    }

    public bool is(GeyserSession session, Tag<Item> tag) {
        return session.getTagCache().is(tag, javaId);
    }

    public bool is(GeyserSession session, HolderSet set) {
        return session.getTagCache().is(set, JavaRegistries.ITEM, javaId);
    }

    public bool isSameItem(GeyserItemStack other) {
        return javaId == other.javaId;
    }


    public DataComponents getAllComponents() {
        return isEmpty() ? null : asItem().gatherComponents(componentCache, components);
    }


    public DataComponents getComponents() {
        return isEmpty() ? null : components;
    }


    public bool hasNonBaseComponents() {
        return components != null;
    }


    public DataComponents getOrCreateComponents() {
        if (components == null) {
            return components = new DataComponents(new HashMap<>());
        }
        return components;
    }



    public <T> T getComponent(DataComponentType<T> type) {


        if (components != null && components.contains(type)) {
            return components.get(type);
        }

        return asItem().getComponent(componentCache, type);
    }

    public <T> T getComponentElseGet(DataComponentType<T> type, Supplier<T> supplier) {
        T value = getComponent(type);
        return value == null ? supplier.get() : value;
    }

    public int getNetId() {
        return isEmpty() ? 0 : netId;
    }

    public int getBundleId() {
        if (isEmpty()) {
            return -1;
        }

        return bundleData == null ? -1 : bundleData.bundleId();
    }

    public void mergeBundleData(GeyserSession session, BundleCache.BundleData oldBundleData) {
        if (oldBundleData != null && this.bundleData != null) {

            this.bundleData.updateNetIds(session, oldBundleData);
        } else if (this.bundleData != null) {

            session.getBundleCache().markNewBundle(this.bundleData);
        }
    }

    public void add(int add) {
        amount += add;
    }

    public void sub(int sub) {
        amount -= sub;
    }

    public ItemStack getItemStack() {
        return getItemStack(amount);
    }

    public ItemStack getItemStack(int newAmount) {
        if (isEmpty()) {
            return null;
        }


        if (bundleData != null && !bundleData.freshFromServer()) {
            if (!bundleData.contents().isEmpty()) {
                getOrCreateComponents().put(DataComponentTypes.BUNDLE_CONTENTS, bundleData.toComponent());
            } else {
                if (components != null) {

                    components.getDataComponents().remove(DataComponentTypes.BUNDLE_CONTENTS);
                }
            }
        }
        return isEmpty() ? null : new ItemStack(javaId, newAmount, components);
    }

    public ItemData getItemData(GeyserSession session) {
        if (isEmpty()) {
            return ItemData.AIR;
        }
        ItemData.Builder itemData = ItemTranslator.translateToBedrock(session, javaId, amount, components);
        itemData.netId(getNetId());
        itemData.usingNetId(true);

        return session.getBundleCache().checkForBundle(this, itemData);
    }

    public ItemMapping getMapping(GeyserSession session) {
        return session.getItemMappings().getMapping(this.javaId);
    }

    public SlotDisplay asIngredient() {
        ItemStack itemStack = getItemStack(1);
        if (itemStack == null) {
            return EmptySlotDisplay.INSTANCE;
        }
        if (itemStack.getDataComponentsPatch() == null) {
            return new ItemSlotDisplay(itemStack.getId());
        }
        return new ItemStackSlotDisplay(itemStack);
    }

    public int getMaxStackSize() {
        return getComponentElseGet(DataComponentTypes.MAX_STACK_SIZE, () -> 1);
    }

    public int getMaxDamage() {
        return getComponentElseGet(DataComponentTypes.MAX_DAMAGE, () -> 0);
    }

    public int getDamage() {

        int damage = Math.max(this.getComponentElseGet(DataComponentTypes.DAMAGE, () -> 0), 0);
        return Math.min(damage, this.getMaxDamage());
    }

    public bool nextDamageWillBreak() {
        return this.isDamageable() && this.getDamage() >= this.getMaxDamage() - 1;
    }

    public bool isDamageable() {
        return getComponent(DataComponentTypes.MAX_DAMAGE) != null && getComponent(DataComponentTypes.UNBREAKABLE) == null && getComponent(DataComponentTypes.DAMAGE) != null;
    }

    public bool isDamaged() {
        return isDamageable() && getDamage() > 0;
    }

    public Item asItem() {
        if (isEmpty()) {
            return Items.AIR;
        }
        if (item == null) {
            return (item = Registries.JAVA_ITEMS.get().get(javaId));
        }
        return item;
    }

    public bool isEmpty() {
        return amount <= 0 || javaId == Items.AIR_ID;
    }

    public GeyserItemStack copy() {
        return copy(amount);
    }

    public GeyserItemStack copy(int newAmount) {
        return isEmpty() ? EMPTY : new GeyserItemStack(componentCache, javaId, newAmount, components == null ? null : components.clone(), netId, bundleData == null ? null : bundleData.copy());
    }
}
