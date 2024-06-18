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

package org.geysermc.geyser.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerHotbarPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.*;
import org.geysermc.geyser.inventory.click.Click;
import org.geysermc.geyser.inventory.recipe.GeyserRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserShapedRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserShapelessRecipe;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.inventory.LecternInventoryTranslator;
import org.geysermc.geyser.translator.inventory.chest.DoubleChestInventoryTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.Ingredient;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundPickItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

public class InventoryUtils {
    /**
     * Stores the last used recipe network ID. Since 1.16.200 (and for server-authoritative inventories),
     * each recipe needs a unique network ID (or else in .200 the client crashes).
     */
    public static int LAST_RECIPE_NET_ID;
    
    public static final ItemStack REFRESH_ITEM = new ItemStack(1, 127, new DataComponents(new HashMap<>()));

    public static void openInventory(GeyserSession session, Inventory inventory) {
        session.setOpenInventory(inventory);
        if (session.isClosingInventory() || !session.getUpstream().isInitialized()) {
            // Wait for close confirmation from client before opening the new inventory.
            // Handled in BedrockContainerCloseTranslator
            // or - client hasn't yet loaded in; wait until inventory is shown
            inventory.setPending(true);
            return;
        }
        displayInventory(session, inventory);
    }

    public static void displayInventory(GeyserSession session, Inventory inventory) {
        InventoryTranslator translator = session.getInventoryTranslator();
        if (translator != null && translator.prepareInventory(session, inventory)) {
            if (translator instanceof DoubleChestInventoryTranslator && !((Container) inventory).isUsingRealBlock()) {
                session.scheduleInEventLoop(() -> {
                    Inventory openInv = session.getOpenInventory();
                    if (openInv != null && openInv.getJavaId() == inventory.getJavaId()) {
                        translator.openInventory(session, inventory);
                        translator.updateInventory(session, inventory);
                        openInv.setDisplayed(true);
                    } else if (openInv != null && openInv.isPending()) {
                        // Presumably, this inventory is no longer relevant, and the client doesn't care about it
                        displayInventory(session, openInv);
                    }
                }, 200, TimeUnit.MILLISECONDS);
            } else {
                translator.openInventory(session, inventory);
                translator.updateInventory(session, inventory);
                inventory.setDisplayed(true);
            }
        } else {
            session.setOpenInventory(null);
        }
    }

    public static void closeInventory(GeyserSession session, int javaId, boolean confirm) {
        session.getPlayerInventory().setCursor(GeyserItemStack.EMPTY, session);
        updateCursor(session);

        Inventory inventory = getInventory(session, javaId);
        if (inventory != null) {
            InventoryTranslator translator = session.getInventoryTranslator();
            translator.closeInventory(session, inventory);
            if (confirm && inventory.isDisplayed() && !inventory.isPending()
                    && !(translator instanceof LecternInventoryTranslator) // Closing lecterns is not followed with a close confirmation
            ) {
                session.setClosingInventory(true);
            }
        }
        session.setInventoryTranslator(InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR);
        session.setOpenInventory(null);
    }

    public static @Nullable Inventory getInventory(GeyserSession session, int javaId) {
        if (javaId == 0) {
            // ugly hack: lecterns aren't their own inventory on Java, and can hence be closed with e.g. an id of 0
            if (session.getOpenInventory() instanceof LecternContainer) {
                return session.getOpenInventory();
            }
            return session.getPlayerInventory();
        } else {
            Inventory openInventory = session.getOpenInventory();
            if (openInventory != null && javaId == openInventory.getJavaId()) {
                return openInventory;
            }
            return null;
        }
    }

    /**
     * Finds a usable block space in the world to place a fake inventory block, and returns the position.
     */
    @Nullable
    public static Vector3i findAvailableWorldSpace(GeyserSession session) {
        // Check if a fake block can be placed, either above the player or beneath.
        BedrockDimension dimension = session.getChunkCache().getBedrockDimension();
        int minY = dimension.minY(), maxY = minY + dimension.height();
        Vector3i flatPlayerPosition = session.getPlayerEntity().getPosition().toInt();
        Vector3i position = flatPlayerPosition.add(Vector3i.UP);
        if (position.getY() < minY) {
            return null;
        }
        if (position.getY() >= maxY) {
            position = flatPlayerPosition.sub(0, 4, 0);
            if (position.getY() >= maxY) {
                return null;
            }
        }
        return position;
    }

    public static void updateCursor(GeyserSession session) {
        InventorySlotPacket cursorPacket = new InventorySlotPacket();
        cursorPacket.setContainerId(ContainerId.UI);
        cursorPacket.setSlot(0);
        cursorPacket.setItem(session.getPlayerInventory().getCursor().getItemData(session));
        session.sendUpstreamPacket(cursorPacket);
    }

