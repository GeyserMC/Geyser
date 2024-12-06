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

import org.cloudburstmc.protocol.bedrock.packet.SetPlayerGameTypePacket;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket;

@Translator(packet = ClientboundRespawnPacket.class)
public class JavaRespawnTranslator extends PacketTranslator<ClientboundRespawnPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundRespawnPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();
        PlayerSpawnInfo spawnInfo = packet.getCommonPlayerSpawnInfo();

        if (!packet.isKeepMetadata()) {
            entity.resetMetadata();
        }

        if (!packet.isKeepAttributeModifiers()) {
            entity.resetAttributes();
        }

        session.setSpawned(false);

        entity.setHealth(entity.getMaxHealth());
        entity.getAttributes().put(GeyserAttributeType.HEALTH, entity.createHealthAttribute());

        session.setInventoryTranslator(InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR);
        session.setOpenInventory(null);
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

        JavaDimension newDimension = session.getRegistryCache().dimensions().byId(spawnInfo.getDimension());
        if (session.getDimensionType() != newDimension || !spawnInfo.getWorldName().equals(session.getWorldName())) {
            // Switching to a new world (based off the world name change or new dimension); send a fake dimension change
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
