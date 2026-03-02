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

package org.geysermc.geyser.entity.type.living.animal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;

import java.util.OptionalInt;

public class FrogEntity extends AnimalEntity implements VariantIntHolder {
    public FrogEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    public void setPose(Pose pose) {
        setFlag(EntityFlag.JUMP_GOAL_JUMP, pose == Pose.LONG_JUMPING);
        setFlag(EntityFlag.CROAKING, pose == Pose.CROAKING);
        setFlag(EntityFlag.EAT_MOB, pose == Pose.USING_TONGUE);

        super.setPose(pose);
    }

    @Override
    public JavaRegistryKey<BuiltInVariant> variantRegistry() {
        return JavaRegistries.FROG_VARIANT;
    }

    @Override
    public void setBedrockVariantId(int bedrockId) {
        dirtyMetadata.put(EntityDataTypes.VARIANT, bedrockId);
    }

    public void setTongueTarget(ObjectEntityMetadata<OptionalInt> entityMetadata) {
        OptionalInt entityId = entityMetadata.getValue();
        if (entityId.isPresent()) {
            Entity entity = session.getEntityCache().getEntityByJavaId(entityId.getAsInt());
            if (entity != null) {
                dirtyMetadata.put(EntityDataTypes.TARGET_EID, entity.geyserId());
            }
        } else {
            dirtyMetadata.put(EntityDataTypes.TARGET_EID, 0L);
        }
    }

    @Override
    @Nullable
    protected Tag<Item> getFoodTag() {
        return ItemTag.FROG_FOOD;
    }

    // Ordered by bedrock id
    // TODO: are these ordered correctly?
    public enum BuiltInVariant implements BuiltIn {
        TEMPERATE,
        COLD,
        WARM
    }
}
