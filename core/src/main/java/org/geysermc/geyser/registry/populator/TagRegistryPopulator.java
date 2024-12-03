/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.populator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import org.cloudburstmc.protocol.bedrock.codec.v748.Bedrock_v748;
import org.cloudburstmc.protocol.bedrock.codec.v765.Bedrock_v765;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class TagRegistryPopulator {
    private static final Gson GSON = new GsonBuilder().create(); // temporary

    public static void populate() {
        Hash.Strategy<int[]> hashStrategy = new Hash.Strategy<>() {
            // Necessary so arrays can actually be compared
            @Override
            public int hashCode(int[] o) {
                return Arrays.hashCode(o);
            }

            @Override
            public boolean equals(int[] a, int[] b) {
                return Arrays.equals(a, b);
            }
        };

        List<ObjectIntPair<String>> paletteVersions = List.of(
            ObjectIntPair.of("1_21_40", Bedrock_v748.CODEC.getProtocolVersion()),
            ObjectIntPair.of("1_21_50", Bedrock_v765.CODEC.getProtocolVersion())
        );
        Type type = new TypeToken<Map<String, List<String>>>() {}.getType();

        GeyserBootstrap bootstrap = GeyserImpl.getInstance().getBootstrap();

        for (var palette : paletteVersions) {
            ItemMappings mappings = Registries.ITEMS.forVersion(palette.rightInt());

            Map<String, List<String>> bedrockTags;
            try (InputStream stream = bootstrap.getResourceOrThrow(String.format("bedrock/item_tags.%s.json", palette.left()))) {
                bedrockTags = GSON.fromJson(new InputStreamReader(stream), type);
            } catch (Exception e) {
                throw new AssertionError("Unable to load Bedrock runtime item IDs", e);
            }

            var javaItemsToBedrockTag = new Object2ObjectOpenCustomHashMap<int[], String>(hashStrategy);

            for (var entry : bedrockTags.entrySet()) {
                List<String> value = entry.getValue();
                if (value.isEmpty() || value.size() == 1) {
                    // For our usecase, we don't need this. Empty values are worthless; one value can just be a reference
                    // to the item itself, instead of the tag.
                    continue;
                }

                // In some cases, the int list will need to be minimized
                IntList javaNetworkIds = new IntArrayList(value.size());
                for (int i = 0; i < value.size(); i++) {
                    String bedrockIdentifier = value.get(i);
                    Item javaItem = Registries.JAVA_ITEM_IDENTIFIERS.get(bedrockIdentifier);
                    if (javaItem == null) {
                        // Time to search the long way around.
                        for (ItemMapping mapping : mappings.getItems()) {
                            if (mapping.getBedrockIdentifier().equals(bedrockIdentifier)) {
                                javaItem = mapping.getJavaItem();
                                break;
                            }
                        }
                    }
                    if (javaItem == null) {
                        // Triggers for Bedrock-only spawn eggs. We don't care.
                        continue;
                    }

                    javaNetworkIds.add(javaItem.javaId());
                }

                int[] javaNetworkIdArray = javaNetworkIds.toIntArray();
                // Sort IDs so equality checks just have to match if each is equal and not necessarily an order difference.
                Arrays.sort(javaNetworkIdArray);

                javaItemsToBedrockTag.put(javaNetworkIdArray, entry.getKey());
            }

            javaItemsToBedrockTag.trim();
            Registries.TAGS.register(palette.rightInt(), javaItemsToBedrockTag);
        }
    }
}
