/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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
package org.geysermc.geyser.platform.viaproxy;

import net.raphimc.viaproxy.cli.ConsoleFormatter;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.command.GeyserCommandSource;

public class GeyserViaProxyLogger implements GeyserLogger, GeyserCommandSource {

    @Override
    public void severe(String message) {
        GeyserViaProxyPlugin.LOGGER.fatal(ConsoleFormatter.convert(message));
    }

    @Override
    public void severe(String message, Throwable error) {
        GeyserViaProxyPlugin.LOGGER.fatal(ConsoleFormatter.convert(message), error);
    }

    @Override
    public void error(String message) {
        GeyserViaProxyPlugin.LOGGER.error(ConsoleFormatter.convert(message));
    }

    @Override
    public void error(String message, Throwable error) {
        GeyserViaProxyPlugin.LOGGER.error(ConsoleFormatter.convert(message), error);
    }

    @Override
    public void warning(String message) {
        GeyserViaProxyPlugin.LOGGER.warn(ConsoleFormatter.convert(message));
    }

    @Override
    public void info(String message) {
        GeyserViaProxyPlugin.LOGGER.info(ConsoleFormatter.convert(message));
    }

    @Override
    public void debug(String message) {
        GeyserViaProxyPlugin.LOGGER.debug(ConsoleFormatter.convert(message));
    }

    @Override
    public void setDebug(boolean debug) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDebug() {
        return false;
    }

}