    public static boolean canStack(GeyserItemStack item1, GeyserItemStack item2) {
        if (GeyserImpl.getInstance().getConfig().isDebugMode())
            canStackDebug(item1, item2);
        if (item1.isEmpty() || item2.isEmpty())
            return false;
        return item1.getJavaId() == item2.getJavaId() && Objects.equals(item1.getComponents(), item2.getComponents());
    }

    private static void canStackDebug(GeyserItemStack item1, GeyserItemStack item2) {
        DataComponents components1 = item1.getComponents();
        DataComponents components2 = item2.getComponents();
        if (components1 != null && components2 != null) {
            if (components1.hashCode() == components2.hashCode() && !components1.equals(components2)) {
                GeyserImpl.getInstance().getLogger().error("DEBUG: DataComponents hash collision");
                GeyserImpl.getInstance().getLogger().error("hash: " + components1.hashCode());
                GeyserImpl.getInstance().getLogger().error("components1: " + components1);
                GeyserImpl.getInstance().getLogger().error("components2: " + components2);
            }
        }
    }

    /**
     * Checks to see if an item stack represents air or has no count.
     */
    @Contract("null -> true")
    public static boolean isEmpty(@Nullable ItemStack itemStack) {
        return itemStack == null || itemStack.getId() == Items.AIR_ID || itemStack.getAmount() <= 0;
    }

    /**
     * Returns a barrier block with custom name and lore to explain why
     * part of the inventory is unusable.
     *
     * @param description the description
     * @return the unusable space block
     */
    public static IntFunction<ItemData> createUnusableSpaceBlock(String description) {
        NbtMapBuilder root = NbtMap.builder();
        NbtMapBuilder display = NbtMap.builder();

        // Not ideal to use log here but we dont get a session
        display.putString("Name", ChatColor.RESET + GeyserLocale.getLocaleStringLog("geyser.inventory.unusable_item.name"));
        display.putList("Lore", NbtType.STRING, Collections.singletonList(ChatColor.RESET + ChatColor.DARK_PURPLE + description));

        root.put("display", display.build());
        return protocolVersion -> ItemData.builder()
                .definition(getUnusableSpaceBlockDefinition(protocolVersion))
                .count(1)
                .tag(root.build()).build();
    }

    private static ItemDefinition getUnusableSpaceBlockDefinition(int protocolVersion) {
        ItemMappings mappings = Registries.ITEMS.forVersion(protocolVersion);
        String unusableSpaceBlock = GeyserImpl.getInstance().getConfig().getUnusableSpaceBlock();
        ItemDefinition itemDefinition = mappings.getDefinition(unusableSpaceBlock);

        if (itemDefinition == null) {
            GeyserImpl.getInstance().getLogger().error("Invalid value " + unusableSpaceBlock + ". Resorting to barrier block.");
            return mappings.getStoredItems().barrier().getBedrockDefinition();
        } else {
            return itemDefinition;
        }
    }

    public static IntFunction<ItemData> getUpgradeTemplate() {
        return protocolVersion -> ItemData.builder()
                .definition(Registries.ITEMS.forVersion(protocolVersion).getStoredItems().upgradeTemplate().getBedrockDefinition())
                .count(1).build();
    }

    /**
     * See {@link #findOrCreateItem(GeyserSession, String)}. This is for finding a specified {@link ItemStack}.
     *
     * @param session the Bedrock client's session
     * @param itemStack the item to try to find a match for. NBT will also be accounted for.
     */
    public static void findOrCreateItem(GeyserSession session, ItemStack itemStack) {
        if (isEmpty(itemStack)) {
            return;
        }
        PlayerInventory inventory = session.getPlayerInventory();

        // Check hotbar for item
        for (int i = 36; i < 45; i++) {
            GeyserItemStack geyserItem = inventory.getItem(i);
            if (geyserItem.isEmpty()) {
                continue;
            }
            // If this is the item we're looking for
            if (geyserItem.getJavaId() == itemStack.getId() && Objects.equals(geyserItem.getComponents(), itemStack.getDataComponents())) { //TODO verify
                setHotbarItem(session, i);
                // Don't check inventory if item was in hotbar
                return;
            }
        }

        // Check inventory for item
        for (int i = 9; i < 36; i++) {
            GeyserItemStack geyserItem = inventory.getItem(i);
            if (geyserItem.isEmpty()) {
                continue;
            }
            // If this is the item we're looking for
            if (geyserItem.getJavaId() == itemStack.getId() && Objects.equals(geyserItem.getComponents(), itemStack.getDataComponents())) { //TODO verify
                ServerboundPickItemPacket packetToSend = new ServerboundPickItemPacket(i); // https://wiki.vg/Protocol#Pick_Item
                session.sendDownstreamGamePacket(packetToSend);
                return;
            }
        }

        // If we still have not found the item, and we're in creative, ask for the item from the server.
        if (session.getGameMode() == GameMode.CREATIVE) {
            int slot = findEmptyHotbarSlot(inventory);

            ServerboundSetCreativeModeSlotPacket actionPacket = new ServerboundSetCreativeModeSlotPacket((short) slot,
                    itemStack);
            if ((slot - 36) != inventory.getHeldItemSlot()) {
                setHotbarItem(session, slot);
            }
            session.sendDownstreamGamePacket(actionPacket);
        }
    }

