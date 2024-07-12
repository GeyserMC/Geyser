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

package org.geysermc.geyser.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.Geyser;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.extension.ExtensionManager;
import org.geysermc.geyser.api.network.BedrockListener;
import org.geysermc.geyser.api.network.RemoteServer;
import org.geysermc.geyser.api.util.MinecraftVersion;
import org.geysermc.geyser.api.util.PlatformType;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Represents the API used in Geyser.
 */
public interface GeyserApi extends GeyserApiBase {
    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable GeyserConnection connectionByUuid(@NonNull UUID uuid);

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable GeyserConnection connectionByXuid(@NonNull String xuid);

    /**
     * {@inheritDoc}
     */
    @NonNull
    List<? extends GeyserConnection> onlineConnections();

    /**
     * Gets the {@link ExtensionManager}.
     *
     * @return the extension manager
     */
    @NonNull
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
    @NonNull
    <R extends T, T> R provider(@NonNull Class<T> apiClass, @Nullable Object... args);

    /**
     * Gets the {@link EventBus} for handling
     * Geyser events.
     *
     * @return the event bus
     */
    @NonNull
    EventBus<EventRegistrar> eventBus();

    /**
     * Gets the default {@link RemoteServer} configured
     * within the config file that is used by default.
     *
     * @return the default remote server used within Geyser
     */
    @NonNull
    RemoteServer defaultRemoteServer();

    /**
     * Gets the {@link BedrockListener} used for listening
     * for Minecraft: Bedrock Edition client connections.
     *
     * @return the listener used for Bedrock client connectins
     */
    @NonNull
    BedrockListener bedrockListener();

    /**
     * Gets the {@link Path} to the Geyser config directory.
     *
     * @return the path to the Geyser config directory
     */
    @NonNull
    Path configDirectory();

    /**
     * Gets the {@link Path} to the Geyser packs directory.
     *
     * @return the path to the Geyser packs directory
     */
    @NonNull
    Path packDirectory();

    /**
     * Gets {@link PlatformType} the extension is running on
     *
     * @return type of platform
     */
    @NonNull
    PlatformType platformType();

    /**
     * Gets the version of Java Minecraft that is supported.
     *
     * @return the supported version of Java Minecraft
     */
    @NonNull
    MinecraftVersion supportedJavaVersion();

    /**
     * Gets a list of Bedrock Minecraft versions that are supported.
     *
     * @return the list of supported Bedrock Minecraft versions
     */
    @NonNull
    List<MinecraftVersion> supportedBedrockVersions();

    /**
     * Gets the {@link CommandSource} for the console.
     *
     * @return the console command source
     */
    @NonNull
    CommandSource consoleCommandSource();

    /**
     * Gets the current {@link GeyserApiBase} instance.
     *
     * @return the current geyser api instance
     */
    @NonNull
    static GeyserApi api() {
        return Geyser.api(GeyserApi.class);
    }
}
