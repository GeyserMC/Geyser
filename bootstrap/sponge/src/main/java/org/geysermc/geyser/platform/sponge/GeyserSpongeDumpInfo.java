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

package org.geysermc.geyser.platform.sponge;

import lombok.Getter;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GeyserSpongeDumpInfo extends BootstrapDumpInfo {
    private final String platformName;
    private final String platformVersion;
    private final boolean onlineMode;
    private final String serverIP;
    private final int serverPort;
    private final List<PluginInfo> plugins;

    GeyserSpongeDumpInfo() {
        super();
        PluginContainer container = Sponge.getPlatform().getContainer(Platform.Component.IMPLEMENTATION);
        this.platformName = container.getName();
        this.platformVersion = container.getVersion().get();
        this.onlineMode = Sponge.getServer().getOnlineMode();
        this.serverIP = Sponge.getServer().getBoundAddress().get().getHostString();
        this.serverPort = Sponge.getServer().getBoundAddress().get().getPort();
        this.plugins = new ArrayList<>();

        for (PluginContainer plugin : Sponge.getPluginManager().getPlugins()) {
            String pluginClass = plugin.getInstance().map((pl) -> pl.getClass().getName()).orElse("unknown");
            this.plugins.add(new PluginInfo(true, plugin.getName(), plugin.getVersion().get(), pluginClass, plugin.getAuthors()));
        }
    }
}
