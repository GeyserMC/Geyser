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

package org.geysermc.geyser.translator.inventory.chest;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.inventory.holder.BlockInventoryHolder;
import org.geysermc.geyser.inventory.holder.InventoryHolder;

public class SingleChestInventoryTranslator extends ChestInventoryTranslator {
    private final InventoryHolder holder;

    public SingleChestInventoryTranslator(int size) {
        super(size, 27);
        this.holder = new BlockInventoryHolder("minecraft:chest[facing=north,type=single,waterlogged=false]", ContainerType.CONTAINER,
                "minecraft:ender_chest", "minecraft:trapped_chest") {
            @Override
            protected boolean isValidBlock(String[] javaBlockString) {
                if (javaBlockString[0].equals("minecraft:ender_chest")) {
                    // Can't have double ender chests
                    return true;
                }

                // Add provision to ensure this isn't a double chest
                return super.isValidBlock(javaBlockString) && (javaBlockString.length > 1 && javaBlockString[1].contains("type=single"));
            }
        };
    }

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {
        holder.prepareInventory(this, session, inventory);
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        holder.openInventory(this, session, inventory);
    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {
        holder.closeInventory(this, session, inventory);
    }
}
