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

package org.geysermc.connector.entity.living.monster.raid;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class SpellcasterIllagerEntity extends AbstractIllagerEntity {
    private static final int SUMMON_VEX_PARTICLE_COLOR = (179 << 16) | (179 << 8) | 204;
    private static final int ATTACK_PARTICLE_COLOR = (102 << 16) | (77 << 8) | 89;
    private static final int WOLOLO_PARTICLE_COLOR = (179 << 16) | (128 << 8) | 51;

    public SpellcasterIllagerEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
        // OptionalPack usage
        metadata.getFlags().setFlag(EntityFlag.BRIBED, this.entityType == EntityType.ILLUSIONER);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 17) {
            int spellType = (int) (byte) entityMetadata.getValue();
            // Summon vex, attack, or wololo
            metadata.getFlags().setFlag(EntityFlag.CASTING, spellType == 1 || spellType == 2 || spellType == 3);
            int rgbData = 0;
            // Set the spell color based on Java values
            switch (spellType) {
                case 1:
                    rgbData = SUMMON_VEX_PARTICLE_COLOR;
                    break;
                case 2:
                    rgbData = ATTACK_PARTICLE_COLOR;
                    break;
                case 3:
                    rgbData = WOLOLO_PARTICLE_COLOR;
                    break;
            }
            metadata.put(EntityData.EVOKER_SPELL_COLOR, rgbData);
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }
}
