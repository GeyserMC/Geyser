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

package org.geysermc.geyser.item.components;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.registry.BlockRegistries;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToolBreakSpeedsUtils {
    private static JsonNode toolBreakSpeeds = null;

    private static void initJsonNode() {
        if (toolBreakSpeeds != null) {
            return;
        }

        GeyserBootstrap bootstrap = GeyserImpl.getInstance().getBootstrap();
        try (InputStream stream = bootstrap.getResource("mappings/block_break_speeds.json")) {
            toolBreakSpeeds = GeyserImpl.JSON_MAPPER.readTree(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static NbtMap createBreakSpeed(float speed, String block) {
        return NbtMap.builder()
                .putCompound("block", NbtMap.builder()
                        .putString("name", block).build())
                .putCompound("on_dig", NbtMap.builder()
                        .putCompound("condition", NbtMap.builder()
                                .putString("expression", "")
                                .putInt("version", -1)
                                .build())
                        .putString("event", "tool_durability")
                        .putString("target", "self")
                        .build())
                .putInt("speed", (int) speed) //TODO: Figure out how to handle .5 speeds
                .build();
    }

    private static NbtMap createDigger(List<NbtMap> speeds) {
        return NbtMap.builder()
                .putList("destroy_speeds", NbtType.COMPOUND, speeds)
                .putCompound("on_dig", NbtMap.builder()
                        .putCompound("condition", NbtMap.builder()
                                .putString("expression", "")
                                .putInt("version", -1)
                                .build())
                        .putString("event", "tool_durability")
                        .putString("target", "self")
                        .build())
                .putBoolean("use_efficiency", true)
                .build();
    }

    public static NbtMap createDigger(String toolType, String toolTier) {
        initJsonNode();

        JsonNode tmpObject = toolBreakSpeeds.get(toolType);
        if (tmpObject == null) {
            return null;
        }

        if (toolTier != null) {
            tmpObject = tmpObject.get(toolTier);
            if (tmpObject == null) {
                return null;
            }
        }

        List<NbtMap> diggerSpeeds = new ArrayList<>();

        TypeReference<Map<String, Float>> mappingItemsType = new TypeReference<>() { };
        Map<String, Float> breakSpeeds = GeyserImpl.JSON_MAPPER.convertValue(tmpObject, mappingItemsType);

        for (Map.Entry<String, Float> entry : breakSpeeds.entrySet()) {
            String bedrockBlockId = BlockRegistries.JAVA_TO_BEDROCK_IDENTIFIERS.get(entry.getKey());
            diggerSpeeds.add(createBreakSpeed(entry.getValue(), bedrockBlockId));
        }

        return createDigger(diggerSpeeds);
    }
}
