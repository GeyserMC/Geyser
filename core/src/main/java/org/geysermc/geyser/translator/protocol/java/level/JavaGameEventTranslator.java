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

package org.geysermc.geyser.translator.protocol.java.level;

import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.level.notify.EnterCreditsValue;
import com.github.steveice10.mc.protocol.data.game.level.notify.RainStrengthValue;
import com.github.steveice10.mc.protocol.data.game.level.notify.RespawnScreenValue;
import com.github.steveice10.mc.protocol.data.game.level.notify.ThunderStrengthValue;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.GameRuleData;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.inventory.PlayerInventoryTranslator;
import org.geysermc.geyser.text.MinecraftLocale;

@Translator(packet = ClientboundGameEventPacket.class)
public class JavaGameEventTranslator extends PacketTranslator<ClientboundGameEventPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundGameEventPacket packet) {
        PlayerEntity entity = session.getPlayerEntity();

        switch (packet.getNotification()) {
            case START_RAIN:
                LevelEventPacket startRainPacket = new LevelEventPacket();
                startRainPacket.setType(LevelEventType.START_RAINING);
                startRainPacket.setData(Integer.MAX_VALUE);
                startRainPacket.setPosition(Vector3f.ZERO);
                session.sendUpstreamPacket(startRainPacket);
                session.setRaining(true);
                break;
            case STOP_RAIN:
                LevelEventPacket stopRainPacket = new LevelEventPacket();
                stopRainPacket.setType(LevelEventType.STOP_RAINING);
                stopRainPacket.setData(0);
                stopRainPacket.setPosition(Vector3f.ZERO);
                session.sendUpstreamPacket(stopRainPacket);
                session.setRaining(false);
                break;
            case RAIN_STRENGTH:
                // While the above values are used, they CANNOT BE TRUSTED on a vanilla server as they are swapped around
                // Spigot and forks implement it correctly
                // Rain strength is your best way for determining if there is any rain
                RainStrengthValue value = (RainStrengthValue) packet.getValue();
                boolean isCurrentlyRaining = value.getStrength() > 0f;
                // Java sends the rain level. Bedrock doesn't care, so we don't care if it's already raining.
                if (isCurrentlyRaining != session.isRaining()) {
                    LevelEventPacket changeRainPacket = new LevelEventPacket();
                    changeRainPacket.setType(isCurrentlyRaining ? LevelEventType.START_RAINING : LevelEventType.STOP_RAINING);
                    changeRainPacket.setData(Integer.MAX_VALUE); // Dunno what this does; used to be implemented with ThreadLocalRandom
                    changeRainPacket.setPosition(Vector3f.ZERO);
                    session.sendUpstreamPacket(changeRainPacket);
                    session.setRaining(isCurrentlyRaining);
                }
                break;
            case THUNDER_STRENGTH:
                // See above, same process
                ThunderStrengthValue thunderValue = (ThunderStrengthValue) packet.getValue();
                boolean isCurrentlyThundering = thunderValue.getStrength() > 0f;
                if (isCurrentlyThundering != session.isThunder()) {
                    LevelEventPacket changeThunderPacket = new LevelEventPacket();
                    changeThunderPacket.setType(isCurrentlyThundering ? LevelEventType.START_THUNDERSTORM : LevelEventType.STOP_THUNDERSTORM);
                    changeThunderPacket.setData(Integer.MAX_VALUE);
                    changeThunderPacket.setPosition(Vector3f.ZERO);
                    session.sendUpstreamPacket(changeThunderPacket);
                    session.setThunder(isCurrentlyThundering);
                }
                break;
            case CHANGE_GAMEMODE:
                GameMode gameMode = (GameMode) packet.getValue();

                SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
                playerGameTypePacket.setGamemode(gameMode.ordinal());
                session.sendUpstreamPacket(playerGameTypePacket);
                session.setGameMode(gameMode);

                session.sendAdventureSettings();

                if (session.getPlayerEntity().isOnGround() && gameMode == GameMode.SPECTATOR) {
                    // Fix a bug where the player has glitched movement and thinks they are still on the ground
                    MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
                    movePlayerPacket.setRuntimeEntityId(entity.getGeyserId());
                    movePlayerPacket.setPosition(entity.getPosition());
                    movePlayerPacket.setRotation(entity.getBedrockRotation());
                    movePlayerPacket.setOnGround(false);
                    movePlayerPacket.setMode(MovePlayerPacket.Mode.TELEPORT);
                    movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.UNKNOWN);
                    session.sendUpstreamPacket(movePlayerPacket);
                }

                // Update the crafting grid to add/remove barriers for creative inventory
                PlayerInventoryTranslator.updateCraftingGrid(session, session.getPlayerInventory());
                break;
            case ENTER_CREDITS:
                switch ((EnterCreditsValue) packet.getValue()) {
                    case SEEN_BEFORE -> {
                        ServerboundClientCommandPacket javaRespawnPacket = new ServerboundClientCommandPacket(ClientCommand.RESPAWN);
                        session.sendDownstreamPacket(javaRespawnPacket);
                    }
                    case FIRST_TIME -> {
                        ShowCreditsPacket showCreditsPacket = new ShowCreditsPacket();
                        showCreditsPacket.setStatus(ShowCreditsPacket.Status.START_CREDITS);
                        showCreditsPacket.setRuntimeEntityId(entity.getGeyserId());
                        session.sendUpstreamPacket(showCreditsPacket);
                    }
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
            case INVALID_BED:
                // Not sent as a proper message? Odd.
                session.sendMessage(MinecraftLocale.getLocaleString("block.minecraft.spawn.not_valid",
                        session.getLocale()));
                break;
            case ARROW_HIT_PLAYER:
                PlaySoundPacket arrowSoundPacket = new PlaySoundPacket();
                arrowSoundPacket.setSound("random.orb");
                arrowSoundPacket.setPitch(0.5f);
                arrowSoundPacket.setVolume(0.5f);
                arrowSoundPacket.setPosition(entity.getPosition());
                session.sendUpstreamPacket(arrowSoundPacket);
                break;
            default:
                break;
        }
    }
}
