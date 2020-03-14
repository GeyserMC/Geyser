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
