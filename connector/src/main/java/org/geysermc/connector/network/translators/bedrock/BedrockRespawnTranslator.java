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

package org.geysermc.connector.network.translators.bedrock;

import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.RespawnPacket;

@Translator(packet = RespawnPacket.class)
public class BedrockRespawnTranslator extends PacketTranslator<RespawnPacket> {

    @Override
    public void translate(RespawnPacket packet, GeyserSession session) {
        if (packet.getState() == RespawnPacket.State.CLIENT_READY) {
            RespawnPacket respawnPacket = new RespawnPacket();
            respawnPacket.setRuntimeEntityId(0);
            respawnPacket.setPosition(Vector3f.ZERO);
            respawnPacket.setState(RespawnPacket.State.SERVER_SEARCHING);
            session.getUpstream().sendPacket(respawnPacket);

            ClientRequestPacket javaRespawnPacket = new ClientRequestPacket(ClientRequest.RESPAWN);
            session.getDownstream().getSession().send(javaRespawnPacket);
        }
    }
}
