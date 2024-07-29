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

package org.geysermc.geyser.translator.protocol.java;

import net.kyori.adventure.key.Key;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.packet.GameRulesChangedPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetPlayerGameTypePacket;
import org.geysermc.erosion.Constants;
import org.geysermc.floodgate.pluginmessage.PluginMessageChannels;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.erosion.GeyserboundHandshakePacketHandler;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Translator(packet = ClientboundLoginPacket.class)
public class JavaLoginTranslator extends PacketTranslator<ClientboundLoginPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundLoginPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();
        entity.setEntityId(packet.getEntityId());

        if (session.getErosionHandler().isActive()) {
            session.getErosionHandler().close();
            session.setErosionHandler(new GeyserboundHandshakePacketHandler(session));
        }

        PlayerSpawnInfo spawnInfo = packet.getCommonPlayerSpawnInfo();
        JavaDimension newDimension = session.getRegistryCache().dimensions().byId(spawnInfo.getDimension());
        boolean forceDimSwitch = false;

        // If the player is already initialized and a join game packet is sent, they
        // are swapping servers
        if (session.isSpawned()) {
            int fakeDim = DimensionUtils.getTemporaryDimension(session.getDimensionType().bedrockId(), newDimension.bedrockId());
            DimensionUtils.fastSwitchDimension(session, fakeDim);
            forceDimSwitch = true;

            session.getWorldCache().removeScoreboard();

            // Remove all bossbars
            session.getEntityCache().removeAllBossBars();
            // Remove extra hearts, hunger, etc.
            entity.resetAttributes();
            entity.resetMetadata();
        } else if (session.getUpstream().isInitialized()) {
            if (newDimension.bedrockId() == 0) {
                // A dimension switch will not happen, so make sure we initialized the dimension choice.
                // Otherwise, the dimension switch will fill these values in.
                session.setDimensionType(newDimension);
                DimensionUtils.setBedrockDimension(session, newDimension.bedrockId());
            }
        }

        session.setWorldName(spawnInfo.getWorldName());
        session.setLevels(Arrays.stream(packet.getWorldNames()).map(Key::asString).toArray(String[]::new));
        session.setGameMode(spawnInfo.getGameMode());

        boolean needsSpawnPacket = !session.isSentSpawnPacket();
        if (needsSpawnPacket) {
            // The player has yet to spawn so let's do that using some of the information in this Java packet
            session.setDimensionType(newDimension);
            DimensionUtils.setBedrockDimension(session, newDimension.bedrockId());
            session.connect();

            // It is now safe to send these packets
            session.getUpstream().sendPostStartGamePackets();
        } else {
            SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
            playerGameTypePacket.setGamemode(EntityUtils.toBedrockGamemode(spawnInfo.getGameMode()).ordinal());
            session.sendUpstreamPacket(playerGameTypePacket);
        }

        entity.setLastDeathPosition(spawnInfo.getLastDeathPos());

        entity.updateBedrockMetadata();

        // Send if client should show respawn screen
        GameRulesChangedPacket gamerulePacket = new GameRulesChangedPacket();
        gamerulePacket.getGameRules().add(new GameRuleData<>("doimmediaterespawn", !packet.isEnableRespawnScreen()));
        session.sendUpstreamPacket(gamerulePacket);

        session.setReducedDebugInfo(packet.isReducedDebugInfo());

        session.setServerRenderDistance(packet.getViewDistance());

        // send this again now that we know the server render distance
        // as the bedrock client isn't required to send a render distance
        session.sendJavaClientSettings();

        Key register = MinecraftKey.key("register");
        if (session.remoteServer().authType() == AuthType.FLOODGATE) {
            session.sendDownstreamPacket(new ServerboundCustomPayloadPacket(register, PluginMessageChannels.getFloodgateRegisterData()));
        }
        session.sendDownstreamPacket(new ServerboundCustomPayloadPacket(register, Constants.PLUGIN_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        if (newDimension != session.getDimensionType() || forceDimSwitch) {
            DimensionUtils.switchDimension(session, newDimension);
        } else if (DimensionUtils.isCustomBedrockNetherId() && newDimension.isNetherLike()) {
            // If the player is spawning into the "fake" nether, send them some fog
            session.camera().sendFog(DimensionUtils.BEDROCK_FOG_HELL);
        }

        ChunkUtils.loadDimension(session);
    }
}
