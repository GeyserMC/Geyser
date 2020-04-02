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

package org.geysermc.connector.network.translators.java.window;

import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientCloseWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nukkitx.protocol.bedrock.packet.ContainerClosePacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.Translators;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.concurrent.TimeUnit;

@Translator(packet = ServerOpenWindowPacket.class)
public class JavaOpenWindowTranslator extends PacketTranslator<ServerOpenWindowPacket> {

    @Override
    public void translate(ServerOpenWindowPacket packet, GeyserSession session) {
        if (packet.getWindowId() == 0) {
            return;
        }
        InventoryTranslator newTranslator = Translators.getInventoryTranslators().get(packet.getType());
        Inventory openInventory = session.getInventoryCache().getOpenInventory();
        if (newTranslator == null) {
            if (openInventory != null) {
                ContainerClosePacket closePacket = new ContainerClosePacket();
                closePacket.setWindowId((byte)openInventory.getId());
                session.getUpstream().sendPacket(closePacket);
                Translators.getInventoryTranslators().get(openInventory.getWindowType()).closeInventory(session, openInventory);
            }
            ClientCloseWindowPacket closeWindowPacket = new ClientCloseWindowPacket(packet.getWindowId());
            session.getDownstream().getSession().send(closeWindowPacket);
            return;
        }

        String name = packet.getName();
        try {
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(packet.getName()).getAsJsonObject();
            if (jsonObject.has("text")) {
                name = jsonObject.get("text").getAsString();
            } else if (jsonObject.has("translate")) {
                name = jsonObject.get("translate").getAsString();
            }
        } catch (Exception e) {
            GeyserConnector.getInstance().getLogger().debug("JavaOpenWindowTranslator: " + e.toString());
        }

        Inventory newInventory = new Inventory(name, packet.getWindowId(), packet.getType(), newTranslator.size + 36);
        session.getInventoryCache().cacheInventory(newInventory);
        if (openInventory != null) {
            InventoryTranslator openTranslator = Translators.getInventoryTranslators().get(openInventory.getWindowType());
            if (!openTranslator.getClass().equals(newTranslator.getClass())) {
                InventoryUtils.closeInventory(session, openInventory.getId());
                GeyserConnector.getInstance().getGeneralThreadPool().schedule(() -> InventoryUtils.openInventory(session, newInventory), 500, TimeUnit.MILLISECONDS);
                return;
            }
        }

        InventoryUtils.openInventory(session, newInventory);
    }
}
