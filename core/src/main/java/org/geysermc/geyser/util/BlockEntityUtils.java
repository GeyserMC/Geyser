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

import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.BedrockOnlyBlockEntity;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.translator.level.block.entity.FlowerPotBlockEntityTranslator;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BlockEntityUtils {
    /**
     * A list of all block entities that require the Java block state in order to fill out their block entity information.
     * This list will be smaller with cache sections on as we don't need to double-cache data
     */
    public static final List<BedrockOnlyBlockEntity> BEDROCK_ONLY_BLOCK_ENTITIES = List.of(
            (BedrockOnlyBlockEntity) Registries.BLOCK_ENTITIES.get().get(BlockEntityType.CHEST),
            new FlowerPotBlockEntityTranslator()
    );

    /**
     * Contains a list of irregular block entity name translations that can't be fit into the regex
     */
    public static final Map<BlockEntityType, String> BLOCK_ENTITY_TRANSLATIONS = Map.of(
            // Bedrock/Java differences
            BlockEntityType.ENCHANTING_TABLE, "EnchantTable",
            BlockEntityType.JIGSAW, "JigsawBlock",
            BlockEntityType.PISTON, "PistonArm",
            BlockEntityType.TRAPPED_CHEST, "Chest"
            // There are some legacy IDs sent but as far as I can tell they are not needed for things to work properly
    );

    public static String getBedrockBlockEntityId(BlockEntityType type) {
        // These are the only exceptions when it comes to block entity ids
        String value = BLOCK_ENTITY_TRANSLATIONS.get(type);
        if (value != null) {
            return value;
        }

        String id = type.name();
        // Split at every space or capital letter - for the latter, some legacy Java block entity tags are the correct format already
        String[] words = id.split("_");
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase(Locale.ROOT) + words[i].substring(1).toLowerCase(Locale.ROOT);
        }

        return String.join("", words);
    }

    public static BlockEntityTranslator getBlockEntityTranslator(BlockEntityType type) {
         return Registries.BLOCK_ENTITIES.get(type);
    }

    public static void updateBlockEntity(GeyserSession session, @NonNull NbtMap blockEntity, Vector3i position) {
        BlockEntityDataPacket blockEntityPacket = new BlockEntityDataPacket();
        blockEntityPacket.setBlockPosition(position);
        blockEntityPacket.setData(blockEntity);
        session.sendUpstreamPacket(blockEntityPacket);
    }
}
