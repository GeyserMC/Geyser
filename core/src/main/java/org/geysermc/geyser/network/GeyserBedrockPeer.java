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

package org.geysermc.geyser.network;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.netty.channel.raknet.RakChildChannel;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockSessionFactory;

import javax.crypto.SecretKey;
import java.net.SocketAddress;

public class GeyserBedrockPeer extends BedrockPeer {
    @Getter
    @Setter
    private SocketAddress proxiedAddress;

    /**
     * Whether Bedrock encryption can be negotiated. RakNet clients can; transports behind a trusted proxy that
     * already authenticated the player set this {@code false} to skip the handshake.
     */
    @Getter
    @Setter
    private boolean encryptionSupported = true;

    public GeyserBedrockPeer(Channel channel, BedrockSessionFactory sessionFactory) {
        super(channel, sessionFactory);
    }

    public SocketAddress getRealAddress() {
        SocketAddress proxied = this.proxiedAddress;
        return proxied == null ? this.getSocketAddress() : proxied;
    }

    @Override
    public void enableEncryption(SecretKey secretKey) {
        if (!this.encryptionSupported) {
            // Security is the transport's responsibility on this connection; do not install Bedrock ciphers.
            return;
        }
        super.enableEncryption(secretKey);
    }

    /**
     * The round-trip latency to the peer in milliseconds, used for {@code /list}-style ping displays and dumps.
     * <p>
     * The default implementation reads RakNet's estimate. Custom transports should override this function
     * and supply their own measurement.
     *
     * @return the latency in milliseconds, or {@code 0} if unavailable
     */
    public int getPing() {
        if (this.getChannel() instanceof RakChildChannel rakChildChannel) {
            RakSessionCodec rakSessionCodec = rakChildChannel.rakPipeline().get(RakSessionCodec.class);
            if (rakSessionCodec != null) {
                return (int) Math.floor(rakSessionCodec.getPing());
            }
        }
        return 0;
    }
}
