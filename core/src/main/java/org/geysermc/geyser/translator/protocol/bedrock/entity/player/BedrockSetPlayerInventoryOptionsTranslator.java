/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player;

import com.github.steveice10.mc.protocol.data.game.inventory.CraftingBookStateType;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundRecipeBookChangeSettingsPacket;
import org.cloudburstmc.protocol.bedrock.data.inventory.InventoryTabLeft;
import org.cloudburstmc.protocol.bedrock.packet.SetPlayerInventoryOptionsPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = SetPlayerInventoryOptionsPacket.class)
public class BedrockSetPlayerInventoryOptionsTranslator extends PacketTranslator<SetPlayerInventoryOptionsPacket> {

    @Override
    public void translate(GeyserSession session, SetPlayerInventoryOptionsPacket packet) {
        // Sent by 1.20.50+ - we can pass it through to the java server
        CraftingBookStateType type = CraftingBookStateType.CRAFTING; // There is no furnace recipe book in bedrock
        boolean bookOpen = packet.getLeftTab() != InventoryTabLeft.NONE && packet.getLeftTab() != InventoryTabLeft.SURVIVAL;
        boolean filtered = packet.isFiltering();

        session.sendDownstreamPacket(new ServerboundRecipeBookChangeSettingsPacket(type, bookOpen, filtered));
    }
}
