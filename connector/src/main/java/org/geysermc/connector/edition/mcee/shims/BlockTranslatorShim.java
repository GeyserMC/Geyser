/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.edition.mcee.shims;

import com.fasterxml.jackson.databind.JsonNode;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.Getter;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;


@Getter
public class BlockTranslatorShim implements BlockTranslator.Shim {
    @Override
    public NbtMap buildBedrockState(JsonNode node) {
        NbtMapBuilder tagBuilder = NbtMap.builder();
        tagBuilder.putString("name", node.get("bedrock_identifier").textValue());

        tagBuilder = NbtMap.builder().putCompound("block", tagBuilder.build());

        // Add Meta tag
        if (node.has("meta")) {
            tagBuilder.putShort("meta", node.get("meta").shortValue());
        } else {
            tagBuilder.putShort("meta", (short) 0);
        }

        return tagBuilder.build();
    }
}
