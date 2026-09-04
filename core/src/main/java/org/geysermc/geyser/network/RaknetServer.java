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

import org.cloudburstmc.netty.util.nethernet.TrustedProxies;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.uring.IoUring;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import lombok.Getter;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.DefaultRakServerThrottle;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.netty.channel.raknet.config.RakServerCookieMode;
import org.cloudburstmc.netty.handler.codec.raknet.server.RakServerOfflineHandler;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.connection.ConnectionRequestEvent;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.geysermc.geyser.network.bedrock.raknet.Bootstraps;
import org.geysermc.geyser.network.bedrock.raknet.RakConnectionRequestHandler;
import org.geysermc.geyser.network.bedrock.raknet.RakPingHandler;
import org.geysermc.geyser.network.bedrock.raknet.RakServerInitializer;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.mcprotocollib.network.helper.TransportHelper;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.cloudburstmc.netty.channel.raknet.RakConstants.DEFAULT_GLOBAL_PACKET_LIMIT;
import static org.cloudburstmc.netty.channel.raknet.RakConstants.DEFAULT_PACKET_LIMIT;

public final class RaknetServer {
    // Let MCPL determine the transport type -> less code duplication and risk of ending up with 2 different types
    private static final TransportHelper.TransportType TRANSPORT = TransportHelper.TRANSPORT_TYPE;

    /**
     * See {@link EventLoopGroup#shutdownGracefully(long, long, TimeUnit)}
     */
    private static final int SHUTDOWN_QUIET_PERIOD_MS = 100;
    private static final int SHUTDOWN_TIMEOUT_MS = 500;

    private final GeyserImpl geyser;
    private EventLoopGroup group;
    // Split childGroup may improve IO
    private EventLoopGroup childGroup;
    private final ServerBootstrap bootstrap;
    private EventLoopGroup playerGroup;

    private int listenCount;

    private ChannelFuture[] bootstrapFutures;

    // Keep track of connection attempts for dump info
    @Getter
    private int connectionAttempts = 0;

    private final BedrockPingHandler pingResponder;

    public RaknetServer(GeyserImpl geyser, int threadCount) {
        this.geyser = geyser;
        this.listenCount = Bootstraps.isReusePortAvailable() ?  Integer.getInteger("Geyser.ListenCount", 1) : 1;
        GeyserImpl.getInstance().getLogger().debug("Listen thread count: " + listenCount);
        this.group = TRANSPORT.eventLoopGroupFactory().apply(listenCount, new DefaultThreadFactory("GeyserServer", true));
        this.childGroup = TRANSPORT.eventLoopGroupFactory().apply(threadCount, new DefaultThreadFactory("GeyserServerChild", true));

        this.bootstrap = this.createBootstrap();
        // setup SO_REUSEPORT if exists - or, if the option does not actually exist, reset listen count
        // otherwise, we try to bind multiple times which wont work if so_reuseport is not valid
        if (listenCount > 1 && !Bootstraps.setupBootstrap(this.bootstrap, TRANSPORT)) {
            this.listenCount = 1;
        }

        this.pingResponder = new BedrockPingHandler(geyser);
    }

    public CompletableFuture<Void> bind(InetSocketAddress address) {
        bootstrapFutures = new ChannelFuture[listenCount];
        for (int i = 0; i < listenCount; i++) {
            ChannelFuture future = bootstrap.bind(address);
            modifyHandlers(future);
            bootstrapFutures[i] = future;
        }

        return Bootstraps.allOf(bootstrapFutures);
    }

    private void modifyHandlers(ChannelFuture future) {
        future.addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                GeyserImpl.getInstance().getLogger().warning("Not modifying handlers due to exception: " + f.cause());
                return;
            }

