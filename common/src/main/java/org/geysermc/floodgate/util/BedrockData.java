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

package org.geysermc.floodgate.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.time.TimeSyncer;

/**
 * This class contains the raw data send by Geyser to Floodgate or from Floodgate to Floodgate. This
 * class is only used internally, and you should look at FloodgatePlayer instead (FloodgatePlayer is
 * present in the API module of the Floodgate repo)
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class BedrockData implements Cloneable {
    public static final int EXPECTED_LENGTH = 13;

    private final String version;
    private final String username;
    private final String xuid;
    private final int deviceOs;
    private final String languageCode;
    private final int uiProfile;
    private final int inputMode;
    private final String ip;
    private final LinkedPlayer linkedPlayer;
    private final boolean fromProxy;

    private final int subscribeId;
    private final String verifyCode;

    private final long timestamp;
    private final int dataLength;

    public static BedrockData of(
            String version, String username, String xuid, int deviceOs,
            String languageCode, int uiProfile, int inputMode, String ip,
            LinkedPlayer linkedPlayer, boolean fromProxy, int subscribeId,
            String verifyCode, TimeSyncer timeSyncer) {
        return new BedrockData(version, username, xuid, deviceOs, languageCode, inputMode,
                uiProfile, ip, linkedPlayer, fromProxy, subscribeId, verifyCode,
                timeSyncer.getRealMillis(), EXPECTED_LENGTH);
    }

    public static BedrockData of(
            String version, String username, String xuid, int deviceOs,
            String languageCode, int uiProfile, int inputMode, String ip,
            int subscribeId, String verifyCode, TimeSyncer timeSyncer) {
        return of(version, username, xuid, deviceOs, languageCode, uiProfile, inputMode, ip, null,
                false, subscribeId, verifyCode, timeSyncer);
    }

    public static BedrockData fromString(String data) {
        String[] split = data.split("\0");
        if (split.length != EXPECTED_LENGTH) {
            return emptyData(split.length);
        }

        LinkedPlayer linkedPlayer = LinkedPlayer.fromString(split[8]);
        // The format is the same as the order of the fields in this class
        return new BedrockData(
                split[0], split[1], split[2], Integer.parseInt(split[3]), split[4],
                Integer.parseInt(split[5]), Integer.parseInt(split[6]), split[7], linkedPlayer,
                "1".equals(split[9]), Integer.parseInt(split[10]), split[11], Long.parseLong(split[12]), split.length
        );
    }

    private static BedrockData emptyData(int dataLength) {
        return new BedrockData(null, null, null, -1, null, -1, -1, null, null, false, -1, null, -1,
                dataLength);
    }

    public boolean hasPlayerLink() {
        return linkedPlayer != null;
    }

    @Override
    public String toString() {
        // The format is the same as the order of the fields in this class
        return version + '\0' + username + '\0' + xuid + '\0' + deviceOs + '\0' +
                languageCode + '\0' + uiProfile + '\0' + inputMode + '\0' + ip + '\0' +
                (linkedPlayer != null ? linkedPlayer.toString() : "null") + '\0' +
                (fromProxy ? 1 : 0) + '\0' + subscribeId + '\0' + verifyCode + '\0' + timestamp;
    }

    @Override
    public BedrockData clone() throws CloneNotSupportedException {
        return (BedrockData) super.clone();
    }
}
