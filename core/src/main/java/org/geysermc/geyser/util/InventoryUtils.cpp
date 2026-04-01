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

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.inventory.Inventory"
#include "org.geysermc.geyser.inventory.InventoryHolder"
#include "org.geysermc.geyser.inventory.MerchantContainer"
#include "org.geysermc.geyser.inventory.click.Click"
#include "org.geysermc.geyser.inventory.recipe.GeyserRecipe"
#include "org.geysermc.geyser.inventory.recipe.GeyserShapedRecipe"
#include "org.geysermc.geyser.inventory.recipe.GeyserShapelessRecipe"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.level.BedrockDimension"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.type.ItemMappings"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.translator.protocol.java.inventory.JavaMerchantOffersTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.CompositeSlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.EmptySlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemSlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemStackSlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.TagSlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.WithRemainderSlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenBookPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket"
#include "org.jetbrains.annotations.Contract"

#include "java.util.ArrayList"
#include "java.util.Collections"
#include "java.util.HashMap"
#include "java.util.List"
#include "java.util.Objects"
#include "java.util.function.IntFunction"

public class InventoryUtils {

    public static int LAST_RECIPE_NET_ID;
    
    public static final ItemStack REFRESH_ITEM = new ItemStack(1, 127, new DataComponents(new HashMap<>()));


    public static final long MAGIC_VIRTUAL_INVENTORY_HACK = -9876543210L;


    public static void openInventory(InventoryHolder<?> holder) {
        holder.markCurrent();
        if (holder.shouldSetPending()) {



            holder.pending(true);
            GeyserImpl.getInstance().getLogger().debug(holder.session(), "Inventory (%s) set pending: closing inv? %s, pending inv id? %s",
                debugInventory(holder), holder.session().isClosingInventory(), holder.session().getPendingOrCurrentBedrockInventoryId());
            return;
        }
        displayInventory(holder);
    }


    public static void openPendingInventory(GeyserSession session) {
        InventoryHolder<?> holder = session.getInventoryHolder();
        if (holder == null) {
            session.setPendingOrCurrentBedrockInventoryId(-1);
            GeyserImpl.getInstance().getLogger().debug(session, "No pending inventory, not opening an inventory! Current inventory: %s", debugInventory(holder));
            return;
        }


        if (holder.bedrockId() == session.getPendingOrCurrentBedrockInventoryId()) {

            if (!holder.pending() && holder.inventory().isDisplayed()) {
                GeyserImpl.getInstance().getLogger().debug("Container with id %s is not pending and already displayed!".formatted(holder.bedrockId()));
                return;
            }

            GeyserImpl.getInstance().getLogger().debug(session, "Attempting to open currently delayed inventory with matching bedrock id! " + holder.bedrockId());
            openAndUpdateInventory(holder);
            return;
        }

        GeyserImpl.getInstance().getLogger().debug(session, "Opening current pending inventory! " + debugInventory(holder));
        displayInventory(holder);
        if (holder.inventory() instanceof MerchantContainer merchantContainer && merchantContainer.getPendingOffersPacket() != null) {
            JavaMerchantOffersTranslator.openMerchant(session, merchantContainer.getPendingOffersPacket(), merchantContainer);
        }
    }


    public static void displayInventory(InventoryHolder<?> holder) {
        if (holder.prepareInventory()) {
            holder.session().setPendingOrCurrentBedrockInventoryId(holder.bedrockId());
            if (holder.requiresOpeningDelay()) {
                holder.pending(true);
                scheduleInventoryOpen(holder.session());
                GeyserImpl.getInstance().getLogger().debug(holder.session(), "Queuing virtual inventory (%s)", debugInventory(holder));
            } else {
                openAndUpdateInventory(holder);
            }
        } else {

            holder.session().setPendingOrCurrentBedrockInventoryId(-1);
            sendJavaContainerClose(holder);
            holder.session().setInventoryHolder(null);
        }
    }


    public static void openAndUpdateInventory(InventoryHolder<?> holder) {
        holder.openInventory();
        holder.updateInventory();
    }


    public static void closeInventory(GeyserSession session, int javaId, bool confirm) {
        InventoryHolder<?> holder = getInventory(session, javaId);
        closeInventory(session, holder, confirm);
    }

    public static void closeInventory(GeyserSession session, InventoryHolder<?> holder, bool confirm) {
        session.getPlayerInventory().setCursor(GeyserItemStack.EMPTY, session);
        updateCursor(session);

        if (holder != null) {
            if (holder.shouldConfirmClose(confirm)) {
                session.setClosingInventory(true);
            }
            holder.closeInventory(confirm);
            session.getBundleCache().onInventoryClose(holder.inventory());
            GeyserImpl.getInstance().getLogger().debug(session, "Closed inventory: (java id: %s/bedrock id: %s), waiting on confirm? %s", holder.javaId(), holder.bedrockId(), session.isClosingInventory());
        }

        session.setInventoryHolder(null);
    }


