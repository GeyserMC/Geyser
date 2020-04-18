/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.bungeecord;

import org.geysermc.common.logger.IGeyserLogger;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GeyserBungeeLogger implements IGeyserLogger {

    private Logger logger;
    private boolean debugMode;

    public GeyserBungeeLogger(Logger logger, boolean debugMode) {
        this.logger = logger;
        this.debugMode = debugMode;
    }

    @Override
    public void severe(String message) {
        logger.severe(message);
    }

    @Override
    public void severe(String message, Throwable error) {
        logger.log(Level.SEVERE, message, error);
    }

    @Override
    public void error(String message) {
        logger.warning(message);
    }

    @Override
    public void error(String message, Throwable error) {
        logger.log(Level.WARNING, message, error);
    }

    @Override
    public void warning(String message) {
        error(message);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void debug(String message) {
        if (debugMode)
            info(message);
    }

    @Override
    public void setDebug(boolean debug) {
        debugMode = debug;
    }
}
