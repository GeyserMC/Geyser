package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.protocol.bedrock.data.ItemData;
import org.geysermc.api.Geyser;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class InventoryUtils {

    public static void openInventory(GeyserSession session, ServerOpenWindowPacket packet) {
        Inventory inventory = new Inventory(packet.getWindowId(), packet.getType());
        InventoryTranslator translator = TranslatorsInit.getInventoryTranslators().get(inventory.getWindowType());
        if (translator != null) {
            session.getInventoryCache().cacheInventory(inventory);
            session.getInventoryCache().setOpenInventory(inventory);
            translator.prepareInventory(session, inventory);
            //TODO: find better way to handle double chest delay
            if (inventory.getWindowType() == WindowType.GENERIC_9X4 || inventory.getWindowType() == WindowType.GENERIC_9X5 || inventory.getWindowType() == WindowType.GENERIC_9X6) {
                Geyser.getGeneralThreadPool().schedule(() -> {
                    translator.openInventory(session, inventory);
                    translator.updateInventory(session, inventory);
                }, 200, TimeUnit.MILLISECONDS);
            } else {
                translator.openInventory(session, inventory);
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
            }
            session.getInventoryCache().setOpenInventory(null);
        }
    }

    //currently, ItemStack.equals() does not check the item id
    public static boolean canCombine(ItemData stack1, ItemData stack2) {
        if (stack1 == null || stack2 == null)
            return false;
        return stack1.getId() == stack2.getId() && stack1.equals(stack2, false, true, true);
    }

    //NPE if nbt tag is null
    public static ItemStack fixNbt(ItemStack stack) {
        if (stack == null)
            return null;
        return new ItemStack(stack.getId(), stack.getAmount(), stack.getNbt() == null ? new CompoundTag("") : stack.getNbt());
    }
}
