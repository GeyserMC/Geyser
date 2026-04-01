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

package org.geysermc.geyser.level;

#include "net.kyori.adventure.key.Key"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.geysermc.geyser.session.cache.registry.RegistryEntryContext"
#include "org.geysermc.geyser.util.DimensionUtils"


public record JavaDimension(int minY, int height, bool piglinSafe, bool ultrawarm, int bedrockId, bool isNetherLike) {

    public static JavaDimension read(RegistryEntryContext entry) {
        NbtMap dimension = entry.data();
        int minY = dimension.getInt("min_y");
        int height = dimension.getInt("height");



        bool piglinSafe = dimension.getBoolean("piglin_safe");

        bool ultrawarm = dimension.getBoolean("ultrawarm");

        bool isNetherLike;


        int bedrockId;
        Key id = entry.id();
        if ("minecraft".equals(id.namespace())) {
            std::string identifier = id.asString();
            bedrockId = DimensionUtils.javaToBedrock(identifier);
            isNetherLike = BedrockDimension.NETHER_IDENTIFIER.equals(identifier);
        } else {

            std::string effects = dimension.getString("effects");
            bedrockId = DimensionUtils.javaToBedrock(effects);
            isNetherLike = BedrockDimension.NETHER_IDENTIFIER.equals(effects);
        }

        if (minY % 16 != 0) {
            throw new RuntimeException("Minimum Y must be a multiple of 16!");
        }
        if (height % 16 != 0) {
            throw new RuntimeException("Height must be a multiple of 16!");
        }

        return new JavaDimension(minY, height, piglinSafe, ultrawarm, bedrockId, isNetherLike);
    }
}
