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
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;

public class PandaEntity extends AnimalEntity {

    private int mainGene;
    private int hiddenGene;

    public PandaEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 19) {
            metadata.getFlags().setFlag(EntityFlag.EATING, (int) entityMetadata.getValue() > 0);
            metadata.put(EntityData.EATING_COUNTER, entityMetadata.getValue());
            if ((int) entityMetadata.getValue() != 0) {
                // Particles and sound
                EntityEventPacket packet = new EntityEventPacket();
                packet.setRuntimeEntityId(geyserId);
                packet.setType(EntityEventType.EATING_ITEM);
                packet.setData(ItemRegistry.BAMBOO.getBedrockId() << 16);
                session.sendUpstreamPacket(packet);
            }
        }
        if (entityMetadata.getId() == 20) {
            mainGene = (int) (byte) entityMetadata.getValue();
            updateAppearance();
        }
        if (entityMetadata.getId() == 21) {
            hiddenGene = (int) (byte) entityMetadata.getValue();
            updateAppearance();
        }
        if (entityMetadata.getId() == 22) {
            byte xd = (byte) entityMetadata.getValue();
            metadata.getFlags().setFlag(EntityFlag.SNEEZING, (xd & 0x02) == 0x02);
            metadata.getFlags().setFlag(EntityFlag.ROLLING, (xd & 0x04) == 0x04);
            metadata.getFlags().setFlag(EntityFlag.SITTING, (xd & 0x08) == 0x08);
            // Required to put these both for sitting to actually show
            metadata.put(EntityData.SITTING_AMOUNT, (xd & 0x08) == 0x08 ? 1f : 0f);
            metadata.put(EntityData.SITTING_AMOUNT_PREVIOUS, (xd & 0x08) == 0x08 ? 1f : 0f);
            metadata.getFlags().setFlag(EntityFlag.LAYING_DOWN, (xd & 0x10) == 0x10);
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public boolean canEat(GeyserSession session, String javaIdentifierStripped, ItemEntry itemEntry) {
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
                metadata.put(EntityData.VARIANT, mainGene);
            } else {
                // Genes have no effect on appearance
                metadata.put(EntityData.VARIANT, 0);
            }
        } else {
            // No need to worry about hidden gene
            metadata.put(EntityData.VARIANT, mainGene);
        }
    }
}
