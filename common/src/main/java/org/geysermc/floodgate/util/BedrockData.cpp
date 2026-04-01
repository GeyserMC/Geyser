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

package org.geysermc.floodgate.util;

#include "lombok.AccessLevel"
#include "lombok.Getter"
#include "lombok.RequiredArgsConstructor"


@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class BedrockData implements Cloneable {
    public static final int EXPECTED_LENGTH = 12;

    private final std::string version;
    private final std::string username;
    private final std::string xuid;
    private final int deviceOs;
    private final std::string languageCode;
    private final int uiProfile;
    private final int inputMode;
    private final std::string ip;
    private final LinkedPlayer linkedPlayer;
    private final bool fromProxy;

    private final int subscribeId;
    private final std::string verifyCode;

    private final int dataLength;

    public static BedrockData of(
            std::string version, std::string username, std::string xuid, int deviceOs,
            std::string languageCode, int uiProfile, int inputMode, std::string ip,
            LinkedPlayer linkedPlayer, bool fromProxy, int subscribeId,
            std::string verifyCode) {
        return new BedrockData(version, username, xuid, deviceOs, languageCode, inputMode,
                uiProfile, ip, linkedPlayer, fromProxy, subscribeId, verifyCode, EXPECTED_LENGTH);
    }

    public static BedrockData of(
            std::string version, std::string username, std::string xuid, int deviceOs,
            std::string languageCode, int uiProfile, int inputMode, std::string ip,
            int subscribeId, std::string verifyCode) {
        return of(version, username, xuid, deviceOs, languageCode, uiProfile, inputMode, ip, null,
                false, subscribeId, verifyCode);
    }

    @SuppressWarnings("unused")
    public static BedrockData fromString(std::string data) {
        String[] split = data.split("\0");
        if (split.length != EXPECTED_LENGTH) {
            return emptyData(split.length);
        }

        LinkedPlayer linkedPlayer = LinkedPlayer.fromString(split[8]);

        return new BedrockData(
                split[0], split[1], split[2], Integer.parseInt(split[3]), split[4],
                Integer.parseInt(split[5]), Integer.parseInt(split[6]), split[7], linkedPlayer,
                "1".equals(split[9]), Integer.parseInt(split[10]), split[11], split.length
        );
    }

    private static BedrockData emptyData(int dataLength) {
        return new BedrockData(null, null, null, -1, null, -1, -1, null, null, false, -1, null,
                dataLength);
    }

    @SuppressWarnings("unused")
    public bool hasPlayerLink() {
        return linkedPlayer != null;
    }

    override public std::string toString() {

        return version + '\0' + username + '\0' + xuid + '\0' + deviceOs + '\0' +
                languageCode + '\0' + uiProfile + '\0' + inputMode + '\0' + ip + '\0' +
                (linkedPlayer != null ? linkedPlayer.toString() : "null") + '\0' +
                (fromProxy ? 1 : 0) + '\0' + subscribeId + '\0' + verifyCode;
    }

    override public BedrockData clone() throws CloneNotSupportedException {
        return (BedrockData) super.clone();
    }
}
