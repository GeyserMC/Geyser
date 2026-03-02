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

import lombok.ToString;

/**
 * A data structure to represent what Bedrock believes are the height requirements for a specific dimension.
 * As of 1.18.30, biome count is representative of the height of the world, and out-of-bounds chunks can crash
 * the client.
 */
@ToString
public class BedrockDimension {

    public static final int OVERWORLD_ID = 0;
    public static final int DEFAULT_NETHER_ID = 1;
    public static final int END_ID = 2;

    // Changes if the above-bedrock Nether building workaround is applied
    public static int BEDROCK_NETHER_ID = DEFAULT_NETHER_ID;

    public static final BedrockDimension OVERWORLD = new BedrockDimension(-64, 384, true, OVERWORLD_ID);
    public static final BedrockDimension THE_NETHER = new BedrockDimension(0, 128, false, -1) {
        @Override
        public int bedrockId() {
            return BEDROCK_NETHER_ID;
        }
    };
    public static final BedrockDimension THE_END = new BedrockDimension(0, 256, true, END_ID);
    public static final String NETHER_IDENTIFIER = "minecraft:the_nether";

    private final int minY;
    private final int height;
    private final boolean doUpperHeightWarn;
    private final int bedrockId;

    /**
     * @param minY The minimum height Bedrock Edition will accept.
     * @param height The maximum chunk height Bedrock Edition will accept, from the lowest point to the highest.
     * @param doUpperHeightWarn whether to warn in the console if the Java dimension height exceeds Bedrock's.
     * @param bedrockId the Bedrock dimension ID of this dimension.
     */
    public BedrockDimension(int minY, int height, boolean doUpperHeightWarn, int bedrockId) {
        this.minY = minY;
        this.height = height;
        this.doUpperHeightWarn = doUpperHeightWarn;
        this.bedrockId = bedrockId;
    }

    /**
     * The Nether dimension in Bedrock does not permit building above Y128 - the Bedrock above the dimension.
     * This workaround sets the Nether as the End dimension to ignore this limit.
     *
     * @param isAboveNetherBedrockBuilding true if we should apply The End workaround
     */
    public static void changeBedrockNetherId(boolean isAboveNetherBedrockBuilding) {
        // Change dimension ID to the End to allow for building above Bedrock
        BEDROCK_NETHER_ID = isAboveNetherBedrockBuilding ? END_ID : DEFAULT_NETHER_ID;
    }

    public static boolean isCustomBedrockNetherId() {
        return BEDROCK_NETHER_ID == END_ID;
    }

    public int maxY() {
        return minY + height;
    }

    public int minY() {
        return minY;
    }

    public int height() {
        return height;
    }

    public boolean doUpperHeightWarn() {
        return doUpperHeightWarn;
    }

    public int bedrockId() {
        return bedrockId;
    }

}
