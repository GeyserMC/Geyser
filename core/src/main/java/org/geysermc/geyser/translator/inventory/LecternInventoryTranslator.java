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

package org.geysermc.geyser.translator.inventory;

import com.github.steveice10.mc.protocol.data.game.inventory.ContainerType;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerButtonClickPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.LecternContainer;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.inventory.updater.InventoryUpdater;
import org.geysermc.geyser.util.BlockEntityUtils;
import org.geysermc.geyser.util.InventoryUtils;

import java.util.Collections;

public class LecternInventoryTranslator extends BaseInventoryTranslator {
    private final InventoryUpdater updater;

    public LecternInventoryTranslator() {
        super(1);
        this.updater = new InventoryUpdater();
    }

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {
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
            updateBook(session, inventory, itemStack);
        }
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        this.updater.updateSlot(this, session, inventory, slot);
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
            ServerboundContainerButtonClickPacket packet = new ServerboundContainerButtonClickPacket(inventory.getId(), 3);
            session.sendDownstreamPacket(packet);
            session.setDroppingLecternBook(false);
            InventoryUtils.closeInventory(session, inventory.getId(), false);
        } else if (lecternContainer.getBlockEntityTag() == null) {
            CompoundTag tag = book.getNbt();
            // Position has to be the last interacted position... right?
            Vector3i position = session.getLastInteractionBlockPosition();
            // If shouldExpectLecternHandled returns true, this is already handled for us
            // shouldRefresh means that we should boot out the client on our side because their lectern GUI isn't updated yet
            boolean shouldRefresh = !session.getGeyser().getWorldManager().shouldExpectLecternHandled() && !session.getLecternCache().contains(position);

            NbtMap blockEntityTag;
            if (tag != null) {
                int pagesSize = ((ListTag) tag.get("pages")).size();
                ItemData itemData = book.getItemData(session);
                NbtMapBuilder lecternTag = getBaseLecternTag(position.getX(), position.getY(), position.getZ(), pagesSize);
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
                NbtMapBuilder lecternTag = getBaseLecternTag(position.getX(), position.getY(), position.getZ(), 1);
                NbtMapBuilder bookTag = NbtMap.builder()
                        .putByte("Count", (byte) 1)
                        .putShort("Damage", (short) 0)
                        .putString("Name", "minecraft:writable_book")
                        .putCompound("tag", NbtMap.builder().putList("pages", NbtType.COMPOUND, Collections.singletonList(
                                NbtMap.builder()
                                        .putString("photoname", "")
                                        .putString("text", "")
                                        .build()
                        )).build());

                blockEntityTag = lecternTag.putCompound("book", bookTag.build()).build();
            }

            // Even with serverside access to lecterns, we don't easily know which lectern this is, so we need to rebuild
            // the block entity tag
            lecternContainer.setBlockEntityTag(blockEntityTag);
            lecternContainer.setPosition(position);
            if (shouldRefresh) {
                // Update the lectern because it's not updated client-side
                BlockEntityUtils.updateBlockEntity(session, blockEntityTag, position);
                session.getLecternCache().add(position);
                // Close the window - we will reopen it once the client has this data synced
                ServerboundContainerClosePacket closeWindowPacket = new ServerboundContainerClosePacket(lecternContainer.getId());
                session.sendDownstreamPacket(closeWindowPacket);
                InventoryUtils.closeInventory(session, inventory.getId(), false);
            }
        }
    }

    @Override
    public Inventory createInventory(String name, int windowId, ContainerType containerType, PlayerInventory playerInventory) {
        return new LecternContainer(name, windowId, this.size, containerType, playerInventory);
    }

    public static NbtMapBuilder getBaseLecternTag(int x, int y, int z, int totalPages) {
        NbtMapBuilder builder = NbtMap.builder()
                .putInt("x", x)
                .putInt("y", y)
                .putInt("z", z)
                .putString("id", "Lectern");
        if (totalPages != 0) {
            builder.putByte("hasBook", (byte) 1);
            builder.putInt("totalPages", totalPages);
        } else {
            // Not usually needed, but helps with kicking out Bedrock players from reading the UI
            builder.putByte("hasBook", (byte) 0);
        }
        return builder;
    }
}
