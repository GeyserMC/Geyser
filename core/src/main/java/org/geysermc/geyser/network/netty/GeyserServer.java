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

package org.geysermc.geyser.network.netty;

import com.github.steveice10.packetlib.helper.TransportHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import lombok.Getter;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.netty.handler.codec.raknet.server.RakServerOfflineHandler;
import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.defaults.ConnectionTestCommand;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.event.type.GeyserBedrockPingEventImpl;
import org.geysermc.geyser.network.CIDRMatcher;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.network.GeyserServerInitializer;
import org.geysermc.geyser.network.netty.handler.RakConnectionRequestHandler;
import org.geysermc.geyser.network.netty.handler.RakPingHandler;
import org.geysermc.geyser.network.netty.proxy.ProxyServerHandler;
import org.geysermc.geyser.ping.GeyserPingInfo;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.skin.SkinProvider;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class GeyserServer {
    private static final boolean PRINT_DEBUG_PINGS = Boolean.parseBoolean(System.getProperty("Geyser.PrintPingsInDebugMode", "true"));

    /*
    The following constants are all used to ensure the ping does not reach a length where it is unparsable by the Bedrock client
     */
    private static final int MINECRAFT_VERSION_BYTES_LENGTH = GameProtocol.DEFAULT_BEDROCK_CODEC.getMinecraftVersion().getBytes(StandardCharsets.UTF_8).length;
    private static final int BRAND_BYTES_LENGTH = GeyserImpl.NAME.getBytes(StandardCharsets.UTF_8).length;
    /**
     * The MOTD, sub-MOTD and Minecraft version ({@link #MINECRAFT_VERSION_BYTES_LENGTH}) combined cannot reach this length.
     */
    private static final int MAGIC_RAKNET_LENGTH = 338;

    private static final Transport TRANSPORT = compatibleTransport();

    /**
     * See {@link EventLoopGroup#shutdownGracefully(long, long, TimeUnit)}
     */
    private static final int SHUTDOWN_QUIET_PERIOD_MS = 100;
    private static final int SHUTDOWN_TIMEOUT_MS = 500;

    private final GeyserImpl geyser;
    private EventLoopGroup group;
    private final ServerBootstrap bootstrap;
    private EventLoopGroup playerGroup;

    @Getter
    private final ExpiringMap<InetSocketAddress, InetSocketAddress> proxiedAddresses;

    private ChannelFuture bootstrapFuture;

    public GeyserServer(GeyserImpl geyser, int threadCount) {
        this.geyser = geyser;
        this.group = TRANSPORT.eventLoopGroupFactory().apply(threadCount);

        this.bootstrap = this.createBootstrap(this.group);

        if (this.geyser.getConfig().getBedrock().isEnableProxyProtocol()) {
            this.proxiedAddresses = ExpiringMap.builder()
                    .expiration(30 + 1, TimeUnit.MINUTES)
                    .expirationPolicy(ExpirationPolicy.ACCESSED).build();
        } else {
            this.proxiedAddresses = null;
        }
    }

    public CompletableFuture<Void> bind(InetSocketAddress address) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.bootstrapFuture = this.bootstrap.bind(address).addListener(bindResult -> {
            if (bindResult.cause() != null) {
                future.completeExceptionally(bindResult.cause());
                return;
            }
            future.complete(null);
        });

        Channel channel = this.bootstrapFuture.channel();

        // Add our ping handler
        channel.pipeline()
                .addFirst(RakConnectionRequestHandler.NAME, new RakConnectionRequestHandler(this))
                .addAfter(RakServerOfflineHandler.NAME, RakPingHandler.NAME, new RakPingHandler(this));

        if (this.geyser.getConfig().getBedrock().isEnableProxyProtocol()) {
            channel.pipeline().addFirst("proxy-protocol-decoder", new ProxyServerHandler());
        }

        return future;
    }

    public void shutdown() {
        try {
            Future<?> future1 = this.group.shutdownGracefully(SHUTDOWN_QUIET_PERIOD_MS, SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            this.group = null;
            Future<?> future2 = this.playerGroup.shutdownGracefully(SHUTDOWN_QUIET_PERIOD_MS, SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            this.playerGroup = null;
            future1.sync();
            future2.sync();

            SkinProvider.shutdown();
        } catch (InterruptedException e) {
            GeyserImpl.getInstance().getLogger().severe("Exception in shutdown process", e);
        }
        this.bootstrapFuture.channel().closeFuture().syncUninterruptibly();
    }

    private ServerBootstrap createBootstrap(EventLoopGroup group) {
        if (this.geyser.getConfig().isDebugMode()) {
            this.geyser.getLogger().debug("EventLoop type: " + TRANSPORT.datagramChannel());
            if (TRANSPORT.datagramChannel() == NioDatagramChannel.class) {
                if (System.getProperties().contains("disableNativeEventLoop")) {
                    this.geyser.getLogger().debug("EventLoop type is NIO because native event loops are disabled.");
                } else {
                    // Use lambda here, not method reference, or else NoClassDefFoundError for Epoll/KQueue will not be caught
                    this.geyser.getLogger().debug("Reason for no Epoll: " + throwableOrCaught(() -> Epoll.unavailabilityCause()));
                    this.geyser.getLogger().debug("Reason for no KQueue: " + throwableOrCaught(() -> KQueue.unavailabilityCause()));
                }
            }
        }

        GeyserServerInitializer serverInitializer = new GeyserServerInitializer(this.geyser);
        playerGroup = serverInitializer.getEventLoopGroup();
        this.geyser.getLogger().debug("Setting MTU to " + this.geyser.getConfig().getMtu());
        return new ServerBootstrap()
                .channelFactory(RakChannelFactory.server(TRANSPORT.datagramChannel()))
                .group(group)
                .option(RakChannelOption.RAK_HANDLE_PING, true)
                .option(RakChannelOption.RAK_MAX_MTU, this.geyser.getConfig().getMtu())
                .childHandler(serverInitializer);
    }

    public boolean onConnectionRequest(InetSocketAddress inetSocketAddress) {
        List<String> allowedProxyIPs = geyser.getConfig().getBedrock().getProxyProtocolWhitelistedIPs();
        if (geyser.getConfig().getBedrock().isEnableProxyProtocol() && !allowedProxyIPs.isEmpty()) {
            boolean isWhitelistedIP = false;
            for (CIDRMatcher matcher : geyser.getConfig().getBedrock().getWhitelistedIPsMatchers()) {
                if (matcher.matches(inetSocketAddress.getAddress())) {
                    isWhitelistedIP = true;
                    break;
                }
            }

            if (!isWhitelistedIP) {
                return false;
            }
        }

        String ip;
        if (geyser.getConfig().isLogPlayerIpAddresses()) {
            if (geyser.getConfig().getBedrock().isEnableProxyProtocol()) {
                ip = this.proxiedAddresses.getOrDefault(inetSocketAddress, inetSocketAddress).toString();
            } else {
                ip = inetSocketAddress.toString();
            }
        } else {
            ip = "<IP address withheld>";
        }
        geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.attempt_connect", ip));
        return true;
    }

    public BedrockPong onQuery(InetSocketAddress inetSocketAddress) {
        if (geyser.getConfig().isDebugMode() && PRINT_DEBUG_PINGS) {
            String ip;
            if (geyser.getConfig().isLogPlayerIpAddresses()) {
                if (geyser.getConfig().getBedrock().isEnableProxyProtocol()) {
                    ip = this.proxiedAddresses.getOrDefault(inetSocketAddress, inetSocketAddress).toString();
                } else {
                    ip = inetSocketAddress.toString();
                }
            } else {
                ip = "<IP address withheld>";
            }
            geyser.getLogger().debug(GeyserLocale.getLocaleStringLog("geyser.network.pinged", ip));
        }

        GeyserConfiguration config = geyser.getConfig();

        GeyserPingInfo pingInfo = null;
        if (config.isPassthroughMotd() || config.isPassthroughPlayerCounts()) {
            IGeyserPingPassthrough pingPassthrough = geyser.getBootstrap().getGeyserPingPassthrough();
            if (pingPassthrough != null) {
                pingInfo = pingPassthrough.getPingInformation(inetSocketAddress);
            }
        }

        BedrockPong pong = new BedrockPong()
                .edition("MCPE")
                .gameType("Survival") // Can only be Survival or Creative as of 1.16.210.59
                .nintendoLimited(false)
                .protocolVersion(GameProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion())
                .version(GameProtocol.DEFAULT_BEDROCK_CODEC.getMinecraftVersion()) // Required to not be empty as of 1.16.210.59. Can only contain . and numbers.
                .ipv4Port(this.geyser.getConfig().getBedrock().port())
                .ipv6Port(this.geyser.getConfig().getBedrock().port())
                .serverId(bootstrapFuture.channel().config().getOption(RakChannelOption.RAK_GUID));

        if (config.isPassthroughMotd() && pingInfo != null && pingInfo.getDescription() != null) {
            String[] motd = MessageTranslator.convertMessageLenient(pingInfo.getDescription()).split("\n");
            String mainMotd = (motd.length > 0) ? motd[0] : config.getBedrock().primaryMotd(); // First line of the motd.
            String subMotd = (motd.length > 1) ? motd[1] : config.getBedrock().secondaryMotd(); // Second line of the motd if present, otherwise default.

            pong.motd(mainMotd.trim());
            pong.subMotd(subMotd.trim()); // Trimmed to shift it to the left, prevents the universe from collapsing on us just because we went 2 characters over the text box's limit.
        } else {
            pong.motd(config.getBedrock().primaryMotd());
            pong.subMotd(config.getBedrock().secondaryMotd());
        }

        // Placed here to prevent overriding values set in the ping event.
        if (config.isPassthroughPlayerCounts() && pingInfo != null) {
            pong.playerCount(pingInfo.getPlayers().getOnline());
            pong.maximumPlayerCount(pingInfo.getPlayers().getMax());
        } else {
            pong.playerCount(geyser.getSessionManager().getSessions().size());
            pong.maximumPlayerCount(config.getMaxPlayers());
        }

        this.geyser.eventBus().fire(new GeyserBedrockPingEventImpl(pong, inetSocketAddress));

        // https://github.com/GeyserMC/Geyser/issues/3388
        pong.motd(pong.motd().replace(';', ':'));
        pong.subMotd(pong.subMotd().replace(';', ':'));

        // Fallbacks to prevent errors and allow Bedrock to see the server
        if (pong.motd() == null || pong.motd().isBlank()) {
            pong.motd(GeyserImpl.NAME);
        }
        if (pong.subMotd() == null || pong.subMotd().isBlank()) {
            // Sub-MOTD cannot be empty as of 1.16.210.59
            pong.subMotd(GeyserImpl.NAME);
        }

        if (ConnectionTestCommand.CONNECTION_TEST_MOTD != null) {
            // Force-override as we are testing the connection and want to verify we are connecting to the right server through the MOTD
            pong.motd(ConnectionTestCommand.CONNECTION_TEST_MOTD);
            pong.subMotd(GeyserImpl.NAME);
        }

        // The ping will not appear if the MOTD + sub-MOTD is of a certain length.
        // We don't know why, though
        byte[] motdArray = pong.motd().getBytes(StandardCharsets.UTF_8);
        int subMotdLength = pong.subMotd().getBytes(StandardCharsets.UTF_8).length;
        if (motdArray.length + subMotdLength > (MAGIC_RAKNET_LENGTH - MINECRAFT_VERSION_BYTES_LENGTH)) {
            // Shorten the sub-MOTD first since that only appears locally
            if (subMotdLength > BRAND_BYTES_LENGTH) {
                pong.subMotd(GeyserImpl.NAME);
                subMotdLength = BRAND_BYTES_LENGTH;
            }
            if (motdArray.length > (MAGIC_RAKNET_LENGTH - MINECRAFT_VERSION_BYTES_LENGTH - subMotdLength)) {
                // If the top MOTD is still too long, we chop it down
                byte[] newMotdArray = new byte[MAGIC_RAKNET_LENGTH - MINECRAFT_VERSION_BYTES_LENGTH - subMotdLength];
                System.arraycopy(motdArray, 0, newMotdArray, 0, newMotdArray.length);
                pong.motd(new String(newMotdArray, StandardCharsets.UTF_8));
            }
        }

        //Bedrock will not even attempt a connection if the client thinks the server is full
        //so we have to fake it not being full
        if (pong.playerCount() >= pong.maximumPlayerCount()) {
            pong.maximumPlayerCount(pong.playerCount() + 1);
        }

        return pong;
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

    private static Transport compatibleTransport() {
        TransportHelper.TransportMethod transportMethod = TransportHelper.determineTransportMethod();
        if (transportMethod == TransportHelper.TransportMethod.EPOLL) {
            return new Transport(EpollDatagramChannel.class, EpollEventLoopGroup::new);
        }

        if (transportMethod == TransportHelper.TransportMethod.KQUEUE) {
            return new Transport(KQueueDatagramChannel.class, KQueueEventLoopGroup::new);
        }

        // if (transportMethod == TransportHelper.TransportMethod.IO_URING) {
        //     return new Transport(IOUringDatagramChannel.class, IOUringEventLoopGroup::new);
        // }

        return new Transport(NioDatagramChannel.class, NioEventLoopGroup::new);
    }

    private record Transport(Class<? extends DatagramChannel> datagramChannel, IntFunction<EventLoopGroup> eventLoopGroupFactory) {
    }
}
