/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.event.lifecycle;

import org.geysermc.event.Event;

import java.nio.file.Path;
import java.util.List;

/**
 * Called once on Geyser's startup, before custom mappings are read, to add mappings files
 * from outside the custom_mappings folder. They are read the same way as the files in that folder.
 * <p>
 * Like that folder, these files are only read on startup; a reload does not read them again.
 * @since 2.12.1
 */
public abstract class GeyserDefineCustomMappingsEvent implements Event {

    /**
     * Gets the mappings files registered so far, including those from the "custom-mappings-urls" config option.
     *
     * @return an unmodifiable list of the registered mappings files
     * @since 2.12.1
     */
    public abstract List<Path> mappingsFiles();

    /**
     * Registers a mappings file.
     *
     * @param file the mappings file
     * @since 2.12.1
     */
    public abstract void register(Path file);

    /**
     * Downloads a mappings file and registers it. If the download fails, or does not contain a mappings file,
     * the copy from the last successful download of this url is registered instead, if there is one.
     *
     * @param url the url of the mappings file
     * @return whether a mappings file was registered
     * @since 2.12.1
     */
    public abstract boolean registerUrl(String url);
}
