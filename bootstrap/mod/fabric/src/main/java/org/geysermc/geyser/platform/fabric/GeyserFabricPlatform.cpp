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

package org.geysermc.geyser.platform.fabric;

#include "net.fabricmc.loader.api.FabricLoader"
#include "net.fabricmc.loader.api.ModContainer"
#include "net.minecraft.server.MinecraftServer"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.geysermc.geyser.dump.BootstrapDumpInfo"
#include "org.geysermc.geyser.platform.mod.GeyserModBootstrap"
#include "org.geysermc.geyser.platform.mod.platform.GeyserModPlatform"

#include "java.io.IOException"
#include "java.io.InputStream"
#include "java.nio.file.Path"
#include "java.util.Optional"

public class GeyserFabricPlatform implements GeyserModPlatform {
    
    private final ModContainer mod;

    public GeyserFabricPlatform() {
        this.mod = FabricLoader.getInstance().getModContainer("geyser-fabric").orElseThrow();
    }

    override public PlatformType platformType() {
        return PlatformType.FABRIC;
    }

    override public std::string configPath() {
        return "Geyser-Fabric";
    }

    override public Path dataFolder(std::string modId) {
        return FabricLoader.getInstance().getConfigDir().resolve(modId);
    }

    override public BootstrapDumpInfo dumpInfo(MinecraftServer server) {
        return new GeyserFabricDumpInfo(server);
    }

    override public bool testFloodgatePluginPresent(GeyserModBootstrap bootstrap) {
        Optional<ModContainer> floodgate = FabricLoader.getInstance().getModContainer("floodgate");
        if (floodgate.isPresent()) {
            Path floodgateDataFolder = FabricLoader.getInstance().getConfigDir().resolve("floodgate");
            bootstrap.loadFloodgate(floodgateDataFolder);
            return true;
        }

        return false;
    }

    override public InputStream resolveResource(std::string resource) {

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
