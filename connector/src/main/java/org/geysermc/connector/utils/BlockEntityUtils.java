/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.entity.BlockEntityTranslator;

public class BlockEntityUtils {

    private static final BlockEntityTranslator EMPTY_TRANSLATOR = BlockEntityTranslator.BLOCK_ENTITY_TRANSLATORS.get("Empty");

    public static String getBedrockBlockEntityId(String id) {
        // These are the only exceptions when it comes to block entity ids
        if (BlockEntityTranslator.BLOCK_ENTITY_TRANSLATIONS.containsKey(id)) {
            return BlockEntityTranslator.BLOCK_ENTITY_TRANSLATIONS.get(id);
        }

        id = id.replace("minecraft:", "")
            .replace("_", " ");
        // Split at every space or capital letter - for the latter, some legacy Java block entity tags are the correct format already
        String[] words;
        if (!id.toUpperCase().equals(id)) { // Otherwise we get [S, K, U, L, L]
            words = id.split("(?=[A-Z])| "); // Split at every space or note or before every capital letter
        } else {
            words = id.split(" ");
        }
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
        }

        return String.join("", words);
    }

    public static BlockEntityTranslator getBlockEntityTranslator(String name) {
        BlockEntityTranslator blockEntityTranslator = BlockEntityTranslator.BLOCK_ENTITY_TRANSLATORS.get(name);
        if (blockEntityTranslator == null) {
            return EMPTY_TRANSLATOR;
        }

        return blockEntityTranslator;
    }

    public static void updateBlockEntity(GeyserSession session, NbtMap blockEntity, Position position) {
        updateBlockEntity(session, blockEntity, Vector3i.from(position.getX(), position.getY(), position.getZ()));
    }

    public static void updateBlockEntity(GeyserSession session, NbtMap blockEntity, Vector3i position) {
        BlockEntityDataPacket blockEntityPacket = new BlockEntityDataPacket();
        blockEntityPacket.setBlockPosition(position);
        blockEntityPacket.setData(blockEntity);
        session.sendUpstreamPacket(blockEntityPacket);
    }
}
