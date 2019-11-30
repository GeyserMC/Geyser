package org.geysermc.connector.utils;

import org.geysermc.api.Geyser;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.inventory.DoubleChestInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;

import java.util.concurrent.TimeUnit;

public class InventoryUtils {

    public static void openInventory(GeyserSession session, Inventory inventory) {
        InventoryTranslator translator = TranslatorsInit.getInventoryTranslators().get(inventory.getWindowType());
        if (translator != null) {
            session.getInventoryCache().setOpenInventory(inventory);
            translator.prepareInventory(session, inventory);
            //TODO: find better way to handle double chest delay
            if (translator instanceof DoubleChestInventoryTranslator) {
                Geyser.getGeneralThreadPool().schedule(() -> {
                    translator.openInventory(session, inventory);
                    translator.updateInventory(session, inventory);
                }, 200, TimeUnit.MILLISECONDS);
            } else {
                translator.openInventory(session, inventory);
                translator.updateInventory(session, inventory);
            }
        }
    }

    public static void closeInventory(GeyserSession session, int windowId) {
        if (windowId != 0) {
            Inventory inventory = session.getInventoryCache().getInventories().get(windowId);
            if (inventory != null) {
                InventoryTranslator translator = TranslatorsInit.getInventoryTranslators().get(inventory.getWindowType());
                translator.closeInventory(session, inventory);
                session.getInventoryCache().uncacheInventory(windowId);
                session.getInventoryCache().setOpenInventory(null);
            }
        } else {
            Inventory inventory = session.getInventory();
            InventoryTranslator translator = TranslatorsInit.getInventoryTranslators().get(inventory.getWindowType());
            translator.updateInventory(session, inventory);
        }
        session.setCraftSlot(0);
        session.getInventory().setCursor(null);
    }
}
