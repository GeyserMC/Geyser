package org.geysermc.connector.network.translators.java.window;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowPropertyPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.cache.InventoryCache;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;

public class JavaWindowPropertyTranslator extends PacketTranslator<ServerWindowPropertyPacket> {

    @Override
    public void translate(ServerWindowPropertyPacket packet, GeyserSession session) {
        Inventory inventory = session.getInventoryCache().getInventories().get(packet.getWindowId());
        if (inventory == null || (packet.getWindowId() != 0 && inventory.getWindowType() == null))
            return;

        InventoryTranslator translator = TranslatorsInit.getInventoryTranslators().get(inventory.getWindowType());
        if (translator != null) {
            translator.updateProperty(session, inventory, packet.getRawProperty(), packet.getValue());
        }
    }
}
