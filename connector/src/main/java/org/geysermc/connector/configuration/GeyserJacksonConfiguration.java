/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.common.serializer.AsteriskSerializer;

import java.nio.file.Path;
import java.util.Map;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class GeyserJacksonConfiguration implements GeyserConfiguration {

    /**
     * If the config was originally 'auto' before the values changed
     */
    @Setter
    private boolean autoconfiguredRemote = false;

    private BedrockConfiguration bedrock;
    private RemoteConfiguration remote;

    @JsonProperty("floodgate-key-file")
    private String floodgateKeyFile;

    public abstract Path getFloodgateKeyFile();

    private Map<String, UserAuthenticationInfo> userAuths;

    @JsonProperty("command-suggestions")
    private boolean commandSuggestions;

    @JsonProperty("passthrough-motd")
    private boolean isPassthroughMotd;

    @JsonProperty("passthrough-player-counts")
    private boolean isPassthroughPlayerCounts;

    @JsonProperty("passthrough-protocol-name")
    private boolean isPassthroughProtocolName;

    @JsonProperty("legacy-ping-passthrough")
    private boolean isLegacyPingPassthrough;

    @JsonProperty("ping-passthrough-interval")
    private int pingPassthroughInterval;

    @JsonProperty("max-players")
    private int maxPlayers;

    @JsonProperty("debug-mode")
    private boolean debugMode;

    @JsonProperty("general-thread-pool")
    private int generalThreadPool;

    @JsonProperty("allow-third-party-capes")
    private boolean allowThirdPartyCapes;

    @JsonProperty("show-cooldown")
    private boolean showCooldown = true;

    @JsonProperty("allow-third-party-ears")
    private boolean allowThirdPartyEars;

    @JsonProperty("default-locale")
    private String defaultLocale;

    @JsonProperty("cache-chunks")
    private boolean cacheChunks;

    @JsonProperty("cache-images")
    private int cacheImages = 0;

    @JsonProperty("above-bedrock-nether-building")
    private boolean aboveBedrockNetherBuilding;

    private MetricsInfo metrics;

    @Getter
    public static class BedrockConfiguration implements IBedrockConfiguration {

        @AsteriskSerializer.Asterisk(sensitive = true)
        private String address;

        @Setter
        private int port;

        @JsonProperty("clone-remote-port")
        private boolean cloneRemotePort;

        private String motd1;
        private String motd2;
    }

    @Getter
    public static class RemoteConfiguration implements IRemoteConfiguration {

        @Setter
        @AsteriskSerializer.Asterisk(sensitive = true)
        private String address;

        @Setter
        private int port;

        @Setter
        @JsonProperty("auth-type")
        private String authType;
    }

    @Getter
    public static class UserAuthenticationInfo implements IUserAuthenticationInfo {
        @AsteriskSerializer.Asterisk()
        private String email;

        @AsteriskSerializer.Asterisk()
        private String password;
    }

    @Getter
    public static class MetricsInfo implements IMetricsInfo {

        private boolean enabled;

        @JsonProperty("uuid")
        private String uniqueId;
    }

    @JsonProperty("enable-proxy-connections")
    private boolean enableProxyConnections = false;

    @JsonProperty("mtu")
    private int mtu = 1400;

    @JsonProperty("config-version")
    private int configVersion;
}
