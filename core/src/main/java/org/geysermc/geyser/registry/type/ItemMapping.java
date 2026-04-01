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

import com.google.common.collect.SortedSetMultimap;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;

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

    String bedrockIdentifier;
    ItemDefinition bedrockDefinition;
    int bedrockData;

    
    BlockDefinition bedrockBlockDefinition;

    String toolType;

    String translationString;

    
    @Nullable
    SortedSetMultimap<Key, GeyserCustomMappingData> customItemDefinitions;

    @Builder.Default
    boolean containsV1Mappings = false;

    @NonNull
    Item javaItem;

    
    public boolean isBlock() {
        return this.bedrockBlockDefinition != null;
    }

    
    public boolean hasTranslation() {
        return this.translationString != null;
    }

    
    public boolean isTool() {
        return this.toolType != null;
    }
}
