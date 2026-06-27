/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;

/**
 * Transport-agnostic logic for turning an established {@link BedrockServerSession} into a fully wired Geyser
 * session. Both the built-in RakNet transport (via {@link GeyserServerInitializer}) and extension-provided
 * transports funnel through here, so session creation stays consistent regardless of
 * how the connection was established.
 */
public class GeyserSessionInitializer {
    private final GeyserImpl geyser;

    /**
     * The event loop group {@link GeyserSession}s run their per-player logic on. Shared across all transports.
     */
    // There is a constructor that doesn't require inputting threads, but older Netty versions don't have it
    @Getter
    private final DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup(0, new DefaultThreadFactory("Geyser player thread"));

    public GeyserSessionInitializer(GeyserImpl geyser) {
        this.geyser = geyser;
    }

    /**
     * Creates a {@link BedrockPeer} that produces Geyser sessions. Useful for transports that build their own
     * Netty pipeline by hand (instead of extending cloudburst's
     * {@link org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer}), which is necessary
     * for non-RakNet transports.
     *
     * @param channel the connection channel
     * @return a peer wired to create Geyser sessions
     */
    public BedrockPeer createPeer(Channel channel) {
        return new GeyserBedrockPeer(channel, this::createSession);
    }

    private BedrockSession createSession(BedrockPeer peer, int subClientId) {
        BedrockServerSession session = new BedrockServerSession(peer, subClientId);
        this.initializeSession(session);
        return session;
    }

    /**
     * Wires a freshly created {@link BedrockServerSession} into Geyser: creates the {@link GeyserSession},
     * installs the invalid-packet guard, and sets the upstream packet handler.
     *
     * @param bedrockServerSession the session to initialize
     */
    public void initializeSession(@NonNull BedrockServerSession bedrockServerSession) {
        try {
            bedrockServerSession.setLogging(this.geyser.config().debugMode());
            GeyserSession session = new GeyserSession(this.geyser, bedrockServerSession, this.eventLoopGroup.next());

            if (!bedrockServerSession.isSubClient()) {
                Channel channel = bedrockServerSession.getPeer().getChannel();
                channel.pipeline().addAfter(BedrockPacketCodec.NAME, InvalidPacketHandler.NAME, new InvalidPacketHandler(session));
            }

            bedrockServerSession.setPacketHandler(new UpstreamPacketHandler(this.geyser, session));
        } catch (Throwable e) {
            // Error must be caught or it will be swallowed
            this.geyser.getLogger().error("Error occurred while initializing player!", e);
            bedrockServerSession.disconnect(e.getMessage());
        }
    }
}
