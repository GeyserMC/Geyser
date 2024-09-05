/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;

@BlockEntity(type = BlockEntityType.TRIAL_SPAWNER)
public class TrialSpawnerBlockEntityTranslator extends BlockEntityTranslator {
    // Note that it would appear block entity updates don't include the NBT, but we do need it on chunk load.
    @Override
    public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, BlockState blockState) {
        if (javaNbt == null) {
            return;
        }

        NbtMap entityData = javaNbt.getCompound("spawn_data").getCompound("entity");
        if (entityData.isEmpty()) {
            return;
        }
        NbtMapBuilder spawnData = NbtMap.builder();
        EntityDefinition<?> definition = Registries.JAVA_ENTITY_IDENTIFIERS.get(entityData.getString("id"));
        if (definition != null) {
            spawnData.putString("TypeId", definition.identifier());
        }
        spawnData.putInt("Weight", entityData.getInt("Size", 1)); // ??? presumably since these are the only other two extra attributes
        bedrockNbt.putCompound("spawn_data", spawnData.build());
    }
}
