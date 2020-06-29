/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.world.block.entity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.*;
import org.geysermc.connector.entity.type.EntityType;

import java.util.ArrayList;
import java.util.List;

@BlockEntity(name = "MobSpawner", regex = "mob_spawner")
public class SpawnerBlockEntityTranslator extends BlockEntityTranslator {

    @Override
    public List<Tag<?>> translateTag(CompoundTag tag, int blockState) {
        List<Tag<?>> tags = new ArrayList<>();

        if (tag.get("MaxNearbyEntities") != null) {
            tags.add(new ShortTag("MaxNearbyEntities", (short) tag.get("MaxNearbyEntities").getValue()));
        }

        if (tag.get("RequiredPlayerRange") != null) {
            tags.add(new ShortTag("RequiredPlayerRange", (short) tag.get("RequiredPlayerRange").getValue()));
        }

        if (tag.get("SpawnCount") != null) {
            tags.add(new ShortTag("SpawnCount", (short) tag.get("SpawnCount").getValue()));
        }

        if (tag.get("MaxSpawnDelay") != null) {
            tags.add(new ShortTag("MaxSpawnDelay", (short) tag.get("MaxSpawnDelay").getValue()));
        }

        if (tag.get("Delay") != null) {
            tags.add(new ShortTag("Delay", (short) tag.get("Delay").getValue()));
        }

        if (tag.get("SpawnRange") != null) {
            tags.add(new ShortTag("SpawnRange", (short) tag.get("SpawnRange").getValue()));
        }

        if (tag.get("MinSpawnDelay") != null) {
            tags.add(new ShortTag("MinSpawnDelay", (short) tag.get("MinSpawnDelay").getValue()));
        }

        if (tag.get("SpawnData") != null) {
            CompoundTag spawnData = tag.get("SpawnData");
            String entityID = (String) spawnData.get("id").getValue();
            tags.add(new StringTag("EntityIdentifier", entityID));

            EntityType type = EntityType.getFromIdentifier(entityID);
            if (type != null) {
                tags.add(new FloatTag("DisplayEntityWidth", type.getWidth()));
                tags.add(new FloatTag("DisplayEntityHeight", type.getHeight()));
                tags.add(new FloatTag("DisplayEntityScale", 1.0f));
            }
        }

        tags.add(new StringTag("id", "MobSpawner"));
        tags.add(new ByteTag("isMovable", (byte) 1));

        return tags;
    }

    @Override
    public CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        return null;
    }

    @Override
    public com.nukkitx.nbt.tag.CompoundTag getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        CompoundTagBuilder tagBuilder = getConstantBedrockTag(bedrockId, x, y, z).toBuilder();
        tagBuilder.byteTag("isMovable", (byte) 1)
                .stringTag("id", "MobSpawner");

        return tagBuilder.buildRootTag();
    }
}
