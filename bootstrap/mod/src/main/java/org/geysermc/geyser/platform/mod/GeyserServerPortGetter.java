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

package org.geysermc.geyser.platform.mod;

import net.minecraft.server.MinecraftServer;

/**
 * Represents a getter to the server port in the dedicated server and in the integrated server.
 */
public interface GeyserServerPortGetter {
    /**
     * Returns the server port.
     *
     * <ul>
     *     <li>If it's a dedicated server, it will return the server port specified in the {@code server.properties} file.</li>
     *     <li>If it's an integrated server, it will return the LAN port if opened, else -1.</li>
     * </ul>
     *
     * The reason is that {@link MinecraftServer#getPort()} doesn't return the LAN port if it's the integrated server,
     * and changing the behavior of this method via a mixin should be avoided as it could have unexpected consequences.
     *
     * @return The server port.
     */
    int geyser$getServerPort();
}
