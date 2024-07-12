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

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.kyori.adventure.key.Key;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.HolderSet;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.session.cache.tags.TagRegistry;
import org.geysermc.geyser.session.cache.tags.VanillaTag;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

/**
 * Manages information sent from the {@link ClientboundUpdateTagsPacket}. If that packet is not sent, all lists here
 * will remain empty, matching Java Edition behavior. Only tags from registries in {@link TagRegistry} are stored.
 */
@ParametersAreNonnullByDefault
public final class TagCache {
    // Stores the indexes of non-vanilla tag keys in the tags array.
    private Object2IntMap<Key>[] tagIndexMaps = new Object2IntMap[TagRegistry.values().length];
    private int[][][] tags = new int[TagRegistry.values().length][][];

    public void loadPacket(GeyserSession session, ClientboundUpdateTagsPacket packet) {
        Map<Key, Map<Key, int[]>> allTags = packet.getTags();
        GeyserLogger logger = session.getGeyser().getLogger();

        this.tagIndexMaps = new Object2IntMap[TagRegistry.values().length];
        this.tags = new int[TagRegistry.values().length][][];

        for (Key registryKey : allTags.keySet()) {
            TagRegistry registry = TagRegistry.fromKey(registryKey);
            if (registry == null) {
                logger.debug("Not loading tags for registry " + registryKey + " (registry is not defined in TagRegistry enum)");
                continue;
            }

            Map<Key, int[]> registryTags = allTags.get(registryKey);

            if (registry == TagRegistry.BLOCK) {
                // Hack btw
                int[] convertableToMud = registryTags.get(MinecraftKey.key("convertable_to_mud"));
                boolean emulatePost1_18Logic = convertableToMud != null && convertableToMud.length != 0;
                session.setEmulatePost1_18Logic(emulatePost1_18Logic);
                if (logger.isDebug()) {
                    logger.debug("Emulating post 1.18 block predication logic for " + session.bedrockUsername() + "? " + emulatePost1_18Logic);
                }
            } else if (registry == TagRegistry.ITEM) {
                // Hack btw
                boolean emulatePost1_13Logic = registryTags.get(MinecraftKey.key("signs")).length > 1;
                session.setEmulatePost1_13Logic(emulatePost1_13Logic);
                if (logger.isDebug()) {
                    logger.debug("Emulating post 1.13 villager logic for " + session.bedrockUsername() + "? " + emulatePost1_13Logic);
                }
            }

            Object2IntMap<Key> tagIndexMap = new Object2IntOpenHashMap<>();
            this.tags[registry.ordinal()] = loadTags(registryTags, tagIndexMap, registry);
            this.tagIndexMaps[registry.ordinal()] = tagIndexMap;
        }
    }

    private int[][] loadTags(Map<Key, int[]> packetTags, Object2IntMap<Key> tagIndexMap, TagRegistry registry) {
        Map<Key, Tag> vanillaTags = registry.getVanillaTags();
        List<Key> nonVanillaTagKeys = packetTags.keySet().stream().filter(tag -> !vanillaTags.containsKey(tag)).toList();

        int[][] tags = new int[vanillaTags.size() + nonVanillaTagKeys.size()][];

        for (Map.Entry<Key, Tag> vanillaTag : vanillaTags.entrySet()) {
            tags[((VanillaTag) vanillaTag.getValue()).ordinal()] = packetTags.getOrDefault(vanillaTag.getKey(), new int[0]);
        }

        int tagIndex = vanillaTags.size();
        for (Key nonVanillaTagKey : nonVanillaTagKeys) {
            tags[tagIndex] = packetTags.get(nonVanillaTagKey);
            tagIndexMap.put(nonVanillaTagKey, tagIndex);
            tagIndex++;
        }

        return tags;
    }

    /**
     * @return true if the block tag is present and contains this block mapping's Java ID.
     */
    public boolean is(Tag tag, Block block) {
        assert tag.registry() == TagRegistry.BLOCK;
        return contains(get(tag), block.javaId());
    }

    /**
     * @return true if the item tag is present and contains this item stack's Java ID.
     */
    public boolean is(Tag tag, GeyserItemStack itemStack) {
        return is(tag, itemStack.asItem());
    }

    /**
     * @return true if the item tag is present and contains this item's Java ID.
     */
    public boolean is(Tag tag, Item item) {
        assert tag.registry() == TagRegistry.ITEM;
        return contains(get(tag), item.javaId());
    }

    /**
     * @return true if the specified network ID is in the given holder set.
     */
    public boolean is(HolderSet holderSet, int id) {
        return contains(holderSet.resolve(this), id);
    }

    /**
     * @return the network IDs in the given tag. This can be an empty list. Vanilla tags will be resolved faster than non-vanilla ones.
     */
    public int[] get(Tag tag) {
        if (tag instanceof VanillaTag vanillaTag) {
            return this.tags[tag.registry().ordinal()][vanillaTag.ordinal()];
        }

        int registryIndex = tag.registry().ordinal();
        Object2IntMap<Key> tagIndexMap = this.tagIndexMaps[registryIndex];
        if (!tagIndexMap.containsKey(tag.tag())) {
            return new int[0];
        }
        return this.tags[registryIndex][tagIndexMap.getInt(tag.tag())];
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
