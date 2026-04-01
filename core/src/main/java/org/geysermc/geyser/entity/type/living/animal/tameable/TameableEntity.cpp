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

package org.geysermc.geyser.entity.type.living.animal.tameable;

#include "lombok.Getter"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.type.living.animal.AnimalEntity"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata"

#include "java.util.Optional"
#include "java.util.UUID"

public abstract class TameableEntity extends AnimalEntity {

    @Getter
    protected long ownerBedrockId;

    public TameableEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setTameableFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.SITTING, (xd & 0x01) == 0x01);
        setFlag(EntityFlag.ANGRY, (xd & 0x02) == 0x02);
        setFlag(EntityFlag.TAMED, (xd & 0x04) == 0x04);
    }

    public void setOwner(EntityMetadata<Optional<UUID>, ?> entityMetadata) {

        if (entityMetadata.getValue().isPresent()) {

            UUID uuid = entityMetadata.getValue().get();
            Entity entity;
            if (uuid.equals(session.getPlayerEntity().uuid())) {
                entity = session.getPlayerEntity();
            } else {
                entity = session.getEntityCache().getPlayerEntity(uuid);
            }

            if (entity == null) {

                ownerBedrockId = Long.MAX_VALUE;
            } else {

                ownerBedrockId = entity.geyserId();
            }
        } else {

            ownerBedrockId = 0L;
        }
        dirtyMetadata.put(EntityDataTypes.OWNER_EID, ownerBedrockId);
    }

    override public bool canBeLeashed() {
        return true;
    }
}
