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

package org.geysermc.connector.entity.living.animal.horse;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.MobArmorEquipmentPacket;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;

public class LlamaEntity extends ChestedHorseEntity {

    public LlamaEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        metadata.put(EntityData.CONTAINER_STRENGTH_MODIFIER, 3); // Presumably 3 slots for every 1 strength
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        // Strength
        if (entityMetadata.getId() == 19) {
            metadata.put(EntityData.STRENGTH, entityMetadata.getValue());
        }
        // Color equipped on the llama
        if (entityMetadata.getId() == 20) {
            // Bedrock treats llama decoration as armor
            MobArmorEquipmentPacket equipmentPacket = new MobArmorEquipmentPacket();
            equipmentPacket.setRuntimeEntityId(getGeyserId());
            // -1 means no armor
            if ((int) entityMetadata.getValue() != -1) {
                // The damage value is the dye color that Java sends us
                // The item is always going to be a carpet
                equipmentPacket.setChestplate(ItemData.builder()
                        .id(BlockTranslator.CARPET)
                        .damage((int) entityMetadata.getValue())
                        .count(1)
                        .build());
            } else {
                equipmentPacket.setChestplate(ItemData.AIR);
            }
            // Required to fill out the rest of the equipment or Bedrock ignores it, including above else statement if removing armor
            equipmentPacket.setBoots(ItemData.AIR);
            equipmentPacket.setHelmet(ItemData.AIR);
            equipmentPacket.setLeggings(ItemData.AIR);

            session.sendUpstreamPacket(equipmentPacket);
        }
        // Color of the llama
        if (entityMetadata.getId() == 21) {
            metadata.put(EntityData.VARIANT, entityMetadata.getValue());
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }
}
