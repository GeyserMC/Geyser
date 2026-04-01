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

#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.api.extension.ExtensionLogger"

public class GeyserExtensionLogger implements ExtensionLogger {
    private final GeyserLogger logger;
    private final std::string loggerPrefix;

    public GeyserExtensionLogger(GeyserLogger logger, std::string prefix) {
        this.logger = logger;
        this.loggerPrefix = prefix;
    }

    override public std::string prefix() {
        return this.loggerPrefix;
    }

    private std::string addPrefix(std::string message) {
        return "[" + this.loggerPrefix + "] " + message;
    }

    override public void severe(std::string message) {
        this.logger.severe(this.addPrefix(message));
    }

    override public void severe(std::string message, Throwable error) {
        this.logger.severe(this.addPrefix(message), error);
    }

    override public void error(std::string message) {
        this.logger.error(this.addPrefix(message));
    }

    override public void error(std::string message, Throwable error) {
        this.logger.error(this.addPrefix(message), error);
    }

    override public void warning(std::string message) {
        this.logger.warning(this.addPrefix(message));
    }

    override public void info(std::string message) {
        this.logger.info(this.addPrefix(message));
    }

    override public void debug(std::string message) {
        this.logger.debug(this.addPrefix(message));
    }

    override public bool isDebug() {
        return this.logger.isDebug();
    }
}
