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

package org.geysermc.geyser.api.extension;

import org.geysermc.api.GeyserApiBase;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class GeyserExtension implements Extension {
    private boolean initialized = false;
    private boolean enabled = false;
    private File file = null;
    private File dataFolder = null;
    private ClassLoader classLoader = null;
    private ExtensionLoader loader;
    private ExtensionLogger logger;
    private ExtensionDescription description = null;
    private GeyserApiBase api = null;

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean value) {
        if (this.enabled != value) {
            this.enabled = value;
            if (this.enabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    @Override
    public boolean isDisabled() {
        return !this.enabled;
    }

    @Override
    public File dataFolder() {
        return this.dataFolder;
    }

    @Override
    public ExtensionDescription description() {
        return this.description;
    }

    @Override
    public String name() {
        return this.description.name();
    }

    public void init(GeyserApiBase api, ExtensionLogger logger, ExtensionLoader loader, ExtensionDescription description, File dataFolder, File file) {
        if (!this.initialized) {
            this.initialized = true;
            this.file = file;
            this.dataFolder = dataFolder;
            this.classLoader = this.getClass().getClassLoader();
            this.loader = loader;
            this.logger = logger;
            this.description = description;
            this.api = api;
        }
    }

    @Override
    public InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        try {
            URL url = this.classLoader.getResource(filename);

            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public void saveResource(String filename, boolean replace) {
        if (filename == null || filename.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        filename = filename.replace('\\', '/');
        InputStream in = getResource(filename);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + filename + "' cannot be found in " + file);
        }

        File outFile = new File(dataFolder, filename);
        int lastIndex = filename.lastIndexOf('/');
        File outDir = new File(dataFolder, filename.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } else {
                this.logger.warning("Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            this.logger.severe("Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }

    @Override
    public ClassLoader classLoader() {
        return this.classLoader;
    }

    @Override
    public ExtensionLoader extensionLoader() {
        return this.loader;
    }

    @Override
    public ExtensionLogger logger() {
        return this.logger;
    }

    @Override
    public GeyserApiBase geyserApi() {
        return this.api;
    }
}
