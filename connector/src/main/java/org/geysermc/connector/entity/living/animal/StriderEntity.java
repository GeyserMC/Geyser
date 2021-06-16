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

package org.geysermc.connector.entity.living.animal;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemEntry;

public class StriderEntity extends AnimalEntity {

    private boolean shaking = false;

    public StriderEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        metadata.getFlags().setFlag(EntityFlag.FIRE_IMMUNE, true);
        metadata.getFlags().setFlag(EntityFlag.BREATHING, true);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 18) {
            shaking = (boolean) entityMetadata.getValue();
        }
        if (entityMetadata.getId() == 19) {
            metadata.getFlags().setFlag(EntityFlag.SADDLED, (boolean) entityMetadata.getValue());
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public void updateBedrockMetadata(GeyserSession session) {
        // Make sure they are not shaking when riding another entity
        // Needs to copy the parent state
        if (metadata.getFlags().getFlag(EntityFlag.RIDING)) {
            boolean parentShaking = false;
            for (Entity ent : session.getEntityCache().getEntities().values()) {
                if (ent.getPassengers().contains(entityId) && ent instanceof StriderEntity) {
                    parentShaking = ent.getMetadata().getFlags().getFlag(EntityFlag.SHAKING);
                    break;
                }
            }
    
            metadata.getFlags().setFlag(EntityFlag.BREATHING, !parentShaking);
            metadata.getFlags().setFlag(EntityFlag.SHAKING, parentShaking);
        } else {
            metadata.getFlags().setFlag(EntityFlag.BREATHING, !shaking);
            metadata.getFlags().setFlag(EntityFlag.SHAKING, shaking);
        }

        // Update the passengers if we have any
        for (long passenger : passengers) {
            Entity passengerEntity = session.getEntityCache().getEntityByJavaId(passenger);
            if (passengerEntity != null) {
                passengerEntity.updateBedrockMetadata(session);
            }
        }

        super.updateBedrockMetadata(session);
    }

    @Override
    public boolean canEat(GeyserSession session, String javaIdentifierStripped, ItemEntry itemEntry) {
        return javaIdentifierStripped.equals("warped_fungus");
    }
}
