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

#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.packet.SetPlayerGameTypePacket"
#include "org.geysermc.geyser.entity.attribute.GeyserAttributeType"
#include "org.geysermc.geyser.entity.type.player.SessionPlayerEntity"
#include "org.geysermc.geyser.level.JavaDimension"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.ChunkUtils"
#include "org.geysermc.geyser.util.DimensionUtils"
#include "org.geysermc.geyser.util.EntityUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket"

@Translator(packet = ClientboundRespawnPacket.class)
public class JavaRespawnTranslator extends PacketTranslator<ClientboundRespawnPacket> {

    override public void translate(GeyserSession session, ClientboundRespawnPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();
        PlayerSpawnInfo spawnInfo = packet.getCommonPlayerSpawnInfo();

        if (!packet.isKeepMetadata()) {
            entity.resetMetadata();
        }

        if (!packet.isKeepAttributeModifiers()) {
            entity.resetAttributes();
        }

        session.setSpawned(false);
        entity.setMotion(Vector3f.ZERO);
        entity.setLastTickEndVelocity(Vector3f.ZERO);

        entity.setHealth(entity.getMaxHealth());
        entity.getAttributes().put(GeyserAttributeType.HEALTH, entity.createHealthAttribute());

        session.setInventoryHolder(null);
        session.setClosingInventory(false);

        entity.setLastDeathPosition(spawnInfo.getLastDeathPos());
        entity.updateBedrockMetadata();

        SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
        playerGameTypePacket.setGamemode(EntityUtils.toBedrockGamemode(spawnInfo.getGameMode()).ordinal());
        session.sendUpstreamPacket(playerGameTypePacket);
        session.setGameMode(spawnInfo.getGameMode());

        if (session.isRaining()) {
            session.updateRain(0);
        }

        if (session.isThunder()) {
            session.updateThunder(0);
        }

        JavaDimension newDimension = session.getRegistryCache().registry(JavaRegistries.DIMENSION_TYPE).byId(spawnInfo.getDimension());
        if (session.getDimensionType() != newDimension || !spawnInfo.getWorldName().equals(session.getWorldName())) {

            if (session.getDimensionType().bedrockId() == newDimension.bedrockId()) {
                int fakeDim = DimensionUtils.getTemporaryDimension(session.getDimensionType().bedrockId(), newDimension.bedrockId());
                DimensionUtils.fastSwitchDimension(session, fakeDim);
            }
            session.setWorldName(spawnInfo.getWorldName());
            session.setWorldTicks(0);
            DimensionUtils.switchDimension(session, newDimension);

            ChunkUtils.loadDimension(session);
        }

        session.sendDownstreamGamePacket(ServerboundPlayerLoadedPacket.INSTANCE);
    }
}
