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

package org.geysermc.connector.entity.living.monster;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class CreeperEntity extends MonsterEntity {

    /**
     * Whether the creeper has been ignited and is using ID 17.
     * In this instance we ignore ID 15 since it's sending us -1 which confuses poor Bedrock.
     */
    private boolean ignitedByFlintAndSteel = false;

    public CreeperEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 16) {
            if (!ignitedByFlintAndSteel) {
                metadata.getFlags().setFlag(EntityFlag.IGNITED, (int) entityMetadata.getValue() == 1);
            }
        }
        if (entityMetadata.getId() == 17) {
            metadata.getFlags().setFlag(EntityFlag.POWERED, (boolean) entityMetadata.getValue());
        }
        if (entityMetadata.getId() == 18) {
            ignitedByFlintAndSteel = (boolean) entityMetadata.getValue();
            metadata.getFlags().setFlag(EntityFlag.IGNITED, ignitedByFlintAndSteel);
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }
}
