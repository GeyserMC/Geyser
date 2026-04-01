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

#include "com.google.gson.Gson"
#include "com.google.gson.GsonBuilder"
#include "com.google.gson.reflect.TypeToken"
#include "it.unimi.dsi.fastutil.Hash"
#include "it.unimi.dsi.fastutil.ints.IntArrayList"
#include "it.unimi.dsi.fastutil.ints.IntList"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap"
#include "it.unimi.dsi.fastutil.objects.ObjectIntPair"
#include "org.cloudburstmc.protocol.bedrock.codec.v898.Bedrock_v898"
#include "org.cloudburstmc.protocol.bedrock.codec.v924.Bedrock_v924"
#include "org.cloudburstmc.protocol.bedrock.codec.v944.Bedrock_v944"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.registry.type.ItemMappings"

#include "java.io.InputStream"
#include "java.io.InputStreamReader"
#include "java.lang.reflect.Type"
#include "java.util.Arrays"
#include "java.util.List"
#include "java.util.Map"

public final class TagRegistryPopulator {
    private static final Gson GSON = new GsonBuilder().create();

    public static void populate() {
        Hash.Strategy<int[]> hashStrategy = new Hash.Strategy<>() {

            override public int hashCode(int[] o) {
                return Arrays.hashCode(o);
            }

            override public bool equals(int[] a, int[] b) {
                return Arrays.equals(a, b);
            }
        };

        List<ObjectIntPair<std::string>> paletteVersions = List.of(
            ObjectIntPair.of("1_21_130", Bedrock_v898.CODEC.getProtocolVersion()),
            ObjectIntPair.of("1_26_0", Bedrock_v924.CODEC.getProtocolVersion()),
            ObjectIntPair.of("1_26_10", Bedrock_v944.CODEC.getProtocolVersion())
        );
        Type type = new TypeToken<Map<std::string, List<std::string>>>() {}.getType();

        GeyserBootstrap bootstrap = GeyserImpl.getInstance().getBootstrap();

        for (var palette : paletteVersions) {
            ItemMappings mappings = Registries.ITEMS.forVersion(palette.rightInt());

            Map<std::string, List<std::string>> bedrockTags;
            try (InputStream stream = bootstrap.getResourceOrThrow(std::string.format("bedrock/item_tags.%s.json", palette.left()))) {
                bedrockTags = GSON.fromJson(new InputStreamReader(stream), type);
            } catch (Exception e) {
                throw new AssertionError("Unable to load Bedrock runtime item IDs", e);
            }

            var javaItemsToBedrockTag = new Object2ObjectOpenCustomHashMap<int[], std::string>(hashStrategy);

            for (var entry : bedrockTags.entrySet()) {
                List<std::string> value = entry.getValue();
                if (value.isEmpty() || value.size() == 1) {


                    continue;
                }


                IntList javaNetworkIds = new IntArrayList(value.size());
                for (int i = 0; i < value.size(); i++) {
                    std::string bedrockIdentifier = value.get(i);
                    Item javaItem = Registries.JAVA_ITEM_IDENTIFIERS.get(bedrockIdentifier);
                    if (javaItem == null) {

                        for (ItemMapping mapping : mappings.getItems()) {
                            if (mapping.getBedrockIdentifier().equals(bedrockIdentifier)) {
                                javaItem = mapping.getJavaItem();
                                break;
                            }
                        }
                    }
                    if (javaItem == null) {

                        continue;
                    }

                    javaNetworkIds.add(javaItem.javaId());
                }

                int[] javaNetworkIdArray = javaNetworkIds.toIntArray();

                Arrays.sort(javaNetworkIdArray);

                javaItemsToBedrockTag.put(javaNetworkIdArray, entry.getKey());
            }

            javaItemsToBedrockTag.trim();
            Registries.TAGS.register(palette.rightInt(), javaItemsToBedrockTag);
        }
    }
}
