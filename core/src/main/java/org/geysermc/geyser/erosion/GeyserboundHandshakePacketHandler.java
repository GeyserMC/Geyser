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

package org.geysermc.geyser.erosion;

import io.netty.channel.Channel;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.erosion.netty.NettyPacketSender;
import org.geysermc.erosion.packet.ErosionPacketHandler;
import org.geysermc.erosion.packet.geyserbound.GeyserboundHandshakePacket;
import org.geysermc.geyser.session.GeyserSession;

public final class GeyserboundHandshakePacketHandler extends AbstractGeyserboundPacketHandler {

    public GeyserboundHandshakePacketHandler(GeyserSession session) {
        super(session);
    }

    @Override
    public void handleHandshake(GeyserboundHandshakePacket packet) {
        boolean useTcp = packet.getTransportType().getSocketAddress() == null;
        GeyserboundPacketHandlerImpl handler = new GeyserboundPacketHandlerImpl(session, useTcp ? new GeyserErosionPacketSender(session) : new NettyPacketSender<>());
        session.setErosionHandler(handler);
        if (!useTcp) {
            if (session.getGeyser().getErosionUnixListener() == null) {
                session.disconnect("Erosion configurations using Unix socket handling are not supported on this hardware!");
                return;
            }
            session.getGeyser().getErosionUnixListener().createClient(handler, packet.getTransportType().getSocketAddress());
        } else {
            handler.onConnect();
        }
        session.ensureInEventLoop(() -> session.getChunkCache().clear());
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public @Nullable GeyserboundPacketHandlerImpl getAsActive() {
        return null;
    }

    @Override
    public @Nullable ErosionPacketHandler setChannel(Channel channel) {
        return null;
    }
}
