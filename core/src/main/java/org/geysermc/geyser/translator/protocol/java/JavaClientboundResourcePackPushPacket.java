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

package org.geysermc.geyser.translator.protocol.java;

import org.geysermc.mcprotocollib.protocol.data.game.ResourcePackStatus;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPushPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundResourcePackPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = ClientboundResourcePackPushPacket.class)
public class JavaClientboundResourcePackPushPacket extends PacketTranslator<ClientboundResourcePackPushPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundResourcePackPushPacket packet) {
        // We need to "answer" this to avoid timeout issues related to resource packs
        // If packs are required, we need to lie to the server that we accepted them, as we get kicked otherwise.
        if (packet.isRequired()) {
            session.sendDownstreamPacket(new ServerboundResourcePackPacket(packet.getId(), ResourcePackStatus.ACCEPTED));
            session.sendDownstreamPacket(new ServerboundResourcePackPacket(packet.getId(), ResourcePackStatus.DOWNLOADED));
            session.sendDownstreamPacket(new ServerboundResourcePackPacket(packet.getId(), ResourcePackStatus.SUCCESSFULLY_LOADED));
        } else {
            session.sendDownstreamPacket(new ServerboundResourcePackPacket(packet.getId(), ResourcePackStatus.DECLINED));
        }
    }
}
