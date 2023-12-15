/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.java.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.recipe.Ingredient;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapedRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.recipe.GeyserShapedRecipe;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.inventory.PlayerInventoryTranslator;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.InventoryUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Translator(packet = ClientboundContainerSetSlotPacket.class)
public class JavaContainerSetSlotTranslator extends PacketTranslator<ClientboundContainerSetSlotPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundContainerSetSlotPacket packet) {
        if (packet.getContainerId() == 255) { //cursor
            GeyserItemStack newItem = GeyserItemStack.from(packet.getItem());
            session.getPlayerInventory().setCursor(newItem, session);
            InventoryUtils.updateCursor(session);
            return;
        }

        //TODO: support window id -2, should update player inventory
        Inventory inventory = InventoryUtils.getInventory(session, packet.getContainerId());
        if (inventory == null)
            return;

        InventoryTranslator translator = session.getInventoryTranslator();
        if (translator != null) {
            if (session.getCraftingGridFuture() != null) {
                session.getCraftingGridFuture().cancel(false);
            }

            int slot = packet.getSlot();
            if (slot >= inventory.getSize()) {
                GeyserImpl geyser = session.getGeyser();
                geyser.getLogger().warning("ClientboundContainerSetSlotPacket sent to " + session.bedrockUsername()
                        + " that exceeds inventory size!");
                if (geyser.getConfig().isDebugMode()) {
                    geyser.getLogger().debug(packet);
                    geyser.getLogger().debug(inventory);
                }
                // 1.19.0 behavior: the state ID will not be set due to exception
                return;
            }

            updateCraftingGrid(session, slot, packet.getItem(), inventory, translator);

            GeyserItemStack newItem = GeyserItemStack.from(packet.getItem());
            if (packet.getContainerId() == 0 && !(translator instanceof PlayerInventoryTranslator)) {
                // In rare cases, the window ID can still be 0 but Java treats it as valid
                session.getPlayerInventory().setItem(slot, newItem, session);
                InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR.updateSlot(session, session.getPlayerInventory(), slot);
            } else {
                inventory.setItem(slot, newItem, session);
                translator.updateSlot(session, inventory, slot);
            }

            // Intentional behavior here below the cursor; Minecraft 1.18.1 also does this.
            int stateId = packet.getStateId();
            session.setEmulatePost1_16Logic(stateId > 0 || stateId != inventory.getStateId());
            inventory.setStateId(stateId);
        }
    }

    /**
     * Checks for a changed output slot in the crafting grid, and ensures Bedrock sees the recipe.
     */
    private static void updateCraftingGrid(GeyserSession session, int slot, ItemStack item, Inventory inventory, InventoryTranslator translator) {
        if (slot != 0) {
            return;
        }
        int gridSize = translator.getGridSize();
        if (gridSize == -1) {
            return;
        }

        if (InventoryUtils.isEmpty(item)) {
            return;
        }

        session.setCraftingGridFuture(session.scheduleInEventLoop(() -> {
            int offset = gridSize == 4 ? 28 : 32;
            int gridDimensions = gridSize == 4 ? 2 : 3;
            int firstRow = -1, height = -1;
            int firstCol = -1, width = -1;
            for (int row = 0; row < gridDimensions; row++) {
                for (int col = 0; col < gridDimensions; col++) {
                    if (!inventory.getItem(col + (row * gridDimensions) + 1).isEmpty()) {
                        if (firstRow == -1) {
                            firstRow = row;
                            firstCol = col;
                        } else {
                            firstCol = Math.min(firstCol, col);
                        }
                        height = Math.max(height, row);
                        width = Math.max(width, col);
                    }
                }
            }

            //empty grid
            if (firstRow == -1) {
                return;
            }

            height += -firstRow + 1;
            width += -firstCol + 1;

            if (InventoryUtils.getValidRecipe(session, item, inventory::getItem, gridDimensions, firstRow,
                    height, firstCol, width) != null) {
                // Recipe is already present on the client; don't send packet
                return;
            }

            UUID uuid = UUID.randomUUID();
            int newRecipeId = session.getLastRecipeNetId().incrementAndGet();

            ItemData[] ingredients = new ItemData[height * width];
            //construct ingredient list and clear slots on client
            Ingredient[] javaIngredients = new Ingredient[height * width];
            int index = 0;
            for (int row = firstRow; row < height + firstRow; row++) {
                for (int col = firstCol; col < width + firstCol; col++) {
                    GeyserItemStack geyserItemStack = inventory.getItem(col + (row * gridDimensions) + 1);
                    ingredients[index] = geyserItemStack.getItemData(session);
                    ItemStack[] itemStacks = new ItemStack[] {geyserItemStack.isEmpty() ? null : geyserItemStack.getItemStack(1)};
                    javaIngredients[index] = new Ingredient(itemStacks);

                    InventorySlotPacket slotPacket = new InventorySlotPacket();
                    slotPacket.setContainerId(ContainerId.UI);
                    slotPacket.setSlot(col + (row * gridDimensions) + offset);
                    slotPacket.setItem(ItemData.AIR);
                    session.sendUpstreamPacket(slotPacket);
                    index++;
                }
            }

            // Cache this recipe so we know the client has received it
            session.getCraftingRecipes().put(newRecipeId, new GeyserShapedRecipe(width, height, javaIngredients, item));

            CraftingDataPacket craftPacket = new CraftingDataPacket();
            craftPacket.getCraftingData().add(ShapedRecipeData.shaped(
                    uuid.toString(),
                    width,
                    height,
                    Arrays.stream(ingredients).map(ItemDescriptorWithCount::fromItem).toList(),
                    Collections.singletonList(ItemTranslator.translateToBedrock(session, item)),
                    uuid,
                    "crafting_table",
                    0,
                    newRecipeId
            ));
            craftPacket.setCleanRecipes(false);
            session.sendUpstreamPacket(craftPacket);

            index = 0;
            for (int row = firstRow; row < height + firstRow; row++) {
                for (int col = firstCol; col < width + firstCol; col++) {
                    InventorySlotPacket slotPacket = new InventorySlotPacket();
                    slotPacket.setContainerId(ContainerId.UI);
                    slotPacket.setSlot(col + (row * gridDimensions) + offset);
                    slotPacket.setItem(ingredients[index]);
                    session.sendUpstreamPacket(slotPacket);
                    index++;
                }
            }
        }, 150, TimeUnit.MILLISECONDS));
    }
}
