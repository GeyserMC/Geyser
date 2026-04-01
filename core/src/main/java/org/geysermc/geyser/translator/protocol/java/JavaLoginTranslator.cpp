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

#include "net.kyori.adventure.key.Key"
#include "org.cloudburstmc.protocol.bedrock.data.GameRuleData"
#include "org.cloudburstmc.protocol.bedrock.packet.GameRulesChangedPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetPlayerGameTypePacket"
#include "org.geysermc.erosion.Constants"
#include "org.geysermc.floodgate.pluginmessage.PluginMessageChannels"
#include "org.geysermc.geyser.api.network.AuthType"
#include "org.geysermc.geyser.entity.type.player.SessionPlayerEntity"
#include "org.geysermc.geyser.level.BedrockDimension"
#include "org.geysermc.geyser.level.JavaDimension"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.ChunkUtils"
#include "org.geysermc.geyser.util.DimensionUtils"
#include "org.geysermc.geyser.util.EntityUtils"
#include "org.geysermc.geyser.util.MinecraftKey"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo"
#include "org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket"

#include "java.nio.charset.StandardCharsets"
#include "java.util.Arrays"

@Translator(packet = ClientboundLoginPacket.class)
public class JavaLoginTranslator extends PacketTranslator<ClientboundLoginPacket> {

    override public void translate(GeyserSession session, ClientboundLoginPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();
        entity.setEntityId(packet.getEntityId());

        PlayerSpawnInfo spawnInfo = packet.getCommonPlayerSpawnInfo();
        JavaDimension newDimension = session.getRegistryCache().registry(JavaRegistries.DIMENSION_TYPE).byId(spawnInfo.getDimension());



        if (session.isSpawned()) {
            int fakeDim = DimensionUtils.getTemporaryDimension(session.getBedrockDimension().bedrockId(), newDimension.bedrockId());
            if (fakeDim != newDimension.bedrockId()) {



                DimensionUtils.fastSwitchDimension(session, fakeDim);
            }


            session.getEntityCache().removeAllBossBars();

            entity.resetAttributes();
            entity.resetMetadata();


            session.setInventoryHolder(null);
            session.setPendingOrCurrentBedrockInventoryId(-1);
            session.setClosingInventory(false);


            session.getWaypointCache().clear();
        }

        session.setDimensionType(newDimension);
        session.setWorldName(spawnInfo.getWorldName());
        session.setLevels(Arrays.stream(packet.getWorldNames()).map(Key::asString).toArray(String[]::new));
        session.setGameMode(spawnInfo.getGameMode());

        bool needsSpawnPacket = !session.isSentSpawnPacket();
        if (needsSpawnPacket) {

            DimensionUtils.setBedrockDimension(session, newDimension.bedrockId());
            session.connect();


            session.getUpstream().sendPostStartGamePackets();
        } else {
            SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
            playerGameTypePacket.setGamemode(EntityUtils.toBedrockGamemode(spawnInfo.getGameMode()).ordinal());
            session.sendUpstreamPacket(playerGameTypePacket);
        }

        entity.setLastDeathPosition(spawnInfo.getLastDeathPos());

        entity.updateBedrockMetadata();


        GameRulesChangedPacket gamerulePacket = new GameRulesChangedPacket();
        gamerulePacket.getGameRules().add(new GameRuleData<>("doimmediaterespawn", !packet.isEnableRespawnScreen()));
        session.sendUpstreamPacket(gamerulePacket);

        session.setReducedDebugInfo(packet.isReducedDebugInfo());

        session.setServerRenderDistance(packet.getViewDistance());



        session.sendJavaClientSettings();

        Key register = MinecraftKey.key("register");
        if (session.remoteServer().authType() == AuthType.FLOODGATE) {
            session.sendDownstreamPacket(new ServerboundCustomPayloadPacket(register, PluginMessageChannels.getFloodgateRegisterData()));
        }
        session.sendDownstreamPacket(new ServerboundCustomPayloadPacket(register, Constants.PLUGIN_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        if (session.getBedrockDimension().bedrockId() != newDimension.bedrockId()) {
            DimensionUtils.switchDimension(session, newDimension);
        } else if (BedrockDimension.isCustomBedrockNetherId() && newDimension.isNetherLike()) {

            session.camera().sendFog(DimensionUtils.BEDROCK_FOG_HELL);
        }

        ChunkUtils.loadDimension(session);

        if (!needsSpawnPacket) {
            session.sendDownstreamGamePacket(ServerboundPlayerLoadedPacket.INSTANCE);
        }
    }
}