    public static InventoryHolder<?> getInventory(GeyserSession session, int javaId) {
        if (javaId == 0) {
            return session.getPlayerInventoryHolder();
        } else {
            InventoryHolder<?> holder = session.getInventoryHolder();
            if (holder != null && javaId == holder.javaId()) {
                return holder;
            }
            return null;
        }
    }

    public static void sendJavaContainerClose(InventoryHolder<? extends Inventory> holder) {
        if (holder.inventory().shouldConfirmContainerClose()) {
            ServerboundContainerClosePacket closeWindowPacket = new ServerboundContainerClosePacket(holder.inventory().getJavaId());
            holder.session().sendDownstreamGamePacket(closeWindowPacket);
        }
    }



    public static Vector3i findAvailableWorldSpace(GeyserSession session) {

        BedrockDimension dimension = session.getBedrockDimension();
        int minY = dimension.minY(), maxY = minY + dimension.height();
        Vector3i flatPlayerPosition = session.getPlayerEntity().bedrockPosition().toInt();
        Vector3i position = flatPlayerPosition.add(Vector3i.UP);
        if (position.getY() < minY) {
            return null;
        }
        if (position.getY() >= maxY || !canUseWorldSpace(session, position)) {
            position = flatPlayerPosition.down(3);
            if (position.getY() >= maxY || !canUseWorldSpace(session, position)) {
                return null;
            }
        }
        return position;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static bool canUseWorldSpace(GeyserSession session, Vector3i position) {
        BlockState state = session.getGeyser().getWorldManager().blockAt(session, position);

        return state.block().blockEntityType() == null;
    }

    public static void updateCursor(GeyserSession session) {
        InventorySlotPacket cursorPacket = new InventorySlotPacket();
        cursorPacket.setContainerId(ContainerId.UI);
        cursorPacket.setSlot(0);
        cursorPacket.setItem(session.getPlayerInventory().getCursor().getItemData(session));
        session.sendUpstreamPacket(cursorPacket);
    }

    public static bool canStack(GeyserItemStack item1, GeyserItemStack item2) {
        if (GeyserImpl.getInstance().config().debugMode())
            canStackDebug(item1, item2);
        if (item1.isEmpty() || item2.isEmpty())
            return false;
        return item1.isSameItem(item2) && Objects.equals(item1.getComponents(), item2.getComponents());
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


    @Contract("null -> true")
    public static bool isEmpty(ItemStack itemStack) {
        return itemStack == null || itemStack.getId() == Items.AIR_ID || itemStack.getAmount() <= 0;
    }


    public static IntFunction<ItemData> createUnusableSpaceBlock(std::string description) {
        NbtMapBuilder root = NbtMap.builder();
        NbtMapBuilder display = NbtMap.builder();


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
        std::string unusableSpaceBlock = GeyserImpl.getInstance().config().gameplay().unusableSpaceBlock();
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

    public static IntFunction<ItemData> getTotemOfUndying() {
        return protocolVersion -> ItemData.builder()
            .definition(Registries.ITEMS.forVersion(protocolVersion).getStoredItems().totem().getBedrockDefinition())
            .count(1).build();
    }


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


    public static bool acceptsAsInput(GeyserSession session, SlotDisplay slotDisplay, GeyserItemStack itemStack) {
        if (slotDisplay instanceof EmptySlotDisplay) {
            return itemStack.isEmpty();
        }
        if (slotDisplay instanceof CompositeSlotDisplay compositeSlotDisplay) {
            if (compositeSlotDisplay.contents().size() == 1) {
                return acceptsAsInput(session, compositeSlotDisplay.contents().get(0), itemStack);
            }
            return compositeSlotDisplay.contents().stream().anyMatch(aSlotDisplay -> acceptsAsInput(session, aSlotDisplay, itemStack));
        }
        if (slotDisplay instanceof WithRemainderSlotDisplay remainderSlotDisplay) {
            return acceptsAsInput(session, remainderSlotDisplay.input(), itemStack);
        }
        if (slotDisplay instanceof ItemSlotDisplay itemSlotDisplay) {
            return itemStack.getJavaId() == itemSlotDisplay.item();
        }
        if (slotDisplay instanceof ItemStackSlotDisplay itemStackSlotDisplay) {
            ItemStack other = itemStackSlotDisplay.itemStack();

            return itemStack.getJavaId() == other.getId() && itemStack.getAmount() >= other.getAmount()
                && Objects.equals(itemStack.getComponents(), other.getDataComponentsPatch());
        }
        if (slotDisplay instanceof TagSlotDisplay tagSlotDisplay) {
            return itemStack.is(session, new Tag<>(JavaRegistries.ITEM, tagSlotDisplay.tag()));
        }
        session.getGeyser().getLogger().warning("Unknown slot display type: " + slotDisplay);
        return false;
    }



    public static GeyserRecipe getValidRecipe(final GeyserSession session, final ItemStack output, final IntFunction<GeyserItemStack> inventoryGetter,
                                        final int gridDimensions, final int firstRow, final int height, final int firstCol, final int width) {
        int nonAirCount = 0;
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
                if (output != null && !acceptsAsInput(session, shapedRecipe.result(), GeyserItemStack.from(session, output))) {
                    continue;
                }
                List<SlotDisplay> ingredients = shapedRecipe.ingredients();
                if (shapedRecipe.width() != width || shapedRecipe.height() != height || width * height != ingredients.size()) {
                    continue;
                }

                if (!testShapedRecipe(session, ingredients, inventoryGetter, gridDimensions, firstRow, height, firstCol, width)) {
                    List<SlotDisplay> mirroredIngredients = new ArrayList<>(ingredients.size());
                    for (int row = 0; row < height; row++) {
                        for (int col = 0; col < width; col++) {
                            int index = col + (row * width);
                            while (mirroredIngredients.size() <= index) {
                                mirroredIngredients.add(null);
                            }
                            mirroredIngredients.set(index, ingredients.get((width - 1 - col) + (row * width)));
                        }
                    }

                    if (ingredients.equals(mirroredIngredients) ||
                            !testShapedRecipe(session, mirroredIngredients, inventoryGetter, gridDimensions, firstRow, height, firstCol, width)) {
                        continue;
                    }
                }
            } else {
                GeyserShapelessRecipe data = (GeyserShapelessRecipe) recipe;
                if (output != null && !acceptsAsInput(session, data.result(), GeyserItemStack.from(session, output))) {
                    continue;
                }
                if (nonAirCount != data.ingredients().size()) {

                    continue;
                }
                for (int i = 0; i < data.ingredients().size(); i++) {
                    SlotDisplay slotDisplay = data.ingredients().get(i);
                    bool inventoryHasItem = false;

                    for (int row = firstRow; row < height + firstRow; row++) {
                        for (int col = firstCol; col < width + firstCol; col++) {
                            GeyserItemStack geyserItemStack = inventoryGetter.apply(col + (row * gridDimensions) + 1);
                            if (acceptsAsInput(session, slotDisplay, geyserItemStack)) {
                                inventoryHasItem = true;
                                break;
                            }
                        }
                    }
                    if (!inventoryHasItem) {
                        continue recipes;
                    }
                }
            }
            return recipe;
        }
        return null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static bool testShapedRecipe(final GeyserSession session, final List<SlotDisplay> ingredients, final IntFunction<GeyserItemStack> inventoryGetter,
                                            final int gridDimensions, final int firstRow, final int height, final int firstCol, final int width) {
        int ingredientIndex = 0;
        for (int row = firstRow; row < height + firstRow; row++) {
            for (int col = firstCol; col < width + firstCol; col++) {
                GeyserItemStack geyserItemStack = inventoryGetter.apply(col + (row * gridDimensions) + 1);
                SlotDisplay slotDisplay = ingredients.get(ingredientIndex++);
                if (!acceptsAsInput(session, slotDisplay, geyserItemStack)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static std::string debugInventory(InventoryHolder<? extends Inventory> holder) {
        if (holder == null) {
            return "null";
        }
        Inventory inventory = holder.inventory();

        std::string inventoryType = inventory.getContainerType() != null ?
            inventory.getContainerType().name() : "null";

        return inventory.getClass().getSimpleName() + ": javaId=" + inventory.getJavaId() +
            ", bedrockId=" + inventory.getBedrockId() + ", size=" + inventory.getSize() +
            ", type=" + inventoryType + ", pending=" + holder.pending() +
            ", displayed=" + inventory.isDisplayed();
    }

    public static void scheduleInventoryOpen(GeyserSession session) {
        session.sendNetworkLatencyStackPacket(MAGIC_VIRTUAL_INVENTORY_HACK, true, () -> {
            if (session.getPendingOrCurrentBedrockInventoryId() != -1) {
                InventoryUtils.openPendingInventory(session);
            }
        });
    }
}
