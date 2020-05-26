/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.edition.mcee.shims;

import net.minidev.json.JSONValue;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.auth.BedrockClientData;
import org.geysermc.connector.utils.SkinProvider;
import org.geysermc.connector.utils.SkinUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SkinUtilsShim implements SkinUtils.Shim {

    @Override
    public void handleBedrockSkin(PlayerEntity playerEntity, BedrockClientData clientData) {
        SkinUtils.GameProfileData data = SkinUtils.GameProfileData.from(playerEntity.getProfile());

        GeyserConnector.getInstance().getLogger().info("Registering bedrock skin for " + playerEntity.getUsername() + " (" + playerEntity.getUuid() + ")");

        try {
            byte[] skinBytes = Base64.getDecoder().decode(clientData.getSkinData().getBytes(StandardCharsets.UTF_8));
            byte[] capeBytes = clientData.getCapeData();

            String geometryName = "{\"geometry\" : {\"default\" : \"" + JSONValue.escape(new String(clientData.getLegacyGeometryName().getBytes())) + "\"}}";

            byte[] geometryBytes = Base64.getDecoder().decode(clientData.getLegacyGeometryData().getBytes(StandardCharsets.UTF_8));

            if (skinBytes.length <= (128 * 128 * 4) && !clientData.isPersonaSkin()) {
                SkinProvider.storeBedrockSkin(playerEntity.getUuid(), data.getSkinUrl(), skinBytes);
                SkinProvider.storeBedrockGeometry(playerEntity.getUuid(), geometryName.getBytes(), geometryBytes);
            } else {
                GeyserConnector.getInstance().getLogger().info("Unable to load bedrock skin for '" + playerEntity.getUsername() + "' as they are likely using a customised skin");
                GeyserConnector.getInstance().getLogger().debug("The size of '" + playerEntity.getUsername() + "' skin is: " + clientData.getSkinImageWidth() + "x" + clientData.getSkinImageHeight());
            }

            if (capeBytes.length != 0) {
                SkinProvider.storeBedrockCape(playerEntity.getUuid(), capeBytes);
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to cache skin for bedrock user (" + playerEntity.getUsername() + "): ", e);
        }
    }
}
