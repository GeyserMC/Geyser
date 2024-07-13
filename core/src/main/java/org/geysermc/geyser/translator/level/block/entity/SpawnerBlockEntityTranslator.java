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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;

@BlockEntity(type = BlockEntityType.MOB_SPAWNER)
public class SpawnerBlockEntityTranslator extends BlockEntityTranslator {
    @Override
    public NbtMap getBlockEntityTag(GeyserSession session, BlockEntityType type, int x, int y, int z, @Nullable NbtMap javaNbt, BlockState blockState) {
        if (javaNbt == null) {
            return super.getBlockEntityTag(session, type, x, y, z, javaNbt, blockState);
        }
        // Sending an empty EntityIdentifier to empty the spawner is ignored by the client, so we send a whole new spawner!
        // Fixes https://github.com/GeyserMC/Geyser/issues/4214
        NbtMap spawnData = javaNbt.getCompound("SpawnData");
        if (spawnData != null) {
            NbtMap entityTag = spawnData.getCompound("entity");
            if (entityTag.isEmpty()) {
                Vector3i position = Vector3i.from(x, y, z);
                // Set to air and back to reset the spawner - "just" updating the spawner doesn't work
                UpdateBlockPacket emptyBlockPacket = new UpdateBlockPacket();
                emptyBlockPacket.setDataLayer(0);
                emptyBlockPacket.setBlockPosition(position);
                emptyBlockPacket.setDefinition(session.getBlockMappings().getBedrockAir());
                session.sendUpstreamPacket(emptyBlockPacket);

                UpdateBlockPacket spawnerBlockPacket = new UpdateBlockPacket();
                spawnerBlockPacket.setDataLayer(0);
                spawnerBlockPacket.setBlockPosition(position);
                spawnerBlockPacket.setDefinition(session.getBlockMappings().getMobSpawnerBlock());
                session.sendUpstreamPacket(spawnerBlockPacket);
            }
        }

        return super.getBlockEntityTag(session, type, x, y, z, javaNbt, blockState);
    }

    @Override
    public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, BlockState blockState) {
        Object current;

        // TODO use primitive get and put methods
        if ((current = javaNbt.get("MaxNearbyEntities")) != null) {
            bedrockNbt.put("MaxNearbyEntities", current);
        }

        if ((current = javaNbt.get("RequiredPlayerRange")) != null) {
            bedrockNbt.put("RequiredPlayerRange", current);
        }

        if ((current = javaNbt.get("SpawnCount")) != null) {
            bedrockNbt.put("SpawnCount", current);
        }

        if ((current = javaNbt.get("MaxSpawnDelay")) != null) {
            bedrockNbt.put("MaxSpawnDelay", current);
        }

        if ((current = javaNbt.get("Delay")) != null) {
            bedrockNbt.put("Delay", current);
        }

        if ((current = javaNbt.get("SpawnRange")) != null) {
            bedrockNbt.put("SpawnRange", current);
        }

        if ((current = javaNbt.get("MinSpawnDelay")) != null) {
            bedrockNbt.put("MinSpawnDelay", current);
        }

        translateSpawnData(bedrockNbt, javaNbt.getCompound("SpawnData", null));

        bedrockNbt.put("isMovable", (byte) 1);
    }

    private static void translateSpawnData(@NonNull NbtMapBuilder builder, @Nullable NbtMap spawnData) {
        if (spawnData == null) {
            return;
        }

        NbtMap entityTag = spawnData.getCompound("entity");
        String entityId = entityTag.getString("id");
        if (entityId != null) {
            // As of 1.19.3, spawners can be empty
            builder.put("EntityIdentifier", entityId);

            EntityDefinition<?> definition = Registries.JAVA_ENTITY_IDENTIFIERS.get(entityId);
            if (definition != null) {
                builder.putFloat("DisplayEntityWidth", definition.width());
                builder.putFloat("DisplayEntityHeight", definition.height());
                builder.putFloat("DisplayEntityScale", 1.0f);
            }
        }
    }
}
