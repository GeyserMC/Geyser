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

package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.data.EntityLink;
import com.nukkitx.protocol.bedrock.packet.SetEntityLinkPacket;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;

public class FurnaceMinecartEntity extends Entity {

    private FurnaceMincartBlockEntity furnace;

    public FurnaceMinecartEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position.add(0d, entityType.getOffset(), 0d), motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 13) {
            boolean hasFuel = (boolean) entityMetadata.getValue();

            if (furnace == null) {
                // We use the entity id * -1 as it wont be used by vanilla
                furnace = new FurnaceMincartBlockEntity(entityId * -1, session.getEntityCache().getNextEntityId().incrementAndGet(),
                        EntityType.MINECART_FURNACE_BLOCK, Vector3f.ZERO, Vector3f.ZERO, rotation, BlockTranslator.JAVA_RUNTIME_FURNACE_ID);

                session.getEntityCache().spawnEntity(furnace);

                furnace.getMetadata().put(EntityData.RIDER_SEAT_POSITION, Vector3f.from(0f, 0.75f, 0f));
                furnace.getMetadata().put(EntityData.RIDER_ROTATION_LOCKED, 1);
                furnace.updateBedrockMetadata(session);

                SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
                linkPacket.setEntityLink(new EntityLink(geyserId, furnace.getGeyserId(), EntityLink.Type.PASSENGER, false));
                session.sendUpstreamPacket(linkPacket);
            }

            furnace.updateBlock(session, hasFuel ? BlockTranslator.JAVA_RUNTIME_FURNACE_LIT_ID : BlockTranslator.JAVA_RUNTIME_FURNACE_ID);
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public boolean despawnEntity(GeyserSession session) {
        // Make sure the furnace falling block is removed
        if (furnace != null) {
            furnace.despawnEntity(session);
            session.getEntityCache().removeEntity(furnace, true);
        }

        return super.despawnEntity(session);
    }
}
