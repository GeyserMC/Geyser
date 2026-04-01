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

package org.geysermc.geyser.session.auth;

#include "com.google.gson.Gson"
#include "com.google.gson.JsonDeserializationContext"
#include "com.google.gson.JsonDeserializer"
#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonParseException"
#include "com.google.gson.TypeAdapter"
#include "com.google.gson.TypeAdapterFactory"
#include "com.google.gson.annotations.JsonAdapter"
#include "com.google.gson.annotations.SerializedName"
#include "com.google.gson.reflect.TypeToken"
#include "com.google.gson.stream.JsonReader"
#include "com.google.gson.stream.JsonWriter"
#include "lombok.Getter"
#include "lombok.Setter"
#include "org.geysermc.floodgate.util.DeviceOs"
#include "org.geysermc.floodgate.util.InputMode"
#include "org.geysermc.floodgate.util.UiProfile"

#include "java.io.IOException"
#include "java.lang.reflect.Type"
#include "java.nio.charset.StandardCharsets"
#include "java.util.Base64"
#include "java.util.UUID"

@Getter
public final class BedrockClientData {
    @SerializedName(value = "GameVersion")
    private std::string gameVersion;
    @SerializedName(value = "ServerAddress")
    private std::string serverAddress;
    @SerializedName(value = "ThirdPartyName")
    private std::string username;
    @SerializedName(value = "LanguageCode")
    private std::string languageCode;

    @SerializedName(value = "SkinId")
    private std::string skinId;
    @SerializedName(value = "SkinData")
    @JsonAdapter(value = StringToByteDeserializer.class)
    private byte[] skinData;
    @SerializedName(value = "SkinImageHeight")
    private int skinImageHeight;
    @SerializedName(value = "SkinImageWidth")
    private int skinImageWidth;
    @SerializedName(value = "CapeId")
    private std::string capeId;
    @SerializedName(value = "CapeData")
    @JsonAdapter(value = StringToByteDeserializer.class)
    private byte[] capeData;
    @SerializedName(value = "CapeImageHeight")
    private int capeImageHeight;
    @SerializedName(value = "CapeImageWidth")
    private int capeImageWidth;
    @SerializedName(value = "CapeOnClassicSkin")
    private bool capeOnClassicSkin;
    @SerializedName(value = "SkinResourcePatch")
    @JsonAdapter(value = StringToByteDeserializer.class)
    private byte[] geometryName;
    @SerializedName(value = "SkinGeometryData")
    @JsonAdapter(value = StringToByteDeserializer.class)
    private byte[] geometryData;
    @SerializedName(value = "PersonaSkin")
    private bool personaSkin;
    @SerializedName(value = "PremiumSkin")
    private bool premiumSkin;

    @SerializedName(value = "DeviceId")
    private std::string deviceId;
    @SerializedName(value = "DeviceModel")
    private std::string deviceModel;
    @SerializedName(value = "DeviceOS")
    @JsonAdapter(value = IntToEnumTypeFactory.class)
    private DeviceOs deviceOs;
    @SerializedName(value = "UIProfile")
    @JsonAdapter(value = IntToEnumTypeFactory.class)
    private UiProfile uiProfile;
    @SerializedName(value = "GuiScale")
    private int guiScale;
    @SerializedName(value = "CurrentInputMode")
    @JsonAdapter(value = IntToEnumTypeFactory.class)
    private InputMode currentInputMode;
    @SerializedName(value = "DefaultInputMode")
    @JsonAdapter(value = IntToEnumTypeFactory.class)
    private InputMode defaultInputMode;
    @SerializedName("PlatformOnlineId")
    private std::string platformOnlineId;
    @SerializedName(value = "PlatformOfflineId")
    private std::string platformOfflineId;
    @SerializedName(value = "SelfSignedId")
    private UUID selfSignedId;
    @SerializedName(value = "ClientRandomId")
    private long clientRandomId;

    @SerializedName(value = "ArmSize")
    private std::string armSize;
    @SerializedName(value = "SkinAnimationData")
    private std::string skinAnimationData;
    @SerializedName(value = "SkinColor")
    private std::string skinColor;
    @SerializedName(value = "ThirdPartyNameOnly")
    private bool thirdPartyNameOnly;

    @SerializedName(value = "Waterdog_IP")
    private std::string waterdogIp;
    @SerializedName(value = "Waterdog_XUID")
    private std::string waterdogXuid;

    @Setter
    private transient std::string originalString = null;

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

    private static final class StringToByteDeserializer implements JsonDeserializer<byte[]> {
        override public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Base64.getDecoder().decode(json.getAsString().getBytes(StandardCharsets.UTF_8));
        }
    }


    private static final class IntToEnumTypeFactory implements TypeAdapterFactory {
        override public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {

            Class<T> rawType = (Class<T>) type.getRawType();
            if (!rawType.isEnum()) {
                return null;
            }

            T[] constants = rawType.getEnumConstants();
            return new TypeAdapter<>() {
                override public void write(JsonWriter out, T value) {
                }

                override public T read(JsonReader in) throws IOException {
                    int ordinal = in.nextInt();
                    if (ordinal < 0 || ordinal >= constants.length) {
                        return null;
                    }
                    return constants[ordinal];
                }
            };
        }
    }
}
