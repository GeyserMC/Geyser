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

import it.unimi.dsi.fastutil.ints.IntArrays;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.BlockTag;
import org.geysermc.geyser.session.cache.tags.EnchantmentTag;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.geyser.util.Ordered;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Map;

import static org.geysermc.geyser.session.cache.tags.BlockTag.ALL_BLOCK_TAGS;
import static org.geysermc.geyser.session.cache.tags.EnchantmentTag.ALL_ENCHANTMENT_TAGS;
import static org.geysermc.geyser.session.cache.tags.ItemTag.ALL_ITEM_TAGS;

/**
 * Manages information sent from the {@link ClientboundUpdateTagsPacket}. If that packet is not sent, all lists here
 * will remain empty, matching Java Edition behavior.
 */
@ParametersAreNonnullByDefault
public final class TagCache {
    private final int[][] blocks = new int[ALL_BLOCK_TAGS.size()][];
    private final int[][] items = new int[ALL_ITEM_TAGS.size()][];
    private final int[][] enchantments = new int[ALL_ENCHANTMENT_TAGS.size()][];

    public void loadPacket(GeyserSession session, ClientboundUpdateTagsPacket packet) {
        Map<Key, int[]> blockTags = packet.getTags().get(MinecraftKey.key("block"));
        loadTags("Block", blockTags, ALL_BLOCK_TAGS, this.blocks);

        // Hack btw
        GeyserLogger logger = session.getGeyser().getLogger();
        int[] convertableToMud = blockTags.get(MinecraftKey.key("convertable_to_mud"));
        boolean emulatePost1_18Logic = convertableToMud != null && convertableToMud.length != 0;
        session.setEmulatePost1_18Logic(emulatePost1_18Logic);
        if (logger.isDebug()) {
            logger.debug("Emulating post 1.18 block predication logic for " + session.bedrockUsername() + "? " + emulatePost1_18Logic);
        }

        Map<Key, int[]> itemTags = packet.getTags().get(MinecraftKey.key("item"));
        loadTags("Item", itemTags, ALL_ITEM_TAGS, this.items);

        // Hack btw
        boolean emulatePost1_13Logic = itemTags.get(MinecraftKey.key("signs")).length > 1;
        session.setEmulatePost1_13Logic(emulatePost1_13Logic);
        if (logger.isDebug()) {
            logger.debug("Emulating post 1.13 villager logic for " + session.bedrockUsername() + "? " + emulatePost1_13Logic);
        }

        Map<Key, int[]> enchantmentTags = packet.getTags().get(MinecraftKey.key("enchantment"));
        loadTags("Enchantment", enchantmentTags, ALL_ENCHANTMENT_TAGS, this.enchantments);
    }

    private <T extends Ordered> void loadTags(String type, @Nullable Map<Key, int[]> packetTags, Map<Key, T> allTags, int[][] localValues) {
        if (packetTags == null) {
            Arrays.fill(localValues, IntArrays.EMPTY_ARRAY);
            GeyserImpl.getInstance().getLogger().debug("Not loading " + type + " tags; they do not exist here.");
            return;
        }
        allTags.forEach((location, tag) -> {
            int[] values = packetTags.get(location);
            if (values != null) {
                if (values.length != 0) {
                    localValues[tag.ordinal()] = values;
                } else {
                    localValues[tag.ordinal()] = IntArrays.EMPTY_ARRAY;
                }
            } else {
                localValues[tag.ordinal()] = IntArrays.EMPTY_ARRAY;
                GeyserImpl.getInstance().getLogger().debug(type + " tag not found from server: " + location);
            }
        });
    }

    /**
     * @return true if the block tag is present and contains this block mapping's Java ID.
     */
    public boolean is(BlockTag tag, Block block) {
        int[] values = this.blocks[tag.ordinal()];
        return contains(values, block.javaId());
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
        int[] values = this.items[tag.ordinal()];
        return contains(values, item.javaId());
    }

    public int[] get(ItemTag itemTag) {
        return this.items[itemTag.ordinal()];
    }

    public int[] get(EnchantmentTag enchantmentTag) {
        return this.enchantments[enchantmentTag.ordinal()];
    }

    private static boolean contains(int[] array, int i) {
        for (int item : array) {
            if (item == i) {
                return true;
            }
        }
        return false;
    }
}
