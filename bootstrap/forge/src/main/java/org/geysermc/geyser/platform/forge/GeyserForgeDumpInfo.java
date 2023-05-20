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

package org.geysermc.geyser.platform.forge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forgespi.language.IModInfo;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.text.AsteriskSerializer;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GeyserForgeDumpInfo extends BootstrapDumpInfo {

    private String platformVersion = null;
    private final Dist dist;

    @AsteriskSerializer.Asterisk(isIp = true)
    private final String serverIP;
    private final int serverPort;
    private final List<ModInfo> mods;

    public GeyserForgeDumpInfo(MinecraftServer server) {
        this.platformVersion = FMLLoader.versionInfo().mcAndForgeVersion();
        this.dist = FMLLoader.getDist();
        this.serverIP = server.getLocalIp() == null ? "unknown" : server.getLocalIp();
        this.serverPort = server.getPort();
        this.mods = new ArrayList<>();

        for (IModInfo mod : ModList.get().getMods()) {
            this.mods.add(new ModInfo(
                ModList.get().isLoaded(mod.getModId()),
                mod.getModId(),
                mod.getVersion().toString(),
                mod.getModURL().map(url -> url.toString()).orElse("")
            ));
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ModInfo {
        public boolean enabled;
        public String name;
        public String version;
        public String url;
    }
}
