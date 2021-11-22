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

package org.geysermc.geyser.translator.protocol.java;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import org.geysermc.geyser.session.auth.AuthType;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.skin.SkinManager;

@Translator(packet = ClientboundGameProfilePacket.class)
public class JavaGameProfileTranslator extends PacketTranslator<ClientboundGameProfilePacket> {

    @Override
    public void translate(GeyserSession session, ClientboundGameProfilePacket packet) {
        PlayerEntity playerEntity = session.getPlayerEntity();
        AuthType remoteAuthType = session.getRemoteAuthType();

        // Required, or else Floodgate players break with Spigot chunk caching
        GameProfile profile = packet.getProfile();
        playerEntity.setUsername(profile.getName());
        playerEntity.setUuid(profile.getId());

        session.getGeyser().getSessionManager().addSession(playerEntity.getUuid(), session);

        // Check if they are not using a linked account
        if (remoteAuthType == AuthType.OFFLINE || playerEntity.getUuid().getMostSignificantBits() == 0) {
            SkinManager.handleBedrockSkin(playerEntity, session.getClientData());
        }

        if (remoteAuthType == AuthType.FLOODGATE) {
            // We'll send the skin upload a bit after the handshake packet (aka this packet),
            // because otherwise the global server returns the data too fast.
            session.getAuthData().upload(session.getGeyser());
        }
    }
}
