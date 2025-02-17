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

package org.geysermc.geyser.network.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.ScheduledFuture;
import org.cloudburstmc.netty.channel.raknet.RakServerChannel;
import org.cloudburstmc.netty.channel.raknet.config.RakServerMetrics;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.session.SessionManager;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RakGeyserRateLimiter extends SimpleChannelInboundHandler<DatagramPacket> {
    public static final String NAME = "rak-geyser-rate-limiter";
    private final RakServerChannel channel;
    private final GeyserLogger logger;
    private final SessionManager sessionManager;

    private final ConcurrentHashMap<InetAddress, AtomicInteger> rateLimitMap = new ConcurrentHashMap<>();
    private final Map<InetAddress, Long> blockedConnections = new ConcurrentHashMap<>();

    private final AtomicLong globalCounter = new AtomicLong(0);

    private ScheduledFuture<?> tickFuture;
    private ScheduledFuture<?> blockedTickFuture;

    public RakGeyserRateLimiter(Channel channel) {
        this.channel = (RakServerChannel) channel;
        this.logger = GeyserImpl.getInstance().getLogger();
        this.sessionManager = GeyserImpl.getInstance().getSessionManager();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.tickFuture = ctx.channel().eventLoop().scheduleAtFixedRate(this::onRakTick, 10, 10, TimeUnit.MILLISECONDS);
        this.blockedTickFuture = ctx.channel().eventLoop().scheduleAtFixedRate(this::onBlockedTick, 100, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        this.tickFuture.cancel(false);
        this.blockedTickFuture.cancel(true);
        this.rateLimitMap.clear();
    }

    private void onRakTick() {
        this.rateLimitMap.clear();
        this.globalCounter.set(0);
    }

    private void onBlockedTick() {
        long currTime = System.currentTimeMillis();

        RakServerMetrics metrics = this.channel.config().getMetrics();

        Iterator<Map.Entry<InetAddress, Long>> iterator = this.blockedConnections.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<InetAddress, Long> entry = iterator.next();
            if (entry.getValue() != 0 && currTime > entry.getValue()) {
                iterator.remove();
                logger.info("Unblocked address %s".formatted(entry.getKey()));
                if (metrics != null) {
                    metrics.addressUnblocked(entry.getKey());
                }
            }
        }
    }

    public boolean blockAddress(InetAddress address, long time, TimeUnit unit) {
        long millis = unit.toMillis(time);
        this.blockedConnections.put(address, System.currentTimeMillis() + millis);

        if (this.channel.config().getMetrics() != null) {
            this.channel.config().getMetrics().addressBlocked(address);
        }
        return true;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket datagram) throws Exception {
        if (this.globalCounter.incrementAndGet() > this.channel.config().getGlobalPacketLimit()) {
            if (logger.isDebug()) {
                logger.debug("[%s] Dropped incoming packet because global packet limit was reached: %s".formatted(datagram.sender(), this.globalCounter.get()));
            }
            return;
        }

        InetAddress address = datagram.sender().getAddress();
        if (this.blockedConnections.containsKey(address)) {
            return;
        }

        AtomicInteger counter = this.rateLimitMap.computeIfAbsent(address, a -> new AtomicInteger());
        int packetLimit = this.channel.config().getPacketLimit() * sessionManager.getAddressMultiplier(address);
        if (counter.incrementAndGet() > packetLimit && this.blockAddress(address, 10, TimeUnit.SECONDS)) {
            logger.warning("[%s] Blocked because packet limit was reached".formatted(address));
        } else {
            ctx.fireChannelRead(datagram.retain());
        }
    }
}
