/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.bedrock.nethernet;

import io.netty.channel.Channel;
import org.cloudburstmc.protocol.bedrock.BedrockSessionFactory;
import org.geysermc.geyser.network.bedrock.GeyserBedrockPeer;

import javax.crypto.SecretKey;

public class NetherNetPeer extends GeyserBedrockPeer {
    /**
     * NetherNet compression behaves like RakNet protocol 11: none until network settings, prefixed afterwards.
     */
    private static final int EQUIVALENT_RAK_VERSION = 11;

    public NetherNetPeer(Channel channel, BedrockSessionFactory sessionFactory) {
        super(channel, sessionFactory);
    }

    @Override
    public void enableEncryption(SecretKey secretKey) {
        // No-op: DTLS already encrypts the data channel
    }

    /**
     * A NetherNet channel has no RakNet protocol version; {@link #setCompression(org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm)}
     * uses this to pick the compression strategy.
     */
    @Override
    public int getRakVersion() {
        return EQUIVALENT_RAK_VERSION;
    }
}
