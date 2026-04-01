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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.session.cache.tags.GeyserHolderSet;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public final class TagCache {
    private final GeyserSession session;
    private final Map<Tag<?>, int[]> tags = new Object2ObjectOpenHashMap<>();

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
                
                int[] convertableToMud = registryTags.get(MinecraftKey.key("convertable_to_mud"));
                boolean emulatePost1_18Logic = convertableToMud != null && convertableToMud.length != 0;
                session.setEmulatePost1_18Logic(emulatePost1_18Logic);
                if (logger.isDebug()) {
                    logger.debug("Emulating post 1.18 block predication logic for " + session.bedrockUsername() + "? " + emulatePost1_18Logic);
                }
            } else if (registry == JavaRegistries.ITEM) {
                
                boolean emulatePost1_13Logic = registryTags.get(MinecraftKey.key("signs")).length > 1;
                session.setEmulatePost1_13Logic(emulatePost1_13Logic);
                if (logger.isDebug()) {
                    logger.debug("Emulating post 1.13 villager logic for " + session.bedrockUsername() + "? " + emulatePost1_13Logic);
                }
            }

            loadTags(registryTags, registry, registry == JavaRegistries.ITEM);
        }
    }

    private void loadTags(Map<Key, int[]> packetTags, JavaRegistryKey<?> registry, boolean sort) {
        for (Map.Entry<Key, int[]> tag : packetTags.entrySet()) {
            int[] value = tag.getValue();
            if (sort) {
                
                Arrays.sort(value);
            }
            this.tags.put(new Tag<>(registry, tag.getKey()), value);
        }
    }

    
    public boolean is(@NonNull Tag<?> tag, int id) {
        return contains(getRaw(tag), id);
    }

    public <T> boolean is(@NonNull Tag<T> tag, @NonNull T object) {
        return contains(getRaw(tag), tag.registry().networkId(session, object));
    }

    
    public <T> boolean is(@NonNull GeyserHolderSet<T> holderSet, @Nullable T object) {
        if (object == null) {
            return false;
        }
        return contains(holderSet.resolveRaw(this), holderSet.getRegistry().networkId(session, object));
    }

    
    public <T> boolean is(@Nullable HolderSet holderSet, @NonNull JavaRegistryKey<T> registry, int id) {
        if (holderSet == null) {
            return false;
        }

        int[] entries = holderSet.resolve(key -> {
            
            
            if (key.value().startsWith("#")) {
                key = Key.key(key.namespace(), key.value().substring(1));
            }
            return getRaw(new Tag<>(registry, key));
        });

        return contains(entries, id);
    }

    public <T> List<T> get(@NonNull Tag<T> tag) {
        return mapRawArray(session, getRaw(tag), tag.registry());
    }

    
    public int[] getRaw(@NonNull Tag<?> tag) {
        return this.tags.getOrDefault(tag, IntArrays.EMPTY_ARRAY);
    }

    
    public static <T> List<T> mapRawArray(GeyserSession session, int[] array, JavaRegistryKey<T> registry) {
        return Arrays.stream(array).mapToObj(i -> registry.value(session, i)).toList();
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
