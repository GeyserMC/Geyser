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
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemTranslator;

public class ItemEntity extends Entity {

    public ItemEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setIdentifier(entityType.getIdentifier());
        addEntityPacket.setRuntimeEntityId(geyserId);
        addEntityPacket.setUniqueEntityId(geyserId);
        addEntityPacket.setPosition(Vector3f.from(position.getX(), position.getY() + 0.3f, position.getZ()));
        addEntityPacket.setMotion(motion);
        addEntityPacket.setRotation(getBedrockRotation());
        addEntityPacket.setEntityType(entityType.getType());
        addEntityPacket.getMetadata().putAll(metadata);

        valid = true;
        session.sendUpstreamPacket(addEntityPacket);

        session.getConnector().getLogger().debug("Spawned entity " + entityType + " at location " + position + " with id " + geyserId + " (java id " + entityId + ")");
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 7) {
            AddItemEntityPacket itemPacket = new AddItemEntityPacket();
            itemPacket.setRuntimeEntityId(geyserId);
            itemPacket.setPosition(Vector3f.from(position.getX(), position.getY() + 0.3f, position.getZ()));
            itemPacket.setMotion(motion);
            itemPacket.setUniqueEntityId(geyserId);
            itemPacket.setFromFishing(false);
            itemPacket.getMetadata().putAll(metadata);
            itemPacket.setItemInHand(ItemTranslator.translateToBedrock(session, (ItemStack) entityMetadata.getValue()));
            session.sendUpstreamPacket(itemPacket);
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public void moveRelative(GeyserSession session, double relX, double relY, double relZ, Vector3f rotation, boolean isOnGround) {
        Vector3f movePosition = Vector3f.from(position.getX() + relX, position.getY() + relY, position.getZ() + relZ);
        boolean OnGroundChange = onGround != isOnGround;

        setRotation(rotation);
        setOnGround(isOnGround);


        if (position.distanceSquared(movePosition) > 0.25f || OnGroundChange)
        {
            this.position = movePosition;

            MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
            moveEntityPacket.setRuntimeEntityId(geyserId);
            moveEntityPacket.setPosition(Vector3f.from(position.getX(), position.getY() + 0.3f, position.getZ()));
            moveEntityPacket.setRotation(getBedrockRotation());
            moveEntityPacket.setOnGround(isOnGround);
            moveEntityPacket.setTeleported(true);

            session.sendUpstreamPacket(moveEntityPacket);
        }
    }

    @Override
    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        boolean OnGroundChange = onGround != isOnGround;

        setRotation(rotation);
        setOnGround(isOnGround);

        if (this.position.distanceSquared(position) > 0.25f || OnGroundChange)
        {
            setPosition(position);

            MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
            moveEntityPacket.setRuntimeEntityId(geyserId);
            moveEntityPacket.setPosition(Vector3f.from(position.getX(), position.getY() + 0.3f, position.getZ()));
            moveEntityPacket.setRotation(getBedrockRotation());
            moveEntityPacket.setOnGround(isOnGround);
            moveEntityPacket.setTeleported(teleported);

            session.sendUpstreamPacket(moveEntityPacket);
        }
    }
}
