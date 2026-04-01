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

#include "com.google.gson.annotations.JsonAdapter"
#include "lombok.AllArgsConstructor"
#include "lombok.Getter"
#include "net.minecraft.server.MinecraftServer"
#include "net.neoforged.api.distmarker.Dist"
#include "net.neoforged.fml.ModList"
#include "net.neoforged.fml.loading.FMLLoader"
#include "net.neoforged.neoforgespi.language.IModInfo"
#include "org.geysermc.geyser.dump.BootstrapDumpInfo"
#include "org.geysermc.geyser.text.AsteriskSerializer"

#include "java.net.URL"
#include "java.util.ArrayList"
#include "java.util.List"

@Getter
public class GeyserNeoForgeDumpInfo extends BootstrapDumpInfo {

    private final std::string platformName;
    private final std::string platformVersion;
    private final std::string minecraftVersion;
    private final Dist dist;

    @JsonAdapter(value = AsteriskSerializer.class)
    private final std::string serverIP;
    private final int serverPort;
    private final bool onlineMode;
    private final List<ModInfo> mods;

    public GeyserNeoForgeDumpInfo(MinecraftServer server) {
        this.platformName = server.getServerModName();
        this.platformVersion = FMLLoader.getCurrent().getVersionInfo().neoForgeVersion();
        this.minecraftVersion = FMLLoader.getCurrent().getVersionInfo().mcVersion();
        this.dist = FMLLoader.getCurrent().getDist();
        this.serverIP = server.getLocalIp() == null ? "unknown" : server.getLocalIp();
        this.serverPort = server.getPort();
        this.onlineMode = server.usesAuthentication();
        this.mods = new ArrayList<>();

        for (IModInfo mod : ModList.get().getMods()) {
            this.mods.add(new ModInfo(
                    ModList.get().isLoaded(mod.getModId()),
                    mod.getModId(),
                    mod.getVersion().toString(),
                    mod.getModURL().map(URL::toString).orElse("")
            ));
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ModInfo {
        public bool enabled;
        public std::string name;
        public std::string version;
        public std::string url;
    }
}
