package org.geysermc.connector.network.translators.java.inventory;

import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.Translators;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;

@Translator(packet = ServerOpenWindowPacket.class)
public class OpenWindowPacketTranslator extends PacketTranslator<ServerOpenWindowPacket> {
    @Override
    public void translate(ServerOpenWindowPacket packet, GeyserSession session) {
        System.out.println("debug: " + packet.getType());
        InventoryTranslator translator = Translators.getInventoryTranslator();

        translator.openInventory(session, new Inventory(packet.getName(), packet.getWindowId(), packet.getType(), 54));

    }
}
