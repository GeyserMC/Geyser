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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player;

import org.cloudburstmc.protocol.bedrock.packet.SetPlayerGameTypePacket;
import org.geysermc.geyser.Permissions;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;

/**
 * In vanilla Bedrock, if you have operator status, this sets the player's gamemode without confirmation from the server.
 * With operator status, the Gamemode change is sent to the Java server, if it is not present, the gamemode is not changed.
 */
@Translator(packet = SetPlayerGameTypePacket.class)
public class BedrockSetPlayerGameTypeTranslator extends PacketTranslator<SetPlayerGameTypePacket> {

    /**
     * Sets client game mode for the server via the Bedrock client's "world" menu (given sufficient permissions).
     */
    @Override
    public void translate(GeyserSession session, SetPlayerGameTypePacket packet) {
        // yes, if you are OP
        if (session.getOpPermissionLevel() >= 2 && session.hasPermission(Permissions.SERVER_SETTINGS)) {
            if (packet.getGamemode() != session.getGameMode().ordinal()) {
                // Bedrock has more Gamemodes than Java, leading to cases 5 (for "default") and 6 (for "spectator") being sent
                // https://github.com/CloudburstMC/Protocol/blob/3.0/bedrock-codec/src/main/java/org/cloudburstmc/protocol/bedrock/data/GameType.java
                GameMode gameMode = switch (packet.getGamemode()) {
                    case 1 -> GameMode.CREATIVE;
                    case 2 -> GameMode.ADVENTURE;
                    case 5 -> session.getGeyser().getWorldManager().getDefaultGameMode(session);
                    case 6 -> GameMode.SPECTATOR;
                    default -> GameMode.SURVIVAL;
                };
                session.getGeyser().getWorldManager().setPlayerGameMode(session, gameMode);
            }
        } else {
            SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
            playerGameTypePacket.setGamemode(EntityUtils.toBedrockGamemode(session.getGameMode()).ordinal());
            session.sendUpstreamPacket(playerGameTypePacket);
        }
    }
}
