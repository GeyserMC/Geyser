/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import org.geysermc.api.Connector;
import org.geysermc.api.plugin.Plugin;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GeyserPluginLoader extends ClassLoader {

    private Connector connector;

    public GeyserPluginLoader(Connector connector) {
        this.connector = connector;
    }

    public void loadPlugins() {
        File dir = new File("plugins");

        if (!dir.exists()) {
            dir.mkdir();
        }

        for (File f : dir.listFiles()) {
            if (!f.getName().toLowerCase().endsWith(".jar"))
                continue;

            try {
                ZipFile file = new ZipFile(f);
                ZipEntry e = file.getEntry("plugin.yml");

                if (e == null || e.isDirectory()) {
                    connector.getLogger().severe("Plugin " + f.getName() + " has no valid plugin.yml!");
                    continue;
                }

                file.stream().forEach((x) -> {
                    if (x.getName().endsWith(".class")) {
                        try {
                            InputStream is = file.getInputStream(x);
                            byte[] b = new byte[is.available()];
                            is.read(b);
                            this.defineClass(x.getName().replace(".class", "").replaceAll("/", "."), b, 0, b.length);
                            is.close();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                });

                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                InputStream is = file.getInputStream(e);

                PluginYML yml = mapper.readValue(is, PluginYML.class);
                is.close();
                Plugin plugin = (Plugin) Class.forName(yml.main, true, this).newInstance();

                Class cl = Plugin.class;

                Field name = cl.getDeclaredField("name");
                name.setAccessible(true);

                Field version = cl.getDeclaredField("version");
                version.setAccessible(true);

                name.set(plugin, yml.name);

                version.set(plugin, yml.version);

                connector.getLogger().info("Loading plugin " + yml.name + " version " + yml.version);
                connector.getPluginManager().loadPlugin(plugin);
            } catch (Exception e) {
                connector.getLogger().severe("Error loading plugin " + f.getName());
                e.printStackTrace();
            }
        }

        for (Plugin plugin : connector.getPluginManager().getPlugins()) {
            connector.getPluginManager().enablePlugin(plugin);
        }
    }

    public void loadPlugin(Plugin plugin) {
        plugin.onLoad();
    }

    public void enablePlugin(Plugin plugin) {
        plugin.setEnabled(true);
        plugin.onEnable();
    }

    public void disablePlugin(Plugin plugin) {
        plugin.setEnabled(false);
        plugin.onDisable();
    }
}
