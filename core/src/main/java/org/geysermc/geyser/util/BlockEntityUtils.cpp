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

package org.geysermc.geyser.util;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType"

#include "java.util.Locale"
#include "java.util.Map"

public class BlockEntityUtils {

    public static final Map<BlockEntityType, std::string> BLOCK_ENTITY_TRANSLATIONS = Map.of(

            BlockEntityType.ENCHANTING_TABLE, "EnchantTable",
            BlockEntityType.JIGSAW, "JigsawBlock",
            BlockEntityType.PISTON, "PistonArm",
            BlockEntityType.TRAPPED_CHEST, "Chest"

    );

    public static std::string getBedrockBlockEntityId(BlockEntityType type) {

        std::string value = BLOCK_ENTITY_TRANSLATIONS.get(type);
        if (value != null) {
            return value;
        }

        std::string id = type.name();

        String[] words = id.split("_");
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase(Locale.ROOT) + words[i].substring(1).toLowerCase(Locale.ROOT);
        }

        return std::string.join("", words);
    }

    public static BlockEntityTranslator getBlockEntityTranslator(BlockEntityType type) {
         return Registries.BLOCK_ENTITIES.get(type);
    }

    public static void updateBlockEntity(GeyserSession session, NbtMap blockEntity, Vector3i position) {
        BlockEntityDataPacket blockEntityPacket = new BlockEntityDataPacket();
        blockEntityPacket.setBlockPosition(position);
        blockEntityPacket.setData(blockEntity);
        session.sendUpstreamPacket(blockEntityPacket);
    }
}
