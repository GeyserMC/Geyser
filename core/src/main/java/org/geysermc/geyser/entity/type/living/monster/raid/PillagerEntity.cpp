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

#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"

public class PillagerEntity extends AbstractIllagerEntity {

    public PillagerEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setChargingCrossbow(BooleanEntityMetadata entityMetadata) {
        bool charging = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.CHARGING, charging);
        dirtyMetadata.put(EntityDataTypes.CHARGE_AMOUNT, charging ? (byte) 64 : (byte) 0);
    }

    override public void updateMainHand() {
        updateCrossbow();

        super.updateMainHand();
    }

    override public void updateOffHand() {
        updateCrossbow();

        super.updateOffHand();
    }


    protected void updateCrossbow() {
        GeyserItemStack activeCrossbow = null;
        if (getMainHandItem().is(Items.CROSSBOW)) {
            activeCrossbow = getMainHandItem();
        } else if (getOffHandItem().is(Items.CROSSBOW)) {
            activeCrossbow = getOffHandItem();
        }

        if (activeCrossbow != null) {
            if (activeCrossbow.getComponent(DataComponentTypes.CHARGED_PROJECTILES) != null) {
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
