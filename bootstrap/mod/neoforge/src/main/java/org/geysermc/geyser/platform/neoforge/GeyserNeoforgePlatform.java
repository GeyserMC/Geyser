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

package org.geysermc.geyser.platform.neoforge;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.platform.mod.GeyserModBootstrap;
import org.geysermc.geyser.platform.mod.platform.GeyserModPlatform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;

public class GeyserNeoforgePlatform implements GeyserModPlatform {

    @Override
    public @NotNull PlatformType platformType() {
        return PlatformType.NEOFORGE;
    }

    @Override
    public @NotNull String configPath() {
        return "Geyser-Neoforge";
    }

    @Override
    public @NotNull Path dataFolder(@NotNull String modId) {
        return FMLPaths.CONFIGDIR.get().resolve(modId);
    }

    @Override
    public @NotNull BootstrapDumpInfo dumpInfo(@NotNull MinecraftServer server) {
        return new GeyserNeoforgeDumpInfo(server);
    }

    @Override
    public boolean testFloodgatePluginPresent(@NotNull GeyserModBootstrap bootstrap) {
        return false; // No Floodgate plugin for Forge yet
    }

    @Override
    public @Nullable InputStream resolveResource(@NotNull String resource) {
        return GeyserBootstrap.class.getClassLoader().getResourceAsStream(resource);
    }
}
