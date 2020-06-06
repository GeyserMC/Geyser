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

import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.data.InventoryActionData;
import lombok.AllArgsConstructor;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.updater.ContainerInventoryUpdater;
import org.geysermc.connector.network.translators.inventory.updater.InventoryUpdater;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public abstract class InventoryTranslator {

    public static final Map<WindowType, InventoryTranslator> INVENTORY_TRANSLATORS = new HashMap<WindowType, InventoryTranslator>() {
        {
            put(null, new PlayerInventoryTranslator()); //player inventory
            put(WindowType.GENERIC_9X1, new SingleChestInventoryTranslator(9));
            put(WindowType.GENERIC_9X2, new SingleChestInventoryTranslator(18));
            put(WindowType.GENERIC_9X3, new SingleChestInventoryTranslator(27));
            put(WindowType.GENERIC_9X4, new DoubleChestInventoryTranslator(36));
            put(WindowType.GENERIC_9X5, new DoubleChestInventoryTranslator(45));
            put(WindowType.GENERIC_9X6, new DoubleChestInventoryTranslator(54));
            put(WindowType.BREWING_STAND, new BrewingInventoryTranslator());
            put(WindowType.ANVIL, new AnvilInventoryTranslator());
            put(WindowType.CRAFTING, new CraftingInventoryTranslator());
            put(WindowType.GRINDSTONE, new GrindstoneInventoryTranslator());
            put(WindowType.MERCHANT, new MerchantInventoryTranslator());
            //put(WindowType.ENCHANTMENT, new EnchantmentInventoryTranslator()); //TODO

            InventoryTranslator furnace = new FurnaceInventoryTranslator();
            put(WindowType.FURNACE, furnace);
            put(WindowType.BLAST_FURNACE, furnace);
            put(WindowType.SMOKER, furnace);

            InventoryUpdater containerUpdater = new ContainerInventoryUpdater();
            put(WindowType.GENERIC_3X3, new BlockInventoryTranslator(9, "minecraft:dispenser[facing=north,triggered=false]", ContainerType.DISPENSER, containerUpdater));
            put(WindowType.HOPPER, new BlockInventoryTranslator(5, "minecraft:hopper[enabled=false,facing=down]", ContainerType.HOPPER, containerUpdater));
            put(WindowType.SHULKER_BOX, new BlockInventoryTranslator(27, "minecraft:shulker_box[facing=north]", ContainerType.CONTAINER, containerUpdater));
            //put(WindowType.BEACON, new BlockInventoryTranslator(1, "minecraft:beacon", ContainerType.BEACON)); //TODO
        }
    };

    public final int size;

    public abstract void prepareInventory(GeyserSession session, Inventory inventory);
    public abstract void openInventory(GeyserSession session, Inventory inventory);
    public abstract void closeInventory(GeyserSession session, Inventory inventory);
    public abstract void updateProperty(GeyserSession session, Inventory inventory, int key, int value);
    public abstract void updateInventory(GeyserSession session, Inventory inventory);
    public abstract void updateSlot(GeyserSession session, Inventory inventory, int slot);
    public abstract int bedrockSlotToJava(InventoryActionData action);
    public abstract int javaSlotToBedrock(int slot);
    public abstract SlotType getSlotType(int javaSlot);
    public abstract void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions);
}
