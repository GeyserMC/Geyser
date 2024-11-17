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

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BundleCache;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.EmptySlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemStackSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SlotDisplay;

import java.util.HashMap;

@Data
public class GeyserItemStack {
    public static final GeyserItemStack EMPTY = new GeyserItemStack(Items.AIR_ID, 0, null);

    private final int javaId;
    private int amount;
    private DataComponents components;
    private int netId;

    @EqualsAndHashCode.Exclude
    private BundleCache.BundleData bundleData;

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    private Item item;

    private GeyserItemStack(int javaId, int amount, DataComponents components) {
        this(javaId, amount, components, 1, null);
    }

    private GeyserItemStack(int javaId, int amount, DataComponents components, int netId, BundleCache.BundleData bundleData) {
        this.javaId = javaId;
        this.amount = amount;
        this.components = components;
        this.netId = netId;
        this.bundleData = bundleData;
    }

    public static @NonNull GeyserItemStack of(int javaId, int amount) {
        return of(javaId, amount, null);
    }

    public static @NonNull GeyserItemStack of(int javaId, int amount, @Nullable DataComponents components) {
        return new GeyserItemStack(javaId, amount, components);
    }

    public static @NonNull GeyserItemStack from(@Nullable ItemStack itemStack) {
        return itemStack == null ? EMPTY : new GeyserItemStack(itemStack.getId(), itemStack.getAmount(), itemStack.getDataComponents());
    }

    public static @NonNull GeyserItemStack from(@NonNull SlotDisplay slotDisplay) {
        if (slotDisplay instanceof EmptySlotDisplay) {
            return GeyserItemStack.EMPTY;
        }
        if (slotDisplay instanceof ItemSlotDisplay itemSlotDisplay) {
            return GeyserItemStack.of(itemSlotDisplay.item(), 1);
        }
        if (slotDisplay instanceof ItemStackSlotDisplay itemStackSlotDisplay) {
            return GeyserItemStack.from(itemStackSlotDisplay.itemStack());
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

    public @Nullable DataComponents getComponents() {
        return isEmpty() ? null : components;
    }

    @NonNull
    public DataComponents getOrCreateComponents() {
        if (components == null) {
            return components = new DataComponents(new HashMap<>());
        }
        return components;
    }

    @Nullable
    public <T> T getComponent(@NonNull DataComponentType<T> type) {
        if (components == null) {
            return null;
        }
        return components.get(type);
    }

    public <T extends Boolean> boolean getComponent(@NonNull DataComponentType<T> type, boolean def) {
        if (components == null) {
            return def;
        }

        Boolean result = components.get(type);
        if (result != null) {
            return result;
        }
        return def;
    }

    public <T extends Integer> int getComponent(@NonNull DataComponentType<T> type, int def) {
        if (components == null) {
            return def;
        }

        Integer result = components.get(type);
        if (result != null) {
            return result;
        }
        return def;
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
            // Old bundle; re-use old IDs
            this.bundleData.updateNetIds(session, oldBundleData);
        } else if (this.bundleData != null) {
            // New bundle; allocate new ID
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

    public @Nullable ItemStack getItemStack(int newAmount) {
        if (isEmpty()) {
            return null;
        }
        // Sync our updated bundle data to server, if applicable
        // Not fresh from server? Then we have changes to apply!~
        if (bundleData != null && !bundleData.freshFromServer()) {
            if (!bundleData.contents().isEmpty()) {
                if (components == null) {
                    components = new DataComponents(new HashMap<>());
                }
                components.put(DataComponentType.BUNDLE_CONTENTS, bundleData.toComponent());
            } else {
                if (components != null) {
                    // Empty list = no component = should delete
                    components.getDataComponents().remove(DataComponentType.BUNDLE_CONTENTS);
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

    public SlotDisplay asSlotDisplay() {
        if (isEmpty()) {
            return EmptySlotDisplay.INSTANCE;
        }
        return new ItemStackSlotDisplay(this.getItemStack());
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

    public boolean isEmpty() {
        return amount <= 0 || javaId == Items.AIR_ID;
    }

    public GeyserItemStack copy() {
        return copy(amount);
    }

    public GeyserItemStack copy(int newAmount) {
        return isEmpty() ? EMPTY : new GeyserItemStack(javaId, newAmount, components == null ? null : components.clone(), netId, bundleData == null ? null : bundleData.copy());
    }
}
