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

package org.geysermc.connector.network.translators.inventory;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.cache.MinecartInventory;
import org.geysermc.connector.network.translators.inventory.updater.ChestInventoryUpdater;
import org.geysermc.connector.utils.LocaleUtils;

// TODO: extend single
public class HopperInventoryTranslator extends BlockInventoryTranslator {
    public HopperInventoryTranslator() {
        super(5, "minecraft:chest[facing=north,type=single,waterlogged=false]", ContainerType.CONTAINER, new ChestInventoryUpdater(27));
    }

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {
        System.out.println("TESTTTTTT!pre[p");
        MinecartInventory minecartInventory = session.getEntityCache().getMinecartInventory();
        if (minecartInventory != null) {
            session.getEntityCache().removeMinecartInventory();
        }
        long entityId = session.getEntityCache().getNextEntityId().incrementAndGet();
        System.out.println(entityId);
        minecartInventory = new MinecartInventory(session, entityId, LocaleUtils.getLocaleString(inventory.getTitle(), session.getClientData().getLanguageCode()));
        session.getEntityCache().addMinecartInventory(minecartInventory);
        // Vector3i position = session.getPlayerEntity().getPosition().toInt();
        // position = position.add(Vector3i.UP);
        /* UpdateBlockPacket blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(position);
        blockPacket.setRuntimeId(blockId);
        blockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(blockPacket); */
        // inventory.setHolderPosition(position);

        /* CompoundTag tag = CompoundTag.builder()
                .intTag("x", position.getX())
                .intTag("y", position.getY())
                .intTag("z", position.getZ())
                .stringTag("CustomName", LocaleUtils.getLocaleString(inventory.getTitle(), session.getClientData().getLanguageCode())).buildRootTag();
        BlockEntityDataPacket dataPacket = new BlockEntityDataPacket();
        dataPacket.setData(tag);
        dataPacket.setBlockPosition(position);
        session.sendUpstreamPacket(dataPacket); */

        System.out.println(inventory.getTitle());




        // session.sendUpstreamPacket(addEntityPacket);

    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        System.out.println("TESTTTTTT!");
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setWindowId((byte) inventory.getId());
        containerOpenPacket.setType((byte) ContainerType.HOPPER.id());
        // containerOpenPacket.setType((byte) 0);
        // containerOpenPacket.setBlockPosition(inventory.getHolderPosition());
        containerOpenPacket.setBlockPosition(Vector3i.from(0, 0, 0));
        // containerOpenPacket.setUniqueEntityId(inventory.getHolderId());
        System.out.println(session.getEntityCache().getMinecartInventory().getEntityId());
        containerOpenPacket.setUniqueEntityId(ession.getEntityCache().getMinecartInventory().getEntityId());
        session.sendUpstreamPacket(containerOpenPacket);
    }
}
