/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.configuration;

import org.geysermc.connector.GeyserLogger;

import java.nio.file.Path;
import java.util.Map;

public interface GeyserConfiguration {

    // Modify this when you update the config
    int CURRENT_CONFIG_VERSION = 3;

    IBedrockConfiguration getBedrock();

    IRemoteConfiguration getRemote();

    Map<String, ? extends IUserAuthenticationInfo> getUserAuths();

    boolean isCommandSuggestions();

    boolean isPassthroughMotd();

    boolean isPassthroughPlayerCounts();

    boolean isLegacyPingPassthrough();

    int getPingPassthroughInterval();

    int getMaxPlayers();

    boolean isDebugMode();

    int getGeneralThreadPool();

    boolean isAllowThirdPartyCapes();

    boolean isAllowThirdPartyEars();

    boolean isShowCooldown();

    String getDefaultLocale();

    Path getFloodgateKeyFile();

    boolean isAboveBedrockNetherBuilding();

    boolean isCacheChunks();

    int getCacheImages();

    IMetricsInfo getMetrics();

    interface IBedrockConfiguration {

        String getAddress();

        int getPort();

        String getMotd1();

        String getMotd2();
    }

    interface IRemoteConfiguration {

        String getAddress();

        int getPort();

        String getAuthType();
    }

    interface IUserAuthenticationInfo {
        String getEmail();

        String getPassword();
    }

    interface IMetricsInfo {

        boolean isEnabled();

        String getUniqueId();
    }

    int getConfigVersion();

    static void checkGeyserConfiguration(GeyserConfiguration geyserConfig, GeyserLogger geyserLogger) {
        if (geyserConfig.getConfigVersion() < CURRENT_CONFIG_VERSION) {
            geyserLogger.warning("Your Geyser config is out of date! Please regenerate your config when possible.");
        } else if (geyserConfig.getConfigVersion() > CURRENT_CONFIG_VERSION) {
            geyserLogger.warning("Your Geyser config is too new! Errors may occur.");
        }
    }
}
