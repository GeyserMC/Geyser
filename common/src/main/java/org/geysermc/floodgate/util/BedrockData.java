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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This class contains the raw data send by Geyser to Floodgate or from Floodgate to Floodgate.
 * This class is only used internally, and you should look at FloodgatePlayer instead
 * (FloodgatePlayer is present in the common module in the Floodgate repo)
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public final class BedrockData {
    public static final int EXPECTED_LENGTH = 9;

    private final String version;
    private final String username;
    private final String xuid;
    private final int deviceOs;
    private final String languageCode;
    private final int uiProfile;
    private final int inputMode;
    private final String ip;
    private final LinkedPlayer linkedPlayer;
    private final int dataLength;

    private RawSkin skin;

    public BedrockData(String version, String username, String xuid, int deviceOs,
                       String languageCode, int uiProfile, int inputMode, String ip,
                       LinkedPlayer linkedPlayer, RawSkin skin) {
        this(version, username, xuid, deviceOs, languageCode,
                inputMode, uiProfile, ip, linkedPlayer, EXPECTED_LENGTH, skin);
    }

    public BedrockData(String version, String username, String xuid, int deviceOs,
                       String languageCode, int uiProfile, int inputMode, String ip, RawSkin skin) {
        this(version, username, xuid, deviceOs, languageCode, uiProfile, inputMode, ip, null, skin);
    }

    public boolean hasPlayerLink() {
        return linkedPlayer != null;
    }

    public static BedrockData fromString(String data, String skin) {
        String[] split = data.split("\0");
        if (split.length != EXPECTED_LENGTH) {
            return emptyData(split.length);
        }

        LinkedPlayer linkedPlayer = LinkedPlayer.fromString(split[8]);
        // The format is the same as the order of the fields in this class
        return new BedrockData(
                split[0], split[1], split[2], Integer.parseInt(split[3]), split[4],
                Integer.parseInt(split[5]), Integer.parseInt(split[6]), split[7],
                linkedPlayer, split.length, RawSkin.parse(skin)
        );
    }

    public static BedrockData fromRawData(byte[] data, String skin) {
        return fromString(new String(data), skin);
    }

    @Override
    public String toString() {
        // The format is the same as the order of the fields in this class
        return version + '\0' + username + '\0' + xuid + '\0' + deviceOs + '\0' +
                languageCode + '\0' + uiProfile + '\0' + inputMode + '\0' + ip + '\0' +
                (linkedPlayer != null ? linkedPlayer.toString() : "null");
    }

    private static BedrockData emptyData(int dataLength) {
        return new BedrockData(null, null, null, -1, null, -1, -1, null, null, dataLength, null);
    }
}
