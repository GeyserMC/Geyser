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

package org.geysermc.floodgate.util;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum DeviceOS {

    @JsonEnumDefaultValue
    UNKNOWN("Unknown"),
    ANDROID("Android"),
    IOS("iOS"),
    OSX("macOS"),
    FIREOS("FireOS"),
    GEARVR("Gear VR"),
    HOLOLENS("Hololens"),
    WIN10("Windows 10"),
    WIN32("Windows"),
    DEDICATED("Dedicated"),
    ORBIS("PS4"),
    NX("Switch"),
    SWITCH("Switch"),
    XBOX_ONE("Xbox One"),
    WIN_PHONE("Windows Phone");

    private static final DeviceOS[] VALUES = values();

    private final String displayName;

    DeviceOS(final String displayName) {
        this.displayName = displayName;
    }

    public static DeviceOS getById(int id) {
        return id < VALUES.length ? VALUES[id] : VALUES[0];
    }

    /**
     * @return friendly display name of platform.
     */
    @Override
    public String toString() {
        return displayName;
    }
}