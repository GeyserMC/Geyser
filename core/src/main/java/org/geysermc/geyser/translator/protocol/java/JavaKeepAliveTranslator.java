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

package org.geysermc.geyser.translator.protocol.java;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundKeepAlivePacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

/**
 * Used to forward the keep alive packet to the client in order to get back a reliable ping.
 */
@Translator(packet = ClientboundKeepAlivePacket.class)
public class JavaKeepAliveTranslator extends PacketTranslator<ClientboundKeepAlivePacket> {

    @Override
    public void translate(GeyserSession session, ClientboundKeepAlivePacket packet) {
        if (!session.getGeyser().getConfig().isForwardPlayerPing()) {
            return;
        }
        // We use this once the client replies (see BedrockNetworkStackLatencyTranslator)
        session.getKeepAliveCache().add(packet.getPingId());

        NetworkStackLatencyPacket latencyPacket = new NetworkStackLatencyPacket();
        latencyPacket.setFromServer(true);
        // We take the abs because we rely on the client responding with a negative value ONLY when we send
        // a negative timestamp in the form-image-hack performed in FormCache.
        // Apart from that case, we don't actually use the value the client responds with, instead using our keep alive cache.
        latencyPacket.setTimestamp(Math.abs(packet.getPingId()));
        session.sendUpstreamPacketImmediately(latencyPacket);
    }

    @Override
    public boolean shouldExecuteInEventLoop() {
        return false;
    }
}
