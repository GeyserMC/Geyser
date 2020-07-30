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

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.world.notify.EnterCreditsValue;
import com.github.steveice10.mc.protocol.data.game.world.notify.RespawnScreenValue;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerNotifyClientPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.AdventureSetting;
import com.nukkitx.protocol.bedrock.data.GameRuleData;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.PlayerPermission;
import com.nukkitx.protocol.bedrock.data.command.CommandPermission;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.packet.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.inventory.PlayerInventoryTranslator;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Translator(packet = ServerNotifyClientPacket.class)
public class JavaNotifyClientTranslator extends PacketTranslator<ServerNotifyClientPacket> {

    @Override
    public void translate(ServerNotifyClientPacket packet, GeyserSession session) {
        PlayerEntity entity = session.getPlayerEntity();
        if (entity == null)
            return;

        switch (packet.getNotification()) {
            case START_RAIN:
                LevelEventPacket startRainPacket = new LevelEventPacket();
                startRainPacket.setType(LevelEventType.START_RAINING);
                startRainPacket.setData(ThreadLocalRandom.current().nextInt(50000) + 10000);
                startRainPacket.setPosition(Vector3f.ZERO);
                session.sendUpstreamPacket(startRainPacket);
                break;
            case STOP_RAIN:
                LevelEventPacket stopRainPacket = new LevelEventPacket();
                stopRainPacket.setType(LevelEventType.STOP_RAINING);
                stopRainPacket.setData(ThreadLocalRandom.current().nextInt(50000) + 10000);
                stopRainPacket.setPosition(Vector3f.ZERO);
                session.sendUpstreamPacket(stopRainPacket);
                break;
            case CHANGE_GAMEMODE:
                GameMode gameMode = (GameMode) packet.getValue();

                session.setNoClip(gameMode == GameMode.SPECTATOR);
                session.setWorldImmutable(gameMode == GameMode.ADVENTURE || gameMode == GameMode.SPECTATOR);
                session.sendAdventureSettings();

                SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
                playerGameTypePacket.setGamemode(gameMode.ordinal());
                session.sendUpstreamPacket(playerGameTypePacket);
                session.setGameMode(gameMode);

                // Update the crafting grid to add/remove barriers for creative inventory
                PlayerInventoryTranslator.updateCraftingGrid(session, session.getInventory());
                break;
            case ENTER_CREDITS:
                switch ((EnterCreditsValue) packet.getValue()) {
                    case SEEN_BEFORE:
                        ClientRequestPacket javaRespawnPacket = new ClientRequestPacket(ClientRequest.RESPAWN);
                        session.sendDownstreamPacket(javaRespawnPacket);
                        break;
                    case FIRST_TIME:
                        ShowCreditsPacket showCreditsPacket = new ShowCreditsPacket();
                        showCreditsPacket.setStatus(ShowCreditsPacket.Status.START_CREDITS);
                        showCreditsPacket.setRuntimeEntityId(entity.getGeyserId());
                        session.sendUpstreamPacket(showCreditsPacket);
                        break;
                }
                break;
            case AFFECTED_BY_ELDER_GUARDIAN:
                EntityEventPacket eventPacket = new EntityEventPacket();
                eventPacket.setType(EntityEventType.ELDER_GUARDIAN_CURSE);
                eventPacket.setData(0);
                eventPacket.setRuntimeEntityId(entity.getGeyserId());
                session.sendUpstreamPacket(eventPacket);
                break;
            case ENABLE_RESPAWN_SCREEN:
                GameRulesChangedPacket gamerulePacket = new GameRulesChangedPacket();
                gamerulePacket.getGameRules().add(new GameRuleData<>("doimmediaterespawn",
                        packet.getValue() == RespawnScreenValue.IMMEDIATE_RESPAWN));
                session.sendUpstreamPacket(gamerulePacket);
                break;
            default:
                break;
        }
    }
}
