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

package org.geysermc.geyser.network.bedrock.nethernet.signaling.provider;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import org.cloudburstmc.netty.signaling.ProviderClient;
import org.cloudburstmc.netty.signaling.admission.AdmissionPrincipal;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntPredicate;

/**
 * Game admission evidence, independent of native transport establishment and teardown.
 */
public final class GameOutcomeReporter {
    public static final String HANDLER_NAME = "nethernet-game-outcome";
    private static final AttributeKey<State> STATE = AttributeKey.valueOf(GameOutcomeReporter.class, "state");
    private final Sink sink;
    private final AtomicLong dropped = new AtomicLong();

    /**
     * Where outcomes go, normally {@link ProviderClient#reportGameOutcome}.
     */
    @FunctionalInterface
    public interface Sink {
        /**
         * @return false when the outcome was dropped
         */
        boolean report(String ticketId, ProviderClient.GameOutcome outcome, String reason);
    }

    public GameOutcomeReporter(Sink sink) {
        this.sink = sink;
    }

    // Owned by the channel: no global session map or references to native resources.
    private static final class State {
        boolean unsupportedVersion;
        boolean reported;
    }

    public ChannelDuplexHandler observer(IntPredicate supportedProtocol) {
        return new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
                if (message instanceof BedrockPacketWrapper wrapper && primary(wrapper)
                        && wrapper.getPacket() instanceof RequestNetworkSettingsPacket settings) {
                    State state = state(ctx.channel());
                    if (state != null) synchronized (state) {
                        state.unsupportedVersion = !supportedProtocol.test(settings.getProtocolVersion());
                    }
                }
                ctx.fireChannelRead(message);
            }

            @Override
            public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) throws Exception {
                if (message instanceof BedrockPacketWrapper wrapper && primary(wrapper)
                        && wrapper.getPacket() instanceof DisconnectPacket) {
                    report(ctx.channel(), false);
                }
                ctx.write(message, promise);
            }
        };
    }

    private static boolean primary(BedrockPacketWrapper wrapper) {
        return wrapper.getSenderSubClientId() == 0 && wrapper.getTargetSubClientId() == 0;
    }

    private static State state(Channel channel) {
        if (channel.attr(AdmissionPrincipal.KEY).get() == null) return null;
        var attribute = channel.attr(STATE);
        State state = attribute.get();
        if (state != null) return state;
        State created = new State();
        State previous = attribute.setIfAbsent(created);
        return previous == null ? created : previous;
    }

    /**
     * Called only by Geyser's play-ready SessionJoinEvent, never by channel-open.
     */
    public void joined(Channel channel) {
        report(channel, true);
    }

    private void report(Channel channel, boolean joined) {
        State state = state(channel);
        if (state == null) return;
        synchronized (state) {
            if (state.reported) return;
            state.reported = true; // A dropped outcome must not turn a later disconnect into rejection.
            // Never publish disconnect text, player identity, or game payloads.
            if (!sink.report(channel.attr(AdmissionPrincipal.KEY).get().ticketId(),
                    joined ? ProviderClient.GameOutcome.JOINED : ProviderClient.GameOutcome.REJECTED,
                    joined ? "play_ready" : state.unsupportedVersion ? "unsupported_version" : "server_rejected")) {
                dropped.incrementAndGet();
            }
        }
    }

    public long droppedEvents() {
        return dropped.get();
    }
}
