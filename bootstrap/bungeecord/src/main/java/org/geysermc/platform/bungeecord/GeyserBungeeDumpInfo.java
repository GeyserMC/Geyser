/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.platform.bungeecord;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.geysermc.connector.dump.BootstrapDumpInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class GeyserBungeeDumpInfo extends BootstrapDumpInfo {

    private String platformName;
    private String platformVersion;
    private boolean onlineMode;
    private List<ListenerInfo> listeners;
    private List<PluginInfo> plugins;

    GeyserBungeeDumpInfo(ProxyServer proxy) {
        super();
        this.platformName = proxy.getName();
        this.platformVersion = proxy.getVersion();
        this.onlineMode = proxy.getConfig().isOnlineMode();
        this.listeners = new ArrayList<>();
        this.plugins = new ArrayList<>();

        for (net.md_5.bungee.api.config.ListenerInfo listener : proxy.getConfig().getListeners()) {
            this.listeners.add(new ListenerInfo(listener.getHost().getHostString(), listener.getHost().getPort()));
        }

        for (Plugin plugin : proxy.getPluginManager().getPlugins()) {
            this.plugins.add(new PluginInfo(true, plugin.getDescription().getName(), plugin.getDescription().getVersion(), plugin.getDescription().getMain(), Arrays.asList(plugin.getDescription().getAuthor())));
        }
    }
}
