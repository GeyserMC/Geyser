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

package org.geysermc.platform.sponge;

import lombok.AllArgsConstructor;
import ninja.leaping.configurate.ConfigurationNode;
import org.geysermc.common.IGeyserConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GeyserSpongeConfiguration implements IGeyserConfiguration {

    private File dataFolder;
    private ConfigurationNode node;

    private SpongeBedrockConfiguration bedrockConfig;
    private SpongeRemoteConfiguration remoteConfig;
    private SpongeMetricsInfo metricsInfo;

    private Map<String, SpongeUserAuthenticationInfo> userAuthInfo = new HashMap<>();

    public GeyserSpongeConfiguration(File dataFolder, ConfigurationNode node) {
        this.dataFolder = dataFolder;
        this.node = node;

        this.bedrockConfig = new SpongeBedrockConfiguration(node.getNode("bedrock"));
        this.remoteConfig = new SpongeRemoteConfiguration(node.getNode("remote"));
        this.metricsInfo = new SpongeMetricsInfo();

        if (node.getNode("userAuths").getValue() == null)
            return;

        List<String> userAuths = new ArrayList<String>(((LinkedHashMap)node.getNode("userAuths").getValue()).keySet());
        for (String key : userAuths) {
            userAuthInfo.put(key, new SpongeUserAuthenticationInfo(key));
        }
    }

    @Override
    public SpongeBedrockConfiguration getBedrock() {
        return bedrockConfig;
    }

    @Override
    public SpongeRemoteConfiguration getRemote() {
        return remoteConfig;
    }

    @Override
    public Map<String, SpongeUserAuthenticationInfo> getUserAuths() {
        return userAuthInfo;
    }

    @Override
    public boolean isPingPassthrough() {
        return node.getNode("ping-passthrough").getBoolean(false);
    }

    @Override
    public int getMaxPlayers() {
        return node.getNode("max-players").getInt(100);
    }

    @Override
    public boolean isDebugMode() {
        return node.getNode("debug-mode").getBoolean(false);
    }

    @Override
    public int getGeneralThreadPool() {
        return node.getNode("genereal-thread-pool").getInt(32);
    }

    @Override
    public boolean isAllowThirdPartyCapes() {
        return node.getNode("allow-third-party-capes").getBoolean(true);
    }

    @Override
    public String getDefaultLocale() {
        return node.getNode("default-locale").getString("en_us");
    }

    @Override
    public Path getFloodgateKeyFile() {
        return Paths.get(dataFolder.toString(), node.getNode("floodgate-key-file").getString("public-key.pem"));
    }

    @Override
    public SpongeMetricsInfo getMetrics() {
        return metricsInfo;
    }

    @AllArgsConstructor
    public class SpongeBedrockConfiguration implements IBedrockConfiguration {

        private ConfigurationNode node;

        @Override
        public String getAddress() {
            return node.getNode("address").getString("0.0.0.0");
        }

        @Override
        public int getPort() {
            return node.getNode("port").getInt(19132);
        }

        @Override
        public String getMotd1() {
            return node.getNode("motd1").getString("GeyserMC");
        }

        @Override
        public String getMotd2() {
            return node.getNode("motd2").getString("GeyserMC");
        }
    }

    @AllArgsConstructor
    public class SpongeRemoteConfiguration implements IRemoteConfiguration {

        private ConfigurationNode node;

        @Override
        public String getAddress() {
            return node.getNode("address").getString("127.0.0.1");
        }

        @Override
        public int getPort() {
            return node.getNode("port").getInt(25565);
        }

        @Override
        public String getAuthType() {
            return node.getNode("auth-type").getString("online");
        }
    }

    public class SpongeUserAuthenticationInfo implements IUserAuthenticationInfo {

        private String key;

        public SpongeUserAuthenticationInfo(String key) {
            this.key = key;
        }

        @Override
        public String getEmail() {
            return node.getNode("userAuths").getNode(key).getNode("email").getString();
        }

        @Override
        public String getPassword() {
            return node.getNode("userAuths").getNode(key).getNode("password").getString();
        }
    }

    public class SpongeMetricsInfo implements IMetricsInfo {

        @Override
        public boolean isEnabled() {
            return node.getNode("metrics").getNode("enabled").getBoolean(true);
        }

        @Override
        public String getUniqueId() {
            return node.getNode("metrics").getNode("uuid").getString("generateduuid");
        }
    }
}
