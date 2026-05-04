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
import org.geysermc.geyser.inventory.LecternContainer;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.geyser.translator.level.block.entity.BedrockChunkWantsBlockEntityTag;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.util.BlockEntityUtils;

import java.util.Objects;

public class LecternBlock extends Block implements BedrockChunkWantsBlockEntityTag {

    public static final NbtMap EMPTY_BOOK =  BedrockItemBuilder.createItemNbt("minecraft:writable_book", 1, 0).build();

    public LecternBlock(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public NbtMap createTag(GeyserSession session, Vector3i position, BlockState blockState) {
        return getBaseLecternTag(position, blockState.getValue(Properties.HAS_BOOK));
    }

    @Override
    public void updateBlock(GeyserSession session, BlockState state, Vector3i position) {
        // Prevent additional block updates while we're reading a book in the lectern
        if (session.getOpenInventory() instanceof LecternContainer container) {
            if (Objects.equals(container.getHolderPosition(), position)) {
                // We'll update the block once we close the lectern
                return;
            }
        }

        super.updateBlock(session, state, position);
        BlockEntityUtils.updateBlockEntity(session, getBaseLecternTag(position, state.getValue(Properties.HAS_BOOK)), position);
    }

    public static NbtMap getBaseLecternTag(Vector3i position, boolean hasBook) {
        if (hasBook) {
            return createLecternTag(position, EMPTY_BOOK, 0, 0);
        }
        return BlockEntityTranslator.getConstantBedrockTag("Lectern", position).build();
    }

    public static NbtMap createLecternTag(Vector3i position, NbtMap book, int page, int total) {
        NbtMapBuilder builder = BlockEntityTranslator.getConstantBedrockTag("Lectern", position);
        if (book != null) {
            builder.putCompound("book", book);
            builder.putByte("hasBook", (byte) 1);
            builder.putInt("page", page);
            builder.putInt("totalPages", total);
        }

        return builder.build();
    }
}
