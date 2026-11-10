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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.netty.channel.nethernet.AddressSource;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannel;
import org.cloudburstmc.netty.channel.nethernet.SourcedAddress;
import org.cloudburstmc.netty.channel.nethernet.config.DefaultNetherChannelConfig;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.netty.codec.batch.BedrockBatchEncoder;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.NoopCompression;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.SimpleCompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec_v3;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.ConnectionRequests;
import org.geysermc.geyser.network.GeyserServerInitializer;
import org.geysermc.geyser.network.bedrock.GameProtocol;
import org.geysermc.geyser.network.bedrock.nethernet.codec.NetherNetFrameCodec;
import org.geysermc.geyser.network.bedrock.nethernet.codec.NetherNetPacketEncoder;
import org.geysermc.geyser.network.bedrock.nethernet.signaling.provider.GameOutcomeReporter;

/**
 * The RakNet pipeline minus RakNet: no 0xFE frame ID, and no Bedrock encryption as DTLS already covers the data channel.
 */
public class NetherNetChannelInitialiser extends GeyserServerInitializer {
    private static final CompressionStrategy NOOP_STRATEGY = new SimpleCompressionStrategy(new NoopCompression());
    private static final String CONNECTION_GATE = "geyser-connection-gate";
    /** Fallback to one packet per NetherNet message, in case batching causes trouble with some clients */
    private static final boolean BATCHING = Boolean.parseBoolean(System.getProperty("Geyser.NetherNetBatching", "true"));

    private final @Nullable GameOutcomeReporter outcomes;

    public NetherNetChannelInitialiser(GeyserImpl geyser) {
        this(geyser, null);
    }

    /**
     * @param outcomes Reports game outcomes to the provider, or null when no provider admits players here
     */
    public NetherNetChannelInitialiser(GeyserImpl geyser, @Nullable GameOutcomeReporter outcomes) {
        super(geyser, "Geyser NetherNet player thread");
        this.outcomes = outcomes;
    }

    @Override
    protected void preInitChannel(Channel channel) {
        // Extensions refuse peers by address before a session exists, as on RakNet. A NetherNet channel only knows
        // where its peer is once active, so the gate goes first in the pipeline and waits for that
        channel.pipeline().addLast(CONNECTION_GATE, new ConnectionGate());

        // Set by the overridden BedrockServerInitializer method; without it clientbound packets would be decoded too
        channel.attr(PacketDirection.ATTRIBUTE).set(PacketDirection.CLIENT_BOUND);

        // Nothing is compressed until the peer sets compression after RequestNetworkSettings
        channel.pipeline()
                .addLast(NetherNetFrameCodec.NAME, NetherNetFrameCodec.INSTANCE)
                .addLast(CompressionCodec.NAME, new CompressionCodec(NOOP_STRATEGY, false));

        // Counts what is sent and received, for the nethernet stats command and backlog debug logging
        if (channel.config() instanceof DefaultNetherChannelConfig config) {
            NetherNetStats stats = new NetherNetStats();
            config.setMetrics(stats);
            channel.attr(NetherNetStats.ATTRIBUTE).set(stats);
            channel.pipeline()
                    .addAfter(NetherNetFrameCodec.NAME, NetherNetStatsHandler.NAME, new NetherNetStatsHandler(geyser, stats))
                    // Compression is swapped in place later, so this stays ahead of it for outbound batches
                    .addAfter(CompressionCodec.NAME, NetherNetStatsHandler.BatchCounter.NAME, new NetherNetStatsHandler.BatchCounter(stats));
        }
    }

    @Override
    protected void initPacketCodec(Channel channel) {
        // The parent picks this by RakNet protocol version, which a NetherNet channel does not have
        channel.pipeline().addLast(BedrockPacketCodec.NAME, new BedrockPacketCodec_v3());
    }

    @Override
    protected void postInitChannel(Channel channel) throws Exception {
        super.postInitChannel(channel);

        // The BedrockBatchEncoder added by BedrockChannelInitializer batches every packet written between flushes into one
        // message, like RakNet, and splits batches at the channel's max message size. Sending one packet per message instead
        // multiplies the message rate and compresses every packet on its own, which backs up the reliable channel (#6854).
        if (!BATCHING) {
            channel.pipeline().replace(BedrockBatchEncoder.NAME, NetherNetPacketEncoder.NAME, NetherNetPacketEncoder.INSTANCE);
        }

        if (outcomes != null) {
            channel.pipeline().addBefore(BedrockPeer.NAME, GameOutcomeReporter.HANDLER_NAME,
                    outcomes.observer(protocol -> GameProtocol.getBedrockCodec(protocol) != null));
        }
    }

    /** Asks extensions about the peer once the channel knows where it is, before anything is read. */
    private final class ConnectionGate extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ctx.pipeline().remove(this);
            if (ctx.channel() instanceof NetherNetChannel netherNet) {
                SourcedAddress peer = netherNet.reportedAddress();
                logSignaled(netherNet.signaledAddress(), peer);
                if (!ConnectionRequests.accept(geyser, peer.address(), peer.via(), peer.source().name())) {
                    ctx.close();
                    return;
                }
            }
            ctx.fireChannelActive();
        }

        /** Where signaling met the peer, when that is not the address it is reported with. */
        private void logSignaled(SourcedAddress signaled, SourcedAddress reported) {
            if (signaled.address().equals(reported.address()) || signaled.source() == AddressSource.UNKNOWN
                    || !geyser.getLogger().isDebug()) {
                return;
            }
            boolean logIps = geyser.config().logPlayerIpAddresses();
            String address = logIps ? String.valueOf(signaled.address()) : "<IP address withheld>";
            String via = signaled.via() == null ? "" : " via " + (logIps ? signaled.via() : "<IP address withheld>");
            geyser.getLogger().debug("NetherNet signaling met " + address + " (" + signaled.source() + via + ")");
        }
    }

    @Override
    protected BedrockPeer createPeer(Channel channel) {
        return new NetherNetPeer(channel, this::createSession);
    }
}
