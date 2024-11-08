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
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;

import java.util.UUID;

public class HoglinEntity extends AnimalEntity {
    private boolean isImmuneToZombification;

    public HoglinEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
        dirtyMetadata.put(EntityDataTypes.TARGET_EID, session.getPlayerEntity().getGeyserId());
        setFlag(EntityFlag.SHAKING, isShaking());
    }

    public void setImmuneToZombification(BooleanEntityMetadata entityMetadata) {
        // Apply shaking effect if not in the nether and zombification is possible
        this.isImmuneToZombification = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.SHAKING, isShaking());
    }

    @Override
    protected boolean isShaking() {
        return (!isImmuneToZombification && !session.getDimensionType().piglinSafe()) || super.isShaking();
    }

    @Override
    @Nullable
    protected Tag<Item> getFoodTag() {
        return ItemTag.HOGLIN_FOOD;
    }

    @Override
    public boolean canBeLeashed() {
        return isNotLeashed();
    }

    @Override
    protected boolean isEnemy() {
        return true;
    }

    @Override
    public boolean useArmSwingAttack() {
        return true;
    }
}
