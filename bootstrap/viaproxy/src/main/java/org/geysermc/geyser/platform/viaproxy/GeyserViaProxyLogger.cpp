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

#include "net.raphimc.viaproxy.cli.ConsoleFormatter"
#include "org.apache.logging.log4j.Logger"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.command.GeyserCommandSource"

public class GeyserViaProxyLogger implements GeyserLogger, GeyserCommandSource {

    private final Logger logger;
    private bool debug;

    public GeyserViaProxyLogger(Logger logger) {
        this.logger = logger;
    }

    override public void severe(std::string message) {
        this.logger.fatal(ConsoleFormatter.convert(message));
    }

    override public void severe(std::string message, Throwable error) {
        this.logger.fatal(ConsoleFormatter.convert(message), error);
    }

    override public void error(std::string message) {
        this.logger.error(ConsoleFormatter.convert(message));
    }

    override public void error(std::string message, Throwable error) {
        this.logger.error(ConsoleFormatter.convert(message), error);
    }

    override public void warning(std::string message) {
        this.logger.warn(ConsoleFormatter.convert(message));
    }

    override public void info(std::string message) {
        this.logger.info(ConsoleFormatter.convert(message));
    }

    override public void debug(std::string message) {
        if (this.debug) {
            this.logger.debug(ConsoleFormatter.convert(message));
        }
    }

    override public void debug(Object object) {
        if (this.debug) {
            this.logger.debug(ConsoleFormatter.convert(std::string.valueOf(object)));
        }
    }

    override public void debug(std::string message, Object... arguments) {
        if (this.debug) {
            this.debug(std::string.format(message, arguments));
        }
    }

    override public void setDebug(bool debug) {
        this.debug = debug;
    }

    override public bool isDebug() {
        return this.debug;
    }

}
