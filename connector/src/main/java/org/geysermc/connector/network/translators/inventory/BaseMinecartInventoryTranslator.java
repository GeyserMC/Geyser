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

// Inventories that extend this class can be handled with a fake minecart, so we don't need to place a block above the player
public class BaseMinecartInventoryTranslator extends BlockInventoryTranslator {
    private ContainerType type;

    public BaseMinecartInventoryTranslator(int size, int paddedSize, ContainerType type) {
        super(size, "", type, new ChestInventoryUpdater(paddedSize));
        this.type = type;
    }

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {
        MinecartInventory minecartInventory = session.getEntityCache().getMinecartInventory();
        if (minecartInventory != null) {
            session.getEntityCache().removeMinecartInventory();
        }
        long entityId = session.getEntityCache().getNextEntityId().incrementAndGet();
        minecartInventory = new MinecartInventory(session, entityId, LocaleUtils.getLocaleString(inventory.getTitle(), session.getClientData().getLanguageCode()));
        session.getEntityCache().addMinecartInventory(minecartInventory);
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setWindowId((byte) inventory.getId());
        containerOpenPacket.setType((byte) type.id());
        containerOpenPacket.setBlockPosition(Vector3i.from(0, 0, 0));
        containerOpenPacket.setUniqueEntityId(session.getEntityCache().getMinecartInventory().getEntityId());
        session.sendUpstreamPacket(containerOpenPacket);
    }
}
