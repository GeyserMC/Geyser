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

package org.geysermc.connector.edition.mcee.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.PlayerActionPacket;
import com.nukkitx.protocol.bedrock.packet.RespawnPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = PlayerActionPacket.class)
public class BedrockActionTranslator extends org.geysermc.connector.network.translators.bedrock.BedrockActionTranslator {

    @Override
    public void translate(PlayerActionPacket packet, GeyserSession session) {
        Entity entity = session.getPlayerEntity();
        if (entity == null)
            return;

        Vector3i vector = packet.getBlockPosition();
        Position position = new Position(vector.getX(), vector.getY(), vector.getZ());

        switch (packet.getAction()) {
            case RESPAWN:
                RespawnPacket respawnPacket = new RespawnPacket();
                respawnPacket.setRuntimeEntityId(0);
                respawnPacket.setPosition(Vector3f.ZERO);
                respawnPacket.setState(RespawnPacket.State.SERVER_SEARCHING);
                session.sendUpstreamPacket(respawnPacket);

                ClientRequestPacket javaRespawnPacket = new ClientRequestPacket(ClientRequest.RESPAWN);
                session.sendDownstreamPacket(javaRespawnPacket);
                break;
            default:
                super.translate(packet, session);
        }
    }
}
