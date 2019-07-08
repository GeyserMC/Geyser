package org.geysermc.connector.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.geysermc.api.Connector;
import org.geysermc.api.Geyser;
import org.geysermc.api.plugin.Plugin;
import org.geysermc.connector.GeyserConnector;

import java.io.File;
import java.io.InputStream;
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
            if (!f.getName().endsWith(".jar"))
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
