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

package org.geysermc.connector.network.translators.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.geysermc.connector.network.translators.world.block.BlockTranslator1_17_0;

@Getter
@AllArgsConstructor
@ToString
public class ItemEntry {

    public static ItemEntry AIR = new ItemEntry("minecraft:air", "minecraft:air", 0, 0, 0,
            BlockTranslator1_17_0.INSTANCE.getBedrockAirId(), 64);

    private final String javaIdentifier;
    private final String bedrockIdentifier;
    private final int javaId;
    private final int bedrockId;
    private final int bedrockData;
    /**
     * The Bedrock block runtime ID to render this item with. The specific state *does* matter in how this item is rendered and used as a crafting ingredient.
     * Required since 1.16.220.
     */
    private final int bedrockBlockId;
    private final int stackSize;

    public boolean isBlock() {
        return bedrockBlockId != -1;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ItemEntry && ((ItemEntry) obj).getBedrockId() == this.getBedrockId() && ((ItemEntry) obj).getJavaIdentifier().equals(this.getJavaIdentifier()));
    }
}
