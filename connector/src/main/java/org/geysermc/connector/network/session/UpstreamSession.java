/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.session;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.packet.DisconnectPacket;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.InetSocketAddress;

@RequiredArgsConstructor
public class UpstreamSession {
    @Getter private final BedrockServerSession session;
    private final int clientId;

    @Getter @Setter
    private boolean initialized = false;

    public void sendPacket(@NonNull BedrockPacket packet) {
        if (isClosed())
            return;

        packet.setSenderId(clientId);
        session.sendPacket(packet);
    }

    public void sendPacketImmediately(@NonNull BedrockPacket packet) {
        if (isClosed())
            return;

        packet.setSenderId(clientId);
        session.sendPacketImmediately(packet);
    }

    public void disconnect(String reason) {
        if (isClosed()) {
            throw new IllegalStateException("Connection has been closed");
        }

        DisconnectPacket packet = new DisconnectPacket();
        packet.setSenderId(clientId);

        if (reason == null) {
            packet.setMessageSkipped(true);
            reason = "disconnect.disconnected";
        }
        packet.setKickMessage(reason);
        this.sendPacketImmediately(packet);
    }

    public boolean isClosed() {
        return session.isClosed();
    }

    public InetSocketAddress getAddress() {
        return session.getAddress();
    }
}
