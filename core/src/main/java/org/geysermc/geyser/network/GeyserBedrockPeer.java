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
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.BedrockSessionFactory;

import java.net.SocketAddress;

public class GeyserBedrockPeer extends BedrockPeer {
    private SocketAddress proxiedAddress;

    public GeyserBedrockPeer(Channel channel, BedrockSessionFactory sessionFactory) {
        super(channel, sessionFactory);
    }

    /**
     * Drops a split-screen sub-client's session from this peer's sub-client map.
     *
     * <p>{@link BedrockSession#close(CharSequence)} deliberately does nothing for a sub-client,
     * since only the primary session owns the connection - but that also means a sub-client that
     * has left is never removed here, and {@link #onBedrockPacket} would keep routing that slot's
     * packets to the dead session rather than building a fresh one for the player rejoining.
     *
     * <p>Exists because {@link #removeSession} is protected: a subclass may only reach it through
     * its own type, so the call has to live inside this class.
     */
    /**
     * Returns the sub-client id this peer has filed {@code session} under - 0 for the player who
     * signed in first, 1-3 for split-screen guests - or -1 if the peer does not hold it.
     *
     * <p>{@link BedrockSession#subClientId} is protected and has no getter, but the id is worth
     * recovering: it identifies the controller slot, so it is stable when a guest leaves and
     * rejoins, which makes it the only durable thing to hang a synthesized identity off for a
     * console that sends no XUID.
     */
    public int subClientIdOf(BedrockSession session) {
        // Bedrock allows at most four local players per connection.
        for (int id = 0; id < 4; id++) {
            if (this.sessions.get(id) == session) {
                return id;
            }
        }
        return -1;
    }

    public void removeSubClientSession(BedrockSession session) {
        if (!session.isSubClient()) {
            throw new IllegalArgumentException("Refusing to evict the primary session from its peer");
        }
        this.removeSession(session);
    }

    public SocketAddress getRealAddress() {
        SocketAddress proxied = this.proxiedAddress;
        return proxied == null ? this.getSocketAddress() : proxied;
    }

    public void setProxiedAddress(SocketAddress proxiedAddress) {
        this.proxiedAddress = proxiedAddress;
    }
}
