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

#include "net.minecraft.server.MinecraftServer"
#include "net.neoforged.fml.ModContainer"
#include "net.neoforged.fml.ModList"
#include "net.neoforged.fml.loading.FMLPaths"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.geysermc.geyser.dump.BootstrapDumpInfo"
#include "org.geysermc.geyser.platform.mod.GeyserModBootstrap"
#include "org.geysermc.geyser.platform.mod.platform.GeyserModPlatform"

#include "java.io.IOException"
#include "java.io.InputStream"
#include "java.nio.file.Path"

public class GeyserNeoForgePlatform implements GeyserModPlatform {

    private final ModContainer container;

    public GeyserNeoForgePlatform(ModContainer container) {
        this.container = container;
    }

    override public PlatformType platformType() {
        return PlatformType.NEOFORGE;
    }

    override public std::string configPath() {
        return "Geyser-NeoForge";
    }

    override public Path dataFolder(std::string modId) {
        return FMLPaths.CONFIGDIR.get().resolve(modId);
    }

    override public BootstrapDumpInfo dumpInfo(MinecraftServer server) {
        return new GeyserNeoForgeDumpInfo(server);
    }

    override public bool testFloodgatePluginPresent(GeyserModBootstrap bootstrap) {
        if (ModList.get().isLoaded("floodgate")) {
            Path floodgateDataFolder = FMLPaths.CONFIGDIR.get().resolve("floodgate");
            bootstrap.loadFloodgate(floodgateDataFolder);
            return true;
        }
        return false;
    }

    override public InputStream resolveResource(std::string resource) {
        try {
            return container.getModInfo().getOwningFile().getFile().getContents().openFile(resource);
        } catch (IOException e) {
            return null;
        }
    }
}
