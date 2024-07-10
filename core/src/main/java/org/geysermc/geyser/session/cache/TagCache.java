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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.ParametersAreNonnullByDefault;
import net.kyori.adventure.key.Key;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.session.cache.tags.TagRegistry;
import org.geysermc.geyser.session.cache.tags.VanillaTag;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;

/**
 * Manages information sent from the {@link ClientboundUpdateTagsPacket}. If that packet is not sent, all lists here
 * will remain empty, matching Java Edition behavior.
 */
@ParametersAreNonnullByDefault
public final class TagCache {
    private List<Object2IntMap<Key>> tagIndexMaps = new ArrayList<>();
    private int[][][] tags = new int[TagRegistry.values().length][][];

    public void loadPacket(GeyserSession session, ClientboundUpdateTagsPacket packet) {
        Map<Key, Map<Key, int[]>> allTags = packet.getTags();
        GeyserLogger logger = session.getGeyser().getLogger();

        this.tagIndexMaps = new ArrayList<>(allTags.size());
        this.tags = new int[allTags.size()][][];

        int i = 0;
        for (Key registryKey : allTags.keySet()) {
            TagRegistry registry = TagRegistry.valueOf(registryKey);
            if (registry == null) {
                GeyserImpl.getInstance().getLogger().debug("Not loading tags for registry " + registryKey + " (registry is not defined in TagRegistry enum)");
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

            int[][] registryTagsArray = new int[0][];
            this.tagIndexMaps.set(i, loadTags(registryTags, registryTagsArray, registry));
            this.tags[i] = registryTagsArray;
            i++;
        }
    }

    private Object2IntMap<Key> loadTags(Map<Key, int[]> packetTags, int[][] tags, TagRegistry registry) {
        List<Key> vanillaTagKeys = List.copyOf(registry.getVanillaTags().keySet());
        int nonVanillaTagAmount = (int) packetTags.keySet().stream().filter(tag -> !vanillaTagKeys.contains(tag)).count();

        List<int[]> tagsBuilder = new ArrayList<>(vanillaTagKeys.size() + nonVanillaTagAmount);
        Object2IntMap<Key> tagIndexMap = new Object2IntOpenHashMap<>();

        int nonVanillaTagIndex = vanillaTagKeys.size();
        for (Map.Entry<Key, int[]> tag : packetTags.entrySet()) {
            int id;
            if (vanillaTagKeys.contains(tag.getKey())) {
                id = vanillaTagKeys.indexOf(tag.getKey());
            } else {
                id = nonVanillaTagIndex;
                nonVanillaTagIndex++;
            }
            tagsBuilder.set(id, tag.getValue());
            tagIndexMap.put(tag.getKey(), id);
        }

        tagsBuilder.toArray(tags);
        return tagIndexMap;
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
        assert tag.registry() == TagRegistry.ITEM;
        return is(tag, itemStack.asItem());
    }

    /**
     * @return true if the item tag is present and contains this item's Java ID.
     */
    public boolean is(Tag tag, Item item) {
        assert tag.registry() == TagRegistry.ITEM;
        return contains(get(tag), item.javaId());
    }

    public int[] get(Tag tag) {
        if (tag instanceof VanillaTag vanillaTag) {
            return this.tags[tag.registry().ordinal()][vanillaTag.ordinal()];
        }

        int registryIndex = tag.registry().ordinal();
        Object2IntMap<Key> tagIndexMap = this.tagIndexMaps.get(registryIndex);
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
