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

package org.geysermc.geyser.platform.sponge;

import lombok.Getter;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.text.AsteriskSerializer;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.metadata.PluginMetadata;
import org.spongepowered.plugin.metadata.model.PluginContributor;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public class GeyserSpongeDumpInfo extends BootstrapDumpInfo {
    private final String platformName;
    private final String platformVersion;
    private final boolean onlineMode;

    @AsteriskSerializer.Asterisk(isIp = true)
    private final String serverIP;
    private final int serverPort;
    private final List<PluginInfo> plugins;

    GeyserSpongeDumpInfo() {
        PluginContainer container = Sponge.platform().container(Platform.Component.IMPLEMENTATION);
        PluginMetadata platformMeta = container.metadata();
        this.platformName = platformMeta.name().orElse("unknown");
        this.platformVersion = platformMeta.version().getQualifier();
        this.onlineMode = Sponge.server().isOnlineModeEnabled();
        Optional<InetSocketAddress> socketAddress = Sponge.server().boundAddress();
        this.serverIP = socketAddress.map(InetSocketAddress::getHostString).orElse("unknown");
        this.serverPort = socketAddress.map(InetSocketAddress::getPort).orElse(-1);
        this.plugins = new ArrayList<>();

        for (PluginContainer plugin : Sponge.pluginManager().plugins()) {
            PluginMetadata meta = plugin.metadata();
            List<String> contributors = meta.contributors().stream().map(PluginContributor::name).collect(Collectors.toList());
            this.plugins.add(new PluginInfo(true, meta.name().orElse("unknown"), meta.version().toString(), meta.entrypoint(), contributors));
        }
    }
}
