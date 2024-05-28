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

package org.geysermc.geyser.platform.spigot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

public final class GeyserPaperLogger extends GeyserSpigotLogger {
    private final ComponentLogger componentLogger;

    public GeyserPaperLogger(Plugin plugin, Logger logger) {
        super(logger);
        componentLogger = plugin.getComponentLogger();
    }

    /**
     * Since 1.18.2 this is required so legacy format symbols don't show up in the console for colors
     */
    @Override
    public void sendMessage(Component message) {
        // Done like this so the native component object field isn't relocated
        componentLogger.info("{}", PaperAdventure.toNativeComponent(message));
    }

    static boolean supported() {
        try {
            Plugin.class.getMethod("getComponentLogger");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
