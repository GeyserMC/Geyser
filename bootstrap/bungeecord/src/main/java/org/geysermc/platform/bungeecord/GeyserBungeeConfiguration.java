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

package org.geysermc.platform.bungeecord;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import org.geysermc.connector.FloodgateKeyLoader;
import org.geysermc.connector.GeyserConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class GeyserBungeeConfiguration implements GeyserConfiguration {

    private File dataFolder;
    private Configuration config;

    private BungeeBedrockConfiguration bedrockConfig;
    private BungeeRemoteConfiguration remoteConfig;
    private BungeeMetricsInfo metricsInfo;

    private Map<String, BungeeUserAuthenticationInfo> userAuthInfo = new HashMap<>();

    private Path floodgateKey;

    public GeyserBungeeConfiguration(File dataFolder, Configuration config) {
        this.dataFolder = dataFolder;
        this.config = config;

        bedrockConfig = new BungeeBedrockConfiguration();
        remoteConfig = new BungeeRemoteConfiguration();
        metricsInfo = new BungeeMetricsInfo();

        if (!config.contains("userAuths"))
            return;

        for (String key : config.getSection("userAuths").getKeys()) {
            userAuthInfo.put(key, new BungeeUserAuthenticationInfo(key));
        }
    }

    public void loadFloodgate(GeyserBungeePlugin plugin) {
        Plugin floodgate = plugin.getProxy().getPluginManager().getPlugin("floodgate-bungee");
        floodgateKey = FloodgateKeyLoader.getKey(plugin.getGeyserLogger(), this, Paths.get(dataFolder.toString(), config.getString("floodgate-key-file", "public-key.pem")), floodgate, floodgate != null ? floodgate.getDataFolder().toPath() : null);
    }

    @Override
    public BungeeBedrockConfiguration getBedrock() {
        return bedrockConfig;
    }

    @Override
    public BungeeRemoteConfiguration getRemote() {
        return remoteConfig;
    }

    @Override
    public Map<String, BungeeUserAuthenticationInfo> getUserAuths() {
        return userAuthInfo;
    }

    @Override
    public boolean isCommandSuggestions() {
        return config.getBoolean("command-suggestions", true);
    }

    @Override
    public boolean isPassthroughMotd() {
        return config.getBoolean("passthrough-motd", false);
    }

    @Override
    public boolean isPassthroughPlayerCounts() {
        return config.getBoolean("passthrough-player-counts", false);
    }

    @Override
    public boolean isLegacyPingPassthrough() {
        return config.getBoolean("legacy-ping-passthrough", false);
    }

    @Override
    public int getPingPassthroughInterval() {
        return config.getInt("ping-passthrough-interval", 3);
    }

    @Override
    public int getMaxPlayers() {
        return config.getInt("max-players", 10);
    }

    @Override
    public boolean isDebugMode() {
        return config.getBoolean("debug-mode", false);
    }

    @Override
    public int getGeneralThreadPool() {
        return config.getInt("general-thread-pool", 32);
    }

    @Override
    public boolean isAllowThirdPartyCapes() {
        return config.getBoolean("allow-third-party-capes", true);
    }

    @Override
    public boolean isAllowThirdPartyEars() {
        return config.getBoolean("allow-third-party-ears", false);
    }

    @Override
    public String getDefaultLocale() {
        return config.getString("default-locale", "en_us");
    }

    @Override
    public Path getFloodgateKeyFile() {
        return floodgateKey;
    }

    @Override
    public boolean isCacheChunks() {
        return config.getBoolean("cache-chunks", false);
    }

    @Override
    public boolean isAboveBedrockNetherBuilding() {
        return config.getBoolean("above-bedrock-nether-building", false);
    }

    @Override
    public BungeeMetricsInfo getMetrics() {
        return metricsInfo;
    }

    public class BungeeBedrockConfiguration implements IBedrockConfiguration {

        @Override
        public String getAddress() {
            return config.getString("bedrock.address", "0.0.0.0");
        }

        @Override
        public int getPort() {
            return config.getInt("bedrock.port", 25565);
        }

        @Override
        public String getMotd1() {
            return config.getString("bedrock.motd1", "GeyserMC");
        }

        @Override
        public String getMotd2() {
            return config.getString("bedrock.motd2", "GeyserMC");
        }

        @Override
        public String getEdition() {
            return config.getString("bedrock.edition", "bedrock");
        }
    }

    public class BungeeRemoteConfiguration implements IRemoteConfiguration {

        @Override
        public String getAddress() {
            return config.getString("remote.address", "127.0.0.1");
        }

        @Override
        public int getPort() {
            return config.getInt("remote.port", 25565);
        }

        @Override
        public String getAuthType() {
            return config.getString("remote.auth-type", "online");
        }
    }

    public class BungeeUserAuthenticationInfo implements IUserAuthenticationInfo {

        private String key;

        public BungeeUserAuthenticationInfo(String key) {
            this.key = key;
        }

        @Override
        public String getEmail() {
            return config.getString("userAuths." + key + ".email");
        }

        @Override
        public String getPassword() {
            return config.getString("userAuths." + key + ".password");
        }
    }

    public class BungeeMetricsInfo implements IMetricsInfo {

        @Override
        public boolean isEnabled() {
            return config.getBoolean("metrics.enabled", true);
        }

        @Override
        public String getUniqueId() {
            return config.getString("metrics.uuid", "generateduuid");
        }
    }

    @Override
    public int getConfigVersion() {
        return config.getInt("config-version", 0);
    }
}
