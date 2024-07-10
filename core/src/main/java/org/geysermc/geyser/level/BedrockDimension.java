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

/**
 * A data structure to represent what Bedrock believes are the height requirements for a specific dimension.
 * As of 1.18.30, biome count is representative of the height of the world, and out-of-bounds chunks can crash
 * the client.
 *
 * @param minY The minimum height Bedrock Edition will accept.
 * @param height The maximum chunk height Bedrock Edition will accept, from the lowest point to the highest.
 * @param doUpperHeightWarn whether to warn in the console if the Java dimension height exceeds Bedrock's.
 */
public record BedrockDimension(int minY, int height, boolean doUpperHeightWarn) {
    public static final BedrockDimension OVERWORLD = new BedrockDimension(-64, 384, true);
    public static final BedrockDimension THE_NETHER = new BedrockDimension(0, 128, false);
    public static final BedrockDimension THE_END = new BedrockDimension(0, 256, true);
}
