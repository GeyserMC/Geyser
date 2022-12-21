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

import com.github.steveice10.mc.protocol.data.game.inventory.ContainerType;
import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.math.vector.Vector3i;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.jetbrains.annotations.Range;

import javax.annotation.Nonnull;
import java.util.Arrays;

@ToString
public abstract class Inventory {
    @Getter
    protected final int javaId;

    /**
     * The Java inventory state ID from the server. As of Java Edition 1.18.1 this value has one instance per player.
     * If this is out of sync with the server when a packet containing it is handled, the server will resync items.
     * This field has existed since Java Edition 1.17.1.
     */
    @Getter
    @Setter
    private int stateId;
    /**
     * See {@link org.geysermc.geyser.inventory.click.ClickPlan#execute(boolean)}; used as a hack
     */
    @Getter
    private int nextStateId = -1;

    @Getter
    protected final int size;

    /**
     * Used for smooth transitions between two windows of the same type.
     */
    @Getter
    protected final ContainerType containerType;

    @Getter
    protected final String title;

    protected final GeyserItemStack[] items;

    /**
     * The location of the inventory block. Will either be a fake block above the player's head, or the actual block location
     */
    @Getter
    @Setter
    protected Vector3i holderPosition = Vector3i.ZERO;

    @Getter
    @Setter
    protected long holderId = -1;

    @Getter
    @Setter
    private boolean pending = false;

    protected Inventory(int id, int size, ContainerType containerType) {
        this("Inventory", id, size, containerType);
    }

    protected Inventory(String title, int javaId, int size, ContainerType containerType) {
        this.title = title;
        this.javaId = javaId;
        this.size = size;
        this.containerType = containerType;
        this.items = new GeyserItemStack[size];
        Arrays.fill(items, GeyserItemStack.EMPTY);
    }

    // This is to prevent conflicts with special bedrock inventory IDs.
    // The vanilla java server only sends an ID between 1 and 100 when opening an inventory,
    // so this is rarely needed. (certain plugins)
    // Example: https://github.com/GeyserMC/Geyser/issues/3254
    public int getBedrockId() {
        return javaId <= 100 ? javaId : (javaId % 100) + 1;
    }

    public GeyserItemStack getItem(int slot) {
        if (slot > this.size) {
            GeyserImpl.getInstance().getLogger().debug("Tried to get an item out of bounds! " + this);
            return GeyserItemStack.EMPTY;
        }
        return items[slot];
    }

    public abstract int getOffsetForHotbar(@Range(from = 0, to = 8) int slot);

    public void setItem(int slot, @Nonnull GeyserItemStack newItem, GeyserSession session) {
        if (slot > this.size) {
            session.getGeyser().getLogger().debug("Tried to set an item out of bounds! " + this);
            return;
        }
        GeyserItemStack oldItem = items[slot];
        updateItemNetId(oldItem, newItem, session);
        items[slot] = newItem;

        // Lodestone caching
        if (newItem.getJavaId() == session.getItemMappings().getStoredItems().compass().getJavaId()) {
            CompoundTag nbt = newItem.getNbt();
            if (nbt != null) {
                Tag lodestoneTag = nbt.get("LodestoneTracked");
                if (lodestoneTag instanceof ByteTag) {
                    session.getLodestoneCache().cacheInventoryItem(newItem);
                }
            }
        }
    }

    protected void updateItemNetId(GeyserItemStack oldItem, GeyserItemStack newItem, GeyserSession session) {
        if (!newItem.isEmpty()) {
            int oldMapping = ItemTranslator.getBedrockItemId(session, oldItem);
            int newMapping = ItemTranslator.getBedrockItemId(session, newItem);
            if (oldMapping == newMapping) {
                newItem.setNetId(oldItem.getNetId());
            } else {
                newItem.setNetId(session.getNextItemNetId());
            }
        }
    }

    /**
     * See {@link org.geysermc.geyser.inventory.click.ClickPlan#execute(boolean)} for more details.
     */
    public void incrementStateId(int count) {
        // nextStateId == -1 means that it was not needed until now
        nextStateId = (nextStateId == -1 ? stateId : nextStateId) + count & Short.MAX_VALUE;
    }

    public void resetNextStateId() {
        nextStateId = -1;
    }
}
