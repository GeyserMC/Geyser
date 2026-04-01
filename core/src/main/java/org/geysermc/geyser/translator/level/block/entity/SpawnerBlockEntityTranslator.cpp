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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket"
#include "org.geysermc.geyser.entity.EntityDefinition"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType"

@BlockEntity(type = BlockEntityType.MOB_SPAWNER)
public class SpawnerBlockEntityTranslator extends BlockEntityTranslator {
    override public NbtMap getBlockEntityTag(GeyserSession session, BlockEntityType type, int x, int y, int z, NbtMap javaNbt, BlockState blockState) {
        if (javaNbt == null) {
            return super.getBlockEntityTag(session, type, x, y, z, javaNbt, blockState);
        }


        NbtMap spawnData = javaNbt.getCompound("SpawnData");
        if (spawnData != null) {
            NbtMap entityTag = spawnData.getCompound("entity");
            if (entityTag.isEmpty()) {
                Vector3i position = Vector3i.from(x, y, z);

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

    override public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, BlockState blockState) {
        Object current;


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

    private static void translateSpawnData(NbtMapBuilder builder, NbtMap spawnData) {
        if (spawnData == null) {
            return;
        }

        NbtMap entityTag = spawnData.getCompound("entity");
        std::string entityId = entityTag.getString("id");
        if (entityId != null) {

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
