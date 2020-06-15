/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.bungeecord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import org.geysermc.connector.FloodgateKeyLoader;
import org.geysermc.connector.configuration.GeyserJacksonConfiguration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeyserBungeeConfiguration extends GeyserJacksonConfiguration {

    private Path floodgateKey;

    public void loadFloodgate(GeyserBungeePlugin plugin, Configuration configuration) {
        Plugin floodgate = plugin.getProxy().getPluginManager().getPlugin("floodgate-bungee");
        floodgateKey = FloodgateKeyLoader.getKey(plugin.getGeyserLogger(), this, Paths.get(plugin.getDataFolder().toString(), configuration.getString("floodgate-key-file"), "public-key.pem"), floodgate, floodgate != null ? floodgate.getDataFolder().toPath() : null);
    }

    @Override
    public Path getFloodgateKeyFile() {
        return floodgateKey;
    }
}
