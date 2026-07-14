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

import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundPingPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket;

/**
 * The deliberate counterpart of {@link JavaKeepAliveTranslator}: vanilla
 * marshals {@code handlePing} onto the client MAIN thread
 * ({@code ensureRunningOnSameThread}), unlike keep alive, which is answered
 * on the network thread. Ping/pong therefore measures network PLUS client
 * processing by design. It descends from the transaction packet pattern, and
 * plugins choose it over keep alive precisely when they need "the client has
 * fully processed everything sent before this point": inventory syncs,
 * transaction style anticheat checks, client lag detection. A frozen client
 * must delay the pong, or those plugins are being lied to.
 * <p>
 * The Java equivalent behavior is therefore a real round trip through the
 * Bedrock client: NetworkStackLatency is handled inside its game loop, the
 * closest analog of the main thread marshal. Unlike keep alive, a late pong
 * carries no timeout kick, so involving the client here is safe.
 */
@Translator(packet = ClientboundPingPacket.class)
public class JavaPingTranslator extends PacketTranslator<ClientboundPingPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundPingPacket packet) {
        // We use this once the client replies
        final int id = packet.getId();

        if (!session.getGeyser().config().gameplay().forwardPlayerPing()) {
            session.sendDownstreamPacket(new ServerboundPongPacket(id));
            return;
        }

        // Real round trip through the client's processing loop; see the
        // class javadoc for why this must involve the client while keep
        // alive must not.
        session.sendNetworkLatencyStackPacket(id, true, () -> session.sendDownstreamPacket(new ServerboundPongPacket(id)));
    }
}
