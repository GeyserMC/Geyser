/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.inventory.holder;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.data.EntityLink;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import com.nukkitx.protocol.bedrock.packet.RemoveEntityPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityLinkPacket;
import lombok.AllArgsConstructor;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;

@AllArgsConstructor
public class MinecartInventoryHolder extends InventoryHolder {
    private final ContainerType containerType;

    @Override
    public void prepareInventory(InventoryTranslator translator, GeyserSession session, Inventory inventory) {
        final long entityId = session.getEntityCache().getNextEntityId().incrementAndGet();
        inventory.setHolderId(entityId);

        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setUniqueEntityId(entityId);
        addEntityPacket.setRuntimeEntityId(entityId);
        // You can't hide the chest of a chest_minecart but Bedrock accepts a normal minecart too
        addEntityPacket.setIdentifier("minecraft:minecart");
        addEntityPacket.setEntityType(0);
        addEntityPacket.setPosition(session.getPlayerEntity().getPosition().sub(0D, 3D, 0D));
        addEntityPacket.setRotation(Vector3f.ZERO);
        addEntityPacket.setMotion(Vector3f.ZERO);
        addEntityPacket.getMetadata()
                .putFloat(EntityData.SCALE, 0F)
                .putFloat(EntityData.BOUNDING_BOX_WIDTH, 0F)
                .putFloat(EntityData.BOUNDING_BOX_HEIGHT, 0F);

        addEntityPacket.getMetadata().put(EntityData.CONTAINER_BASE_SIZE, 27);
        addEntityPacket.getMetadata().put(EntityData.NAMETAG, inventory.getTitle());

        session.sendUpstreamPacket(addEntityPacket);

        // Mount the minecart on the player so the inventory doesn't close when the player moves away
        SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
        EntityLink.Type type = EntityLink.Type.PASSENGER;
        linkPacket.setEntityLink(new EntityLink(session.getPlayerEntity().getGeyserId(), entityId, type, false));
        session.sendUpstreamPacket(linkPacket);
    }

    @Override
    public void openInventory(InventoryTranslator translator, GeyserSession session, Inventory inventory) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setWindowId((byte) inventory.getId());
        containerOpenPacket.setType((byte) containerType.id());
        containerOpenPacket.setBlockPosition(Vector3i.ZERO);
        containerOpenPacket.setUniqueEntityId(inventory.getHolderId());
        session.sendUpstreamPacket(containerOpenPacket);
    }

    @Override
    public void closeInventory(InventoryTranslator translator, GeyserSession session, Inventory inventory) {
        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(inventory.getHolderId());
        session.sendUpstreamPacket(removeEntityPacket);
    }
}
