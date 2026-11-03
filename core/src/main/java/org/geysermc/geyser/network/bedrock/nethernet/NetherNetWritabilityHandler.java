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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.ScheduledFuture;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannel;

import java.util.concurrent.TimeUnit;

/**
 * Turns a NetherNet channel unwritable well before the transport does. Cloudburst only does so past 512 KiB of
 * {@code bufferedAmount}, which at the rates seen in #6854 is tens of seconds of backlog. This sets a second
 * user-defined writability bit from the same queue, with lower watermarks, so {@link io.netty.channel.Channel#isWritable()}
 * tells senders to hold back while holding back still helps.
 * TODO NetherNet: drop once Cloudburst makes its watermarks configurable
 */
public class NetherNetWritabilityHandler extends ChannelOutboundHandlerAdapter {
    public static final String NAME = "geyser-nethernet-writability";

    /** Cloudburst uses index 1 for its own watermarks */
    private static final int GEYSER_WRITABILITY = 2;
    static final long HIGH_WATERMARK = Long.getLong("Geyser.NetherNetWritabilityHigh", 64 << 10);
    static final long LOW_WATERMARK = Math.min(HIGH_WATERMARK, Long.getLong("Geyser.NetherNetWritabilityLow", 16 << 10));
    /** No native callback fires at our low watermark, so the queue is polled while it is over the high one */
    private static final long POLL_MILLIS = 10;

    private boolean writable = true;
    private @Nullable ScheduledFuture<?> poll;

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        // The channel hands everything to native during the flush, so the queue is up to date once it returns
        super.flush(ctx);
        update(ctx);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        cancelPoll();
        super.close(ctx, promise);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        cancelPoll();
    }

    private void update(ChannelHandlerContext ctx) {
        if (!(ctx.channel() instanceof NetherNetChannel channel) || !channel.isActive()) {
            cancelPoll();
            return;
        }
        long queued = NetherNetQueue.queuedBytes(channel);
        ChannelOutboundBuffer out = channel.unsafe().outboundBuffer();
        if (queued < 0 || out == null) {
            return;
        }

        if (writable && queued > HIGH_WATERMARK) {
            writable = false;
            out.setUserDefinedWritability(GEYSER_WRITABILITY, false);
            if (poll == null) {
                poll = ctx.executor().scheduleAtFixedRate(() -> update(ctx), POLL_MILLIS, POLL_MILLIS, TimeUnit.MILLISECONDS);
            }
        } else if (!writable && queued <= LOW_WATERMARK) {
            writable = true;
            out.setUserDefinedWritability(GEYSER_WRITABILITY, true);
            cancelPoll();
        }
    }

    private void cancelPoll() {
        if (poll != null) {
            poll.cancel(false);
            poll = null;
        }
    }
}
