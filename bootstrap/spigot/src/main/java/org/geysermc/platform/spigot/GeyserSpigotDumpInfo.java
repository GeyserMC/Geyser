/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.spigot;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.geysermc.connector.common.serializer.AsteriskSerializer;
import org.geysermc.connector.dump.BootstrapDumpInfo;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GeyserSpigotDumpInfo extends BootstrapDumpInfo {

    private String platformName;
    private String platformVersion;
    private String platformAPIVersion;
    private boolean onlineMode;
    private String serverIP;
    private int serverPort;
    private List<PluginInfo> plugins;

    GeyserSpigotDumpInfo() {
        super();
        this.platformName = Bukkit.getName();
        this.platformVersion = Bukkit.getVersion();
        this.platformAPIVersion = Bukkit.getBukkitVersion();
        this.onlineMode = Bukkit.getOnlineMode();
        if (AsteriskSerializer.showSensitive || (Bukkit.getIp().equals("") || Bukkit.getIp().equals("0.0.0.0"))) {
            this.serverIP = Bukkit.getIp();
        } else {
            this.serverIP = "***";
        }
        this.serverPort = Bukkit.getPort();
        this.plugins = new ArrayList<>();

        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            this.plugins.add(new PluginInfo(plugin.isEnabled(), plugin.getName(), plugin.getDescription().getVersion(), plugin.getDescription().getMain(), plugin.getDescription().getAuthors()));
        }
    }
}
