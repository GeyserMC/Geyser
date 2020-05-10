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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientMoveItemToHotbarPacket;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.BlockPickRequestPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerHotbarPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.item.ItemTranslator;

@Translator(packet = BlockPickRequestPacket.class)
public class BedrockBlockPickRequestPacketTranslator extends PacketTranslator<BlockPickRequestPacket> {

    @Override
    public void translate(BlockPickRequestPacket packet, GeyserSession session) {
        Vector3i vector = packet.getBlockPosition();
        BlockState blockToPick = session.getConnector().getWorldManager().getBlockAt(session, vector.getX(), vector.getY(), vector.getZ());

        if (blockToPick.getId() == 0) return; // Block is air - chunk caching is probably off

        Inventory inventory = session.getInventoryCache().getOpenInventory(); // Get the inventory to choose a slot to pick
        if (inventory == null) inventory = session.getInventory();

        String targetIdentifier = BlockTranslator.getJavaIdBlockMap().inverse().get(blockToPick).split("\\[")[0];
        System.out.println(targetIdentifier);
        ItemTranslator itemTranslator = new ItemTranslator();

        for (int i = 36; i < 45; i++) { // Check hotbar for item
            if (inventory.getItem(i) != null) {
                ItemEntry item = itemTranslator.getItem(inventory.getItem(i));
                if (item.getJavaIdentifier().equals(targetIdentifier)) {
                    PlayerHotbarPacket hotbarPacket = new PlayerHotbarPacket();
                    hotbarPacket.setContainerId(0);
                    hotbarPacket.setSelectedHotbarSlot(i - 36); // Java inventory slot to hotbar slot ID
                    hotbarPacket.setSelectHotbarSlot(true);
                    session.sendUpstreamPacket(hotbarPacket);
                    session.getInventory().setHeldItemSlot(i - 36);
                    return; // Don't check inventory if item was in hotbar
                }
            }
        }

        for (int i = 9; i < 36; i++) { // Check inventory for item
            if (inventory.getItem(i) != null) {
                ItemEntry item = itemTranslator.getItem(inventory.getItem(i));
                if (item.getJavaIdentifier().equals(targetIdentifier)) {
                    ClientMoveItemToHotbarPacket packetToSend = new ClientMoveItemToHotbarPacket(i); // https://wiki.vg/Protocol#Pick_Item
                    session.sendDownstreamPacket(packetToSend);
                    break;
                }
            }
        }
    }
}