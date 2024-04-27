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

package org.geysermc.geyser.session.cache;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.GlobalPos;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.LodestoneTracker;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A temporary cache for lodestone information.
 * Bedrock requests the lodestone position information separately from the item.
 */
public final class LodestoneCache {
    /**
     * A list of any GeyserItemStacks that are lodestones. Used mainly to minimize Bedrock's "pop-in" effect
     * when a new item has been created; instead we can re-use already existing IDs
     */
    private final Map<GeyserItemStack, LodestonePos> activeLodestones = new WeakHashMap<>();
    private final Int2ObjectMap<LodestonePos> lodestones = new Int2ObjectOpenHashMap<>();
    /**
     * An ID to increment for each lodestone
     */
    private int id = 1;

    public void cacheInventoryItem(GeyserItemStack itemStack, LodestoneTracker tracker) {
        GlobalPos position = tracker.getPos();

        if (position == null) {
            GeyserImpl.getInstance().getLogger().error("Position is null. Find out why.");
            Thread.dumpStack();
            return;
        }
        int x = position.getX();
        int y = position.getY();
        int z = position.getZ();
        String dim = position.getDimension();

        for (LodestonePos pos : this.activeLodestones.values()) {
            if (pos.equals(x, y, z, dim)) {
                this.activeLodestones.put(itemStack, pos);
                return;
            }
        }

        for (LodestonePos pos : this.lodestones.values()) {
            if (pos.equals(x, y, z, dim)) {
                // Use this existing position instead
                this.activeLodestones.put(itemStack, pos);
                return;
            }
        }

        this.activeLodestones.put(itemStack, new LodestonePos(id++, x, y, z, dim));
    }

    public int store(LodestoneTracker tracker) {
        GlobalPos position = tracker.getPos();

        if (position == null) {
            GeyserImpl.getInstance().getLogger().error("Position is null. Find out why.");
            Thread.dumpStack();
            return -1;
        }
        int x = position.getX();
        int y = position.getY();
        int z = position.getZ();
        String dim = position.getDimension();

        for (LodestonePos pos : this.activeLodestones.values()) {
            if (pos.equals(x, y, z, dim)) {
                // No need to add this into the lodestones map as it should not be re-requested
                return pos.id;
            }
        }

        for (Int2ObjectMap.Entry<LodestonePos> entry : this.lodestones.int2ObjectEntrySet()) {
            if (entry.getValue().equals(x, y, z, dim)) {
                // Use this existing position instead
                return entry.getIntKey();
            }
        }

        // Start at 1 as 0 does not work
        this.lodestones.put(id, new LodestonePos(id, x, y, z, dim));
        return id++;
    }

    public @Nullable LodestonePos getPos(int id) {
        LodestonePos pos = this.lodestones.remove(id);
        if (pos != null) {
            return pos;
        }
        for (LodestonePos activePos : this.activeLodestones.values()) {
            if (activePos.id == id) {
                return activePos;
            }
        }
        return null;
    }

    public void clear() {
        // Just in case...
        this.activeLodestones.clear();
        this.lodestones.clear();
    }

    public record LodestonePos(int id, int x, int y, int z, String dimension) {
        boolean equals(int x, int y, int z, String dimension) {
            return this.x == x && this.y == y && this.z == z && this.dimension.equals(dimension);
        }
    }
}
