package org.geysermc.connector.network.translators.java.inventory;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translators;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;

public class OpenWindowPacketTranslator extends PacketTranslator<ServerOpenWindowPacket> {
    @Override
    public void translate(ServerOpenWindowPacket packet, GeyserSession session) {
        System.out.println("debug: " + packet.getType());
        InventoryTranslator translator = Translators.getInventoryTranslator();

        translator.openInventory(session, new Inventory(packet.getName(), packet.getWindowId(), packet.getType(), 54));

    }
}
