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

package org.geysermc.geyser.api;

import org.geysermc.api.Geyser;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.api.util.ApiVersion;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.extension.ExtensionManager;
import org.geysermc.geyser.api.network.BedrockListener;
import org.geysermc.geyser.api.network.RemoteServer;
import org.geysermc.geyser.api.util.MinecraftVersion;
import org.geysermc.geyser.api.util.PlatformType;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Represents the API used in Geyser.
 */
@ApiStatus.NonExtendable
public interface GeyserApi extends GeyserApiBase {
    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable GeyserConnection connectionByUuid(UUID uuid);

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable GeyserConnection connectionByXuid(String xuid);

    /**
     * {@inheritDoc}
     */
    List<? extends GeyserConnection> onlineConnections();

    /**
     * Gets the {@link ExtensionManager}.
     *
     * @return the extension manager
     */
    ExtensionManager extensionManager();

    /**
     * Provides an implementation for the specified API type.
     *
     * @param apiClass the builder class
     * @param <R> the implementation type
     * @param <T> the API type
     * @throws IllegalArgumentException if there is no provider for the specified API class
     * @return the builder instance
     */
    <R extends T, T> R provider(Class<T> apiClass, @Nullable Object... args);

    /**
     * Gets the {@link EventBus} for handling
     * Geyser events.
     *
     * @return the event bus
     */
    EventBus<EventRegistrar> eventBus();

    /**
     * Gets the default {@link RemoteServer} configured
     * within the config file that is used by default.
     *
     * @return the default remote server used within Geyser
     */
    RemoteServer defaultRemoteServer();

    /**
     * Gets the {@link BedrockListener} used for listening
     * for Minecraft: Bedrock Edition client connections.
     *
     * @return the listener used for Bedrock client connectins
     */
    BedrockListener bedrockListener();

    /**
     * Gets the {@link Path} to the Geyser config directory.
     *
     * @return the path to the Geyser config directory
     */
    Path configDirectory();

    /**
     * Gets the {@link Path} to the Geyser packs directory.
     *
     * @return the path to the Geyser packs directory
     */
    Path packDirectory();

    /**
     * Gets {@link PlatformType} the extension is running on
     *
     * @return type of platform
     */
    PlatformType platformType();

    /**
     * Gets the version of Java Minecraft that is supported.
     *
     * @return the supported version of Java Minecraft
     */
    MinecraftVersion supportedJavaVersion();

    /**
     * Gets a list of Bedrock Minecraft versions that are supported.
     *
     * @return the list of supported Bedrock Minecraft versions
     */
    List<MinecraftVersion> supportedBedrockVersions();

    /**
     * Gets the {@link CommandSource} for the console.
     *
     * @return the console command source
     */
    CommandSource consoleCommandSource();

    /**
     * Gets the current {@link GeyserApiBase} instance.
     *
     * @return the current geyser api instance
     */
    static GeyserApi api() {
        return Geyser.api(GeyserApi.class);
    }

    /**
     * Returns the {@link ApiVersion} representing the current Geyser api version.
     * See the <a href="https://github.com/geysermc/api/blob/master/geyser-versioning.md">Geyser version outline</a>)
     *
     * @return the current geyser api version
     */
     default ApiVersion geyserApiVersion() {
        return BuildData.API_VERSION;
     }
}
