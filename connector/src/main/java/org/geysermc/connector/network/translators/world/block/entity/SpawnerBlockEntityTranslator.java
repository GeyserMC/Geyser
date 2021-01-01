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

package org.geysermc.connector.network.translators.world.block.entity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.nbt.NbtMapBuilder;
import org.geysermc.connector.entity.type.EntityType;

@BlockEntity(name = "MobSpawner")
public class SpawnerBlockEntityTranslator extends BlockEntityTranslator {
    @Override
    public void translateTag(NbtMapBuilder builder, CompoundTag tag, int blockState) {
        Tag current;

        if ((current = tag.get("MaxNearbyEntities")) != null) {
            builder.put("MaxNearbyEntities", current.getValue());
        }

        if ((current = tag.get("RequiredPlayerRange")) != null) {
            builder.put("RequiredPlayerRange", current.getValue());
        }

        if ((current = tag.get("SpawnCount")) != null) {
            builder.put("SpawnCount", current.getValue());
        }

        if ((current = tag.get("MaxSpawnDelay")) != null) {
            builder.put("MaxSpawnDelay", current.getValue());
        }

        if ((current = tag.get("Delay")) != null) {
            builder.put("Delay", current.getValue());
        }

        if ((current = tag.get("SpawnRange")) != null) {
            builder.put("SpawnRange", current.getValue());
        }

        if ((current = tag.get("MinSpawnDelay")) != null) {
            builder.put("MinSpawnDelay", current.getValue());
        }

        CompoundTag spawnData = tag.get("SpawnData");
        if (spawnData != null) {
            String entityID = (String) spawnData.get("id").getValue();
            builder.put("EntityIdentifier", entityID);

            EntityType type = EntityType.getFromIdentifier(entityID);
            if (type != null) {
                builder.put("DisplayEntityWidth", type.getWidth());
                builder.put("DisplayEntityHeight", type.getHeight());
                builder.put("DisplayEntityScale", 1.0f);
            }
        }

        builder.put("id", "MobSpawner");
        builder.put("isMovable", (byte) 1);
    }
}
