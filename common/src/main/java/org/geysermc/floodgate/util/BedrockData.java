package org.geysermc.floodgate.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This class contains the raw data send by Geyser to Floodgate or from Floodgate to Floodgate.
 * This class is only used internally, and you should look at FloodgatePlayer instead
 * (FloodgatePlayer is present in the common module in the Floodgate repo)
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class BedrockData {
    public static final int EXPECTED_LENGTH = 9;
    public static final String FLOODGATE_IDENTIFIER = "Geyser-Floodgate";

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

    public BedrockData(String version, String username, String xuid, int deviceOs,
                       String languageCode, int uiProfile, int inputMode, String ip,
                       LinkedPlayer linkedPlayer) {
        this(version, username, xuid, deviceOs, languageCode,
                inputMode, uiProfile, ip, linkedPlayer, EXPECTED_LENGTH);
    }

    public BedrockData(String version, String username, String xuid, int deviceOs,
                       String languageCode, int uiProfile, int inputMode, String ip) {
        this(version, username, xuid, deviceOs, languageCode, uiProfile, inputMode, ip, null);
    }

    public static BedrockData fromString(String data) {
        String[] split = data.split("\0");
        if (split.length != EXPECTED_LENGTH) return emptyData(split.length);

        LinkedPlayer linkedPlayer = LinkedPlayer.fromString(split[8]);
        // The format is the same as the order of the fields in this class
        return new BedrockData(
                split[0], split[1], split[2], Integer.parseInt(split[3]), split[4],
                Integer.parseInt(split[5]), Integer.parseInt(split[6]), split[7],
                linkedPlayer, split.length
        );
    }

    public static BedrockData fromRawData(byte[] data) {
        return fromString(new String(data));
    }

    @Override
    public String toString() {
        // The format is the same as the order of the fields in this class
        return version + '\0' + username + '\0' + xuid + '\0' + deviceOs + '\0' +
                languageCode + '\0' + uiProfile + '\0' + inputMode + '\0' + ip + '\0' +
                (linkedPlayer != null ? linkedPlayer.toString() : "null");
    }

    public boolean hasPlayerLink() {
        return linkedPlayer != null;
    }

    private static BedrockData emptyData(int dataLength) {
        return new BedrockData(null, null, null, -1, null, -1, -1, null, null, dataLength);
    }
}
