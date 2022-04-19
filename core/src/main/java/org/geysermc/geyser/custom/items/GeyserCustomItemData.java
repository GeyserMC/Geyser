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

package org.geysermc.geyser.custom.items;

import com.nukkitx.protocol.bedrock.data.inventory.ComponentItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import org.geysermc.geyser.api.custom.items.CustomItemData;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.HashMap;
import java.util.Map;

public class GeyserCustomItemData {
    private Map<Integer, Mapping> mappings = new HashMap<>();

    public void addMapping(Integer protocolVersion, Mapping mapping) {
        if (protocolVersion == null || mapping == null) {
            throw new IllegalArgumentException("Protocol version and mapping cannot be null");
        }

        mappings.put(protocolVersion, mapping);
    }

    public Mapping getMapping(Integer protocolVersion) {
        if (protocolVersion == null) {
            throw new IllegalArgumentException("Protocol version cannot be null");
        }

        return mappings.get(protocolVersion);
    }

    public Mapping[] getMappings() {
        return mappings.values().toArray(new Mapping[0]);
    }

    public int mappingNumber() {
        return mappings.size();
    }

    public record Mapping(ComponentItemData componentItemData, ItemMapping itemMapping, StartGamePacket.ItemEntry startGamePacketItemEntry, String stringId, int integerId) {
    }
}
