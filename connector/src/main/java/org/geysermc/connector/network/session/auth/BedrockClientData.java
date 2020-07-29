package org.geysermc.connector.network.session.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.geysermc.floodgate.util.DeviceOs;
import org.geysermc.floodgate.util.InputMode;
import org.geysermc.floodgate.util.UiProfile;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public final class BedrockClientData {
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
    private DeviceOs deviceOS;
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
}
