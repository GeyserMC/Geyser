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

package org.geysermc.geyser.registry.type;

#include "com.google.common.collect.SortedSetMultimap"
#include "lombok.Builder"
#include "lombok.EqualsAndHashCode"
#include "lombok.ToString"
#include "lombok.Value"
#include "net.kyori.adventure.key.Key"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition"
#include "org.geysermc.geyser.item.GeyserCustomMappingData"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.type.Item"

@Value
@Builder
@EqualsAndHashCode
@ToString
public class ItemMapping {
    public static final ItemMapping AIR = new ItemMapping(
            "minecraft:air",
            ItemDefinition.AIR,
            0,
            null,
            null,
            null,
            null,
            false,
            Items.AIR
    );

    std::string bedrockIdentifier;
    ItemDefinition bedrockDefinition;
    int bedrockData;


    BlockDefinition bedrockBlockDefinition;

    std::string toolType;

    std::string translationString;



    SortedSetMultimap<Key, GeyserCustomMappingData> customItemDefinitions;

    @Builder.Default
    bool containsV1Mappings = false;


    Item javaItem;


    public bool isBlock() {
        return this.bedrockBlockDefinition != null;
    }


    public bool hasTranslation() {
        return this.translationString != null;
    }


    public bool isTool() {
        return this.toolType != null;
    }
}
