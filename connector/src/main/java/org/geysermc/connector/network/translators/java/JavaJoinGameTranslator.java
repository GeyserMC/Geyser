/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.java;

import com.github.steveice10.mc.protocol.data.game.entity.player.HandPreference;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientSettingsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.nukkitx.protocol.bedrock.data.PlayerPermission;
import com.nukkitx.protocol.bedrock.packet.AdventureSettingsPacket;
import com.nukkitx.protocol.bedrock.packet.PlayStatusPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.SetPlayerGameTypePacket;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.DimensionUtils;

import java.util.Arrays;
import java.util.List;

@Translator(packet = ServerJoinGamePacket.class)
public class JavaJoinGameTranslator extends PacketTranslator<ServerJoinGamePacket> {

    @Override
    public void translate(ServerJoinGamePacket packet, GeyserSession session) {
        PlayerEntity entity = session.getPlayerEntity();
        entity.setEntityId(packet.getEntityId());
        // If the player is already initialized and a join game packet is sent, they
        // are swapping servers
        if (session.isSpawned()) {
            String fakeDim = entity.getDimension().equals(DimensionUtils.OVERWORLD) ? DimensionUtils.NETHER : DimensionUtils.OVERWORLD;
            DimensionUtils.switchDimension(session, fakeDim);
            DimensionUtils.switchDimension(session, packet.getDimension());

            session.getScoreboardCache().removeScoreboard();
        }

        AdventureSettingsPacket bedrockPacket = new AdventureSettingsPacket();
        bedrockPacket.setUniqueEntityId(session.getPlayerEntity().getGeyserId());
        bedrockPacket.setPlayerPermission(PlayerPermission.MEMBER);
        session.sendUpstreamPacket(bedrockPacket);

        PlayStatusPacket playStatus = new PlayStatusPacket();
        playStatus.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        // session.sendPacket(playStatus);

        SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
        playerGameTypePacket.setGamemode(packet.getGameMode().ordinal());
        session.sendUpstreamPacket(playerGameTypePacket);
        session.setGameMode(packet.getGameMode());

        SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
        entityDataPacket.setRuntimeEntityId(entity.getGeyserId());
        entityDataPacket.getMetadata().putAll(entity.getMetadata());
        session.sendUpstreamPacket(entityDataPacket);

        session.setRenderDistance(packet.getViewDistance());

        // We need to send our skin parts to the server otherwise java sees us with no hat, jacket etc
        String locale = session.getClientData().getLanguageCode();
        List<SkinPart> skinParts = Arrays.asList(SkinPart.values());
        ClientSettingsPacket clientSettingsPacket = new ClientSettingsPacket(locale, (byte) session.getRenderDistance(), ChatVisibility.FULL, true, skinParts, HandPreference.RIGHT_HAND);
        session.sendDownstreamPacket(clientSettingsPacket);

        if (!packet.getDimension().equals(entity.getDimension())) {
            DimensionUtils.switchDimension(session, packet.getDimension());
        }
    }
}
