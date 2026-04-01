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

#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.inventory.Inventory"
#include "org.geysermc.geyser.inventory.InventoryHolder"
#include "org.geysermc.geyser.inventory.recipe.GeyserRecipe"
#include "org.geysermc.geyser.inventory.recipe.GeyserShapedRecipe"
#include "org.geysermc.geyser.inventory.recipe.GeyserSmithingRecipe"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.inventory.SmithingInventoryTranslator"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.InventoryUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemStackSlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.concurrent.ThreadLocalRandom"
#include "java.util.concurrent.TimeUnit"

@Translator(packet = ClientboundContainerSetSlotPacket.class)
public class JavaContainerSetSlotTranslator extends PacketTranslator<ClientboundContainerSetSlotPacket> {

    override public void translate(GeyserSession session, ClientboundContainerSetSlotPacket packet) {
        InventoryHolder<?> holder = InventoryUtils.getInventory(session, packet.getContainerId());
        if (holder == null) {
            return;
        }

        Inventory inventory = holder.inventory();
        int slot = packet.getSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            GeyserLogger logger = session.getGeyser().getLogger();
            logger.warning("Slot of ClientboundContainerSetSlotPacket sent to " + session.bedrockUsername()
                    + " is out of bounds! Was: " + slot + " for container: " + packet.getContainerId());
            if (logger.isDebug()) {
                logger.debug(packet.toString());
                logger.debug(inventory.toString());
            }

            return;
        }

        if (holder.translator() instanceof SmithingInventoryTranslator) {
            updateSmithingTableOutput(slot, packet.getItem(), holder);
        } else {
            updateCraftingGrid(slot, packet.getItem(), holder);
        }

        GeyserItemStack newItem = GeyserItemStack.from(session, packet.getItem());
        session.getBundleCache().initialize(newItem);

        holder.inventory().setItem(slot, newItem, session);
        holder.updateSlot(slot);


        int stateId = packet.getStateId();
        session.setEmulatePost1_16Logic(stateId > 0 || stateId != inventory.getStateId());
        inventory.setStateId(stateId);
    }


    private static void updateCraftingGrid(int slot, ItemStack item, InventoryHolder<? extends Inventory> holder) {

        if (slot != 0) {
            return;
        }


        int gridSize = holder.translator().getGridSize();
        if (gridSize == -1) {
            return;
        }

        GeyserSession session = holder.session();


        if (session.getContainerOutputFuture() != null) {
            session.getContainerOutputFuture().cancel(false);
        }

        if (InventoryUtils.isEmpty(item)) {
            return;
        }

        session.setContainerOutputFuture(session.scheduleInEventLoop(() -> {
            int offset = gridSize == 4 ? 28 : 32;
            int gridDimensions = gridSize == 4 ? 2 : 3;
            int firstRow = -1, height = -1;
            int firstCol = -1, width = -1;
            for (int row = 0; row < gridDimensions; row++) {
                for (int col = 0; col < gridDimensions; col++) {
                    if (!holder.inventory().getItem(col + (row * gridDimensions) + 1).isEmpty()) {
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


            if (firstRow == -1) {
                return;
            }

            height += -firstRow + 1;
            width += -firstCol + 1;

            if (InventoryUtils.getValidRecipe(session, item, holder.inventory()::getItem, gridDimensions, firstRow,
                    height, firstCol, width) != null) {

                return;
            }

            int newRecipeId = session.getLastRecipeNetId().incrementAndGet();

            ItemData[] ingredients = new ItemData[height * width];

            List<SlotDisplay> javaIngredients = new ArrayList<>(height * width);
            int index = 0;
            for (int row = firstRow; row < height + firstRow; row++) {
                for (int col = firstCol; col < width + firstCol; col++) {
                    GeyserItemStack geyserItemStack = holder.inventory().getItem(col + (row * gridDimensions) + 1);
                    ingredients[index] = geyserItemStack.getItemData(session);
                    javaIngredients.add(geyserItemStack.asIngredient());

                    InventorySlotPacket slotPacket = new InventorySlotPacket();
                    slotPacket.setContainerId(ContainerId.UI);
                    slotPacket.setSlot(col + (row * gridDimensions) + offset);
                    slotPacket.setItem(ItemData.AIR);
                    session.sendUpstreamPacket(slotPacket);
                    index++;
                }
            }

            GeyserRecipe geyserRecipe = new GeyserShapedRecipe(ThreadLocalRandom.current().nextInt(), newRecipeId,
                    width, height, javaIngredients, new ItemStackSlotDisplay(item));
            session.getCraftingRecipes().put(newRecipeId, geyserRecipe);

            CraftingDataPacket craftPacket = new CraftingDataPacket();
            craftPacket.getCraftingData().add(geyserRecipe.asRecipeData(session).get(0));
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

    static void updateSmithingTableOutput(int slot, ItemStack output, InventoryHolder<?> holder) {
        if (slot != SmithingInventoryTranslator.OUTPUT) {
            return;
        }
        GeyserSession session = holder.session();


        if (session.getContainerOutputFuture() != null) {
            session.getContainerOutputFuture().cancel(false);
        }

        if (InventoryUtils.isEmpty(output)) {
            return;
        }

        Inventory inventory = holder.inventory();
        session.setContainerOutputFuture(session.scheduleInEventLoop(() -> {
            GeyserItemStack template = inventory.getItem(SmithingInventoryTranslator.TEMPLATE);
            if (!template.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {

                return;
            }

            GeyserItemStack input = inventory.getItem(SmithingInventoryTranslator.INPUT);
            GeyserItemStack material = inventory.getItem(SmithingInventoryTranslator.MATERIAL);
            GeyserItemStack geyserOutput = GeyserItemStack.from(session, output);

            for (GeyserSmithingRecipe recipe : session.getSmithingRecipes()) {
                if (InventoryUtils.acceptsAsInput(session, recipe.result(), geyserOutput)
                && InventoryUtils.acceptsAsInput(session, recipe.base(), input)
                && InventoryUtils.acceptsAsInput(session, recipe.addition(), material)
                && InventoryUtils.acceptsAsInput(session, recipe.template(), template)) {

                    return;
                }
            }

            GeyserSmithingRecipe geyserRecipe = new GeyserSmithingRecipe(
                ThreadLocalRandom.current().nextInt(),
                session.getLastRecipeNetId().incrementAndGet(),
                template.asIngredient(),
                input.asIngredient(),
                material.asIngredient(),
                new ItemStackSlotDisplay(output)
            );
            session.getSmithingRecipes().add(geyserRecipe);

            CraftingDataPacket craftPacket = new CraftingDataPacket();
            craftPacket.getCraftingData().add(geyserRecipe.asRecipeData(session).get(0));
            session.sendUpstreamPacket(craftPacket);


            InventorySlotPacket slotPacket = new InventorySlotPacket();
            slotPacket.setContainerId(ContainerId.UI);
            slotPacket.setSlot(holder.translator().javaSlotToBedrock(SmithingInventoryTranslator.MATERIAL));
            slotPacket.setItem(ItemData.AIR);
            session.sendUpstreamPacket(slotPacket);

            holder.updateSlot(SmithingInventoryTranslator.MATERIAL);
        }, 150, TimeUnit.MILLISECONDS));
    }
}
