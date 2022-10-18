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

package org.geysermc.geyser.platform.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.text.AsteriskSerializer;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused") // The way that the dump renders makes them used
public class GeyserFabricDumpInfo extends BootstrapDumpInfo {

    private String platformVersion = null;
    private final EnvType environmentType;

    private final String serverIP;
    private final int serverPort;
    private final List<ModInfo> mods;

    public GeyserFabricDumpInfo(MinecraftServer server) {
        super();
        for (ModContainer modContainer : FabricLoader.getInstance().getAllMods()) {
            if (modContainer.getMetadata().getId().equals("fabricloader")) {
                this.platformVersion = modContainer.getMetadata().getVersion().getFriendlyString();
                break;
            }
        }
        this.environmentType = FabricLoader.getInstance().getEnvironmentType();
        if (AsteriskSerializer.showSensitive || (server.getLocalIp() == null || server.getLocalIp().equals("") || server.getLocalIp().equals("0.0.0.0"))) {
            this.serverIP = server.getLocalIp();
        } else {
            this.serverIP = "***";
        }
        this.serverPort = server.getPort();
        this.mods = new ArrayList<>();

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            this.mods.add(new ModInfo(mod));
        }
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public EnvType getEnvironmentType() {
        return environmentType;
    }

    public String getServerIP() {
        return this.serverIP;
    }

    public int getServerPort() {
        return this.serverPort;
    }

    public List<ModInfo> getMods() {
        return this.mods;
    }
}
