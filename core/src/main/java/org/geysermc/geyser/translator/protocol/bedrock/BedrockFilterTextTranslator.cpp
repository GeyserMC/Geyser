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

package org.geysermc.geyser.translator.protocol.bedrock;

#include "org.cloudburstmc.protocol.bedrock.packet.FilterTextPacket"
#include "org.geysermc.geyser.inventory.AnvilContainer"
#include "org.geysermc.geyser.inventory.CartographyContainer"
#include "org.geysermc.geyser.inventory.InventoryHolder"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"


@Translator(packet = FilterTextPacket.class)
public class BedrockFilterTextTranslator extends PacketTranslator<FilterTextPacket> {

    override public void translate(GeyserSession session, FilterTextPacket packet) {
        InventoryHolder<?> holder = session.getInventoryHolder();

        if (holder != null) {
            if (holder.inventory() instanceof CartographyContainer) {

                return;
            }
            if (holder.inventory() instanceof AnvilContainer anvilContainer) {
                packet.setText(anvilContainer.checkForRename(session, packet.getText()));
                holder.updateSlot(1);
            }
        }

        packet.setFromServer(true);
        session.sendUpstreamPacket(packet);
    }
}
