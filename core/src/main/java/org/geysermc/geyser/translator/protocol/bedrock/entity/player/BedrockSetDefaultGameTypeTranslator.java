/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

import org.cloudburstmc.protocol.bedrock.packet.SetDefaultGameTypePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetPlayerGameTypePacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;

@Translator(packet = SetDefaultGameTypePacket.class)
public class BedrockSetDefaultGameTypeTranslator extends PacketTranslator<SetDefaultGameTypePacket> {

    /**
     * Sets the default game mode for the server via the Bedrock client's "world" menu (given sufficient permissions).
     */
    @Override
    public void translate(GeyserSession session, SetDefaultGameTypePacket packet) {
        if (session.getOpPermissionLevel() >= 2 && session.hasPermission("geyser.settings.server")) {
            session.getGeyser().getWorldManager().setDefaultGameMode(session, GameMode.byId(packet.getGamemode()));
        }
        // Stop the client from updating their own Gamemode without telling the server
        // Can occur when client Gamemode is set to "default", and default game mode is changed.
        SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
        playerGameTypePacket.setGamemode(EntityUtils.toBedrockGamemode(session.getGameMode()).ordinal());
        session.sendUpstreamPacket(playerGameTypePacket);
    }
}
