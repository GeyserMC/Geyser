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

package org.geysermc.geyser.translator.inventory;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.erosion.util.LecternUtils;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.LecternContainer;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.inventory.updater.ContainerInventoryUpdater;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.LecternBlock;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.BlockEntityUtils;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.WritableBookContent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.WrittenBookContent;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerButtonClickPacket;

public class LecternInventoryTranslator extends AbstractBlockInventoryTranslator {

    /**
     * Hack: Java opens a lectern first, and then follows it up with a ClientboundContainerSetContentPacket
     * to actually send the book's contents. We delay opening the inventory until the book was sent.
     */
    private boolean receivedBook = false;

    public LecternInventoryTranslator() {
        super(1, Blocks.LECTERN, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.LECTERN , ContainerInventoryUpdater.INSTANCE);
    }

    @Override
    public boolean prepareInventory(GeyserSession session, Inventory inventory) {
        super.prepareInventory(session, inventory);
        if (((LecternContainer) inventory).isFakeLectern()) {
            // See JavaOpenBookTranslator; this isn't a lectern but a book in the player inventory
            updateBook(session, inventory, inventory.getItem(0));
            receivedBook = true;
        } else {
            receivedBook = false; // We have to wait until we get the book
        }
        return true;
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        // Hacky, but we're dealing with LECTERNS! It cannot not be hacky.
        // "initialized" indicates whether we've received the book from the Java server yet.
        // dropping lectern book is the fun workaround when we have to enter the gui to drop the book.
        // Since we leave it immediately... don't open it!
        if (receivedBook && !session.isDroppingLecternBook()) {
            super.openInventory(session, inventory);
        }
    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {
        // Of course, sending a simple ContainerClosePacket, or even breaking the block doesn't work to close a lectern.
        // Heck, the latter crashes the client xd
        // BDS just sends an empty base lectern tag... that kicks out the client. Fine. Let's do that!
        LecternContainer lecternContainer = (LecternContainer) inventory;
        Vector3i position = lecternContainer.isUsingRealBlock() ? session.getLastInteractionBlockPosition() : inventory.getHolderPosition();
        var baseLecternTag = LecternUtils.getBaseLecternTag(position.getX(), position.getY(), position.getZ(), 0);
        BlockEntityUtils.updateBlockEntity(session, baseLecternTag.build(), position);

        super.closeInventory(session, inventory); // Removes the fake blocks if need be

        // Now: Restore the lectern, if it actually exists
        if (lecternContainer.isUsingRealBlock()) {
            boolean hasBook = session.getGeyser().getWorldManager().blockAt(session, position).getValue(Properties.HAS_BOOK, false);

            NbtMap map = LecternBlock.getBaseLecternTag(position, hasBook);
            BlockEntityUtils.updateBlockEntity(session, map, position);
        }
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
        if (key == 0) { // Lectern page update
            LecternContainer lecternContainer = (LecternContainer) inventory;
            lecternContainer.setCurrentBedrockPage(value / 2);
            lecternContainer.setBlockEntityTag(lecternContainer.getBlockEntityTag().toBuilder().putInt("page", lecternContainer.getCurrentBedrockPage()).build());
            BlockEntityUtils.updateBlockEntity(session, lecternContainer.getBlockEntityTag(), lecternContainer.getPosition());
        }
    }

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        GeyserItemStack itemStack = inventory.getItem(0);
        if (!itemStack.isEmpty()) {
            boolean isDropping = session.isDroppingLecternBook();
            updateBook(session, inventory, itemStack);

            if (!receivedBook && !isDropping) {
                receivedBook = true;
                openInventory(session, inventory);
            }
        }
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        // If we're not in a real lectern, the Java server thinks we are still in the player inventory.
        if (((LecternContainer) inventory).isFakeLectern()) {
            InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR.updateSlot(session, session.getPlayerInventory(), slot);
            return;
        }
        super.updateSlot(session, inventory, slot);
        if (slot == 0) {
            updateBook(session, inventory, inventory.getItem(0));
        }
    }

    /**
     * Translate the data of the book in the lectern into a block entity tag.
     */
    private void updateBook(GeyserSession session, Inventory inventory, GeyserItemStack book) {
        LecternContainer lecternContainer = (LecternContainer) inventory;
        if (session.isDroppingLecternBook()) {
            // We have to enter the inventory GUI to eject the book
            ServerboundContainerButtonClickPacket packet = new ServerboundContainerButtonClickPacket(inventory.getJavaId(), 3);
            session.sendDownstreamGamePacket(packet);
            session.setDroppingLecternBook(false);
            InventoryUtils.closeInventory(session, inventory.getJavaId(), false);
        } else if (lecternContainer.getBlockEntityTag() == null) {
            Vector3i position = lecternContainer.isUsingRealBlock() ?
                    session.getLastInteractionBlockPosition() : inventory.getHolderPosition();

            NbtMap blockEntityTag;
            if (book.getComponents() != null) {
                int pages = 0;
                WrittenBookContent writtenBookComponents = book.getComponents().get(DataComponentType.WRITTEN_BOOK_CONTENT);
                if (writtenBookComponents != null) {
                    pages = writtenBookComponents.getPages().size();
                } else {
                    WritableBookContent writableBookComponents = book.getComponents().get(DataComponentType.WRITABLE_BOOK_CONTENT);
                    if (writableBookComponents != null) {
                        pages = writableBookComponents.getPages().size();
                    }
                }

                ItemData itemData = book.getItemData(session);
                NbtMapBuilder lecternTag = LecternBlock.getBaseLecternTag(position, pages);
                lecternTag.putCompound("book", NbtMap.builder()
                        .putByte("Count", (byte) itemData.getCount())
                        .putShort("Damage", (short) 0)
                        .putString("Name", "minecraft:written_book")
                        .putCompound("tag", itemData.getTag())
                        .build());
                lecternTag.putInt("page", lecternContainer.getCurrentBedrockPage());
                blockEntityTag = lecternTag.build();
            } else {
                // There is *a* book here, but... no NBT.
                blockEntityTag = LecternBlock.getBaseLecternTag(position, true);
            }

            // Even with serverside access to lecterns, we don't easily know which lectern this is, so we need to rebuild
            // the block entity tag
            lecternContainer.setBlockEntityTag(blockEntityTag);
            lecternContainer.setPosition(position);

            BlockEntityUtils.updateBlockEntity(session, blockEntityTag, position);
        }
    }

    @Override
    public Inventory createInventory(String name, int windowId, ContainerType containerType, PlayerInventory playerInventory) {
        return new LecternContainer(name, windowId, this.size + playerInventory.getSize(), containerType, playerInventory);
    }
}
