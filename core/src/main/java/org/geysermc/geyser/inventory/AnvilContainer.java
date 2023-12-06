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

package org.geysermc.geyser.inventory;

import com.github.steveice10.mc.protocol.data.game.inventory.ContainerType;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundRenameItemPacket;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.ItemUtils;

/**
 * Used to determine if rename packets should be sent and stores
 * the expected level cost for AnvilInventoryUpdater
 */
@Getter @Setter
public class AnvilContainer extends Container {
    /**
     * Stores the level cost received as a window property from Java
     */
    private int javaLevelCost = 0;
    /**
     * A flag to specify whether javaLevelCost can be used as it can
     * be outdated or not sent at all.
     */
    private boolean useJavaLevelCost = false;

    /**
     * The new name of the item as received from Bedrock
     */
    @Nullable
    private String newName = null;

    private GeyserItemStack lastInput = GeyserItemStack.EMPTY;
    private GeyserItemStack lastMaterial = GeyserItemStack.EMPTY;

    private int lastTargetSlot = -1;

    public AnvilContainer(String title, int id, int size, ContainerType containerType, PlayerInventory playerInventory) {
        super(title, id, size, containerType, playerInventory);
    }

    /**
     * @return the name to use instead for renaming.
     */
    public String checkForRename(GeyserSession session, String rename) {
        String correctRename;
        newName = rename;

        String originalName = ItemUtils.getCustomName(getInput().getNbt());

        String plainOriginalName = MessageTranslator.convertToPlainTextLenient(originalName, session.locale());
        String plainNewName = MessageTranslator.convertToPlainText(rename);
        if (!plainOriginalName.equals(plainNewName)) {
            // Strip out formatting since Java Edition does not allow it
            correctRename = plainNewName;
            // Java Edition sends a packet every time an item is renamed even slightly in GUI. Fortunately, this works out for us now
            ServerboundRenameItemPacket renameItemPacket = new ServerboundRenameItemPacket(plainNewName);
            session.sendDownstreamGamePacket(renameItemPacket);
        } else {
            // Restore formatting for item since we're not renaming
            correctRename = MessageTranslator.convertMessageLenient(originalName);
            // Java Edition sends the original custom name when not renaming,
            // if there isn't a custom name an empty string is sent
            ServerboundRenameItemPacket renameItemPacket = new ServerboundRenameItemPacket(plainOriginalName);
            session.sendDownstreamGamePacket(renameItemPacket);
        }

        useJavaLevelCost = false;
        return correctRename;
    }

    public GeyserItemStack getInput() {
        return getItem(0);
    }

    public GeyserItemStack getMaterial() {
        return getItem(1);
    }

    public GeyserItemStack getResult() {
        return getItem(2);
    }
}
