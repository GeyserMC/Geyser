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

package org.geysermc.geyser.extension;

import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.extension.ExtensionLogger;

public class GeyserExtensionLogger implements ExtensionLogger {
    private final GeyserLogger logger;
    private final String loggerPrefix;

    public GeyserExtensionLogger(GeyserLogger logger, String prefix) {
        this.logger = logger;
        this.loggerPrefix = prefix;
    }

    @Override
    public String prefix() {
        return this.loggerPrefix;
    }

    private String addPrefix(String message) {
        return "[" + this.loggerPrefix + "] " + message;
    }

    @Override
    public void severe(String message) {
        this.logger.severe(this.addPrefix(message));
    }

    @Override
    public void severe(String message, Throwable error) {
        this.logger.severe(this.addPrefix(message), error);
    }

    @Override
    public void error(String message) {
        this.logger.error(this.addPrefix(message));
    }

    @Override
    public void error(String message, Throwable error) {
        this.logger.error(this.addPrefix(message), error);
    }

    @Override
    public void warning(String message) {
        this.logger.warning(this.addPrefix(message));
    }

    @Override
    public void info(String message) {
        this.logger.info(this.addPrefix(message));
    }

    @Override
    public void debug(String message) {
        this.logger.debug(this.addPrefix(message));
    }

    @Override
    public boolean isDebug() {
        return this.logger.isDebug();
    }
}
