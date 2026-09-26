/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.cache.registry;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistryKey;
import org.geysermc.geyser.session.cache.tags.GeyserHolderSet;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;

import java.util.Arrays;
import java.util.Map;

// TODO javadocs
/**
 * Manages information sent from the {@link ClientboundUpdateTagsPacket}. If that packet is not sent, all lists here
 * will remain empty, matching Java Edition behavior. Looking up a tag that wasn't listed in that packet will return an empty array.
 * Only tags from registries in {@link JavaRegistries} are stored. Read {@link JavaRegistryKey} for more information.
 *
 * <p>To simply check if an element is in a tag, it's preferred to use the element's "{@code is}" method, if available. For example:</p>
 *
 * <ul>
 *     <li>{@link org.geysermc.geyser.level.block.type.Block#is(GeyserSession, HolderSet)}</li>
 *     <li>{@link org.geysermc.geyser.inventory.GeyserItemStack#is(GeyserSession, Tag)}</li>
 *     <li>{@link org.geysermc.geyser.inventory.GeyserItemStack#is(GeyserSession, HolderSet)}</li>
 * </ul>
 */
public final class TagCache {
    private final GeyserSession session;
    private final Map<Tag<?>, IntList> tags = new Object2ObjectOpenHashMap<>();

    public TagCache(GeyserSession session) {
        this.session = session;
    }

    public void loadPacket(ClientboundUpdateTagsPacket packet) {
        Map<Key, Map<Key, int[]>> allTags = packet.getTags();
        GeyserLogger logger = session.getGeyser().getLogger();

        this.tags.clear();

        for (Key registryKey : allTags.keySet()) {
            JavaRegistryKey<?> registry = JavaRegistries.fromKey(registryKey);
            if (registry == null) {
                logger.debug("Not loading tags for registry " + registryKey + " (registry not listed in JavaRegistries)");
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
                int[] signs = registryTags.get(MinecraftKey.key("signs"));
                boolean emulatePost1_13Logic = signs != null && signs.length > 1;
                session.setEmulatePost1_13Logic(emulatePost1_13Logic);
                if (logger.isDebug()) {
                    logger.debug("Emulating post 1.13 villager logic for " + session.bedrockUsername() + "? " + emulatePost1_13Logic);
                }
            }

            loadTags(registryTags, registry, registry == JavaRegistries.ITEM);
        }
    }

    private void loadTags(Map<Key, int[]> packetTags, JavaRegistryKey<?> registry, @Deprecated boolean sort) {
        for (Map.Entry<Key, int[]> tag : packetTags.entrySet()) {
            int[] value = tag.getValue();
            if (sort) {
                // Used in RecipeBookAddTranslator, please note that you're not really supposed to sort tags: they are order-dependent,
                // however this functionality is not yet used for item tags on Java Edition
                Arrays.sort(value);
            }
            this.tags.put(new Tag<>(registry, tag.getKey()), IntList.of(value));
        }
    }

    /**
     * @return the network IDs in the given tag. This can be an empty list.
     */
    public @NonNull IntList get(@NonNull Tag<?> tag) {
        return this.tags.getOrDefault(tag, IntLists.emptyList());
    }
}
