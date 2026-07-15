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
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;

import java.util.concurrent.TimeUnit;

/**
 * Vanilla ships two latency primitives with deliberately different semantics,
 * enforced by which client thread answers them.
 * <p>
 * Keep alive is answered by Java clients on the NETWORK thread, before the
 * packet ever touches the main thread queue ({@code handleKeepAlive} has no
 * {@code ensureRunningOnSameThread}). Its round trip feeds the vanilla
 * latency field, the tab list, and {@code Player#getPing}, and by that design
 * it means pure network round trip, excluding all client processing.
 * <p>
 * Bedrock has no such fast path: bouncing the keep alive off the client via
 * NetworkStackLatency includes its processing loop, a number no Java client
 * ever reports, and historically also risked keep alive timeout kicks when
 * clients stalled. So with forward-player-ping enabled, Geyser answers on the
 * client's behalf after the measured transport round trip time: the server
 * measures the same thing it measures for a Java player, and the client can
 * never be kicked over it. See {@link JavaPingTranslator} for the deliberate
 * counterpart where the client IS involved.
 */
@Translator(packet = ClientboundKeepAlivePacket.class)
public class JavaKeepAliveTranslator extends PacketTranslator<ClientboundKeepAlivePacket> {

    @Override
    public void translate(GeyserSession session, ClientboundKeepAlivePacket packet) {
        if (!session.getGeyser().config().gameplay().forwardPlayerPing()) {
            // MCProtocolLib answers the keep alive instantly at the network
            // layer; the server reads a near zero ping. Never fails.
            return;
        }

        // Echo after the transport RTT (see class javadoc). A remote Java
        // backend adds its own hop to Geyser on top, exactly as it would for
        // a Java client sitting where the Bedrock client sits.
        final long javaId = packet.getPingId();
        long delay = Math.max(0, session.ping());
        session.scheduleInEventLoop(() ->
                session.sendDownstreamPacket(new ServerboundKeepAlivePacket(javaId)),
                delay, TimeUnit.MILLISECONDS);
    }
}
