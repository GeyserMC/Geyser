/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPlaceBlockPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientClickWindowButtonPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientCloseWindowPacket;
import com.nukkitx.protocol.bedrock.packet.LecternUpdatePacket;
import org.geysermc.connector.inventory.LecternContainer;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.InventoryUtils;

/**
 * Used to translate moving pages, or closing the inventory
 */
@Translator(packet = LecternUpdatePacket.class)
public class BedrockLecternUpdateTranslator extends PacketTranslator<LecternUpdatePacket> {

    @Override
    public void translate(LecternUpdatePacket packet, GeyserSession session) {
        if (packet.isDroppingBook()) {
            // Bedrock drops the book outside of the GUI. Java drops it in the GUI
            // So, we enter the GUI and then drop it! :)
            session.setDroppingLecternBook(true);

            // Emulate an interact packet
            ClientPlayerPlaceBlockPacket blockPacket = new ClientPlayerPlaceBlockPacket(
                    new Position(packet.getBlockPosition().getX(), packet.getBlockPosition().getY(), packet.getBlockPosition().getZ()),
                    BlockFace.values()[0],
                    Hand.MAIN_HAND,
                    0, 0, 0, // Java doesn't care about these when dealing with a lectern
                    false);
            session.sendDownstreamPacket(blockPacket);
        } else {
            // Bedrock wants to either move a page or exit
            if (!(session.getOpenInventory() instanceof LecternContainer)) {
                session.getConnector().getLogger().debug("Expected lectern but it wasn't open!");
                return;
            }

            LecternContainer lecternContainer = (LecternContainer) session.getOpenInventory();
            if (lecternContainer.getCurrentBedrockPage() == packet.getPage()) {
                // The same page means Bedrock is closing the window
                ClientCloseWindowPacket closeWindowPacket = new ClientCloseWindowPacket(lecternContainer.getId());
                session.sendDownstreamPacket(closeWindowPacket);
                InventoryUtils.closeInventory(session, lecternContainer.getId(), false);
            } else {
                // Each "page" Bedrock gives to us actually represents two pages (think opening a book and seeing two pages)
                // Each "page" on Java is just one page (think a spiral notebook folded back to only show one page)
                int newJavaPage = (packet.getPage() * 2);
                int currentJavaPage = (lecternContainer.getCurrentBedrockPage() * 2);

                // Send as many click button packets as we need to
                // Java has the option to specify exact page numbers by adding 100 to the number, but buttonId variable
                // is a byte when transmitted over the network and therefore this stops us at 128
                if (newJavaPage > currentJavaPage) {
                    for (int i = currentJavaPage; i < newJavaPage; i++) {
                        ClientClickWindowButtonPacket clickButtonPacket = new ClientClickWindowButtonPacket(session.getOpenInventory().getId(), 2);
                        session.sendDownstreamPacket(clickButtonPacket);
                    }
                } else {
                    for (int i = currentJavaPage; i > newJavaPage; i--) {
                        ClientClickWindowButtonPacket clickButtonPacket = new ClientClickWindowButtonPacket(session.getOpenInventory().getId(), 1);
                        session.sendDownstreamPacket(clickButtonPacket);
                    }
                }
            }
        }
    }
}
