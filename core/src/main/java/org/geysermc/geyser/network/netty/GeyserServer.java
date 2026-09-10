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

import io.netty.channel.Channel;
import lombok.Getter;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.connection.ConnectionRequestEvent;
import org.geysermc.geyser.command.defaults.ConnectionTestCommand;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.geysermc.geyser.event.type.GeyserBedrockPingEventImpl;
import org.geysermc.geyser.network.CIDRMatcher;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.network.GeyserSessionInitializer;
import org.geysermc.geyser.network.netty.transport.GeyserBedrockTransport;
import org.geysermc.geyser.network.netty.transport.GeyserDefineBedrockTransportsEvent;
import org.geysermc.geyser.network.netty.transport.RakNetGeyserTransport;
import org.geysermc.geyser.ping.GeyserPingInfo;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.skin.SkinProvider;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.WebUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GeyserServer {
    private static final boolean PRINT_DEBUG_PINGS = Boolean.parseBoolean(System.getProperty("Geyser.PrintPingsInDebugMode", "true"));

    /*
    The following constants are all used to ensure the ping does not reach a length where it is unparsable by the Bedrock client
     */
    private static final String PING_VERSION = GameProtocol.DEFAULT_BEDROCK_VERSION;
    private static final int PING_VERSION_BYTES_LENGTH = PING_VERSION.getBytes(StandardCharsets.UTF_8).length;
    private static final int BRAND_BYTES_LENGTH = GeyserImpl.NAME.getBytes(StandardCharsets.UTF_8).length;
    /**
     * The MOTD, sub-MOTD and Minecraft version ({@link #PING_VERSION_BYTES_LENGTH}) combined cannot reach this length.
     */
    private static final int MAGIC_RAKNET_LENGTH = 338;

    private final GeyserImpl geyser;

    /**
     * Transport-agnostic session creation, shared across all bound transports.
     */
    @Getter
    private final GeyserSessionInitializer sessionInitializer;

    /**
     * The transports Geyser binds, as decided by {@link GeyserDefineBedrockTransportsEvent}.
     */
    private final List<GeyserBedrockTransport> transports = new ArrayList<>();

    // Keep track of connection attempts for dump info
    @Getter
    private int connectionAttempts = 0;

    /**
     * The port to broadcast in the pong. This can be different from the port the server is bound to, e.g. due to port forwarding.
     */
    private final int broadcastPort;

    public GeyserServer(GeyserImpl geyser, int threadCount) {
        this.geyser = geyser;
        this.sessionInitializer = new GeyserSessionInitializer(geyser);

        this.transports.add(new RakNetGeyserTransport(geyser, threadCount, this.sessionInitializer));
        geyser.eventBus().fire(new GeyserDefineBedrockTransportsEvent(this.transports));

        if (this.transports.isEmpty()) {
            geyser.getLogger().warning("No Bedrock transports registered; Geyser will not be reachable.");
        }

        this.broadcastPort = geyser.config().advanced().bedrock().broadcastPort();
    }

    public CompletableFuture<Void> bind(InetSocketAddress address) {
        List<CompletableFuture<Void>> futures = new ArrayList<>(this.transports.size());
        for (GeyserBedrockTransport transport : this.transports) {
            futures.add(transport.bind(this, address));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public void shutdown() {
        for (GeyserBedrockTransport transport : this.transports) {
            try {
                transport.shutdown();
            } catch (Exception e) {
                geyser.getLogger().severe("Exception while shutting down transport " + transport.id(), e);
            }
        }
        this.sessionInitializer.getEventLoopGroup().shutdownGracefully();
        SkinProvider.shutdown();
    }

    public boolean onConnectionRequest(InetSocketAddress inetSocketAddress, InetSocketAddress clientAddress) {
        List<String> allowedProxyIPs = geyser.config().advanced().bedrock().haproxyProtocolWhitelistedIps();
        if (geyser.config().advanced().bedrock().useHaproxyProtocol() && !allowedProxyIPs.isEmpty()) {
            boolean isWhitelistedIP = false;
            for (CIDRMatcher matcher : getWhitelistedIPsMatchers()) {
                if (matcher.matches(inetSocketAddress.getAddress())) {
                    isWhitelistedIP = true;
                    break;
                }
            }

            if (!isWhitelistedIP) {
                connectionAttempts++;
                return false;
            }
        }

        String ip = geyser.config().logPlayerIpAddresses() ? clientAddress.toString() : "<IP address withheld>";

        ConnectionRequestEvent requestEvent = new ConnectionRequestEvent(
            clientAddress,
            geyser.config().advanced().bedrock().useHaproxyProtocol() ? inetSocketAddress : null
        );
        geyser.eventBus().fire(requestEvent);
        if (requestEvent.isCancelled()) {
            geyser.getLogger().debug("Connection request from " + ip + " was cancelled using the API!");
            connectionAttempts++;
            return false;
        }

        geyser.getLogger().debug(GeyserLocale.getLocaleStringLog("geyser.network.attempt_connect", ip));
        connectionAttempts++;
        return true;
    }

    public BedrockPong onQuery(Channel channel, InetSocketAddress inetSocketAddress) {
        if (geyser.config().debugMode() && PRINT_DEBUG_PINGS) {
            String ip = geyser.config().logPlayerIpAddresses() ? inetSocketAddress.toString() : "<IP address withheld>";
            geyser.getLogger().debug(GeyserLocale.getLocaleStringLog("geyser.network.pinged", ip));
        }

        GeyserConfig config = geyser.config();

        GeyserPingInfo pingInfo = null;
        if (config.motd().passthroughMotd() || config.motd().passthroughPlayerCounts()) {
            IGeyserPingPassthrough pingPassthrough = geyser.getBootstrap().getGeyserPingPassthrough();
            if (pingPassthrough != null) {
                pingInfo = pingPassthrough.getPingInformation(inetSocketAddress);
            }
        }

        BedrockPong pong = new BedrockPong()
                .edition("MCPE")
                .gameType("Survival") // Can only be Survival or Creative as of 1.16.210.59
                .nintendoLimited(false)
                .protocolVersion(GameProtocol.DEFAULT_BEDROCK_PROTOCOL)
                .version(PING_VERSION)
                .ipv4Port(this.broadcastPort)
                .ipv6Port(this.broadcastPort)
                .serverId(channel.config().getOption(RakChannelOption.RAK_GUID));

        if (config.motd().passthroughMotd() && pingInfo != null && pingInfo.getDescription() != null) {
            String[] motd = MessageTranslator.convertToPlainTextLenient(pingInfo.getDescription(), GeyserLocale.getDefaultLocale()).split("\n");
            String mainMotd = (motd.length > 0) ? motd[0] : config.motd().primaryMotd(); // First line of the motd.
            String subMotd = (motd.length > 1) ? motd[1] : config.motd().secondaryMotd(); // Second line of the motd if present, otherwise default.

            pong.motd(mainMotd.trim());
            pong.subMotd(subMotd.trim()); // Trimmed to shift it to the left, prevents the universe from collapsing on us just because we went 2 characters over the text box's limit.
        } else {
            pong.motd(config.motd().primaryMotd());
            pong.subMotd(config.motd().secondaryMotd());
        }

        // Placed here to prevent overriding values set in the ping event.
        if (config.motd().passthroughPlayerCounts() && pingInfo != null) {
            pong.playerCount(pingInfo.getPlayers().getOnline());
            pong.maximumPlayerCount(pingInfo.getPlayers().getMax());
        } else {
            pong.playerCount(geyser.getSessionManager().getSessions().size());
            pong.maximumPlayerCount(config.motd().maxPlayers());
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
        if (motdArray.length + subMotdLength > (MAGIC_RAKNET_LENGTH - PING_VERSION_BYTES_LENGTH)) {
            // Shorten the sub-MOTD first since that only appears locally
            if (subMotdLength > BRAND_BYTES_LENGTH) {
                pong.subMotd(GeyserImpl.NAME);
                subMotdLength = BRAND_BYTES_LENGTH;
            }
            if (motdArray.length > (MAGIC_RAKNET_LENGTH - PING_VERSION_BYTES_LENGTH - subMotdLength)) {
                // If the top MOTD is still too long, we chop it down
                byte[] newMotdArray = new byte[MAGIC_RAKNET_LENGTH - PING_VERSION_BYTES_LENGTH - subMotdLength];
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

    private List<CIDRMatcher> whitelistedIPsMatchers = null;

    /**
     * @return Unmodifiable list of {@link CIDRMatcher}s from {@link GeyserConfig.AdvancedBedrockConfig#haproxyProtocolWhitelistedIps()}
     */
    public List<CIDRMatcher> getWhitelistedIPsMatchers() {
        // Effective Java, Third Edition; Item 83: Use lazy initialization judiciously
        List<CIDRMatcher> matchers = this.whitelistedIPsMatchers;
        if (matchers == null) {
            synchronized (this) {
                // Check if proxyProtocolWhitelistedIPs contains URLs we need to fetch and parse by line
                List<String> whitelistedCIDRs = new ArrayList<>();
                for (String ip: geyser.config().advanced().bedrock().haproxyProtocolWhitelistedIps()) {
                    if (!ip.startsWith("http")) {
                        whitelistedCIDRs.add(ip);
                        continue;
                    }

                    WebUtils.getLineStream(ip).forEach(whitelistedCIDRs::add);
                }

                this.whitelistedIPsMatchers = matchers = whitelistedCIDRs.stream()
                    .map(CIDRMatcher::new)
                    .toList();
            }
        }
        return Collections.unmodifiableList(matchers);
    }
}
