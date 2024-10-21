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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.session.cache.tags.HolderSet;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

/**
 * Manages information sent from the {@link ClientboundUpdateTagsPacket}. If that packet is not sent, all lists here
 * will remain empty, matching Java Edition behavior. Only tags from registries in {@link JavaRegistries} are stored.
 */
@ParametersAreNonnullByDefault
public final class TagCache {
    private final Map<Tag<?>, int[]> tags = new Object2ObjectOpenHashMap<>();

    public void loadPacket(GeyserSession session, ClientboundUpdateTagsPacket packet) {
        Map<Key, Map<Key, int[]>> allTags = packet.getTags();
        GeyserLogger logger = session.getGeyser().getLogger();

        this.tags.clear();

        for (Key registryKey : allTags.keySet()) {
            JavaRegistryKey<?> registry = JavaRegistries.fromKey(registryKey);
            if (registry == null) {
                logger.debug("Not loading tags for registry " + registryKey + " (registry is not defined in TagRegistry enum)");
                continue;
            }

            Map<Key, int[]> registryTags = allTags.get(registryKey);

            if (registry == JavaRegistries.BLOCK) {
                // Hack btw
                int[] convertableToMud = registryTags.get(MinecraftKey.key("convertable_to_mud"));
                boolean emulatePost1_18Logic = convertableToMud != null && convertableToMud.length != 0;
                session.setEmulatePost1_18Logic(emulatePost1_18Logic);
                if (logger.isDebug()) {
                    logger.debug("Emulating post 1.18 block predication logic for " + session.bedrockUsername() + "? " + emulatePost1_18Logic);
                }
            } else if (registry == JavaRegistries.ITEM) {
                // Hack btw
                boolean emulatePost1_13Logic = registryTags.get(MinecraftKey.key("signs")).length > 1;
                session.setEmulatePost1_13Logic(emulatePost1_13Logic);
                if (logger.isDebug()) {
                    logger.debug("Emulating post 1.13 villager logic for " + session.bedrockUsername() + "? " + emulatePost1_13Logic);
                }
            }

            loadTags(registryTags, registry);
        }
    }

    private void loadTags(Map<Key, int[]> packetTags, JavaRegistryKey<?> registry) {
        for (Map.Entry<Key, int[]> tag : packetTags.entrySet()) {
            this.tags.put(new Tag<>(registry, tag.getKey()), tag.getValue());
        }
    }

    /**
     * @return true if the block tag is present and contains this block mapping's Java ID.
     */
    public boolean is(Tag<Block> tag, Block block) {
        // TODO should this check be removed? should it be changed into an assertion?
        if (tag.registry() != JavaRegistries.BLOCK) {
            throw new IllegalArgumentException("Given tag is not a block tag! (tag registry=%s)".formatted(tag.registry()));
        }
        return contains(get(tag), block.javaId());
    }

    /**
     * @return true if the item tag is present and contains this item stack's Java ID.
     */
    public boolean is(Tag<Item> tag, GeyserItemStack itemStack) {
        return is(tag, itemStack.asItem());
    }

    /**
     * @return true if the item tag is present and contains this item's Java ID.
     */
    public boolean is(Tag<Item> tag, Item item) {
        // TODO same
        if (tag.registry() != JavaRegistries.ITEM) {
            throw new IllegalArgumentException("Given tag is not an item tag! (tag registry=%s)".formatted(tag.registry()));
        }
        return contains(get(tag), item.javaId());
    }

    /**
     * @return true if the specified network ID is in the given holder set.
     */
    // TODO typed?
    public boolean is(HolderSet<?> holderSet, int id) {
        return contains(holderSet.resolve(this), id);
    }

    /**
     * @return the network IDs in the given tag. This can be an empty list. Vanilla tags will be resolved faster than non-vanilla ones.
     */
    public int[] get(Tag<?> tag) {
        return this.tags.getOrDefault(tag, new int[]{});
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
