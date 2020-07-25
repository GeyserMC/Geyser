/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.entity.living.animal.tameable;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class WolfEntity extends TameableEntity {

    public WolfEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        //Reset wolf color
        if (entityMetadata.getId() == 16) {
            byte xd = (byte) entityMetadata.getValue();
            boolean angry = (xd & 0x02) == 0x02;
            if (angry) {
                metadata.put(EntityData.COLOR, (byte) 0);
            }
        }

        // "Begging" on wiki.vg, "Interested" in Nukkit - the tilt of the head
        if (entityMetadata.getId() == 18) {
            metadata.getFlags().setFlag(EntityFlag.INTERESTED, (boolean) entityMetadata.getValue());
        }

        // Wolf collar color
        // Relies on EntityData.OWNER_EID being set in TameableEntity.java
        if (entityMetadata.getId() == 19 && !metadata.getFlags().getFlag(EntityFlag.ANGRY)) {
            metadata.put(EntityData.COLOR, (byte) (int) entityMetadata.getValue());
        }

        // Wolf anger (1.16+)
        if (entityMetadata.getId() == 20) {
            metadata.getFlags().setFlag(EntityFlag.ANGRY, (int) entityMetadata.getValue() != 0);
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }
}
