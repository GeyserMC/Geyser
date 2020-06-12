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

package org.geysermc.connector.command.defaults;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.Arrays;

public class ArmorCommand extends GeyserCommand {

    private GeyserConnector connector;

    public ArmorCommand(GeyserConnector connector, String name, String description, String permission) {
        super(name, description, permission);

        this.connector = connector;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender.isConsole()) {
            return;
        }

        // Make sure the sender is a Bedrock edition client
        if (sender instanceof GeyserSession) {
            GeyserSession session = (GeyserSession) sender;

            InventoryTranslator newTranslator = InventoryTranslator.INVENTORY_TRANSLATORS.get(WindowType.HOPPER);

            // Id seems to be constrained to a byte so we just use the largest number
            Inventory newInventory = new Inventory("Armour and Offhand", 127, WindowType.HOPPER, newTranslator.size + 36);
            session.getInventoryCache().cacheInventory(newInventory);
            InventoryUtils.openInventory(session, newInventory);

            ItemStack[] items = new ItemStack[newInventory.getSize()];
            // Armor slots
            items[0] = session.getInventory().getItem(5);
            items[1] = session.getInventory().getItem(6);
            items[2] = session.getInventory().getItem(7);
            items[3] = session.getInventory().getItem(8);

            // Offhand
            items[4] = session.getInventory().getItem(45);
            
            // Rest of inv
            for (int i = 0; i < 36; i++) {
                ItemStack item = session.getInventory().getItem(9 + i);
                items[5 + i] = item;
            }

            newInventory.setItems(Arrays.copyOf(items, newInventory.getSize()));
            newTranslator.updateInventory(session, newInventory);

            // We dont handle item moving yet
            // Doesnt work for bukkit yet, see offhand command to fix that

            return;
        }
    }
}
