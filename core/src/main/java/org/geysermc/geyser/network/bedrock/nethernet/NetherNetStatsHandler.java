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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.ScheduledFuture;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannel;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;

import java.util.concurrent.TimeUnit;

/**
 * Sits between compression and the NetherNet frame codec, where outbound batches carry both their compressed and
 * uncompressed forms. In debug mode, also logs when a connection is falling behind.
 */
public class NetherNetStatsHandler extends ChannelDuplexHandler {
    public static final String NAME = "geyser-nethernet-stats";

    private static final long CHECK_INTERVAL_SECONDS = 5;

    private final GeyserImpl geyser;
    private final NetherNetStats stats;
    private @Nullable ScheduledFuture<?> check;
    private long lastDataTimeouts;

    public NetherNetStatsHandler(GeyserImpl geyser, NetherNetStats stats) {
        this.geyser = geyser;
        this.stats = stats;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof BedrockBatchWrapper batch) {
            ByteBuf uncompressed = batch.getUncompressed();
            stats.batchOut(batch.getPackets().size(), uncompressed == null ? 0 : uncompressed.readableBytes());
            ByteBuf compressed = batch.getCompressed();
            if (compressed != null) {
                stats.compressedOut(compressed.readableBytes());
            }
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        stats.writabilityChanged(ctx.channel().isWritable());
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Scheduled regardless of debug mode, which can be turned on while players are connected
        if (ctx.channel() instanceof NetherNetChannel channel) {
            check = ctx.executor().scheduleAtFixedRate(() -> checkBacklog(channel),
                CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cancelCheck();
        super.channelInactive(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        cancelCheck();
    }

    private void cancelCheck() {
        if (check != null) {
            check.cancel(false);
            check = null;
        }
    }

    /**
     * Logs only connections that are behind or losing data, so healthy players stay out of the log.
     */
    private void checkBacklog(NetherNetChannel channel) {
        if (!geyser.getLogger().isDebug() || !channel.isActive()) {
            return;
        }
        NetherNetStats.Snapshot snapshot = stats.snapshot(channel, false);
        long newTimeouts = snapshot.dataTimeouts() - lastDataTimeouts;
        lastDataTimeouts = Math.max(lastDataTimeouts, snapshot.dataTimeouts());
        // Queued bytes only appear once SCTP's own send buffer is full, so any at all means a real backlog
        if (snapshot.writable() && snapshot.queuedBytes() <= 0 && newTimeouts <= 0) {
            return;
        }

        geyser.getLogger().debug("[NetherNet] " + name(channel) + " is falling behind: "
            + (snapshot.writable() ? "" : "unwritable for " + snapshot.unwritableMillis() + " ms, ")
            + "queued " + snapshot.queuedBytes() + " B, unacked " + snapshot.unackedChunks() + " chunks"
            + ", receive queue " + snapshot.receiveQueueChunks() + " chunks"
            + ", cwnd " + snapshot.congestionWindow() + " B, peer rwnd " + snapshot.peerReceiveWindow() + " B"
            + ", rtt " + snapshot.sctpRtt() + " ms, rto " + snapshot.sctpRto() + " ms"
            + ", out " + snapshot.bytesOutPerSecond() + " B/s in " + snapshot.messagesOutPerSecond() + " msg/s"
            + ", timeouts +" + Math.max(newTimeouts, 0));
    }

    private String name(NetherNetChannel channel) {
        for (GeyserSession session : geyser.getSessionManager().getAllSessions()) {
            if (session.getUpstream().getSession().getPeer().getChannel() == channel) {
                return session.bedrockUsername();
            }
        }
        return "A connection that has not logged in";
    }
}
