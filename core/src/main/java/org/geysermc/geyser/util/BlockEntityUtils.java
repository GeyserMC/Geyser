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

package org.geysermc.geyser.util;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.BedrockOnlyBlockEntity;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.translator.level.block.entity.FlowerPotBlockEntityTranslator;
import org.geysermc.geyser.registry.Registries;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class BlockEntityUtils {
    /**
     * A list of all block entities that require the Java block state in order to fill out their block entity information.
     * This list will be smaller with cache sections on as we don't need to double-cache data
     */
    public static final ObjectArrayList<BedrockOnlyBlockEntity> BEDROCK_ONLY_BLOCK_ENTITIES = new ObjectArrayList<>();

    /**
     * Contains a list of irregular block entity name translations that can't be fit into the regex
     */
    public static final Map<BlockEntityType, String> BLOCK_ENTITY_TRANSLATIONS = new HashMap<>() {
        {
            // Bedrock/Java differences
            put(BlockEntityType.ENCHANTING_TABLE, "EnchantTable");
            put(BlockEntityType.JIGSAW, "JigsawBlock");
            put(BlockEntityType.PISTON, "PistonArm");
            put(BlockEntityType.TRAPPED_CHEST, "Chest");
            // There are some legacy IDs sent but as far as I can tell they are not needed for things to work properly
        }
    };

    static {
        // Seeing as there are only two - and, hopefully, will only ever be two - we can hardcode this
        BEDROCK_ONLY_BLOCK_ENTITIES.add((BedrockOnlyBlockEntity) Registries.BLOCK_ENTITIES.get().get(BlockEntityType.CHEST));
        BEDROCK_ONLY_BLOCK_ENTITIES.add(new FlowerPotBlockEntityTranslator());
    }

    public static String getBedrockBlockEntityId(BlockEntityType type) {
        // These are the only exceptions when it comes to block entity ids
        String value = BLOCK_ENTITY_TRANSLATIONS.get(type);
        if (value != null) {
            return value;
        }

        String id = type.name();
        // Split at every space or capital letter - for the latter, some legacy Java block entity tags are the correct format already
        String[] words = id.split("_");;
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
        }

        return String.join("", words);
    }

    public static BlockEntityTranslator getBlockEntityTranslator(BlockEntityType type) {
         return Registries.BLOCK_ENTITIES.get(type);
    }

    public static void updateBlockEntity(GeyserSession session, @Nonnull NbtMap blockEntity, Position position) {
        updateBlockEntity(session, blockEntity, Vector3i.from(position.getX(), position.getY(), position.getZ()));
    }

    public static void updateBlockEntity(GeyserSession session, @Nonnull NbtMap blockEntity, Vector3i position) {
        BlockEntityDataPacket blockEntityPacket = new BlockEntityDataPacket();
        blockEntityPacket.setBlockPosition(position);
        blockEntityPacket.setData(blockEntity);
        session.sendUpstreamPacket(blockEntityPacket);
    }
}
