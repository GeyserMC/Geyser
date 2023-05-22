/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.mod.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.geysermc.common.PlatformType;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.platform.mod.GeyserModBootstrap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * An interface which holds common methods that have different
 * APIs on their respective mod platforms.
 */
public interface GeyserModPlatform {

    /**
     * Gets the {@link PlatformType} of the mod platform.
     *
     * @return the platform type of the mod platform
     */
    @NotNull
    PlatformType platformType();

    /**
     * Gets the config path of the mod platform.
     *
     * @return the config path of the mod platform
     */
    @NotNull
    String configPath();

    /**
     * Gets the data folder of the mod platform.
     *
     * @return the data folder of the mod platform
     */
    @NotNull
    Path dataFolder(@NotNull String modId);

    /**
     * Gets the dump info of the mod platform.
     *
     * @param server the server to get the dump info from
     * @return the dump info of the mod platform
     */
    @NotNull
    BootstrapDumpInfo dumpInfo(@NotNull MinecraftServer server);

    /**
     * Tests if the Floodgate plugin is present on the mod platform.
     *
     * @return {@code true} if the Floodgate plugin is present on the mod platform, {@code false} otherwise
     */
    boolean testFloodgatePluginPresent(@NotNull GeyserModBootstrap bootstrap);

    /**
     * Resolves a resource from the mod jar.
     *
     * @param resource the name of the resource
     * @return the input stream of the resource
     */
    @Nullable
    InputStream resolveResource(@NotNull String resource);

    /**
     * Tests if the given source has the specified permission node.
     *
     * @param source the source to test
     * @param permissionNode the permission node
     * @return {@code true} if the source has the specified permission node, {@code false} otherwise
     */
    boolean hasPermission(@NotNull Player source, @NotNull String permissionNode);

    /**
     * Tests if the given source has the specified permission node.
     *
     * @param source the source to test
     * @param permissionNode the permission node
     * @param permissionLevel the permission level
     * @return {@code true} if the source has the specified permission node, {@code false} otherwise
     */
    boolean hasPermission(@NotNull Player source, @NotNull String permissionNode, int permissionLevel);
}
