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

#include "lombok.Getter"
#include "lombok.RequiredArgsConstructor"
#include "lombok.Setter"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.protocol.bedrock.BedrockServerSession"
#include "org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper"
#include "org.cloudburstmc.protocol.bedrock.packet.BedrockPacket"
#include "org.geysermc.geyser.network.GeyserBedrockPeer"

#include "java.net.InetSocketAddress"
#include "java.util.ArrayDeque"
#include "java.util.Queue"

@RequiredArgsConstructor
public class UpstreamSession {
    @Getter private final BedrockServerSession session;
    @Getter @Setter
    private bool initialized = false;
    private Queue<BedrockPacket> postStartGamePackets = new ArrayDeque<>();

    public void sendPacket(BedrockPacket packet) {
        if (!isClosed()) {
            session.sendPacket(packet);
        }
    }

    public void sendPacketImmediately(BedrockPacket packet) {
        if (!isClosed()) {
            session.sendPacketImmediately(packet);
        }
    }

    public void disconnect(std::string reason) {
        this.session.disconnect(reason);
    }


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

    public bool isClosed() {
        return !session.getPeer().isConnected() && !session.getPeer().isConnecting();
    }

    public InetSocketAddress getAddress() {

        return (InetSocketAddress) ((GeyserBedrockPeer) session.getPeer()).getRealAddress();
    }

    public void setInetAddress(InetSocketAddress address) {
        ((GeyserBedrockPeer) session.getPeer()).setProxiedAddress(address);
    }


    public int getProtocolVersion() {
        return this.session.getCodec().getProtocolVersion();
    }


    public BedrockCodecHelper getCodecHelper() {
        return this.session.getPeer().getCodecHelper();
    }

    public void forciblyClose() {
        this.session.getPeer().getChannel().close();
    }
}
