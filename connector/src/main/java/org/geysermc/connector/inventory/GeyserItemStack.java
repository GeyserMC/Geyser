/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import lombok.Data;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;

@Data
public class GeyserItemStack {
    public static final GeyserItemStack EMPTY = new GeyserItemStack(0, 0, null);

    private final int javaId;
    private int amount;
    private CompoundTag nbt;
    private int netId;

    public GeyserItemStack(int javaId) {
        this(javaId, 1);
    }

    public GeyserItemStack(int javaId, int amount) {
        this(javaId, amount, null);
    }

    public GeyserItemStack(int javaId, int amount, CompoundTag nbt) {
        this(javaId, amount, nbt, 1);
    }

    public GeyserItemStack(int javaId, int amount, CompoundTag nbt, int netId) {
        this.javaId = javaId;
        this.amount = amount;
        this.nbt = nbt;
        this.netId = netId;
    }

    public static GeyserItemStack from(ItemStack itemStack) {
        return itemStack == null ? EMPTY : new GeyserItemStack(itemStack.getId(), itemStack.getAmount(), itemStack.getNbt());
    }

    public int getJavaId() {
        return isEmpty() ? 0 : javaId;
    }

    public int getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public CompoundTag getNbt() {
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

    public ItemStack getItemStack(int newAmount) {
        return isEmpty() ? null : new ItemStack(javaId, newAmount, nbt);
    }

    public ItemData getItemData(GeyserSession session) {
        ItemData itemData = ItemTranslator.translateToBedrock(session, getItemStack());
        itemData.setNetId(getNetId());
        itemData.setUsingNetId(true); // Seems silly - this should probably be on the protocol level
        return itemData;
    }

    public ItemEntry getItemEntry() {
        return ItemRegistry.ITEM_ENTRIES.get(getJavaId());
    }

    public boolean isEmpty() {
        return amount <= 0 || javaId == 0;
    }

    public GeyserItemStack copy() {
        return copy(amount);
    }

    public GeyserItemStack copy(int newAmount) {
        return isEmpty() ? EMPTY : new GeyserItemStack(javaId, newAmount, nbt == null ? null : nbt.clone(), netId);
    }
}
