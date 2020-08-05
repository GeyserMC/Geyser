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

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class BedrockData {
    public static final int EXPECTED_LENGTH = 7;
    public static final String FLOODGATE_IDENTIFIER = "Geyser-Floodgate";

    private String version;
    private String username;
    private String xuid;
    private int deviceId;
    private String languageCode;
    private int inputMode;
    private String ip;
    private int dataLength;

    public BedrockData(String version, String username, String xuid, int deviceId, String languageCode, int inputMode, String ip) {
        this(version, username, xuid, deviceId, languageCode, inputMode, ip, EXPECTED_LENGTH);
    }

    public static BedrockData fromString(String data) {
        String[] split = data.split("\0");
        if (split.length != EXPECTED_LENGTH) return null;

        return new BedrockData(
                split[0], split[1], split[2], Integer.parseInt(split[3]),
                split[4], Integer.parseInt(split[5]), split[6], split.length
        );
    }

    public static BedrockData fromRawData(byte[] data) {
        return fromString(new String(data));
    }

    @Override
    public String toString() {
        return version +'\0'+ username +'\0'+ xuid +'\0'+ deviceId +'\0'+ languageCode +'\0'+
                inputMode +'\0'+ ip;
    }
}
