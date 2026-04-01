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

package org.geysermc.geyser.translator.protocol.java.level;

#include "org.geysermc.mcprotocollib.protocol.data.game.ClientCommand"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.notify.EnterCreditsValue"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.notify.RainStrengthValue"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.notify.RespawnScreenValue"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.notify.ThunderStrengthValue"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket"
#include "org.cloudburstmc.protocol.bedrock.data.GameRuleData"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType"
#include "org.cloudburstmc.protocol.bedrock.packet.*"
#include "org.geysermc.geyser.entity.type.player.PlayerEntity"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.MinecraftLocale"
#include "org.geysermc.geyser.translator.inventory.PlayerInventoryTranslator"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.EntityUtils"

@Translator(packet = ClientboundGameEventPacket.class)
public class JavaGameEventTranslator extends PacketTranslator<ClientboundGameEventPacket> {
    override public void translate(GeyserSession session, ClientboundGameEventPacket packet) {
        PlayerEntity entity = session.getPlayerEntity();

        switch (packet.getNotification()) {








            case START_RAINING:
                session.updateRain(0);
                break;
            case STOP_RAINING:
                session.updateRain(1);
                break;
            case RAIN_LEVEL_CHANGE:

                float rainStrength = ((RainStrengthValue) packet.getValue()).getStrength();
                session.updateRain(rainStrength);
                break;
            case THUNDER_LEVEL_CHANGE:

                float thunderStrength = ((ThunderStrengthValue) packet.getValue()).getStrength();
                session.updateThunder(thunderStrength);
                break;
            case CHANGE_GAME_MODE:
                GameMode gameMode = (GameMode) packet.getValue();

                SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
                playerGameTypePacket.setGamemode(EntityUtils.toBedrockGamemode(gameMode).ordinal());
                session.sendUpstreamPacket(playerGameTypePacket);
                session.setGameMode(gameMode);

                session.sendAdventureSettings();

                if (session.getPlayerEntity().isOnGround() && gameMode == GameMode.SPECTATOR) {

                    MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
                    movePlayerPacket.setRuntimeEntityId(entity.geyserId());
                    movePlayerPacket.setPosition(entity.bedrockPosition());
                    movePlayerPacket.setRotation(entity.bedrockRotation());
                    movePlayerPacket.setOnGround(false);
                    movePlayerPacket.setMode(MovePlayerPacket.Mode.TELEPORT);
                    movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.UNKNOWN);
                    session.sendUpstreamPacket(movePlayerPacket);
                }


                PlayerInventoryTranslator.updateCraftingGrid(session, session.getPlayerInventory());
                break;
            case WIN_GAME:
                switch ((EnterCreditsValue) packet.getValue()) {
                    case SEEN_BEFORE -> {
                        ServerboundClientCommandPacket javaRespawnPacket = new ServerboundClientCommandPacket(ClientCommand.RESPAWN);
                        session.sendDownstreamGamePacket(javaRespawnPacket);
                    }
                    case FIRST_TIME -> {
                        ShowCreditsPacket showCreditsPacket = new ShowCreditsPacket();
                        showCreditsPacket.setStatus(ShowCreditsPacket.Status.START_CREDITS);
                        showCreditsPacket.setRuntimeEntityId(entity.geyserId());
                        session.sendUpstreamPacket(showCreditsPacket);
                    }
                }
                break;
            case GUARDIAN_ELDER_EFFECT:


                EntityEventPacket eventPacket = new EntityEventPacket();
                eventPacket.setType(EntityEventType.ELDER_GUARDIAN_CURSE);
                eventPacket.setData(0);
                eventPacket.setRuntimeEntityId(entity.geyserId());
                session.sendUpstreamPacket(eventPacket);
                break;
            case IMMEDIATE_RESPAWN:
                GameRulesChangedPacket gamerulePacket = new GameRulesChangedPacket();
                gamerulePacket.getGameRules().add(new GameRuleData<>("doimmediaterespawn",
                        packet.getValue() == RespawnScreenValue.IMMEDIATE_RESPAWN));
                session.sendUpstreamPacket(gamerulePacket);
                break;
            case NO_RESPAWN_BLOCK_AVAILABLE:

                session.sendMessage(MinecraftLocale.getLocaleString("block.minecraft.spawn.not_valid",
                        session.locale()));
                break;
            case PLAY_ARROW_HIT_SOUND:
                PlaySoundPacket arrowSoundPacket = new PlaySoundPacket();
                arrowSoundPacket.setSound("random.orb");
                arrowSoundPacket.setPitch(0.5f);
                arrowSoundPacket.setVolume(0.5f);
                arrowSoundPacket.setPosition(entity.bedrockPosition());
                session.sendUpstreamPacket(arrowSoundPacket);
                break;
            default:



                break;
        }
    }
}
