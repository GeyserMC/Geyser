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

import io.jsonwebtoken.lang.Collections;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class Conversion766_748 {
    static List<String> newBlockIds = new ArrayList<>();
    static List<String> bedrockIds = new ArrayList<>(); // TODO temp remove
    static {
        var blocks = Collections.of(
            Blocks.PALE_OAK_WOOD,
            Blocks.PALE_OAK_PLANKS,
            Blocks.PALE_OAK_SAPLING,
            Blocks.PALE_OAK_LOG,
            Blocks.STRIPPED_PALE_OAK_LOG,
            Blocks.STRIPPED_PALE_OAK_WOOD,
            Blocks.PALE_OAK_LEAVES,
            Blocks.PALE_OAK_SIGN,
            Blocks.PALE_OAK_WALL_SIGN,
            Blocks.PALE_OAK_HANGING_SIGN,
            Blocks.PALE_OAK_WALL_HANGING_SIGN,
            Blocks.PALE_OAK_PRESSURE_PLATE,
            Blocks.PALE_OAK_TRAPDOOR,
            Blocks.POTTED_PALE_OAK_SAPLING,
            Blocks.PALE_OAK_BUTTON,
            Blocks.PALE_OAK_STAIRS,
            Blocks.PALE_OAK_SLAB,
            Blocks.PALE_OAK_FENCE_GATE,
            Blocks.PALE_OAK_FENCE,
            Blocks.PALE_OAK_DOOR,
            Blocks.PALE_MOSS_BLOCK,
            Blocks.PALE_MOSS_CARPET,
            Blocks.PALE_HANGING_MOSS,

            Blocks.OPEN_EYEBLOSSOM,
            Blocks.CLOSED_EYEBLOSSOM,
            Blocks.POTTED_OPEN_EYEBLOSSOM,
            Blocks.POTTED_CLOSED_EYEBLOSSOM,

            Blocks.RESIN_CLUMP,
            Blocks.RESIN_BLOCK,
            Blocks.RESIN_BRICKS,
            Blocks.RESIN_BRICK_STAIRS,
            Blocks.RESIN_BRICK_SLAB,
            Blocks.RESIN_BRICK_WALL,
            Blocks.CHISELED_RESIN_BRICKS,

            Blocks.CREAKING_HEART
        );

        blocks.forEach(block -> newBlockIds.add(block.javaIdentifier().value()));
    }

    static NbtMap remapBlock(NbtMap tag) {

        GeyserImpl.getInstance().getLogger().info(tag.toString());

        String name = tag.getString("name");
        if (newBlockIds.contains(name)) {
            bedrockIds.add(name);
            // TODO
            return tag.toBuilder()
                .putCompound("states", NbtMap.builder().build())
                .putString("name", "minecraft:unknown")
                .build();
        }

        if (name.contains("resin") || name.contains("creaking") || name.contains("pale")) {
            throw new RuntimeException("ya missed " + name);
        }

        return tag;
    }
}
