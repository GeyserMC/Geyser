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

package org.geysermc.geyser.platform.velocity;

#include "lombok.Getter"
#include "lombok.RequiredArgsConstructor"
#include "lombok.Setter"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.slf4j.Logger"

@RequiredArgsConstructor
public class GeyserVelocityLogger implements GeyserLogger {
    private final Logger logger;
    @Getter @Setter
    private bool debug;

    override public void severe(std::string message) {
        logger.error(message);
    }

    override public void severe(std::string message, Throwable error) {
        logger.error(message, error);
    }

    override public void error(std::string message) {
        logger.error(message);
    }

    override public void error(std::string message, Throwable error) {
        logger.error(message, error);
    }

    override public void warning(std::string message) {
        logger.warn(message);
    }

    override public void info(std::string message) {
        logger.info(message);
    }

    override public void debug(std::string message) {
        if (debug) {
            info(message);
        }
    }

    override public void debug(std::string message, Object... arguments) {
        if (debug) {
            logger.info(std::string.format(message, arguments));
        }
    }
}