            Channel channel = f.channel();
            // Add our handlers
            channel.pipeline()
                .addBefore(RakServerOfflineHandler.NAME, RakConnectionRequestHandler.NAME, new RakConnectionRequestHandler(this))
                .addAfter(RakServerOfflineHandler.NAME, RakPingHandler.NAME, new RakPingHandler(this.pingResponder));
        });
    }

    public void shutdown() {
        try {
            Future<?> futureChildGroup = this.childGroup.shutdownGracefully(SHUTDOWN_QUIET_PERIOD_MS, SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            this.childGroup = null;
            Future<?> futureGroup = this.group.shutdownGracefully(SHUTDOWN_QUIET_PERIOD_MS, SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            this.group = null;
            Future<?> futurePlayerGroup = this.playerGroup.shutdownGracefully(SHUTDOWN_QUIET_PERIOD_MS, SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            this.playerGroup = null;

            futureChildGroup.sync();
            futureGroup.sync();
            futurePlayerGroup.sync();
        } catch (InterruptedException e) {
            GeyserImpl.getInstance().getLogger().severe("Exception in shutdown process", e);
        }
        for (ChannelFuture f : bootstrapFutures) {
            f.channel().closeFuture().syncUninterruptibly();
        }
    }

    @SuppressWarnings("Convert2MethodRef")
    private ServerBootstrap createBootstrap() {
        if (this.geyser.config().debugMode()) {
            this.geyser.getLogger().debug("Transport type: " + TRANSPORT.method().name());
            if (TRANSPORT.datagramChannelClass() == NioDatagramChannel.class) {
                if (System.getProperties().contains("disableNativeEventLoop")) {
                    this.geyser.getLogger().debug("EventLoop type is NIO because native event loops are disabled.");
                } else {
                    // Use lambda here, not method reference, or else NoClassDefFoundError for Epoll/KQueue will not be caught
                    this.geyser.getLogger().debug("Reason for no Epoll: " + throwableOrCaught(() -> Epoll.unavailabilityCause()));
                    this.geyser.getLogger().debug("Reason for no KQueue: " + throwableOrCaught(() -> KQueue.unavailabilityCause()));
                    this.geyser.getLogger().debug("Reason for no IoUring: " + throwableOrCaught(() -> IoUring.unavailabilityCause()));
                }
            }
        }

        this.geyser.getLogger().debug("Setting MTU to " + this.geyser.config().advanced().bedrock().mtu());

        int rakPacketLimit = positivePropOrDefault("Geyser.RakPacketLimit", DEFAULT_PACKET_LIMIT);
        this.geyser.getLogger().debug("Setting RakNet packet limit to " + rakPacketLimit);

        int rakGlobalPacketLimit = positivePropOrDefault("Geyser.RakGlobalPacketLimit", DEFAULT_GLOBAL_PACKET_LIMIT);
        this.geyser.getLogger().debug("Setting RakNet global packet limit to " + rakGlobalPacketLimit);

        boolean rakSendCookie = Boolean.parseBoolean(System.getProperty("Geyser.RakSendCookie", "true"));
        this.geyser.getLogger().debug("Setting RakNet send cookie to " + rakSendCookie);

        int maxConnectionsPerAddress =  positivePropOrDefault("Geyser.MaxConnectionsPerAddress", 10);
        this.geyser.getLogger().debug("Setting max connections per address to " + maxConnectionsPerAddress);

        boolean rakRateLimitingDisabled = Boolean.parseBoolean(System.getProperty(
            "Geyser.RakRateLimitingDisabled",
            Boolean.toString(this.geyser.config().advanced().bedrock().useWaterdogpeForwarding())
        ));
        this.geyser.getLogger().debug("Disabling RakNet rate limiting " + rakRateLimitingDisabled);

        RakServerInitializer serverInitializer = new RakServerInitializer(this.geyser, rakSendCookie);
        playerGroup = serverInitializer.getEventLoopGroup();

        return new ServerBootstrap()
            .channelFactory(RakChannelFactory.server(TRANSPORT.datagramChannelClass()))
            .group(group, childGroup)
            .option(RakChannelOption.RAK_HANDLE_PING, true)
            .option(RakChannelOption.RAK_MAX_MTU, this.geyser.config().advanced().bedrock().mtu())
            .option(RakChannelOption.RAK_PACKET_LIMIT, rakRateLimitingDisabled ? 0 : rakPacketLimit)
            .option(RakChannelOption.RAK_GLOBAL_PACKET_LIMIT, rakGlobalPacketLimit)
            .option(RakChannelOption.RAK_SERVER_COOKIE_MODE, rakSendCookie ? RakServerCookieMode.ACTIVE : RakServerCookieMode.INVALID)
            .option(RakChannelOption.RAK_PROXY_PROTOCOL, this.geyser.config().advanced().bedrock().useHaproxyProtocol())
            .option(RakChannelOption.RAK_THROTTLE, rakRateLimitingDisabled ? null : new DefaultRakServerThrottle(maxConnectionsPerAddress, 4_000, 3))
            .childHandler(serverInitializer);
    }

    public boolean onConnectionRequest(InetSocketAddress inetSocketAddress, InetSocketAddress clientAddress) {
        List<String> allowedProxyIPs = geyser.config().advanced().bedrock().haproxyProtocolWhitelistedIps();
        if (geyser.config().advanced().bedrock().useHaproxyProtocol() && !allowedProxyIPs.isEmpty()) {
            if (!TrustedProxies.parse(geyser.config().advanced().bedrock().haproxyProtocolWhitelistedIps()).contains(inetSocketAddress.getAddress())) {
                connectionAttempts++;
                return false;
            }
        }

        connectionAttempts++;
        return ConnectionRequests.accept(geyser, clientAddress,
            geyser.config().advanced().bedrock().useHaproxyProtocol() ? inetSocketAddress : null);
    }


    /**
     * @return the throwable from the given supplier, or the throwable caught while calling the supplier.
     */
    private static Throwable throwableOrCaught(Supplier<Throwable> supplier) {
        try {
            return supplier.get();
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private static int positivePropOrDefault(String property, int defaultValue) {
        String value = System.getProperty(property);
        try {
            int parsed = value != null ? Integer.parseInt(value) : defaultValue;

            if (parsed < 1) {
                GeyserImpl.getInstance().getLogger().warning(
                    "Non-postive integer value for " + property + ": " + value + ". Using default value: " + defaultValue
                );
                return defaultValue;
            }

            return parsed;
        } catch (NumberFormatException e) {
            GeyserImpl.getInstance().getLogger().warning(
                "Invalid integer value for " + property + ": " + value + ". Using default value: " + defaultValue
            );
            return defaultValue;
        }
    }
}