    // Please remove!!!
    public static void findOrCreateItem(GeyserSession session, String itemName) {
        findOrCreateItem(session, Registries.JAVA_ITEM_IDENTIFIERS.getOrDefault(itemName, Items.AIR));
    }

    /**
     * Attempt to find the specified item name in the session's inventory.
     * If it is found and in the hotbar, set the user's held item to that slot.
     * If it is found in another part of the inventory, move it.
     * If it is not found and the user is in creative mode, create the item,
     * overriding the current item slot if no other hotbar slots are empty, or otherwise selecting the empty slot.
     * <p>
     * This attempts to mimic Java Edition behavior as best as it can.
     * @param session the Bedrock client's session
     * @param item the Java item to search/select for
     */
    public static void findOrCreateItem(GeyserSession session, Item item) {
        // Get the inventory to choose a slot to pick
        PlayerInventory inventory = session.getPlayerInventory();

        if (item == Items.AIR) {
            return;
        }

        // Check hotbar for item
        for (int i = 36; i < 45; i++) {
            GeyserItemStack geyserItem = inventory.getItem(i);
            if (geyserItem.isEmpty()) {
                continue;
            }
            // If this isn't the item we're looking for
            if (!geyserItem.asItem().equals(item)) {
                continue;
            }

            setHotbarItem(session, i);
            // Don't check inventory if item was in hotbar
            return;
        }

        // Check inventory for item
        for (int i = 9; i < 36; i++) {
            GeyserItemStack geyserItem = inventory.getItem(i);
            if (geyserItem.isEmpty()) {
                continue;
            }
            // If this isn't the item we're looking for
            if (!geyserItem.asItem().equals(item)) {
                continue;
            }

            ServerboundPickItemPacket packetToSend = new ServerboundPickItemPacket(i); // https://wiki.vg/Protocol#Pick_Item
            session.sendDownstreamGamePacket(packetToSend);
            return;
        }

        // If we still have not found the item, and we're in creative, ask for the item from the server.
        if (session.getGameMode() == GameMode.CREATIVE) {
            int slot = findEmptyHotbarSlot(inventory);

            ItemMapping mapping = session.getItemMappings().getMapping(item);
            ServerboundSetCreativeModeSlotPacket actionPacket = new ServerboundSetCreativeModeSlotPacket((short)slot,
                    new ItemStack(mapping.getJavaItem().javaId()));
            if ((slot - 36) != inventory.getHeldItemSlot()) {
                setHotbarItem(session, slot);
            }
            session.sendDownstreamGamePacket(actionPacket);
        }
    }

    /**
     * @return the first empty slot found in this inventory, or else the player's currently held slot.
     */
    private static int findEmptyHotbarSlot(PlayerInventory inventory) {
        int slot = inventory.getHeldItemSlot() + 36;
        if (!inventory.getItemInHand().isEmpty()) { // Otherwise we should just use the current slot
            for (int i = 36; i < 45; i++) {
                if (inventory.getItem(i).isEmpty()) {
                    slot = i;
                    break;
                }
            }
        }
        return slot;
    }

    /**
     * Changes the held item slot to the specified slot
     * @param session GeyserSession
     * @param slot inventory slot to be selected
     */
    private static void setHotbarItem(GeyserSession session, int slot) {
        PlayerHotbarPacket hotbarPacket = new PlayerHotbarPacket();
        hotbarPacket.setContainerId(0);
        // Java inventory slot to hotbar slot ID
        hotbarPacket.setSelectedHotbarSlot(slot - 36);
        hotbarPacket.setSelectHotbarSlot(true);
        session.sendUpstreamPacket(hotbarPacket);
        // No need to send a Java packet as Bedrock sends a confirmation packet back that we translate
    }

