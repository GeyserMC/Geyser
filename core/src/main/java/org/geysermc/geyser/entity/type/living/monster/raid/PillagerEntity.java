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

package org.geysermc.geyser.entity.type.living.monster.raid;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;

import java.util.UUID;

public class PillagerEntity extends AbstractIllagerEntity {

    public PillagerEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void setChargingCrossbow(BooleanEntityMetadata entityMetadata) {
        boolean charging = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.CHARGING, charging);
        dirtyMetadata.put(EntityDataTypes.CHARGE_AMOUNT, charging ? (byte) 64 : (byte) 0); // TODO: gradually increase
    }

    @Override
    public void updateMainHand(GeyserSession session) {
        updateCrossbow();

        super.updateMainHand(session);
    }

    @Override
    public void updateOffHand(GeyserSession session) {
        updateCrossbow();

        super.updateOffHand(session);
    }

    /**
     * Check for a crossbow in either the mainhand or offhand. If one exists, indicate that the pillager should be posing
     */
    protected void updateCrossbow() {
        ItemMapping crossbow = session.getItemMappings().getStoredItems().crossbow();
        ItemData activeCrossbow = null;
        if (this.hand.getDefinition() == crossbow.getBedrockDefinition()) {
            activeCrossbow = this.hand;
        } else if (this.offhand.getDefinition() == crossbow.getBedrockDefinition()) {
            activeCrossbow = this.offhand;
        }

        if (activeCrossbow != null) {
            if (activeCrossbow.getTag() != null && activeCrossbow.getTag().containsKey("chargedItem")) {
                dirtyMetadata.put(EntityDataTypes.CHARGE_AMOUNT, Byte.MAX_VALUE);
                setFlag(EntityFlag.CHARGING, false);
                setFlag(EntityFlag.CHARGED, true);
                setFlag(EntityFlag.USING_ITEM, true);
            } else if (getFlag(EntityFlag.CHARGED)) {
                dirtyMetadata.put(EntityDataTypes.CHARGE_AMOUNT, (byte) 0);
                setFlag(EntityFlag.CHARGED, false);
                setFlag(EntityFlag.USING_ITEM, false);
            }
        }

        updateBedrockMetadata();
    }
}
