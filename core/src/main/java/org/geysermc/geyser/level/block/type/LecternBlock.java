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

package org.geysermc.geyser.level.block.type;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.BedrockChunkWantsBlockEntityTag;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;

import java.util.Collections;

public class LecternBlock extends Block implements BedrockChunkWantsBlockEntityTag {
    public LecternBlock(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public NbtMap createTag(GeyserSession session, Vector3i position, BlockState blockState) {
        NbtMapBuilder builder = getBaseLecternTag(position,
                blockState.getValue(Properties.HAS_BOOK, false));

        GeyserImpl.getInstance().getLogger().error("Sending tag: " + builder.build());
        return builder.build();
    }

    public static NbtMapBuilder getBaseLecternTag(Vector3i position, boolean hasBook) {
        if (hasBook) {
            return getBaseLecternTag(position, 0);
        } else {
            return getBaseLecternTag(position, 1)
                    .putCompound("book", NbtMap.builder()
                        .putByte("Count", (byte) 1)
                        .putShort("Damage", (short) 0)
                        .putString("Name", "minecraft:writable_book")
                        .putCompound("tag", NbtMap.builder().putList("pages", NbtType.COMPOUND, Collections.singletonList(
                                NbtMap.builder()
                                        .putString("photoname", "")
                                        .putString("text", "")
                                        .build()
                        )).build())
                    .build());
        }
    }

    public static NbtMapBuilder getBaseLecternTag(Vector3i position, int pages) {
        NbtMapBuilder builder = BlockEntityTranslator.getConstantBedrockTag("Lectern", position);
        builder.putBoolean("isMovable", true);

        if (pages != 0) {
            builder.putByte("hasBook", (byte) 1);
            builder.putInt("totalPages", 1); // we'll override it anyway
        }

        return builder;
    }
}
