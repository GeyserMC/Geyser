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

package org.geysermc.geyser.api.util;

/**
 * Represents the platform Geyser is running on.
 */
public record PlatformType(String platformName) {

    @Deprecated
    public static final PlatformType ANDROID = new PlatformType("Android");
    public static final PlatformType BUNGEECORD = new PlatformType("BungeeCord");
    public static final PlatformType FABRIC = new PlatformType("Fabric");
    public static final PlatformType NEOFORGE = new PlatformType("NeoForge");
    public static final PlatformType SPIGOT = new PlatformType("Spigot");

    @Deprecated
    public static final PlatformType SPONGE = new PlatformType("Sponge");
    public static final PlatformType STANDALONE = new PlatformType("Standalone");
    public static final PlatformType VELOCITY = new PlatformType("Velocity");
    public static final PlatformType VIAPROXY = new PlatformType("ViaProxy");
}
