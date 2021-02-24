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

package org.geysermc.connector.network.translators.world.block;

import com.google.common.collect.ImmutableSet;
import com.nukkitx.nbt.NbtMapBuilder;

import java.util.Set;

public class BlockTranslator1_16_100 extends BlockTranslator {
    private static final Set<String> CORRECTED_STATES = ImmutableSet.of("minecraft:stripped_warped_stem",
            "minecraft:stripped_warped_hyphae", "minecraft:stripped_crimson_stem", "minecraft:stripped_crimson_hyphae");

    public static final BlockTranslator1_16_100 INSTANCE = new BlockTranslator1_16_100();

    public BlockTranslator1_16_100() {
        super("bedrock/blockpalette.1_16_100.nbt");
    }

    @Override
    public int getBlockStateVersion() {
        return 17825808;
    }

    @Override
    protected NbtMapBuilder adjustBlockStateForVersion(String bedrockIdentifier, NbtMapBuilder statesBuilder) {
        if (CORRECTED_STATES.contains(bedrockIdentifier)) {
            statesBuilder.putInt("deprecated", 0);
        }
        return super.adjustBlockStateForVersion(bedrockIdentifier, statesBuilder);
    }

    public static void init() {
        // no-op
    }
}
