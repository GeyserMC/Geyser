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

package org.geysermc.geyser;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.configuration.ConfigLoader;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.level.GeyserWorldManager;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.util.metrics.MetricsPlatform;
import org.geysermc.geyser.util.metrics.ProvidedMetricsPlatform;

import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface GeyserBootstrap {

    GeyserWorldManager DEFAULT_CHUNK_MANAGER = new GeyserWorldManager();

    
    void onGeyserInitialize();

    
    void onGeyserEnable();

    
    void onGeyserDisable();

    
    void onGeyserShutdown();

    
    @NonNull
    PlatformType platformType();

    
    GeyserConfig config();

    
    GeyserLogger getGeyserLogger();

    
    CommandRegistry getCommandRegistry();

    
    @Nullable
    IGeyserPingPassthrough getGeyserPingPassthrough();

    
    default WorldManager getWorldManager() {
        return DEFAULT_CHUNK_MANAGER;
    }

    
    Path getConfigFolder();

    
    default Path getSavedUserLoginsFolder() {
        return getConfigFolder();
    }

    
    BootstrapDumpInfo getDumpInfo();

    
    @Nullable
    default String getMinecraftServerVersion() {
        return null;
    }

    @Nullable
    default SocketAddress getSocketAddress() {
        return null;
    }

    default Path getLogsPath() {
        return Paths.get("logs/latest.log");
    }

    
    @NonNull String getServerPlatform();

    
    default @Nullable InputStream getResourceOrNull(String resource) {
        return GeyserBootstrap.class.getClassLoader().getResourceAsStream(resource);
    }

    
    default @NonNull InputStream getResourceOrThrow(@NonNull String resource) {
        InputStream stream = getResourceOrNull(resource);
        if (stream == null) {
            throw new AssertionError("Unable to find resource: " + resource);
        }
        return stream;
    }

    
    @NonNull
    String getServerBindAddress();

    
    int getServerPort();

    
    boolean testFloodgatePluginPresent();

    
    Path getFloodgateKeyPath();

    @Nullable
    default MetricsPlatform createMetricsPlatform() {
        return new ProvidedMetricsPlatform();
    }

    default <T extends GeyserConfig> T loadConfig(Class<T> configClass) {
        return new ConfigLoader(this).createFolder().load(configClass);
    }
}
