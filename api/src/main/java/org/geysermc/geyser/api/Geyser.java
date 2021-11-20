/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api;

import org.geysermc.geyser.api.logger.GeyserLogger;

/**
 * General API class for Geyser.
 */
public abstract class Geyser {
    private static Geyser instance;

    /**
     * Gets the logger used by Geyser.
     *
     * @return the logger used by Geyser
     */
    public abstract GeyserLogger getLogger();

    /**
     * Returns the current {@link Geyser} instance.
     *
     * @return the current Geyser instance
     */
    public static Geyser getInstance() {
        return instance;
    }

    protected static void setInstance(Geyser instance) {
        if (Geyser.instance != null) {
            throw new RuntimeException("Cannot redefine singleton Geyser!");
        }

        Geyser.instance = instance;
    }
}
