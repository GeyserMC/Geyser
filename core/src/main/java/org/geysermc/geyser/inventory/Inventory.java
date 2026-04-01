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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.click.ClickPlan;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.jetbrains.annotations.Range;

import java.util.Arrays;

@ToString
public abstract class Inventory {
    @Getter
    protected final int javaId;

    @Setter
    @Getter
    private int bedrockId;

    
    @Getter
    @Setter
    private int stateId;
    
    @Getter
    private int nextStateId = -1;

    @Getter
    protected final int size;

    
    @Getter
    protected final @Nullable ContainerType containerType;

    @Getter
    protected final String title;

    protected final GeyserItemStack[] items;

    
    @Getter
    @Setter
    protected Vector3i holderPosition = Vector3i.ZERO;

    
    @Getter
    @Setter
    protected long holderId = -1;

    
    @Getter
    @Setter
    private boolean displayed;

    protected Inventory(GeyserSession session, int id, int size, @Nullable ContainerType containerType) {
        this(session, "Inventory", id, size, containerType);
    }

    protected Inventory(GeyserSession session, String title, int javaId, int size, @Nullable ContainerType containerType) {
        this.javaId = javaId;
        this.size = size;
        this.containerType = containerType;
        this.title = getPrefixedTitle(session, title);
        this.items = new GeyserItemStack[size];
        Arrays.fill(items, GeyserItemStack.EMPTY);

        
        
        
        
        this.bedrockId = javaId <= 100 ? javaId : (javaId % 100) + 1;

        
        
        
        
        if ((session.getInventoryHolder() != null && session.getInventoryHolder().bedrockId() == bedrockId) || session.isClosingInventory()) {
            this.bedrockId += 1;
        }
    }

    public GeyserItemStack getItem(int slot) {
        if (slot > this.size) {
            GeyserImpl.getInstance().getLogger().debug("Tried to get an item out of bounds! " + this);
            return GeyserItemStack.EMPTY;
        }
        return items[slot];
    }

    public abstract int getOffsetForHotbar(@Range(from = 0, to = 8) int slot);

    public void setItem(int slot, @NonNull GeyserItemStack newItem, GeyserSession session) {
        if (slot < 0 || slot >= this.size) {
            session.getGeyser().getLogger().debug("Tried to set an item out of bounds (slot was " + slot + ")! " + this);
            return;
        }
        GeyserItemStack oldItem = items[slot];
        updateItemNetId(oldItem, newItem, session);
        items[slot] = newItem;

        
        if (newItem.is(Items.COMPASS)) {
            var tracker = newItem.getComponent(DataComponentTypes.LODESTONE_TRACKER);
            if (tracker != null) {
                session.getLodestoneCache().cacheInventoryItem(newItem, tracker);
            }
        }
    }

    public static void updateItemNetId(GeyserItemStack oldItem, GeyserItemStack newItem, GeyserSession session) {
        if (!newItem.isEmpty()) {
            ItemDefinition oldMapping = ItemTranslator.getBedrockItemDefinition(session, oldItem);
            ItemDefinition newMapping = ItemTranslator.getBedrockItemDefinition(session, newItem);
            if (oldMapping.equals(newMapping)) {
                newItem.setNetId(oldItem.getNetId());
                newItem.mergeBundleData(session, oldItem.getBundleData());
            } else {
                newItem.setNetId(session.getNextItemNetId());
                session.getBundleCache().markNewBundle(newItem.getBundleData());
                session.getBundleCache().onOldItemDelete(oldItem);
            }
        } else {
            
            session.getBundleCache().onOldItemDelete(oldItem);
        }
    }

    
    public void incrementStateId(int count) {
        
        nextStateId = (nextStateId == -1 ? stateId : nextStateId) + count & Short.MAX_VALUE;
    }

    public void resetNextStateId() {
        nextStateId = -1;
    }

    
    public boolean shouldConfirmContainerClose() {
        return true;
    }

    
    protected String getPrefixedTitle(GeyserSession session, String title) {
        return title;
    }
}
