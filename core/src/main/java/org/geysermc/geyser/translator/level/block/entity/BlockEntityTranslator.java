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

package org.geysermc.geyser.translator.level.block.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.BlockEntityUtils;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;

/**
 * The class that all block entities (on both Java and Bedrock) should translate with
 */
public abstract class BlockEntityTranslator {
    protected BlockEntityTranslator() {
    }

    public abstract void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, BlockState blockState);

    public NbtMap getBlockEntityTag(GeyserSession session, BlockEntityType type, int x, int y, int z, @Nullable NbtMap javaNbt, BlockState blockState) {
        NbtMapBuilder tagBuilder = getConstantBedrockTag(type, x, y, z);
        if (javaNbt != null || this instanceof RequiresBlockState) {
            // Always process tags if the block state is part of the tag.
            // See: banner base colors.
            translateTag(session, tagBuilder, javaNbt, blockState);
        }
        return tagBuilder.build();
    }

    public static NbtMapBuilder getConstantBedrockTag(BlockEntityType type, int x, int y, int z) {
        return getConstantBedrockTag(BlockEntityUtils.getBedrockBlockEntityId(type), x, y, z);
    }

    public static NbtMapBuilder getConstantBedrockTag(String bedrockId, Vector3i position) {
        return getConstantBedrockTag(bedrockId, position.getX(), position.getY(), position.getZ());
    }

    public static NbtMapBuilder getConstantBedrockTag(String bedrockId, int x, int y, int z) {
        return NbtMap.builder()
                .putInt("x", x)
                .putInt("y", y)
                .putInt("z", z)
                .putString("id", bedrockId);
    }
}
