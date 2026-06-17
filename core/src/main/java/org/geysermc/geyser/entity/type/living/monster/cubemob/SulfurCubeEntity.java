/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.living.monster.cubemob;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;

public class SulfurCubeEntity extends AbstractCubeEntity {

    public SulfurCubeEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    public void setBody(GeyserItemStack stack) {
        MobEquipmentPacket handPacket = new MobEquipmentPacket();
        handPacket.setRuntimeEntityId(geyserId);
        handPacket.setItem(ItemTranslator.translateToBedrock(session, stack));
        handPacket.setHotbarSlot(0);
        handPacket.setInventorySlot(0);
        handPacket.setContainerId(ContainerId.INVENTORY);

        session.sendUpstreamPacket(handPacket);
    }

    @Override
    public int getScaleOffset() {
        return -1;
    }

    public void setMaxFuse(IntEntityMetadata entityMetadata) {
        this.dirtyMetadata.put(EntityDataTypes.FUSE_TIME, entityMetadata.getPrimitiveValue());
    }
}
