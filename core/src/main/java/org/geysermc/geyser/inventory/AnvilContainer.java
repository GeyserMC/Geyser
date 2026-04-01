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

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundRenameItemPacket;


@Getter @Setter
public class AnvilContainer extends Container {
    
    private int javaLevelCost = 0;
    
    private boolean useJavaLevelCost = false;

    
    @Nullable
    private String newName = null;

    private GeyserItemStack lastInput = GeyserItemStack.EMPTY;
    private GeyserItemStack lastMaterial = GeyserItemStack.EMPTY;

    private int lastTargetSlot = -1;

    public AnvilContainer(GeyserSession session, String title, int id, int size, ContainerType containerType) {
        super(session, title, id, size, containerType);
    }

    
    public String checkForRename(GeyserSession session, String rename) {
        String correctRename;
        newName = rename;

        Component originalName = getInput().getComponent(DataComponentTypes.CUSTOM_NAME);

        String plainOriginalName = MessageTranslator.convertToPlainText(originalName, session.locale());
        String plainNewName = MessageTranslator.convertIncomingToPlainText(rename);
        if (!plainOriginalName.equals(plainNewName)) {
            
            correctRename = plainNewName;
            
            ServerboundRenameItemPacket renameItemPacket = new ServerboundRenameItemPacket(plainNewName);
            session.sendDownstreamGamePacket(renameItemPacket);
        } else {
            
            correctRename = originalName != null ? MessageTranslator.convertMessage(originalName, session.locale()) : "";
            
            
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
