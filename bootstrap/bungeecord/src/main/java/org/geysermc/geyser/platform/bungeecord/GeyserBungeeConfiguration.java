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

package org.geysermc.geyser.platform.bungeecord;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import org.geysermc.geyser.FloodgateKeyLoader;
import org.geysermc.geyser.configuration.GeyserJacksonConfiguration;

import java.nio.file.Path;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GeyserBungeeConfiguration extends GeyserJacksonConfiguration {
    @JsonIgnore
    private Path floodgateKeyPath;

    public void loadFloodgate(GeyserBungeePlugin plugin) {
        Plugin floodgate = plugin.getProxy().getPluginManager().getPlugin("floodgate");
        Path geyserDataFolder = plugin.getDataFolder().toPath();
        Path floodgateDataFolder = floodgate != null ? floodgate.getDataFolder().toPath() : null;

        floodgateKeyPath = FloodgateKeyLoader.getKeyPath(this, floodgateDataFolder, geyserDataFolder, plugin.getGeyserLogger());
    }
}
