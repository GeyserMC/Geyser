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

package org.geysermc.geyser.translator.inventory.chest;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlags;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.ContainerClosePacket;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import com.nukkitx.protocol.bedrock.packet.RemoveEntityPacket;
import org.geysermc.geyser.inventory.Container;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;

public class SingleChestInventoryTranslator extends ChestInventoryTranslator {
    public SingleChestInventoryTranslator(int size) {
        super(size, 27);
    }

    @Override
    public boolean prepareInventory(GeyserSession session, Inventory inventory) {
        // See BlockInventoryHolder - same concept there except we're also dealing with a specific block state
        if (session.getLastInteractionPlayerPosition().equals(session.getPlayerEntity().getPosition())) {
            int javaBlockId = session.getGeyser().getWorldManager().getBlockAt(session, session.getLastInteractionBlockPosition());
            String[] javaBlockString = BlockRegistries.JAVA_IDENTIFIERS.get().getOrDefault(javaBlockId, "minecraft:air").split("\\[");
            if (javaBlockString[0].equals("minecraft:ender_chest") || javaBlockString.length > 1 && (javaBlockString[0].equals("minecraft:chest") || javaBlockString[0].equals("minecraft:trapped_chest"))
                    && javaBlockString[1].contains("type=single")) {
                inventory.setHolderPosition(session.getLastInteractionBlockPosition());
                ((Container) inventory).setUsingRealBlock(true, javaBlockString[0]);

                NbtMap tag = NbtMap.builder()
                        .putInt("x", session.getLastInteractionBlockPosition().getX())
                        .putInt("y", session.getLastInteractionBlockPosition().getY())
                        .putInt("z", session.getLastInteractionBlockPosition().getZ())
                        .putString("CustomName", inventory.getTitle()).build();
                BlockEntityDataPacket dataPacket = new BlockEntityDataPacket();
                dataPacket.setData(tag);
                dataPacket.setBlockPosition(session.getLastInteractionBlockPosition());
                session.sendUpstreamPacket(dataPacket);

                return true;
            }
        }

        long entityId = session.getEntityCache().getNextEntityId().incrementAndGet();
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setUniqueEntityId(entityId);
        addEntityPacket.setRuntimeEntityId(entityId);
        addEntityPacket.setIdentifier("minecraft:creeper");
        addEntityPacket.setPosition(session.getPlayerEntity().getPosition());
        addEntityPacket.setMotion(Vector3f.ZERO);
        addEntityPacket.setRotation(Vector3f.ZERO);
        EntityFlags entityFlags = new EntityFlags();
        entityFlags.setFlag(EntityFlag.INVISIBLE, true);
        addEntityPacket.getMetadata()
                .putFlags(entityFlags)
                .putFloat(EntityData.SCALE, 0F)
                .putFloat(EntityData.BOUNDING_BOX_WIDTH, 0F)
                .putFloat(EntityData.BOUNDING_BOX_HEIGHT, 0F)
                .putString(EntityData.NAMETAG, inventory.getTitle())
                .putInt(EntityData.CONTAINER_BASE_SIZE, inventory.getSize());
        session.sendUpstreamPacket(addEntityPacket);
        inventory.setHolderId(entityId);

        return true;
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setId((byte) inventory.getBedrockId());
        containerOpenPacket.setType(ContainerType.CONTAINER);
        containerOpenPacket.setBlockPosition(inventory.getHolderPosition());
        containerOpenPacket.setUniqueEntityId(inventory.getHolderId());
        session.sendUpstreamPacket(containerOpenPacket);
    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {
        if (((Container) inventory).isUsingRealBlock()) {
            // No need to reset a block since we didn't change any blocks
            // But send a container close packet because we aren't destroying the original.
            ContainerClosePacket packet = new ContainerClosePacket();
            packet.setId((byte) inventory.getBedrockId());
            packet.setUnknownBool0(true); //TODO needs to be changed in Protocol to "server-side" or something
            session.sendUpstreamPacket(packet);
            return;
        }

        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(inventory.getHolderId());
        session.sendUpstreamPacket(removeEntityPacket);
    }
}
