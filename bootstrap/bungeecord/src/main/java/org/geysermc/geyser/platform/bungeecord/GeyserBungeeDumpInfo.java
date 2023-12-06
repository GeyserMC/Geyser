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

package org.geysermc.geyser.platform.bungeecord;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.geysermc.geyser.dump.BootstrapDumpInfo;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class GeyserBungeeDumpInfo extends BootstrapDumpInfo {
    private final String platformName;
    private final String platformVersion;
    private final boolean onlineMode;
    private final List<ListenerInfo> listeners;
    private final List<PluginInfo> plugins;

    GeyserBungeeDumpInfo(ProxyServer proxy) {
        super();
        this.platformName = proxy.getName();
        this.platformVersion = proxy.getVersion();
        this.onlineMode = proxy.getConfig().isOnlineMode();
        this.listeners = new ArrayList<>();
        this.plugins = new ArrayList<>();

        for (net.md_5.bungee.api.config.ListenerInfo listener : proxy.getConfig().getListeners()) {
            InetSocketAddress address = (InetSocketAddress) listener.getSocketAddress();
            this.listeners.add(new ListenerInfo(address.getHostString(), address.getPort()));
        }

        for (Plugin plugin : proxy.getPluginManager().getPlugins()) {
            this.plugins.add(new PluginInfo(
                true,
                plugin.getDescription().getName(),
                plugin.getDescription().getVersion(),
                plugin.getDescription().getMain(),
                Collections.singletonList(plugin.getDescription().getAuthor()))
            );
        }
    }
}
