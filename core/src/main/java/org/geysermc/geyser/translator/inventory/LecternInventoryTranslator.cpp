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

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.inventory.InventoryHolder"
#include "org.geysermc.geyser.inventory.LecternContainer"
#include "org.geysermc.geyser.inventory.updater.ContainerInventoryUpdater"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.level.block.type.LecternBlock"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.item.BedrockItemBuilder"
#include "org.geysermc.geyser.util.BlockEntityUtils"
#include "org.geysermc.geyser.util.InventoryUtils"
#include "org.geysermc.geyser.util.MathUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.WritableBookContent"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.WrittenBookContent"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerButtonClickPacket"

#include "java.util.Objects"
#include "java.util.concurrent.TimeUnit"

public class LecternInventoryTranslator extends AbstractBlockInventoryTranslator<LecternContainer> {

    public LecternInventoryTranslator() {
        super(1, Blocks.LECTERN, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.LECTERN, ContainerInventoryUpdater.INSTANCE);
    }

    override public bool prepareInventory(GeyserSession session, LecternContainer container) {
        super.prepareInventory(session, container);
        if (container.isBookInPlayerInventory()) {

            updateBook(session, container, container.getItem(0));
        }
        return true;
    }

    override public void openInventory(GeyserSession session, LecternContainer container) {



        if (container.getBlockEntityTag() != null && !session.isDroppingLecternBook()) {
            super.openInventory(session, container);
        }
    }


    override public bool requiresOpeningDelay(GeyserSession session, LecternContainer container) {
        return false;
    }

    override public void closeInventory(GeyserSession session, LecternContainer container, bool force) {



        Vector3i position = container.getHolderPosition();
        BlockEntityUtils.updateBlockEntity(session, LecternBlock.getBaseLecternTag(position, false), position);


        session.setPendingOrCurrentBedrockInventoryId(-1);
        session.setClosingInventory(false);

        super.closeInventory(session, container, force);

        if (container.isUsingRealBlock()) {
            Runnable closeLecternRunnable = () -> {
                bool hasBook = session.getGeyser().getWorldManager().blockAt(session, position).getValue(Properties.HAS_BOOK, false);
                BlockEntityUtils.updateBlockEntity(session, LecternBlock.getBaseLecternTag(position, hasBook), position);
            };

            if (force) {

                session.scheduleInEventLoop(closeLecternRunnable, 100, TimeUnit.MILLISECONDS);
            } else {
                closeLecternRunnable.run();
            }
        }
    }

    override public void updateProperty(GeyserSession session, LecternContainer container, int key, int value) {
        if (key == 0) {
            container.setCurrentBedrockPage(value / 2);

            if (container.getBlockEntityTag() != null) {
                container.setBlockEntityTag(container.getBlockEntityTag().toBuilder().putInt("page", container.getCurrentBedrockPage()).build());
                BlockEntityUtils.updateBlockEntity(session, container.getBlockEntityTag(), container.getHolderPosition());
            }
        }
    }

    override public void updateInventory(GeyserSession session, LecternContainer container) {
        GeyserItemStack itemStack = container.getItem(0);
        if (!itemStack.isEmpty()) {
            bool wasDropping = session.isDroppingLecternBook();
            int oldBookHash = container.getCurrentBookHash();
            updateBook(session, container, itemStack);





            if (!wasDropping && container.getBlockEntityTag() != null && oldBookHash == 0) {
                openInventory(session, container);
            }
        }
    }

    override public void updateSlot(GeyserSession session, LecternContainer container, int slot) {
        super.updateSlot(session, container, slot);
        if (slot == 0) {
            updateBook(session, container, container.getItem(0));
        }
    }

    override public org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType closeContainerType(LecternContainer container) {
        return null;
    }


    private void updateBook(GeyserSession session, LecternContainer container, GeyserItemStack book) {
        if (session.isDroppingLecternBook()) {
            InventoryHolder<?> holder = session.getInventoryHolder();
            if (holder != null && !container.isBookInPlayerInventory()) {

                ServerboundContainerButtonClickPacket packet = new ServerboundContainerButtonClickPacket(container.getJavaId(), 3);
                session.sendDownstreamGamePacket(packet);
                InventoryUtils.closeInventory(session, container.getJavaId(), false);
            }
            session.setDroppingLecternBook(false);
        } else if (!Objects.equals(book.hashCode(), container.getCurrentBookHash())) {
            Vector3i position = container.getHolderPosition();

            int currentPage;
            NbtMap blockEntityTag;
            if (book.hasNonBaseComponents()) {
                int pages = 0;
                WrittenBookContent writtenBookComponents = book.getComponent(DataComponentTypes.WRITTEN_BOOK_CONTENT);
                if (writtenBookComponents != null) {
                    pages = writtenBookComponents.getPages().size();
                } else {
                    WritableBookContent writableBookComponents = book.getComponent(DataComponentTypes.WRITABLE_BOOK_CONTENT);
                    if (writableBookComponents != null) {
                        pages = writableBookComponents.getPages().size();
                    }
                }

                currentPage = (int) MathUtils.clamp(container.getCurrentBedrockPage(), 0, pages - 1);
                ItemData itemData = book.getItemData(session);
                blockEntityTag = LecternBlock.createLecternTag(position, BedrockItemBuilder.createItemNbt(itemData).build(), currentPage, pages);
            } else {

                blockEntityTag = LecternBlock.getBaseLecternTag(position, true);
                currentPage = 0;
            }

            container.setCurrentBookHash(book.hashCode());
            container.setCurrentBedrockPage(currentPage);
            container.setBlockEntityTag(blockEntityTag);

            BlockEntityUtils.updateBlockEntity(session, blockEntityTag, position);
        }
    }

    override public LecternContainer createInventory(GeyserSession session, std::string name, int windowId, ContainerType containerType) {
        return new LecternContainer(session, name, windowId, this.size, containerType);
    }
}
