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
import com.google.common.collect.ImmutableSet;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.connector.entity.attribute.GeyserAttributeType;
import org.geysermc.connector.entity.living.animal.AnimalEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.registry.type.ItemMapping;

import java.util.Set;

public class AbstractHorseEntity extends AnimalEntity {
    /**
     * A list of all foods a horse/donkey can eat on Java Edition.
     * Used to display interactive tag if needed.
     */
    private static final Set<String> DONKEY_AND_HORSE_FOODS = ImmutableSet.of("golden_apple", "enchanted_golden_apple",
            "golden_carrot", "sugar", "apple", "wheat", "hay_block");

    public AbstractHorseEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        // Specifies the size of the entity's inventory. Required to place slots in the entity.
        metadata.put(EntityData.CONTAINER_BASE_SIZE, 2);

        metadata.getFlags().setFlag(EntityFlag.WASD_CONTROLLED, true);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        super.spawnEntity(session);

        // Add horse jump strength attribute to allow donkeys and mules to jump, if they don't send the attribute themselves.
        // Confirmed broken without this code by making a new donkey in vanilla 1.17.1
        // The spawn packet does have an attributes section, but adding the jump strength property there causes the
        // donkey to jump very high.
        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(geyserId);
        attributesPacket.getAttributes().add(GeyserAttributeType.HORSE_JUMP_STRENGTH.getAttribute(0.5f, 2));
        session.sendUpstreamPacket(attributesPacket);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 17) {
            byte xd = (byte) entityMetadata.getValue();
            boolean tamed = (xd & 0x02) == 0x02;
            boolean saddled = (xd & 0x04) == 0x04;
            metadata.getFlags().setFlag(EntityFlag.TAMED, tamed);
            metadata.getFlags().setFlag(EntityFlag.SADDLED, saddled);
            metadata.getFlags().setFlag(EntityFlag.EATING, (xd & 0x10) == 0x10);
            metadata.getFlags().setFlag(EntityFlag.STANDING, (xd & 0x20) == 0x20);

            // HorseFlags
            // Bred 0x10
            // Eating 0x20
            // Open mouth 0x80
            int horseFlags = 0x0;
            horseFlags = (xd & 0x40) == 0x40 ? horseFlags | 0x80 : horseFlags;

            // Only set eating when we don't have mouth open so a player interaction doesn't trigger the eating animation
            horseFlags = (xd & 0x10) == 0x10 && (xd & 0x40) != 0x40 ? horseFlags | 0x20 : horseFlags;

            // Set the flags into the display item
            metadata.put(EntityData.DISPLAY_ITEM, horseFlags);

            // Send the eating particles
            // We use the wheat metadata as static particles since Java
            // doesn't send over what item was used to feed the horse
            if ((xd & 0x40) == 0x40) {
                EntityEventPacket entityEventPacket = new EntityEventPacket();
                entityEventPacket.setRuntimeEntityId(geyserId);
                entityEventPacket.setType(EntityEventType.EATING_ITEM);
                entityEventPacket.setData(session.getItemMappings().getStoredItems().wheat().getBedrockId() << 16);
                session.sendUpstreamPacket(entityEventPacket);
            }

            // Set container type if tamed
            metadata.put(EntityData.CONTAINER_TYPE, tamed ? (byte) ContainerType.HORSE.getId() : (byte) 0);

            // Shows the jump meter
            metadata.getFlags().setFlag(EntityFlag.CAN_POWER_JUMP, saddled);
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public boolean canEat(GeyserSession session, String javaIdentifierStripped, ItemMapping mapping) {
        return DONKEY_AND_HORSE_FOODS.contains(javaIdentifierStripped);
    }
}
