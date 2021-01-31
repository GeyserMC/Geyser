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

package org.geysermc.connector.network.session.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import lombok.Getter;
import org.geysermc.connector.skin.SkinProvider;
import org.geysermc.floodgate.util.DeviceOs;
import org.geysermc.floodgate.util.InputMode;
import org.geysermc.floodgate.util.RawSkin;
import org.geysermc.floodgate.util.UiProfile;

import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public final class BedrockClientData {
    @JsonIgnore
    private JsonNode jsonData;

    @JsonProperty(value = "GameVersion")
    private String gameVersion;
    @JsonProperty(value = "ServerAddress")
    private String serverAddress;
    @JsonProperty(value = "ThirdPartyName")
    private String username;
    @JsonProperty(value = "LanguageCode")
    private String languageCode;

    @JsonProperty(value = "SkinId")
    private String skinId;
    @JsonProperty(value = "SkinData")
    private String skinData;
    @JsonProperty(value = "SkinImageHeight")
    private int skinImageHeight;
    @JsonProperty(value = "SkinImageWidth")
    private int skinImageWidth;
    @JsonProperty(value = "CapeId")
    private String capeId;
    @JsonProperty(value = "CapeData")
    private byte[] capeData;
    @JsonProperty(value = "CapeImageHeight")
    private int capeImageHeight;
    @JsonProperty(value = "CapeImageWidth")
    private int capeImageWidth;
    @JsonProperty(value = "CapeOnClassicSkin")
    private boolean capeOnClassicSkin;
    @JsonProperty(value = "SkinResourcePatch")
    private String geometryName;
    @JsonProperty(value = "SkinGeometryData")
    private String geometryData;
    @JsonProperty(value = "PersonaSkin")
    private boolean personaSkin;
    @JsonProperty(value = "PremiumSkin")
    private boolean premiumSkin;

    @JsonProperty(value = "DeviceId")
    private String deviceId;
    @JsonProperty(value = "DeviceModel")
    private String deviceModel;
    @JsonProperty(value = "DeviceOS")
    private DeviceOs deviceOs;
    @JsonProperty(value = "UIProfile")
    private UiProfile uiProfile;
    @JsonProperty(value = "GuiScale")
    private int guiScale;
    @JsonProperty(value = "CurrentInputMode")
    private InputMode currentInputMode;
    @JsonProperty(value = "DefaultInputMode")
    private InputMode defaultInputMode;
    @JsonProperty("PlatformOnlineId")
    private String platformOnlineId;
    @JsonProperty(value = "PlatformOfflineId")
    private String platformOfflineId;
    @JsonProperty(value = "SelfSignedId")
    private UUID selfSignedId;
    @JsonProperty(value = "ClientRandomId")
    private long clientRandomId;

    @JsonProperty(value = "ArmSize")
    private String armSize;
    @JsonProperty(value = "SkinAnimationData")
    private String skinAnimationData;
    @JsonProperty(value = "SkinColor")
    private String skinColor;
    @JsonProperty(value = "ThirdPartyNameOnly")
    private boolean thirdPartyNameOnly;

    private static RawSkin getLegacyImage(byte[] imageData, boolean alex) {
        if (imageData == null) {
            return null;
        }

        // width * height * 4 (rgba)
        switch (imageData.length) {
            case 8192:
                return new RawSkin(64, 32, imageData, alex);
            case 16384:
                return new RawSkin(64, 64, imageData, alex);
            case 32768:
                return new RawSkin(64, 128, imageData, alex);
            case 65536:
                return new RawSkin(128, 128, imageData, alex);
            default:
                throw new IllegalArgumentException("Unknown legacy skin size");
        }
    }

    public void setJsonData(JsonNode data) {
        if (this.jsonData == null && data != null) {
            this.jsonData = data;
        }
    }

    /**
     * Taken from https://github.com/NukkitX/Nukkit/blob/master/src/main/java/cn/nukkit/network/protocol/LoginPacket.java<br>
     * Internally only used for Skins, but can be used for Capes too
     */
    public RawSkin getImage(String name) {
        if (jsonData == null || !jsonData.has(name + "Data")) {
            return null;
        }

        boolean alex = false;
        if (name.equals("Skin")) {
            alex = isAlex();
        }

        byte[] image = Base64.getDecoder().decode(jsonData.get(name + "Data").asText());
        if (jsonData.has(name + "ImageWidth") && jsonData.has(name + "ImageHeight")) {
            return new RawSkin(
                    jsonData.get(name + "ImageWidth").asInt(),
                    jsonData.get(name + "ImageHeight").asInt(),
                    image, alex
            );
        }
        return getLegacyImage(image, alex);
    }

    public RawSkin getAndTransformImage(String name) {
        RawSkin skin = getImage(name);
        if (skin != null && (skin.width > 64 || skin.height > 64)) {
            BufferedImage scaledImage =
                    SkinProvider.imageDataToBufferedImage(skin.data, skin.width, skin.height);

            int max = Math.max(skin.width, skin.height);
            while (max > 64) {
                max /= 2;
                scaledImage = SkinProvider.scale(scaledImage);
            }

            byte[] skinData = SkinProvider.bufferedImageToImageData(scaledImage);
            skin.width = scaledImage.getWidth();
            skin.height = scaledImage.getHeight();
            skin.data = skinData;
        }
        return skin;
    }

    public boolean isAlex() {
        try {
            byte[] bytes = Base64.getDecoder().decode(geometryName.getBytes(Charsets.UTF_8));
            String geometryName =
                    SkinProvider.OBJECT_MAPPER
                            .readTree(bytes)
                            .get("geometry").get("default")
                            .asText();
            return "geometry.humanoid.customSlim".equals(geometryName);
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public DeviceOs getDeviceOs() {
        return deviceOs != null ? deviceOs : DeviceOs.UNKNOWN;
    }

    public InputMode getCurrentInputMode() {
        return currentInputMode != null ? currentInputMode : InputMode.UNKNOWN;
    }

    public InputMode getDefaultInputMode() {
        return defaultInputMode != null ? defaultInputMode : InputMode.UNKNOWN;
    }

    public UiProfile getUiProfile() {
        return uiProfile != null ? uiProfile : UiProfile.CLASSIC;
    }
}
