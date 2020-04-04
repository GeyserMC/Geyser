package org.geysermc.connector.network.session.auth;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.geysermc.floodgate.util.DeviceOS;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class BedrockClientData {
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
    @JsonProperty(value = "CapeId")
    private String capeId;
    @JsonProperty(value = "CapeData")
    private byte[] capeData;
    @JsonProperty(value = "CapeOnClassicSkin")
    private boolean capeOnClassicSkin;
    @JsonProperty(value = "SkinResourcePatch")
    private String geometryName;
    @JsonProperty(value = "SkinGeometryData")
    private String geometryData;
    @JsonProperty(value = "PremiumSkin")
    private boolean premiumSkin;

    @JsonProperty(value = "DeviceId")
    private String deviceId;
    @JsonProperty(value = "DeviceModel")
    private String deviceModel;
    @JsonProperty(value = "DeviceOS")
    private DeviceOS deviceOS;
    @JsonProperty(value = "UIProfile")
    private UIProfile uiProfile;
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

    public enum UIProfile {
        @JsonEnumDefaultValue
        CLASSIC,
        POCKET
    }

    public enum InputMode {
        @JsonEnumDefaultValue
        UNKNOWN,
        KEYBOARD_MOUSE,
        TOUCH, // I guess Touch?
        CONTROLLER,
        VR
    }
}