    @Nullable
    public static Click getClickForHotbarSwap(int slot) {
        return switch (slot) {
            case 0 -> Click.SWAP_TO_HOTBAR_1;
            case 1 -> Click.SWAP_TO_HOTBAR_2;
            case 2 -> Click.SWAP_TO_HOTBAR_3;
            case 3 -> Click.SWAP_TO_HOTBAR_4;
            case 4 -> Click.SWAP_TO_HOTBAR_5;
            case 5 -> Click.SWAP_TO_HOTBAR_6;
            case 6 -> Click.SWAP_TO_HOTBAR_7;
            case 7 -> Click.SWAP_TO_HOTBAR_8;
            case 8 -> Click.SWAP_TO_HOTBAR_9;
            default -> null;
        };
    }

    /**
     * Test all known recipes to find a valid match
     *
     * @param output if not null, the recipe has to output this item
     */
    @Nullable
    public static GeyserRecipe getValidRecipe(final GeyserSession session, final @Nullable ItemStack output, final IntFunction<GeyserItemStack> inventoryGetter,
                                        final int gridDimensions, final int firstRow, final int height, final int firstCol, final int width) {
        int nonAirCount = 0; // Used for shapeless recipes for amount of items needed in recipe
        for (int row = firstRow; row < height + firstRow; row++) {
            for (int col = firstCol; col < width + firstCol; col++) {
                if (!inventoryGetter.apply(col + (row * gridDimensions) + 1).isEmpty()) {
                    nonAirCount++;
                }
            }
        }

        recipes:
        for (GeyserRecipe recipe : session.getCraftingRecipes().values()) {
            if (recipe.isShaped()) {
                GeyserShapedRecipe shapedRecipe = (GeyserShapedRecipe) recipe;
                if (output != null && !shapedRecipe.result().equals(output)) {
                    continue;
                }
                Ingredient[] ingredients = shapedRecipe.ingredients();
                if (shapedRecipe.width() != width || shapedRecipe.height() != height || width * height != ingredients.length) {
                    continue;
                }

                if (!testShapedRecipe(ingredients, inventoryGetter, gridDimensions, firstRow, height, firstCol, width)) {
                    Ingredient[] mirroredIngredients = new Ingredient[ingredients.length];
                    for (int row = 0; row < height; row++) {
                        for (int col = 0; col < width; col++) {
                            mirroredIngredients[col + (row * width)] = ingredients[(width - 1 - col) + (row * width)];
                        }
                    }

                    if (Arrays.equals(ingredients, mirroredIngredients) ||
                            !testShapedRecipe(mirroredIngredients, inventoryGetter, gridDimensions, firstRow, height, firstCol, width)) {
                        continue;
                    }
                }
            } else {
                GeyserShapelessRecipe data = (GeyserShapelessRecipe) recipe;
                if (output != null && !data.result().equals(output)) {
                    continue;
                }
                if (nonAirCount != data.ingredients().length) {
                    // There is an amount of items on the crafting table that is not the same as the ingredient count so this is invalid
                    continue;
                }
                for (int i = 0; i < data.ingredients().length; i++) {
                    Ingredient ingredient = data.ingredients()[i];
                    for (ItemStack itemStack : ingredient.getOptions()) {
                        boolean inventoryHasItem = false;
                        // Iterate only over the crafting table to find this item
                        crafting:
                        for (int row = firstRow; row < height + firstRow; row++) {
                            for (int col = firstCol; col < width + firstCol; col++) {
                                GeyserItemStack geyserItemStack = inventoryGetter.apply(col + (row * gridDimensions) + 1);
                                if (geyserItemStack.isEmpty()) {
                                    inventoryHasItem = itemStack == null || itemStack.getId() == 0;
                                    if (inventoryHasItem) {
                                        break crafting;
                                    }
                                } else if (itemStack.equals(geyserItemStack.getItemStack(1))) {
                                    inventoryHasItem = true;
                                    break crafting;
                                }
                            }
                        }
                        if (!inventoryHasItem) {
                            continue recipes;
                        }
                    }
                }
            }
            return recipe;
        }
        return null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean testShapedRecipe(final Ingredient[] ingredients, final IntFunction<GeyserItemStack> inventoryGetter,
                                            final int gridDimensions, final int firstRow, final int height, final int firstCol, final int width) {
        int ingredientIndex = 0;
        for (int row = firstRow; row < height + firstRow; row++) {
            for (int col = firstCol; col < width + firstCol; col++) {
                GeyserItemStack geyserItemStack = inventoryGetter.apply(col + (row * gridDimensions) + 1);
                Ingredient ingredient = ingredients[ingredientIndex++];
                if (ingredient.getOptions().length == 0) {
                    if (!geyserItemStack.isEmpty()) {
                        return false;
                    }
                } else {
                    boolean inventoryHasItem = false;
                    for (ItemStack item : ingredient.getOptions()) {
                        if (Objects.equals(geyserItemStack.getItemStack(1), item)) {
                            inventoryHasItem = true;
                            break;
                        }
                    }
                    if (!inventoryHasItem) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
