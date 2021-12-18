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

package org.geysermc.geyser.entity.type.living.animal;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.UUID;

public class PandaEntity extends AnimalEntity {
    private int mainGene;
    private int hiddenGene;

    public PandaEntity(GeyserSession session, long entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void setEatingCounter(IntEntityMetadata entityMetadata) {
        int count = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.EATING, count > 0);
        dirtyMetadata.put(EntityData.EATING_COUNTER, count);
        if (count != 0) {
            // Particles and sound
            EntityEventPacket packet = new EntityEventPacket();
            packet.setRuntimeEntityId(geyserId);
            packet.setType(EntityEventType.EATING_ITEM);
            packet.setData(session.getItemMappings().getStoredItems().bamboo().getBedrockId() << 16);
            session.sendUpstreamPacket(packet);
        }
    }

    public void setMainGene(ByteEntityMetadata entityMetadata) {
        mainGene = entityMetadata.getPrimitiveValue();
        updateAppearance();
    }

    public void setHiddenGene(ByteEntityMetadata entityMetadata) {
        hiddenGene = entityMetadata.getPrimitiveValue();
        updateAppearance();
    }

    public void setPandaFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.SNEEZING, (xd & 0x02) == 0x02);
        setFlag(EntityFlag.ROLLING, (xd & 0x04) == 0x04);
        setFlag(EntityFlag.SITTING, (xd & 0x08) == 0x08);
        // Required to put these both for sitting to actually show
        dirtyMetadata.put(EntityData.SITTING_AMOUNT, (xd & 0x08) == 0x08 ? 1f : 0f);
        dirtyMetadata.put(EntityData.SITTING_AMOUNT_PREVIOUS, (xd & 0x08) == 0x08 ? 1f : 0f);
        setFlag(EntityFlag.LAYING_DOWN, (xd & 0x10) == 0x10);
    }

    @Override
    public boolean canEat(String javaIdentifierStripped, ItemMapping mapping) {
        return javaIdentifierStripped.equals("bamboo");
    }

    /**
     * Update the panda's appearance, and take into consideration the recessive brown and weak traits that only show up
     * when both main and hidden genes match
     */
    private void updateAppearance() {
        if (mainGene == 4 || mainGene == 5) {
            // Main gene is a recessive trait
            if (mainGene == hiddenGene) {
                // Main and hidden genes match; this is what the panda looks like.
                dirtyMetadata.put(EntityData.VARIANT, mainGene);
            } else {
                // Genes have no effect on appearance
                dirtyMetadata.put(EntityData.VARIANT, 0);
            }
        } else {
            // No need to worry about hidden gene
            dirtyMetadata.put(EntityData.VARIANT, mainGene);
        }
    }
}
