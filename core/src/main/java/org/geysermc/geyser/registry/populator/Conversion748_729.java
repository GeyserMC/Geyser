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

import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;

import java.util.List;

public class Conversion748_729 {

    private static final List<String> NEW_PLAYER_HEADS = List.of("minecraft:skeleton_skull", "minecraft:wither_skeleton_skull", "minecraft:zombie_head", "minecraft:player_head", "minecraft:creeper_head", "minecraft:dragon_head", "minecraft:piglin_head");

    static GeyserMappingItem remapItem(Item item, GeyserMappingItem mapping) {
        String identifier = mapping.getBedrockIdentifier();
        if (!NEW_PLAYER_HEADS.contains(identifier)) {
            return mapping;
        }
        switch (identifier) {
            case "minecraft:skeleton_skull" -> {
                return mapping.withBedrockIdentifier("minecraft:skull")
                    .withBedrockData(0);
            }
            case "minecraft:wither_skeleton_skull" -> {
                return mapping.withBedrockIdentifier("minecraft:skull")
                    .withBedrockData(1);
            }
            case "minecraft:zombie_head" -> {
                return mapping.withBedrockIdentifier("minecraft:skull")
                    .withBedrockData(2);
            }
            case "minecraft:player_head" -> {
                return mapping.withBedrockIdentifier("minecraft:skull")
                    .withBedrockData(3);
            }
            case "minecraft:creeper_head" -> {
                return mapping.withBedrockIdentifier("minecraft:skull")
                    .withBedrockData(4);
            }
            case "minecraft:dragon_head" -> {
                return mapping.withBedrockIdentifier("minecraft:skull")
                    .withBedrockData(5);
            }
            case "minecraft:piglin_head" -> {
                return mapping.withBedrockIdentifier("minecraft:skull")
                    .withBedrockData(6);
            }
        }
        return mapping;
    }

}
