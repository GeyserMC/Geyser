package org.geysermc.connector.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.geysermc.api.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Loader extends ClassLoader {
    static {
        System.out.println("a");
        Loader l = new Loader();
        System.out.println("b");
        File dir = new File("plugins");
        System.out.println(dir.getAbsoluteFile());

        if(!dir.exists()) {
            dir.mkdir();
        }

        for(File f : dir.listFiles()) {
            if(f.getName().endsWith(".jar")) {
                try {
                    ZipFile file = new ZipFile(f);

                    ZipEntry e = file.getEntry("plugin.yml");

                    if(e == null || e.isDirectory()) {
                        System.err.println("Plugin " + f.getName() + " has no valid plugin.yml!");
                        continue;
                    }

                    file.stream().forEach((x) -> {
                        if(x.getName().endsWith(".class")) {
                            try {
                                InputStream is = file.getInputStream(x);
                                byte[] b = new byte[is.available()];
                                is.read(b);
                                l.defineClass(x.getName().replace(".class", "").replaceAll("/", "."), b, 0, b.length);
                                is.close();
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    });

                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

                    InputStream is = file.getInputStream(e);

                    PluginYML yml;

                    yml = mapper.readValue(is, PluginYML.class);

                    is.close();

                    ((Plugin) Class.forName(yml.main, true, l).newInstance()).onEnable();

                } catch (Exception e) {
                    System.out.println("Error loading plugin " + f.getName());
                    e.printStackTrace();
                }
            }
        }
        LOADER = l;
    }

    public static final Loader LOADER;

    public static void start() {

    }

    private Loader() {

    }
}
