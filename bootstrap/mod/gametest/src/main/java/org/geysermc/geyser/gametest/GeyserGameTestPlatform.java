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

package org.geysermc.geyser.gametest;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.platform.mod.GeyserModBootstrap;
import org.geysermc.geyser.platform.mod.platform.GeyserModPlatform;
import org.geysermc.geyser.util.InternalPlatformType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

public class GeyserGameTestPlatform implements GeyserModPlatform {
    
    private final ModContainer mod;

    public GeyserGameTestPlatform() {
        this.mod = FabricLoader.getInstance().getModContainer("geyser-gametest").orElseThrow();
    }

    @Override
    public PlatformType platformType() {
        return InternalPlatformType.GAMETEST;
    }

    @Override
    public String configPath() {
        return "Geyser-Gametest";
    }

    @Override
    public Path dataFolder(String modId) {
        return FabricLoader.getInstance().getConfigDir().resolve(modId);
    }

    @Override
    public BootstrapDumpInfo dumpInfo(MinecraftServer server) {
        return new BootstrapDumpInfo();
    }

    @Override
    public boolean testFloodgatePluginPresent(GeyserModBootstrap bootstrap) {
        Optional<ModContainer> floodgate = FabricLoader.getInstance().getModContainer("floodgate");
        if (floodgate.isPresent()) {
            Path floodgateDataFolder = FabricLoader.getInstance().getConfigDir().resolve("floodgate");
            bootstrap.loadFloodgate(floodgateDataFolder);
            return true;
        }

        return false;
    }

    @Override
    public @Nullable InputStream resolveResource(String resource) {
        // We need to handle this differently, because Fabric shares the classloader across multiple mods
        Path path = this.mod.findPath(resource).orElse(null);
        if (path == null) {
            return null;
        }

        try {
            return path.getFileSystem()
                    .provider()
                    .newInputStream(path);
        } catch (IOException e) {
            return null;
        }
    }
}
