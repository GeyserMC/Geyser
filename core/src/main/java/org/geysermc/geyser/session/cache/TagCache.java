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

import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;
import it.unimi.dsi.fastutil.ints.IntList;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.BlockMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.BlockTag;
import org.geysermc.geyser.session.cache.tags.ItemTag;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages information sent from the {@link ClientboundUpdateTagsPacket}. If that packet is not sent, all lists here
 * will remain empty, matching Java Edition behavior.
 *
 * This system is designed for easy extensibility - just add an enum to {@link BlockTag} or {@link ItemTag}.
 */
@ParametersAreNonnullByDefault
public final class TagCache {
    // Put these here so the enums can load without a static map
    public static final Map<String, BlockTag> ALL_BLOCK_TAGS = new HashMap<>();
    public static final Map<String, ItemTag> ALL_ITEM_TAGS = new HashMap<>();

    private final Map<BlockTag, IntList> blocks = new EnumMap<>(BlockTag.class);
    private final Map<ItemTag, IntList> items = new EnumMap<>(ItemTag.class);

    public void loadPacket(GeyserSession session, ClientboundUpdateTagsPacket packet) {
        Map<String, int[]> blockTags = packet.getTags().get("minecraft:block");
        this.blocks.clear();
        ALL_BLOCK_TAGS.forEach((location, tag) -> {
            int[] values = blockTags.get(location);
            if (values != null) {
                this.blocks.put(tag, IntList.of(values));
            } else {
                session.getGeyser().getLogger().debug("Block tag not found from server: " + location);
            }
        });

        // Hack btw
        GeyserLogger logger = session.getGeyser().getLogger();
        int[] convertableToMud = blockTags.get("minecraft:convertable_to_mud");
        boolean emulatePost1_18Logic = convertableToMud != null && convertableToMud.length != 0;
        session.setEmulatePost1_18Logic(emulatePost1_18Logic);
        if (logger.isDebug()) {
            logger.debug("Emulating post 1.18 block predication logic for " + session.bedrockUsername() + "? " + emulatePost1_18Logic);
        }

        Map<String, int[]> itemTags = packet.getTags().get("minecraft:item");
        this.items.clear();
        ALL_ITEM_TAGS.forEach((location, tag) -> {
            int[] values = itemTags.get(location);
            if (values != null) {
                this.items.put(tag, IntList.of(values));
            } else {
                session.getGeyser().getLogger().debug("Item tag not found from server: " + location);
            }
        });

        // Hack btw
        boolean emulatePost1_13Logic = itemTags.get("minecraft:signs").length > 1;
        session.setEmulatePost1_13Logic(emulatePost1_13Logic);
        if (logger.isDebug()) {
            logger.debug("Emulating post 1.13 villager logic for " + session.bedrockUsername() + "? " + emulatePost1_13Logic);
        }
    }

    /**
     * @return true if the block tag is present and contains this block mapping's Java ID.
     */
    public boolean is(BlockTag tag, BlockMapping mapping) {
        IntList values = this.blocks.get(tag);
        if (values != null) {
            return values.contains(mapping.getJavaBlockId());
        }
        return false;
    }

    /**
     * @return true if the item tag is present and contains this item stack's Java ID.
     */
    public boolean is(ItemTag tag, GeyserItemStack itemStack) {
        return is(tag, itemStack.asItem());
    }

    /**
     * @return true if the item tag is present and contains this item's Java ID.
     */
    public boolean is(ItemTag tag, Item item) {
        IntList values = this.items.get(tag);
        if (values != null) {
            return values.contains(item.javaId());
        }
        return false;
    }
}
