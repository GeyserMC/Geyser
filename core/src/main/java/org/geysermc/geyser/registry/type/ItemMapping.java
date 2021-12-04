/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.geysermc.geyser.network.MinecraftProtocol;
import org.geysermc.geyser.registry.BlockRegistries;

import java.util.Set;

@Value
@Builder
@EqualsAndHashCode
public class ItemMapping {
    public static final ItemMapping AIR = new ItemMapping("minecraft:air", "minecraft:air", 0, 0, 0,
            BlockRegistries.BLOCKS.forVersion(MinecraftProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion()).getBedrockAirId(),
            64, null, null, null, 0, null);

    String javaIdentifier;
    String bedrockIdentifier;
    int javaId;
    int bedrockId;
    int bedrockData;

    /**
     * The Bedrock block runtime ID to render this item with. The specific state *does* matter in how this item is rendered and used as a crafting ingredient.
     * Required since 1.16.220.
     */
    int bedrockBlockId;
    int stackSize;

    String toolType;
    String toolTier;

    String translationString;

    int maxDamage;

    Set<String> repairMaterials;

    /**
     * Gets if this item is a block.
     *
     * @return if this item is a block
     */
    public boolean isBlock() {
        return this.bedrockBlockId != -1;
    }

    /**
     * Gets if this item has a translation string present.
     *
     * @return if this item has a translation string present
     */
    public boolean hasTranslation() {
        return this.translationString != null;
    }

    /**
     * Gets if this item is a tool.
     *
     * @return if this item is a tool
     */
    public boolean isTool() {
        return this.toolType != null;
    }
}