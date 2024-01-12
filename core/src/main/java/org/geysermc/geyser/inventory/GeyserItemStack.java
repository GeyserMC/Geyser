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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;

@Data
public class GeyserItemStack {
    public static final GeyserItemStack EMPTY = new GeyserItemStack(Items.AIR_ID, 0, null);

    private final int javaId;
    private int amount;
    private CompoundTag nbt;
    private int netId;

    @Getter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    private Item item;

    private GeyserItemStack(int javaId, int amount, CompoundTag nbt) {
        this(javaId, amount, nbt, 1);
    }

    private GeyserItemStack(int javaId, int amount, CompoundTag nbt, int netId) {
        this.javaId = javaId;
        this.amount = amount;
        this.nbt = nbt;
        this.netId = netId;
    }

    public static @NonNull GeyserItemStack from(@Nullable ItemStack itemStack) {
        return itemStack == null ? EMPTY : new GeyserItemStack(itemStack.getId(), itemStack.getAmount(), itemStack.getNbt());
    }

    public int getJavaId() {
        return isEmpty() ? 0 : javaId;
    }

    public int getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public @Nullable CompoundTag getNbt() {
        return isEmpty() ? null : nbt;
    }

    public int getNetId() {
        return isEmpty() ? 0 : netId;
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
        return isEmpty() ? null : new ItemStack(javaId, newAmount, nbt);
    }

    public ItemData getItemData(GeyserSession session) {
        if (isEmpty()) {
            return ItemData.AIR;
        }
        ItemData.Builder itemData = ItemTranslator.translateToBedrock(session, javaId, amount, nbt);
        itemData.netId(getNetId());
        itemData.usingNetId(true);
        return itemData.build();
    }

    public ItemMapping getMapping(GeyserSession session) {
        return session.getItemMappings().getMapping(this.javaId);
    }

    public Item asItem() {
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
        return isEmpty() ? EMPTY : new GeyserItemStack(javaId, newAmount, nbt == null ? null : nbt.clone(), netId);
    }
}
