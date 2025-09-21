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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.data.skin.AnimatedTextureType;
import org.cloudburstmc.protocol.bedrock.data.skin.AnimationData;
import org.cloudburstmc.protocol.bedrock.data.skin.AnimationExpressionType;
import org.cloudburstmc.protocol.bedrock.data.skin.ImageData;
import org.cloudburstmc.protocol.bedrock.data.skin.PersonaPieceData;
import org.cloudburstmc.protocol.bedrock.data.skin.PersonaPieceTintData;
import org.geysermc.floodgate.util.DeviceOs;
import org.geysermc.floodgate.util.InputMode;
import org.geysermc.floodgate.util.UiProfile;

import java.util.List;
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
    private byte[] skinData;
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
    private byte[] geometryName;
    @JsonProperty(value = "SkinGeometryData")
    private byte[] geometryData;
    @JsonProperty(value = "PersonaSkin")
    private boolean personaSkin;
    @JsonProperty(value = "PremiumSkin")
    private boolean premiumSkin;
    @JsonIgnore
    private List<PersonaPieceData> personaPieces;
    @JsonIgnore
    private List<PersonaPieceTintData> personaPieceTint;
    @JsonIgnore
    private List<AnimationData> skinAnimations;
    @JsonProperty(value = "SkinAnimationData")
    private byte[] skinAnimationData;
    @JsonProperty(value = "ArmSize")
    private String armSize;
    @JsonProperty(value = "SkinColor")
    private String skinColor;
    @JsonProperty(value = "SkinGeometryDataEngineVersion")
    private byte[] geometryDataEngineVersion;

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

    @JsonProperty(value = "ThirdPartyNameOnly")
    private boolean thirdPartyNameOnly;
    @JsonProperty(value = "PlayFabId")
    private String playFabId;

    @JsonIgnore
    @Setter
    private String originalString = null;

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

    @JsonProperty(value = "AnimatedImageData")
    private void processAnimationData(List<AnimationDataDTO> animationDataDTO) {
        this.skinAnimations = animationDataDTO.stream().map(animation -> new AnimationData(ImageData.of(animation.imageWidth, animation.ImageHeight, animation.image), animation.textureType(), animation.frames())).toList();
    }

    @JsonProperty(value = "PersonaPieces")
    private void processPersonaPieceData(List<PersonaPieceDataDTO> personaPieceDataDTO) {
        this.personaPieces = personaPieceDataDTO.stream().map(personaPiece -> new PersonaPieceData(personaPiece.id, personaPiece.type, personaPiece.packId, personaPiece.isDefault, personaPiece.productId)).toList();
    }

    @JsonProperty(value = "PieceTintColors")
    private void processPersonaPieceTintData(List<PersonaPieceTintDataDTO> personaPieceTintDataDTO) {
        this.personaPieceTint = personaPieceTintDataDTO.stream().map(personaPieceTint -> new PersonaPieceTintData(personaPieceTint.type, personaPieceTint.colors)).toList();
    }

    private record AnimationDataDTO(@JsonProperty(value = "Type") AnimatedTextureType textureType,
                                    @JsonProperty(value = "Image") byte[] image,
                                    @JsonProperty(value = "ImageWidth") int imageWidth,
                                    @JsonProperty(value = "ImageHeight") int ImageHeight,
                                    @JsonProperty(value = "Frames") float frames,
                                    @JsonProperty(value = "AnimationExpression") AnimationExpressionType expressionType) {}

    private record PersonaPieceDataDTO(@JsonProperty(value = "PieceId") String id,
                                       @JsonProperty(value = "PieceType") String type,
                                       @JsonProperty(value = "PackId") String packId,
                                       @JsonProperty(value = "IsDefault") boolean isDefault,
                                       @JsonProperty(value = "ProductId") String productId) {}

    private record PersonaPieceTintDataDTO(@JsonProperty(value = "PieceType") String type,
                                           @JsonProperty(value = "Colors") List<String> colors) {}
}
