/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.auth.AuthType;
import org.geysermc.geyser.text.AsteriskSerializer;
import org.geysermc.geyser.network.CIDRMatcher;
import org.geysermc.geyser.text.GeyserLocale;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("FieldMayBeFinal") // Jackson requires that the fields are not final
public abstract class GeyserJacksonConfiguration implements GeyserConfiguration {

    /**
     * If the config was originally 'auto' before the values changed
     */
    @Setter
    private boolean autoconfiguredRemote = false;

    private BedrockConfiguration bedrock = new BedrockConfiguration();
    private RemoteConfiguration remote = new RemoteConfiguration();

    @JsonProperty("floodgate-key-file")
    private String floodgateKeyFile = "key.pem";

    public abstract Path getFloodgateKeyPath();

    private Map<String, UserAuthenticationInfo> userAuths;

    @JsonProperty("command-suggestions")
    private boolean commandSuggestions = true;

    @JsonProperty("passthrough-motd")
    private boolean isPassthroughMotd = false;

    @JsonProperty("passthrough-player-counts")
    private boolean isPassthroughPlayerCounts = false;

    @JsonProperty("passthrough-protocol-name")
    private boolean isPassthroughProtocolName = false;

    @JsonProperty("legacy-ping-passthrough")
    private boolean isLegacyPingPassthrough = false;

    @JsonProperty("ping-passthrough-interval")
    private int pingPassthroughInterval = 3;

    @JsonProperty("forward-player-ping")
    private boolean forwardPlayerPing = false;

    @JsonProperty("max-players")
    private int maxPlayers = 100;

    @JsonProperty("debug-mode")
    private boolean debugMode = false;

    @JsonProperty("allow-third-party-capes")
    private boolean allowThirdPartyCapes = true;

    @JsonProperty("show-cooldown")
    private String showCooldown = "title";

    @JsonProperty("show-coordinates")
    private boolean showCoordinates = true;

    @JsonDeserialize(using = EmoteOffhandWorkaroundOption.Deserializer.class)
    @JsonProperty("emote-offhand-workaround")
    private EmoteOffhandWorkaroundOption emoteOffhandWorkaround = EmoteOffhandWorkaroundOption.DISABLED;

    @JsonProperty("allow-third-party-ears")
    private boolean allowThirdPartyEars = false;

    @JsonProperty("default-locale")
    private String defaultLocale = null; // is null by default so system language takes priority

    @JsonProperty("cache-images")
    private int cacheImages = 0;

    @JsonProperty("allow-custom-skulls")
    private boolean allowCustomSkulls = true;

    @JsonProperty("add-non-bedrock-items")
    private boolean addNonBedrockItems = true;

    @JsonProperty("above-bedrock-nether-building")
    private boolean aboveBedrockNetherBuilding = false;

    @JsonProperty("force-resource-packs")
    private boolean forceResourcePacks = true;

    @JsonProperty("xbox-achievements-enabled")
    private boolean xboxAchievementsEnabled = false;

    private MetricsInfo metrics = new MetricsInfo();

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BedrockConfiguration implements IBedrockConfiguration {
        @AsteriskSerializer.Asterisk(isIp = true)
        private String address = "0.0.0.0";

        @Setter
        private int port = 19132;

        @JsonProperty("clone-remote-port")
        private boolean cloneRemotePort = false;

        private String motd1 = "GeyserMC";
        private String motd2 = "Geyser";

        @JsonProperty("server-name")
        private String serverName = GeyserImpl.NAME;

        @JsonProperty("compression-level")
        private int compressionLevel = 6;

        public int getCompressionLevel() {
            return Math.max(-1, Math.min(compressionLevel, 9));
        }

        @JsonProperty("enable-proxy-protocol")
        private boolean enableProxyProtocol = false;

        @JsonProperty("proxy-protocol-whitelisted-ips")
        private List<String> proxyProtocolWhitelistedIPs = Collections.emptyList();

        @JsonIgnore
        private List<CIDRMatcher> whitelistedIPsMatchers = null;

        @Override
        public List<CIDRMatcher> getWhitelistedIPsMatchers() {
            // Effective Java, Third Edition; Item 83: Use lazy initialization judiciously
            List<CIDRMatcher> matchers = this.whitelistedIPsMatchers;
            if (matchers == null) {
                synchronized (this) {
                    this.whitelistedIPsMatchers = matchers = proxyProtocolWhitelistedIPs.stream()
                            .map(CIDRMatcher::new)
                            .collect(Collectors.toList());
                }
            }
            return Collections.unmodifiableList(matchers);
        }
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RemoteConfiguration implements IRemoteConfiguration {
        @Setter
        @AsteriskSerializer.Asterisk(isIp = true)
        private String address = "auto";

        @JsonDeserialize(using = PortDeserializer.class)
        @Setter
        private int port = 25565;

        @Setter
        @JsonDeserialize(using = AuthType.Deserializer.class)
        @JsonProperty("auth-type")
        private AuthType authType = AuthType.ONLINE;

        @JsonProperty("allow-password-authentication")
        private boolean passwordAuthentication = true;

        @JsonProperty("use-proxy-protocol")
        private boolean useProxyProtocol = false;

        @JsonProperty("forward-hostname")
        private boolean forwardHost = false;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true) // DO NOT REMOVE THIS! Otherwise, after we remove microsoft-account configs will not load
    public static class UserAuthenticationInfo implements IUserAuthenticationInfo {
        @AsteriskSerializer.Asterisk()
        private String email;

        @AsteriskSerializer.Asterisk()
        private String password;

        @JsonProperty("microsoft-account")
        private boolean microsoftAccount = false;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetricsInfo implements IMetricsInfo {
        private boolean enabled = true;

        @JsonProperty("uuid")
        private String uniqueId = UUID.randomUUID().toString();
    }

    @JsonProperty("scoreboard-packet-threshold")
    private int scoreboardPacketThreshold = 10;

    @JsonProperty("enable-proxy-connections")
    private boolean enableProxyConnections = false;

    @JsonProperty("mtu")
    private int mtu = 1400;

    @JsonProperty("use-direct-connection")
    private boolean useDirectConnection = true;

    @JsonProperty("config-version")
    private int configVersion = 0;

    /**
     * Ensure that the port deserializes in the config as a number no matter what.
     */
    protected static class PortDeserializer extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println(GeyserLocale.getLocaleStringLog("geyser.bootstrap.config.invalid_port"));
                return 25565;
            }
        }
    }
}
