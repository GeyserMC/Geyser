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

package org.geysermc.geyser.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.geysermc.geyser.network.GeyserBedrockPeer;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;

@RequiredArgsConstructor
public class UpstreamSession {
    @Getter private final BedrockServerSession session;
    @Getter @Setter
    private boolean initialized = false;
    private Queue<BedrockPacket> postStartGamePackets = new ArrayDeque<>();

    public void sendPacket(@NonNull BedrockPacket packet) {
        if (!isClosed()) {
            session.sendPacket(packet);
        }
    }

    public void sendPacketImmediately(@NonNull BedrockPacket packet) {
        if (!isClosed()) {
            session.sendPacketImmediately(packet);
        }
    }

    public void disconnect(String reason) {
        this.session.disconnect(reason);
    }

    /**
     * Queue a packet that must be delayed until after login.
     */
    public void queuePostStartGamePacket(BedrockPacket packet) {
        postStartGamePackets.add(packet);
    }

    public void sendPostStartGamePackets() {
        if (isClosed()) {
            return;
        }

        BedrockPacket packet;
        while ((packet = postStartGamePackets.poll()) != null) {
            session.sendPacket(packet);
        }
        postStartGamePackets = null;
    }

    public boolean isClosed() {
        return !session.getPeer().isConnected() && !session.getPeer().isConnecting();
    }

    public InetSocketAddress getAddress() {
        // Will always be an InetSocketAddress. See ProxyChannel#remoteAddress
        return (InetSocketAddress) ((GeyserBedrockPeer) session.getPeer()).getRealAddress();
    }

    /**
     * Gets the session's protocol version.
     *
     * @return the session's protocol version.
     */
    public int getProtocolVersion() {
        return this.session.getCodec().getProtocolVersion();
    }

    /**
     * Gets the codec helper for this session.
     *
     * @return the codec helper for this session
     */
    public BedrockCodecHelper getCodecHelper() {
        return this.session.getPeer().getCodecHelper();
    }
}
