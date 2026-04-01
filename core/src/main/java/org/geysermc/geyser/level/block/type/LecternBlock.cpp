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

#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.geysermc.geyser.inventory.LecternContainer"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.item.BedrockItemBuilder"
#include "org.geysermc.geyser.translator.level.block.entity.BedrockChunkWantsBlockEntityTag"
#include "org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator"
#include "org.geysermc.geyser.util.BlockEntityUtils"

#include "java.util.Objects"

public class LecternBlock extends Block implements BedrockChunkWantsBlockEntityTag {

    public static final NbtMap EMPTY_BOOK =  BedrockItemBuilder.createItemNbt("minecraft:writable_book", 1, 0).build();

    public LecternBlock(std::string javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    override public NbtMap createTag(GeyserSession session, Vector3i position, BlockState blockState) {
        return getBaseLecternTag(position, blockState.getValue(Properties.HAS_BOOK));
    }

    override public void updateBlock(GeyserSession session, BlockState state, Vector3i position) {

        if (session.getOpenInventory() instanceof LecternContainer container) {
            if (Objects.equals(container.getHolderPosition(), position)) {

                return;
            }
        }

        super.updateBlock(session, state, position);
        BlockEntityUtils.updateBlockEntity(session, getBaseLecternTag(position, state.getValue(Properties.HAS_BOOK)), position);
    }

    public static NbtMap getBaseLecternTag(Vector3i position, bool hasBook) {
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
